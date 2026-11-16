-- =============================================================================
-- Profile DB: Additional DDL from schema.sql (6 new tables)
-- =============================================================================
-- Run against profile_db

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