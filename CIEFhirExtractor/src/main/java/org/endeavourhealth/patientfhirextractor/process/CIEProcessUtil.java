package org.endeavourhealth.patientfhirextractor.process;

import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.endpoints.CIEFhirExtractorEndpoint;

import javax.ws.rs.core.Response;

public class CIEProcessUtil {
    public static Response stopIfUserInterrupted(String processType, CIEJdbcDAL cieExtractsDal) throws Exception {

        if(CIEFhirExtractorEndpoint.isInterrupted(processType))
        {
            cieExtractsDal.setProcessStatus(processType,"Interrupted");
            return Response.status(503).entity(processType + " Process User Interrupted").build();
        }
        return null;
    }
}
