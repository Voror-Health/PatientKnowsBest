package org.endeavourhealth.patientfhirextractor.dal;

public class CIEReference {
    private long local_id;
    private String refUUID;
    private long config_id;
    private long patient_id;
    private long non_core_concept_id;
    private String status;

    public long getLocal_id() {
        return local_id;
    }

    public void setLocal_id(long local_id) {
        this.local_id = local_id;
    }

    public String getRefUUID() {
        return refUUID;
    }

    public void setRefUUID(String refUUID) {
        this.refUUID = refUUID;
    }

    public long getConfig_id() {
        return config_id;
    }

    public void setConfig_id(long config_id) {
        this.config_id = config_id;
    }

    public long getPatientId() {
        return patient_id;
    }

    public void setPatientId(long patient_id) {
        this.patient_id = patient_id;
    }

    public long getConceptId() {
        return non_core_concept_id;
    }

    public void setConceptId(long non_core_concept_id) {
        this.non_core_concept_id = non_core_concept_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
