CREATE SEQUENCE cdm_applications_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cdm_datamodels_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cdm_types_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cdm_states_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cdm_links_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cdm_cases_int_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE cdm_job_queue_seq NO MAXVALUE CYCLE;

CREATE TABLE cdm_applications
(
  id                        NUMERIC(28,0) NOT NULL, -- id assigned from a sequence (primary key for this table)
  application_name          VARCHAR(256) NOT NULL, 
  application_id            VARCHAR(256) NOT NULL,  -- Fully qualified application name (com.example.ProjectX)
  deployment_id             NUMERIC(28,0) NOT NULL, -- Unique number for each deployed version, used in undeploy/status calls
  major_version             INTEGER NOT NULL, -- Columns types for the 4 version number parts mirror those used by DEM's schema
  minor_version             INTEGER NOT NULL,
  micro_version             INTEGER NOT NULL,
  qualifier                 VARCHAR(36)	NOT NULL,
  deployment_timestamp      TIMESTAMP NOT NULL,
  CONSTRAINT cdm_applications_pk PRIMARY KEY (id)
);

CREATE TABLE cdm_datamodels
(
  id                        NUMERIC(28,0) NOT NULL, -- id assigned from a sequence (primary key for this table)
  application_id			NUMERIC(28,0) NOT NULL, -- id of application (numeric, assigned from sequence) in cdm_applications
  major_version             NUMERIC(10,0) NOT NULL, -- e.g. 1 (duplicated from cdm_applications to allow cdm_datamodels_unq) 
  namespace                 VARCHAR(256) NOT NULL, -- e.g. "com.example.carmodel"
  model                     TEXT NOT NULL,
  script                    TEXT, -- TODO make this NOT NULL once everything is integrated
  CONSTRAINT cdm_datamodels_pk PRIMARY KEY (id),
  CONSTRAINT cdm_datamodels_fk1 FOREIGN KEY (application_id)
    REFERENCES cdm_applications (id) ON DELETE CASCADE,
  CONSTRAINT cdm_datamodels_unq UNIQUE (namespace, major_version)    
);

CREATE TABLE cdm_types
(
	id                     NUMERIC(28,0) NOT NULL, -- id assigned from a sequence (primary key for this table)
	datamodel_id           NUMERIC(28,0) NOT NULL, -- id of datamodel in cdm_datamodels
	name                   VARCHAR(256) NOT NULL, -- name of type (e.g. 'Address')
	is_case                BOOLEAN NOT NULL, -- true if case, false if not
  CONSTRAINT cdm_types_pk PRIMARY KEY (id),
  CONSTRAINT cdm_types_fk1 FOREIGN KEY (datamodel_id)
    REFERENCES cdm_datamodels (id) ON DELETE CASCADE
);

CREATE TABLE cdm_states -- TODO Does order of states need to be maintained? If so, need an idx column.
(
    id                     NUMERIC(28,0) NOT NULL, -- id assigned from a sequence (primary key for this table)
    type_id                NUMERIC(28,0) NOT NULL, -- id of type in cdm_types
    value                  VARCHAR(100) NOT NULL,
    label                  VARCHAR(100) NOT NULL, -- TODO don't think label length is validated
    is_terminal            BOOLEAN NOT NULL,
  CONSTRAINT cdm_states_pk PRIMARY KEY (id),
  CONSTRAINT cdm_states_fk1 FOREIGN KEY (type_id)
    REFERENCES cdm_types (id) ON DELETE CASCADE
);

CREATE TABLE cdm_type_indexes
(
    type_id                NUMERIC(28,0) NOT NULL, -- id of type in cdm_types
	name                   VARCHAR(63) NOT NULL, -- name of index (e.g. 'i_cdm_ordermodel_Address_postcode')
	attribute_name         VARCHAR(256) NOT NULL, -- name of attribute (e.g. 'postcode')
  CONSTRAINT cdm_type_indexes_fk1 FOREIGN KEY (type_id)
    REFERENCES cdm_types (id) -- no 'on delete cascade', as we want to make sure we explicitly remove indexes
);

