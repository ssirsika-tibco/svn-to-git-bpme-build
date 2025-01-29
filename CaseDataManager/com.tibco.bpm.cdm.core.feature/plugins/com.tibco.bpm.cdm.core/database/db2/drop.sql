--
-- Script to cleanup the Case Data Manager Database Schema for DB2 Database
--

BEGIN
	DECLARE CONTINUE HANDLER FOR SQLSTATE '42704'
		BEGIN END;
	EXECUTE IMMEDIATE 'DROP VIEW cdm_cases';
END
/

DROP TABLE cdm_job_queue IF EXISTS;
DROP TABLE cdm_properties IF EXISTS;
DROP TABLE cdm_case_links IF EXISTS;
DROP TABLE cdm_links IF EXISTS;
DROP TABLE cdm_cases_int IF EXISTS;
DROP TABLE cdm_states IF EXISTS;
DROP TABLE cdm_identifier_infos IF EXISTS;
DROP TABLE cdm_type_indexes IF EXISTS;
DROP TABLE cdm_datamodel_deps IF EXISTS;
DROP TABLE cdm_types IF EXISTS;
DROP TABLE cdm_datamodels IF EXISTS;
DROP TABLE cdm_applications IF EXISTS;


