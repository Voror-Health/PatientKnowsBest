package org.endeavourhealth.patientfhirextractor.constants;

public class ResourceConstants {

    public static final String SYSTEM_ID = "https://fhir.hl7.org.uk/Id/dds";
    public static final String ORGANIZATION_CODE = "https://fhir.nhs.uk/Id/ODS-Code";

    //Practitioner Role
    public static final String PRACTITIONER_ROLE_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-PractitionerRole-1";

    //Practitioner
    public static final String IDENTIFIER_URL = "https://fhir.nhs.uk/Id/sds-user-id";
    public static final String PRACTITIONER_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Practitioner-1";
    public static final String PRACTITIONER_ROLE_SYSTEM = "https://fhir.hl7.org.uk/STU3/CodeSystem/CareConnect-SDSJobRoleName-1";

    //Observation
    public static final String TITLE = "Miscellaneous record";
    public static final String  OBSERVATION_LIST_PROFILE = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-List-1";
    public static final String  OBSERVATION_LIST_CODE = "event-date";
    public static final String  OBSERVATION_LIST_SYSTEM = "http://hl7.org/fhir/list-order";
    public static final String OBSERVATION_PROFILE = "https://fhir.nhs.uk/STU3/StructureDefinition/CareConnect-GPC-Observation-1";
    public static final String OBSERVATION_SYSTEM = "http://snomed.info/sct";
    public static final String OBSERVATION_DESCRIPTION = "descriptionDisplay";
    public static final String OBSERVATION_QUANTITY_VALUE = "http://unitsofmeasure.org";
    public static final String VALUE_STRING = "valueString";
    public static final String VALUE_QUANTITY = "valueQuantity";

    //Referrel Request
    public static final String REFERREL_REQUEST_PROFILE = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-ReferralRequest-1";

    //Family History
    public static final String FAMILY_HISTORY_PROFILE = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-FamilyMemberHistory-1";

    //Episode Of Care
    public static final String EPISODE_OF_CARE_TYPE_CODING = "http://terminology.hl7.org/CodeSystem/episodeofcare-type";

    //Location
    public static final String LOCATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/location-physical-type";

    //Procedure
    public static final String PROCEDURE_SYSTEM = "http://snomed.info/sct";

    public static final String Organization ="Organization";
    public static final String Patient ="Patient";
    //public static final String NewPatient ="NewPatient";
    public static final String Condition ="Condition";
    public static final String MedicationStatement = "MedicationStatement";
    public static final String Medication = "Medication";
    public static final String Practitioner="Practitioner";
    public static final String PractitionerRole="PractitionerRole";
    public static final String Appointment ="Appointment";

    public static final String Location="Location";

    public static final String CIEOrganizationTable ="cie_org_references";
    public static final String CIEPatientTable ="cie_patient_reference";
    // public static final String CIENewPatientTable ="cie_new_patient_reference";
    public static final String CIEConditionTable ="cie_condition_reference";
    public static final String CIEMedicationStatementTable = "cie_medication_statement_reference";
    public static final String CIEMedicationTable = "cie_medication_reference";
    public static final String CIEPractitionerTable="cie_practitioner_reference";
    public static final String CIEAppointmentTable ="cie_appointment_reference";
    //Adding the response code as part of Reprocess design
    public static final String CIEHTTPResponseCode = "503";

    //Audit report file location
    public static final String AUDIT_ERROR_REPORT_FILE_LOC = "/tmp/auditreport.csv";

    //SMTP Server config details
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "465";
    public static final String SMTP_SERVER_USERNAME = "pendiminagraj@gmail.com";
    public static final String SMTP_SERVER_PWD = "hoxxvpkhukbgfrue";
    public static final String CW_ODS_CODE = "566348274104";



    public static String referenceString(String uuid,String resourceType){
        String result="";
        switch (resourceType) {
            case ResourceConstants.Organization:
                result = "Organization/"+uuid;
                break;
            case ResourceConstants.Patient:
                result = "Patient/"+uuid;
                break;
            case ResourceConstants.Condition:
                result = "Condition/"+uuid;
                break;
            case ResourceConstants.Medication:
                result = "Medication/"+uuid;;
                break;
            case ResourceConstants.MedicationStatement:
                result = "MedicationStatement/"+uuid;
                break;
            case ResourceConstants.Practitioner:
                result = "Practitioner/"+uuid;
                break;
           /* case ResourceConstants.PractitionerRole:
                result = "PractitionerRole/"+uuid;
                break;*/
            case ResourceConstants.Appointment:
                result = "Appointment/"+uuid;
                break;
        }
        return result;
    }


}

