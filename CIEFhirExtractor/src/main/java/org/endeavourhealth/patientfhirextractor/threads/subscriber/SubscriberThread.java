package org.endeavourhealth.patientfhirextractor.threads.subscriber;

import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.common.utility.ThreadPool;
import org.endeavourhealth.common.utility.ThreadPoolError;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;
import org.endeavourhealth.patientfhirextractor.models.CIEExtractorConfig;
import org.endeavourhealth.patientfhirextractor.threads.organization.*;

import java.util.List;
import java.util.concurrent.Callable;

public class SubscriberThread implements Callable<Void> {
    private String configName;
    private int configId;
    private List<String> odsCodes;
    private Integer subscriberThreadId;
    private String processType;
    private String resourceType;
    private CIEExtractorConfig configurations;

    public Integer getSubscriberThreadId() {
        return subscriberThreadId;
    }

    public SubscriberThread(String configName, List<String> odsCodes, String processType, CIEExtractorConfig configurations,int configId) {
        this.configName = configName;
        this.odsCodes = odsCodes;
        this.processType=processType;
        this.configurations = configurations;
        this.configId=configId;
    }
 // for Rebulk purpose (all, resource type)
    public SubscriberThread(String configName, List<String> odsCodes, String processType, CIEExtractorConfig configurations, int configId, String resourceType) {
        this.configName = configName;
        this.odsCodes = odsCodes;
        this.processType=processType;
        this.configurations = configurations;
        this.configId=configId;
        this.resourceType = resourceType;
    }

    @Override
    public Void call() throws Exception {
        try(CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()){
            cieExtractsDal.setCieProcessConnection();
            int processId= cieExtractsDal.getProcessId(processType);
            // Inserting a record into subscriber config thread status table for each thread.
            subscriberThreadId =cieExtractsDal.newSubscriberThread(processId,configName);
            if(processType.equalsIgnoreCase("Delta"))
            {
                cieExtractsDal.syncOrgCohortforDelta(configId,odsCodes);
            }
            else if(processType.equalsIgnoreCase("DeltaRC"))
            {
                cieExtractsDal.syncOrgCohortForDeltaRC(configId,odsCodes);
            }
            else if(processType.equalsIgnoreCase("ReBulk"))
            {
                cieExtractsDal.syncOrgCohortForReBulk(configId,odsCodes);
            }

                cieExtractsDal.close();


            int maxNoOfOrgThreads = configurations.getMaxThreadPoolSize();
            int maxOrgsPerThread = configurations.getMaxOrgsPerThread();
            int required_threads = (int)Math.ceil(odsCodes.size()/(double)maxOrgsPerThread);
            int orgThreadPoolSize = Math.min(maxNoOfOrgThreads,required_threads);
            int currentOrgIndex =0;

            if(CIEFhirExtractorEndpoint.isInterrupted(processType))
            {
               throw new Exception("User Interrupted");

            }

        /*    int threadPoolSize = ConnectionManager.getPublisherCommonConnectionPoolMaxSize();
            ThreadPool threadpool = new ThreadPool(threadPoolSize, 1000, processType+ " Org Threads");*/

          ThreadPool threadpool = new ThreadPool(orgThreadPoolSize,configurations.getMaxQueueBeforeBlocking(),processType+ " Org Threads");

            for (int i = 0; i < required_threads; i++) {
                if(CIEFhirExtractorEndpoint.isInterrupted(processType))
                {
                    throw new Exception("User Interrupted");

                }
                int nextIndex=0;
                if(currentOrgIndex+maxOrgsPerThread > odsCodes.size())
                     nextIndex = odsCodes.size();
                    else
                    nextIndex = currentOrgIndex + maxOrgsPerThread;


                   if(processType.equalsIgnoreCase("Bulk")) {
                        threadpool.submit(new BulkOrganizationThread(subscriberThreadId, configName, odsCodes.subList(currentOrgIndex, nextIndex), configurations, configId));
                    }
                     else if(processType.equalsIgnoreCase("Delta")) {
                        threadpool.submit(new DeltaOrganizationThread(subscriberThreadId, configName, odsCodes.subList(currentOrgIndex, nextIndex), configurations, configId));
                     }
                    else if(processType.equalsIgnoreCase("DeltaRC")) {
                        threadpool.submit(new DeltaRCOrganizationThread(subscriberThreadId, configName, odsCodes.subList(currentOrgIndex, nextIndex), configurations, configId));
                    }
                     else {
                        threadpool.submit(new RebulkOrganizationThread(subscriberThreadId, configName, odsCodes.subList(currentOrgIndex, nextIndex), processType, configurations, configId, resourceType));
                    }
                    currentOrgIndex = currentOrgIndex + maxOrgsPerThread;
            }

            List<ThreadPoolError> errors= threadpool.waitAndStop();

            handleErrors(errors);
            try(CIEJdbcDAL cieExtractsDal1 = new CIEJdbcDAL()) {
                cieExtractsDal1.setCieProcessConnection();
                cieExtractsDal1.subscriberThreadCompleted(subscriberThreadId);
            }
            catch (Exception e){
                throw new Exception(e);
            }

        }
        catch (Exception e){
            throw new Exception(e);
        }
        return null;
    }



    private static void handleErrors(List<ThreadPoolError> errors) throws Exception {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        //if we've had multiple errors, just throw the first one, since they'll most-likely be the same
        for (ThreadPoolError error:errors) {
            try(CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {
                cieExtractsDal.setCieProcessConnection();
                OrganizationThread org = (OrganizationThread) error.getCallable();
                cieExtractsDal.orgsThreadErrored(org.getOrgsThreadId());
            }
        }
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
}
