-- =============================================================================
-- Evidence DB: Evidence Reports
-- =============================================================================
-- Run against evidence_db

CREATE TABLE IF NOT EXISTS evidence_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evidence_id UUID NOT NULL,
    user_id UUID NOT NULL,
    reporter_id UUID,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED', 'REJECTED')),
    handled_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_evidence_reports_evidence ON evidence_reports(evidence_id);
CREATE INDEX IF NOT EXISTS idx_evidence_reports_user ON evidence_reports(user_id);
CREATE INDEX IF NOT EXISTS idx_evidence_reports_status ON evidence_reports(status);

-- Seed: 5 evidence reports (will insert after skill_evidences exist in profile_db)
-- These use fixed IDs to avoid subquery dependency
INSERT INTO evidence_reports (id, evidence_id, user_id, reporter_id, reason, status, handled_by, created_at, resolved_at) VALUES
('50000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', '550e8400-e29b-41d4-a716-446655440000', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Điểm LMS không khớp với kết quả thực tế', 'OPEN', NULL, now() - interval '10 days', NULL),
('50000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000', '550e8400-e29b-41d4-a716-446655440000', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Đề thi bị rò rỉ, cần rà soát lại', 'RESOLVED', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', now() - interval '20 days', now() - interval '5 days'),
('50000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000000', '550e8400-e29b-41d4-a716-446655440000', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Nhiệm vụ chưa hoàn thành nhưng đã chấm', 'REJECTED', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '15 days', now() - interval '2 days'),
('50000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000000', '660e8400-e29b-41d4-a716-446655440000', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'Điểm attendance rate bị sai', 'OPEN', NULL, now() - interval '5 days', NULL),
('50000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000000', '770e8400-e29b-41d4-a716-446655440000', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'Task Kafka chưa merge nhưng đã chấm accepted', 'RESOLVED', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '3 days', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;