package org.endeavourhealth.patientfhirextractor.helper;
import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.endeavourhealth.common.utility.SlackHelper;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.dal.CIEReference;
import org.endeavourhealth.patientfhirextractor.dal.SubscriberJdbcDAL;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;
import org.endeavourhealth.patientfhirextractor.models.*;
import org.endeavourhealth.patientfhirextractor.resources.Appointment;
import org.endeavourhealth.patientfhirextractor.resources.Condition;
import org.endeavourhealth.patientfhirextractor.resources.MedicationStatement;
import org.endeavourhealth.patientfhirextractor.resources.Organization;
import org.endeavourhealth.patientfhirextractor.resources.Patient;
import org.endeavourhealth.patientfhirextractor.resources.Practitioner;
import org.endeavourhealth.patientfhirextractor.resources.PractitionerRole;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.endeavourhealth.patientfhirextractor.threads.organization.BulkOrganizationThread.setIsOrgErrored;


public  class ResourceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUtil.class);
    private static FhirContext ctx = null;
    private  FhirContext getFhirContext()
    {
        if(null==ctx)
            ctx= FhirContext.forDstu3();
        return ctx;

    }

    public void processPatientData(String orgName, long orgId, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, String orgUUID ,String processType, CIEExtractorConfig configurations,boolean isBulk,int configID) throws Exception {
        List<Long> patientIds= subscriberDal.getPatientIds(orgId);
        String patientReferenceUUID = "";
        boolean isPatientProcessErr = false;
        for (Long patientId:patientIds) {

            if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                throw new Exception("User Interrupted");

            }
            try {
                patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(patientId), ResourceConstants.Patient, orgName, configID);
                if ((patientReferenceUUID.isEmpty() && isBulk) || !isBulk) {
                    processPatient(subscriberDal, patientId, orgUUID, processType, cieExtractsDal, orgName, patientReferenceUUID, configID);
                }

                for (JsonNode resource : configurations.getBulkPriorities()) {
                    switch (resource.asText()) {
                        case ResourceConstants.Condition:
                            processConditions(orgName, orgId, orgUUID, patientId, cieExtractsDal, subscriberDal, processType, isBulk, configID);
                            continue;
                        case ResourceConstants.MedicationStatement:
                            processMedicationAndStatements(orgName, orgId, orgUUID, patientId, cieExtractsDal, subscriberDal, processType, isBulk, configID);
                            continue;
                        case ResourceConstants.Appointment:
                            processAppointments(orgName, orgId, orgUUID, cieExtractsDal, patientId, subscriberDal, processType, isBulk, configID);
                            continue;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                isPatientProcessErr =  true;
                LOG.error("Patient_Id:"+patientId+" got Runtime Exception while running Bulk process:"+e.getMessage());
            }
        }
        if(isPatientProcessErr){
            throw new Exception("Bulk process throws Runtime Exception while processing patients");
        }
    }
    public void processConditions(String orgName, long orgId, String orgUUID, Long patientId, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal,String processType,boolean isBulk,int configID) throws Exception {

        List<ConditionFull> conditions= subscriberDal.getConditionFullList(orgId,patientId);
        for (ConditionFull condition:conditions) {
            String patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(condition.getPatientId()), ResourceConstants.Patient,orgName,configID);
            String conditionReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(condition.getId()), ResourceConstants.Condition,orgName,configID);
            if(!patientReferenceUUID.isEmpty() && ((conditionReferenceUUID.isEmpty() && isBulk)|| !isBulk)) {
                processCondition(condition, cieExtractsDal, orgName, subscriberDal, processType,conditionReferenceUUID,configID);
            }
        }
    }
    public void processMedicationAndStatements(String orgName, long orgId, String orgUUID, Long patientId, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal,String processType,boolean isBulk,int configID) throws Exception {
        List<MedicationStatementFull> medicationStatementList = subscriberDal.getMedicationStatementFullList(orgId,patientId);
        if (medicationStatementList != null || medicationStatementList.size() > 0) {
            for (MedicationStatementFull medicationStatementFull : medicationStatementList) {
                String patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(medicationStatementFull.getPatientId()), ResourceConstants.Patient,orgName,configID);
                String medicationStatementReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(medicationStatementFull.getId()), ResourceConstants.MedicationStatement,orgName,configID);
                if(!patientReferenceUUID.isEmpty() && ((medicationStatementReferenceUUID.isEmpty() && isBulk) || !isBulk)) {
                    processMedication(cieExtractsDal, medicationStatementFull,orgName,processType,orgUUID,medicationStatementReferenceUUID,configID);
                }
            }
        }
    }
    public void processAppointments(String orgName, long orgId, String orgUUID, CIEJdbcDAL cieExtractsDal, Long patientId, SubscriberJdbcDAL subscriberDal,String  processType,boolean isBulk,int configID) throws Exception {
        List<AppointmentFull> appointmentList;
        if (orgName.equals(ResourceConstants.CW_ODS_CODE)) {
            appointmentList = subscriberDal.getAppointmentFullListForCW(orgId, patientId);
        } else {
            appointmentList = subscriberDal.getAppointmentFullList(orgId, patientId);
        }

        if (appointmentList != null || appointmentList.size() > 0) {
            for (AppointmentFull appointmentFull:appointmentList) {
                String patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(appointmentFull.getPatientId()), ResourceConstants.Patient,orgName,configID);
                String appointmentReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(appointmentFull.getId()), ResourceConstants.Appointment,orgName,configID);
                if(!patientReferenceUUID.isEmpty() && ((appointmentReferenceUUID.isEmpty() && isBulk) || !isBulk)) {
                    processAppointment(subscriberDal,appointmentFull,orgId,orgUUID,processType,cieExtractsDal,orgName,appointmentReferenceUUID,configID);
                }
            }
        }
    }

    public void processDeltaPatients(String orgName, long orgId, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, java.sql.Timestamp lastProcessedDateTime , String processType,String orgUUID,int configId) throws Exception {
        List<EventLog> patientEventLogs= subscriberDal.getDeltaPatientIds(orgId,lastProcessedDateTime);

        boolean isDeltaPatientProcErr = false;
        for (EventLog patientEvent:patientEventLogs) {
            if(CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                throw new Exception("User Interrupted for "+processType);

            }
            long patientId = patientEvent.getRecordId();
            try {
                String patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(patientEvent.getRecordId()), ResourceConstants.Patient, orgName, configId);
                processPatient(subscriberDal, patientEvent.getRecordId(), orgUUID, processType, cieExtractsDal, orgName, patientReferenceUUID, configId);
            }catch(Exception e){
                e.printStackTrace();
                isDeltaPatientProcErr = true;
                String msg="Patient_Id:"+patientId+" got Runtime Exception while running Delta process:"+e.getMessage();
                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg );
                LOG.error(msg);
                java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                AuditLog auditLog = new AuditLog(ResourceConstants.Patient, patientEvent.getRecordId(), "", configId,
                        msg,updatedDateTime, orgName, "Y","", e.getMessage());
                cieExtractsDal.createAuditEntry(auditLog);
            }

        }
        if(isDeltaPatientProcErr){
            throw new Exception("Delta Process failed with RuntimeException,while processing Patient");
        }

    }
    public void processDeltaConditions(String orgName, long orgId, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, Timestamp lastProcessedDateTime ,String processType,int configId) throws Exception {
        List<EventLog> conditionEventLogs= subscriberDal.getDeltaConditionsIds(orgId,lastProcessedDateTime);
        String resourceType=ResourceConstants.Condition;

        boolean isDeltaCondProcErr = false;
        for (EventLog event:conditionEventLogs) {
            if(CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                throw new Exception("User Interrupted for "+processType);

            }
            ConditionFull conditionConcept = null;
            try {

                conditionConcept = subscriberDal.getCondition(event.getRecordId(), orgId);
                Long patientId = conditionConcept.getPatientId();
                Long conceptCode = conditionConcept.getNonCoreConceptId();
                String resourceReferenceUUID = "";
                CIEReference activeCondition = cieExtractsDal.conditionResourceList(patientId, conceptCode,"active");

                ConditionFull condition;
                if(activeCondition.getRefUUID()==null) {
                    condition = subscriberDal.getConditionFull(event.getRecordId(), orgId);
                } else {
                    condition = subscriberDal.getConditionFull(activeCondition.getLocal_id(), orgId);
                    if (null != conditionConcept.getEndDate()) {
                        condition.setEndDate(conditionConcept.getEndDate());
                        condition.setClinicalStatus("resolved");
                    }
                    resourceReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(activeCondition.getLocal_id()), resourceType, orgName, configId);
                }
                processCondition(condition, cieExtractsDal, orgName, subscriberDal, processType, resourceReferenceUUID, configId);

            }catch(Exception e){
                e.printStackTrace();
                String msg="Condition_Id:"+event.getRecordId()+" got Runtime Exception while running Delta process:"+e.getMessage();
                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg );
                LOG.error(msg);
                java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                AuditLog auditLog = new AuditLog(ResourceConstants.Condition, event.getRecordId(), "", configId,
                        msg,updatedDateTime, orgName, "Y","", e.getMessage());
                cieExtractsDal.createAuditEntry(auditLog);
            }
        }
        if(isDeltaCondProcErr){
            throw new Exception("Delta Process failed with RuntimeException,while processing Condition");
        }
    }
    public void processDeltaMedicationAndStatements(String orgName, long orgId, String orgUUID, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, java.sql.Timestamp lastProcessedDateTime ,String processType,int configId) throws Exception {
        List<EventLog> medicationStatementEventLogs= subscriberDal.getDeltaMedicationStatementIds(orgId,lastProcessedDateTime);
        String resourceType=ResourceConstants.MedicationStatement;

        boolean isDeltaMediProcErr = false;
        for (EventLog event:medicationStatementEventLogs) {
            if(CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                throw new Exception("User Interrupted for "+processType);

            }
            try {
                String resourceReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(event.getRecordId()), resourceType, orgName, configId);
                MedicationStatementFull medicationStatement = subscriberDal.getMedicationStatementFull(event.getRecordId(), orgId);
                if(medicationStatement != null ) {
                    processMedication(cieExtractsDal, medicationStatement, orgName, processType, orgUUID, resourceReferenceUUID, configId);
                }
            }catch(Exception e){
                e.printStackTrace();
                String msg="Medication_Id:"+event.getRecordId()+" got Runtime Exception while running Delta process:"+e.getMessage();
                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg );
                LOG.error(msg);
                java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                AuditLog auditLog = new AuditLog(ResourceConstants.MedicationStatement, event.getRecordId(), "", configId,
                        msg,updatedDateTime, orgName, "Y","", e.getMessage());
                cieExtractsDal.createAuditEntry(auditLog);
            }
        }

        if(isDeltaMediProcErr){
            throw new Exception("Delta Process failed with RuntimeException,while processing Medication");
        }
    }

    public void processMedication(CIEJdbcDAL cieExtractsDal,MedicationStatementFull medicationStatement,String orgName,String processType,String orgUUID,String mUUID,int configId) throws Exception {
        String patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(medicationStatement.getPatientId()), ResourceConstants.Patient,orgName,configId);
        if (!patientReferenceUUID.isEmpty()) {
            String medicationReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(medicationStatement.getCode(), ResourceConstants.Medication, orgName, configId);
            org.hl7.fhir.dstu3.model.MedicationStatement medicationStatementResource = null;
            medicationStatementResource = MedicationStatement.getMedicationStatementResource(medicationStatement);
            medicationStatementResource.setSubject(new Reference(ResourceConstants.referenceString(patientReferenceUUID, ResourceConstants.Patient)));

            if (medicationReferenceUUID.isEmpty()) {
                Medication medicationResource = processNewMedication(medicationStatement, orgUUID, orgName, cieExtractsDal, processType, configId);
                medicationStatementResource.setMedication(new Reference(ResourceConstants.referenceString(medicationResource.getId(), ResourceConstants.Medication)));
            } else
                medicationStatementResource.setMedication(new Reference(ResourceConstants.referenceString(medicationReferenceUUID, ResourceConstants.Medication)));

            String httpmethod = "POST";
            String result = "";
            if (!mUUID.isEmpty()) {
                httpmethod = "PUT";
                medicationStatementResource.setId(mUUID);

            }

            String fhirStr = getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(medicationStatementResource);
            result = CIESenderUtil.sendResource(fhirStr, ResourceConstants.MedicationStatement, httpmethod, cieExtractsDal,null,null);
            if (result.equals("success")) {
                if (httpmethod.equalsIgnoreCase("PUT"))
                    cieExtractsDal.updateReference(medicationStatementResource.getId(), ResourceConstants.MedicationStatement);
                else
                    cieExtractsDal.saveReference(medicationStatementResource.getId(), medicationStatement.getId(), orgName, ResourceConstants.MedicationStatement, configId);
            } else {
                java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());

                AuditLog auditLog = new AuditLog(ResourceConstants.MedicationStatement, medicationStatement.getId(),
                        medicationStatementResource.getId(), configId, fhirStr,
                        updatedTime, orgName, "Y",
                        httpmethod, result);

                cieExtractsDal.createAuditEntry(auditLog);

            }
        }

    }
    public void processDeltaAppointments(String orgName, long orgId, String orgUUID, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, java.sql.Timestamp lastProcessedDateTime ,String processType,int configId) throws Exception {
        List<EventLog> appointmentEventLogs= subscriberDal.getDeltaAppointmentsIds(orgId,lastProcessedDateTime);
        String resourceType=ResourceConstants.Appointment;

        boolean isDeltaAppProcErr = false;
        for (EventLog event:appointmentEventLogs) {
            if(CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                throw new Exception("User Interrupted for "+processType);

            }
            try {
                AppointmentFull appointment;
                if (orgName.equals(ResourceConstants.CW_ODS_CODE)) {
                    appointment = subscriberDal.getAppointmentFullForCW(event.getRecordId(), orgId);
                } else {
                    appointment = subscriberDal.getAppointmentFull(event.getRecordId(), orgId);
                }
                String resourceReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(event.getRecordId()), resourceType, orgName, configId);
                if(appointment != null ){
                    processAppointment(subscriberDal, appointment, orgId, orgUUID, processType, cieExtractsDal, orgName, resourceReferenceUUID, configId);
                }
            }catch(Exception e){
                e.printStackTrace();
                String msg="Appointment_Id:"+event.getRecordId()+" got Runtime Exception while running Delta process: "+e.getMessage();
                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg );
                LOG.error(msg);
                java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                AuditLog auditLog = new AuditLog(ResourceConstants.Appointment, event.getRecordId(), "", configId,
                        msg,updatedDateTime, orgName, "Y","", e.getMessage());
                cieExtractsDal.createAuditEntry(auditLog);
                isDeltaAppProcErr = true;
            }
        }

        if (isDeltaAppProcErr){
            throw new Exception("Delta Process failed with RuntimeException,while processing Appointments");
        }
    }


    public void processDeltaDeleteAppointments(String orgName,CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, java.sql.Timestamp lastProcessedDateTime ,String processType,int configId) throws Exception {
        List<EventLog> appointmentEventLogs= subscriberDal.getDeltaDeleteEvents(lastProcessedDateTime,18);
        String resourceType=ResourceConstants.Appointment;
        boolean isDeltaDelProcErr = false;
        for(EventLog event : appointmentEventLogs){
            if(event.getChangeType()==2){
                try {
                    deleteResource(resourceType, String.valueOf(event.getRecordId()), cieExtractsDal, orgName, processType, configId);
                }catch(Exception e){
                    e.printStackTrace();
                     isDeltaDelProcErr = true;
                    String msg="Appointment_Id:"+event.getRecordId()+" got Runtime Exception while running Delta Delete process:"+e.getMessage();
                    SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg );
                    LOG.error(msg);
                    java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                    AuditLog auditLog = new AuditLog(ResourceConstants.Appointment, event.getRecordId(), "", configId,
                            msg,updatedDateTime, orgName, "Y","", e.getMessage());
                    cieExtractsDal.createAuditEntry(auditLog);
                }
              }
            }
        if(isDeltaDelProcErr){
            throw new Exception("Appointment Processing failed with RuntimeException,while running Delta Delete process.");
          }
        }

    public void processDeltaDeletePatients(String orgName,CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, java.sql.Timestamp lastProcessedDateTime ,String processType,int configId) throws Exception {
        List<EventLog> patientEventLogs = subscriberDal.getDeltaDeleteEvents(lastProcessedDateTime, 2);
        boolean isDeltaDelProcErr = false;
        for (EventLog patientEvent : patientEventLogs) {
            if (patientEvent.getChangeType() == 2) {
                try {
                    deleteResource(ResourceConstants.Patient, String.valueOf(patientEvent.getRecordId()), cieExtractsDal, orgName, processType, configId);
                } catch (Exception e) {
                    e.printStackTrace();
                    isDeltaDelProcErr = true;
                    String msg = "Patient_Id:" + patientEvent.getRecordId() + " got Runtime Exception while running Delta Delete process:" + e.getMessage();
                    SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg);
                    LOG.error(msg);
                    java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                    AuditLog auditLog = new AuditLog(ResourceConstants.Patient, patientEvent.getRecordId(), "", configId,
                            msg, updatedDateTime, orgName, "Y", "", e.getMessage());
                    cieExtractsDal.createAuditEntry(auditLog);
                }

            }
        }

        if (isDeltaDelProcErr) {
            throw new Exception("Patience Processing failed with RuntimeException,while running Delta Delete process");
        }
    }

    public void processDeltaDeleteConditions(String orgName,CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, java.sql.Timestamp lastProcessedDateTime ,String processType,int configId) throws Exception {
        List<EventLog> conditionEventLogs = subscriberDal.getDeltaDeleteEvents(lastProcessedDateTime, 11);
        String resourceType = ResourceConstants.Condition;
        boolean isDeltaDelProcErr = false;
        for (EventLog event : conditionEventLogs) {
            if (event.getChangeType() == 2) {
                try {
                    deleteResource(resourceType, String.valueOf(event.getRecordId()), cieExtractsDal, orgName, processType, configId);
                } catch (Exception e) {
                    e.printStackTrace();
                    isDeltaDelProcErr = true;
                    String msg = "Condition_Id:" + event.getRecordId() + " got Runtime Exception while running Delta Delete process:" + e.getMessage();
                    SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg);
                    LOG.error(msg);
                    java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                    AuditLog auditLog = new AuditLog(ResourceConstants.Condition, event.getRecordId(), "", configId,
                            msg, updatedDateTime, orgName, "Y", "", e.getMessage());
                    cieExtractsDal.createAuditEntry(auditLog);
                }
            }
        }
        if (isDeltaDelProcErr) {
            throw new Exception("Condition Processing failed with RuntimeException,while running Delta Delete process.");
        }
    }

    public void processDeltaDeleteMedicationAndStatements(String orgName,CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal, java.sql.Timestamp lastProcessedDateTime ,String processType,int configId) throws Exception {
        List<EventLog> medicationStatementEventLogs= subscriberDal.getDeltaDeleteEvents(lastProcessedDateTime,10);
        String resourceType=ResourceConstants.MedicationStatement;
        boolean isDeltaDelProcErr = false;
        for(EventLog event : medicationStatementEventLogs){
            if(event.getChangeType()==2){
                try {
                    deleteResource(resourceType, String.valueOf(event.getRecordId()), cieExtractsDal, orgName, processType, configId);
                }catch(Exception e){
                    e.printStackTrace();
                    isDeltaDelProcErr = true;
                    String msg="Medication_Id:"+event.getRecordId()+" got Runtime Exception while running Delta Delete process:"+e.getMessage();
                    SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg );
                    LOG.error(msg);
                    java.sql.Timestamp updatedDateTime = new java.sql.Timestamp(System.currentTimeMillis());
                    AuditLog auditLog = new AuditLog(ResourceConstants.MedicationStatement, event.getRecordId(), "", configId,
                            msg,updatedDateTime, orgName, "Y","", e.getMessage());
                    cieExtractsDal.createAuditEntry(auditLog);
                }
            }
        }
        if(isDeltaDelProcErr){
            throw new Exception("Medication Processing failed with RuntimeException,while running Delta Delete process.");
        }
    }


    public Medication processNewMedication(MedicationStatementFull medicationStatement, String orgUUID, String orgName, CIEJdbcDAL cieExtractsDal,String processType,int configID) throws Exception {
        Medication medicationResource = null;
        medicationResource = MedicationStatement.getMedicationResource(medicationStatement);
        medicationResource.getMeta().addTag(new Coding().setCode(orgUUID));
        if(CIEFhirExtractorEndpoint.isInterrupted(processType))
        {
            throw new Exception("User Interrupted for "+processType);

        }
        String fhirStr= getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(medicationResource);
        String result = CIESenderUtil.sendResource(fhirStr,ResourceConstants.Medication,"POST",cieExtractsDal,null,null);
        if  (result.equals("success")) {
            cieExtractsDal.saveMedicationReference(medicationResource.getId(), medicationStatement.getCode(),orgName,configID);
        }
        else{
            java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());

            AuditLog auditLog = new AuditLog(ResourceConstants.Medication, medicationStatement.getId(),
                    medicationResource.getId(), configID, fhirStr,
                    updatedTime, orgName,  "Y",
                    "POST",result) ;

            cieExtractsDal.createAuditEntry(auditLog);
        }

        return medicationResource;

    }
    private void deleteResource(String resourceType, String recordId, CIEJdbcDAL cieExtractsDal,String orgName,String processType,int configID) throws Exception {
        if(CIEFhirExtractorEndpoint.isInterrupted(processType))
        {
            throw new Exception("User Interrupted for "+processType);

        }
        String resourceReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(recordId,resourceType,orgName,configID);
        String result=null;
        if (!resourceReferenceUUID.isEmpty()) {
            result = CIESenderUtil.deleteResource(resourceReferenceUUID, resourceType);

            if(result.equalsIgnoreCase("success")) {
                cieExtractsDal.removeReferenceForResource(recordId, resourceType, orgName);
            }
            else{
                java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());

                AuditLog auditLog = new AuditLog(resourceType, Long.parseLong(recordId),
                        resourceReferenceUUID, configID, null,
                        updatedTime, orgName,  "N",
                        "DELETE",result) ;

                cieExtractsDal.createAuditEntry(auditLog);
            }
        }
    }
    public org.hl7.fhir.dstu3.model.Organization processRBOrganization( OrganizationFull organizationFull, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberJdbcDAL,String uuid) throws Exception {
        org.hl7.fhir.dstu3.model.Organization organizationResource;
        if(organizationFull.getParentOrganizationId() > 0 && organizationFull.getParentOrganizationId() != organizationFull.getId()){
            OrganizationFull parentOrganizationFull = subscriberJdbcDAL.getOrganizationFull(organizationFull.getParentOrganizationId());
            String porgUUID = cieExtractsDal.checkReferenceExistsForOrg(parentOrganizationFull.getOdsCode());
            if(parentOrganizationFull.getId()>0 ){
                org.hl7.fhir.dstu3.model.Organization parentOrgResource = processRBOrganization(parentOrganizationFull,cieExtractsDal,subscriberJdbcDAL,porgUUID);
                organizationFull.setParentOrganizationUUID(ResourceConstants.referenceString(parentOrgResource.getId(),ResourceConstants.Organization));
            }
        }
        organizationResource = Organization.getOrganizationResource(organizationFull,uuid);
        String fhirStr = getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(organizationResource);
        String result = CIESenderUtil.sendResource(fhirStr,ResourceConstants.Organization,"PUT",cieExtractsDal,null,null);

        if ( result.equals("success")) {
            cieExtractsDal.updateReference(organizationResource.getId(), ResourceConstants.Organization);
        }
        return organizationResource;
    }

    public void processPatient(SubscriberJdbcDAL subscriberDal, Long patientId,String orgUUID,String processType,CIEJdbcDAL cieExtractsDal,String orgName,String pUUID,int configId) throws Exception {

        PatientFull patientFull = subscriberDal.getPatientFull(patientId);
        List<TelecomFull> telecomFullList = subscriberDal.getTelecomFull(patientFull.getId());
        patientFull.setTelecomFullList(telecomFullList);

        org.hl7.fhir.dstu3.model.Patient patientResource = Patient.getPatientResource(patientFull, subscriberDal,cieExtractsDal,configId);
        patientResource.setManagingOrganization(new Reference(ResourceConstants.referenceString(orgUUID, ResourceConstants.Organization)));
        String httpmethod = "POST";
        String result = "";
        if (!pUUID.isEmpty()) {
            httpmethod = "PUT";
            patientResource.setId(pUUID);
        }
        String fhirStr = getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(patientResource);

        if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
            throw new Exception("User Interrupted");
        }

     /*   if (cieExtractsDal.checkNewPatientResource(patientFull.getNhsNumber(), patientFull.getOds_code())) {
            OrgData orgList= cieExtractsDal.getPublicOrgId(patientFull.getOds_code());
            String response = CIESenderUtil.sendResource(fhirStr, ResourceConstants.NewPatient, "POST", cieExtractsDal,orgList.getPublicOrg(),orgList.getRegAuthAccess());
            if (response.equals("success")) {
                cieExtractsDal.saveNewPatientReference(patientFull.getNhsNumber(), patientFull.getOds_code());
            } else {
                java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
                AuditLog auditLog = new AuditLog(ResourceConstants.NewPatient, patientFull.getId(),
                        patientResource.getId(), configId, fhirStr,
                        updatedTime, orgName, "Y", httpmethod, response);

                cieExtractsDal.createAuditEntry(auditLog);
            }
        }*/
        result = CIESenderUtil.sendResource(fhirStr, ResourceConstants.Patient, httpmethod, cieExtractsDal,null,null);

        if (result.equals("success")) {
            if (httpmethod.equalsIgnoreCase("PUT"))
                cieExtractsDal.updateReference(patientResource.getId(), ResourceConstants.Patient);
            else
                cieExtractsDal.saveReference(patientResource.getId(), patientFull.getId(), orgName, ResourceConstants.Patient, configId);
        } else {
            java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
            AuditLog auditLog = new AuditLog(ResourceConstants.Patient, patientFull.getId(),
                    patientResource.getId(), configId, fhirStr,
                    updatedTime, orgName, "Y", httpmethod, result);

            cieExtractsDal.createAuditEntry(auditLog);

            setIsOrgErrored(true);

        }
    }
    public void processCondition(ConditionFull condition,CIEJdbcDAL cieExtractsDal,String orgName,SubscriberJdbcDAL subscriberDal,String processType,String cUUID,int configId) throws Exception {
        Long patientId=condition.getPatientId();
        String patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(patientId), ResourceConstants.Patient, orgName,configId);

        if (!patientReferenceUUID.isEmpty()) {
            org.hl7.fhir.dstu3.model.Condition conditionFhirObj = Condition.getConditionResource(condition, subscriberDal);
            conditionFhirObj.setSubject(new Reference(ResourceConstants.referenceString(patientReferenceUUID, ResourceConstants.Patient)));

            String httpmethod = "POST";
            String result = "";
            if (!cUUID.isEmpty()) {
                httpmethod = "PUT";
                conditionFhirObj.setId(cUUID);
            }
            String fhirStr = getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(conditionFhirObj);

            if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                throw new Exception("User Interrupted");

            }
            result = CIESenderUtil.sendResource(fhirStr, ResourceConstants.Condition, httpmethod,cieExtractsDal,null,null);

            String status=condition.getClinicalStatus();
            if (result.equals("success")) {
                if (httpmethod.equalsIgnoreCase("PUT"))
                cieExtractsDal.updateConditionReference(conditionFhirObj.getId(),status,patientId,condition.getNonCoreConceptId());
                else
                cieExtractsDal.saveConditionReference(conditionFhirObj.getId(), condition.getId(), orgName,status, patientId,condition.getNonCoreConceptId(),configId);
            } else {
                java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
                AuditLog auditLog = new AuditLog(ResourceConstants.Condition, condition.getId(),
                        conditionFhirObj.getId(), configId, fhirStr,
                        updatedTime, orgName, "Y",
                        httpmethod, result);

                cieExtractsDal.createAuditEntry(auditLog);
            }
        }
    }
    public void processAppointment(SubscriberJdbcDAL subscriberDal, AppointmentFull appointmentfull,long orgId,String orgUUID,String processType, CIEJdbcDAL cieExtractsDal,String orgName,String aUUID,int configID) throws Exception {
        java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
        if(null!=appointmentfull.getStartDate()) {
            SpecialityFull spl = null;
            if (appointmentfull.getType() != null) {
                spl = cieExtractsDal.getSpeciality(appointmentfull.getType(), orgName);
            }
            if (null != spl) {
                processPractitioner(orgName, orgId, appointmentfull.getPractitionerId(), orgUUID, cieExtractsDal, subscriberDal, processType, false, configID);
                String patientReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(appointmentfull.getPatientId()), ResourceConstants.Patient, orgName, configID);
                String practitionerReferenceUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(appointmentfull.getPractitionerId()), ResourceConstants.Practitioner, orgName, configID);

                if (!patientReferenceUUID.isEmpty()) {
                    org.hl7.fhir.dstu3.model.Appointment appointmentResource = Appointment.getAppointmentResource(appointmentfull);
                    String practStatus = appointmentfull.getStatus().toLowerCase().replaceAll(" ", "").equalsIgnoreCase("cancelled") ? "declined" : "accepted";
                    appointmentResource.addParticipant().setActor(new Reference(ResourceConstants.referenceString(patientReferenceUUID, ResourceConstants.Patient)))
                            .setStatus((new org.hl7.fhir.dstu3.model.Appointment.ParticipationStatusEnumFactory().fromCode(practStatus)));
                    if (!practitionerReferenceUUID.isEmpty()) {
                        appointmentResource.addParticipant().setActor(new Reference(ResourceConstants.referenceString(practitionerReferenceUUID, ResourceConstants.Practitioner)))
                                .setStatus((new org.hl7.fhir.dstu3.model.Appointment.ParticipationStatusEnumFactory().fromCode(practStatus)));
                    }

                    if (!Strings.isNullOrEmpty(appointmentfull.getLocationType())) {
                        appointmentResource.addParticipant().setActor(new Reference().setDisplay(appointmentfull.getLocationType()))
                                .setStatus((new org.hl7.fhir.dstu3.model.Appointment.ParticipationStatusEnumFactory().fromCode(practStatus)))
                                .setExtension(singletonList(new Extension()
                                        .setUrl("http://fhir.patientsknowbest.com/structuredefinition/appointment-actor-type")
                                        .setValue(new StringType(ResourceConstants.Location)))
                                );
                    } else {
                        if (!Strings.isNullOrEmpty(appointmentfull.getLocation())) {
                            appointmentResource.addParticipant().setActor(new Reference().setDisplay(appointmentfull.getLocation()))
                                    .setStatus((new org.hl7.fhir.dstu3.model.Appointment.ParticipationStatusEnumFactory().fromCode(practStatus)))
                                    .setExtension(singletonList(new Extension()
                                            .setUrl("http://fhir.patientsknowbest.com/structuredefinition/appointment-actor-type")
                                            .setValue(new StringType(ResourceConstants.Location)))
                                    );
                        }
                    }

                    Coding code = new Coding();
                    code.setSystem("http://hl7.org/fhir/stu3/valueset-c80-practice-codes.html");
                    code.setCode(spl.getCode());
                    code.setDisplay(spl.getSpeciality());
                    appointmentResource.addSpecialty().addCoding(code);


                    String httpmethod = "POST";
                    String result = "";
                    if (!aUUID.isEmpty()) {
                        appointmentResource.setId(aUUID);
                        httpmethod = "PUT";
                    }
                    String fhirStr = getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(appointmentResource);

                    if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                        throw new Exception("User Interrupted");
                    }

                    result = CIESenderUtil.sendResource(fhirStr, ResourceConstants.Appointment, httpmethod, cieExtractsDal,null,null);

                    if (result.equals("success")) {
                        if (httpmethod.equalsIgnoreCase("PUT"))
                            cieExtractsDal.updateReference(appointmentResource.getId(), ResourceConstants.Appointment);
                        else
                            cieExtractsDal.saveReference(appointmentResource.getId(), appointmentfull.getId(), orgName, ResourceConstants.Appointment, configID);
                    } else {
                        updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
                        AuditLog auditLog = new AuditLog(ResourceConstants.Appointment, appointmentfull.getId(),
                                appointmentResource.getId(), configID, fhirStr,
                                updatedTime, orgName, "Y",
                                httpmethod, result);

                        cieExtractsDal.createAuditEntry(auditLog);
                    }
                }
            } else {
                String alertMessage = "speciality not found for Schedule type/rota type :" + appointmentfull.getType()
                        + " \n for the appointment id:" + appointmentfull.getId() + " \n patient id:" + appointmentfull.getPatientId()
                        + " \n schedule id:" + appointmentfull.getScheduleId() +" \n for the Organisation:" + orgName;

                SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, alertMessage);

                AuditLog auditLog = new AuditLog(ResourceConstants.Appointment, appointmentfull.getId(), aUUID, configID,
                        "speciality not found for schedule type/rota type :" + appointmentfull.getType(),updatedTime, orgName, "Y","", alertMessage);
                cieExtractsDal.createAuditEntry(auditLog);
            }

        }
        else {
            String alertMessage = "start date not found for the appointment id:" + appointmentfull.getId() + " \n patient id:" + appointmentfull.getPatientId()
                    + " \n schedule id:" + appointmentfull.getScheduleId() +" \n for the Organisation:" + orgName;

            SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, alertMessage);

            AuditLog auditLog = new AuditLog(ResourceConstants.Appointment, appointmentfull.getId(), aUUID, configID,
                    "start date not found for the appointment id:" + appointmentfull.getId() ,updatedTime, orgName, "Y","", alertMessage);
            cieExtractsDal.createAuditEntry(auditLog);
        }
    }
    public void processPractitioner(String orgName, long orgId, long practitionerId, String orgUUID, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberDal,String processType,boolean isRebulk,int configId) throws Exception {
        if (practitionerId > 0) {
            PractitionerFull practitionerResult = subscriberDal.getPractitionerFull(orgId, practitionerId);

            String practitionerUUID="";
            if(practitionerResult.getId()>0) {
                practitionerUUID = cieExtractsDal.checkReferenceExistsForResource(String.valueOf(practitionerResult.getId()), ResourceConstants.Practitioner, orgName, configId);

                org.hl7.fhir.dstu3.model.Practitioner practitionerResource = Practitioner.getPractitionerResource(practitionerResult);

                if(!practitionerUUID.isEmpty()) {
                    practitionerResource.setId(practitionerUUID);
                }
                String practitionerFhirStr="";

                practitionerFhirStr = getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(practitionerResource);

                if (CIEFhirExtractorEndpoint.isInterrupted(processType)) {
                    throw new Exception("User Interrupted");

                }
                if(practitionerUUID.isEmpty())  {
                    String postPractitionerResult = "";

                    postPractitionerResult = CIESenderUtil.sendResource(practitionerFhirStr, ResourceConstants.Practitioner,"POST",cieExtractsDal,null,null);

                    if( !postPractitionerResult.equals("success")){
                        java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
                        AuditLog auditLog = new AuditLog(ResourceConstants.Practitioner, practitionerResult.getId(),
                                practitionerResource.getId(), configId, practitionerFhirStr,
                                updatedTime, orgName,  "Y",
                                "POST",(postPractitionerResult )) ;

                        cieExtractsDal.createAuditEntry(auditLog);

                    }

                    if ( postPractitionerResult.equals("success")) {
                        cieExtractsDal.savePractitionerReference(practitionerResource.getId(), null, practitionerResult.getId(), orgName, configId);
                    }
                } else {
                    String result="";

                    result = CIESenderUtil.sendResource(practitionerFhirStr, ResourceConstants.Practitioner,"PUT",cieExtractsDal,null,null);


                    if( !result.equals("success")){
                        java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
                        AuditLog auditLog = new AuditLog(ResourceConstants.Practitioner, practitionerResult.getId(),
                                practitionerResource.getId(), configId, practitionerFhirStr,
                                updatedTime, orgName,  "Y",
                                "PUT", result) ;

                        cieExtractsDal.createAuditEntry(auditLog);
                    }
                    if ( result.equals("success")) {
                        cieExtractsDal.updateReference(practitionerResource.getId(),ResourceConstants.Practitioner );
                    }
                }
            }
        }
    }
    public  org.hl7.fhir.dstu3.model.Organization processOrganization(OrganizationFull organizationFull, CIEJdbcDAL cieExtractsDal, SubscriberJdbcDAL subscriberJdbcDAL, int configID) throws Exception {
        org.hl7.fhir.dstu3.model.Organization organizationResource;
        if(organizationFull.getParentOrganizationId() > 0 && organizationFull.getParentOrganizationId() != organizationFull.getId()){
            OrganizationFull parentOrganizationFull = subscriberJdbcDAL.getOrganizationFull(organizationFull.getParentOrganizationId());
            if(parentOrganizationFull.getId() > 0 ) {
                String pOrgUUID = cieExtractsDal.checkReferenceExistsForOrg(parentOrganizationFull.getOdsCode());
                if (pOrgUUID.isEmpty()) {
                    org.hl7.fhir.dstu3.model.Organization parentOrgResource = processOrganization(parentOrganizationFull, cieExtractsDal, subscriberJdbcDAL, configID);
                    organizationFull.setParentOrganizationUUID(ResourceConstants.referenceString(parentOrgResource.getId(), ResourceConstants.Organization));
                } else {
                    organizationFull.setParentOrganizationUUID(ResourceConstants.referenceString(pOrgUUID, ResourceConstants.Organization));
                }
            }
        }
        organizationResource = Organization.getOrganizationResource(organizationFull,null);

        String fhirStr="";
        String result="";

        fhirStr = getFhirContext().newJsonParser().setPrettyPrint(true).encodeResourceToString(organizationResource);
        result = CIESenderUtil.sendResource(fhirStr, ResourceConstants.Organization,"POST",cieExtractsDal,null,null);

        if ( result.equals("success")) {
            cieExtractsDal.saveOrganizationReference(organizationResource.getId(), organizationFull.getOdsCode());
        } else {
            java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());
            AuditLog auditLog = new AuditLog(ResourceConstants.Organization, organizationFull.getId(),
                    organizationResource.getId(), configID, fhirStr,
                    updatedTime, organizationFull.getOdsCode(), "Y",
                    "POST",  result );

            cieExtractsDal.createAuditEntry(auditLog);
            throw new Exception(auditLog.getReason());
        }
        return organizationResource;
    }
}