
-- DỮ LIỆU MẪU CHỈ DÙNG CHO DEV/TEST.
-- Mật khẩu của mọi tài khoản: Password@123

INSERT INTO companies (
    id, name, tax_code, industry, size, address,
    city, website, description, status, created_by
)
VALUES (
    '11111111-1111-4111-8111-111111111111',
    'Công ty Cổ phần Công nghệ Rikkei A',
    '0101234567',
    'Công nghệ thông tin',
    '201-1000',
    'Tầng 21, Tháp Keangnam, Mễ Trì',
    'Hà Nội',
    'https://rikkeisoft.com',
    'Doanh nghiệp phát triển phần mềm và đào tạo công nghệ hàng đầu',
    'ACTIVE',
    'staff1@rikkei.vn'
);

INSERT INTO companies (
    id, name, tax_code, industry, size, address,
    city, website, description, status, created_by
)
VALUES (
    '22222222-2222-4222-8222-222222222222',
    'Tập đoàn Giải pháp Công nghệ B',
    '0107654321',
    'Viễn thông & AI',
    '51-200',
    'Tòa nhà Landmark 81, Bình Thạnh',
    'TP. Hồ Chí Minh',
    'https://tech-b.example.com',
    'Doanh nghiệp nghiên cứu và ứng dụng trí tuệ nhân tạo',
    'ACTIVE',
    'staff1@rikkei.vn'
);

INSERT INTO users (
    id, email, full_name, password_hash,
    role, company_id, must_change_password
)
VALUES (
    '33333333-3333-4333-8333-333333333333',
    'staff1@rikkei.vn',
    'Cán bộ Tuyển dụng 1',
    '$2a$10$T8h7mV7wtIQKg4xMvIHrHevXZoMab1Wi51surgFBcHvt/1ZWth9kS',
    'ROLE_STAFF',
    NULL,
    FALSE
);

INSERT INTO users (
    id, email, full_name, password_hash,
    role, company_id, must_change_password
)
VALUES (
    '44444444-4444-4444-8444-444444444444',
    'hr1@rikkei.vn',
    'HR Rikkei Công ty A',
    '$2a$10$T8h7mV7wtIQKg4xMvIHrHevXZoMab1Wi51surgFBcHvt/1ZWth9kS',
    'ROLE_HR',
    '11111111-1111-4111-8111-111111111111',
    FALSE
);

INSERT INTO users (
    id, email, full_name, password_hash,
    role, company_id, must_change_password
)
VALUES (
    '55555555-5555-4555-8555-555555555555',
    'hr2@rikkei.vn',
    'HR Doanh nghiệp B',
    '$2a$10$T8h7mV7wtIQKg4xMvIHrHevXZoMab1Wi51surgFBcHvt/1ZWth9kS',
    'ROLE_HR',
    '22222222-2222-4222-8222-222222222222',
    FALSE
);
