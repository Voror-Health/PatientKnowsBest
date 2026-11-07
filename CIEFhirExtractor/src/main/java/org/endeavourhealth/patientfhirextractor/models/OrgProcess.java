package org.endeavourhealth.patientfhirextractor.models;
import java.sql.Timestamp;

public class OrgProcess {

    private String org;
    private int orgThreadsId;
    private Timestamp startDatetime;
    private Timestamp endDatetime;
    private String status;

    public String getOrg() {
        return org;
    }

    public OrgProcess setOrg(String org) {
        this.org = org;
        return this;
    }

    public int getOrgThreadsId() {
        return orgThreadsId;
    }

    public OrgProcess setOrgThreadsId(int orgThreadsId) {
        this.orgThreadsId = orgThreadsId;
        return this;
    }

    public Timestamp getStartDatetime() {
        return startDatetime;
    }

    public OrgProcess setStartDatetime(Timestamp startDatetime) {
        this.startDatetime = startDatetime;
        return this;
    }

    public Timestamp getEndDatetime() {
        return endDatetime;
    }

    public OrgProcess setEndDatetime(Timestamp endDatetime) {
        this.endDatetime = endDatetime;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public OrgProcess setStatus(String status) {
        this.status = status;
        return this;
    }
}


