USE cie_data_extract;

DROP TABLE IF EXISTS appointment_speciality;
DROP TABLE IF EXISTS available_jobs;
DROP TABLE IF EXISTS cie_allergy_reference;
DROP TABLE IF EXISTS cie_appointment_reference;
DROP TABLE IF EXISTS cie_audit_log;
DROP TABLE IF EXISTS cie_condition_reference;
DROP TABLE IF EXISTS cie_medication_reference;
DROP TABLE IF EXISTS cie_medication_statement_reference;
DROP TABLE IF EXISTS cie_new_patient_reference;
DROP TABLE IF EXISTS cie_org_cohorts;
DROP TABLE IF EXISTS cie_org_process_status;
DROP TABLE IF EXISTS cie_org_references;
DROP TABLE IF EXISTS cie_orgs_thread_status;
DROP TABLE IF EXISTS cie_patient_reference;
DROP TABLE IF EXISTS cie_practitioner_reference;
DROP TABLE IF EXISTS cie_process_status;
DROP TABLE IF EXISTS cie_subscriber_config;
DROP TABLE IF EXISTS cie_subscribercfg_thread_status;
DROP TABLE IF EXISTS history;
DROP TABLE IF EXISTS scheduler_calendars;
DROP TABLE IF EXISTS scheduler_fired_triggers;
DROP TABLE IF EXISTS scheduler_job_details;
DROP TABLE IF EXISTS scheduler_locks;
DROP TABLE IF EXISTS scheduler_paused_trigger_grps;
DROP TABLE IF EXISTS scheduler_scheduler_state;
DROP TABLE IF EXISTS scheduler_triggers;
DROP TABLE IF EXISTS resource_fhir_data;

DROP PROCEDURE IF EXISTS get_audit_error_list;

CREATE TABLE appointment_speciality
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `rota_type`  varchar(200) NOT NULL,
    `speciality` varchar(200) DEFAULT NULL,
    `code`       varchar(50)  DEFAULT NULL,
    `org`        varchar(200) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE available_jobs
(
    `job_name`     varchar(30) NOT NULL,
    `start_url`    varchar(500)  DEFAULT NULL,
    `status`       tinyint(1) DEFAULT NULL,
    `stop_url`     varchar(500)  DEFAULT NULL,
    `class_path`   varchar(500)  DEFAULT NULL,
    `start_method` varchar(100)  DEFAULT NULL,
    `stop_method`  varchar(100)  DEFAULT NULL,
    `parameters`   varchar(1000) DEFAULT NULL,
    PRIMARY KEY (`job_name`)
);

CREATE TABLE cie_allergy_reference
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`     varchar(200) DEFAULT NULL,
    `local_id`          bigint(20) DEFAULT NULL,
    `organization_name` varchar(30) NOT NULL,
    `updated_datetime`  datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE cie_appointment_reference
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`     varchar(200) DEFAULT NULL,
    `local_id`          bigint(20) DEFAULT NULL,
    `organization_name` varchar(30) NOT NULL,
    `updated_datetime`  datetime     DEFAULT NULL,
    `config_id`         int(11) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX appointment_ref_id
    ON cie_appointment_reference
        (id);

CREATE INDEX appointment_id
    ON cie_appointment_reference
        (local_id);

CREATE INDEX ods_code
    ON cie_appointment_reference
        (organization_name);

CREATE INDEX updated_datetime
    ON cie_appointment_reference
        (updated_datetime);

CREATE INDEX config_id
    ON cie_appointment_reference
        (config_id);



CREATE TABLE cie_audit_log
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `resource_type`      varchar(30)   DEFAULT NULL,
    `resource_local_id`  bigint(20) DEFAULT NULL,
    `resource_uuid`      varchar(200)  DEFAULT NULL,
    `log_string`         mediumtext,
    `config_id`          int(11) DEFAULT NULL,
    `org_name`           varchar(30) NOT NULL,
    `reprocess_required` varchar(2)  NOT NULL,
    `http_method`        varchar(10) NOT NULL,
    `updated_datetime`   datetime      DEFAULT NULL,
    `reason`             varchar(1000) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE cie_condition_reference
