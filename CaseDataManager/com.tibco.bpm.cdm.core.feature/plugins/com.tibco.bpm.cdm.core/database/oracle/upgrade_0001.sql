-- Add the new is_case_app boolean column to the cdm_applications table.
ALTER TABLE cdm_applications ADD is_case_app number(1)	DEFAULT 0;
