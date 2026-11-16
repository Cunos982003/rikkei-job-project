-- =============================================================================
-- Recruitment DB: Partner Portal & Recruitment Tables
-- =============================================================================
-- Run against recruitment_db

CREATE TABLE IF NOT EXISTS partner_accounts (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS shortlists (
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, user_id)
);

CREATE TABLE IF NOT EXISTS interview_requests (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL,
    account_id UUID,
    user_id UUID NOT NULL,
    position VARCHAR(255) NOT NULL,
    proposed_time TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONTACT_OPENED', 'REJECTED', 'INTERVIEWED', 'CLOSED')),
    result VARCHAR(20) CHECK (result IN ('HIRED', 'NOT_HIRED')),
    handled_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (company_id, user_id, position)
);

CREATE INDEX IF NOT EXISTS idx_interview_requests_company ON interview_requests(company_id);
CREATE INDEX IF NOT EXISTS idx_interview_requests_user ON interview_requests(user_id);

CREATE TABLE IF NOT EXISTS interview_request_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES interview_requests(id) ON DELETE CASCADE,
    status_from VARCHAR(30),
    status_to VARCHAR(30) NOT NULL,
    actor_id UUID,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_interview_request_history_request ON interview_request_history(request_id);

CREATE TABLE IF NOT EXISTS contact_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL UNIQUE REFERENCES interview_requests(id) ON DELETE CASCADE,
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    granted_by UUID,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_contact_grants_company ON contact_grants(company_id);
CREATE INDEX IF NOT EXISTS idx_contact_grants_user ON contact_grants(user_id);

