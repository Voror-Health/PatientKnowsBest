package org.endeavourhealth.patientfhirextractor.models;

import java.sql.Date;

public class EventLog {
    private long recordId;

    public long getRecordId() {
        return recordId;
    }

    public EventLog setRecordId(long recordId) {
        this.recordId = recordId;
        return this;
    }

    public int getTableId() {
        return tableId;
    }

    public EventLog setTableId(int tableId) {
        this.tableId = tableId;
        return this;
    }

    public int getChangeType() {
        return changeType;
    }

    public EventLog setChangeType(int changeType) {
        this.changeType = changeType;
        return this;
    }

    public Date getDateChange() {
        return dateChange;
    }

    public EventLog setDateChange(Date dateChange) {
        this.dateChange = dateChange;
        return this;
    }

    private int tableId;
    private int changeType;
    private Date dateChange;
}
