package org.endeavourhealth.patientfhirextractor.dal;

import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.models.AuditLog;
import org.endeavourhealth.patientfhirextractor.models.OrgData;
import org.endeavourhealth.patientfhirextractor.models.OrgProcess;
import org.endeavourhealth.patientfhirextractor.models.SpecialityFull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CIEJdbcDAL implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CIEJdbcDAL.class);

    private Connection connection = null;

    public  SpecialityFull getSpeciality(String rType, String orgId) throws SQLException {
        SpecialityFull spl=null;
        String sql = "SELECT speciality ,code FROM cie_data_extract.appointment_speciality where org=? and rota_type=?";
        try (PreparedStatement sqlStatement = connection.prepareStatement(sql)) {
            sqlStatement.setString(1, orgId);
            sqlStatement.setString(2, rType);
           try(ResultSet rs = sqlStatement.executeQuery()) {
               if (rs.next()) {
                   spl = new SpecialityFull(rs.getString("speciality"), rs.getString("code"));
               }
           }
        }
        return spl;
    }

    public void setCieProcessConnection() throws Exception {
        connection = ConnectionManager.getSubscriberConnection("cie_data_extract");
        if (connection == null) {
            LOG.info("Connection has not been found");
            throw new Exception("Connection has not been found");
        }
    }

    public boolean isInProgress(String type) throws Exception {
        boolean flag = false;
        boolean no_recFound=true;
        //select the bulk record
         try (PreparedStatement sqlStatement = connection.prepareStatement(QueryHelper.createIsInProgress(type))) {
            try(ResultSet rs = sqlStatement.executeQuery()) {
                if (rs.next()) {
                    no_recFound = false;
                    int col = 1;
                    if (null != rs.getString(col) && rs.getString(col).equalsIgnoreCase("InProgress")) {
                        flag = true;
                    }
                }
            }
        }
         if(no_recFound)
         {
             createProcessRec(type);
         }
        return flag;
    }

    public void createProcessRec(String process_name) throws Exception {
        int orgId = 0;
        String[] returnId = {"id"};
        String sql = "INSERT INTO cie_process_status (process_type,status,start_datetime)"
                + " VALUES (?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setString(1, process_name);
            stmt.setString(2, "InProgress");
            stmt.setTimestamp(3, now);

            stmt.executeUpdate();
            connection.commit();

        }

    }

    public int getProcessId(String processType) throws Exception {
        int id = 0;
        //select the bulk record
        String sql = "SELECT id FROM cie_process_status where process_type='" + processType + "'";

        try (PreparedStatement sqlStatement = connection.prepareStatement(sql)) {
            try(ResultSet rs = sqlStatement.executeQuery()) {
                while (rs.next()) {
                    id = rs.getInt(1);
                }
            }
        }
        return id;
    }

    public void setProcessStatus(String type,String status) throws Exception {
        String sql="";
        if(status.equalsIgnoreCase("InProgress")) {
             sql = "UPDATE cie_process_status SET status=?, start_datetime= ?,end_datetime= NULL WHERE process_type=?";
        }
        else
        {
             sql = "UPDATE cie_process_status SET status=?, end_datetime= ? WHERE process_type=?";
        }
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setTimestamp(2, now);
            stmt.setString(3, type);
            stmt.executeUpdate();
            connection.commit();
        }
    }


    // Pass config name, if config name exists, returns status as 'active' or 'Inactive' else returns empty string.
    public String getConfigNameStatus(String configName) throws SQLException {
        String status = "";
        //select the bulk record
        String sql = "SELECT status FROM cie_subscriber_config where config_name=?";

        try (PreparedStatement sqlStatement = connection.prepareStatement(sql)) {
            sqlStatement.setString(1, configName);
            try(ResultSet rs = sqlStatement.executeQuery()) {
                if (rs.next()) {
                    status = rs.getString(1);
                }
            }
        }
        return status;
    }

    //
    private void setConfigNameStatus(String configName, String status) throws SQLException {
        String sql = "UPDATE cie_subscriber_config SET status= ? WHERE config_name=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, configName);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void setConfigNameToActive(String configName) throws SQLException {
        setConfigNameStatus(configName, "Active");
    }

    public void setRemainingConfigNameToInactive(Set<String> configNames) throws SQLException {
        String sql = "UPDATE cie_subscriber_config SET status='Inactive'" +
                "where config_name not in ('" + StringUtils.join(configNames, "','") + "') ";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void saveNewConfigNameAsActive(String configName) throws SQLException {
        String sql = "INSERT INTO cie_subscriber_config (config_name,status)"
                + " VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, configName);
            stmt.setString(2, "Active");
            stmt.executeUpdate();

            connection.commit();
        }
    }

    public int newSubscriberThread(int process_id, String config_name) throws Exception {
        int threadId = 0;
        String[] returnId = {"id"};
        String sql = "INSERT INTO cie_subscribercfg_thread_status (cie_process_id, start_datetime, status)"
                + " VALUES (?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setInt(1, process_id);
            stmt.setTimestamp(2, now);
            stmt.setString(3, "Started");

            stmt.executeUpdate();
            connection.commit();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    threadId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Subscriber thread save Failed");
                }
            }
        }
        return threadId;
    }

    public void subscriberThreadCompleted(int id) throws Exception {
        String sql = "UPDATE cie_subscribercfg_thread_status SET end_datetime= ?, status= ? WHERE id=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, "Completed");
            stmt.setInt(3, id);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void subscriberThreadErrored(int id) throws Exception {
        String sql = "UPDATE cie_subscribercfg_thread_status SET end_datetime= ?, status= ? WHERE id=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, "Errored");
            stmt.setInt(3, id);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    /*public void subscriberThreadInterupted(String id) throws Exception {
        String sql = "UPDATE cie_subscribercfg_thread_status SET end_time= ?, status= ? WHERE id=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, "Interrupted");
            stmt.setString(3, id);
            stmt.executeUpdate();
            connection.commit();
        }
    }*/

    public int newOrgsThread(int cie_subscribercfg_thread_id) throws Exception {
        int threadId = 0;
        String[] returnId = {"id"};
        String sql = "INSERT INTO cie_orgs_thread_status (cie_subscribercfg_thread_id, start_datetime, status)"
                + " VALUES (?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setInt(1, cie_subscribercfg_thread_id);
            stmt.setTimestamp(2, now);
            stmt.setString(3, "Started");

            stmt.executeUpdate();
            connection.commit();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    threadId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("orgs thread save Failed");
                }
            }
        }
        return threadId;
    }

    public void orgsThreadCompleted(int id) throws Exception {
        String sql = "UPDATE cie_orgs_thread_status SET end_datetime= ?, status= ? WHERE id=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, "Completed");
            stmt.setInt(3, id);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void orgsThreadErrored(int id) throws Exception {
        String sql = "UPDATE cie_orgs_thread_status SET end_datetime= ?, status= ? WHERE id=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, "Errored");
            stmt.setInt(3, id);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void newOrgProcess(int cie_orgs_thread_id, String ods_code, java.sql.Timestamp start_time, java.sql.Timestamp end_time, String status) throws Exception {
        String sql = "INSERT INTO cie_org_process_status (cie_orgs_thread_id, ods_code, start_datetime, end_datetime, status)"
                + " VALUES (?, ?, ? , ?, ?)"
                + " ON DUPLICATE KEY UPDATE"
                + " cie_orgs_thread_id = VALUES(cie_orgs_thread_id), start_datetime = VALUES(start_datetime), end_datetime = VALUES" +
                "(end_datetime), status = VALUES(status)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, cie_orgs_thread_id);
            stmt.setString(2, ods_code);
            stmt.setTimestamp(3, start_time);
            stmt.setTimestamp(4, end_time);
            stmt.setString(5, status);

            stmt.executeUpdate();
            connection.commit();
        } catch (Exception e) {
               throw new Exception(e);
        }
    }

  /*  public void orgBulkProcessCompleted(String org, java.sql.Timestamp end_time) throws Exception {
        String sql = "UPDATE cie_org_process_status SET end_datetime= ?, status= ? WHERE ods_code=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, end_time);
            stmt.setString(2, "Completed");
            stmt.setString(3, org);
            stmt.executeUpdate();
            connection.commit();
        }
    }*/

    /*public void orgProcessCompleted(String org) throws Exception {
        String sql = "UPDATE cie_org_process_status SET end_datetime= ?, status= ? WHERE ods_code=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, "Completed");
            stmt.setString(3, org);
            stmt.executeUpdate();
            connection.commit();
        }
    }*/

    public OrgProcess getOrgProcess(String org) throws Exception {
        String sql = "SELECT ods_code as org, cie_orgs_thread_id as orgThreadId, start_datetime as startDateTime, end_datetime as endDateTime, status as status FROM cie_org_process_status where ods_code=?";
        OrgProcess orgProcess = new OrgProcess();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, org);
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    orgProcess
                            .setOrg(resultSet.getString("org"))
                            .setOrgThreadsId(resultSet.getInt("orgThreadId"))
                            .setStartDatetime(resultSet.getTimestamp("startDateTime"))
                            .setEndDatetime(resultSet.getTimestamp("endDateTime"))
                            .setStatus(resultSet.getString("status"));
                }
            }
        }
        return orgProcess;
    }

    /*public void updateOrgProcess(String org, Date startDateTime) throws Exception {
        String sql = "UPDATE cie_org_process_status SET end_datetime= ?, status= ? WHERE ods_code=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, "Errored");
            stmt.setString(3, org);
            stmt.executeUpdate();
            connection.commit();
        }
    }*/

    public void updateOrgProcessStatus(String org,String status) throws Exception {
        String sql = "UPDATE cie_org_process_status SET end_datetime= ?, status= ? WHERE ods_code=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, status);
            stmt.setString(3, org);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public boolean isInOrgCohort(String ods_code,String status) throws SQLException {
        boolean flag = false;
        String sql="";
        //select the bulk record
        if(null!=status) {
            sql = "SELECT ods_code FROM cie_org_cohorts where ods_code=? and status='"+status+"'";
        }
        else
        {
            sql = "SELECT ods_code FROM cie_org_cohorts where ods_code=?";
        }

        try (PreparedStatement sqlStatement = connection.prepareStatement(sql)) {
            sqlStatement.setString(1, ods_code);
            try(ResultSet rs = sqlStatement.executeQuery()) {
                if (rs.next()) {
                    flag = true;
                }
            }
        }
        return flag;
    }

    public void orgCohortCompleted(String ods_code,int configId) throws Exception {

        String sql = "INSERT INTO cie_org_cohorts (cie_subscriber_config_id,ods_code,updated_datetime, status)"
                + " VALUES (?,?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, configId);
            stmt.setString(2, ods_code);
            stmt.setTimestamp(3, now);
            stmt.setString(4, "Active");

            stmt.executeUpdate();
            connection.commit();
        }

    }

    public String checkReferenceExistsForOrg(String organizationName) throws SQLException {
        String rerefenceUUID = "";
        //select the bulk record
        String sql = "SELECT resource_uuid FROM cie_org_references where organization_name=?";

        try (PreparedStatement sqlStmt = connection.prepareStatement(sql)) {
            sqlStmt.setString(1, organizationName);
            try(ResultSet rs = sqlStmt.executeQuery()) {
                while (rs.next()) {
                    int col = 1;
                    rerefenceUUID = rs.getString(col);
                }
            }
        }
        return rerefenceUUID;
    }

    public int saveOrganizationReference(String resource_uuid, String ods_code) throws Exception {
        int orgId = 0;
        String[] returnId = {"id"};
        String sql = "INSERT INTO cie_org_references (resource_uuid, organization_name, updated_datetime)"
                + " VALUES (?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setString(1, resource_uuid);
            stmt.setString(2, ods_code);
            stmt.setTimestamp(3, now);

            stmt.executeUpdate();
            connection.commit();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    orgId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("New Organization reference save failed.");
                }
            }
        }
        return orgId;
    }
    // Need to return practitioner role for appointment resource as per requirement
    public String checkReferenceExistsForResource(String id, String resource_type, String org_name,int configID) throws SQLException {
        String referenceUUID = "";
        String tableName = "";
        // create an enum or hashmap to get the table name based on the resource_type
        switch (resource_type) {
            case ResourceConstants.Patient:
                tableName = ResourceConstants.CIEPatientTable;
                break;
            case ResourceConstants.Condition:
                tableName = ResourceConstants.CIEConditionTable;
                break;
            case ResourceConstants.Medication:
                tableName = ResourceConstants.CIEMedicationTable;
                break;
            case ResourceConstants.MedicationStatement:
                tableName = ResourceConstants.CIEMedicationStatementTable;
                break;
            case ResourceConstants.Practitioner:
                tableName = ResourceConstants.CIEPractitionerTable;
                break;
            case ResourceConstants.Appointment:
                tableName = ResourceConstants.CIEAppointmentTable;
                break;
        }
        String sql = "SELECT resource_uuid FROM $tableName where local_id=? and organization_name=? and config_id =?";

        String query = sql.replace("$tableName", tableName);

        try (PreparedStatement sqlStmt = connection.prepareStatement(query)) {
            sqlStmt.setString(1, id);
            sqlStmt.setString(2, org_name);
            sqlStmt.setInt(3, configID);
            try(ResultSet rs = sqlStmt.executeQuery()) {
                while (rs.next()) {
                    int col = 1;
                    referenceUUID = rs.getString(col);
                }
            }
        }
        return referenceUUID;
    }
