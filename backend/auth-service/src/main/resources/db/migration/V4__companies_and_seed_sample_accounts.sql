-- Migration V4: Seed 8 tài khoản mẫu chuẩn NT-02 cho auth_db
-- Mật khẩu mặc định của 8 tài khoản: Test@123 (BCrypt hash)

INSERT INTO users (
    id, email, full_name, role, password_hash,
    lms_user_id, company_id, must_change_password, failed_login_count, status, created_at, updated_at
) VALUES
('00000000-0000-0000-0000-000000000001', 'candidate1@rikkeijobs.test', 'Nguyễn Văn An', 'CANDIDATE', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'lms-sv-001', NULL, false, 0, 'ACTIVE', now(), now()),
('00000000-0000-0000-0000-000000000002', 'candidate2@rikkeijobs.test', 'Trần Thị Bình', 'CANDIDATE', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'lms-sv-002', NULL, false, 0, 'ACTIVE', now(), now()),
('00000000-0000-0000-0000-000000000003', 'candidate3@rikkeijobs.test', 'Lê Hoàng Cường', 'CANDIDATE', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'lms-sv-003', NULL, false, 0, 'ACTIVE', now(), now()),
('00000000-0000-0000-0000-000000000004', 'hr1@rikkeijobs.test', 'HR Account One', 'HR', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', NULL, '11111111-1111-4111-8111-111111111111', false, 0, 'ACTIVE', now(), now()),
('00000000-0000-0000-0000-000000000005', 'hr2@rikkeijobs.test', 'HR Account Two', 'HR', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', NULL, '22222222-2222-4222-8222-222222222222', false, 0, 'ACTIVE', now(), now()),
('00000000-0000-0000-0000-000000000006', 'mentor1@rikkeijobs.test', 'Phạm Minh Đức', 'MENTOR', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'lms-mt-001', NULL, false, 0, 'ACTIVE', now(), now()),
('00000000-0000-0000-0000-000000000007', 'staff1@rikkeijobs.test', 'Hoàng Thu Hà', 'STAFF', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'lms-st-001', NULL, false, 0, 'ACTIVE', now(), now()),
('00000000-0000-0000-0000-000000000008', 'admin1@rikkeijobs.test', 'Quản trị viên Hệ thống', 'ADMIN', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'lms-ad-001', NULL, false, 0, 'ACTIVE', now(), now())
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    password_hash = EXCLUDED.password_hash,
    company_id = EXCLUDED.company_id,
    lms_user_id = EXCLUDED.lms_user_id,
    status = EXCLUDED.status,
    updated_at = now();

