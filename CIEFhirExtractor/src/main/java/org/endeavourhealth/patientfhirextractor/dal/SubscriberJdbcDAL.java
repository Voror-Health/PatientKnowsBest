package org.endeavourhealth.patientfhirextractor.dal;


import org.endeavourhealth.core.database.rdbms.ConnectionManager;
import org.endeavourhealth.patientfhirextractor.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SubscriberJdbcDAL implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SubscriberJdbcDAL.class);

    private Connection conn = null;

    public void setSubscriberConnection(String configName) throws Exception {
        conn = ConnectionManager.getSubscriberConnection(configName);
        if (conn == null) {
            LOG.info("Connection has not been found");
            throw new Exception("Connection has not been found");
        }
    }

    public OrganizationFull getOrganizationFull(String organizationName) throws Exception {

        String sql = "select id as id," +
                "coalesce(ods_code,'') as ods_code," +
                "coalesce(name,'') as name," +
                "coalesce(parent_organization_id,'') as parent_organization_id," +
                "coalesce(postcode,'') as postcode  from organization where ods_code= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, organizationName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return getOrganizationFull(resultSet);
            }
        }

        return new OrganizationFull();
    }

    public OrganizationFull getOrganizationFull(long organizationId) throws Exception {

        String sql = "select id as id," +
                "coalesce(ods_code,'') as ods_code," +
                "coalesce(name,'') as name," +
                "coalesce(parent_organization_id,'') as parent_organization_id," +
                "coalesce(postcode,'') as postcode  from organization where id= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, organizationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next())
                    return getOrganizationFull(resultSet);
            }
        }

        return new OrganizationFull();
    }

    public OrganizationFull getOrganizationFull(ResultSet resultSet) throws SQLException {
        OrganizationFull organizationFull = new OrganizationFull();
        organizationFull
                .setId(resultSet.getLong("id"))
                .setOdsCode(resultSet.getString("ods_code"))
                .setName(resultSet.getString("name"))
                .setPostCode(resultSet.getString("postcode"))
                .setParentOrganizationId(resultSet.getLong("parent_organization_id"));
        return organizationFull;
    }

    public List<Long> getPatientIds(long orgId) throws SQLException {
        List<Long> patient_ids= new ArrayList<>();
        String sql = "select id from patient where organization_id= ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, orgId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    patient_ids.add(resultSet.getLong(1));
                }
            }
        }
        return patient_ids;
    }

    public PatientFull getPatientFull(Long id) throws Exception {
        PatientFull result = null;

        String sql = "SELECT p.id,"+
                "coalesce(p.organization_id,'') as orglocation,"+
                "coalesce(o.name,'') as orgname,"+
                "p.date_of_birth as dob,"+
                "p.date_of_death as dod,"+
                "coalesce(c.description, c.name, '') as gender,"+
                "coalesce(p.nhs_number,'') as nhsNumber,"+
                "coalesce(p.last_name,'') as lastname,"+
                "coalesce(p.first_names,'') as firstname,"+
                "coalesce(p.title,'') as title,"+
                "coalesce(a.address_line_1,'') as add1,"+
                "coalesce(a.address_line_2,'') as add2,"+
                "coalesce(a.address_line_3,'') as add3,"+
                "coalesce(a.address_line_4,'') as add4,"+
                " a.start_date as address_sdate, "+
                " a.end_date as address_edate,"+
                "coalesce(a.city,'') as city,"+
                "coalesce(e.usual_gp_practitioner_id,'') as practitionerId,"+
                "coalesce(a.postcode,'') as postcode," +
                "coalesce(c3.description, '') as ethnicDescription," +
                "coalesce(c3.code, '') as ethnicCode," +
                "coalesce(c2.description, '') as registrationStatusValue," +
                "coalesce(e.registration_type_concept_id,'') as registrationType,"+
                "e.date_registered_end as registeredEndDate,"+
                "e.date_registered as startdate,"+
                "'HOME' as adduse,"+
                "'' as otheraddresses, "+
                " o.ods_code as ods_code "+
                "FROM patient p " +
                "join organization o on o.id = p.organization_id " +
                "left join patient_address a on a.id = p.current_address_id " +
                "left join concept c on c.dbid = p.gender_concept_id " +
                "left join episode_of_care e on e.patient_id = p.id "+
                "left join concept c3 on c3.dbid = p.ethnic_code_concept_id "+
                "left join concept c2 on c2.dbid = e.registration_type_concept_id "+
                "where (p.id = ?) ";


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return getPatientFull(resultSet);
                }
            }
        }

        return null;
    }

    public PatientFull getPatientFull(ResultSet resultSet) throws SQLException {
        PatientFull patient = new PatientFull();

        patient
                .setId(resultSet.getLong("id"))
                .setNhsNumber(resultSet.getString("nhsNumber"))
                .setGender(resultSet.getString("gender"))
                .setLastname(resultSet.getString("lastname"))
                .setTitle(resultSet.getString("title"))
                .setFirstname(resultSet.getString("firstname"))
                .setDob(resultSet.getDate("dob"))
                .setDod(resultSet.getDate("dod"))
                .setAdduse(resultSet.getString("adduse"))
                .setAdd1(resultSet.getString("add1"))
                .setAdd2(resultSet.getString("add2"))
                .setAdd3(resultSet.getString("add3"))
                .setAdd4(resultSet.getString("add4"))
                .setPostcode(resultSet.getString("postcode"))
                .setCity(resultSet.getString("city"))
                .setOtheraddresses(resultSet.getString("otheraddresses"))
                .setOrglocation(resultSet.getString("orglocation"))
                .setOrgname(resultSet.getString("orgname"))
                .setPractitionerId(resultSet.getLong("practitionerId"))
                .setAddressStartDate(resultSet.getDate("address_sdate"))
                .setRegistrationEndDate(resultSet.getDate("registeredEndDate"))
                .setAddressEndDate(resultSet.getDate("address_edate"))
                .setRegistrationType(resultSet.getString("registrationType"))
                .setStartdate(resultSet.getDate("startdate"))
                .setRegistrationStatusValue(resultSet.getString("registrationStatusValue"))
                .setEthnicDescription(resultSet.getString("ethnicDescription"))
                .setEthnicCode(resultSet.getString("ethnicCode"))
                .setOds_code(resultSet.getString("ods_code"));


        return patient;
    }

    public String[] getReligion(Long patientId) throws Exception {
        String[] religionData = new String[2];

        String sql = "select " +
                "coalesce(c.description, c.name, '') as name, c.code as code from observation o " +
                "join concept c on o.non_core_concept_id = c.dbid " +
                " join value_set_member vsm on vsm.concept = o.non_core_concept_id "+
                " join  value_set v ON v.dbid = vsm.value_set and v.id = 'http://endhealth.info/im#VSET_Religion' "+
                "where patient_id = " + patientId;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return getDemographicsData(resultSet, religionData);
                } else {
                    return null;
                }
            }
        }
    }

    public String[] getLanguage(Long patientId) throws Exception {
        String[] language = new String[2];

        String sql = "select " +
                "coalesce(c.description, c.name, '') as name, c.code as code from observation o " +
                "join concept c on o.non_core_concept_id = c.dbid " +
                " join value_set_member vsm on vsm.concept = o.non_core_concept_id "+
                " join  value_set v ON v.dbid = vsm.value_set and v.id = 'http://endhealth.info/im#VSET_Language' "+
                "where patient_id = " + patientId;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return getDemographicsData(resultSet, language);
                } else {
                    return null;
                }
            }
        }
    }

    private String[] getDemographicsData(ResultSet resultSet, String[] data) throws SQLException {
        data[0] = (resultSet.getString("code"));
        data[1] =  resultSet.getString("name");
        return data;
    }

    public List<TelecomFull> getTelecomFull(long patientId) throws Exception {
        List<TelecomFull> telecomFullList = new ArrayList<>();

        String sql = "select ctype.description as description1, cuse.description as description2, pc.value as value, pc.patient_id as id from patient_contact pc " +
                "join concept cuse on cuse.dbid = pc.use_concept_id " +
                "join concept ctype on ctype.dbid = pc.type_concept_id " +
                "where patient_id = " + patientId;

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    telecomFullList.add(getTelecom(resultSet));
            }
        }

        return telecomFullList;
    }

    private TelecomFull getTelecom(ResultSet resultSet) throws SQLException {
        TelecomFull telecomFull = new TelecomFull();

        telecomFull.setDescription1(resultSet.getString("description1"))
                .setDescription2(resultSet.getString("description2"))
                .setValue(resultSet.getString("value"))
                .setId(resultSet.getLong("id"));
        return telecomFull;
    }

    public List<ConditionFull>  getConditionFullList(Long orgId, Long patientId) throws Exception {
        ArrayList<ConditionFull> conditionFullList =null;
        String sql ="SELECT a.id,a.practitioner_id,a.patient_id AS patientId, min(a.clinical_effective_date) AS date,a.problem_end_date AS EndDate,'active' AS ClinicalStatus," +
                            "COALESCE(c.description, c.name, '') AS name, c.code,c.id AS concept_id,vs.name AS category,a.is_problem AS problem, a.non_core_concept_id " +
                    "FROM observation a " +
                    "LEFT JOIN concept c ON c.dbid = a.non_core_concept_id " +
                    "LEFT JOIN value_set_member vsm ON vsm.concept = a.non_core_concept_id " +
                    "LEFT JOIN value_set vs ON vs.dbid = vsm.value_set " +
                     "WHERE vs.id IN ('http://endhealth.info/im#VSET_Conditions_1') " + "AND a.organization_id IN ("+orgId+") " + "AND a.patient_id IN ("+patientId+")  " +
                     "AND a.clinical_effective_date > (SELECT max(a1.problem_end_date) FROM observation a1 " +
                                                         "WHERE a1.organization_id IN ("+orgId+") " + "AND a1.patient_id IN ("+patientId+")  " +
                                                         "AND a1.non_core_concept_id = a.non_core_concept_id " + "AND a1.problem_end_date IS NOT NULL " +
                                                         "GROUP BY a1.non_core_concept_id) " +
                    "AND a.problem_end_date IS NULL " +
                    "GROUP BY c.code " +
        "UNION " +
                "SELECT DISTINCT a.id, a.practitioner_id, a.patient_id AS patientId,MIN(clinical_effective_date) AS date,a.problem_end_date AS EndDate, " +
                        "IF(ISNULL(a.problem_end_date), 'active', 'resolved') AS ClinicalStatus,COALESCE(c.description, c.name, '') AS name, " +
                         "c.code, c.id AS concept_id,vs.name AS category,a.is_problem AS problem, a.non_core_concept_id " +
                "FROM observation a " +
                "LEFT JOIN concept c ON c.dbid = a.non_core_concept_id " +
                "LEFT JOIN value_set_member vsm ON vsm.concept = a.non_core_concept_id " +
                "LEFT JOIN value_set vs ON vs.dbid = vsm.value_set " +
                "WHERE vs.id = 'http://endhealth.info/im#VSET_Conditions_1' " + "AND a.organization_id IN ("+orgId+") " + "AND a.patient_id IN ("+patientId+")  " +
                "GROUP BY c.code " +
                "HAVING COUNT(a.problem_end_date) < 1 " +
        "UNION " +
                "SELECT (SELECT DISTINCT a1.id FROM observation a1 " +
                        "WHERE a1.organization_id IN ("+orgId+") " + "AND a1.patient_id IN ("+patientId+")  AND a1.non_core_concept_id = a.non_core_concept_id AND a1.id IS NOT NULL " +
                        "GROUP BY c.code) AS id, " +
                        "a.practitioner_id, a.patient_id AS patientId, " +
                        "(SELECT DISTINCT MIN(a1.clinical_effective_date) FROM observation a1 " +
                        "WHERE a1.organization_id IN ("+orgId+") " + "AND a1.patient_id IN ("+patientId+")  AND a1.non_core_concept_id = a.non_core_concept_id AND a1.clinical_effective_date IS NOT NULL " +
                        "GROUP BY c.code) AS date, " +
                        "(SELECT DISTINCT MAX(a1.problem_end_date) FROM observation a1 " +
                        "WHERE a1.organization_id IN ("+orgId+") " + "AND a1.patient_id IN ("+patientId+")  AND a1.non_core_concept_id = a.non_core_concept_id AND a1.problem_end_date IS NOT NULL " +
                         "GROUP BY c.code) AS EndDate, " +
                        "IF(ISNULL(a.problem_end_date), 'active', 'resolved') AS ClinicalStatus, COALESCE(c.description, c.name, '') AS name, " +
                        "c.code, c.id AS concept_id, vs.name AS category, a.is_problem AS problem, a.non_core_concept_id " +
                "FROM observation a " +
                "LEFT JOIN concept c ON c.dbid = a.non_core_concept_id " +
                "LEFT JOIN value_set_member vsm ON vsm.concept = a.non_core_concept_id " +
                "LEFT JOIN value_set vs ON vs.dbid = vsm.value_set " +
                "WHERE vs.id = 'http://endhealth.info/im#VSET_Conditions_1' " + "AND a.organization_id IN ("+orgId+") " + "AND a.patient_id IN ("+patientId+")  AND a.problem_end_date IS NOT NULL " +
                "GROUP BY c.code";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                conditionFullList= getConditionFullList(resultSet);
            }
        }
        return conditionFullList;
    }

    public  ArrayList<ConditionFull> getConditionFullList(ResultSet resultSet) throws SQLException {
        ArrayList<ConditionFull> conditionList=new ArrayList<ConditionFull>();
        if(null !=resultSet) {
            while (resultSet.next()) {
                ConditionFull conditionDtls = new ConditionFull();
                conditionDtls
                        .setId(resultSet.getLong("id"))
                        .setPatientId(resultSet.getLong("patientId"))
                        .setDate(resultSet.getDate("date"))
                        .setEndDate(resultSet.getDate("EndDate"))
                        .setName(resultSet.getString("name"))
                        .setCode(resultSet.getString("code"))
                        .setCategory(resultSet.getString("category"))
                        .setProblem(resultSet.getBoolean("problem"))
                        .setClinicalStatus(resultSet.getString("ClinicalStatus"))
                        .setCodeURL(getCodeURL(resultSet.getString("concept_id")))
                        .setNonCoreConceptId(resultSet.getLong("non_core_concept_id"))
                        .setPractitionerId(resultSet.getLong("practitioner_id"));

                conditionList.add(conditionDtls);
            }
        }
        return conditionList;
    }

    private String getCodeURL(String concept_id) {
        if(null != concept_id)
        {
            if(concept_id.startsWith("SN_"))
            {
                return "http://snomed.info/sct";
            }
            else if(concept_id.startsWith("R2_"))
            {
                return "http://read.info/readv2";
            }
            else if(concept_id.startsWith("R3_"))
            {
                return "http://read.info/ctv3";
            }
        }
        return "http://snomed.info/sct";
    }

    public String getDescriptionFromObservation(Long id) throws Exception{
        String description = null;
        String sql = "select valueConcept.description as description from observation_additional oa " +
                " join concept propConcept on propConcept.dbid = oa.property_id " +
                "join concept valueConcept on valueConcept.dbid = oa.value_id " +
                "where propConcept.id = 'CM_ProblemSignificance' and oa.id=" + id;


        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next())
                    description = resultSet.getString("description").replace(" (qualifier value)", "");
            }
        }

        return description;
    }

    public List<MedicationStatementFull> getMedicationStatementFullList(Long orgId,Long patientId) throws Exception {
        List<MedicationStatementFull> result = null;
        String sql = "select ms.id as msid, coalesce(c.description, c.name, '') as name, c.code,c.id concept_id ,  " +
                "coalesce(ms.clinical_effective_date,'') as clinicalEffDt, " +
                "coalesce(ms.patient_id,'') as patientId, " +
                "coalesce(ms.dose,'') as dose, " +
                "coalesce(ms.quantity_value,'') as qValue, " +
                "coalesce(ms.cancellation_date,'') as cancellation_date, " +
                "coalesce(ms.quantity_unit,'') as qUnit, \n" +
                "max(coalesce(mo.clinical_effective_date,'')) as valueDtTime, " +
                "ms.authorisation_type_concept_id as atCid, " +
                "coalesce(ms.practitioner_id, 0) as practitionerId, " +
                "coalesce(ms.organization_id, 0) as organizationId, " +
                "max(coalesce(mo.clinical_effective_date,'')) as last_issue_date " +
                "from medication_statement ms join medication_order mo on ms.id=mo.medication_statement_id and ms.patient_id = mo.patient_id " +
                "join concept c on c.dbid=ms.non_core_concept_id " +
                "where  ms.organization_id in ("+ orgId + ") and ms.patient_id in ("+ patientId +") group by msid";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getMedicationStatementFullList(resultSet);
            }
        }
        return result;
    }

    public List<MedicationStatementFull> getMedicationStatementFullList(ResultSet resultSet) throws SQLException {
        List<MedicationStatementFull> medicationStatementList = new ArrayList<MedicationStatementFull>();
        while (resultSet.next()) {
            MedicationStatementFull medicationStatement = new MedicationStatementFull();
            medicationStatement
                    .setId(resultSet.getLong("msid"))
                    .setPatientId(resultSet.getLong("patientId"))
                    .setName(resultSet.getString("name"))
                    .setCode(resultSet.getString("code"))
                    .setDate(resultSet.getString("clinicalEffDt"))
                    .setDose(resultSet.getString("dose"))
                    .setCancellationDate(resultSet.getString("cancellation_date"))
                    .setValueDateTime(resultSet.getString("valueDtTime"))
                    .setLastIssueDate(resultSet.getString("last_issue_date"))
                    .setOrganizationId(resultSet.getLong("organizationId"))
                    .setCodeURL(getCodeURL(resultSet.getString("concept_id")))
                    .setPractitionerId(resultSet.getLong("practitionerId"));
            medicationStatementList.add(medicationStatement);
        }
        return medicationStatementList;
    }
    public PractitionerFull getPractitionerFull(long orgId, long practitionerId) throws Exception {
        PractitionerFull practitionerFull =new PractitionerFull();

        String sql = "select id,name,role_code,role_Desc ,gmc_code from practitioner pr where id = ? and organization_id = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setLong(1, practitionerId);
            statement.setLong(2, orgId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {


                    practitionerFull.setId(resultSet.getLong("id"))
                            .setName(resultSet.getString("name"))
                            .setRoleCode(resultSet.getString("role_code"))
                            .setRoleDesc(resultSet.getString("role_Desc"))
                            .setGmcCode(resultSet.getString("gmc_code"));

                }
            }
        }
        return practitionerFull;
    }

    public List<AppointmentFull> getAppointmentFullList(Long orgId, Long patientId) throws Exception {
        List<AppointmentFull> result = null;
        String sql = "SELECT a.id, a.patient_id as patientId, a.schedule_id as sId, a.practitioner_id as prId, a.organization_id as oId, " +
                "a.actual_duration as actualDura, a.start_date as startDt, a.planned_duration as plannedDura, s.type,s.location,coalesce(c.description, c.name, '') as appointment_status," +
                "sa.text_value as locationType" +
                " FROM appointment a join schedule s on a.schedule_id = s.id" +
                " left join schedule_additional sa on s.id=sa.id" +
                " join concept c on c.dbid = a.appointment_status_concept_id  " +
                " where a.organization_id in (" + orgId + ") and a.patient_id in ("+ patientId +")" +
                " and (sa.text_value is null or sa.text_value NOT in ('***This is NOT an appointment, so the patient SHOULD NOT attend Hospital!***','***This is NOT and appointment, so the patient SHOULD NOT attend!***'))" +
                " and (s.location is null or s.location NOT in ('***This is NOT an appointment, so the patient SHOULD NOT attend Hospital!***','***This is NOT and appointment, so the patient SHOULD NOT attend!***'))";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getAppointmentFullList(resultSet);
            }
        }
        return result;
    }

    public List<AppointmentFull> getAppointmentFullList(ResultSet resultSet) throws SQLException {
        List<AppointmentFull> appointmentList = new ArrayList<AppointmentFull>();
        while (resultSet.next()) {
            AppointmentFull appointment = new AppointmentFull();
            appointment
                    .setId(resultSet.getLong("id"))
                    .setPatientId(resultSet.getLong("patientId"))
                    .setActualDuration(resultSet.getInt("actualDura"))
                    .setStartDate(resultSet.getString("startDt"))
                    .setPlannedDuration(resultSet.getInt("plannedDura"))
                    .setType(resultSet.getString("type"))
                    .setOrgId(resultSet.getLong("oId"))
                    .setPractitionerId(resultSet.getLong("prId"))
                    .setScheduleId(resultSet.getLong("sId"))
                    .setLocation(resultSet.getString("location"))
                    .setStatus(resultSet.getString("appointment_status"))
                    .setLocationType(resultSet.getString("locationType"));

            appointmentList.add(appointment);
        }
        return appointmentList;
    }

    public List<EventLog> getDeltaPatientIds(long orgId, java.sql.Timestamp lastProcessedDateTime ) throws SQLException {
            return getDeltaEvents(orgId,lastProcessedDateTime,2,"patient");
    }

    public List<EventLog> getDeltaConditionsIds(long orgId, java.sql.Timestamp lastProcessedDateTime) throws SQLException {
              return getDeltaEvents(orgId,lastProcessedDateTime,11,"observation");
    }

    public List<EventLog> getDeltaEvents(long orgId, java.sql.Timestamp lastProcessedDateTime ,int tableId,String tableName) throws SQLException {
        List<EventLog> result = null;

        String sql = "SELECT dt_change as dateChange,change_type as changeType ,table_id as tableId,record_id as recordId FROM event_log e " +
                            " join " + tableName + " t on t.id=e.record_id " +
                            " where e.table_id=" + tableId + " " +
                            " and t.organization_id in(" + orgId + ") " +
                            " and (e.dt_change> '"+lastProcessedDateTime+"')";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getEventLogs(resultSet);
            }
        }
        return result;
    }

    public List<EventLog> getDeltaDeleteEvents(java.sql.Timestamp lastProcessedDateTime ,int tableId) throws SQLException {
        List<EventLog> result = null;

        String sql = " SELECT dt_change as dateChange,change_type as changeType ,table_id as tableId,record_id as recordId FROM event_log e " +
                " where e.table_id=" + tableId + " " +
                "  and e.change_type=2 " +
                " and (e.dt_change> '"+lastProcessedDateTime+"')";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getEventLogs(resultSet);
            }
        }
        return result;
    }
    public List<EventLog> getDeltaMedicationStatementIds(long orgId, java.sql.Timestamp lastProcessedDateTime) throws SQLException {
           return getDeltaEvents(orgId,lastProcessedDateTime,10,"medication_statement");
    }

    public List<EventLog> getDeltaAppointmentsIds(long orgId, java.sql.Timestamp lastProcessedDateTime) throws SQLException {
            return getDeltaEvents(orgId,lastProcessedDateTime,18,"appointment");
    }

    public List<EventLog> getEventLogs(ResultSet resultSet) throws SQLException {
        List<EventLog> eventList = new ArrayList<EventLog>();
        while (resultSet.next()) {
            EventLog event = new EventLog();
            event
                    .setRecordId(resultSet.getLong("recordId"))
                    .setChangeType(resultSet.getInt("changeType"))
                    .setTableId(resultSet.getInt("tableId"))
                    .setDateChange(resultSet.getDate("dateChange"));
            eventList.add(event);
        }
        return eventList;
    }

    public ConditionFull  getConditionFull(long id, Long orgId) throws Exception {
        ConditionFull conditionFull =new ConditionFull();
        String sql = "SELECT a.id as id, a.patient_id as patientId, a.clinical_effective_date as date,IF(ISNULL(a.problem_end_date), 'active', 'resolved') " +
                "AS ClinicalStatus, coalesce(c.description, c.name, '') as name ,c.code,c.id concept_id , vs.name as category, a.is_problem as problem, a.problem_end_date as endDate,a.non_core_concept_id  " +
                "FROM observation a  "+
                "LEFT JOIN concept c on c.dbid = a.non_core_concept_id  "+
                "LEFT JOIN value_set_member vsm on vsm.concept = a.non_core_concept_id  "+
                "LEFT JOIN value_set vs on vs.dbid = vsm.value_set  "+
                "WHERE     vs.id in ('http://endhealth.info/im#VSET_Conditions_1')  "+
                "AND  "+
                "a.organization_id in ("+orgId+")  "+
                "AND  "+
                "a.id in ("+id+") ";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    conditionFull
                            .setId(resultSet.getLong("id"))
                            .setPatientId(resultSet.getLong("patientId"))
                            .setDate(resultSet.getDate("date"))
                            .setEndDate(resultSet.getDate("EndDate"))
                            .setName(resultSet.getString("name"))
                            .setCode(resultSet.getString("code"))
                            .setCategory(resultSet.getString("category"))
                            .setProblem(resultSet.getBoolean("problem"))
                            .setClinicalStatus(resultSet.getString("ClinicalStatus"))
                            .setNonCoreConceptId(resultSet.getLong("non_core_concept_id"))
                            .setCodeURL(getCodeURL(resultSet.getString("concept_id")));
                }
            }
        }
        return conditionFull;
    }

     public AppointmentFull getAppointmentFull(long id,Long orgId) throws Exception {
         AppointmentFull appointment = new AppointmentFull();
         String sql = "SELECT a.id, a.patient_id as patientId, a.schedule_id as sId, a.practitioner_id as prId, a.organization_id as oId, " +
                 "a.actual_duration as actualDura, a.start_date as startDt, a.planned_duration as plannedDura, s.type,s.location,coalesce(c.description, c.name, '') as appointment_status," +
                 "sa.text_value as locationType" +
                 " FROM appointment a join schedule s on a.schedule_id = s.id" +
                 " left join schedule_additional sa on s.id=sa.id" +
                 " join concept c on c.dbid = a.appointment_status_concept_id  where a.organization_id in (" + orgId + ") and a.id in (" + id + ")" +
                 " and (sa.text_value is null or sa.text_value NOT in ('***This is NOT an appointment, so the patient SHOULD NOT attend Hospital!***','***This is NOT and appointment, so the patient SHOULD NOT attend!***'))" +
                 " and (s.location is null or s.location NOT in ('***This is NOT an appointment, so the patient SHOULD NOT attend Hospital!***','***This is NOT and appointment, so the patient SHOULD NOT attend!***'))";

         try (PreparedStatement statement = conn.prepareStatement(sql)) {
             try (ResultSet resultSet = statement.executeQuery()) {
                 if (resultSet.next()) {
                     appointment
                             .setId(resultSet.getLong("id"))
                             .setPatientId(resultSet.getLong("patientId"))
                             .setActualDuration(resultSet.getInt("actualDura"))
                             .setStartDate(resultSet.getString("startDt"))
                             .setPlannedDuration(resultSet.getInt("plannedDura"))
                             .setType(resultSet.getString("type"))
                             .setOrgId(resultSet.getLong("oId"))
                             .setPractitionerId(resultSet.getLong("prId"))
                             .setScheduleId(resultSet.getLong("sId"))
                             .setLocation(resultSet.getString("location"))
                             .setStatus(resultSet.getString("appointment_status"))
                             .setLocationType(resultSet.getString("locationType"));
                     return appointment;
                 }
             }
         }
         return null;
     }
    public MedicationStatementFull getMedicationStatementFull(long id, Long orgId) throws Exception {
        MedicationStatementFull medicationStatement = new MedicationStatementFull();
        String sql = "select ms.id as msid, coalesce(c.description, c.name, '') as name, c.code,c.id concept_id ,  " +
                "coalesce(ms.clinical_effective_date,'') as clinicalEffDt, " +
                "coalesce(ms.patient_id,'') as patientId, " +
                "coalesce(ms.dose,'') as dose, " +
                "coalesce(ms.quantity_value,'') as qValue, " +
                "coalesce(ms.cancellation_date,'') as cancellation_date, " +
                "coalesce(ms.quantity_unit,'') as qUnit, \n" +
                "max(coalesce(mo.clinical_effective_date,'')) as valueDtTime, " +
                "ms.authorisation_type_concept_id as atCid, " +
                "coalesce(ms.practitioner_id, 0) as practitionerId, " +
                "coalesce(ms.organization_id, 0) as organizationId, " +
                "max(coalesce(mo.clinical_effective_date,'')) as last_issue_date " +
                "from medication_statement ms join medication_order mo on ms.id=mo.medication_statement_id and ms.patient_id = mo.patient_id " +
                "join concept c on c.dbid=ms.non_core_concept_id " +
                "where  ms.organization_id in ("+ orgId + ") and ms.id in ("+ id +") group by msid";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    medicationStatement
                            .setId(resultSet.getLong("msid"))
                            .setPatientId(resultSet.getLong("patientId"))
                            .setName(resultSet.getString("name"))
                            .setCode(resultSet.getString("code"))
                            .setDate(resultSet.getString("clinicalEffDt"))
                            .setDose(resultSet.getString("dose"))
                            .setCancellationDate(resultSet.getString("cancellation_date"))
                            .setValueDateTime(resultSet.getString("valueDtTime"))
                            .setLastIssueDate(resultSet.getString("last_issue_date"))
                            .setOrganizationId(resultSet.getLong("organizationId"))
                            .setCodeURL(getCodeURL(resultSet.getString("concept_id")))
                            .setPractitionerId(resultSet.getLong("practitionerId"));
                }
            }
        }
        return medicationStatement;
    }

    public ConditionFull  getCondition(long id, Long orgId) throws Exception {
        ConditionFull condition =new ConditionFull();

        String sql = "SELECT patient_id as patientId, non_core_concept_id as conceptCode, problem_end_date as endDate  " +
                "FROM observation " +
                "WHERE organization_id in (" + orgId + ") " +
                "AND id in (" + id + ")";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    condition
                            .setPatientId(resultSet.getLong("patientId"))
                            .setNonCoreConceptId(resultSet.getLong("conceptCode"))
                            .setEndDate(resultSet.getDate("endDate"));
                }
            }
        }
        return condition;
    }

    public List<AppointmentFull> getAppointmentFullListForCW(Long orgId, Long patientId) throws Exception {
        List<AppointmentFull> result = null;
        String sql = "SELECT a.id, a.patient_id as patientId, a.schedule_id as sId, a.practitioner_id as prId, a.organization_id as oId, " +
                "a.actual_duration as actualDura, a.start_date as startDt, a.planned_duration as plannedDura, s.type,s.location,coalesce(c.description, c.name, '') as appointment_status," +
                "sa.text_value as locationType" +
                " FROM appointment a join schedule s on a.schedule_id = s.id" +
                " left join schedule_additional sa on s.id=sa.id" +
                " join concept c on c.dbid = a.appointment_status_concept_id  " +
                " where a.organization_id in (" + orgId + ") and a.patient_id in ("+ patientId +")" +
                "and (s.location in ('WMUH - Heart failure service','West Middlesex University Hospital Outpatient Clinic') or sa.text_value in ('WMUH - Heart failure service','West Middlesex University Hospital Outpatient Clinic'))";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                result = getAppointmentFullList(resultSet);
            }
        }
        return result;
    }

    public AppointmentFull getAppointmentFullForCW(long id,Long orgId) throws Exception {
        AppointmentFull appointment = new AppointmentFull();
        String sql = "SELECT a.id, a.patient_id as patientId, a.schedule_id as sId, a.practitioner_id as prId, a.organization_id as oId, " +
                "a.actual_duration as actualDura, a.start_date as startDt, a.planned_duration as plannedDura, s.type,s.location,coalesce(c.description, c.name, '') as appointment_status," +
                "sa.text_value as locationType" +
                " FROM appointment a join schedule s on a.schedule_id = s.id" +
                " left join schedule_additional sa on s.id=sa.id" +
                " join concept c on c.dbid = a.appointment_status_concept_id  where a.organization_id in (" + orgId + ") and a.id in (" + id + ")" +
                "and (s.location in ('WMUH - Heart failure service','West Middlesex University Hospital Outpatient Clinic') or sa.text_value in ('WMUH - Heart failure service','West Middlesex University Hospital Outpatient Clinic'))";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    appointment
                            .setId(resultSet.getLong("id"))
                            .setPatientId(resultSet.getLong("patientId"))
                            .setActualDuration(resultSet.getInt("actualDura"))
                            .setStartDate(resultSet.getString("startDt"))
                            .setPlannedDuration(resultSet.getInt("plannedDura"))
                            .setType(resultSet.getString("type"))
                            .setOrgId(resultSet.getLong("oId"))
                            .setPractitionerId(resultSet.getLong("prId"))
                            .setScheduleId(resultSet.getLong("sId"))
                            .setLocation(resultSet.getString("location"))
                            .setStatus(resultSet.getString("appointment_status"))
                            .setLocationType(resultSet.getString("locationType"));
                    return appointment;
                }
            }
        }
        return null;
    }
    @Override
    public void close() throws Exception {

        if (conn != null) {
            this.conn.close();
        }
    }
}
