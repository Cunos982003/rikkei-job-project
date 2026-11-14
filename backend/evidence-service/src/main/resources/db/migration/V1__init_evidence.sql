-- ==============================================================================
-- V1__init_evidence.sql: Báo sai bằng chứng (Evidence Reports)
-- ==============================================================================

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

-- Seed: 5 evidence reports từ evaluations.json
INSERT INTO evidence_reports (id, evidence_id, user_id, reporter_id, reason, status, handled_by, created_at, resolved_at) VALUES
('50000000-0000-0000-0000-000000000001',
 (SELECT id FROM skill_evidences WHERE user_id='550e8400-e29b-41d4-a716-446655440000' AND source='LMS' AND source_ref='JAVA01' LIMIT 1),
 '550e8400-e29b-41d4-a716-446655440000', 'user-006', 'Điểm LMS không khớp với kết quả thực tế', 'OPEN', NULL, now() - interval '10 days', NULL),
('50000000-0000-0000-0000-000000000002',
 (SELECT id FROM skill_evidences WHERE user_id='550e8400-e29b-41d4-a716-446655440000' AND source='EXAM' AND source_ref='JAVA_MID' LIMIT 1),
 '550e8400-e29b-41d4-a716-446655440000', 'user-007', 'Đề thi bị rò rỉ, cần rà soát lại', 'RESOLVED', 'user-007', now() - interval '20 days', now() - interval '5 days'),
('50000000-0000-0000-0000-000000000003',
 (SELECT id FROM skill_evidences WHERE user_id='550e8400-e29b-41d4-a716-446655440000' AND source='PM' AND source_ref='IOC_JAVA' LIMIT 1),
 '550e8400-e29b-41d4-a716-446655440000', 'user-006', 'Nhiệm vụ chưa hoàn thành nhưng đã chấm', 'REJECTED', 'user-006', now() - interval '15 days', now() - interval '2 days'),
('50000000-0000-0000-0000-000000000004',
 (SELECT id FROM skill_evidences WHERE user_id='user-002' AND source='LMS' AND source_ref='FE-01' LIMIT 1),
 'user-002', 'user-007', 'Điểm attendance rate bị sai', 'OPEN', NULL, now() - interval '5 days', NULL),
('50000000-0000-0000-0000-000000000005',
 (SELECT id FROM skill_evidences WHERE user_id='user-003' AND source='PM' AND source_ref='IOC_FULLSTACK' LIMIT 1),
 'user-003', 'user-006', 'Task Kafka chưa merge nhưng đã chấm accepted', 'RESOLVED', 'user-006', now() - interval '3 days', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;