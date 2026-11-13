-- Migration V2: Seed 2 công ty mẫu và 8 tài khoản mẫu chuẩn NT-02
-- Mật khẩu mặc định của 8 tài khoản: Test@123

INSERT INTO companies (
    id, name, tax_code, industry, size, address,
    city, website, description, status, created_by, created_at, updated_at
) VALUES (
    '11111111-1111-4111-8111-111111111111',
    'Công ty Cổ phần Rikkeisoft',
    '0101234567',
    'Công nghệ thông tin',
    '201-1000',
    'Tầng 21, Tháp Keangnam, Mễ Trì',
    'Hà Nội',
    'https://rikkeisoft.com',
    'Doanh nghiệp phát triển phần mềm và đào tạo công nghệ',
    'ACTIVE',
    'staff1@rikkeijobs.test',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO companies (
    id, name, tax_code, industry, size, address,
    city, website, description, status, created_by, created_at, updated_at
) VALUES (
    '22222222-2222-4222-8222-222222222222',
    'Rikkei Digital Solutions',
    '0107654321',
    'Công nghệ thông tin',
    '51-200',
    'Quận Hải Châu',
    'Đà Nẵng',
    'https://rikkeidigital.com',
    'Doanh nghiệp phát triển giải pháp số',
    'ACTIVE',
    'staff1@rikkeijobs.test',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO users (
    id, email, full_name, password_hash,
    role, company_id, must_change_password, created_at
) VALUES
('00000000-0000-0000-0000-000000000001', 'candidate1@rikkeijobs.test', 'Nguyễn Văn An', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_CANDIDATE', NULL, false, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000002', 'candidate2@rikkeijobs.test', 'Trần Thị Bình', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_CANDIDATE', NULL, false, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000003', 'candidate3@rikkeijobs.test', 'Lê Hoàng Cường', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_CANDIDATE', NULL, false, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000004', 'hr1@rikkeijobs.test', 'HR Account One', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_HR', '11111111-1111-4111-8111-111111111111', false, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000005', 'hr2@rikkeijobs.test', 'HR Account Two', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_HR', '22222222-2222-4222-8222-222222222222', false, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000006', 'mentor1@rikkeijobs.test', 'Phạm Minh Đức', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_MENTOR', NULL, false, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000007', 'staff1@rikkeijobs.test', 'Hoàng Thu Hà', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_STAFF', NULL, false, CURRENT_TIMESTAMP),
('00000000-0000-0000-0000-000000000008', 'admin1@rikkeijobs.test', 'Quản trị viên Hệ thống', '$2a$10$YSH0HwPStZhLGxs0Ao7mXuFvJ5QafUCQ.lxryoa5nSMaBUbaWXtUS', 'ROLE_ADMIN', NULL, false, CURRENT_TIMESTAMP);
