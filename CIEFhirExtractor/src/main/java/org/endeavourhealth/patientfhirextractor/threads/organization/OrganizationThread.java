package org.endeavourhealth.patientfhirextractor.threads.organization;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public abstract class  OrganizationThread implements Callable<Void> {


    public Integer orgsThreadId;


    public Integer getOrgsThreadId() {
        return orgsThreadId;
    }




}

