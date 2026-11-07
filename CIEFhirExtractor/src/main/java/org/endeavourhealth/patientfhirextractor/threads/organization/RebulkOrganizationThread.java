package org.endeavourhealth.patientfhirextractor.threads.organization;

import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.dal.CIEReference;
import org.endeavourhealth.patientfhirextractor.dal.SubscriberJdbcDAL;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;
import org.endeavourhealth.patientfhirextractor.helper.ResourceUtil;
import org.endeavourhealth.patientfhirextractor.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RebulkOrganizationThread extends OrganizationThread {
    private static final Logger LOG = LoggerFactory.getLogger(RebulkOrganizationThread.class);
    private Integer subscriberThreadId;
    private int configId;
    private String configName;
    private String processType;
    private String rebulktype;
    private CIEExtractorConfig configurations;
    private List<String> odsCodes;


    public RebulkOrganizationThread(Integer subscriberThreadId, String configName, List<String> odsCodes, String processType, CIEExtractorConfig cieExtractorConfig, int configId, String rebulktype) {
        this.subscriberThreadId = subscriberThreadId;
        this.configName = configName;
        this.odsCodes = odsCodes;
        this.processType = processType;
        this.configurations = cieExtractorConfig;
        this.configId = configId;
        this.rebulktype=rebulktype;
    }

    public Integer getOrgsThreadId() {
        return orgsThreadId;
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

                    if (cieExtractsDal.isInOrgCohort(org, "Active")) {
                        OrgProcess orgProcess = cieExtractsDal.getOrgProcess(org);

                        if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                            throw new Exception("User Interrupted");

                        }/* java.sql.Timestamp startDateTime;

                        if (orgProcess.getStatus().equalsIgnoreCase("Completed")) {
                            startDateTime = orgProcess.getEndDatetime();
                        } else {
                            startDateTime = orgProcess.getStartDatetime();
                        }*/
                        java.sql.Timestamp startDateTime= new java.sql.Timestamp(System.currentTimeMillis());
                        cieExtractsDal.newOrgProcess(orgsThreadId, org, startDateTime, null, "Started");
                        OrganizationFull organizationFull = subscriberDal.getOrganizationFull(org);
                        try {
                            String orgUUID = cieExtractsDal.checkReferenceExistsForOrg(org);
                            if(rebulktype.equalsIgnoreCase("ALL")) {
                                LOG.info("Rebulk Initiated for resourceType:ALL");

                                if (!orgUUID.isEmpty()) {
                                    org.hl7.fhir.dstu3.model.Organization organizationResource = null;
                                    if (organizationFull.getId() > 0) {
                                        organizationResource = resourceUtil.processRBOrganization(organizationFull, cieExtractsDal, subscriberDal, orgUUID);
                                    } else {
                                        throw new Exception( org + "does not exist in db");
                                    }
                                    resourceUtil.processPatientData(org, organizationFull.getId(), cieExtractsDal, subscriberDal, organizationResource.getId(), processType, configurations,false,configId);
                                }
                                else
                                {
                                    throw new Exception( org + "reference does not exist in db");
                                }
                            }
                            else if(rebulktype.equalsIgnoreCase("PATIENT"))
                            {
                                List<CIEReference> cieRefList=cieExtractsDal.getReferences(ResourceConstants.Patient,org,configId);
                                for(CIEReference cieRef:cieRefList) {
                                        {
                                        resourceUtil.processPatient(subscriberDal, cieRef.getLocal_id(), orgUUID, processType, cieExtractsDal, org,cieRef.getRefUUID(),configId);

                                    }
                                }
                            }
                            else if(rebulktype.equalsIgnoreCase("MEDICATIONSTATEMENT"))
                            {
                                List<CIEReference> cieRefList=cieExtractsDal.getReferences(ResourceConstants.MedicationStatement,org,configId);
                                for(CIEReference cieRef:cieRefList) {
                                    {
                                        MedicationStatementFull medicationStatement = subscriberDal.getMedicationStatementFull( cieRef.getLocal_id(),organizationFull.getId());
                                        resourceUtil.processMedication(cieExtractsDal, medicationStatement,org,processType,orgUUID,cieRef.getRefUUID(),configId);
                                    }
                                }

                            }
                            else if(rebulktype.equalsIgnoreCase("APPOINTMENT"))
                            {
                                List<CIEReference> cieRefList=cieExtractsDal.getReferences(ResourceConstants.Appointment,org,configId);
                                for(CIEReference cieRef:cieRefList) {
                                    {
                                        AppointmentFull appointment;
                                        if (org.equals(ResourceConstants.CW_ODS_CODE)) {
                                            appointment = subscriberDal.getAppointmentFullForCW(cieRef.getLocal_id(), organizationFull.getId());
                                        } else {
                                            appointment = subscriberDal.getAppointmentFull(cieRef.getLocal_id(), organizationFull.getId());
                                        }
                                        if(appointment != null ) {
                                            resourceUtil.processAppointment(subscriberDal, appointment, organizationFull.getId(), orgUUID, processType, cieExtractsDal, org, cieRef.getRefUUID(), configId);
                                        }

                                    }
                                }

                            }
                            else if(rebulktype.equalsIgnoreCase("CONDITION"))
                            {
                                List<CIEReference> cieRefList=cieExtractsDal.getReferences(ResourceConstants.Condition,org,configId);
                                for(CIEReference cieRef:cieRefList) {
                                    {
                                        ConditionFull condition = subscriberDal.getConditionFull(cieRef.getLocal_id(),organizationFull.getId());
                                        resourceUtil.processCondition(condition, cieExtractsDal, org, subscriberDal, processType,cieRef.getRefUUID(),configId);
                                    }
                                }

                            }
                            else if(rebulktype.equalsIgnoreCase("PRACTITIONER"))
                            {
                                List<CIEReference> cieRefList=cieExtractsDal.getReferences(ResourceConstants.Practitioner,org,configId);
                                for(CIEReference cieRef:cieRefList) {
                                    {
                                        resourceUtil.processPractitioner(org, organizationFull.getId(), cieRef.getLocal_id(), orgUUID, cieExtractsDal, subscriberDal,processType,true,configId);
                                    }
                                }

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

