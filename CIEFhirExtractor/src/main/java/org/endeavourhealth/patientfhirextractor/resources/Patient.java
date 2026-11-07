package org.endeavourhealth.patientfhirextractor.resources;

import com.google.common.base.Strings;
import org.endeavourhealth.common.utility.SlackHelper;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.dal.SubscriberJdbcDAL;
import org.endeavourhealth.patientfhirextractor.models.AuditLog;
import org.endeavourhealth.patientfhirextractor.models.PatientFull;
import org.endeavourhealth.patientfhirextractor.models.TelecomFull;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class Patient {

    private static final Logger LOG = LoggerFactory.getLogger(Patient.class);

    public static org.hl7.fhir.dstu3.model.Patient getPatientResource(PatientFull patientResult, SubscriberJdbcDAL JDBCDAL, CIEJdbcDAL cieExtractsDal,int configId) throws Exception {
        long id = patientResult.getId();
        List<TelecomFull> telecomFullList = patientResult.getTelecomFullList();
        String nhsNumber = replaceNull(patientResult.getNhsNumber());
        // CHDT-138 HIE (Mandatory Fields) - In the case of gender not present please still send it with the value being - unknown
        String gender =	patientResult.getGender() == null || patientResult.getGender().equals("")
                && patientResult.getGender().length() == 0 ? "Unknown" : patientResult.getGender();
        String lastname = replaceNullorEmptyWithQuotes(patientResult.getLastname());
        String title = replaceNullorEmptyWithQuotes(patientResult.getTitle());
        String firstname = replaceNullorEmptyWithQuotes(patientResult.getFirstname());
        String dob = replaceNull(patientResult.getDob());
        String dod = replaceNull(patientResult.getDod());
        String adduse = replaceNull(patientResult.getAdduse());
        String add1 = replaceNull(patientResult.getAdd1());
        String add2 = replaceNull(patientResult.getAdd2());
        String add3 = replaceNull(patientResult.getAdd3());
        String add4 = replaceNull(patientResult.getAdd4());
        String postcode = replaceNull(patientResult.getPostcode());
        String city = replaceNull(patientResult.getCity());
        String otheraddresses = replaceNull(patientResult.getOtheraddresses());
        String orglocation = replaceNull(patientResult.getOrglocation());
        String startdate = replaceNull(patientResult.getStartdate());
        String ethnicDescription = replaceNullorEmptyWithQuotes(patientResult.getEthnicDescription());
        String ethnicCode = replaceNullorEmptyWithQuotes(patientResult.getEthnicCode());
        String endDate = replaceNull(patientResult.getRegistrationEndDate());
        String registrationStatusValue = replaceNull(patientResult.getRegistrationStatusValue());

        java.sql.Timestamp updatedTime = new java.sql.Timestamp(System.currentTimeMillis());

        String alert_message= "";

        if(Strings.isNullOrEmpty(nhsNumber)){
            alert_message = alert_message + "NHS number,";
        }
        if(Strings.isNullOrEmpty(firstname)){
            alert_message = alert_message + "First Name \n";
        }
        if(Strings.isNullOrEmpty(lastname)){
            alert_message = alert_message + "Last Name \n";
        }
        if(Strings.isNullOrEmpty(dob)){
            alert_message = alert_message + "Date of Birth \n";
        }
        if(Strings.isNullOrEmpty(gender) || gender.equalsIgnoreCase("Unknown")){
            alert_message = alert_message + "Gender ";
        }

        if(!Strings.isNullOrEmpty(alert_message)){
           String msg = "Alert for missing fields of " + alert_message + "for Patient Id = "+ id +", \n Organization = " + patientResult.getOds_code()
                                +", \n NHS number = " + patientResult.getNhsNumber()+", \n First Name = " + patientResult.getFirstname()
                                +", \n Last Name = " + patientResult.getLastname()+", \n Date of Birth = " + patientResult.getDob()
                                +", \n Gender = " + patientResult.getGender();

            SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor, msg);
            AuditLog auditLog = new AuditLog(ResourceConstants.Patient, id, "", configId,
                    "Patient Mandatory Fields are missing for "+alert_message,updatedTime, patientResult.getOds_code(), "Y","", msg);
            cieExtractsDal.createAuditEntry(auditLog);
        }

        org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();

        String[] religionData = JDBCDAL.getReligion(id);
        if (religionData != null) {
            Extension extension1 = new Extension();
            List<Coding> coding = new ArrayList<>();
            coding.add(new Coding(" https://fhir.hl7.org.uk/STU3/ValueSet/CareConnect-ReligiousAffiliation-1", religionData[0], religionData[1]));
            CodeableConcept codeableConcept = (CodeableConcept) extension1.addChild("valueCodeableConcept");
            codeableConcept.setCoding(coding);
            extension1.setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ReligiousAffiliation-1");
            patient.addExtension(extension1);
        }

      /*  String[] language = JDBCDAL.getLanguage(id);
        if (language != null) {
            org.hl7.fhir.dstu3.model.Patient.PatientCommunicationComponent patientCommunicationComponent = new org.hl7.fhir.dstu3.model.Patient.PatientCommunicationComponent();
            List<Coding> coding = new ArrayList<>();
            coding.add(new Coding(null, language[0], language[1]));
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.setCoding(coding);
            patientCommunicationComponent.setLanguage(codeableConcept);
            patient.setCommunication(Arrays.asList(patientCommunicationComponent));
        }
*/
        patient.setId(UUID.randomUUID().toString());

        patient.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Patient-1");

        nhsNumber=nhsNumber.trim();
       if (Long.parseLong(nhsNumber) >= 4000000000L && Long.parseLong(nhsNumber) <= 9999999999L) {
           patient.addIdentifier()
                   .setSystem("https://fhir.nhs.uk/Id/nhs-number")
                   .setValue(nhsNumber);
       } else if (Long.parseLong(nhsNumber) >= 0101010000 && Long.parseLong(nhsNumber) <= 3112999999L ) {

            patient.addIdentifier()
                    .setSystem("urn:oid:2.16.840.1.113883.2.1.3.2.4.16.53")
                    .setValue(nhsNumber);
        } else if(Long.parseLong(nhsNumber) >= 3200000010L && Long.parseLong(nhsNumber) <= 3999999999L ) {
            patient.addIdentifier()
                    .setSystem("urn:uuid:1479257a-ce8f-482a-89fa-429ac066c158")
                    .setValue(nhsNumber);

        } else {
           String msg="NHS Number is not in the range " +nhsNumber + "for Patient Id" + patient.getId();
           SlackHelper.sendSlackMessage(SlackHelper.Channel.CIEFhirExtractor,msg);
           AuditLog auditLog = new AuditLog(ResourceConstants.Patient, id, "", configId,
                   "NHS Number is not in the range for Patient"+patient.getId(),updatedTime, patientResult.getOds_code(), "Y","", msg);
           cieExtractsDal.createAuditEntry(auditLog);
        }

        switch(gender) {
            case "Other":
                patient.setGender(Enumerations.AdministrativeGender.OTHER);
                break;
            case "Male":
                patient.setGender(Enumerations.AdministrativeGender.MALE);
                break;
            case "Female":
                patient.setGender(Enumerations.AdministrativeGender.FEMALE);
                break;
            case "Unknown":
                patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
                break;
            default:
                // code block
        }

        patient.addName()
                .setFamily(lastname)
                .addPrefix(title)
                .addGiven(firstname)
                .setUse(HumanName.NameUse.OFFICIAL);
        if(null!=ethnicDescription) {
            CodeableConcept codeEthnic = new CodeableConcept();
            codeEthnic.addCoding()
                    .setSystem("https://fhir.hl7.org.uk/STU3/CodeSystem/CareConnect-EthnicCategory-1")
                    .setDisplay(ethnicDescription)
                    .setCode(ethnicCode);
            patient.addExtension().setUrl("https://fhir.hl7.org.uk/StructureDefinition/Extension-CareConnect-EthnicCategory-1")
                    .setValue(codeEthnic);
        }
        if (telecomFullList.size() > 0) {
            for(TelecomFull telecomFull : telecomFullList) {
                String telecom = replaceNull(telecomFull.getValue());
                String desc1 = replaceNull(telecomFull.getDescription1());
                String desc2 = replaceNull(telecomFull.getDescription2());
                if (desc1.equalsIgnoreCase("Phone")) {
                    ContactPoint t = new ContactPoint();
                    t.setValue(telecom);
                    t.setSystem(ContactPoint.ContactPointSystem.PHONE);
                    setPhoneData(desc2, t);
                    patient.addTelecom(t);
                }
            }
        }

        if (dob!=null && !dob.isEmpty()) {
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            patient.setBirthDate(format.parse(dob));
        }

        if (dod!=null && !dod.isEmpty()) {
            DateTimeType dateTimeType = new DateTimeType();
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            dateTimeType.setValue(format.parse(dod));
            patient.setDeceased(dateTimeType);
        } else {
           patient.setDeceased(new BooleanType(false));
        }

        Address address = new Address();

        if (adduse.equals("HOME")) {address.setUse(Address.AddressUse.HOME);}
        if (adduse.equals("TEMP")) {address.setUse(Address.AddressUse.TEMP);}
        if (adduse.equals("OLD")) {address.setUse(Address.AddressUse.OLD);}

        address.addLine(add1);
        address.addLine(add2);
        address.addLine(add3);
        address.addLine(add4);
        address.setPostalCode(postcode);
        address.setCity(city);
        if (patientResult.getAddressStartDate()!=null && !patientResult.getAddressStartDate().isEmpty()) {
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            Period period = new Period();
            period.setStart(format.parse(patientResult.getAddressStartDate()));
            if(patientResult.getAddressEndDate()!=null && !patientResult.getAddressEndDate().isEmpty()) {
                period.setEnd(format.parse(patientResult.getAddressEndDate()));
            }
          address.setPeriod(period);
        }

        patient.addAddress(address);

        // add1`add2`add3`add4`city`postcode`useconceptid| <= alternative addresses
        if (otheraddresses.length()>0) {
            String[] ss = otheraddresses.split("\\|");
            String z = "";
            for (int i = 0; i < ss.length; i++) {
                z = ss[i];
                String[] zaddress = z.split("\\`");
                Address t = new Address();
                if (zaddress[6].equals("1335358")) {t.setUse(Address.AddressUse.HOME);}
                if (zaddress[6].equals("1335360")) {t.setUse(Address.AddressUse.TEMP);}
                if (zaddress[6].equals("1335361")) {t.setUse(Address.AddressUse.OLD);}
                t.addLine(zaddress[0]);
                t.addLine(zaddress[1]);
                t.addLine(zaddress[2]);
                t.addLine(zaddress[3]);
                t.setPostalCode(zaddress[5]);
                t.setCity(zaddress[4]);
                patient.addAddress(t);
            }
        }

        return patient;
    }

    public static String replaceNull(String input) {
        return input == null ? null : input;
    }

    /*
     * CHDT-138 HIE (Mandatory Fields) - As a part of this requirement some of the fields to be returned as
     * double quote to ensure they are not being dropped from the response in case they are null/empty.
     */
    public static String replaceNullorEmptyWithQuotes(String input) {
        return input == null || input.equals("") && input.length() == 0 ? null : input;
    }

    private static void setPhoneData(String desc, ContactPoint contactPoint){
        switch (desc.toUpperCase()) {
            case "MOBILE":
                contactPoint.setUse(ContactPoint.ContactPointUse.MOBILE);
                break;
            case "WORK":
                contactPoint.setUse(ContactPoint.ContactPointUse.WORK);
                break;
            case "TEMPORARY":
                contactPoint.setUse(ContactPoint.ContactPointUse.TEMP);
                break;
            case "OLD":
                contactPoint.setUse(ContactPoint.ContactPointUse.OLD);
                break;
            default:
                contactPoint.setUse(ContactPoint.ContactPointUse.HOME);
        }
    }


}

