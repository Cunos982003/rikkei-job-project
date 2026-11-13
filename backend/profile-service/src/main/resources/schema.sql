CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    "group" VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS skill_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(20) NOT NULL CHECK (source IN ('LMS','EXAM','PM')),
    source_code VARCHAR(50) NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    weight NUMERIC(3,2) NOT NULL CHECK (weight > 0 AND weight <= 1),
    UNIQUE (source, source_code, skill_id)
);

CREATE TABLE IF NOT EXISTS skill_evidences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    source VARCHAR(20) NOT NULL CHECK (source IN ('LMS','EXAM','PM','INTERNSHIP','MENTOR','SELF')),
    source_ref VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    score SMALLINT NOT NULL CHECK (score BETWEEN 0 AND 100),
    recorded_at TIMESTAMPTZ NOT NULL,
    verified BOOLEAN NOT NULL,
    UNIQUE (user_id, skill_id, source, source_ref)
);

CREATE TABLE IF NOT EXISTS skill_scores (
    user_id UUID NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    verified_score SMALLINT,
    self_score SMALLINT,
    level VARCHAR(20) NOT NULL CHECK (level IN ('Cơ bản','Khá','Vững','Nâng cao')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_skill_evidences_user_skill ON skill_evidences(user_id, skill_id);
CREATE INDEX IF NOT EXISTS idx_skill_mappings_source_code ON skill_mappings(source, source_code);

CREATE TABLE IF NOT EXISTS internships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL, batch_id VARCHAR(100) NOT NULL,
    batch_name VARCHAR(200), group_name VARCHAR(200), topic_name VARCHAR(300), mentor_email VARCHAR(200),
    start_date DATE, end_date DATE, final_score NUMERIC(5,2), synced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, batch_id)
);
CREATE TABLE IF NOT EXISTS internship_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), internship_id UUID NOT NULL REFERENCES internships(id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL, accepted_score NUMERIC(5,2), accepted_at TIMESTAMPTZ,
    UNIQUE(internship_id, title)
);
CREATE TABLE IF NOT EXISTS portfolio_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_id UUID NOT NULL, source VARCHAR(20) NOT NULL,
    source_ref VARCHAR(100) NOT NULL, name VARCHAR(300) NOT NULL, role VARCHAR(300), created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, source, source_ref)
);

-- Candidate Profile Tables
CREATE TABLE IF NOT EXISTS candidate_profiles (
    user_id UUID PRIMARY KEY,
    full_name VARCHAR(200),
    dob DATE,
    gender VARCHAR(20),
    phone VARCHAR(50),
    email VARCHAR(200),
    address VARCHAR(500),
    avatar_url VARCHAR(500),
    headline VARCHAR(500),
    summary TEXT,
    program VARCHAR(100),
    class_code VARCHAR(50),
    course_name VARCHAR(200),
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'PENDING', 'PUBLIC', 'REJECTED', 'RECRUITER')),
    consent_at TIMESTAMPTZ,
    consent_withdrawn_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS educations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    school VARCHAR(200) NOT NULL,
    major VARCHAR(200),
    from_date DATE,
    to_date DATE,
    gpa NUMERIC(3,2),
    note TEXT
);

CREATE TABLE IF NOT EXISTS certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    issuer VARCHAR(200),
    issued_at DATE,
    expires_at DATE,
    file_url VARCHAR(500),
    file_type VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_educations_user_id ON educations(user_id);
CREATE INDEX IF NOT EXISTS idx_certificates_user_id ON certificates(user_id);

-- Profile Visibility Workflow Tables
CREATE TABLE IF NOT EXISTS profile_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL CHECK (action IN ('SUBMIT','APPROVE','REJECT','WITHDRAW')),
    reason TEXT,
    actor_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_profile_reviews_user_id ON profile_reviews(user_id);

-- File metadata (chứng chỉ, ảnh minh chứng, ảnh đại diện)
CREATE TABLE IF NOT EXISTS files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL CHECK (content_type IN ('application/pdf', 'image/jpeg', 'image/png')),
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    storage_path VARCHAR(500) NOT NULL,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('CERTIFICATE', 'EVIDENCE', 'AVATAR')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);

-- Trọng số giữa các nguồn bằng chứng khi tính chỉ số tổng hợp
CREATE TABLE IF NOT EXISTS score_source_weights (
    source VARCHAR(20) PRIMARY KEY CHECK (source IN ('LMS', 'EXAM', 'PM', 'INTERNSHIP', 'MENTOR', 'SELF')),
    weight NUMERIC(3,2) NOT NULL CHECK (weight > 0 AND weight <= 1)
);

-- Quy tắc cấp huy hiệu theo kỹ năng
CREATE TABLE IF NOT EXISTS badge_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    skill_id UUID REFERENCES skills(id),
    min_score NUMERIC(5,2) NOT NULL CHECK (min_score >= 0 AND min_score <= 100),
    need_internship_task BOOLEAN NOT NULL DEFAULT FALSE
);

-- Huy hiệu đã cấp cho hồ sơ ứng viên
CREATE TABLE IF NOT EXISTS profile_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    badge_rule_id UUID NOT NULL REFERENCES badge_rules(id),
    granted_type VARCHAR(10) NOT NULL CHECK (granted_type IN ('AUTO', 'MANUAL')),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    UNIQUE (user_id, badge_rule_id)
);

CREATE INDEX IF NOT EXISTS idx_profile_badges_user_id ON profile_badges(user_id);

-- Nhật ký đồng bộ định kỳ từ các hệ thống nguồn
CREATE TABLE IF NOT EXISTS sync_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'PARTIAL', 'FAILED')),
    error TEXT
);

-- Chi tiết từng nguồn trong một lượt đồng bộ (một nguồn lỗi không dừng cả lượt)
CREATE TABLE IF NOT EXISTS sync_run_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL REFERENCES sync_runs(id) ON DELETE CASCADE,
    source VARCHAR(20) NOT NULL CHECK (source IN ('LMS', 'EXAM', 'PM')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    records_processed INT NOT NULL DEFAULT 0,
    error TEXT,
    UNIQUE (run_id, source)
);

CREATE INDEX IF NOT EXISTS idx_sync_run_sources_run_id ON sync_run_sources(run_id);