DROP SEQUENCE IF EXISTS cdm_cases_seq;
DROP SEQUENCE IF EXISTS cdm_datamodels_seq;
DROP TABLE IF EXISTS cdm_links;
DROP TABLE IF EXISTS cdm_identifier_infos;
DROP TABLE IF EXISTS cdm_cases;
DROP TABLE IF EXISTS cdm_datamodels;

CREATE SEQUENCE cdm_cases_seq START WITH 1 INCREMENT BY 1;

CREATE SEQUENCE cdm_datamodels_seq START WITH 1001 INCREMENT BY 1;

CREATE TABLE cdm_datamodels
(
  id                        NUMERIC(28,0) NOT NULL, -- id assigned from a sequence
  application_id            VARCHAR(256) NOT NULL,  -- e.g "com.tibco.myPackage.SimpleApplication" (according to deployment FP)
  application_major_version NUMERIC(28,0) NOT NULL, -- e.g. 1 (derived from application_id)
  application_version       VARCHAR(256) NOT NULL, -- e.g. "1.0.0.20190201130800"
  namespace                VARCHAR(256) NOT NULL, -- e.g. "com.example.carmodel"
  model                     TEXT NOT NULL,
  deployment_timestamp      TIMESTAMP NOT NULL,
  CONSTRAINT cdm_datamodels_pk PRIMARY KEY (id),
  CONSTRAINT cdm_datamodels_unq UNIQUE (application_id, application_major_version)
);

CREATE TABLE cdm_identifier_infos
(
  datamodel_id              NUMERIC(28,0) NOT NULL, -- Foreign key to cdm_datamodels table
  type_id                   NUMERIC(28,0) NOT NULL,
  prefix                    VARCHAR(10),
  suffix                    VARCHAR(10),
  min_num_length            NUMERIC(2,0),
  next_num                  NUMERIC(15,0) NOT NULL,
  CONSTRAINT cdm_identifier_infos_pm PRIMARY KEY (datamodel_id, type_id),
  CONSTRAINT cdm_identifier_infos_fk1 FOREIGN KEY (datamodel_id)
    REFERENCES cdm_datamodels (id) ON DELETE CASCADE
);

CREATE TABLE cdm_cases
(
  id                        NUMERIC(28,0) NOT NULL, -- (like 'bds_id' in BDS)
  version                  NUMERIC(15,0) NOT NULL, -- (like 'bds_version' in BDS)
  datamodel_id              NUMERIC(28,0) NOT NULL, -- Foreign key to cdm_datamodels table
  type_id                   NUMERIC(28,0) NOT NULL,
  casedata                  JSONB NOT NULL,
  cid                       VARCHAR(256) NOT NULL,
  state_id                  NUMERIC(28,0) NOT NULL,
  modification_timestamp    TIMESTAMP NOT NULL,
  CONSTRAINT cdm_cases_pk PRIMARY KEY (id, datamodel_id, type_id),
  CONSTRAINT cdm_cases_fk1 FOREIGN KEY (datamodel_id)
	REFERENCES cdm_datamodels (id) ON DELETE CASCADE
);

CREATE TABLE cdm_links
(
  datamodel_id              NUMERIC(28,0) NOT NULL, -- Foreign key to cdm_datamodels table
  link_id                   NUMERIC(28,0) NOT NULL,
  end1_id                   NUMERIC(28,0) NOT NULL,
  end2_id                   NUMERIC(28,0) NOT NULL,
  CONSTRAINT cdm_links_pk PRIMARY KEY (datamodel_id, link_id, end1_id, end2_id),
  CONSTRAINT cdm_links_fk1 FOREIGN KEY (datamodel_id)
    REFERENCES cdm_datamodels (id) ON DELETE CASCADE
);

/*

-- Deploy applications (example app id taken from Deployment FP)
INSERT INTO cdm_datamodels VALUES (NEXTVAL('cdm_datamodels_seq'), 'com.tibco.myPackage.SimpleApplication1', 1, '1.0.0.20190131101100', 'com.example.carmodel', '{"fake":"datamodel"}', now());
INSERT INTO cdm_datamodels VALUES (NEXTVAL('cdm_datamodels_seq'), 'com.tibco.myPackage.SimpleApplication2', 1, '1.0.0.20190131152100', 'com.example.fruitmodel', '{"fake":"datamodel"}', now());

-- Indexes for Car app (partial index, constrained to Car case type)
CREATE INDEX idx_cdm_cases_car_1 ON cdm_cases ((casedata->'make')) WHERE datamodel_id = 1001 AND type_id = 1;
CREATE INDEX idx_cdm_cases_car_2 ON cdm_cases ((casedata->'model')) WHERE datamodel_id = 1001 AND type_id = 1;

-- Indexes for Fruit app (partial index, constrained to Fruit case type)
CREATE INDEX idx_cdm_cases_fruit_1 ON cdm_cases ((casedata->'name')) WHERE datamodel_id = 1002 AND type_id = 1;
CREATE INDEX idx_cdm_cases_fruit_2 ON cdm_cases ((casedata->'colour')) WHERE datamodel_id = 1002 AND type_id = 1;

-- Create Cars
INSERT INTO cdm_cases VALUES (1, 0, 1001, 1, '{"reg": "YT53NMZ", "make": "Vauxhall", "model": "Zafira"}', 'YT53NMZ', 1, now());
INSERT INTO cdm_cases VALUES (2, 0, 1001, 1, '{"reg": "A123XYZ", "make": "Vauxhall", "model": "Corsa"}', 'A123XYZ', 1, now());

-- Create Fruits
INSERT INTO cdm_cases VALUES (1, 0, 1002, 1, '{"name": "Banana", "colour": "Yellow"}', 'Banana', 1, now());
INSERT INTO cdm_cases VALUES (2, 0, 1002, 1, '{"name": "Plum", "colour": "Purple"}', 'Plum', 1, now());

-- Uses idx_cd_cases_car_1
EXPLAIN SELECT * FROM cdm_cases WHERE datamodel_id = 1001 AND type_id = 1 AND casedata -> 'model' = '"Corsa"';

-- Uses idx_cd_cases_fruit_2
EXPLAIN SELECT * FROM cdm_cases WHERE datamodel_id = 1002 AND type_id = 1 AND casedata -> 'colour' = '"Yellow"';

*/