package org.endeavourhealth.patientfhirextractor.threads.organization;

import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.dal.SubscriberJdbcDAL;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;
import org.endeavourhealth.patientfhirextractor.helper.ResourceUtil;
import org.endeavourhealth.patientfhirextractor.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BulkOrganizationThread extends OrganizationThread {
    private static final Logger LOG = LoggerFactory.getLogger(BulkOrganizationThread.class);
    private Integer subscriberThreadId;
    private int configId;
    private String configName;
    private CIEExtractorConfig configurations;
    private List<String> odsCodes;

    public static void setIsOrgErrored(boolean isOrgErrored) {
        BulkOrganizationThread.isOrgErrored = isOrgErrored;
    }

    private static boolean isOrgErrored = false;

    public BulkOrganizationThread(Integer subscriberThreadId, String configName, List<String> odsCodes, CIEExtractorConfig cieExtractorConfig, int configId) {
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

            orgsThreadId = cieExtractsDal.newOrgsThread(subscriberThreadId);
            if (CIEFhirExtractorEndpoint.isInterrupted("Bulk")) {
                throw new Exception("User Interrupted");

            }
            try (SubscriberJdbcDAL subscriberDal = new SubscriberJdbcDAL()) {
                subscriberDal.setSubscriberConnection(configName);
                LOG.debug("ODS codes received . Loop through and process them" + odsCodes);
                if (CIEFhirExtractorEndpoint.isInterrupted("Bulk")) {
                    throw new Exception("User Interrupted");

                }
                for (String org : odsCodes) {
                    isOrgErrored=false;
                    LOG.info("Organization '"+org+"' Thread initiated for process:Bulk");
                    if (!cieExtractsDal.isInOrgCohort(org, null) ) {
                        java.sql.Timestamp bulkStartTime = new java.sql.Timestamp(System.currentTimeMillis());
                        cieExtractsDal.newOrgProcess(orgsThreadId, org, bulkStartTime, null, "Started");
                        OrganizationFull bulkOrganization = null;
                        try {

                            String orgUUID = cieExtractsDal.checkReferenceExistsForOrg(org);
                            if (orgUUID.isEmpty()) {
                                bulkOrganization = subscriberDal.getOrganizationFull(org);
                                org.hl7.fhir.dstu3.model.Organization organizationResource = null;
                                if (bulkOrganization.getId() > 0) {
                                    organizationResource = resourceUtil.processOrganization(bulkOrganization, cieExtractsDal, subscriberDal, configId);
                                } else {
                                    LOG.debug("Org" + org + "does not exist in db");
                                }
                                if (CIEFhirExtractorEndpoint.isInterrupted("Bulk")) {
                                    throw new Exception("User Interrupted");

                                }
                                resourceUtil.processPatientData(org, bulkOrganization.getId(), cieExtractsDal, subscriberDal, organizationResource.getId(), "Bulk", configurations,true,configId);
                            }
                            else {
                                bulkOrganization = subscriberDal.getOrganizationFull(org);
                                if (CIEFhirExtractorEndpoint.isInterrupted("Bulk")) {
                                    throw new Exception("User Interrupted");

                                }
                                resourceUtil.processPatientData(org,bulkOrganization.getId(), cieExtractsDal, subscriberDal, orgUUID,"Bulk", configurations,true,configId);
                            }

                            cieExtractsDal.updateOrgProcessStatus(org, "Completed");
                        } catch (Exception e) {
                            cieExtractsDal.updateOrgProcessStatus(org, "Errored");
                            e.printStackTrace();
                            throw new Exception(e);
                        }

                       //Once the organization process completed. Update in Org cohort table.
                        if(!isOrgErrored) {
                            cieExtractsDal.orgCohortCompleted(org, configId);
                        }
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


}

