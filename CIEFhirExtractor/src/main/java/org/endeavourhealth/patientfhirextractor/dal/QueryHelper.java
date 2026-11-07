package org.endeavourhealth.patientfhirextractor.dal;

import java.sql.Date;

public class QueryHelper {
    public static String createIsInProgress(String type) {
      /*  if(type.equalsIgnoreCase("REBULK")) type="bulk";
        return "SELECT status FROM cie_process_status where process_type like '%" + type + "%'";*/

        return "SELECT status FROM cie_process_status where process_type = '"+ type+"'";

    }

    public static String getAuditLogErrorsQuery(Date[] date, String processType, boolean isReprocess) {
        String sql="";
        if(isReprocess)
        sql = "SELECT id,resource_type,resource_local_id,resource_uuid,log_string,config_id,org_name," +
                "reprocess_required,http_method,updated_datetime,reason FROM cie_audit_log a" +
                " Where  reprocess_required ='Y' " +
                ((date.length >0)? " AND date(a.updated_datetime) BETWEEN '" + date[0] + "' AND '" + date[1] + "'":"" )+
                "  ORDER BY a.resource_local_id ASC,\n" +
                "          a.http_method ASC,\n" +
                "          a.updated_datetime ASC;";

        else
        {
            sql = "SELECT id,resource_type,resource_local_id,resource_uuid,log_string,config_id,org_name," +
                    "reprocess_required,http_method,updated_datetime,reason FROM cie_audit_log a" +
                    " Where  reprocess_required ='N' " +
//            ((null!=processType)? " AND processType='"+processType+"' " :"" )+
                    ((date.length >0)? " AND date(a.updated_datetime) BETWEEN '" + date[0] + "' AND '" + date[1] + "'":"" )+
                    "  ORDER BY a.resource_local_id ASC,\n" +
                    "          a.http_method ASC,\n" +
                    "          a.updated_datetime ASC;";

        }
        return sql;
    }
}
