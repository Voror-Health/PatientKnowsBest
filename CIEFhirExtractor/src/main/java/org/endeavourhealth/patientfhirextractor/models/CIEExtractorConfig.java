package org.endeavourhealth.patientfhirextractor.models;

import com.fasterxml.jackson.databind.JsonNode;



public class CIEExtractorConfig {
    private int maxThreadPoolSize;
    private int maxQueueBeforeBlocking;
    private int maxOrgsPerThread;
    private JsonNode bulkPriorities;
    private JsonNode deltaPriorities;
    private JsonNode deltaDeletePriorities;
    private JsonNode emailIds;


    public int getMaxOrgsPerThread() {
        return maxOrgsPerThread;
    }

    public void setMaxOrgsPerThread(int maxOrgsPerThread) {
        this.maxOrgsPerThread = maxOrgsPerThread;
    }
    public int getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public void setMaxThreadPoolSize(int maxThreadPoolSize) {
        this.maxThreadPoolSize = maxThreadPoolSize;
    }

    public int getMaxQueueBeforeBlocking() {
        return maxQueueBeforeBlocking;
    }

    public void setMaxQueueBeforeBlocking(int maxQueueBeforeBlocking) {
        this.maxQueueBeforeBlocking = maxQueueBeforeBlocking;
    }

    public JsonNode getBulkPriorities() {
        return bulkPriorities;
    }

    public void setBulkPriorities(JsonNode bulkPriorities) {
        this.bulkPriorities = bulkPriorities;
    }

    public JsonNode getDeltaPriorities() {
        return deltaPriorities;
    }

    public void setDeltaPriorities(JsonNode deltaPriorities) {
        this.deltaPriorities = deltaPriorities;
    }

    public JsonNode getDeltaDeletePriorities() {
        return deltaDeletePriorities;
    }

    public void setDeltaDeletePriorities(JsonNode deltaDeletePriorities) {
        this.deltaDeletePriorities = deltaDeletePriorities;
    }

    public JsonNode getEmailIds() {
        return emailIds;
    }

    public void setEmailIds(JsonNode emailIds) {
        this.emailIds = emailIds;
    }



}
