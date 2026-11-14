-- =============================================================================
-- Notification DB: Notifications
-- =============================================================================
-- Run against notification_db

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

INSERT INTO notifications (id, user_id, type, title, body, read_at, created_at) VALUES
('c0000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'PROFILE_REVIEW', 'Hồ sơ được duyệt', 'Hồ sơ của bạn đã được duyệt và hiện đang PUBLIC', now() - interval '2 days', now() - interval '3 days'),
('c0000000-0000-0000-0000-000000000002', '550e8400-e29b-41d4-a716-446655440000', 'INTERVIEW_REQUEST', 'Yêu cầu phỏng vấn mới', 'Rikkeisoft mời bạn phỏng vấn vị trí Java Backend Developer', NULL, now() - interval '1 day'),
('c0000000-0000-0000-0000-000000000003', '660e8400-e29b-41d4-a716-446655440000', 'SHORTLIST', 'Được thêm vào shortlist', 'Rikkei Digital Solutions đã thêm bạn vào danh sách quan tâm', now() - interval '4 days', now() - interval '5 days'),
('c0000000-0000-0000-0000-000000000004', '770e8400-e29b-41d4-a716-446655440000', 'INTERVIEW_SCHEDULED', 'Lịch phỏng vấn', 'Phỏng vấn với Rikkei Digital Solutions vào lúc 10:00 ngày mai', NULL, now() - interval '12 hours'),
('c0000000-0000-0000-0000-000000000005', '550e8400-e29b-41d4-a716-446655440000', 'EVIDENCE_REPORT', 'Có báo sai bằng chứng', 'Có người báo sai bằng chứng Java Core Course của bạn', now() - interval '1 day', now() - interval '2 days'),
('c0000000-0000-0000-0000-000000000006', '550e8400-e29b-41d4-a716-446655440000', 'BADGE_EARNED', 'Đã nhận huy hiệu mới', 'Bạn đã nhận huy hiệu Java Expert', now() - interval '6 hours', now() - interval '1 day'),
('c0000000-0000-0000-0000-000000000007', '770e8400-e29b-41d4-a716-446655440000', 'JOB_MATCH', 'Việc làm phù hợp', 'Có 3 vị trí phù hợp với kỹ năng của bạn', NULL, now() - interval '3 hours')
ON CONFLICT (id) DO NOTHING;