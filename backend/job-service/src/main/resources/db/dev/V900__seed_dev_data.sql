-- DỮ LIỆU MẪU CHỈ DÙNG CHO DEV/TEST, KHÔNG nạp ở môi trường thật.
-- Mật khẩu của mọi tài khoản bên dưới: Password@123

INSERT INTO companies (name, tax_code, industry, size, address, city, website, description, status, created_by)
VALUES ('Công ty Cổ phần Công nghệ Rikkei A', '0101234567', 'Công nghệ thông tin', '201-1000',
        'Tầng 21, Tháp Keangnam, Mễ Trì', 'Hà Nội', 'https://rikkeisoft.com',
        'Doanh nghiệp phát triển phần mềm và đào tạo công nghệ hàng đầu', 'ACTIVE', 'staff1@rikkei.vn');

INSERT INTO companies (name, tax_code, industry, size, address, city, website, description, status, created_by)
VALUES ('Tập đoàn Giải pháp Công nghệ B', '0107654321', 'Viễn thông & AI', '51-200',
        'Tòa nhà Landmark 81, Bình Thạnh', 'TP. Hồ Chí Minh', 'https://tech-b.example.com',
        'Doanh nghiệp nghiên cứu và ứng dụng trí tuệ nhân tạo', 'ACTIVE', 'staff1@rikkei.vn');

INSERT INTO users (email, full_name, password_hash, role, company_id, must_change_password)
VALUES ('staff1@rikkei.vn', 'Cán bộ Tuyển dụng 1',
        '$2a$10$T8h7mV7wtIQKg4xMvIHrHevXZoMab1Wi51surgFBcHvt/1ZWth9kS',
        'ROLE_STAFF', NULL, FALSE);

INSERT INTO users (email, full_name, password_hash, role, company_id, must_change_password)
VALUES ('hr1@rikkei.vn', 'HR Rikkei Công ty A',
        '$2a$10$T8h7mV7wtIQKg4xMvIHrHevXZoMab1Wi51surgFBcHvt/1ZWth9kS',
        'ROLE_HR', (SELECT id FROM companies WHERE tax_code = '0101234567'), FALSE);

INSERT INTO users (email, full_name, password_hash, role, company_id, must_change_password)
VALUES ('hr2@rikkei.vn', 'HR Doanh nghiệp B',
        '$2a$10$T8h7mV7wtIQKg4xMvIHrHevXZoMab1Wi51surgFBcHvt/1ZWth9kS',
        'ROLE_HR', (SELECT id FROM companies WHERE tax_code = '0107654321'), FALSE);