(
    `id`                    int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`         varchar(200) DEFAULT NULL,
    `local_id`              bigint(20) DEFAULT NULL,
    `organization_name`     varchar(30) NOT NULL,
    `updated_datetime`      datetime DEFAULT NULL,
    `config_id`             int(11) DEFAULT NULL,
    `status`                varchar(45) DEFAULT NULL,
    `patient_id`            bigint DEFAULT NULL,
    `non_core_concept_id`   bigint DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX condition_ref_id
    ON cie_condition_reference
        (id);

CREATE INDEX condition_id
    ON cie_condition_reference
        (local_id);

CREATE INDEX ods_code
    ON cie_condition_reference
        (organization_name);

CREATE INDEX updated_datetime
    ON cie_condition_reference
        (updated_datetime);

CREATE INDEX config_id
    ON cie_condition_reference
        (config_id);


CREATE TABLE cie_medication_reference
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`     varchar(200) DEFAULT NULL,
    `local_id`          varchar(50)  DEFAULT NULL,
    `organization_name` varchar(30) NOT NULL,
    `updated_datetime`  datetime     DEFAULT NULL,
    `config_id`         int(11) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX medication_ref_id
    ON cie_medication_reference
        (id);

CREATE INDEX medication_id
    ON cie_medication_reference
        (local_id);

CREATE INDEX ods_code
    ON cie_medication_reference
        (organization_name);

CREATE INDEX updated_datetime
    ON cie_medication_reference
        (updated_datetime);

CREATE INDEX config_id
    ON cie_medication_reference
        (config_id);


CREATE TABLE cie_medication_statement_reference
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`     varchar(200) DEFAULT NULL,
    `local_id`          bigint(20) DEFAULT NULL,
    `organization_name` varchar(30) NOT NULL,
    `updated_datetime`  datetime     DEFAULT NULL,
    `config_id`         int(11) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX medication_statement_ref_id
    ON cie_medication_statement_reference
        (id);

CREATE INDEX medication_statement_id
    ON cie_medication_statement_reference
        (local_id);

CREATE INDEX ods_code
    ON cie_medication_statement_reference
        (organization_name);

CREATE INDEX updated_datetime
    ON cie_medication_statement_reference
        (updated_datetime);

CREATE INDEX config_id
    ON cie_medication_statement_reference
        (config_id);

CREATE TABLE cie_new_patient_reference
(
    `id`               int(11) NOT NULL AUTO_INCREMENT,
    `nhs_number`       varchar(255) NOT NULL,
    `ods_code`         varchar(30)  NOT NULL,
    `updated_datetime` datetime DEFAULT NULL,
    PRIMARY KEY (`id`, `nhs_number`, `ods_code`)
);

CREATE UNIQUE INDEX cie_new_patient_ref_id
    ON cie_new_patient_reference
        (id);

CREATE INDEX nhs_number
    ON cie_new_patient_reference
        (nhs_number);

CREATE INDEX ods_code
    ON cie_new_patient_reference
        (ods_code);

CREATE TABLE cie_org_cohorts
(
    `id`                       int(11) NOT NULL AUTO_INCREMENT,
    `cie_subscriber_config_id` int(11) DEFAULT NULL,
    `ods_code`                 varchar(20) NOT NULL,
    `updated_datetime`         datetime    DEFAULT NULL,
    `config_id`                int(11) DEFAULT NULL,
    `status`                   varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE cie_org_process_status
(
    `ods_code`           varchar(20) NOT NULL,
    `cie_orgs_thread_id` int(11) DEFAULT NULL,
    `start_datetime`     datetime    DEFAULT NULL,
    `end_datetime`       datetime    DEFAULT NULL,
    `status`             varchar(20) DEFAULT NULL,
    PRIMARY KEY (`ods_code`)
);

CREATE TABLE cie_org_references
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`     varchar(200) DEFAULT NULL,
    `organization_name` varchar(30) NOT NULL,
    `updated_datetime`  datetime     DEFAULT NULL,
    `config_id`         int(11) DEFAULT NULL,
    PRIMARY KEY (`id`, `organization_name`)
);

CREATE UNIQUE INDEX cie_org_ref_id
    ON cie_org_references
        (id);

CREATE INDEX ods_code
    ON cie_org_references
        (organization_name);