/*
    public boolean checkNewPatientResource(String nhsNumber, String ods_code) throws SQLException {
        String id = "";
        String tableName = ResourceConstants.CIENewPatientTable;

        String sql = "SELECT id FROM $tableName where nhs_number=? and ods_code=?";
        String query = sql.replace("$tableName", tableName);

        try (PreparedStatement sqlStmt = connection.prepareStatement(query)) {
            sqlStmt.setString(1, nhsNumber);
            sqlStmt.setString(2, ods_code);
            try(ResultSet rs = sqlStmt.executeQuery()) {
                while (rs.next()) {
                    int col = 1;
                    id = rs.getString(col);
                }
            }
        }
        return id.isEmpty();
    }

    public int saveNewPatientReference(String nhs_number, String ods_code) throws Exception {
        int referenceId = 0;
        String[] returnId = {"id"};
        String tableName = ResourceConstants.CIENewPatientTable;
        String sql = "INSERT INTO " + tableName + "(nhs_number, ods_code, updated_datetime)"
                + " VALUES (?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setString(1, nhs_number);
            stmt.setString(2, ods_code);
            stmt.setTimestamp(3, now);

            stmt.executeUpdate();
            connection.commit();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    referenceId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("New reference save failed.");
                }
            }
        }
        return referenceId;
    }
 */

    public List<CIEReference> getReferences(String resource_type, String org_name, int config_Id) throws SQLException {
           String tableName = "";
        // create a enum or hashmap to get the table name based on the resource_type
        switch (resource_type) {
            case ResourceConstants.Patient:
                tableName = ResourceConstants.CIEPatientTable;
                break;
            case ResourceConstants.Condition:
                tableName = ResourceConstants.CIEConditionTable;
                break;
            case ResourceConstants.Medication:
                tableName = ResourceConstants.CIEMedicationTable;
                break;
            case ResourceConstants.MedicationStatement:
                tableName = ResourceConstants.CIEMedicationStatementTable;
                break;
            case ResourceConstants.Practitioner:
                tableName = ResourceConstants.CIEPractitionerTable;
                break;
            case ResourceConstants.Appointment:
                tableName = ResourceConstants.CIEAppointmentTable;
                break;
        }
        String sql = "SELECT local_id , resource_uuid, config_id  " +
                     "FROM $tableName " +
                     " WHERE  organization_name=? AND config_id=?";

        String query = sql.replace("$tableName", tableName);
        ArrayList<CIEReference> reflist=new ArrayList<CIEReference>();
        try (PreparedStatement sqlStmt = connection.prepareStatement(query)) {
               sqlStmt.setString(1, org_name);
               sqlStmt.setInt(2,config_Id);
               try(ResultSet rs = sqlStmt.executeQuery()) {

                   while (rs.next()) {
                       CIEReference cieref = new CIEReference();
                       cieref.setLocal_id(rs.getLong("local_id"));
                       cieref.setRefUUID(rs.getString("resource_uuid"));
                       cieref.setConfig_id(rs.getInt("config_id"));
                       reflist.add(cieref);
                   }
               }
        }
        return reflist;
    }

    public int saveReference(String resource_uuid, long local_id, String ods_code, String resource_type,int config_id) throws Exception {
        int referenceId = 0;
        String[] returnId = {"id"};
        String tableName = "";
        switch (resource_type) {
            case ResourceConstants.Patient:
                tableName = ResourceConstants.CIEPatientTable;
                break;
            case ResourceConstants.Condition:
                tableName = ResourceConstants.CIEConditionTable;
                break;
            case ResourceConstants.Medication:
                tableName = ResourceConstants.CIEMedicationTable;
                break;
            case ResourceConstants.MedicationStatement:
                tableName = ResourceConstants.CIEMedicationStatementTable;
                break;
            case ResourceConstants.Appointment:
                tableName = ResourceConstants.CIEAppointmentTable;
                break;
        }
        String sql = "INSERT INTO " + tableName + "(resource_uuid, local_id, organization_name, updated_datetime,config_id)"
                + " VALUES (?, ?, ?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setString(1, resource_uuid);
            stmt.setLong(2, local_id);
            stmt.setString(3, ods_code);
            stmt.setTimestamp(4, now);
            stmt.setInt(5, config_id);

            stmt.executeUpdate();
            connection.commit();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    referenceId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("New reference save failed.");
                }
            }
        }
        return referenceId;
    }

    public void  savePractitionerReference(String resource_uuid, String role_uuid, long local_id, String organization_name,int config_id) throws Exception {

        String sql = "INSERT INTO cie_practitioner_reference (resource_uuid, role_uuid, local_id, organization_name, updated_datetime, config_id)"
                + " VALUES (?, ?, ?, ?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, resource_uuid);
            stmt.setString(2, role_uuid);
            stmt.setLong(3, local_id);
            stmt.setString(4, organization_name);
            stmt.setTimestamp(5, now);
            stmt.setInt(6, config_id);

            stmt.executeUpdate();
            connection.commit();


        }

    }
    public void saveMedicationReference(String resource_uuid, String mcode, String ods_code, long configId) throws Exception {
        String sql = "INSERT INTO cie_medication_reference (resource_uuid, local_id, organization_name, updated_datetime,config_id)"
                + " VALUES (?, ?, ?,?,?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, resource_uuid);
            stmt.setString(2, mcode);
            stmt.setString(3, ods_code);
            stmt.setTimestamp(4, now);
            stmt.setLong(5, configId);
            stmt.executeUpdate();
            connection.commit();

        }

    }

    public boolean removeReferenceForResource(String id, String resource_type, String org_name) throws SQLException {
        boolean deleted = false;
        String tableName = "";
        // create a enum or hashmap to get the table name based on the resource_type
        switch (resource_type) {
            case ResourceConstants.Patient:
                tableName = ResourceConstants.CIEPatientTable;
                break;
            case ResourceConstants.Condition:
                tableName = ResourceConstants.CIEConditionTable;
                break;
            case ResourceConstants.Medication:
                tableName = ResourceConstants.CIEMedicationTable;
                break;
            case ResourceConstants.MedicationStatement:
                tableName = ResourceConstants.CIEMedicationStatementTable;
                break;
            case ResourceConstants.Practitioner:
                tableName = ResourceConstants.CIEPractitionerTable;
                break;
            case ResourceConstants.Appointment:
                tableName = ResourceConstants.CIEAppointmentTable;
                break;
        }
        String sql = "DELETE FROM $tableName where local_id=? and organization_name=?";

        String query = sql.replace("$tableName", tableName);

        try (PreparedStatement sqlStmt = connection.prepareStatement(query)) {
            sqlStmt.setString(1, id);
            sqlStmt.setString(2, org_name);
            int rowCount = sqlStmt.executeUpdate();
            if (rowCount > 0) {
                deleted = true;
            }
        }
        return deleted;
    }

    public boolean updateReference(String resource_uuid, String resource_type) throws Exception {
        boolean updated = false;
        String[] returnId = {"id"};
        String tableName = "";
        // create a enum or hashmap to get the table name based on the resource_type
        switch (resource_type) {
            case ResourceConstants.Organization:
                tableName = ResourceConstants.CIEOrganizationTable;
                break;
            case ResourceConstants.Patient:
                tableName = ResourceConstants.CIEPatientTable;
                break;
            case ResourceConstants.Condition:
                tableName = ResourceConstants.CIEConditionTable;
                break;
            case ResourceConstants.Medication:
                tableName = ResourceConstants.CIEMedicationTable;
                break;
            case ResourceConstants.MedicationStatement:
                tableName = ResourceConstants.CIEMedicationStatementTable;
                break;
            case ResourceConstants.Practitioner:
                tableName = ResourceConstants.CIEPractitionerTable;
                break;
            case ResourceConstants.Appointment:
                tableName = ResourceConstants.CIEAppointmentTable;
                break;
        }

        String sql = "UPDATE "+ tableName +" SET updated_datetime=? where resource_uuid=?";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, resource_uuid);

            int row = stmt.executeUpdate();

            if (row > 0) {
                updated = true;
            } else {
                throw new SQLException("New reference save failed for table:"+tableName);
            }
            connection.commit();
        }
        return updated;
    }

    @Override
    public void close() throws Exception {
        LOG.info("closing the CIEJdbcDal");
        if (connection != null) {
            this.connection.close();
        }
    }

    public List<AuditLog> getReprocessRecords(java.sql.Date... date) throws SQLException {
        List<AuditLog> result = new ArrayList<AuditLog>();
        String sql=QueryHelper.getAuditLogErrorsQuery(date,null,true);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getAuditLogs(resultSet);
            }
        }
        return result;
    }

    public List<AuditLog> getAuditLogs(ResultSet resultSet) throws SQLException {
        List<AuditLog> auditLogs = new ArrayList<AuditLog>();
        while (resultSet.next()) {
            AuditLog audit = new AuditLog();
            audit
                    .setId(resultSet.getInt("id"))
                    .setResourceType(resultSet.getString("resource_type"))
                    .setResourceLocalId(resultSet.getLong("resource_local_id"))
                    .setResourceUUID(resultSet.getString("resource_uuid"))
                    .setLogString(resultSet.getString("log_string"))
                    .setConfigID(resultSet.getInt("config_id"))
                    .setOrgName(resultSet.getString("org_name"))
                    .setReprocessRequired(resultSet.getString("reprocess_required"))
                    .setHttpMethod(resultSet.getString("http_method"))
                    .setUpdatedDateTime(resultSet.getTimestamp("updated_datetime"))
                    .setReason(resultSet.getString("reason"));
            auditLogs.add(audit);
        }
        return auditLogs;
    }

    //Adding changes part of re-process requirement.
    public void createAuditEntry(AuditLog auditlog) throws Exception {
          String sql = "INSERT INTO cie_audit_log(resource_type, resource_local_id," +
                "resource_uuid, log_string, config_id, org_name," +
                "reprocess_required, http_method, updated_datetime, reason)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, auditlog.getResourceType());
            stmt.setLong(2, auditlog.getResourceLocalId());
            stmt.setString(3, auditlog.getResourceUUID());
            stmt.setString(4, auditlog.getLogString());
            stmt.setInt(5, auditlog.getConfigID());
            stmt.setString(6, auditlog.getOrgName());
            stmt.setString(7, auditlog.getReprocessRequired());
            stmt.setString(8, auditlog.getHttpMethod());
            stmt.setTimestamp(9, auditlog.getUpdatedDateTime());
            stmt.setString(10, auditlog.getReason());
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void updateAuditLog(Integer auditId, String reprocessRequired ) throws Exception {
        String sql = "UPDATE cie_audit_log SET reprocess_required=? , updated_datetime= ? WHERE id=?";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, reprocessRequired);
            stmt.setTimestamp(2, now);
            stmt.setInt(3, auditId);
            stmt.executeUpdate();
            connection.commit();
        }
    }

    public void orgRebulkProcessCompleted(String org, Timestamp rebulkStartTime) {
        //need to implement later
    }

    public int getConfigId(String configName) throws SQLException {
            int configId = -1;
            //select the bulk record
            String sql = "SELECT id FROM cie_subscriber_config where config_name=?";

            try (PreparedStatement sqlStatement = connection.prepareStatement(sql)) {
                sqlStatement.setString(1, configName);
                try(ResultSet rs = sqlStatement.executeQuery()) {
                    if (rs.next()) {
                        configId = rs.getInt(1);
                    }
                }
            }
            return configId;
        }

    public void syncOrgCohortforDelta(int configId, List<String> odsCodes) throws SQLException {
        //need to inactive if cohort ods code is active and  not available in odscodes list
        //fetch acitve odscodes from syncOrgCohort
        List<String> ods_codes=getCohortOdsCodes(configId,"Active");
       for(String ods_code:ods_codes)
       {
                if(!odsCodes.contains(ods_code))
                {
                    updateOrgCohort(configId,ods_code,"Inactive");
                }

            }
      }

    public void syncOrgCohortForDeltaRC(int configId, List<String> odsCodes) throws SQLException {
        //need to inactive if cohort ods code is active and  not available in odscodes list
        //fetch acitve odscodes from syncOrgCohort
        List<String> ods_codes=getCohortOdsCodes(configId,"Inactive");
        for(String ods_code:ods_codes)
        {
            if(odsCodes.contains(ods_code))
            {
                updateOrgCohort(configId,ods_code,"Reconsented");
            }

        }
    }

    public void syncOrgCohortForReBulk(int configId, List<String> odsCodes) throws SQLException {
        //need to inactive if cohort ods code is active and  not available in odscodes list
        //fetch acitve odscodes from syncOrgCohort
        List<String> ods_codes=getCohortOdsCodes(configId,"Active");
        for(String ods_code:ods_codes)
        {
            if(odsCodes.contains(ods_code))
            {
                updateOrgCohort(configId,ods_code,"ReBulk");
            }

        }
    }

    private List<String> getCohortOdsCodes(int configId, String status) throws SQLException {
        List<String> odsList=new ArrayList<String>();
        String sql= "SELECT ods_code FROM cie_org_cohorts where cie_subscriber_config_id=? and  status=?";
        try (PreparedStatement sqlStatement = connection.prepareStatement(sql)) {
            sqlStatement.setInt(1,configId);
            sqlStatement.setString(2,status);
            try(ResultSet rs = sqlStatement.executeQuery()) {
                while (rs.next()) {
                    odsList.add(rs.getString("ods_code"));
                }
            }
        }
        return odsList;
    }

    public void updateOrgCohort(int configId, String ods_code, String status) throws SQLException {
        String sql = "UPDATE cie_org_cohorts SET status=? , updated_datetime= ? WHERE cie_subscriber_config_id=? and ods_code=? ";
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1,status );
            stmt.setTimestamp(2, now);
            stmt.setInt(3, configId);
            stmt.setString(4,ods_code );
            stmt.executeUpdate();
            connection.commit();
        }
    }
    public  void syncSubscriberConfigIds(Set<String> configNames) throws SQLException {
        LOG.debug("ConfigNames"+configNames);
        for (String configName : configNames) {
            String configStatus = getConfigNameStatus(configName);
            if(configStatus.isEmpty()){
                saveNewConfigNameAsActive(configName);
            }
            else if(configStatus.equals("Inactive")){
                setConfigNameToActive(configName);
            }
        }
        setRemainingConfigNameToInactive(configNames);
    }

    public String checkRoleResource(String id,  String orgName,int configId) throws SQLException {
        String referenceUUID = "";
        String sql = "SELECT role_uuid FROM cie_practitioner_reference where local_id=? and organization_name=? and config_id=?";
        try (PreparedStatement sqlStmt = connection.prepareStatement(sql)) {
            sqlStmt.setString(1, id);
            sqlStmt.setString(2, orgName);
            sqlStmt.setInt(3, configId);
            try(ResultSet rs = sqlStmt.executeQuery()) {
                while (rs.next()) {
                    int col = 1;
                    referenceUUID = rs.getString(col);
                }
            }
        }
        return referenceUUID;
    }

    //prasahnth3: need to implement method
    public List<AuditLog> getErrorsList(Date startDateTime, Date endDateTime,String  processType) throws Exception
    {
        List<AuditLog> result = null;
        Date[] date=new Date[2];
        date[0]=startDateTime;
        date[1]= endDateTime;
        String sql=QueryHelper.getAuditLogErrorsQuery(date,processType,false);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getAuditLogs(resultSet);
            }
        }
        return result;
    }

    public int mockSendToDB(String resourceId, String resource_type, String fhir) throws Exception{
        int id=0;
        String[] returnId = { "id" };
        String sql = "INSERT IGNORE INTO resource_fhir_data (resource_id, resource_type, fhir_obj, datetime)"
                + " VALUES (?, ?, ?, ?)";

        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setString (1, resourceId);
            stmt.setString (2, resource_type);
            stmt.setString(3,fhir);
            stmt.setTimestamp(4, now);

            stmt.executeUpdate();
            connection.commit();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    id = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("New Fhir reference save failed.");
                }
            }
        }
        return id;
    }

    public boolean updateReference(String resource_uuid, String resource_type, String resource_data) throws Exception{
        boolean updated=false;
        String[] returnId = { "id" };

        String sql = "UPDATE resource_fhir_data SET datetime=?, fhir_obj=? where resource_id=? and resource_type=?";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, resource_data);
            stmt.setString(3,resource_uuid);
            stmt.setString(4,resource_type);

            int row = stmt.executeUpdate();

            if (row>0) {
                updated=true;
            }
            else {
                throw new SQLException("Reference update is failed.");
            }
            connection.commit();
        }
        return updated;
    }

    public Boolean checkIfReferenceExists(String resource_id, String resource_type) throws SQLException {
        boolean exists= false;
        String sql = "SELECT resource_id FROM resource_fhir_data where resource_id=? and resource_type=?";

        try (PreparedStatement sqlStmt = connection.prepareStatement(sql)){
            sqlStmt.setString(1, resource_id);
            sqlStmt.setString(2, resource_type);
            try(ResultSet rs = sqlStmt.executeQuery()) {
                while (rs.next()) {
                    int col = 1;
                    exists = true;
                }
            }
        }
        return exists;
    }

    public int saveConditionReference(String resource_uuid, long local_id, String ods_code,String status, long patient_id,long non_core_concept_id,int config_id) throws Exception {
        int referenceId = 0;
        String[] returnId = {"id"};
        String sql = "INSERT INTO " + "cie_condition_reference" + "(resource_uuid, local_id, organization_name, updated_datetime,config_id,status,patient_id,non_core_concept_id)"
                + " VALUES (?, ?, ?, ?, ?,?,?,?)";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setString(1, resource_uuid);
            stmt.setLong(2, local_id);
            stmt.setString(3, ods_code);
            stmt.setTimestamp(4, now);
            stmt.setInt(5, config_id);
            stmt.setString(6, status);
            stmt.setLong(7, patient_id);
            stmt.setLong(8, non_core_concept_id);


            stmt.executeUpdate();
            connection.commit();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    referenceId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("New reference save failed.");
                }
            }
        }
        return referenceId;
    }

    public boolean updateConditionReference(String resource_uuid, String status,long patient_id,long non_core_concept_id) throws Exception {
        boolean updated = false;
        String[] returnId = {"id"};

        String sql = "UPDATE "+ "cie_condition_reference" +" SET updated_datetime=?,status=?,patient_id=?,non_core_concept_id=? where resource_uuid=?";

        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (PreparedStatement stmt = connection.prepareStatement(sql, returnId)) {
            stmt.setTimestamp(1, now);
            stmt.setString(2, status);
            stmt.setLong(3, patient_id);
            stmt.setLong(4, non_core_concept_id);
            stmt.setString(5, resource_uuid);

            int row = stmt.executeUpdate();

            if (row > 0) {
                updated = true;
            } else {
                throw new SQLException("New reference save failed for table: cie_condition_reference");
            }
            connection.commit();
        }
        return updated;
    }

    public  CIEReference conditionResourceList(long patient_id, long non_core_concept_id, String status) throws SQLException {
        String sql = "SELECT local_id ,resource_uuid, patient_id, non_core_concept_id, status  " +
                        "FROM cie_condition_reference where patient_id=? and non_core_concept_id=? and status=? ";

        CIEReference cieRef = new CIEReference();
        try (PreparedStatement sqlStmt = connection.prepareStatement(sql)){
            sqlStmt.setLong(1, patient_id);
            sqlStmt.setLong(2, non_core_concept_id);
            sqlStmt.setString(3, status);
            try(ResultSet rs = sqlStmt.executeQuery()) {
                while (rs.next()) {

                    cieRef.setLocal_id(rs.getLong("local_id"));
                    cieRef.setRefUUID(rs.getString("resource_uuid"));
                    cieRef.setPatientId(rs.getLong("patient_id"));
                    cieRef.setConceptId(rs.getLong("non_core_concept_id"));
                    cieRef.setStatus(rs.getString("status"));
                }
            }
        }
        return cieRef;
    }

    public OrgData getPublicOrgId(String ods_code) throws SQLException {
        OrgData orgData=new OrgData();

        String sql = "SELECT public_org,reg_auth_access FROM ods_code_mapping where  ods_code=?";

        try (PreparedStatement sqlStmt = connection.prepareStatement(sql)) {
            sqlStmt.setString(1, ods_code);
            try(ResultSet rs = sqlStmt.executeQuery()) {
                while (rs.next()) {
                    orgData.setPublicOrg(rs.getString("public_org"));
                    orgData.setRegAuthAccess(rs.getString("reg_auth_access"));

                }
            }
        }
        return orgData;
    }
}