CREATE TABLE IF NOT EXISTS profile_access_logs (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL,
    user_id UUID NOT NULL,
    access_type VARCHAR(20) NOT NULL CHECK (access_type IN ('VIEW', 'DOWNLOAD_PDF')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_profile_access_logs_account ON profile_access_logs(account_id, access_type, created_at);

-- ==============================================================================
-- SEED DATA
-- ==============================================================================

-- Partner accounts
INSERT INTO partner_accounts (id, company_id, email, password_hash, full_name, must_change_password, created_at) VALUES
('80000000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'hr1@rikkeijobs.test', '$2a$10$dummy_hash_1', 'HR Account One', FALSE, now() - interval '30 days'),
('80000000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'hr2@rikkeijobs.test', '$2a$10$dummy_hash_2', 'HR Account Two', FALSE, now() - interval '25 days'),
('80000000-0000-0000-0000-000000000003', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'hr3@rikkeijobs.test', '$2a$10$dummy_hash_3', 'HR Account Three', TRUE, now() - interval '10 days'),
('80000000-0000-0000-0000-000000000004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'hr4@rikkeijobs.test', '$2a$10$dummy_hash_4', 'HR Account Four', TRUE, now() - interval '5 days'),
('80000000-0000-0000-0000-000000000005', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'hr5@rikkeijobs.test', '$2a$10$dummy_hash_5', 'HR Account Five', TRUE, now())
ON CONFLICT (email) DO NOTHING;

-- Shortlists
INSERT INTO shortlists (company_id, user_id, note, created_at) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '550e8400-e29b-41d4-a716-446655440000', 'Java backend mạnh, phù hợp vị trí Fresher', now() - interval '5 days'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '660e8400-e29b-41d4-a716-446655440000', 'Frontend React tốt, xem xét cho vị trí Fullstack', now() - interval '4 days'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '770e8400-e29b-41d4-a716-446655440000', 'Fullstack xuất sắc, ưu tiên tuyển', now() - interval '3 days'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '660e8400-e29b-41d4-a716-446655440000', 'React/TypeScript chuyên nghiệp', now() - interval '6 days'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '770e8400-e29b-41d4-a716-446655440000', 'Kafka + Microservices, phù hợp dự án mới', now() - interval '2 days'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '660e8400-e29b-41d4-a716-446655440000', 'Xem xét thêm cho vị trí thực tập', now() - interval '1 day'),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '550e8400-e29b-41d4-a716-446655440000', 'Java/Spring Boot tốt', now() - interval '3 days')
ON CONFLICT (company_id, user_id) DO NOTHING;

-- Interview requests
INSERT INTO interview_requests (id, company_id, account_id, user_id, position, proposed_time, status, result, handled_by, created_at, updated_at) VALUES
('90000000-0000-0000-0000-000000000001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '80000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'Java Backend Developer (Fresher / Junior)', now() + interval '2 days', 'PENDING', NULL, NULL, now() - interval '7 days', now() - interval '7 days'),
('90000000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '80000000-0000-0000-0000-000000000001', '770e8400-e29b-41d4-a716-446655440000', 'Thực tập sinh Fullstack (Dự án IOC)', now() + interval '3 days', 'CONTACT_OPENED', NULL, '80000000-0000-0000-0000-000000000001', now() - interval '10 days', now() - interval '1 day'),
('90000000-0000-0000-0000-000000000003', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '80000000-0000-0000-0000-000000000002', '660e8400-e29b-41d4-a716-446655440000', 'Frontend React Developer (Fresher)', now() + interval '1 day', 'PENDING', NULL, NULL, now() - interval '5 days', now() - interval '5 days'),
('90000000-0000-0000-0000-000000000004', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '80000000-0000-0000-0000-000000000001', '660e8400-e29b-41d4-a716-446655440000', 'Java Backend Developer (Fresher / Junior)', now() + interval '5 days', 'REJECTED', 'NOT_HIRED', '80000000-0000-0000-0000-000000000001', now() - interval '12 days', now() - interval '2 days'),
('90000000-0000-0000-0000-000000000005', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '80000000-0000-0000-0000-000000000002', '770e8400-e29b-41d4-a716-446655440000', 'Thực tập sinh Fullstack (Dự án IOC)', now() + interval '4 days', 'INTERVIEWED', 'HIRED', '80000000-0000-0000-0000-000000000002', now() - interval '8 days', now() - interval '1 day'),
('90000000-0000-0000-0000-000000000006', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '80000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'Thực tập sinh Fullstack (Dự án IOC)', now() + interval '2 days', 'PENDING', NULL, NULL, now() - interval '4 days', now() - interval '4 days'),
('90000000-0000-0000-0000-000000000007', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '80000000-0000-0000-0000-000000000002', '660e8400-e29b-41d4-a716-446655440000', 'Thực tập sinh Fullstack (Dự án IOC)', now() + interval '3 days', 'CLOSED', 'NOT_HIRED', '80000000-0000-0000-0000-000000000002', now() - interval '6 days', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

-- Interview request history
INSERT INTO interview_request_history (id, request_id, status_from, status_to, actor_id, note, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000002', 'PENDING', 'CONTACT_OPENED', '80000000-0000-0000-0000-000000000001', 'Đã mở liên hệ', now() - interval '1 day'),
('a0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000004', 'PENDING', 'REJECTED', '80000000-0000-0000-0000-000000000001', 'Không phù hợp kỹ năng', now() - interval '2 days'),
('a0000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000005', 'PENDING', 'INTERVIEWED', '80000000-0000-0000-0000-000000000002', 'Đã phỏng vấn', now() - interval '3 days'),
('a0000000-0000-0000-0000-000000000004', '90000000-0000-0000-0000-000000000005', 'INTERVIEWED', 'CLOSED', '80000000-0000-0000-0000-000000000002', 'Đã tuyển dụng', now() - interval '1 day'),
('a0000000-0000-0000-0000-000000000005', '90000000-0000-0000-0000-000000000003', 'PENDING', 'CONTACT_OPENED', '80000000-0000-0000-0000-000000000002', 'Mở liên hệ', now() - interval '1 day'),
('a0000000-0000-0000-0000-000000000006', '90000000-0000-0000-0000-000000000006', 'PENDING', 'CONTACT_OPENED', '80000000-0000-0000-0000-000000000001', 'Mở liên hệ lần 2', now() - interval '12 hours'),
('a0000000-0000-0000-0000-000000000007', '90000000-0000-0000-0000-000000000007', 'PENDING', 'CLOSED', '80000000-0000-0000-0000-000000000002', 'Ứng viên từ chối', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

-- Contact grants
INSERT INTO contact_grants (id, request_id, company_id, user_id, granted_by, granted_at) VALUES
('b0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '770e8400-e29b-41d4-a716-446655440000', '80000000-0000-0000-0000-000000000001', now() - interval '1 day'),
('b0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000005', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '770e8400-e29b-41d4-a716-446655440000', '80000000-0000-0000-0000-000000000002', now() - interval '1 day'),
('b0000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000003', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '660e8400-e29b-41d4-a716-446655440000', '80000000-0000-0000-0000-000000000002', now() - interval '12 hours')
ON CONFLICT (id) DO NOTHING;

-- Profile access logs
INSERT INTO profile_access_logs (account_id, user_id, access_type, created_at) VALUES
('80000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'VIEW', now() - interval '5 days'),
('80000000-0000-0000-0000-000000000001', '770e8400-e29b-41d4-a716-446655440000', 'VIEW', now() - interval '4 days'),
('80000000-0000-0000-0000-000000000002', '660e8400-e29b-41d4-a716-446655440000', 'VIEW', now() - interval '3 days'),
('80000000-0000-0000-0000-000000000001', '770e8400-e29b-41d4-a716-446655440000', 'DOWNLOAD_PDF', now() - interval '2 days'),
('80000000-0000-0000-0000-000000000002', '770e8400-e29b-41d4-a716-446655440000', 'DOWNLOAD_PDF', now() - interval '1 day'),
('80000000-0000-0000-0000-000000000003', '660e8400-e29b-41d4-a716-446655440000', 'VIEW', now() - interval '12 hours'),
('80000000-0000-0000-0000-000000000004', '550e8400-e29b-41d4-a716-446655440000', 'VIEW', now() - interval '6 hours');