CREATE TABLE cdm_datamodel_deps
(
  datamodel_id_from			NUMERIC(28,0) NOT NULL, -- id of datamodel in cdm_datamodels
  datamodel_id_to			NUMERIC(28,0) NOT NULL, -- id of datamodel on which the first depends
  CONSTRAINT cdm_datamodel_deps_pk PRIMARY KEY (datamodel_id_from, datamodel_id_to),
  CONSTRAINT cdm_datamodel_deps_fk1 FOREIGN KEY (datamodel_id_from) 
    REFERENCES cdm_datamodels (id) ON DELETE CASCADE,
  CONSTRAINT cdm_datamodel_deps_fk2 FOREIGN KEY (datamodel_id_to) 
    REFERENCES cdm_datamodels (id) -- no 'on delete cascade' (no desire to allow the 'to' model to be deleted while the 'from' depends on it)
);

CREATE TABLE cdm_identifier_infos
(
  type_id                   NUMERIC(28,0) NOT NULL, -- id of type in cdm_types
  prefix                    VARCHAR(32), 
  suffix                    VARCHAR(32), 
  min_num_length            NUMERIC(2,0),
  next_num                  NUMERIC(15,0) NOT NULL,
  CONSTRAINT cdm_identifier_infos_pk PRIMARY KEY (type_id),
  CONSTRAINT cdm_identifier_infos_fk_type_id FOREIGN KEY (type_id)
    REFERENCES cdm_types (id) ON DELETE CASCADE
);

CREATE TABLE cdm_links
(
  id                        NUMERIC(28,0) NOT NULL, -- id assigned from a sequence (primary key for this table)
  end1_owner_id             NUMERIC(28,0) NOT NULL, -- id of owner type in cdm_types
  end1_name                 VARCHAR(256) NOT NULL,  -- name of end
  end1_is_array             BOOLEAN NOT NULL,       -- true if array
  end2_owner_id             NUMERIC(28,0) NOT NULL, 
  end2_name                 VARCHAR(256) NOT NULL,
  end2_is_array             BOOLEAN NOT NULL,
  CONSTRAINT cdm_links_pk PRIMARY KEY (id),
  CONSTRAINT cdm_links_unq UNIQUE (end1_owner_id, end1_name, end2_owner_id, end2_name), 
  CONSTRAINT cdm_links_fk_end1_owner_id FOREIGN KEY (end1_owner_id)
    REFERENCES cdm_types (id) ON DELETE CASCADE,
  CONSTRAINT cdm_links_fk_end2_owner_id FOREIGN KEY (end2_owner_id)
    REFERENCES cdm_types (id) ON DELETE CASCADE
);

CREATE TABLE cdm_cases_int
(
  id                        NUMERIC(28,0) NOT NULL, -- (like 'bds_id' in BDS)
  version                   NUMERIC(15,0) NOT NULL, -- (like 'bds_version' in BDS)
  type_id                   NUMERIC(28,0) NOT NULL, -- id of type in cdm_types
  casedata                  JSONB NOT NULL,
  cid                       VARCHAR(400) NOT NULL, 
  state_id                  NUMERIC(28,0) NOT NULL, -- id of state in cdm_states
  created_by                VARCHAR(36) NOT NULL, -- 36 char GUID (obtained from RequestContext.getCurrentUser().getGuid())
  creation_timestamp        TIMESTAMP NOT NULL,
  modified_by               VARCHAR(36) NOT NULL, -- 36 char GUID (obtained from RequestContext.getCurrentUser().getGuid())
  modification_timestamp    TIMESTAMP NOT NULL,
  marked_for_purge          BOOLEAN DEFAULT FALSE,
  CONSTRAINT cdm_cases_int_pk PRIMARY KEY (id),
  CONSTRAINT cdm_cases_int_unq UNIQUE (type_id, cid), -- enforce uniqueness of CID for a given type
  CONSTRAINT cdm_cases_int_fk_type_id FOREIGN KEY (type_id)
	REFERENCES cdm_types (id),
  CONSTRAINT cdm_cases_int_fk_state_id FOREIGN KEY (state_id)
    REFERENCES cdm_states (id) 
);

