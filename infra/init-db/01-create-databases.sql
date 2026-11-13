-- Script khởi tạo các cơ sở dữ liệu cho các microservices của dự án Rikkei Jobs
-- Tự động chạy khi Docker container PostgreSQL khởi động lần đầu

CREATE DATABASE auth_db;
CREATE DATABASE job_db;
CREATE DATABASE profile_db;
CREATE DATABASE recruitment_db;
CREATE DATABASE evidence_db;
CREATE DATABASE assessment_db;
CREATE DATABASE ai_db;

-- Cấp toàn quyền cho user postgres trên các database
GRANT ALL PRIVILEGES ON DATABASE auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE job_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE profile_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE recruitment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE evidence_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE assessment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE ai_db TO postgres;

