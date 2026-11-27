-- ==============================================================================
-- V1__init_notification.sql: Thông báo hệ thống
-- ==============================================================================

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id, created_at DESC);

-- Seed: 5 notifications
INSERT INTO notifications (id, user_id, type, title, body, read_at, created_at) VALUES
('c0000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'PROFILE_REVIEW', 'Hồ sơ được duyệt',
 'Hồ sơ của bạn đã được duyệt và hiện đang PUBLIC', now() - interval '2 days', now() - interval '3 days'),
('c0000000-0000-0000-0000-000000000002', '550e8400-e29b-41d4-a716-446655440000', 'INTERVIEW_REQUEST', 'Yêu cầu phỏng vấn mới',
 'Rikkeisoft mời bạn phỏng vấn vị trí Java Backend Developer', NULL, now() - interval '1 day'),
('c0000000-0000-0000-0000-000000000003', '550e8400-e29b-41d4-a716-446655440001', 'SHORTLIST', 'Được thêm vào shortlist',
 'Rikkei Digital Solutions đã thêm bạn vào danh sách quan tâm', now() - interval '4 days', now() - interval '5 days'),
('c0000000-0000-0000-0000-000000000004', '550e8400-e29b-41d4-a716-446655440002', 'INTERVIEW_SCHEDULED', 'Lịch phỏng vấn',
 'Phỏng vấn với Rikkei Digital Solutions vào lúc 10:00 ngày mai', NULL, now() - interval '12 hours'),
('c0000000-0000-0000-0000-000000000005', '550e8400-e29b-41d4-a716-446655440003', 'EVIDENCE_REPORT', 'Có báo sai bằng chứng',
 'Có người báo sai bằng chứng Java Core Course của bạn', now() - interval '1 day', now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;
