package org.endeavourhealth.patientfhirextractor.threads.organization;


import com.fasterxml.jackson.databind.JsonNode;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.dal.SubscriberJdbcDAL;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;
import org.endeavourhealth.patientfhirextractor.helper.ResourceUtil;
import org.endeavourhealth.patientfhirextractor.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;

public class DeltaOrganizationThread extends OrganizationThread {
    private static final Logger LOG = LoggerFactory.getLogger(DeltaOrganizationThread.class);
    private Integer subscriberThreadId;
    private int configId;
    private String configName;
    private CIEExtractorConfig configurations;
    private List<String> odsCodes;
    private String processType="Delta";

    public DeltaOrganizationThread(Integer subscriberThreadId, String configName, List<String> odsCodes,  CIEExtractorConfig cieExtractorConfig, int configId) {
        this.subscriberThreadId = subscriberThreadId;
        this.configName = configName;
        this.odsCodes = odsCodes;

        this.configurations = cieExtractorConfig;
        this.configId = configId;
    }

    @Override
    public Void call() throws Exception {
        ResourceUtil resourceUtil = new ResourceUtil();
        try (CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {
            cieExtractsDal.setCieProcessConnection();

            orgsThreadId= cieExtractsDal.newOrgsThread(subscriberThreadId); //Table: cie_orgs_thread_status
            if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                throw new Exception("User Interrupted");

            }
            try (SubscriberJdbcDAL subscriberDal = new SubscriberJdbcDAL()) {
                subscriberDal.setSubscriberConnection(configName);
                LOG.debug("ODS codes received . Loop through and process them" + odsCodes);
                if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                    throw new Exception("User Interrupted");

                }
                for (String org : odsCodes) {
                    LOG.info("Organization '"+org+"' Thread initiated for process:"+processType);
                 if (cieExtractsDal.isInOrgCohort(org, "Active")) {
                        OrgProcess orgProcess = cieExtractsDal.getOrgProcess(org);

                        if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                            throw new Exception("User Interrupted");

                        }

                        java.sql.Timestamp lastProcessedDateTime = orgProcess.getStartDatetime();
                        java.sql.Timestamp startDateTime= new java.sql.Timestamp(System.currentTimeMillis());
                        cieExtractsDal.newOrgProcess(orgsThreadId, org, startDateTime, null, "Started"); //Table:cie_org_process_status
                        try {
                            String orgUUID = cieExtractsDal.checkReferenceExistsForOrg(org);
                            if (orgUUID.isEmpty()) {
                                LOG.debug("Org reference cannot be empty for a Delta reference, throw error.");
                            } else {
                                OrganizationFull organizationFull = subscriberDal.getOrganizationFull(org);

                                //Delta events deletion process, priorities are different
                                deltaDeleteProcess(resourceUtil, cieExtractsDal, subscriberDal, org, lastProcessedDateTime, configurations, processType, configId);

                                //Delta events create/update process,priorities are different
                                deltaProcess(resourceUtil, cieExtractsDal, subscriberDal, org, lastProcessedDateTime, orgUUID, organizationFull, configurations, processType, configId);

                                cieExtractsDal.updateOrgProcessStatus(org, "Completed");
                            }
                        } catch (Exception e) {
                            cieExtractsDal.updateOrgProcessStatus(org, "Errored");
                            e.printStackTrace();
                            throw new Exception(e);
                        }
                   //  cieExtractsDal.orgProcessCompleted(org);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception(e);
            }
            cieExtractsDal.orgsThreadCompleted(orgsThreadId); //Table: org_thread_status
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
        return null;
    }

   public static void deltaProcess(ResourceUtil resourceUtil, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, String org, Timestamp lastProcessedDateTime, String orgUUID, OrganizationFull organizationFull, CIEExtractorConfig configurations, String processType, int configId) {
        for (JsonNode resource : configurations.getDeltaPriorities()) {
            switch (resource.asText()) {
                case ResourceConstants.Patient:
                    try {
                        resourceUtil.processDeltaPatients(org, organizationFull.getId(), cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, orgUUID, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
                case ResourceConstants.Condition:
                    try {
                        resourceUtil.processDeltaConditions(org, organizationFull.getId(), cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
                case ResourceConstants.MedicationStatement:
                    try {
                        resourceUtil.processDeltaMedicationAndStatements(org, organizationFull.getId(), orgUUID, cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
                case ResourceConstants.Appointment:
                    try {
                        resourceUtil.processDeltaAppointments(org, organizationFull.getId(), orgUUID, cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
            }
        }
    }

    public static void deltaDeleteProcess(ResourceUtil resourceUtil, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, String org, Timestamp lastProcessedDateTime, CIEExtractorConfig configurations, String processType, int configId) {
        for (JsonNode resource : configurations.getDeltaDeletePriorities()) {
            switch (resource.asText()) {
                case ResourceConstants.Patient:
                    try {
                        resourceUtil.processDeltaDeletePatients(org, cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
                case ResourceConstants.Condition:
                    try {
                        resourceUtil.processDeltaDeleteConditions(org,  cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
                case ResourceConstants.MedicationStatement:
                    try {
                        resourceUtil.processDeltaDeleteMedicationAndStatements(org, cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
                case ResourceConstants.Appointment:
                    try {
                        resourceUtil.processDeltaDeleteAppointments(org, cieExtractsDal, subscriberDal, lastProcessedDateTime, processType, configId);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
            }
        }
    }
}

