package org.endeavourhealth.patientfhirextractor.models;

public class SpecialityFull {
    private String speciality;
    private String code;

    public SpecialityFull(String speciality, String code) {
        this.speciality=speciality;
        this.code=code;
    }
    public SpecialityFull()
    {

    }
    public String getSpeciality() {
        return speciality;
    }

    public SpecialityFull setSpeciality(String speciality) {
        this.speciality = speciality;
        return this;
    }

    public String getCode() {
        return code;
    }

    public SpecialityFull setCode(String code) {
        this.code = code;
        return this;
    }
}
