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
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CIEBulkProcess {

    private static final Logger LOG = LoggerFactory.getLogger(CIEBulkProcess.class);
    public CIEExtractorConfig cieConfig;

    public CIEBulkProcess(CIEExtractorConfig cieConfig) {
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

    public Response process(String[] projectIds) throws Exception {
        Response response;
        java.sql.Date startDateTime = new Date(System.currentTimeMillis());
        try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {
            try {
                cieExtractsDal.setCieProcessConnection();
                // Reading Config Names with ODS Codes.

                response = CIEProcessUtil.stopIfUserInterrupted("Bulk", cieExtractsDal);
                if (null != response) return response;


                Map<String, List<String>> configNamesWithPubCodes = SubscriberConfigHelper.getConfigNamesAndPublisherCodes(projectIds);
                cieExtractsDal.syncSubscriberConfigIds(configNamesWithPubCodes.keySet());

                response = CIEProcessUtil.stopIfUserInterrupted("Bulk", cieExtractsDal);
                if (null != response) return response;

                // Create a Thread Pool and submit each Config Name with ODS Code to thread.
                ThreadPool threadpool = new ThreadPool(configNamesWithPubCodes.keySet().size(), cieConfig.getMaxQueueBeforeBlocking(), "Subscriber Thread Pool Bulk");
                try {
                    Set<String> configNames = configNamesWithPubCodes.keySet();
                    for (String configName : configNames) {
                        int configId = cieExtractsDal.getConfigId(configName);
                        LOG.debug("Submit a Callable Class with Config Name " + configName + " with ODS Codes" + configNamesWithPubCodes.get(configName));
                        threadpool.submit(new SubscriberThread(configName, configNamesWithPubCodes.get(configName), "Bulk", cieConfig, configId));
                    }
                } finally {
                    cieExtractsDal.close();
                    List<ThreadPoolError> errors = threadpool.waitAndStop();
                    handleErrors(errors);
                }

            } catch (Exception e) {
                try(CIEJdbcDAL cieExDlEr = new CIEJdbcDAL()) {
                    LOG.error("Error processing the data for Bulk: " + e);
                    SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, "Exception in processing the data for Bulk" + e);
                    auditFailure(new String[]{"Bulk Process"}, e);
                response = CIEProcessUtil.stopIfUserInterrupted("Bulk", cieExDlEr);
                if (null != response){
                    return response;
                }
                    cieExDlEr.setCieProcessConnection();
                    cieExDlEr.setProcessStatus("Bulk", "Errored");
                }
                catch (Exception ex){
                    throw new Exception(ex);
                }

                e.printStackTrace();
                return Response.status(503).entity("Internal Server Error at Subscriber thread").build();
            }

            try(CIEJdbcDAL cieExDl = new CIEJdbcDAL()) {
                LOG.info("Bulk Process is completed");
                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, "Bulk process completed successfully" );
                auditSuccess(new String[]{"Bulk Process"});
                cieExDl.setCieProcessConnection();
                cieExDl.setProcessStatus("Bulk", "Completed");
            }
            catch (Exception e){
                throw new Exception(e);
            }
        }

        response = Response.status(Response.Status.OK).build();
        return response;
    }

    private static void auditSuccess(String[] args) throws Exception {
        ScheduledTaskAuditDalI dal = DalProvider.factoryScheduledTaskAuditDal();
        dal.auditTaskSuccess("cie-bulk", args);
    }

    private static void auditFailure(String[] args, Throwable t) throws Exception {
        ScheduledTaskAuditDalI dal = DalProvider.factoryScheduledTaskAuditDal();
        dal.auditTaskFailure("cie-bulk", args, t);
    }
}
