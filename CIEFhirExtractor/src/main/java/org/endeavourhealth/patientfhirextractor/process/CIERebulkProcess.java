package org.endeavourhealth.patientfhirextractor.process;

import org.endeavourhealth.common.utility.SlackHelper;
import org.endeavourhealth.common.utility.ThreadPool;
import org.endeavourhealth.common.utility.ThreadPoolError;
import org.endeavourhealth.core.database.dal.DalProvider;
import org.endeavourhealth.core.database.dal.audit.ScheduledTaskAuditDalI;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.helper.SubscriberConfigHelper;
import org.endeavourhealth.patientfhirextractor.models.CIEExtractorConfig;
import org.endeavourhealth.patientfhirextractor.threads.subscriber.SubscriberThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CIERebulkProcess {
    private static final Logger LOG = LoggerFactory.getLogger(CIERebulkProcess.class);

    public CIEExtractorConfig cieConfig;

    public CIERebulkProcess(CIEExtractorConfig cieConfig) {
        this.cieConfig = cieConfig;
    }

    private static void handleErrors(List<ThreadPoolError> errors) throws Exception {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        for (ThreadPoolError error : errors) {
            CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL();
            cieExtractsDal.setCieProcessConnection();
            SubscriberThread subscriberThreadCallable = (SubscriberThread) error.getCallable();
            cieExtractsDal.subscriberThreadErrored(subscriberThreadCallable.getSubscriberThreadId());
            cieExtractsDal.close();
        }
        //if we've had multiple errors, just throw the first one, since they'll most-likely be the same
        ThreadPoolError first = errors.get(0);
        Throwable cause = first.getException();
        //the cause may be an Exception or Error so we need to explicitly
        //cast to the right type to throw it without changing the method signature
        if (cause instanceof Exception) {
            throw (Exception) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        }
    }

    public Response process(String[] projectIds, String resourceType) throws Exception {

        try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {
            try {
                cieExtractsDal.setCieProcessConnection();
                // Reading CIE Configrations and getting Config Names with ODS Codes.
                Map<String, List<String>> configNamesWithPubCodes = SubscriberConfigHelper.getConfigNamesAndPublisherCodes(projectIds);
                syncSubscriberConfigIds(configNamesWithPubCodes.keySet(), cieExtractsDal);

                // Create a Thread Pool and submit each Config Name with ODS Code to thread.
                ThreadPool threadpool = new ThreadPool(configNamesWithPubCodes.keySet().size(), cieConfig.getMaxQueueBeforeBlocking(), "Subscriber Thread Pool");
                try {
                    Set<String> configNames = configNamesWithPubCodes.keySet();
                    for (String configName : configNames) {
                        int configId = cieExtractsDal.getConfigId(configName);
                        LOG.debug("Submit a Callable Class with Config Name " + configName + " with ODS Codes" + configNamesWithPubCodes.get(configName));
                        threadpool.submit(new SubscriberThread(configName, configNamesWithPubCodes.get(configName), "Rebulk", cieConfig, configId, resourceType));
                    }
                } finally {
                    List<ThreadPoolError> errors = threadpool.waitAndStop();
                    handleErrors(errors);
                }
            } catch (Exception e) {
                LOG.error("Error processing the data for Re-Bulk: " + e);
                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, "Exception in processing the data for Re-Bulk" + e);
                auditFailure(new String[]{"Re-Bulk Process"}, e);
                cieExtractsDal.setProcessStatus("ReBulk", "Errored");
                // need to prepare error response
                e.printStackTrace();
                throw new Exception(e);
            }
            LOG.info("Re-Bulk Process is completed");
            SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, "Re-Bulk process completed successfully" );
            auditSuccess(new String[]{"Re-Bulk Process"});
            cieExtractsDal.setProcessStatus("ReBulk", "Completed");
        }

        return Response
                .status(Response.Status.OK).build();
    }

    private void syncSubscriberConfigIds(Set<String> configNames, CIEJdbcDAL cieExtractsDal) throws SQLException {
        LOG.debug("ConfigNames" + configNames);
        for (String configName : configNames) {
            String configStatus = cieExtractsDal.getConfigNameStatus(configName);
            if (configStatus.isEmpty()) {
                cieExtractsDal.saveNewConfigNameAsActive(configName);
            } else if (configStatus.equals("Inactive")) {
                cieExtractsDal.setConfigNameToActive(configName);
            }
        }
        cieExtractsDal.setRemainingConfigNameToInactive(configNames);
    }

    private static void auditSuccess(String[] args) throws Exception {
        ScheduledTaskAuditDalI dal = DalProvider.factoryScheduledTaskAuditDal();
        dal.auditTaskSuccess("cie-rebulk", args);
    }

    private static void auditFailure(String[] args, Throwable t) throws Exception {
        ScheduledTaskAuditDalI dal = DalProvider.factoryScheduledTaskAuditDal();
        dal.auditTaskFailure("cie-rebulk", args, t);
    }
}

