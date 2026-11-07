package org.endeavourhealth.patientfhirextractor.resources;

import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.models.MedicationStatementFull;
import org.hl7.fhir.dstu3.model.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MedicationStatement {

    public static org.hl7.fhir.dstu3.model.MedicationStatement getMedicationStatementResource(MedicationStatementFull medicationStatementResult) throws Exception {
        String clinicalEffDate = replaceNull(medicationStatementResult.getDate());
        String dose = replaceNull(medicationStatementResult.getDose());
        UUID id = UUID.randomUUID();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        org.hl7.fhir.dstu3.model.MedicationStatement medicationStatement = new org.hl7.fhir.dstu3.model.MedicationStatement();

        medicationStatement.addIdentifier()
                .setValue(String.valueOf(medicationStatementResult.getId()))
                .setSystem(ResourceConstants.SYSTEM_ID);

        Date cancellationDate = medicationStatementResult.getCancellationDate().equals("") ? null:new SimpleDateFormat("yyyy-MM-dd").parse(medicationStatementResult.getCancellationDate());
        medicationStatement.setStatus(cancellationDate == null || new Date().before(cancellationDate)
                ? org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementStatus.ACTIVE :
                org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementStatus.COMPLETED);

        medicationStatement.setId(String.valueOf(id));
        medicationStatement.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-MedicationStatement-1");
        medicationStatement.setTaken(org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementTaken.UNK);

        Period period = new Period();
        try {
            period.setStart(formatter.parse(clinicalEffDate));
            period.setEnd(cancellationDate);

        } catch (Exception e) {
        }

        medicationStatement.setEffective(period);

        List<Extension> extensionList = new ArrayList<Extension>();

        medicationStatement.setExtension(extensionList);

        List<Dosage> dosageList = new ArrayList<Dosage>();
        Dosage dosage = new Dosage();
        dosage.setText(dose);
        dosageList.add(dosage);
        medicationStatement.setDosage(dosageList);

        return medicationStatement;
    }

    private static String setMedicationName(String name) {
        name = name.replaceAll("Product containing precisely ","");
        name = name.replaceAll("Product containing ","");
        name = name.replaceAll("\\(clinical drug\\)","");
        name = name.replaceAll("\\(product\\)","");
        return name;
    }

    public static org.hl7.fhir.dstu3.model.Medication getMedicationResource(MedicationStatementFull medicationStatementResult) throws Exception {
        String name = setMedicationName(replaceNull(medicationStatementResult.getName()));
        String code = replaceNull(medicationStatementResult.getCode());
        UUID id = UUID.randomUUID();

        org.hl7.fhir.dstu3.model.Medication medication = new org.hl7.fhir.dstu3.model.Medication();

        medication.setId(String.valueOf(id));
        medication.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Medication-1");

        Coding coding = new Coding();
        coding.setDisplay(name);
        coding.setCode(code);
        coding.setSystem(medicationStatementResult.getCodeURL());
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.addCoding(coding);
        medication.setCode(codeableConcept);

        return medication;
    }

    /**
     *
     * @param input
     * @return
     */
    public static String replaceNull(String input) {
        return input == null ? "" : input;
    }
}

