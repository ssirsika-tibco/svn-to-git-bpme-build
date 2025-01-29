-- Add a new index on columns referencing cdm_cases_int table.
CREATE INDEX ix_cdm_case_links_end1_id ON cdm_case_links (end1_id);
CREATE INDEX ix_cdm_case_links_end2_id ON cdm_case_links (end2_id);

