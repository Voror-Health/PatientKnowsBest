package org.endeavourhealth.patientfhirextractor.process;


import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.dal.CIEJdbcDAL;
import org.endeavourhealth.patientfhirextractor.helper.CIESenderUtil;
import org.endeavourhealth.patientfhirextractor.models.AuditLog;
import org.endeavourhealth.patientfhirextractor.models.CIEExtractorConfig;
import org.endeavourhealth.patientfhirextractor.models.OrgData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.sql.Date;
import java.util.List;


public class CIEReProcess {

    private static final Logger LOG = LoggerFactory.getLogger(CIEReProcess.class);
    public CIEExtractorConfig cieExtractorConfig;

    public CIEReProcess(CIEExtractorConfig cieExtractorConfig) {
        this.cieExtractorConfig=cieExtractorConfig;
    }

    public Response process(Date... date) throws Exception {

        try(CIEJdbcDAL cieExtractsDal = new CIEJdbcDAL()) {
            //Prasahnth:  need to get the connection as line hl7api
            cieExtractsDal.setCieProcessConnection();
            List<AuditLog> auditLogList = null;

            if(date.length > 0){
                auditLogList = cieExtractsDal.getReprocessRecords(date[0], date[1]);
            }else{
                auditLogList = cieExtractsDal.getReprocessRecords();
            }

            LOG.debug("Reprocess list size:" + auditLogList.size());
            for (AuditLog audit:auditLogList) {
                String fhirString = audit.getLogString();
                String resourceName = audit.getResourceType();
                String result = "";
                String response="";

                try {
                    if (audit.getHttpMethod().equals("POST") || audit.getHttpMethod().equals("PUT")) {
                       /* if (resourceName.equals(ResourceConstants.NewPatient)) {
                            JSONParser parser = new JSONParser();
                            JSONObject fhirJson = (JSONObject) parser.parse(fhirString);
                            Object identifier = fhirJson.get("identifier");
                            org.json.simple.JSONArray array = (JSONArray) identifier;
                            JSONObject identifierValue = (JSONObject) parser.parse((array.get(0)).toString());
                            String nhsNumber = identifierValue.get("value").toString();
                            if (cieExtractsDal.checkNewPatientResource(nhsNumber, audit.getOrgName())) {
                                OrgData orgList = cieExtractsDal.getPublicOrgId(audit.getOrgName());
                                response = CIESenderUtil.sendResource(fhirString, ResourceConstants.NewPatient, "POST", cieExtractsDal, orgList.getPublicOrg(), orgList.getRegAuthAccess());
                                if (response.equals("success")) {
                                    cieExtractsDal.saveNewPatientReference(nhsNumber, audit.getOrgName());
                                    cieExtractsDal.updateAuditLog(audit.getId(), "N");
                                }
                            } else {
                                cieExtractsDal.updateAuditLog(audit.getId(), "N");
                            }
                        } else { */
                       result = CIESenderUtil.sendResource(fhirString, resourceName, audit.getHttpMethod(), cieExtractsDal, null, null);
                  //  }
                } else {
                        if (audit.getHttpMethod().equals("DELETE")) {
                            result = CIESenderUtil.deleteResource(audit.getResourceUUID(), resourceName);
                         }
                        }
                if (result.equals("success")) {
                            cieExtractsDal.updateAuditLog(audit.getId(), "N");
                            cieExtractsDal.updateReference(audit.getResourceUUID(), resourceName);
                 }
                }catch(Exception e){
                    LOG.debug("Exception in Reprocess for local_id:"+audit.getId()+":uuid :"+audit.getResourceUUID());
                    e.printStackTrace();
                }
                //}
            }
            //cieExtractsDal.close();
        }
        Response response = Response
                .status(Response.Status.OK).build();
        return response;
    }


}
