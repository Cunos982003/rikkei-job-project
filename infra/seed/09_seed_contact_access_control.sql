-- =============================================================================
-- 09_seed_contact_access_control.sql : UT-03 Contact Access Control Seed Data
-- =============================================================================
-- Requires: profile_db (candidate_profiles) and job_db (companies) to be seeded first
-- Run after: 02_seed_profile_db.sql and 03_seed_job_db.sql
-- =============================================================================

-- Use existing UUIDs from seeded data
-- Companies: 11111111-1111-4111-8111-111111111111 (Rikkeisoft), etc.
-- Candidates: ed854c07-f3eb-48f1-a186-4e7135f54645 (candidate1), etc.
-- Staff users (from auth_db): staff1@rikkeijobs.test, staff2@rikkeijobs.test

-- Sample HR user IDs (these would be from auth-service users with HR role)
-- For testing, we'll use placeholder UUIDs that represent HR users
-- In real scenario, these come from auth-service

DO $$
DECLARE
    v_company_rikkeisoft UUID := '11111111-1111-4111-8111-111111111111';
    v_company_digital UUID := '22222222-2222-4222-8222-222222222222';
    v_company_fpt UUID := '33333333-3333-4333-8333-333333333333';
    v_company_viettel UUID := '44444444-4444-4444-8444-444444444444';
    v_company_vng UUID := '55555555-5555-4555-8555-555555555555';

    v_candidate1 UUID := 'ed854c07-f3eb-48f1-a186-4e7135f54645';  -- Nguyễn Văn An
    v_candidate2 UUID := '797c5663-386b-4e96-9112-6d3e414b4449';  -- Trần Thị Bình
    v_candidate3 UUID := '4a09b051-d7ab-40a5-8322-963ec9ebdbd7';  -- Lê Hoàng Cường
    v_candidate4 UUID := 'f848319f-9e6e-4815-a697-2fe2400fd32f';  -- Phạm Thu Dung
    v_candidate5 UUID := '6257a31f-e618-4ec1-ad25-67574b41a04e';  -- Vũ Đức Giang

    v_hr1 UUID := 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee1';  -- HR at Rikkeisoft
    v_hr2 UUID := 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee2';  -- HR at Rikkeisoft
    v_hr3 UUID := 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee3';  -- HR at Digital Solutions
    v_staff1 UUID := 'bbbbbbbb-cccc-4ddd-8eee-fffffffffff1'; -- STAFF user
    v_staff2 UUID := 'bbbbbbbb-cccc-4ddd-8eee-fffffffffff2'; -- STAFF user
    v_job_id UUID := 'cccccccc-dddd-4eee-8fff-aaaaaaaaaaa1'; -- Sample job ID
