--
-- Script to cleanup the Case Data Manager Database Schema for MS SQL DB
--

DROP VIEW IF EXISTS cdm_cases;
GO

DROP TABLE IF EXISTS cdm_job_queue;
DROP TABLE IF EXISTS cdm_properties;
DROP TABLE IF EXISTS cdm_case_links;
DROP TABLE IF EXISTS cdm_links;
DROP TABLE IF EXISTS cdm_cases_int;
DROP TABLE IF EXISTS cdm_states;
DROP TABLE IF EXISTS cdm_identifier_infos;
DROP TABLE IF EXISTS cdm_type_indexes;
DROP TABLE IF EXISTS cdm_datamodel_deps;
DROP TABLE IF EXISTS cdm_types;
DROP TABLE IF EXISTS cdm_datamodels;
DROP TABLE IF EXISTS cdm_applications;

GO

