-- ==============================================================================
-- V1__init_recruitment.sql: Partner portal & tuyển dụng
-- ==============================================================================

-- 1. Tài khoản đối tác doanh nghiệp
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

-- 2. Danh sách quan tâm (shortlist)
CREATE TABLE IF NOT EXISTS shortlists (
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (company_id, user_id)
);

-- 3. Yêu cầu phỏng vấn
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

-- 4. Lịch sử trạng thái yêu cầu phỏng vấn
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

-- 5. Cấp quyền liên hệ (mỗi yêu cầu được duyệt tạo 1 dòng)
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

-- 6. Nhật ký truy cập hồ sơ (giới hạn xem/tải hàng loạt)
CREATE TABLE IF NOT EXISTS profile_access_logs (
    id BIGSERIAL PRIMARY KEY,
    account_id UUID NOT NULL,
    user_id UUID NOT NULL,
    access_type VARCHAR(20) NOT NULL CHECK (access_type IN ('VIEW', 'DOWNLOAD_PDF')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_profile_access_logs_account ON profile_access_logs(account_id, access_type, created_at);

-- ==============================================================================
-- SEED DATA: Từ mock data (companies.json, users.json, candidates.json)
-- ==============================================================================

-- Partner accounts (2 HR users từ users.json)
INSERT INTO partner_accounts (id, company_id, email, password_hash, full_name, must_change_password, created_at) VALUES
('80000000-0000-0000-0000-000000000001', 'comp-001', 'hr1@rikkeijobs.test', '$2a$10$dummy_hash_1', 'HR Account One', FALSE, now() - interval '30 days'),
('80000000-0000-0000-0000-000000000002', 'comp-002', 'hr2@rikkeijobs.test', '$2a$10$dummy_hash_2', 'HR Account Two', FALSE, now() - interval '25 days')
ON CONFLICT (email) DO NOTHING;

-- Shortlists: mỗi HR quan tâm 2-3 candidates
INSERT INTO shortlists (company_id, user_id, note, created_at) VALUES
('comp-001', '550e8400-e29b-41d4-a716-446655440000', 'Java backend mạnh, phù hợp vị trí Fresher', now() - interval '5 days'),
('comp-001', 'user-002', 'Frontend React tốt, xem xét cho vị trí Fullstack', now() - interval '4 days'),
('comp-001', 'user-003', 'Fullstack xuất sắc, ưu tiên tuyển', now() - interval '3 days'),
('comp-002', 'user-002', 'React/TypeScript chuyên nghiệp', now() - interval '6 days'),
('comp-002', 'user-003', 'Kafka + Microservices, phù hợp dự án mới', now() - interval '2 days')
ON CONFLICT (company_id, user_id) DO NOTHING;

-- Interview requests: 5 yêu cầu
INSERT INTO interview_requests (id, company_id, account_id, user_id, position, proposed_time, status, result, handled_by, created_at, updated_at) VALUES
('90000000-0000-0000-0000-000000000001', 'comp-001', '80000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'Java Backend Developer (Fresher / Junior)', now() + interval '2 days', 'PENDING', NULL, NULL, now() - interval '7 days', now() - interval '7 days'),
('90000000-0000-0000-0000-000000000002', 'comp-001', '80000000-0000-0000-0000-000000000001', 'user-003', 'Thực tập sinh Fullstack (Dự án IOC)', now() + interval '3 days', 'CONTACT_OPENED', NULL, '80000000-0000-0000-0000-000000000001', now() - interval '10 days', now() - interval '1 day'),
('90000000-0000-0000-0000-000000000003', 'comp-002', '80000000-0000-0000-0000-000000000002', 'user-002', 'Frontend React Developer (Fresher)', now() + interval '1 day', 'PENDING', NULL, NULL, now() - interval '5 days', now() - interval '5 days'),
('90000000-0000-0000-0000-000000000004', 'comp-001', '80000000-0000-0000-0000-000000000001', 'user-002', 'Java Backend Developer (Fresher / Junior)', now() + interval '5 days', 'REJECTED', 'NOT_HIRED', '80000000-0000-0000-0000-000000000001', now() - interval '12 days', now() - interval '2 days'),
('90000000-0000-0000-0000-000000000005', 'comp-002', '80000000-0000-0000-0000-000000000002', 'user-003', 'Thực tập sinh Fullstack (Dự án IOC)', now() + interval '4 days', 'INTERVIEWED', 'HIRED', '80000000-0000-0000-0000-000000000002', now() - interval '8 days', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

-- Interview request history
INSERT INTO interview_request_history (id, request_id, status_from, status_to, actor_id, note, created_at) VALUES
('a0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000002', 'PENDING', 'CONTACT_OPENED', '80000000-0000-0000-0000-000000000001', 'Đã mở liên hệ', now() - interval '1 day'),
('a0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000004', 'PENDING', 'REJECTED', '80000000-0000-0000-0000-000000000001', 'Không phù hợp kỹ năng', now() - interval '2 days'),
('a0000000-0000-0000-0000-000000000003', '90000000-0000-0000-0000-000000000005', 'PENDING', 'INTERVIEWED', '80000000-0000-0000-0000-000000000002', 'Đã phỏng vấn', now() - interval '3 days'),
('a0000000-0000-0000-0000-000000000004', '90000000-0000-0000-0000-000000000005', 'INTERVIEWED', 'CLOSED', '80000000-0000-0000-0000-000000000002', 'Đã tuyển dụng', now() - interval '1 day'),
('a0000000-0000-0000-0000-000000000005', '90000000-0000-0000-0000-000000000003', 'PENDING', 'CONTACT_OPENED', '80000000-0000-0000-0000-000000000002', 'Mở liên hệ', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

-- Contact grants (cho 2 yêu cầu được duyệt)
INSERT INTO contact_grants (id, request_id, company_id, user_id, granted_by, granted_at) VALUES
('b0000000-0000-0000-0000-000000000001', '90000000-0000-0000-0000-000000000002', 'comp-001', 'user-003', '80000000-0000-0000-0000-000000000001', now() - interval '1 day'),
('b0000000-0000-0000-0000-000000000002', '90000000-0000-0000-0000-000000000005', 'comp-002', 'user-003', '80000000-0000-0000-0000-000000000002', now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

-- Profile access logs (5 log entries)
INSERT INTO profile_access_logs (account_id, user_id, access_type, created_at) VALUES
('80000000-0000-0000-0000-000000000001', '550e8400-e29b-41d4-a716-446655440000', 'VIEW', now() - interval '5 days'),
('80000000-0000-0000-0000-000000000001', 'user-003', 'VIEW', now() - interval '4 days'),
('80000000-0000-0000-0000-000000000002', 'user-002', 'VIEW', now() - interval '3 days'),
('80000000-0000-0000-0000-000000000001', 'user-003', 'DOWNLOAD_PDF', now() - interval '2 days'),
('80000000-0000-0000-0000-000000000002', 'user-003', 'DOWNLOAD_PDF', now() - interval '1 day');

-- Note: View v_partner_profiles không tạo ở đây vì cần join với candidate_profiles
-- đang ở database profile_db (cross-database view không hỗ trợ trong PostgreSQL).
-- View sẽ được tạo ở API layer hoặc khi có replica/synced data.