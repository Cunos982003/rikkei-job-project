-- =============================================================================
-- infra/seed/ut07_offer_seed.sql : Dữ liệu mẫu kiểm thử đầy đủ kịch bản [UT-07]
-- Chạy trên database: recruitment_db
-- Khớp UUID users từ clean_and_sync_users.sql & JPA ordinal status (0..6)
-- =============================================================================

-- 1. Đảm bảo các bảng tồn tại
CREATE TABLE IF NOT EXISTS offers (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES recruitments(id) ON DELETE CASCADE,
    company_name VARCHAR(255),
    position VARCHAR(255) NOT NULL,
    salary NUMERIC(15, 2) NOT NULL CHECK (salary > 0),
    start_date DATE NOT NULL,
    work_location VARCHAR(500) NOT NULL,
    note TEXT,
    response_deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    responded_at TIMESTAMPTZ,
    decline_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS employment_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    company_name VARCHAR(255),
    job_id UUID NOT NULL,
    application_id UUID UNIQUE REFERENCES recruitments(id) ON DELETE SET NULL,
    position VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. 5 đơn ứng tuyển demo cho UT-07 (Company 11111111-... của hr1 & ducanhn2810)
--   app-ut07-01: INTERVIEW + vòng cuối PASS  -> sẵn sàng để HR gửi Offer (201)
--   app-ut07-02: INTERVIEW + vòng cuối PENDING -> kiểm thử gửi Offer trả 409
--   app-ut07-03: OFFER (SENT, hạn +3 ngày)    -> ứng viên candidate1 bấm Chấp nhận / Từ chối
--   app-ut07-04: REJECTED (DECLINED + lý do)  -> HR xem rõ lý do ứng viên từ chối
--   app-ut07-05: OFFER (EXPIRED, quá hạn 2h)  -> kiểm thử ứng viên không bấm Nhận được (409)
INSERT INTO recruitments (id, job_id, company_id, user_id, cv_id, cv_snapshot_url, cover_letter, candidate_email, status, created_at, updated_at)
VALUES
    ('77070000-0000-4000-8000-000000000001', 'd0000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', 'ed854c07-f3eb-48f1-a186-4e7135f54645', '77071111-0000-4000-8000-000000000001', '/recruitments/77070000-0000-4000-8000-000000000001/cv-snapshot', 'Ứng tuyển vị trí Java Backend Developer', 'candidate1@rikkeijobs.test', 2, NOW() - interval '7 days', NOW() - interval '1 day'),
    ('77070000-0000-4000-8000-000000000002', 'd0000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111', '797c5663-386b-4e96-9112-6d3e414b4449', '77071111-0000-4000-8000-000000000002', '/recruitments/77070000-0000-4000-8000-000000000002/cv-snapshot', 'Ứng tuyển Frontend Developer', 'candidate2@rikkeijobs.test', 2, NOW() - interval '6 days', NOW() - interval '1 day'),
    ('77070000-0000-4000-8000-000000000003', 'd0000000-0000-4000-8000-000000000011', '11111111-1111-4111-8111-111111111111', 'ed854c07-f3eb-48f1-a186-4e7135f54645', '77071111-0000-4000-8000-000000000003', '/recruitments/77070000-0000-4000-8000-000000000003/cv-snapshot', 'Ứng tuyển Spring Boot Microservices', 'candidate1@rikkeijobs.test', 3, NOW() - interval '10 days', NOW() - interval '1 day'),
    ('77070000-0000-4000-8000-000000000004', 'd0000000-0000-4000-8000-000000000021', '11111111-1111-4111-8111-111111111111', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', '77071111-0000-4000-8000-000000000004', '/recruitments/77070000-0000-4000-8000-000000000004/cv-snapshot', 'Ứng tuyển Fullstack Engineer', 'candidate3@rikkeijobs.test', 5, NOW() - interval '12 days', NOW() - interval '2 days'),
    ('77070000-0000-4000-8000-000000000005', 'd0000000-0000-4000-8000-000000000031', '11111111-1111-4111-8111-111111111111', 'ed854c07-f3eb-48f1-a186-4e7135f54645', '77071111-0000-4000-8000-000000000005', '/recruitments/77070000-0000-4000-8000-000000000005/cv-snapshot', 'Ứng tuyển DevOps Fresher', 'candidate1@rikkeijobs.test', 5, NOW() - interval '14 days', NOW() - interval '2 hours')
ON CONFLICT (id) DO NOTHING;

-- 3. Các vòng phỏng vấn tương ứng
INSERT INTO interview_rounds (id, recruitment_id, round_no, scheduled_at, result, score)
VALUES
    ('77072000-0000-4000-8000-000000000001', '77070000-0000-4000-8000-000000000001', 1, NOW() - interval '3 days', 'PASS', 88),
    ('77072000-0000-4000-8000-000000000002', '77070000-0000-4000-8000-000000000002', 1, NOW() + interval '1 day', 'PENDING', NULL),
    ('77072000-0000-4000-8000-000000000003', '77070000-0000-4000-8000-000000000003', 1, NOW() - interval '5 days', 'PASS', 92),
    ('77072000-0000-4000-8000-000000000004', '77070000-0000-4000-8000-000000000004', 1, NOW() - interval '7 days', 'PASS', 86),
    ('77072000-0000-4000-8000-000000000005', '77070000-0000-4000-8000-000000000005', 1, NOW() - interval '8 days', 'PASS', 84)
ON CONFLICT (recruitment_id, round_no) DO NOTHING;

-- 4. 3 Offers mẫu (SENT, DECLINED, EXPIRED)
INSERT INTO offers (id, application_id, company_name, position, salary, start_date, work_location, note, response_deadline, status, responded_at, decline_reason, created_at, updated_at)
VALUES
    ('77073000-0000-4000-8000-000000000003', '77070000-0000-4000-8000-000000000003', 'Công ty Cổ phần Rikkeisoft', 'Java Spring Boot Microservices (Junior)', 20000000, '2026-12-15', 'Tầng 21, Tòa nhà Handico, Nam Từ Liêm, Hà Nội', 'Gói đãi ngộ gồm thưởng tháng 13, bảo hiểm sức khỏe và phụ cấp ăn trưa.', NOW() + interval '3 days', 'SENT', NULL, NULL, NOW() - interval '1 day', NOW() - interval '1 day'),
    ('77073000-0000-4000-8000-000000000004', '77070000-0000-4000-8000-000000000004', 'Công ty Cổ phần Rikkeisoft', 'Fullstack Software Engineer', 22000000, '2026-12-01', 'Hà Nội', 'Chào mừng gia nhập đội ngũ sản phẩm', NOW() + interval '2 days', 'DECLINED', NOW() - interval '2 days', 'Đã nhận offer gần nhà hơn và phù hợp định hướng hiện tại', NOW() - interval '5 days', NOW() - interval '2 days'),
    ('77073000-0000-4000-8000-000000000005', '77070000-0000-4000-8000-000000000005', 'Công ty Cổ phần Rikkeisoft', 'DevOps Fresher Engineer', 15000000, '2026-12-01', 'Đà Nẵng', 'Cơ hội làm việc trực tiếp với Kubernetes & AWS', NOW() - interval '2 hours', 'EXPIRED', NULL, NULL, NOW() - interval '6 days', NOW() - interval '2 hours')
ON CONFLICT (application_id) DO NOTHING;
