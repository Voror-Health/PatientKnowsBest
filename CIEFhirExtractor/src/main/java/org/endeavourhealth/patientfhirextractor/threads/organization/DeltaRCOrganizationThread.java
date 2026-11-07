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

import java.util.List;

public class DeltaRCOrganizationThread extends OrganizationThread {
    private static final Logger LOG = LoggerFactory.getLogger(DeltaRCOrganizationThread.class);
    private Integer subscriberThreadId;
    private int configId;
    private String configName;
    private String processType="DeltaRC";
    private String rebulktype;
    private CIEExtractorConfig configurations;
    private List<String> odsCodes;

    public DeltaRCOrganizationThread(Integer subscriberThreadId, String configName, List<String> odsCodes,  CIEExtractorConfig cieExtractorConfig, int configId) {
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

            orgsThreadId = cieExtractsDal.newOrgsThread(subscriberThreadId); //Table: cie_orgs_thread_status
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
                    if (cieExtractsDal.isInOrgCohort(org, "Reconsented")) {
                        OrgProcess orgProcess = cieExtractsDal.getOrgProcess(org);

                        java.sql.Timestamp lastProcessedDateTime = orgProcess.getStartDatetime();
                        java.sql.Timestamp startDateTime= new java.sql.Timestamp(System.currentTimeMillis());

                        cieExtractsDal.newOrgProcess(orgsThreadId, org, startDateTime, null, "Started");
                        try {
                            String orgUUID = cieExtractsDal.checkReferenceExistsForOrg(org);
                            if (orgUUID.isEmpty()) {
                                LOG.debug("Org reference cannot be empty for a DeltaRC reference, throw error.");
                            } else {
                                OrganizationFull organizationFull = subscriberDal.getOrganizationFull(org);

                                //Delta events deletion process, priorities are different
                                DeltaOrganizationThread.deltaDeleteProcess(resourceUtil, cieExtractsDal, subscriberDal, org, lastProcessedDateTime, configurations, processType, configId);

                                //Delta events create/update process,priorities are different
                                DeltaOrganizationThread.deltaProcess(resourceUtil, cieExtractsDal, subscriberDal, org, lastProcessedDateTime, orgUUID, organizationFull, configurations, processType, configId);

                            }

                            cieExtractsDal.updateOrgProcessStatus(org, "Completed");
                            cieExtractsDal.updateOrgCohort(configId, org, "Active");
                        } catch (Exception e) {
                            cieExtractsDal.updateOrgProcessStatus(org, "Errored");
                            e.printStackTrace();
                            throw new Exception(e);
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