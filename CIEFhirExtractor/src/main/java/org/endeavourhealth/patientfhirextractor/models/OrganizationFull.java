package org.endeavourhealth.patientfhirextractor.models;
public class OrganizationFull {
    private long id=0;
    private String odsCode;
    private String name;
    private String postCode;
    private long parentOrganizationId;
    private String parentOrganizationUUID;

    public long getId() {
        return id;
    }

    public OrganizationFull setId(long id) {
        this.id = id;
        return this;
    }

    public String getOdsCode() {
        return odsCode;
    }

    public OrganizationFull setOdsCode(String odsCode) {
        this.odsCode = odsCode;
        return this;
    }

    public String getName() {
        return name;
    }

    public OrganizationFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostCode() {
        return postCode;
    }

    public OrganizationFull setPostCode(String postCode) {
        this.postCode = postCode;
        return this;
    }
    public long getParentOrganizationId() {
        return parentOrganizationId;
    }

    public OrganizationFull setParentOrganizationId(long parentOrganizationId) {
        this.parentOrganizationId = parentOrganizationId;
        return this;
    }

    public String getParentOrganizationUUID() {
        return parentOrganizationUUID;
    }

    public OrganizationFull setParentOrganizationUUID(String parentOrganizationUUID) {
        this.parentOrganizationUUID = parentOrganizationUUID;
        return this;
    }
}

