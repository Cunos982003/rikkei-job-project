-- ==============================================================================
-- V5__init_dt04_staff_upskilling.sql: Cán bộ theo dõi đào tạo bổ sung theo lớp, khoá (DT-04)
-- ==============================================================================

-- 1. Bảng lưu trữ thông tin hồ sơ học viên theo lớp, khoá trong learning_db
CREATE TABLE IF NOT EXISTS candidate_profiles (
    user_id UUID PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    phone VARCHAR(50),
    class_code VARCHAR(50),
    course_name VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Chỉ mục theo đúng đặc tả kỹ thuật DT-04
-- Chỉ mục trên enrollments(status) phục vụ lọc trạng thái học tập nhanh
CREATE INDEX IF NOT EXISTS idx_enrollments_status ON enrollments(status);

-- Chỉ mục trên candidate_profiles(class_code, course_name) phục vụ truy vấn gộp theo lớp và khoá
CREATE INDEX IF NOT EXISTS idx_candidate_profiles_class_course ON candidate_profiles(class_code, course_name);

-- 3. Nạp dữ liệu seed đồng bộ 40 học viên mẫu theo lớp & khoá đào tạo
INSERT INTO candidate_profiles (user_id, full_name, email, phone, class_code, course_name) VALUES
    ('ed854c07-f3eb-48f1-a186-4e7135f54645', 'Nguyễn Văn An', 'candidate1@rikkeijobs.test', '0912345600', 'RA-JAVA-2026', 'Khóa đào tạo Kỹ sư Phần mềm Java'),
    ('797c5663-386b-4e96-9112-6d3e414b4449', 'Trần Thị Bình', 'candidate2@rikkeijobs.test', '0912345737', 'RA-FE-2026', 'Khóa đào tạo Chuyên viên Frontend React'),
    ('4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'Lê Hoàng Cường', 'candidate3@rikkeijobs.test', '0912345874', 'RA-FS-2026', 'Khóa đào tạo Kỹ sư Phần mềm Fullstack'),
    ('f848319f-9e6e-4815-a697-2fe2400fd32f', 'Phạm Thu Dung', 'candidate4@rikkeijobs.test', '0912346011', 'RA-AI-2026', 'Khóa đào tạo Kỹ sư Trí tuệ Nhân tạo & Data'),
    ('6257a31f-e618-4ec1-ad25-67574b41a04e', 'Vũ Đức Giang', 'candidate5@rikkeijobs.test', '0912346148', 'RA-MOB-2026', 'Khóa đào tạo Lập trình Di động Flutter & React Native'),
    ('c633fa6f-51e7-4257-a13b-9bc051acab55', 'Đỗ Thị Hương', 'candidate6@rikkeijobs.test', '0912346285', 'RA-QA-2026', 'Khóa đào tạo Chuyên viên Kiểm thử Tự động'),
    ('41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'Hoàng Văn Hải', 'candidate7@rikkeijobs.test', '0912346422', 'RA-CLOUD-2026', 'Khóa đào tạo Kỹ sư DevOps & Cloud AWS'),
    ('7d26abe3-98c9-4af8-a904-e57f81783d54', 'Bùi Thị Khánh', 'candidate8@rikkeijobs.test', '0912346559', 'RA-GO-2026', 'Khóa đào tạo Golang Backend Developer'),
    ('6f7d791c-8ef0-4404-ab53-5871eb562621', 'Đặng Quốc Khánh', 'candidate9@rikkeijobs.test', '0912346696', 'RA-JAVA-2026', 'Khóa đào tạo Kỹ sư Phần mềm Java'),
    ('1c1cae0c-2691-4876-a05e-e1c133a1f013', 'Ngô Mai Linh', 'candidate10@rikkeijobs.test', '0912346833', 'RA-FE-2026', 'Khóa đào tạo Chuyên viên Frontend React'),
    ('84e1f7fb-48da-480c-ab2a-799f6d931982', 'Dương Văn Long', 'candidate11@rikkeijobs.test', '0912346970', 'RA-FS-2026', 'Khóa đào tạo Kỹ sư Phần mềm Fullstack'),
    ('47687ff7-7211-405f-a081-230fad46f127', 'Lý Thị Minh', 'candidate12@rikkeijobs.test', '0912347107', 'RA-AI-2026', 'Khóa đào tạo Kỹ sư Trí tuệ Nhân tạo & Data'),
    ('a3692669-b304-40c2-a999-50695e5fedb6', 'Trịnh Văn Nam', 'candidate13@rikkeijobs.test', '0912347244', 'RA-MOB-2026', 'Khóa đào tạo Lập trình Di động Flutter & React Native'),
    ('f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'Phan Thị Nga', 'candidate14@rikkeijobs.test', '0912347381', 'RA-QA-2026', 'Khóa đào tạo Chuyên viên Kiểm thử Tự động'),
    ('988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'Tạ Hoàng Phúc', 'candidate15@rikkeijobs.test', '0912347518', 'RA-CLOUD-2026', 'Khóa đào tạo Kỹ sư DevOps & Cloud AWS'),
    ('56de20bd-ad10-4cd8-a732-7d41b40b6734', 'Cao Thị Quỳnh', 'candidate16@rikkeijobs.test', '0912347655', 'RA-GO-2026', 'Khóa đào tạo Golang Backend Developer'),
    ('7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'Lương Văn Sơn', 'candidate17@rikkeijobs.test', '0912347792', 'RA-JAVA-2026', 'Khóa đào tạo Kỹ sư Phần mềm Java'),
    ('984682cd-5097-4e07-aea4-7f6e423a80d5', 'Trương Mỹ Tâm', 'candidate18@rikkeijobs.test', '0912347929', 'RA-FE-2026', 'Khóa đào tạo Chuyên viên Frontend React'),
    ('0539425d-c9ea-42ad-a526-af16615ee51e', 'Đoàn Quốc Thắng', 'candidate19@rikkeijobs.test', '0912348066', 'RA-FS-2026', 'Khóa đào tạo Kỹ sư Phần mềm Fullstack'),
    ('4b20ea51-eba8-4087-a312-b142beefc5b7', 'Hà Thị Trang', 'candidate20@rikkeijobs.test', '0912348203', 'RA-AI-2026', 'Khóa đào tạo Kỹ sư Trí tuệ Nhân tạo & Data'),
    ('0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'Đinh Văn Tuấn', 'candidate21@rikkeijobs.test', '0912348340', 'RA-MOB-2026', 'Khóa đào tạo Lập trình Di động Flutter & React Native'),
    ('550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'Mai Thị Uyên', 'candidate22@rikkeijobs.test', '0912348477', 'RA-QA-2026', 'Khóa đào tạo Chuyên viên Kiểm thử Tự động'),
    ('315a14b5-f83c-42bc-a038-39cf8340bf3a', 'Lưu Văn Việt', 'candidate23@rikkeijobs.test', '0912348614', 'RA-CLOUD-2026', 'Khóa đào tạo Kỹ sư DevOps & Cloud AWS'),
    ('be569c29-d7e0-482e-a485-654f2511098e', 'Đào Thị Xuân', 'candidate24@rikkeijobs.test', '0912348751', 'RA-GO-2026', 'Khóa đào tạo Golang Backend Developer'),
    ('85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'Chu Văn Yên', 'candidate25@rikkeijobs.test', '0912348888', 'RA-JAVA-2026', 'Khóa đào tạo Kỹ sư Phần mềm Java'),
    ('33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'Quách Bảo Anh', 'candidate26@rikkeijobs.test', '0912349025', 'RA-FE-2026', 'Khóa đào tạo Chuyên viên Frontend React'),
    ('03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'Thân Đức Bình', 'candidate27@rikkeijobs.test', '0912349162', 'RA-FS-2026', 'Khóa đào tạo Kỹ sư Phần mềm Fullstack'),
    ('e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'Diệp Thanh Châu', 'candidate28@rikkeijobs.test', '0912349299', 'RA-AI-2026', 'Khóa đào tạo Kỹ sư Trí tuệ Nhân tạo & Data'),
    ('26dc3569-5b76-4c65-aaf9-eafcc3fd2412', 'Nghiêm Văn Dũng', 'candidate29@rikkeijobs.test', '0912349436', 'RA-MOB-2026', 'Khóa đào tạo Lập trình Di động Flutter & React Native'),
    ('5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', 'Khổng Thị Hà', 'candidate30@rikkeijobs.test', '0912349573', 'RA-QA-2026', 'Khóa đào tạo Chuyên viên Kiểm thử Tự động'),
    ('98da37df-6ea3-439a-a76b-356a87e3c39b', 'La Minh Khang', 'candidate31@rikkeijobs.test', '0912349710', 'RA-CLOUD-2026', 'Khóa đào tạo Kỹ sư DevOps & Cloud AWS'),
    ('55c98170-fe5f-49bf-af88-812dc97231fa', 'Ôn Thị Liên', 'candidate32@rikkeijobs.test', '0912349847', 'RA-GO-2026', 'Khóa đào tạo Golang Backend Developer'),
    ('a68f2877-ef24-461c-abbf-090bb2a7c579', 'Phí Văn Nghĩa', 'candidate33@rikkeijobs.test', '0912349984', 'RA-JAVA-2026', 'Khóa đào tạo Kỹ sư Phần mềm Java'),
    ('f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'Thiều Thị Oanh', 'candidate34@rikkeijobs.test', '0912350121', 'RA-FE-2026', 'Khóa đào tạo Chuyên viên Frontend React'),
    ('79518343-7d11-463f-a6ef-b199d83106da', 'Doãn Quốc Phong', 'candidate35@rikkeijobs.test', '0912350258', 'RA-FS-2026', 'Khóa đào tạo Kỹ sư Phần mềm Fullstack'),
    ('f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', 'Triệu Thị Quyên', 'candidate36@rikkeijobs.test', '0912350395', 'RA-AI-2026', 'Khóa đào tạo Kỹ sư Trí tuệ Nhân tạo & Data'),
    ('e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'Hứa Văn Thành', 'candidate37@rikkeijobs.test', '0912350532', 'RA-MOB-2026', 'Khóa đào tạo Lập trình Di động Flutter & React Native'),
    ('754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', 'Uông Thị Thảo', 'candidate38@rikkeijobs.test', '0912350669', 'RA-QA-2026', 'Khóa đào tạo Chuyên viên Kiểm thử Tự động'),
    ('172c2f01-2008-4e41-a7cb-12a83c6fd75f', 'Nông Văn Tiến', 'candidate39@rikkeijobs.test', '0912350806', 'RA-CLOUD-2026', 'Khóa đào tạo Kỹ sư DevOps & Cloud AWS'),
    ('1a56c767-f6f9-4bd7-aced-b8edca9dffbb', 'Vi Thị Yến', 'candidate40@rikkeijobs.test', '0912350943', 'RA-GO-2026', 'Khóa đào tạo Golang Backend Developer')
ON CONFLICT (user_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    class_code = EXCLUDED.class_code,
    course_name = EXCLUDED.course_name;
