package org.endeavourhealth.patientfhirextractor.helper;

import java.util.ArrayList;
import java.util.List;

public class ResourceFieldUtil {

    public static final List<String> orgList ;
    public static final List<String> patientList ;
    public static final List<String> appointmentList ;
    public static final List<String> MedicationList ;
    public static final List<String> conditionList ;
    public static final List<String> practitionerList ;

    static {
        orgList = new ArrayList<String>();
        orgList.add("Org_uuid");
        orgList.add("meta.Profile");
        orgList.add("Name");

        patientList = new ArrayList<String>();
        patientList.add("Patient_uuid");
        patientList.add("meta.Profile");
        patientList.add("NHSNumber");
        //patientList.add("NHS_number_status");
        patientList.add("Patient_Name");
        //patientList.add("lastname");
        patientList.add("DOB");
        patientList.add("Gender");//sex (male,female)
        patientList.add("ManagingOrganization_reference");//Source

        appointmentList = new ArrayList<String>();
        appointmentList.add("appointment_uuid");
        appointmentList.add("meta.Profile");
        appointmentList.add("participant.actor");//need to find it
        appointmentList.add("participant.status");//need to find it
        appointmentList.add("start_date");

        MedicationList = new ArrayList<String>();
        MedicationList.add("medication_uuid");
        MedicationList.add("meta.Profile");
        MedicationList.add("patient_uuid");
        MedicationList.add("medicationReference");//need to find it
        MedicationList.add("last_issue_date");
        MedicationList.add("Taken");

        conditionList = new ArrayList<String>();
        conditionList.add("condition_uuid");
        conditionList.add("meta.Profile");
        conditionList.add("patient_uuid");
        conditionList.add("ClinicalStatus");
        conditionList.add("DiagnosisDate");

        practitionerList = new ArrayList<String>();
        practitionerList.add("practitioner_uuid");
        practitionerList.add("meta.Profile");
        practitionerList.add("practitioner.reference");//need to find it
        practitionerList.add("organization.reference");//need to find it

    }

}
