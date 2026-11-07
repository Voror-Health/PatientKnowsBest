package org.endeavourhealth.patientfhirextractor.resources;

import org.apache.commons.lang3.ArrayUtils;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.models.PractitionerFull;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;

public class Practitioner {
    private static final Logger LOG = LoggerFactory.getLogger(Practitioner.class);

    public static org.hl7.fhir.dstu3.model.Practitioner getPractitionerResource(PractitionerFull practitionerResult) {
        if (practitionerResult == null) {
            return null;
        }
        org.hl7.fhir.dstu3.model.Practitioner practitioner = new org.hl7.fhir.dstu3.model.Practitioner();

        UUID uuid = UUID.randomUUID();
        practitioner.setId(uuid.toString());

        practitioner.getMeta().addProfile(ResourceConstants.PRACTITIONER_URL);
        practitioner.addIdentifier()
                .setValue(String.valueOf(practitionerResult.getId()))
                .setSystem(ResourceConstants.SYSTEM_ID);
        if(practitionerResult.getGmcCode()!=null && !practitionerResult.getGmcCode().isEmpty()) {
            practitioner.addIdentifier()
                    .setSystem(ResourceConstants.IDENTIFIER_URL)
                    .setValue(practitionerResult.getGmcCode());
        }
        HumanName name = practitioner.addName();

        if (practitionerResult.getName() != null) {
            name.setText(practitionerResult.getName());
        }
        else {
            LOG.info("Practitioner name details are not available");
        }
        return practitioner;
    }



}

