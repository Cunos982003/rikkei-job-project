-- ============================================================================
-- V3__nt05_external_sync.sql: NT-05 External records sync and manual overrides
-- ============================================================================

-- 1. Bảng lưu trữ bản ghi thô từ các hệ thống ngoài (LMS, EXAM, PM)
CREATE TABLE IF NOT EXISTS external_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    source VARCHAR(20) NOT NULL CHECK (source IN ('LMS', 'EXAM', 'PM')),
    source_key VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    synced_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp(),
    CONSTRAINT uq_external_records_source_key UNIQUE (source, source_key)
);

CREATE INDEX IF NOT EXISTS idx_external_records_user_id ON external_records(user_id);
CREATE INDEX IF NOT EXISTS idx_external_records_source ON external_records(source);
CREATE INDEX IF NOT EXISTS idx_external_records_synced_at ON external_records(synced_at DESC);

-- 2. Bảng lưu các trường do cán bộ vận hành can thiệp sửa tay (Manual Overrides)
CREATE TABLE IF NOT EXISTS manual_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id UUID NOT NULL REFERENCES external_records(id) ON DELETE CASCADE,
    field VARCHAR(50) NOT NULL,
    value TEXT NOT NULL,
    reason TEXT NOT NULL,
    staff_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp(),
    CONSTRAINT uq_manual_overrides_record_field UNIQUE (record_id, field)
);

CREATE INDEX IF NOT EXISTS idx_manual_overrides_record_id ON manual_overrides(record_id);
CREATE INDEX IF NOT EXISTS idx_manual_overrides_staff_id ON manual_overrides(staff_id);

-- 3. Cập nhật bảng lịch sử các lần chạy đồng bộ (sync_runs)
-- Bổ sung các cột theo đặc tả NT-05 (nếu chưa có)
ALTER TABLE sync_runs ADD COLUMN IF NOT EXISTS trigger VARCHAR(20) DEFAULT 'MANUAL';
ALTER TABLE sync_runs ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE sync_runs ADD COLUMN IF NOT EXISTS finished_at TIMESTAMPTZ;
ALTER TABLE sync_runs ADD COLUMN IF NOT EXISTS counts JSONB DEFAULT '{}'::jsonb;

-- Đảm bảo ràng buộc trigger an toàn
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_sync_runs_trigger'
    ) THEN
        ALTER TABLE sync_runs ADD CONSTRAINT chk_sync_runs_trigger 
            CHECK (trigger IN ('NIGHTLY', 'ON_OPEN', 'MANUAL'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sync_runs_status_started ON sync_runs(status, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_sync_runs_user_id ON sync_runs(user_id);

-- 4. Bổ sung ánh xạ kỹ năng (skill_mappings) cho các mã môn thực tế từ LMS, EXAM, PM mock
INSERT INTO skill_mappings (source, source_code, skill_id, weight) VALUES
    ('LMS', 'JV-01', (SELECT id FROM skills WHERE code='JAVA_CORE' LIMIT 1), 1.00),
    ('LMS', 'DB-01', (SELECT id FROM skills WHERE code='SQL' LIMIT 1), 1.00),
    ('LMS', 'SB-01', (SELECT id FROM skills WHERE code='SPRING_BOOT' LIMIT 1), 1.00),
    ('LMS', 'GIT-01', (SELECT id FROM skills WHERE code='GIT' LIMIT 1), 1.00),
    ('LMS', 'FE-01', (SELECT id FROM skills WHERE code='HTML_CSS' LIMIT 1), 1.00),
    ('LMS', 'JS-01', (SELECT id FROM skills WHERE code='JAVASCRIPT' LIMIT 1), 1.00),
    ('LMS', 'RC-01', (SELECT id FROM skills WHERE code='REACT' LIMIT 1), 1.00),
    ('EXAM', 'JV-01', (SELECT id FROM skills WHERE code='JAVA_CORE' LIMIT 1), 1.00),
    ('EXAM', 'DB-01', (SELECT id FROM skills WHERE code='SQL' LIMIT 1), 1.00),
    ('EXAM', 'SB-01', (SELECT id FROM skills WHERE code='SPRING_BOOT' LIMIT 1), 1.00),
    ('EXAM', 'FE-01', (SELECT id FROM skills WHERE code='HTML_CSS' LIMIT 1), 1.00),
    ('EXAM', 'JS-01', (SELECT id FROM skills WHERE code='JAVASCRIPT' LIMIT 1), 1.00),
    ('EXAM', 'RC-01', (SELECT id FROM skills WHERE code='REACT' LIMIT 1), 1.00),
    ('EXAM', 'EXAM-JV-2026', (SELECT id FROM skills WHERE code='JAVA_CORE' LIMIT 1), 1.00),
    ('EXAM', 'EXAM-DB-2026', (SELECT id FROM skills WHERE code='SQL' LIMIT 1), 1.00),
    ('EXAM', 'EXAM-SB-2026', (SELECT id FROM skills WHERE code='SPRING_BOOT' LIMIT 1), 1.00),
    ('PM', 'IOC-2026-B1', (SELECT id FROM skills WHERE code='SPRING_BOOT' LIMIT 1), 0.60),
    ('PM', 'IOC-2026-B1', (SELECT id FROM skills WHERE code='TEAMWORK' LIMIT 1), 0.40)
ON CONFLICT (source, source_code, skill_id) DO NOTHING;