BEGIN
    -- =============================================================================
    -- CONTACT REQUESTS
    -- =============================================================================
    -- HR from Rikkeisoft requests contact for candidate1 (approved)
    INSERT INTO contact_requests (company_id, user_id, job_id, reason, status, reviewed_by, reviewed_at, created_at, updated_at)
    VALUES (v_company_rikkeisoft, v_candidate1, v_job_id, 'Cần liên hệ để mời phỏng vấn vị trí Java Backend', 'APPROVED', v_staff1, NOW() - interval '5 days', NOW() - interval '7 days', NOW() - interval '5 days')
    ON CONFLICT DO NOTHING;

    -- HR from Rikkeisoft requests contact for candidate2 (pending)
    INSERT INTO contact_requests (company_id, user_id, job_id, reason, status, created_at, updated_at)
    VALUES (v_company_rikkeisoft, v_candidate2, v_job_id, 'Hồ sơ phù hợp, muốn trao đổi thêm về dự án', 'PENDING', NOW() - interval '2 days', NOW() - interval '2 days')
    ON CONFLICT DO NOTHING;

    -- HR from Digital Solutions requests contact for candidate3 (rejected)
    INSERT INTO contact_requests (company_id, user_id, job_id, reason, status, reviewed_by, reviewed_at, created_at, updated_at)
    VALUES (v_company_digital, v_candidate3, v_job_id, 'Ứng viên có kỹ năng React phù hợp', 'REJECTED', v_staff2, NOW() - interval '3 days', NOW() - interval '5 days', NOW() - interval '3 days')
    ON CONFLICT DO NOTHING;

    -- HR from Digital Solutions requests contact for candidate4 (approved)
    INSERT INTO contact_requests (company_id, user_id, job_id, reason, status, reviewed_by, reviewed_at, created_at, updated_at)
    VALUES (v_company_digital, v_candidate4, v_job_id, 'Mời phỏng vấn vị trí AI Engineer', 'APPROVED', v_staff1, NOW() - interval '1 day', NOW() - interval '3 days', NOW() - interval '1 day')
    ON CONFLICT DO NOTHING;

    -- HR from FPT requests contact for candidate5 (pending)
    INSERT INTO contact_requests (company_id, user_id, job_id, reason, status, created_at, updated_at)
    VALUES (v_company_fpt, v_candidate5, v_job_id, 'Xem xét hồ sơ cho vị trí Mobile Developer', 'PENDING', NOW() - interval '1 day', NOW() - interval '1 day')
    ON CONFLICT DO NOTHING;

    -- Additional requests for more test data
    INSERT INTO contact_requests (company_id, user_id, job_id, reason, status, reviewed_by, reviewed_at, created_at, updated_at)
    VALUES (v_company_viettel, v_candidate1, v_job_id, 'Cần liên hệ gấp về vị trí DevOps', 'APPROVED', v_staff2, NOW() - interval '10 days', NOW() - interval '12 days', NOW() - interval '10 days')
    ON CONFLICT DO NOTHING;

    INSERT INTO contact_requests (company_id, user_id, job_id, reason, status, created_at, updated_at)
    VALUES (v_company_vng, v_candidate2, v_job_id, 'Hồ sơ ấn tượng, muốn biết thêm chi tiết', 'PENDING', NOW() - interval '4 hours', NOW() - interval '4 hours')
    ON CONFLICT DO NOTHING;

    -- =============================================================================
    -- CONTACT ACCESS LOGS
    -- =============================================================================
    -- Log 1: Rikkeisoft HR viewed candidate1 via APPROVED_REQUEST
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_rikkeisoft, v_hr1, v_candidate1, 'APPROVED_REQUEST', NOW() - interval '5 days')
    ON CONFLICT DO NOTHING;

    -- Log 2: Rikkeisoft HR viewed candidate1 again via APPROVED_REQUEST
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_rikkeisoft, v_hr1, v_candidate1, 'APPROVED_REQUEST', NOW() - interval '4 days')
    ON CONFLICT DO NOTHING;

    -- Log 3: Rikkeisoft HR viewed candidate1 again via APPROVED_REQUEST
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_rikkeisoft, v_hr1, v_candidate1, 'APPROVED_REQUEST', NOW() - interval '3 days')
    ON CONFLICT DO NOTHING;

    -- Log 4: Digital Solutions HR viewed candidate3 via APPROVED_REQUEST (after approval)
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_digital, v_hr3, v_candidate3, 'APPROVED_REQUEST', NOW() - interval '2 days')
    ON CONFLICT DO NOTHING;

    -- Log 5: Digital Solutions HR viewed candidate4 via APPROVED_REQUEST
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_digital, v_hr3, v_candidate4, 'APPROVED_REQUEST', NOW() - interval '1 day')
    ON CONFLICT DO NOTHING;

    -- Log 6: Viettel HR viewed candidate1 via APPROVED_REQUEST
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_viettel, v_hr2, v_candidate1, 'APPROVED_REQUEST', NOW() - interval '9 days')
    ON CONFLICT DO NOTHING;

    -- Log 7: FPT HR viewed candidate5 via APPLICATION (candidate applied to FPT job)
    -- This simulates the APPLICATION path where candidate applied to company's job
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_fpt, v_hr1, v_candidate5, 'APPLICATION', NOW() - interval '6 hours')
    ON CONFLICT DO NOTHING;

    -- Log 8: Rikkeisoft HR viewed candidate2 via APPLICATION
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_rikkeisoft, v_hr1, v_candidate2, 'APPLICATION', NOW() - interval '1 day')
    ON CONFLICT DO NOTHING;

    -- Log 9: VNG HR viewed candidate2 via APPLICATION
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_vng, v_hr2, v_candidate2, 'APPLICATION', NOW() - interval '3 hours')
    ON CONFLICT DO NOTHING;

    -- Log 10: Rikkeisoft HR viewed candidate3 via APPLICATION
    INSERT INTO contact_access_logs (company_id, hr_id, user_id, via, created_at)
    VALUES (v_company_rikkeisoft, v_hr1, v_candidate3, 'APPLICATION', NOW() - interval '12 hours')
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'Contact access control seed data inserted successfully!';
    RAISE NOTICE 'Companies: Rikkeisoft, Digital Solutions, FPT, Viettel, VNG';
    RAISE NOTICE 'Candidates: 5 candidates with various contact request statuses';
    RAISE NOTICE 'Logs: 10 access logs covering both APPLICATION and APPROVED_REQUEST paths';
END $$;