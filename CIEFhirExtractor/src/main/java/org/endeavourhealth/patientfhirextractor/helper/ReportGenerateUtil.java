package org.endeavourhealth.patientfhirextractor.helper;

import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.models.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.util.List;

public class ReportGenerateUtil {


    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerateUtil.class);

    public static String generateAuditReport(List<AuditLog> auditList) throws Exception{
     LOG.debug("Start of audit report generation");
     FileWriter csvWriter = null;
     try {
         csvWriter = new FileWriter(ResourceConstants.AUDIT_ERROR_REPORT_FILE_LOC);
         csvWriter.append("RESOURCE_LOCAL_ID");
         csvWriter.append(",");
         csvWriter.append("RESOURCE_TYPE");
         csvWriter.append(",");
         csvWriter.append("RESOURCE_UUID");
         csvWriter.append(",");
         csvWriter.append("CONFIG_ID");
         csvWriter.append(",");
         csvWriter.append("ORG_NAME");
         csvWriter.append(",");
         csvWriter.append("HTTP_METHOD");
         csvWriter.append(",");
         csvWriter.append("REASON");
         csvWriter.append("\n");

         for (AuditLog rowData : auditList) {
             csvWriter.append(String.valueOf(rowData.getResourceLocalId()));
             csvWriter.append(",");
             csvWriter.append(rowData.getResourceType());
             csvWriter.append(",");
             csvWriter.append(rowData.getResourceUUID());
             csvWriter.append(",");
             csvWriter.append(String.valueOf(rowData.getConfigID()));
             csvWriter.append(",");
             csvWriter.append(rowData.getOrgName());
             csvWriter.append(",");
             csvWriter.append(rowData.getHttpMethod());
             csvWriter.append(",");
             csvWriter.append(String.format("\"%s\"", rowData.getReason().trim()));
             csvWriter.append("\n");
         }
         csvWriter.flush();
         csvWriter.close();

     }catch(Exception e){
         e.printStackTrace();
     }finally{
         if (csvWriter!=null){
             csvWriter.close();
         }
     }
     LOG.debug("End of audit report generation");
     return ResourceConstants.AUDIT_ERROR_REPORT_FILE_LOC;
    }

}
