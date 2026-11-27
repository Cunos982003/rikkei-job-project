-- Fix created_by column type in jobs table
-- The entity expects VARCHAR(100) but the database has UUID

ALTER TABLE jobs
ALTER COLUMN created_by TYPE VARCHAR(100) USING created_by::text;