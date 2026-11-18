-- ==============================================================================
-- UT-06 Seed Data: Interview Slots & Schedules (Idempotent for recruitment_db)
-- Chạy trên PostgreSQL database: recruitment_db
-- ==============================================================================

-- 0. Đảm bảo bảng interview_slots và interview_schedules đã tồn tại
CREATE TABLE IF NOT EXISTS interview_slots (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES interview_rounds(id) ON DELETE CASCADE,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'PICKED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_interview_slots_time_order CHECK (end_at > start_at)
);

CREATE TABLE IF NOT EXISTS interview_schedules (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES interview_rounds(id) ON DELETE CASCADE,
    slot_id UUID NOT NULL REFERENCES interview_slots(id) ON DELETE CASCADE,
    meet_url VARCHAR(500) NOT NULL,
    reminded_24h_at TIMESTAMPTZ,
    reminded_1h_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'DONE', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 1. Tạo 2 đơn ứng tuyển INTERVIEW mẫu cho HR Account One (company 11111111-1111-4111-8111-111111111111)
--    Ứng viên 1: candidate1@rikkeijobs.test (ed854c07-f3eb-48f1-a186-4e7135f54645) -> đang có 3 khung giờ OPEN chờ chọn
--    Ứng viên 2: candidate2@rikkeijobs.test (797c5663-386b-4e96-9112-6d3e414b4449) -> đã chọn khung giờ & có meet_url trong tuần này
INSERT INTO recruitments (id, job_id, user_id, cv_id, company_id, candidate_email, cv_snapshot_url, cover_letter, status, created_at, updated_at)
VALUES
(
    '77060000-0000-4000-8000-000000000001',
    'd0000000-0000-4000-8000-000000000001',
    'ed854c07-f3eb-48f1-a186-4e7135f54645',
    '77061111-0000-4000-8000-000000000001',
    '11111111-1111-4111-8111-111111111111',
    'candidate1@rikkeijobs.test',
    '/recruitments/77060000-0000-4000-8000-000000000001/cv-snapshot',
    'Ứng tuyển Java Backend Developer (UT-06 chờ chọn khung giờ)',
    2,
    now() - interval '3 days',
    now() - interval '1 day'
),
(
    '77060000-0000-4000-8000-000000000002',
    'd0000000-0000-4000-8000-000000000001',
    '797c5663-386b-4e96-9112-6d3e414b4449',
    '77061111-0000-4000-8000-000000000002',
    '11111111-1111-4111-8111-111111111111',
    'candidate2@rikkeijobs.test',
    '/recruitments/77060000-0000-4000-8000-000000000002/cv-snapshot',
    'Ứng tuyển Frontend React Developer (UT-06 đã chốt lịch Meet)',
    2,
    now() - interval '4 days',
    now() - interval '1 day'
)
ON CONFLICT (id) DO NOTHING;

-- 2. Vòng phỏng vấn mẫu cho UT-06
INSERT INTO interview_rounds (id, recruitment_id, round_no, name, interviewer_email, scheduled_at, result, score, note, created_at)
VALUES
(
    '77062000-0000-4000-8000-000000000001',
    '77060000-0000-4000-8000-000000000001',
    1,
    'Phỏng vấn Kỹ thuật Java & Spring Boot',
    'mentor1@rikkeijobs.test',
    NULL,
    'PENDING',
    NULL,
    'HR đã đề xuất 3 khung giờ chờ ứng viên chọn',
    now() - interval '1 day'
),
(
    '77062000-0000-4000-8000-000000000002',
    '77060000-0000-4000-8000-000000000002',
    1,
    'Phỏng vấn Kỹ thuật React & TypeScript',
    'mentor2@rikkeijobs.test',
    now() + interval '1 day',
    'PENDING',
    NULL,
    'Ứng viên đã chọn lịch phỏng vấn trực tuyến qua Rikkei Meet',
    now() - interval '2 days'
)
ON CONFLICT (id) DO NOTHING;

-- 3. Khung giờ phỏng vấn (interview_slots)
--    Round 1: 3 khung OPEN trong tương lai (ứng viên candidate1 có thể bấm chọn ngay)
--    Round 2: 1 khung PICKED + 1 khung CANCELLED (test AC chọn khung đã hủy -> 409)
INSERT INTO interview_slots (id, round_id, start_at, end_at, status, created_at)
VALUES
(
    '77063000-0000-4000-8000-000000000001',
    '77062000-0000-4000-8000-000000000001',
    date_trunc('hour', now() + interval '1 day') + interval '2 hours',
    date_trunc('hour', now() + interval '1 day') + interval '3 hours',
    'OPEN',
    now() - interval '2 hours'
),
(
    '77063000-0000-4000-8000-000000000002',
    '77062000-0000-4000-8000-000000000001',
    date_trunc('hour', now() + interval '2 days') + interval '1 hour',
    date_trunc('hour', now() + interval '2 days') + interval '2 hours',
    'OPEN',
    now() - interval '2 hours'
),
(
    '77063000-0000-4000-8000-000000000003',
    '77062000-0000-4000-8000-000000000001',
    date_trunc('hour', now() + interval '3 days') + interval '2 hours',
    date_trunc('hour', now() + interval '3 days') + interval '3 hours',
    'OPEN',
    now() - interval '2 hours'
),
(
    '77063000-0000-4000-8000-000000000004',
    '77062000-0000-4000-8000-000000000002',
    date_trunc('hour', now() + interval '1 day'),
    date_trunc('hour', now() + interval '1 day') + interval '1 hour',
    'PICKED',
    now() - interval '1 day'
),
(
    '77063000-0000-4000-8000-000000000005',
    '77062000-0000-4000-8000-000000000002',
    date_trunc('hour', now() + interval '2 days'),
    date_trunc('hour', now() + interval '2 days') + interval '1 hour',
    'CANCELLED',
    now() - interval '1 day'
)
ON CONFLICT (id) DO NOTHING;

-- 4. Lịch phỏng vấn đã chốt (interview_schedules) cho Round 2
INSERT INTO interview_schedules (id, round_id, slot_id, meet_url, reminded_24h_at, reminded_1h_at, status, created_at, updated_at)
VALUES
(
    '77064000-0000-4000-8000-000000000001',
    '77062000-0000-4000-8000-000000000002',
    '77063000-0000-4000-8000-000000000004',
    'https://meet.rikkei.edu.vn/room-ut06-demo01',
    NULL,
    NULL,
    'SCHEDULED',
    now() - interval '1 day',
    now() - interval '1 day'
)
ON CONFLICT (id) DO NOTHING;
