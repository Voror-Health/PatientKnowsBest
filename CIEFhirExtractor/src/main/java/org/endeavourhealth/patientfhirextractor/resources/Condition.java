package org.endeavourhealth.patientfhirextractor.resources;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.dal.SubscriberJdbcDAL;
import org.endeavourhealth.patientfhirextractor.models.ConditionFull;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

import java.util.Arrays;
import java.util.TimeZone;
import java.util.UUID;

public class Condition {

    public static org.hl7.fhir.dstu3.model.Condition getConditionResource(ConditionFull conditionfull, SubscriberJdbcDAL jdbcdal) throws Exception {
        org.hl7.fhir.dstu3.model.Condition condition = new org.hl7.fhir.dstu3.model.Condition();
        if(conditionfull.getClinicalStatus().equalsIgnoreCase("active"))
        { condition.setClinicalStatus(org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus.ACTIVE); }
        else {
            condition.setClinicalStatus(org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus.RESOLVED);
        }
        condition.setOnset(new org.hl7.fhir.dstu3.model.DateTimeType(conditionfull.getDate(), TemporalPrecisionEnum.SECOND, TimeZone.getDefault()));
        condition.setAbatement(new org.hl7.fhir.dstu3.model.DateTimeType(conditionfull.getEndDate(),TemporalPrecisionEnum.SECOND, TimeZone.getDefault()));
        condition.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Condition-1");

        condition.addIdentifier()
                .setValue(String.valueOf(conditionfull.getId()))
                .setSystem(ResourceConstants.SYSTEM_ID);

        String description = jdbcdal.getDescriptionFromObservation(conditionfull.getId());
        if(description != null) {
            description=description.equalsIgnoreCase("Significant")?"Major":"Minor";
            String pdcode=description.equalsIgnoreCase("Major")?"major":"minor";
            CodeableConcept problemSignificanceCode = new CodeableConcept();
            problemSignificanceCode.addCoding()
                    .setCode(pdcode)
                    .setDisplay(description)
                    .setSystem("https://fhir.hl7.org.uk/STU3/ValueSet/CareConnect-ProblemSignificance-1");

            condition.addExtension()
                    .setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-ProblemSignificance-1")
                    .setValue(problemSignificanceCode);

        }
        // manifestation or codeable concept?
        CodeableConcept code = new CodeableConcept();
            code.addCoding()
                .setCode(conditionfull.getCode())
                .setSystem(conditionfull.getCodeURL())
                .setDisplay(conditionfull.getName());
        condition.setId(UUID.randomUUID().toString());
        condition.setCode(code);

        CodeableConcept category = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/ValueSet/condition-category");

        if(conditionfull.isProblem()){
            coding.setCode("problem-list-item");
            coding.setDisplay("Problem list Item");
        } else {
            coding.setCode("encounter-diagnosis");
            coding.setDisplay("encounter diagnosis");
        }
        category.addCoding(coding);
        condition.setCategory(Arrays.asList(category));

        return condition;
    }


}

