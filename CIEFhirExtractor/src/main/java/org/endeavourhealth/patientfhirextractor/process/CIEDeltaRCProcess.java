package org.endeavourhealth.patientfhirextractor.process;

import org.endeavourhealth.common.utility.SlackHelper;
import org.endeavourhealth.common.utility.ThreadPool;
import org.endeavourhealth.common.utility.ThreadPoolError;
import org.endeavourhealth.core.database.dal.DalProvider;
import org.endeavourhealth.core.database.dal.audit.ScheduledTaskAuditDalI;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;
import org.endeavourhealth.patientfhirextractor.helper.SubscriberConfigHelper;
import org.endeavourhealth.patientfhirextractor.models.CIEExtractorConfig;
import org.endeavourhealth.patientfhirextractor.threads.subscriber.SubscriberThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.core.Response;

import java.util.*;

public class CIEDeltaRCProcess {
    private static final Logger LOG = LoggerFactory.getLogger(CIEDeltaRCProcess.class);

    public CIEExtractorConfig cieExtractorConfig;

    public CIEDeltaRCProcess(CIEExtractorConfig cieExtractorConfig) {
        this.cieExtractorConfig=cieExtractorConfig;
    }

    public Response process(String[] projectIds) throws Exception {
        java.sql.Date startDateTime = new java.sql.Date(System.currentTimeMillis());
        try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {
            try {

                cieExtractsDal.setCieProcessConnection();
                // Reading CIE Configrations and getting Config Names with ODS Codes.

                if (CIEFhirExtractorEndpoint.isInterrupted("DeltaRC")) {
                    cieExtractsDal.setProcessStatus("DeltaRC", "Interrupted");
                    return Response.status(503).entity("User Interrupted").build();

                }
               // Reading CIE Configrations and getting Config Names with ODS Codes.
                Map<String, List<String>> configNamesWithPubCodes = SubscriberConfigHelper.getConfigNamesAndPublisherCodes(projectIds);
                cieExtractsDal.syncSubscriberConfigIds(configNamesWithPubCodes.keySet());

                if (CIEFhirExtractorEndpoint.isInterrupted("DeltaRC")) {
                    cieExtractsDal.setProcessStatus("DeltaRC", "Interrupted");
                    return Response.status(503).entity("User Interrupted").build();

                }

                // Create a Thread Pool and submit each Config Name with ODS Code to thread.
                ThreadPool threadpool = new ThreadPool(configNamesWithPubCodes.keySet().size(), 10, "Subscriber Delta Thread Pool");
                try {
                    Set<String> configNames = configNamesWithPubCodes.keySet();
                    for (String configName : configNames) {
                        int configId = cieExtractsDal.getConfigId(configName);
                        LOG.debug("Submit a Callable Class with Config Name " + configName + " with ODS Codes" + configNamesWithPubCodes.get(configName));
                        threadpool.submit(new SubscriberThread(configName, configNamesWithPubCodes.get(configName), "DeltaRC", cieExtractorConfig, configId));
                    }
                } finally {
                    List<ThreadPoolError> errors = threadpool.waitAndStop();
                    handleErrors(errors, cieExtractsDal);
                }
            }
            catch (Exception e) {
                LOG.error("Error processing the data for DeltaRC: " + e);
                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, "Exception in processing the data for DeltaRC" + e);
                auditFailure(new String[]{"Delta RC Process"}, e);
                if(CIEFhirExtractorEndpoint.isInterrupted("DeltaRC"))
                {
                    cieExtractsDal.setProcessStatus("DeltaRC","Interrupted");
                    return Response.status(503).entity("User Interrupted").build();

                }
                else {
                    cieExtractsDal.setProcessStatus("DeltaRC","Errored");
                }
                e.printStackTrace();
                return Response.status(503).entity("Internal Server Error at Subscriber thread").build();
            }
            LOG.info("DeltaRC Process is completed");
            SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, "DeltaRC process completed successfully" );
            auditSuccess(new String[]{"Delta RC Process"});
            cieExtractsDal.setProcessStatus("DeltaRC","Completed");
        }

        return Response
                .status(Response.Status.OK).build();
    }


    private static void handleErrors(List<ThreadPoolError> errors, CIEJdbcDAL cieExtractsDal) throws Exception {
        if (errors == null || errors.isEmpty()) {
            return;
        }

        for (ThreadPoolError error:errors) {
            SubscriberThread subscriberThreadCallable = (SubscriberThread) error.getCallable();
            cieExtractsDal.subscriberThreadErrored(subscriberThreadCallable.getSubscriberThreadId());
        }
        //if we've had multiple errors, just throw the first one, since they'll most-likely be the same
        ThreadPoolError first = errors.get(0);
        Throwable cause = first.getException();
        //the cause may be an Exception or Error so we need to explicitly
        //cast to the right type to throw it without changing the method signature
        if (cause instanceof Exception) {
            throw (Exception)cause;
        } else if (cause instanceof Error) {
            throw (Error)cause;
        }
    }

    private static void auditSuccess(String[] args) throws Exception {
        ScheduledTaskAuditDalI dal = DalProvider.factoryScheduledTaskAuditDal();
        dal.auditTaskSuccess("cie-deltarc", args);
    }

    private static void auditFailure(String[] args, Throwable t) throws Exception {
        ScheduledTaskAuditDalI dal = DalProvider.factoryScheduledTaskAuditDal();
        dal.auditTaskFailure("cie-deltarc", args, t);
    }
}
