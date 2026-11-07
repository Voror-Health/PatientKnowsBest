package org.endeavourhealth.patientfhirextractor.models;
import java.sql.Timestamp;

public class AuditLog {
    private int id;
    private String resourceType;
    private long resourceLocalId;
    private String resourceUUID;
    private int configID;
    private String logString;
    private Timestamp updatedDateTime;
    private String orgName;
    private String reprocessRequired;
    private String httpMethod;
    private String reason;

    public AuditLog() {

    }
    public AuditLog(String resourceType, long resourceLocalId, String resourceUUID, int configID, String logString, Timestamp updatedDateTime, String orgName, String reprocessRequired, String httpMethod, String reason) {
        this.resourceType = resourceType;
        this.resourceLocalId = resourceLocalId;
        this.resourceUUID = resourceUUID;
        this.configID = configID;
        this.logString = logString;
        this.updatedDateTime = updatedDateTime;
        this.orgName = orgName;
        this.reprocessRequired = reprocessRequired;
        this.httpMethod = httpMethod;
        this.reason = reason;
    }

    public int getId() {
        return id;
    }

    public AuditLog setId(int id) {
        this.id = id;
        return this;
    }

    public String getResourceType() {
        return resourceType;
    }

    public AuditLog setResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }
    public long getResourceLocalId() {
        return resourceLocalId;
    }

    public AuditLog setResourceLocalId(long resourceLocalId) {
        this.resourceLocalId = resourceLocalId;
        return this;
    }

    public String getResourceUUID() {
        return resourceUUID;
    }

    public AuditLog setResourceUUID(String resourceUUID) {
        this.resourceUUID = resourceUUID;
        return this;
    }

    public String getLogString() {
        return logString;
    }

    public AuditLog setLogString(String logString) {
        this.logString = logString;
        return this;
    }

    public Timestamp getUpdatedDateTime() {
        return updatedDateTime;
    }

    public AuditLog setUpdatedDateTime(Timestamp updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
        return this;
    }

    public String getOrgName() {
        return orgName;
    }

    public AuditLog setOrgName(String orgName) {
        this.orgName = orgName;
        return this;
    }

    public String getReprocessRequired() {
        return reprocessRequired;
    }

    public AuditLog setReprocessRequired(String reprocessRequired) {
        this.reprocessRequired = reprocessRequired;
        return this;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public AuditLog setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public int getConfigID() {
        return configID;
    }

    public AuditLog setConfigID(int configID) {
        this.configID = configID;
        return this;
    }
    public String getReason() {
        return reason;
    }

    public AuditLog setReason(String reason) {
        this.reason = reason;
        return this;
    }
}
