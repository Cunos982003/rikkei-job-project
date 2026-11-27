-- HS-04 CV Schema Update
-- Update CV and CV Section tables to match spec

-- Add target_position and template to cvs table
ALTER TABLE cvs
    ADD COLUMN IF NOT EXISTS target_position VARCHAR(200),
    ADD COLUMN IF NOT EXISTS template VARCHAR(50) NOT NULL DEFAULT 'RIKKEI_STANDARD';

-- Update cv_sections table: remove content_json, add item_ids and included
-- First, add new columns
ALTER TABLE cv_sections
    ADD COLUMN IF NOT EXISTS item_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS included BOOLEAN NOT NULL DEFAULT true;

-- Drop existing check constraint on section_type (auto-generated name from inline CHECK)
-- Must drop BEFORE updating data, because old constraint only allows lowercase values
-- Try multiple possible auto-generated constraint names
DO $$
DECLARE
    constraint_name text;
BEGIN
    -- Try to find and drop the constraint by checking constraint definition
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'cv_sections'::regclass
    AND contype = 'c'
    AND pg_get_constraintdef(oid) ILIKE '%section_type%';

    IF constraint_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE cv_sections DROP CONSTRAINT ' || quote_ident(constraint_name);
    END IF;

    -- Also try common auto-generated names as fallback
    BEGIN
        EXECUTE 'ALTER TABLE cv_sections DROP CONSTRAINT IF EXISTS cv_sections_section_type_check';
    EXCEPTION WHEN OTHERS THEN
        -- Ignore errors
    END;
    BEGIN
        EXECUTE 'ALTER TABLE cv_sections DROP CONSTRAINT IF EXISTS cv_sections_section_type_check1';
    EXCEPTION WHEN OTHERS THEN
        -- Ignore errors
    END;
END $$;

-- Migrate existing section_type values to new enum
-- Map old values to new enum values
UPDATE cv_sections SET section_type = 'SUMMARY' WHERE section_type = 'summary';
UPDATE cv_sections SET section_type = 'SKILLS' WHERE section_type = 'skills';
UPDATE cv_sections SET section_type = 'EDUCATION' WHERE section_type = 'education';
UPDATE cv_sections SET section_type = 'PROJECTS' WHERE section_type = 'projects';
UPDATE cv_sections SET section_type = 'CERTIFICATES' WHERE section_type = 'certificates';
UPDATE cv_sections SET section_type = 'INTERNSHIP' WHERE section_type = 'internship';
-- personal_info is not in the new enum, set to SUMMARY as default
UPDATE cv_sections SET section_type = 'SUMMARY' WHERE section_type = 'personal_info';
-- experience is not in the new enum, set to INTERNSHIP as default
UPDATE cv_sections SET section_type = 'INTERNSHIP' WHERE section_type = 'experience';

-- Add new check constraint with explicit name
ALTER TABLE cv_sections
    ADD CONSTRAINT cv_sections_section_type_check
    CHECK (section_type IN ('SUMMARY','SKILLS','EDUCATION','PROJECTS','CERTIFICATES','INTERNSHIP'));

-- Remove content_json column (after data migration if needed)
-- Note: Existing data in content_json will be lost. This is intentional per spec.
ALTER TABLE cv_sections
    DROP COLUMN IF EXISTS content_json;

-- Update existing sections to have default item_ids and included
UPDATE cv_sections
SET item_ids = '[]'::jsonb, included = true
WHERE item_ids IS NULL OR included IS NULL;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_cv_sections_item_ids ON cv_sections USING GIN (item_ids);