CREATE TABLE cie_orgs_thread_status
(
    `id`                          int(11) NOT NULL AUTO_INCREMENT,
    `cie_subscribercfg_thread_id` int(11) DEFAULT NULL,
    `start_datetime`              datetime    DEFAULT NULL,
    `end_datetime`                datetime    DEFAULT NULL,
    `status`                      varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE cie_patient_reference
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`     varchar(200) DEFAULT NULL,
    `local_id`          bigint(20) DEFAULT NULL,
    `organization_name` varchar(30) NOT NULL,
    `updated_datetime`  datetime     DEFAULT NULL,
    `config_id`         int(11) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX cie_patient_ref_id
    ON cie_patient_reference
        (id);

CREATE INDEX cie_patient_id
    ON cie_patient_reference
        (local_id);

CREATE INDEX ods_code
    ON cie_patient_reference
        (organization_name);

CREATE INDEX updated_datetime
    ON cie_patient_reference
        (updated_datetime);

CREATE INDEX config_id
    ON cie_patient_reference
        (config_id);


CREATE TABLE cie_practitioner_reference
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `resource_uuid`     varchar(200) DEFAULT NULL,
    `local_id`          bigint(20) DEFAULT NULL,
    `organization_name` varchar(30) NOT NULL,
    `updated_datetime`  datetime     DEFAULT NULL,
    `config_id`         int(11) DEFAULT NULL,
    `role_uuid`         varchar(200) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX cie_practitioner_ref_id
    ON cie_practitioner_reference
        (id);

CREATE INDEX cie_practitioner_id
    ON cie_practitioner_reference
        (local_id);

CREATE INDEX ods_code
    ON cie_practitioner_reference
        (organization_name);

CREATE INDEX updated_datetime
    ON cie_practitioner_reference
        (updated_datetime);

CREATE INDEX config_id
    ON cie_practitioner_reference
        (config_id);


CREATE TABLE cie_process_status
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `process_type`   varchar(10) DEFAULT NULL,
    `start_datetime` datetime    DEFAULT NULL,
    `end_datetime`   datetime    DEFAULT NULL,
    `status`         varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE cie_subscriber_config
(
    `id`          int(11) NOT NULL AUTO_INCREMENT,
    `config_name` varchar(30) DEFAULT NULL,
    `status`      varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE cie_subscribercfg_thread_status
(
    `id`                       int(11) NOT NULL AUTO_INCREMENT,
    `cie_process_id`           int(11) DEFAULT NULL,
    `cie_subscriber_config_id` int(11) DEFAULT NULL,
    `start_datetime`           datetime    DEFAULT NULL,
    `end_datetime`             datetime    DEFAULT NULL,
    `status`                   varchar(20) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE history
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `job_name`          varchar(100) NOT NULL,
    `status`            varchar(100) NOT NULL,
    `information`       varchar(250) DEFAULT NULL,
    `job_start_time`    datetime     DEFAULT NULL,
    `job_complete_time` datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE scheduler_calendars
(
    `sched_name`    varchar(120) NOT NULL,
    `calendar_name` varchar(200) NOT NULL,
    `calendar`      blob         NOT NULL,
    PRIMARY KEY (`sched_name`, `calendar_name`)
);

CREATE TABLE scheduler_fired_triggers
(
    `sched_name`        varchar(120) NOT NULL,
    `entry_id`          varchar(95)  NOT NULL,
    `trigger_name`      varchar(200) NOT NULL,
    `trigger_group`     varchar(200) NOT NULL,
    `instance_name`     varchar(200) NOT NULL,
    `fired_time`        bigint(20) NOT NULL,
    `sched_time`        bigint(20) NOT NULL,
    `priority`          int(11) NOT NULL,
    `state`             varchar(16)  NOT NULL,
    `job_name`          varchar(200) DEFAULT NULL,
    `job_group`         varchar(200) DEFAULT NULL,
    `is_nonconcurrent`  tinyint(1) DEFAULT NULL,
    `requests_recovery` tinyint(1) DEFAULT NULL,
    PRIMARY KEY (`sched_name`, `entry_id`)
);

CREATE INDEX idx_scheduler_ft_trig_inst_name
    ON scheduler_fired_triggers (sched_name,instance_name);

CREATE INDEX idx_scheduler_ft_inst_job_req_rcvry
    ON scheduler_fired_triggers (sched_name,instance_name,requests_recovery);

CREATE INDEX idx_scheduler_ft_j_g
    ON scheduler_fired_triggers (sched_name,job_name,job_group);

CREATE INDEX idx_scheduler_ft_jg
    ON scheduler_fired_triggers (sched_name,job_name);

CREATE INDEX idx_scheduler_ft_t_g
    ON scheduler_fired_triggers (sched_name,trigger_name,trigger_group);

CREATE INDEX idx_scheduler_ft_tg
    ON scheduler_fired_triggers (sched_name,trigger_group);

CREATE TABLE scheduler_job_details
(
    `sched_name`        varchar(120) NOT NULL,
    `job_name`          varchar(200) NOT NULL,
    `job_group`         varchar(200) NOT NULL,
    `description`       varchar(250) DEFAULT NULL,
    `job_class_name`    varchar(250) NOT NULL,
    `is_durable`        tinyint(1) NOT NULL,
    `is_nonconcurrent`  tinyint(1) NOT NULL,
    `is_update_data`    tinyint(1) NOT NULL,
    `requests_recovery` tinyint(1) NOT NULL,
    `job_data`          blob,
    PRIMARY KEY (`sched_name`, `job_name`, `job_group`)
);

CREATE INDEX idx_scheduler_j_req_recovery
    ON scheduler_job_details (sched_name,requests_recovery);

CREATE INDEX idx_scheduler_j_grp
    ON scheduler_job_details (sched_name,job_group);


CREATE TABLE scheduler_locks
(
    `sched_name` varchar(120) NOT NULL,
    `lock_name`  varchar(40)  NOT NULL,
    PRIMARY KEY (`sched_name`, `lock_name`)
);

CREATE TABLE scheduler_paused_trigger_grps
(
    `sched_name`    varchar(120) NOT NULL,
    `trigger_group` varchar(200) NOT NULL,
    PRIMARY KEY (`sched_name`, `trigger_group`)
);

CREATE TABLE scheduler_scheduler_state
(
    `sched_name`        varchar(120) NOT NULL,
    `instance_name`     varchar(200) NOT NULL,
    `last_checkin_time` bigint(20) NOT NULL,
    `checkin_interval`  bigint(20) NOT NULL,
    PRIMARY KEY (`sched_name`, `instance_name`)
);

CREATE TABLE `scheduler_triggers`
(
    `sched_name`     varchar(120) NOT NULL,
    `trigger_name`   varchar(200) NOT NULL,
    `trigger_group`  varchar(200) NOT NULL,
    `job_name`       varchar(200) NOT NULL,
    `job_group`      varchar(200) NOT NULL,
    `description`    varchar(250) DEFAULT NULL,
    `next_fire_time` bigint(20) DEFAULT NULL,
    `prev_fire_time` bigint(20) DEFAULT NULL,
    `priority`       int(11) DEFAULT NULL,
    `trigger_state`  varchar(16)  NOT NULL,
    `trigger_type`   varchar(8)   NOT NULL,
    `start_time`     bigint(20) NOT NULL,
    `end_time`       bigint(20) DEFAULT NULL,
    `calendar_name`  varchar(200) DEFAULT NULL,
    `misfire_instr`  smallint(6) DEFAULT NULL,
    `job_data`       blob,
    PRIMARY KEY (`sched_name`, `trigger_name`, `trigger_group`)
    CONSTRAINT `scheduler_triggers_ibfk_1` FOREIGN KEY (`sched_name`, `job_name`, `job_group`) REFERENCES `scheduler_job_details` (`sched_name`, `job_name`, `job_group`)
);

CREATE INDEX idx_scheduler_t_j
    ON scheduler_triggers (sched_name,job_name,job_group);

CREATE INDEX idx_scheduler_t_jg
    ON scheduler_triggers (sched_name,job_group);

CREATE INDEX idx_scheduler_t_c
    ON scheduler_triggers (sched_name,calendar_name);

CREATE INDEX idx_scheduler_t_g
    ON scheduler_triggers (sched_name,trigger_group);

CREATE INDEX idx_scheduler_t_state
    ON scheduler_triggers (sched_name,trigger_state);

CREATE INDEX idx_scheduler_t_n_state
    ON scheduler_triggers (sched_name,trigger_name,trigger_group,trigger_state);

CREATE INDEX idx_scheduler_t_n_g_state
    ON scheduler_triggers (sched_name,trigger_group,trigger_state);

CREATE INDEX idx_scheduler_t_next_fire_time
    ON scheduler_triggers (sched_name,next_fire_time);

CREATE INDEX idx_scheduler_t_nft_st
    ON scheduler_triggers (sched_name,trigger_state,next_fire_time);

CREATE INDEX idx_scheduler_t_nft_misfire
    ON scheduler_triggers (sched_name,misfire_instr,next_fire_time);

CREATE INDEX idx_scheduler_t_nft_st_misfire
    ON scheduler_triggers (sched_name,misfire_instr,next_fire_time,trigger_state);

CREATE INDEX idx_scheduler_t_nft_st_misfire_grp
    ON scheduler_triggers (sched_name,misfire_instr,next_fire_time,trigger_group,trigger_state);


CREATE TABLE resource_fhir_data
(
    `id`            int(11) NOT NULL AUTO_INCREMENT,
    `resource_id`   varchar(45) DEFAULT NULL,
    `resource_type` varchar(30) DEFAULT NULL,
    `fhir_obj`      mediumtext,
    `datetime`      datetime    DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE UNIQUE INDEX id
    ON resource_fhir_data
        (id);

CREATE INDEX resource_uuid
    ON resource_fhir_data
        (resource_uuid);

CREATE INDEX resource_type
    ON resource_fhir_data
        (resource_type);


CREATE TABLE `ods_code_mapping`
(
    `id`         int          NOT NULL AUTO_INCREMENT,
    `ods_code`   varchar(30)  NOT NULL,
    `org`        varchar(45)  NOT NULL,
    `name`       varchar(255) NOT NULL,
    `public_org` varchar(255) NOT NULL,
    PRIMARY KEY (`id`, `ods_code`),
    UNIQUE KEY `ods_code_mapping_id` (`id`),
    KEY          `ods_code` (`ods_code`),
    KEY          `name` (`name`)
);

DELIMITER $$
CREATE PROCEDURE `get_audit_error_list`( IN input_date datetime)
BEGIN

    select al.*
    from cie_audit_log al
    where  al.updated_datetime > input_date;

END $$
DELIMITER ;



