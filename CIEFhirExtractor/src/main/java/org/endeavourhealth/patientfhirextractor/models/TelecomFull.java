package org.endeavourhealth.patientfhirextractor.models;

public class TelecomFull {

    private long id;
    private String value;
    private String description1;
    private String description2;


    public long getId() {
        return id;
    }

    public TelecomFull setId(long id) {
        this.id = id;
        return this;
    }

    public String getValue() {
        return value;
    }

    public TelecomFull setValue(String value) {
        this.value = value;
        return this;
    }

    public String getDescription1() {
        return description1;
    }

    public TelecomFull setDescription1(String description1) {
        this.description1 = description1;
        return this;
    }

    public String getDescription2() {
        return description2;
    }

    public TelecomFull setDescription2(String description2) {
        this.description2 = description2;
        return this;
    }
}

