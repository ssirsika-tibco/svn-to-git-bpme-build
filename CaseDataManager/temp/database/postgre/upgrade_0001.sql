-- Add the new is_case_app boolean column to the cdm_applications table.
ALTER TABLE cdm_applications ADD COLUMN is_case_app BOOLEAN DEFAULT false;
