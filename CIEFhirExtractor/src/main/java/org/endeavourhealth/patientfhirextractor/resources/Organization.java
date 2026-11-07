package org.endeavourhealth.patientfhirextractor.resources;

import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.models.OrganizationFull;
import org.hl7.fhir.dstu3.model.Reference;

import java.util.UUID;

public class Organization {

    public static org.hl7.fhir.dstu3.model.Organization  getOrganizationResource(OrganizationFull organizationFull,String uuid)
    {
        org.hl7.fhir.dstu3.model.Organization organization = null;
        organization = new org.hl7.fhir.dstu3.model.Organization();

        if(organizationFull !=null) {
            if(null!=uuid && !uuid.isEmpty())   organization.setId(uuid);
            else   organization.setId(UUID.randomUUID().toString());
            organization.addIdentifier()
                    .setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
                    .setValue(organizationFull.getOdsCode());
            organization.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Organization-1");
            organization.setName(organizationFull.getName());
            organization.addIdentifier()
                    .setValue(String.valueOf(organizationFull.getId()))
                    .setSystem(ResourceConstants.SYSTEM_ID);
            organization.setPartOf(new Reference(organizationFull.getParentOrganizationUUID()));
        }

        return organization;
    }



}

