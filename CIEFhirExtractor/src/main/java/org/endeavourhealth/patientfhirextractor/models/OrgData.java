package org.endeavourhealth.patientfhirextractor.models;

public class OrgData {
    private String publicOrg;

    private String regAuthAccess;

    public String getPublicOrg() {
        return publicOrg;
    }

    public OrgData setPublicOrg(String publicOrg) {
        this.publicOrg = publicOrg;
        return this;
    }

    public String getRegAuthAccess() {
        return regAuthAccess;
    }

    public OrgData setRegAuthAccess(String regAuthAccess) {
        this.regAuthAccess = regAuthAccess;
        return this;
    }

}