CREATE TABLE cdm_case_links
(
  link_id                   NUMERIC(28,0) NOT NULL, -- Foreign key to link definition in cdm_links table
  end1_id                   NUMERIC(28,0) NOT NULL, -- id of first case object
  end2_id                   NUMERIC(28,0) NOT NULL, -- id of second case object
  CONSTRAINT cdm_case_links_pk PRIMARY KEY (link_id, end1_id, end2_id),
  CONSTRAINT cdm_case_links_fk_link_id FOREIGN KEY (link_id)
    REFERENCES cdm_links (id) ON DELETE CASCADE,
  CONSTRAINT cdm_case_links_fk_end1_id FOREIGN KEY (end1_id)
    REFERENCES cdm_cases_int (id) ON DELETE CASCADE, -- TODO remove cascade once delete (multiple) cases made to remove links
  CONSTRAINT cdm_case_links_fk_end2_id FOREIGN KEY (end2_id)
    REFERENCES cdm_cases_int (id) ON DELETE CASCADE  -- TODO remove cascade once delete (multiple) cases made to remove links
);

CREATE INDEX idx_casedata ON cdm_cases_int USING gin(casedata);

CREATE TABLE cdm_properties
(
	name		VARCHAR(256) NOT NULL,
	value		VARCHAR(256) NOT NULL,
	CONSTRAINT cdm_properties_pk PRIMARY KEY (name)
);

CREATE TABLE cdm_job_queue
(
	message_id 	bigint DEFAULT nextval('cdm_job_queue_seq'),
	correlation_id 	varchar(128) NULL,
	priority 	integer NOT NULL,
	delay 		timestamp with time zone NOT NULL,
	payload 	bytea NOT NULL,
	enq_time 	timestamp with time zone NOT NULL,
	retry_count 	integer DEFAULT -1,
	CONSTRAINT cdm_job_queue_pk PRIMARY KEY (message_id)
);

CREATE INDEX ix_cdm_job_queue_correlation_id ON cdm_job_queue (correlation_id);
CREATE INDEX ix_cdm_job_queue_delay ON cdm_job_queue (delay);
CREATE INDEX ix_cdm_job_queue_priority_enq_time ON cdm_job_queue (priority, enq_time);

/*
 * Object:  View cdm_cases
 */
CREATE OR REPLACE VIEW cdm_cases 
AS
 SELECT c.cid AS case_identifier,
    concat(c.id, '-', d.namespace, '.', t.name, '-', d.major_version, '-', c.version) AS casereference,
    concat(c.id, '-', d.namespace, '.', t.name, '-', d.major_version) AS unversioned_casereference,
    concat(d.namespace, '.', t.name) AS type,
    c.version,
    s.value AS state,
    c.casedata,
    NOT s.is_terminal AS is_active,
    c.creation_timestamp AS creation_timestamp,
    c.modification_timestamp AS modification_timestamp,
    ( SELECT
                CASE
                    WHEN s.is_terminal = true THEN c.modification_timestamp - c.creation_timestamp
                    ELSE '00:00:00'::interval
                END AS "case") AS completed_case_duration,
    a.application_name,
    a.application_id,
    concat(a.major_version, '.', a.minor_version, '.', a.micro_version, '.', a.qualifier) AS application_version
   FROM cdm_cases_int c
     JOIN cdm_types t ON c.type_id = t.id
     JOIN cdm_datamodels d ON t.datamodel_id = d.id
     JOIN cdm_states s ON c.state_id = s.id
     JOIN cdm_applications a ON d.application_id = a.id
   WHERE c.marked_for_purge = FALSE 
   ORDER BY c.modification_timestamp desc;

 
 