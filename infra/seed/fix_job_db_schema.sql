-- ==============================================================================
-- FIX SCHEMA AND MIGRATIONS FOR JOB_DB
-- Khớp schema database job_db với Entity Job, JobSkill và Flyway V4 của job-service
-- ==============================================================================

-- 1. Bổ sung các cột còn thiếu vào bảng jobs nếu chưa có
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS position VARCHAR(255);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS level VARCHAR(20);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS type VARCHAR(20);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS quantity INT DEFAULT 5;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS city VARCHAR(100);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS address VARCHAR(500);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS work_mode VARCHAR(20) DEFAULT 'ONSITE';
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS deadline DATE;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS benefits TEXT;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS rejected_reason VARCHAR(1000);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(100);
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS company_actions (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    target_id UUID,
    reason VARCHAR(1000),
    staff_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_company_actions_company ON company_actions(company_id, created_at);

-- 2. Đồng bộ dữ liệu từ các cột cũ sang cột mới cho 120 jobs hiện có
UPDATE jobs SET
    position = COALESCE(position, title),
    level = COALESCE(level, experience_level, 'FRESHER'),
    type = COALESCE(type, CASE 
        WHEN UPPER(job_type) LIKE '%FULL%' THEN 'FULLTIME'
        WHEN UPPER(job_type) LIKE '%PART%' THEN 'PARTTIME'
        ELSE 'INTERNSHIP'
    END),
    quantity = COALESCE(quantity, 5),
    city = COALESCE(city, CASE 
        WHEN work_location ILIKE '%Hà Nội%' THEN 'Hà Nội'
        WHEN work_location ILIKE '%Đà Nẵng%' THEN 'Đà Nẵng'
        ELSE 'Hồ Chí Minh'
    END),
    address = COALESCE(address, work_location, 'Hà Nội'),
    work_mode = COALESCE(work_mode, 'ONSITE'),
    deadline = COALESCE(deadline, expires_at::date, (CURRENT_DATE + interval '30 days')::date),
    benefits = COALESCE(benefits, 'Lương thưởng cạnh tranh, BHXH theo luật, hỗ trợ trang thiết bị làm việc');

-- Đổi kiểu created_by sang VARCHAR(100) khớp với Entity Job.java
ALTER TABLE jobs ALTER COLUMN created_by TYPE VARCHAR(100) USING created_by::text;

-- Đặt NOT NULL cho các trường bắt buộc của Entity Job.java
ALTER TABLE jobs ALTER COLUMN position SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN level SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN type SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN quantity SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN work_mode SET NOT NULL;
ALTER TABLE jobs ALTER COLUMN deadline SET NOT NULL;

-- 3. Cập nhật bảng job_skills cho khớp với Entity JobSkill.java
ALTER TABLE job_skills ADD COLUMN IF NOT EXISTS skill_id VARCHAR(64);
ALTER TABLE job_skills ADD COLUMN IF NOT EXISTS required BOOLEAN DEFAULT TRUE;

UPDATE job_skills SET
    skill_id = COALESCE(skill_id, id::text, gen_random_uuid()::text),
    required = COALESCE(required, is_mandatory, true);

ALTER TABLE job_skills ALTER COLUMN skill_id SET NOT NULL;
ALTER TABLE job_skills ALTER COLUMN required SET NOT NULL;

-- 4. Đồng bộ bảng flyway_schema_history trong job_db
-- Đánh dấu các migration V2, V3, V4 đã hoàn tất để Flyway không chạy lại
DELETE FROM flyway_schema_history WHERE installed_rank > 1;
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES 
    (2, '2', 'add hr account lock columns', 'SQL', 'V2__add_hr_account_lock_columns.sql', NULL, 'postgres', now(), 10, true),
    (3, '3', 'drop local users', 'SQL', 'V3__drop_local_users.sql', NULL, 'postgres', now(), 10, true),
    (4, '4', 'create jobs and job skills', 'SQL', 'V4__create_jobs_and_job_skills.sql', NULL, 'postgres', now(), 10, true);
