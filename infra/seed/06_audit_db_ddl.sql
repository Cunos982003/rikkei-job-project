-- =============================================================================
-- Audit DB: Edit History & Audit Logs
-- =============================================================================
-- Run against audit_db

CREATE TABLE IF NOT EXISTS edit_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(30) NOT NULL CHECK (entity_type IN ('EVIDENCE', 'COMPETENCY_SCORE', 'PROFILE_BADGE', 'CANDIDATE_PROFILE', 'CERTIFICATE', 'EDUCATION', 'INTERNSHIP', 'PORTFOLIO_PROJECT')),
    entity_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    value_before JSONB,
    value_after JSONB,
    reason VARCHAR(500),
    actor_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_edit_history_entity ON edit_history(entity_type, entity_id);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('USER', 'PARTNER', 'SYSTEM', 'STAFF', 'ADMIN')),
    actor_id UUID,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id UUID,
    detail JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_type, actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target ON audit_logs(target_type, target_id);

-- Trigger chặn UPDATE/DELETE
DROP TRIGGER IF EXISTS trg_edit_history_immutable ON edit_history;
CREATE OR REPLACE FUNCTION prevent_modify() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Bảng % chỉ được thêm, không sửa/xoá', TG_TABLE_NAME;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_edit_history_immutable
BEFORE UPDATE OR DELETE ON edit_history
FOR EACH ROW EXECUTE FUNCTION prevent_modify();

DROP TRIGGER IF EXISTS trg_audit_logs_immutable ON audit_logs;
CREATE TRIGGER trg_audit_logs_immutable
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_modify();

-- ==============================================================================
-- SEED DATA
-- ==============================================================================

INSERT INTO edit_history (id, entity_type, entity_id, action, value_before, value_after, reason, actor_id, created_at) VALUES
('d0000000-0000-0000-0000-000000000001', 'CANDIDATE_PROFILE', '550e8400-e29b-41d4-a716-446655440000', 'UPDATE_PROFILE',
 '{"headline": "Học viên Java", "summary": "Java developer"}',
 '{"headline": "Java Backend Developer", "summary": "Học viên xuất sắc khóa Java Fullstack"}',
 'Cập nhật headline & summary', '550e8400-e29b-41d4-a716-446655440000', now() - interval '5 days'),
('d0000000-0000-0000-0000-000000000002', 'EVIDENCE', '00000000-0000-0000-0000-000000000000', 'UPDATE_EVIDENCE',
 '{"score": 88, "verified": false}', '{"score": 90, "verified": true}',
 'Mentor xác thực kỹ năng React', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', now() - interval '3 days'),
('d0000000-0000-0000-0000-000000000003', 'PROFILE_BADGE', '00000000-0000-0000-0000-000000000000', 'GRANT_BADGE',
 '{}', '{"badge_rule_id": "BR-JAVA-EXPERT", "granted_type": "AUTO"}',
 'Tự động cấp huy hiệu Java Expert', 'ffffffff-ffff-ffff-ffff-ffffffffffff', now() - interval '2 days'),
('d0000000-0000-0000-0000-000000000004', 'CERTIFICATE', '00000000-0000-0000-0000-000000000000', 'ADD_CERTIFICATE',
 '{}', '{"name": "AWS Certified Solutions Architect", "issuer": "AWS"}',
 'Thêm chứng chỉ AWS mới', '550e8400-e29b-41d4-a716-446655440000', now() - interval '1 day'),
('d0000000-0000-0000-0000-000000000005', 'INTERNSHIP', '00000000-0000-0000-0000-000000000000', 'SYNC_INTERNSHIP',
 '{"final_score": 85.5, "synced_at": "2026-10-15T10:00:00Z"}',
 '{"final_score": 86.0, "synced_at": "2026-11-01T10:00:00Z"}',
 'Đồng bộ lại điểm thực tập từ PM', 'ffffffff-ffff-ffff-ffff-ffffffffffff', now() - interval '6 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_logs (id, actor_type, actor_id, action, target_type, target_id, detail, created_at) VALUES
('e0000000-0000-0000-0000-000000000001', 'USER', '550e8400-e29b-41d4-a716-446655440000', 'LOGIN', 'SESSION', '550e8400-e29b-41d4-a716-446655440000', '{"ip": "192.168.1.100", "user_agent": "Chrome/119"}', now() - interval '5 days'),
('e0000000-0000-0000-0000-000000000002', 'STAFF', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'APPROVE_PROFILE', 'CANDIDATE_PROFILE', '550e8400-e29b-41d4-a716-446655440000', '{"previous_status": "PENDING", "new_status": "PUBLIC"}', now() - interval '3 days'),
('e0000000-0000-0000-0000-000000000003', 'PARTNER', '80000000-0000-0000-0000-000000000001', 'VIEW_PROFILE', 'CANDIDATE_PROFILE', '770e8400-e29b-41d4-a716-446655440000', '{"access_type": "DOWNLOAD_PDF"}', now() - interval '2 days'),
('e0000000-0000-0000-0000-000000000004', 'SYSTEM', NULL, 'SYNC_RUN_COMPLETED', 'SYNC_RUN', '00000000-0000-0000-0000-000000000001', '{"sources": ["LMS", "EXAM", "PM"], "records": 127}', now() - interval '1 day'),
('e0000000-0000-0000-0000-000000000005', 'ADMIN', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'DELETE_USER', 'USER', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '{"reason": "GDPR request", "deleted_at": "2026-11-01"}', now() - interval '12 hours'),
('e0000000-0000-0000-0000-000000000006', 'USER', '660e8400-e29b-41d4-a716-446655440000', 'UPDATE_PROFILE', 'CANDIDATE_PROFILE', '660e8400-e29b-41d4-a716-446655440000', '{"fields": ["headline", "summary"]}', now() - interval '4 days'),
('e0000000-0000-0000-0000-000000000007', 'STAFF', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'REJECT_PROFILE', 'CANDIDATE_PROFILE', '660e8400-e29b-41d4-a716-446655440000', '{"reason": "Kỹ năng không đủ yêu cầu"}', now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;