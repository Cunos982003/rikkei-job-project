-- =============================================================================
-- Tạo sẵn database cho từng service
-- =============================================================================
-- PostgreSQL chạy tự động file này ở lần khởi tạo cluster đầu tiên
-- (docker-entrypoint-initdb.d), tức chỉ khi thư mục dữ liệu còn trống.
-- Chạy lại trên cluster đã có dữ liệu sẽ báo lỗi "database already exists" —
-- khi đó tạo tay bằng chính các câu lệnh bên dưới.
--
-- Quy ước: mỗi service một database riêng, đặt tên <tên-service>_db. Vì vậy
-- không thể gom chung vào biến SPRING_DATASOURCE_URL của một file ..env dùng
-- chung — mỗi service phải được set biến này riêng khi chạy.
-- =============================================================================

-- Tài khoản, phiên đăng nhập
CREATE DATABASE auth_db;

-- Hồ sơ ứng viên, kỹ năng, học vấn, chứng chỉ (đang dùng)
CREATE DATABASE profile_db;

-- Doanh nghiệp, tin tuyển dụng
CREATE DATABASE job_db;

-- Hồ sơ ứng tuyển, CV
CREATE DATABASE recruitment_db;

-- Đề thi, kết quả khảo thí
CREATE DATABASE assessment_db;

-- Minh chứng năng lực
CREATE DATABASE evidence_db;

-- Lộ trình học bổ sung
CREATE DATABASE learning_db;

-- Thông báo
CREATE DATABASE notification_db;

-- Nhật ký kiểm toán
CREATE DATABASE audit_db;

-- Gợi ý nghề nghiệp bằng AI
CREATE DATABASE ai_db;

-- KHÔNG tạo `rikkei_jobs` ở đây. PostgreSQL đã tạo sẵn database này từ biến
-- POSTGRES_DB của docker-compose TRƯỚC khi chạy các script trong initdb.d.
-- Thêm dòng CREATE DATABASE rikkei_jobs sẽ báo lỗi "already exists", và vì
-- entrypoint chạy với ON_ERROR_STOP=1 nên container dừng ngay sau đó.
