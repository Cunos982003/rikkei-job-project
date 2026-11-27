-- =============================================================================
-- 05_seed_learning_db.sql : LEARNING SERVICE SEED DATA (learning_db)
-- Khớp 100% V1..V3 migrations + 18 khóa học (5 BE, 5 FE, 3 DevOps, 3 DB, 2 Bổ trợ)
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- ==============================================================================
-- V1__init_learning_gap.sql: Khởi tạo bảng cho module Đào tạo bổ sung (DT-01)
-- ==============================================================================

-- 1. Bảng danh mục vị trí mục tiêu (Target Positions)
CREATE TABLE IF NOT EXISTS target_positions (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng kỹ năng yêu cầu theo vị trí (Position Skills)
CREATE TABLE IF NOT EXISTS position_skills (
    position_id UUID NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    skill_name VARCHAR(255) NOT NULL,
    min_score INT NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_position_skills PRIMARY KEY (position_id, skill_id),
    CONSTRAINT fk_position_skills_position FOREIGN KEY (position_id) REFERENCES target_positions(id) ON DELETE CASCADE,
    CONSTRAINT chk_min_score CHECK (min_score >= 0 AND min_score <= 100)
);

CREATE INDEX IF NOT EXISTS idx_position_skills_pos ON position_skills(position_id);

-- 3. Bảng lưu mục tiêu nghề nghiệp của học viên (Candidate Targets)
CREATE TABLE IF NOT EXISTS candidate_targets (
    user_id UUID PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    position_id UUID,
    job_id VARCHAR(64),
    chosen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidate_targets_position FOREIGN KEY (position_id) REFERENCES target_positions(id) ON DELETE SET NULL,
    CONSTRAINT chk_target_type CHECK (target_type IN ('POSITION', 'JOB'))
);

-- 4. Bảng lưu kết quả phân tích khoảng thiếu hụt năng lực (Gap Results)
CREATE TABLE IF NOT EXISTS gap_results (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    skill_name VARCHAR(255) NOT NULL,
    required INT NOT NULL,
    current INT NOT NULL,
    gap INT NOT NULL,
    priority VARCHAR(32) NOT NULL,
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_gap_target_type CHECK (target_type IN ('POSITION', 'JOB')),
    CONSTRAINT chk_gap_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))
);

CREATE INDEX IF NOT EXISTS idx_gap_results_user_target ON gap_results(user_id, target_id);

-- ==============================================================================
-- SEED DATA: 4 Vị trí mục tiêu mẫu chuẩn hóa
-- ==============================================================================

-- Vị trí 1: Java Backend Fresher
INSERT INTO target_positions (id, code, name, description, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111101',
    'JAVA_BACKEND_FRESHER',
    'Java Backend Fresher',
    'Lộ trình đào tạo lập trình viên Java Backend Fresher chuyên sâu với Spring Boot, CSDL quan hệ và RESTful API.',
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

INSERT INTO position_skills (position_id, skill_id, skill_name, min_score, is_mandatory) VALUES
('11111111-1111-1111-1111-111111111101', 'JAVA_CORE', 'Java Core', 75, TRUE),
('11111111-1111-1111-1111-111111111101', 'SPRING_BOOT', 'Spring Boot', 70, TRUE),
('11111111-1111-1111-1111-111111111101', 'SQL', 'SQL', 70, TRUE),
('11111111-1111-1111-1111-111111111101', 'REST_API', 'REST API', 70, TRUE),
('11111111-1111-1111-1111-111111111101', 'GIT', 'Git', 60, FALSE)
ON CONFLICT (position_id, skill_id) DO NOTHING;

-- Vị trí 2: Frontend ReactJS Fresher
INSERT INTO target_positions (id, code, name, description, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111102',
    'FRONTEND_REACT_FRESHER',
    'Frontend ReactJS Fresher',
    'Lộ trình phát triển giao diện hiện đại với React 19, TypeScript và tối ưu UX/UI.',
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

INSERT INTO position_skills (position_id, skill_id, skill_name, min_score, is_mandatory) VALUES
('11111111-1111-1111-1111-111111111102', 'REACT', 'ReactJS', 70, TRUE),
('11111111-1111-1111-1111-111111111102', 'JAVASCRIPT', 'JavaScript', 75, TRUE),
('11111111-1111-1111-1111-111111111102', 'HTML_CSS', 'HTML/CSS', 75, TRUE),
('11111111-1111-1111-1111-111111111102', 'TYPESCRIPT', 'TypeScript', 65, FALSE),
('11111111-1111-1111-1111-111111111102', 'TAILWIND_CSS', 'Tailwind CSS', 60, FALSE)
ON CONFLICT (position_id, skill_id) DO NOTHING;

-- Vị trí 3: Tester
INSERT INTO target_positions (id, code, name, description, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111103',
    'TESTER',
    'Tester',
    'Lộ trình chuyên viên kiểm thử chất lượng phần mềm, viết test case và kiểm thử tự động cơ bản.',
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

INSERT INTO position_skills (position_id, skill_id, skill_name, min_score, is_mandatory) VALUES
('11111111-1111-1111-1111-111111111103', 'TESTING', 'Testing', 75, TRUE),
('11111111-1111-1111-1111-111111111103', 'SQL', 'SQL', 60, TRUE),
('11111111-1111-1111-1111-111111111103', 'GIT', 'Git', 50, FALSE),
('11111111-1111-1111-1111-111111111103', 'TEAMWORK', 'Làm việc nhóm', 70, FALSE)
ON CONFLICT (position_id, skill_id) DO NOTHING;

-- Vị trí 4: Fullstack
INSERT INTO target_positions (id, code, name, description, created_at)
VALUES (
    '11111111-1111-1111-1111-111111111104',
    'FULLSTACK',
    'Fullstack',
    'Kỹ sư phần mềm Fullstack có khả năng làm chủ cả hệ thống Backend Spring Boot và Frontend ReactJS.',
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

INSERT INTO position_skills (position_id, skill_id, skill_name, min_score, is_mandatory) VALUES
('11111111-1111-1111-1111-111111111104', 'JAVA_CORE', 'Java Core', 70, TRUE),
('11111111-1111-1111-1111-111111111104', 'SPRING_BOOT', 'Spring Boot', 65, TRUE),
('11111111-1111-1111-1111-111111111104', 'REACT', 'ReactJS', 65, TRUE),
('11111111-1111-1111-1111-111111111104', 'SQL', 'SQL', 65, TRUE),
('11111111-1111-1111-1111-111111111104', 'GIT', 'Git', 60, FALSE)
ON CONFLICT (position_id, skill_id) DO NOTHING;

-- ==============================================================================
-- V2__init_courses_and_skill_map.sql: Khóa học bổ sung & ánh xạ kỹ năng
-- ==============================================================================

-- 1. Bảng khóa học bổ sung
CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    level VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng ánh xạ khóa học - kỹ năng
CREATE TABLE IF NOT EXISTS course_skill_map (
    course_id UUID NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    weight NUMERIC(3,2) NOT NULL CHECK (weight > 0 AND weight <= 1),
    PRIMARY KEY (course_id, skill_id),
    CONSTRAINT fk_course_skill_map_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_skill_map_course ON course_skill_map(course_id);

-- ==============================================================================
-- SEED DATA: 6 khóa học mẫu (4 từ lms.json + 2 synthetic)
-- ==============================================================================

INSERT INTO courses (id, code, name, description, level, created_at) VALUES
('22222222-2222-2222-2222-222222222201', 'CRS-01', 'Lập trình Spring Boot nâng cao & Microservices',
 'Spring Boot, Microservices, Docker, Kafka cho backend enterprise', 'Tốt', CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222202', 'CRS-02', 'React 19 & Next.js Fullstack Architecture',
 'React 19, TypeScript, Tailwind CSS, Next.js cho frontend hiện đại', 'Tốt', CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222203', 'CRS-03', 'Tối ưu hóa CSDL quan hệ PostgreSQL',
 'SQL nâng cao, PostgreSQL internals, query optimization, index design', 'Khá', CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222204', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux',
 'Containerization, pipeline CI/CD GitHub Actions, Linux administration', 'Khá', CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222205', 'CRS-05', 'Kiểm thử tự động & Chất lượng phần mềm',
 'JUnit 5, Mockito, test coverage, CI integration, mutation testing', 'Tốt', CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222206', 'CRS-06', 'Phân tích dữ liệu với Python',
 'Pandas, NumPy, visualisasi data, statistical analysis, Jupyter', 'Khá', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Ánh xạ khóa học - kỹ năng (skill_id dùng code từ bảng skills)
INSERT INTO course_skill_map (course_id, skill_id, weight) VALUES
-- CRS-01: Spring Boot, Microservices, Docker, Kafka
('22222222-2222-2222-2222-222222222201', 'SPRING_BOOT', 0.35),
('22222222-2222-2222-2222-222222222201', 'MICROSERVICES', 0.30),
('22222222-2222-2222-2222-222222222201', 'DOCKER', 0.20),
('22222222-2222-2222-2222-222222222201', 'APACHE_KAFKA', 0.15),
-- CRS-02: React, TypeScript, Tailwind
('22222222-2222-2222-2222-222222222202', 'REACT', 0.40),
('22222222-2222-2222-2222-222222222202', 'TYPESCRIPT', 0.35),
('22222222-2222-2222-2222-222222222202', 'TAILWIND_CSS', 0.25),
-- CRS-03: SQL, PostgreSQL, Database Optimization
('22222222-2222-2222-2222-222222222203', 'SQL', 0.50),
('22222222-2222-2222-2222-222222222203', 'POSTGRESQL', 0.30),
('22222222-2222-2222-2222-222222222203', 'DATABASE_OPTIMIZATION', 0.20),
-- CRS-04: Docker, CI/CD, Linux
('22222222-2222-2222-2222-222222222204', 'DOCKER', 0.40),
('22222222-2222-2222-2222-222222222204', 'CI_CD', 0.35),
('22222222-2222-2222-2222-222222222204', 'LINUX_BASH', 0.25),
-- CRS-05: Testing, Java Core, CI/CD
('22222222-2222-2222-2222-222222222205', 'TESTING', 0.50),
('22222222-2222-2222-2222-222222222205', 'JAVA_CORE', 0.30),
('22222222-2222-2222-2222-222222222205', 'CI_CD', 0.20),
-- CRS-06: Python, SQL
('22222222-2222-2222-2222-222222222206', 'PYTHON', 0.60),
('22222222-2222-2222-2222-222222222206', 'SQL', 0.40)
ON CONFLICT (course_id, skill_id) DO NOTHING;

-- ==============================================================================
-- V3__init_dt02_enrollments_and_mappings.sql: Gợi ý khóa học LMS & Ghi danh (DT-02)
-- ==============================================================================

-- 1. Bảng ánh xạ kỹ năng - khóa học LMS (Skill Course Mappings)
CREATE TABLE IF NOT EXISTS skill_course_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_id VARCHAR(64) NOT NULL,
    lms_course_id VARCHAR(64) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    lesson_hint TEXT,
    level_from INT NOT NULL DEFAULT 0,
    level_to INT NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_skill_course UNIQUE (skill_id, lms_course_id),
    CONSTRAINT chk_level_range CHECK (level_from >= 0 AND level_to <= 100 AND level_from <= level_to)
);

CREATE INDEX IF NOT EXISTS idx_skill_course_mappings_skill ON skill_course_mappings(skill_id);

-- 2. Bảng ghi danh khóa học của học viên (Enrollments)
CREATE TABLE IF NOT EXISTS enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    lms_course_id VARCHAR(64) NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENROLLED',
    progress NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    score NUMERIC(5,2),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_enrollment_user_course UNIQUE (user_id, lms_course_id),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_enrollment_progress CHECK (progress >= 0.00 AND progress <= 100.00)
);

CREATE INDEX IF NOT EXISTS idx_enrollments_user_id ON enrollments(user_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_user_status ON enrollments(user_id, status);

-- ==============================================================================
-- SEED DATA: Bảng ánh xạ khóa học gợi ý cho các kỹ năng cốt lõi
-- ==============================================================================

INSERT INTO skill_course_mappings (skill_id, lms_course_id, course_name, lesson_hint, level_from, level_to)
VALUES
    -- JAVA_CORE
    ('JAVA_CORE', 'CRS-01', 'Lập trình Spring Boot nâng cao & Microservices', 'Bài 1-3: Ôn tập OOP, Concurrency & Stream API nâng cao', 0, 75),
    ('JAVA_CORE', 'CRS-05', 'Kiểm thử tự động & Chất lượng phần mềm', 'Bài 2-4: Unit Test Java với JUnit 5 và Mockito', 0, 80),

    -- SPRING_BOOT
    ('SPRING_BOOT', 'CRS-01', 'Lập trình Spring Boot nâng cao & Microservices', 'Bài 4-8: Spring Data JPA, Security và Microservices Architecture', 0, 85),

    -- SQL & CSDL
    ('SQL', 'CRS-03', 'Tối ưu hóa CSDL quan hệ PostgreSQL', 'Bài 1-5: Thiết kế Index, tối ưu câu truy vấn phức tạp và Transaction Isolation', 0, 80),
    ('POSTGRESQL', 'CRS-03', 'Tối ưu hóa CSDL quan hệ PostgreSQL', 'Bài 3-6: PostgreSQL Internals, EXPLAIN ANALYZE và Partitioning', 0, 85),

    -- REACT & FRONTEND
    ('REACT', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Bài 1-6: React 19 Hooks, Server Components và State Management', 0, 80),
    ('JAVASCRIPT', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Bài 1-3: JavaScript ES6+, Async/Await và TypeScript cơ bản', 0, 70),
    ('TYPESCRIPT', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Bài 3-5: TypeScript nâng cao, Generics và Type Narrowing', 0, 75),
    ('HTML_CSS', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Bài 2-4: Responsive Layouts với Tailwind CSS & Modern CSS', 0, 70),
    ('TAILWIND_CSS', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Bài 4: Tailwind CSS chuyên sâu và tối ưu bundle kích thước nhỏ', 0, 75),

    -- TESTING
    ('TESTING', 'CRS-05', 'Kiểm thử tự động & Chất lượng phần mềm', 'Bài 1-7: TDD, BDD, Mockito và Test Coverage tối ưu', 0, 85),

    -- DEVOPS, DOCKER & CI/CD, GIT
    ('DOCKER', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Bài 1-3: Dockerfile đa tầng, Docker Compose cho microservices', 0, 80),
    ('CI_CD', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Bài 4-6: Xây dựng pipeline CI/CD GitHub Actions tự động hóa', 0, 80),
    ('LINUX_BASH', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Bài 1-2: Quản trị Linux dòng lệnh và shell scripting thực chiến', 0, 75),
    ('GIT', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Bài 1: Git Branching Strategy, Gitflow và giải quyết xung đột mã nguồn', 0, 70),

    -- PYTHON
    ('PYTHON', 'CRS-06', 'Phân tích dữ liệu với Python', 'Bài 1-5: Lập trình Python xử lý dữ liệu với Pandas và NumPy', 0, 80)
ON CONFLICT (skill_id, lms_course_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS course_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL
);

CREATE TABLE IF NOT EXISTS course_modules (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    order_index INT NOT NULL,
    UNIQUE (course_id, order_index)
);

CREATE TABLE IF NOT EXISTS course_lessons (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES course_modules(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 30,
    order_index INT NOT NULL,
    UNIQUE (module_id, order_index)
);

CREATE TABLE IF NOT EXISTS learning_progress (
    id UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL REFERENCES enrollments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL REFERENCES course_lessons(id) ON DELETE CASCADE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    watched_seconds INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (enrollment_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS course_reviews (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (course_id, user_id)
);

-- 1. 6 Course Categories
INSERT INTO course_categories (id, code, name) VALUES
    ('119dfe0c-db87-4fda-b567-684279fbbd56', 'BACKEND', 'Lập trình Backend'),
    ('636016c3-a4c7-4659-bd4c-3f3a64eb4af7', 'FRONTEND', 'Lập trình Frontend'),
    ('2a83b297-2283-43fe-baad-c675e69fe853', 'DEVOPS', 'DevOps & Cloud'),
    ('6e41d683-bbe1-46df-9671-4830f407bab0', 'DATABASE', 'Cơ sở dữ liệu'),
    ('715f5efc-cf90-4f7f-8205-4ca058ce9963', 'SOFT_SKILLS', 'Kỹ năng mềm & Giao tiếp'),
    ('9672f175-8e81-4d02-982d-112faec5fd6a', 'QA_DATA', 'Kiểm thử & Dữ liệu')
ON CONFLICT (code) DO NOTHING;

-- 2. 18 Courses
INSERT INTO courses (id, code, name, description, level, created_at) VALUES
    ('22222222-2222-2222-2222-222222222201', 'CRS-01', 'Lập trình Spring Boot nâng cao & Microservices', 'Khóa học chuyên sâu thuộc nhóm BACKEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222207', 'CRS-07', 'Java Core & Lập trình Hướng đối tượng thực chiến', 'Khóa học chuyên sâu thuộc nhóm BACKEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Khá', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222208', 'CRS-08', 'Xây dựng RESTful API chuẩn Enterprise với Node.js', 'Khóa học chuyên sâu thuộc nhóm BACKEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Khá', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222209', 'CRS-09', 'Lập trình Golang & gRPC Hiệu năng cao', 'Khóa học chuyên sâu thuộc nhóm BACKEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222210', 'CRS-10', 'Kiến trúc Hệ thống Phân tán & Apache Kafka', 'Khóa học chuyên sâu thuộc nhóm BACKEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222202', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Khóa học chuyên sâu thuộc nhóm FRONTEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222211', 'CRS-11', 'HTML5, CSS3 & Tailwind CSS Chuyên sâu', 'Khóa học chuyên sâu thuộc nhóm FRONTEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Cơ bản', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222212', 'CRS-12', 'JavaScript ES6+ & Lập trình Bất đồng bộ', 'Khóa học chuyên sâu thuộc nhóm FRONTEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Khá', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222213', 'CRS-13', 'TypeScript Nâng cao & Design System cho Web', 'Khóa học chuyên sâu thuộc nhóm FRONTEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222214', 'CRS-14', 'Vue 3 & Pinia State Management Hiện đại', 'Khóa học chuyên sâu thuộc nhóm FRONTEND, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Khá', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222204', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Khóa học chuyên sâu thuộc nhóm DEVOPS, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Khá', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222215', 'CRS-15', 'Kubernetes Production & Helm Charts', 'Khóa học chuyên sâu thuộc nhóm DEVOPS, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222216', 'CRS-16', 'AWS Cloud Architecture & Terraform IaC', 'Khóa học chuyên sâu thuộc nhóm DEVOPS, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222203', 'CRS-03', 'Tối ưu hóa CSDL quan hệ PostgreSQL', 'Khóa học chuyên sâu thuộc nhóm DATABASE, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222217', 'CRS-17', 'Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 'Khóa học chuyên sâu thuộc nhóm DATABASE, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Khá', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222218', 'CRS-18', 'NoSQL với MongoDB & Caching tốc độ cao Redis', 'Khóa học chuyên sâu thuộc nhóm DATABASE, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Khá', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222205', 'CRS-05', 'Kiểm thử tự động & Chất lượng phần mềm', 'Khóa học chuyên sâu thuộc nhóm QA_DATA, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Tốt', NOW() - interval '30 days'),
    ('22222222-2222-2222-2222-222222222219', 'CRS-19', 'Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn IT', 'Khóa học chuyên sâu thuộc nhóm SOFT_SKILLS, thiết kế sát yêu cầu doanh nghiệp tuyển dụng tại Rikkei Jobs.', 'Cơ bản', NOW() - interval '30 days')
ON CONFLICT (code) DO NOTHING;

-- 3. 41 Course Skill Maps & 41 Skill Course Mappings
INSERT INTO course_skill_map (course_id, skill_id, weight) VALUES
    ('22222222-2222-2222-2222-222222222201', 'SPRING_BOOT', 0.45),
    ('22222222-2222-2222-2222-222222222201', 'MICROSERVICES', 0.35),
    ('22222222-2222-2222-2222-222222222201', 'JAVA_CORE', 0.20),
    ('22222222-2222-2222-2222-222222222207', 'JAVA_CORE', 0.60),
    ('22222222-2222-2222-2222-222222222207', 'DESIGN_PATTERNS', 0.40),
    ('22222222-2222-2222-2222-222222222208', 'NODEJS', 0.55),
    ('22222222-2222-2222-2222-222222222208', 'REST_API', 0.45),
    ('22222222-2222-2222-2222-222222222209', 'GOLANG', 0.60),
    ('22222222-2222-2222-2222-222222222209', 'GRPC', 0.40),
    ('22222222-2222-2222-2222-222222222210', 'APACHE_KAFKA', 0.50),
    ('22222222-2222-2222-2222-222222222210', 'SYSTEM_DESIGN', 0.50),
    ('22222222-2222-2222-2222-222222222202', 'REACT', 0.45),
    ('22222222-2222-2222-2222-222222222202', 'NEXTJS', 0.30),
    ('22222222-2222-2222-2222-222222222202', 'TYPESCRIPT', 0.25),
    ('22222222-2222-2222-2222-222222222211', 'HTML_CSS', 0.50),
    ('22222222-2222-2222-2222-222222222211', 'TAILWIND_CSS', 0.50),
    ('22222222-2222-2222-2222-222222222212', 'JAVASCRIPT', 0.70),
    ('22222222-2222-2222-2222-222222222212', 'TYPESCRIPT', 0.30),
    ('22222222-2222-2222-2222-222222222213', 'TYPESCRIPT', 0.60),
    ('22222222-2222-2222-2222-222222222213', 'REACT', 0.40),
    ('22222222-2222-2222-2222-222222222214', 'VUEJS', 0.65),
    ('22222222-2222-2222-2222-222222222214', 'JAVASCRIPT', 0.35),
    ('22222222-2222-2222-2222-222222222204', 'DOCKER', 0.45),
    ('22222222-2222-2222-2222-222222222204', 'CI_CD', 0.35),
    ('22222222-2222-2222-2222-222222222204', 'LINUX_BASH', 0.20),
    ('22222222-2222-2222-2222-222222222215', 'KUBERNETES', 0.65),
    ('22222222-2222-2222-2222-222222222215', 'DOCKER', 0.35),
    ('22222222-2222-2222-2222-222222222216', 'AWS', 0.60),
    ('22222222-2222-2222-2222-222222222216', 'TERRAFORM', 0.40),
    ('22222222-2222-2222-2222-222222222203', 'SQL', 0.45),
    ('22222222-2222-2222-2222-222222222203', 'POSTGRESQL', 0.35),
    ('22222222-2222-2222-2222-222222222203', 'DATABASE_OPTIMIZATION', 0.20),
    ('22222222-2222-2222-2222-222222222217', 'MYSQL', 0.55),
    ('22222222-2222-2222-2222-222222222217', 'SQL', 0.45),
    ('22222222-2222-2222-2222-222222222218', 'MONGODB', 0.50),
    ('22222222-2222-2222-2222-222222222218', 'REDIS', 0.50),
    ('22222222-2222-2222-2222-222222222205', 'TESTING', 0.60),
    ('22222222-2222-2222-2222-222222222205', 'SELENIUM', 0.40),
    ('22222222-2222-2222-2222-222222222219', 'COMMUNICATION', 0.45),
    ('22222222-2222-2222-2222-222222222219', 'TEAMWORK', 0.35),
    ('22222222-2222-2222-2222-222222222219', 'PRESENTATION', 0.20)
ON CONFLICT (course_id, skill_id) DO NOTHING;

INSERT INTO skill_course_mappings (skill_id, lms_course_id, course_name, lesson_hint, level_from, level_to) VALUES
    ('SPRING_BOOT', 'CRS-01', 'Lập trình Spring Boot nâng cao & Microservices', 'Lộ trình học thực chiến bổ sung kỹ năng SPRING_BOOT', 0, 80),
    ('MICROSERVICES', 'CRS-01', 'Lập trình Spring Boot nâng cao & Microservices', 'Lộ trình học thực chiến bổ sung kỹ năng MICROSERVICES', 0, 80),
    ('JAVA_CORE', 'CRS-01', 'Lập trình Spring Boot nâng cao & Microservices', 'Lộ trình học thực chiến bổ sung kỹ năng JAVA_CORE', 0, 80),
    ('JAVA_CORE', 'CRS-07', 'Java Core & Lập trình Hướng đối tượng thực chiến', 'Lộ trình học thực chiến bổ sung kỹ năng JAVA_CORE', 0, 80),
    ('DESIGN_PATTERNS', 'CRS-07', 'Java Core & Lập trình Hướng đối tượng thực chiến', 'Lộ trình học thực chiến bổ sung kỹ năng DESIGN_PATTERNS', 0, 80),
    ('NODEJS', 'CRS-08', 'Xây dựng RESTful API chuẩn Enterprise với Node.js', 'Lộ trình học thực chiến bổ sung kỹ năng NODEJS', 0, 80),
    ('REST_API', 'CRS-08', 'Xây dựng RESTful API chuẩn Enterprise với Node.js', 'Lộ trình học thực chiến bổ sung kỹ năng REST_API', 0, 80),
    ('GOLANG', 'CRS-09', 'Lập trình Golang & gRPC Hiệu năng cao', 'Lộ trình học thực chiến bổ sung kỹ năng GOLANG', 0, 80),
    ('GRPC', 'CRS-09', 'Lập trình Golang & gRPC Hiệu năng cao', 'Lộ trình học thực chiến bổ sung kỹ năng GRPC', 0, 80),
    ('APACHE_KAFKA', 'CRS-10', 'Kiến trúc Hệ thống Phân tán & Apache Kafka', 'Lộ trình học thực chiến bổ sung kỹ năng APACHE_KAFKA', 0, 80),
    ('SYSTEM_DESIGN', 'CRS-10', 'Kiến trúc Hệ thống Phân tán & Apache Kafka', 'Lộ trình học thực chiến bổ sung kỹ năng SYSTEM_DESIGN', 0, 80),
    ('REACT', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Lộ trình học thực chiến bổ sung kỹ năng REACT', 0, 80),
    ('NEXTJS', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Lộ trình học thực chiến bổ sung kỹ năng NEXTJS', 0, 80),
    ('TYPESCRIPT', 'CRS-02', 'React 19 & Next.js Fullstack Architecture', 'Lộ trình học thực chiến bổ sung kỹ năng TYPESCRIPT', 0, 80),
    ('HTML_CSS', 'CRS-11', 'HTML5, CSS3 & Tailwind CSS Chuyên sâu', 'Lộ trình học thực chiến bổ sung kỹ năng HTML_CSS', 0, 80),
    ('TAILWIND_CSS', 'CRS-11', 'HTML5, CSS3 & Tailwind CSS Chuyên sâu', 'Lộ trình học thực chiến bổ sung kỹ năng TAILWIND_CSS', 0, 80),
    ('JAVASCRIPT', 'CRS-12', 'JavaScript ES6+ & Lập trình Bất đồng bộ', 'Lộ trình học thực chiến bổ sung kỹ năng JAVASCRIPT', 0, 80),
    ('TYPESCRIPT', 'CRS-12', 'JavaScript ES6+ & Lập trình Bất đồng bộ', 'Lộ trình học thực chiến bổ sung kỹ năng TYPESCRIPT', 0, 80),
    ('TYPESCRIPT', 'CRS-13', 'TypeScript Nâng cao & Design System cho Web', 'Lộ trình học thực chiến bổ sung kỹ năng TYPESCRIPT', 0, 80),
    ('REACT', 'CRS-13', 'TypeScript Nâng cao & Design System cho Web', 'Lộ trình học thực chiến bổ sung kỹ năng REACT', 0, 80),
    ('VUEJS', 'CRS-14', 'Vue 3 & Pinia State Management Hiện đại', 'Lộ trình học thực chiến bổ sung kỹ năng VUEJS', 0, 80),
    ('JAVASCRIPT', 'CRS-14', 'Vue 3 & Pinia State Management Hiện đại', 'Lộ trình học thực chiến bổ sung kỹ năng JAVASCRIPT', 0, 80),
    ('DOCKER', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Lộ trình học thực chiến bổ sung kỹ năng DOCKER', 0, 80),
    ('CI_CD', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Lộ trình học thực chiến bổ sung kỹ năng CI_CD', 0, 80),
    ('LINUX_BASH', 'CRS-04', 'DevOps cơ bản: Docker, CI/CD và Linux', 'Lộ trình học thực chiến bổ sung kỹ năng LINUX_BASH', 0, 80),
    ('KUBERNETES', 'CRS-15', 'Kubernetes Production & Helm Charts', 'Lộ trình học thực chiến bổ sung kỹ năng KUBERNETES', 0, 80),
    ('DOCKER', 'CRS-15', 'Kubernetes Production & Helm Charts', 'Lộ trình học thực chiến bổ sung kỹ năng DOCKER', 0, 80),
    ('AWS', 'CRS-16', 'AWS Cloud Architecture & Terraform IaC', 'Lộ trình học thực chiến bổ sung kỹ năng AWS', 0, 80),
    ('TERRAFORM', 'CRS-16', 'AWS Cloud Architecture & Terraform IaC', 'Lộ trình học thực chiến bổ sung kỹ năng TERRAFORM', 0, 80),
    ('SQL', 'CRS-03', 'Tối ưu hóa CSDL quan hệ PostgreSQL', 'Lộ trình học thực chiến bổ sung kỹ năng SQL', 0, 80),
    ('POSTGRESQL', 'CRS-03', 'Tối ưu hóa CSDL quan hệ PostgreSQL', 'Lộ trình học thực chiến bổ sung kỹ năng POSTGRESQL', 0, 80),
    ('DATABASE_OPTIMIZATION', 'CRS-03', 'Tối ưu hóa CSDL quan hệ PostgreSQL', 'Lộ trình học thực chiến bổ sung kỹ năng DATABASE_OPTIMIZATION', 0, 80),
    ('MYSQL', 'CRS-17', 'Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 'Lộ trình học thực chiến bổ sung kỹ năng MYSQL', 0, 80),
    ('SQL', 'CRS-17', 'Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 'Lộ trình học thực chiến bổ sung kỹ năng SQL', 0, 80),
    ('MONGODB', 'CRS-18', 'NoSQL với MongoDB & Caching tốc độ cao Redis', 'Lộ trình học thực chiến bổ sung kỹ năng MONGODB', 0, 80),
    ('REDIS', 'CRS-18', 'NoSQL với MongoDB & Caching tốc độ cao Redis', 'Lộ trình học thực chiến bổ sung kỹ năng REDIS', 0, 80),
    ('TESTING', 'CRS-05', 'Kiểm thử tự động & Chất lượng phần mềm', 'Lộ trình học thực chiến bổ sung kỹ năng TESTING', 0, 80),
    ('SELENIUM', 'CRS-05', 'Kiểm thử tự động & Chất lượng phần mềm', 'Lộ trình học thực chiến bổ sung kỹ năng SELENIUM', 0, 80),
    ('COMMUNICATION', 'CRS-19', 'Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn IT', 'Lộ trình học thực chiến bổ sung kỹ năng COMMUNICATION', 0, 80),
    ('TEAMWORK', 'CRS-19', 'Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn IT', 'Lộ trình học thực chiến bổ sung kỹ năng TEAMWORK', 0, 80),
    ('PRESENTATION', 'CRS-19', 'Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn IT', 'Lộ trình học thực chiến bổ sung kỹ năng PRESENTATION', 0, 80)
ON CONFLICT (skill_id, lms_course_id) DO NOTHING;

-- 4. 54 Modules & 144 Lessons
INSERT INTO course_modules (id, course_id, title, order_index) VALUES
    ('fd6044b0-ba3a-495b-8e5f-ef0ac5daeb01', '22222222-2222-2222-2222-222222222201', 'Phần 1: Nền tảng & Kiến trúc CRS-01', 1),
    ('59d2271c-20a3-42d4-ac4f-7f4e421692ff', '22222222-2222-2222-2222-222222222201', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('d0213708-7381-487f-9dc6-10bd89b067bd', '22222222-2222-2222-2222-222222222201', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('ee937e81-1d1d-4959-aa13-a57bdbe8a77f', '22222222-2222-2222-2222-222222222207', 'Phần 1: Nền tảng & Kiến trúc CRS-07', 1),
    ('83f21f2a-53ca-42fd-adac-744e64cc6b6f', '22222222-2222-2222-2222-222222222207', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('eb163445-4492-4e7a-a2c5-50bfd41e5845', '22222222-2222-2222-2222-222222222207', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('ad17be11-b346-4c47-a680-9c6803efae20', '22222222-2222-2222-2222-222222222208', 'Phần 1: Nền tảng & Kiến trúc CRS-08', 1),
    ('378c5dff-b3e5-4138-81db-034308e5ed6a', '22222222-2222-2222-2222-222222222208', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('90cf7727-f06e-4181-bb1c-a5197158c6af', '22222222-2222-2222-2222-222222222208', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('cd58bb6c-ab77-4f87-966a-7f956386eabe', '22222222-2222-2222-2222-222222222209', 'Phần 1: Nền tảng & Kiến trúc CRS-09', 1),
    ('84e3f2b2-9aee-4546-b285-52ed24837e70', '22222222-2222-2222-2222-222222222209', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('5776cc30-e098-48d5-aed5-deeaae3f87f5', '22222222-2222-2222-2222-222222222209', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('f2120916-845a-4d32-b3e0-0aa764554b43', '22222222-2222-2222-2222-222222222210', 'Phần 1: Nền tảng & Kiến trúc CRS-10', 1),
    ('5efeb02a-bea3-4681-b524-3c7a47967e53', '22222222-2222-2222-2222-222222222210', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('c1b58924-024c-4913-a8ff-2517872b9a14', '22222222-2222-2222-2222-222222222210', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('aa38a4cd-a1ce-410b-849e-6f112b732d43', '22222222-2222-2222-2222-222222222202', 'Phần 1: Nền tảng & Kiến trúc CRS-02', 1),
    ('89b03052-6245-4325-b48a-e06833dde1b7', '22222222-2222-2222-2222-222222222202', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('dc3d0ee9-5b53-48aa-b2fa-759b2d472fd6', '22222222-2222-2222-2222-222222222202', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('d9cd22ee-e6c0-4f39-897a-1b3c8b4e3d16', '22222222-2222-2222-2222-222222222211', 'Phần 1: Nền tảng & Kiến trúc CRS-11', 1),
    ('40a3151e-07c0-4ddc-a1ad-8978fe6a3c64', '22222222-2222-2222-2222-222222222211', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('7ba14d0b-84d3-4f22-b712-4d76d8ea89af', '22222222-2222-2222-2222-222222222211', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('850c1d7e-6b86-4481-91c2-7d4145d08a81', '22222222-2222-2222-2222-222222222212', 'Phần 1: Nền tảng & Kiến trúc CRS-12', 1),
    ('fddeb368-94a9-458a-851b-295c5db5c237', '22222222-2222-2222-2222-222222222212', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('b39297ac-f823-4fb1-ac71-9e844baa2532', '22222222-2222-2222-2222-222222222212', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('44fbd565-5e49-4be7-85b6-3986e2267448', '22222222-2222-2222-2222-222222222213', 'Phần 1: Nền tảng & Kiến trúc CRS-13', 1),
    ('8096fcef-efcc-4fb9-b56e-5669e83b300f', '22222222-2222-2222-2222-222222222213', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('195a9e95-a626-4337-981f-394ae6675cb6', '22222222-2222-2222-2222-222222222213', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('2b9ec3d4-b406-4db7-b269-336a22941647', '22222222-2222-2222-2222-222222222214', 'Phần 1: Nền tảng & Kiến trúc CRS-14', 1),
    ('9f94368d-1d85-4900-82d2-a031cedfc5a5', '22222222-2222-2222-2222-222222222214', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('15824269-45e5-47ea-90ea-5d55993238f9', '22222222-2222-2222-2222-222222222214', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('446b2338-7f98-4732-a623-af383c94edd8', '22222222-2222-2222-2222-222222222204', 'Phần 1: Nền tảng & Kiến trúc CRS-04', 1),
    ('d592eeb0-e7a4-4726-bd7d-2ff7e99d577a', '22222222-2222-2222-2222-222222222204', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('fc6e1dde-caef-4cff-b1ef-eb1f7cb67339', '22222222-2222-2222-2222-222222222204', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('54eb96d6-1e04-40ec-8e1e-d64396888a50', '22222222-2222-2222-2222-222222222215', 'Phần 1: Nền tảng & Kiến trúc CRS-15', 1),
    ('1d423af7-7446-4a9a-b479-5847810319df', '22222222-2222-2222-2222-222222222215', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('1487b5a6-f28d-422c-a733-a9295275668a', '22222222-2222-2222-2222-222222222215', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('a2908f83-896a-46b2-a1eb-53032185daa9', '22222222-2222-2222-2222-222222222216', 'Phần 1: Nền tảng & Kiến trúc CRS-16', 1),
    ('be1aa2d2-ac63-461a-8da9-04dabb5787c1', '22222222-2222-2222-2222-222222222216', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('f9e26482-4f23-41c1-98f9-767c2d07f841', '22222222-2222-2222-2222-222222222216', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('18299577-efc3-4a79-bf93-d2f15b6f5564', '22222222-2222-2222-2222-222222222203', 'Phần 1: Nền tảng & Kiến trúc CRS-03', 1),
    ('45955581-fd0c-4f83-8279-7352afae6dcd', '22222222-2222-2222-2222-222222222203', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('829e173f-6aa7-437d-a3df-b182f88d61ef', '22222222-2222-2222-2222-222222222203', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('4b814d6c-0ca1-46f1-b006-7ff81099fa75', '22222222-2222-2222-2222-222222222217', 'Phần 1: Nền tảng & Kiến trúc CRS-17', 1),
    ('f1fd40ee-7266-49d8-829e-9ea0a00c521d', '22222222-2222-2222-2222-222222222217', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('d907a1a0-2c1e-4c11-ac54-e1c3a7c9cea3', '22222222-2222-2222-2222-222222222217', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('7d6c6bf0-d36d-420b-b6ca-2df6d857b3e1', '22222222-2222-2222-2222-222222222218', 'Phần 1: Nền tảng & Kiến trúc CRS-18', 1),
    ('d3ac9601-00ad-4ebc-84f4-c9e1ec77fefd', '22222222-2222-2222-2222-222222222218', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('668e86c1-b934-4ebc-a1c1-b67b91f0d826', '22222222-2222-2222-2222-222222222218', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('1ea92a9f-d2cb-433a-9b13-69540918b946', '22222222-2222-2222-2222-222222222205', 'Phần 1: Nền tảng & Kiến trúc CRS-05', 1),
    ('3b8a2657-e9ba-4900-934d-e16838a4164f', '22222222-2222-2222-2222-222222222205', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('2e85b554-b20e-4c51-b3d4-abb5707598c5', '22222222-2222-2222-2222-222222222205', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3),
    ('16215a7f-8ef4-4cee-a9a6-3ba7869c66ee', '22222222-2222-2222-2222-222222222219', 'Phần 1: Nền tảng & Kiến trúc CRS-19', 1),
    ('61c220d9-2617-4513-b66a-391734b2f25c', '22222222-2222-2222-2222-222222222219', 'Phần 2: Thực hành Dự án & Best Practices', 2),
    ('e5a1beef-f356-4157-95d9-16f2419d1c58', '22222222-2222-2222-2222-222222222219', 'Phần 3: Tối ưu hóa & Bài tập Tổng hợp', 3)
ON CONFLICT (course_id, order_index) DO NOTHING;

INSERT INTO course_lessons (id, module_id, title, duration_minutes, order_index) VALUES
    ('4de21448-5c86-4e4e-80a2-05359ed85b11', 'fd6044b0-ba3a-495b-8e5f-ef0ac5daeb01', 'Bài 1.1: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 27, 1),
    ('e87c0d9c-44d5-487d-9d4c-571140fc2d66', 'fd6044b0-ba3a-495b-8e5f-ef0ac5daeb01', 'Bài 1.2: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 34, 2),
    ('a5a46664-e5e2-4b6c-b251-5f0f283f9a38', 'fd6044b0-ba3a-495b-8e5f-ef0ac5daeb01', 'Bài 1.3: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 41, 3),
    ('313e2ebf-0b35-43a9-8a75-fb1c75444661', '59d2271c-20a3-42d4-ac4f-7f4e421692ff', 'Bài 2.1: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 34, 1),
    ('ea911f73-4341-4b2a-b19d-5cbc2371246b', '59d2271c-20a3-42d4-ac4f-7f4e421692ff', 'Bài 2.2: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 48, 2),
    ('579bbb6f-dba6-4a86-aa0c-665d27abf106', '59d2271c-20a3-42d4-ac4f-7f4e421692ff', 'Bài 2.3: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 27, 3),
    ('fd3f0ed0-e319-4c2d-8e4b-d6e17577ad2d', 'd0213708-7381-487f-9dc6-10bd89b067bd', 'Bài 3.1: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 41, 1),
    ('e997301c-f5a9-4f03-a1da-f716a9534329', 'd0213708-7381-487f-9dc6-10bd89b067bd', 'Bài 3.2: Thực chiến Lập trình Spring Boot nâng cao & Microservice', 27, 2),
    ('3a3321ec-3382-4d07-b592-6332a1b096e4', 'ee937e81-1d1d-4959-aa13-a57bdbe8a77f', 'Bài 1.1: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 27, 1),
    ('b2c7014e-cf31-47c2-9936-b39166f459d0', 'ee937e81-1d1d-4959-aa13-a57bdbe8a77f', 'Bài 1.2: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 34, 2),
    ('24442a86-ad51-4945-88fd-e7057f51d14e', 'ee937e81-1d1d-4959-aa13-a57bdbe8a77f', 'Bài 1.3: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 41, 3),
    ('854ac8b4-32f7-41cf-b1a1-390f78c6b8bb', '83f21f2a-53ca-42fd-adac-744e64cc6b6f', 'Bài 2.1: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 34, 1),
    ('752d6305-aa04-4a8d-87a3-69a21641277b', '83f21f2a-53ca-42fd-adac-744e64cc6b6f', 'Bài 2.2: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 48, 2),
    ('9a8ba771-5834-4e86-8ac6-a1909328561a', '83f21f2a-53ca-42fd-adac-744e64cc6b6f', 'Bài 2.3: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 27, 3),
    ('7f097685-0e74-42f2-8b94-eb5c8b836257', 'eb163445-4492-4e7a-a2c5-50bfd41e5845', 'Bài 3.1: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 41, 1),
    ('03720bea-abef-480f-8c43-0f281262f7e5', 'eb163445-4492-4e7a-a2c5-50bfd41e5845', 'Bài 3.2: Thực chiến Java Core & Lập trình Hướng đối tượng thực ch', 27, 2),
    ('16e621db-bbd8-4650-ac6f-f30cc1356857', 'ad17be11-b346-4c47-a680-9c6803efae20', 'Bài 1.1: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 27, 1),
    ('8dc8c301-e387-49a7-b1e3-1d374e92253a', 'ad17be11-b346-4c47-a680-9c6803efae20', 'Bài 1.2: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 34, 2),
    ('891995ad-2c74-4170-b635-20af72e533fe', 'ad17be11-b346-4c47-a680-9c6803efae20', 'Bài 1.3: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 41, 3),
    ('ca47c8e6-c61c-45ac-911d-4ca597e3e8a2', '378c5dff-b3e5-4138-81db-034308e5ed6a', 'Bài 2.1: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 34, 1),
    ('408c2870-eb45-4fe8-a3d1-0f55cdbb3282', '378c5dff-b3e5-4138-81db-034308e5ed6a', 'Bài 2.2: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 48, 2),
    ('61fadf8e-454a-4776-8817-c26c083e318e', '378c5dff-b3e5-4138-81db-034308e5ed6a', 'Bài 2.3: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 27, 3),
    ('df5c7c07-9f51-4898-ac59-0762a5bbb27e', '90cf7727-f06e-4181-bb1c-a5197158c6af', 'Bài 3.1: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 41, 1),
    ('8b94a778-c096-4f64-b0ab-8dbf39c0d805', '90cf7727-f06e-4181-bb1c-a5197158c6af', 'Bài 3.2: Thực chiến Xây dựng RESTful API chuẩn Enterprise với Nod', 27, 2),
    ('771cb6c7-aa59-469e-8561-6967881d6039', 'cd58bb6c-ab77-4f87-966a-7f956386eabe', 'Bài 1.1: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 27, 1),
    ('ca86fdc0-2f1d-44b8-8658-e8d12520efd7', 'cd58bb6c-ab77-4f87-966a-7f956386eabe', 'Bài 1.2: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 34, 2),
    ('8cb63ebe-f54f-4fff-ad85-7443fd6b57ca', 'cd58bb6c-ab77-4f87-966a-7f956386eabe', 'Bài 1.3: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 41, 3),
    ('6fa4e46f-121d-47c3-985b-48d5b183b84f', '84e3f2b2-9aee-4546-b285-52ed24837e70', 'Bài 2.1: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 34, 1),
    ('0a9fd833-35a8-4760-96dd-8c6038dfd42d', '84e3f2b2-9aee-4546-b285-52ed24837e70', 'Bài 2.2: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 48, 2),
    ('58ecbc9f-0f45-43dc-be55-198801180043', '84e3f2b2-9aee-4546-b285-52ed24837e70', 'Bài 2.3: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 27, 3),
    ('7e500d7d-4c17-48e9-9ad9-8a858c423550', '5776cc30-e098-48d5-aed5-deeaae3f87f5', 'Bài 3.1: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 41, 1),
    ('ab01b08c-313d-493e-bc6e-e03084d29030', '5776cc30-e098-48d5-aed5-deeaae3f87f5', 'Bài 3.2: Thực chiến Lập trình Golang & gRPC Hiệu năng cao', 27, 2),
    ('52a677b9-72f7-4dfa-8608-372d1c1480ad', 'f2120916-845a-4d32-b3e0-0aa764554b43', 'Bài 1.1: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 27, 1),
    ('5579e66d-24e1-4f40-bd9f-27c8b92e3497', 'f2120916-845a-4d32-b3e0-0aa764554b43', 'Bài 1.2: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 34, 2),
    ('336d5330-a3d3-46ee-9001-87a538fa602f', 'f2120916-845a-4d32-b3e0-0aa764554b43', 'Bài 1.3: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 41, 3),
    ('9c0f8105-c3c3-435f-985e-fa9be84f48a6', '5efeb02a-bea3-4681-b524-3c7a47967e53', 'Bài 2.1: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 34, 1),
    ('8029e8f6-11cb-4df4-9561-8e41a56073be', '5efeb02a-bea3-4681-b524-3c7a47967e53', 'Bài 2.2: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 48, 2),
    ('e1848f51-703c-433f-9d74-a3f27db506a9', '5efeb02a-bea3-4681-b524-3c7a47967e53', 'Bài 2.3: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 27, 3),
    ('576a7044-e8d9-4927-bf6b-6b9b36431833', 'c1b58924-024c-4913-a8ff-2517872b9a14', 'Bài 3.1: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 41, 1),
    ('0052a798-0bf3-44cf-86c1-9dfc6034810b', 'c1b58924-024c-4913-a8ff-2517872b9a14', 'Bài 3.2: Thực chiến Kiến trúc Hệ thống Phân tán & Apache Kafka', 27, 2),
    ('687f897e-c866-4c30-8e3f-b770d5903375', 'aa38a4cd-a1ce-410b-849e-6f112b732d43', 'Bài 1.1: Thực chiến React 19 & Next.js Fullstack Architecture', 27, 1),
    ('d82ca277-dd92-44d2-b812-29bd413277b0', 'aa38a4cd-a1ce-410b-849e-6f112b732d43', 'Bài 1.2: Thực chiến React 19 & Next.js Fullstack Architecture', 34, 2),
    ('247449ba-0648-4293-be8e-fe20e3d6f92c', 'aa38a4cd-a1ce-410b-849e-6f112b732d43', 'Bài 1.3: Thực chiến React 19 & Next.js Fullstack Architecture', 41, 3),
    ('272edc05-f5d7-43ac-87e6-b9a26ae7dfa4', '89b03052-6245-4325-b48a-e06833dde1b7', 'Bài 2.1: Thực chiến React 19 & Next.js Fullstack Architecture', 34, 1),
    ('756d0909-f3bf-451c-bb1e-6a56e0e1725c', '89b03052-6245-4325-b48a-e06833dde1b7', 'Bài 2.2: Thực chiến React 19 & Next.js Fullstack Architecture', 48, 2),
    ('6475790e-2b11-40f6-9fa4-068d55aaf422', '89b03052-6245-4325-b48a-e06833dde1b7', 'Bài 2.3: Thực chiến React 19 & Next.js Fullstack Architecture', 27, 3),
    ('71baf5bb-4f11-491f-928e-35db767f5ef3', 'dc3d0ee9-5b53-48aa-b2fa-759b2d472fd6', 'Bài 3.1: Thực chiến React 19 & Next.js Fullstack Architecture', 41, 1),
    ('7c63ddf3-04bc-4161-8768-0d3a0b1c8fc7', 'dc3d0ee9-5b53-48aa-b2fa-759b2d472fd6', 'Bài 3.2: Thực chiến React 19 & Next.js Fullstack Architecture', 27, 2),
    ('a8c9358a-6262-43cd-8fd0-fb884624a3d3', 'd9cd22ee-e6c0-4f39-897a-1b3c8b4e3d16', 'Bài 1.1: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 27, 1),
    ('570e1361-2384-4ff8-8acb-05afd98aca9e', 'd9cd22ee-e6c0-4f39-897a-1b3c8b4e3d16', 'Bài 1.2: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 34, 2),
    ('95c8bc69-bffa-4745-9b3d-fe4b871c2dda', 'd9cd22ee-e6c0-4f39-897a-1b3c8b4e3d16', 'Bài 1.3: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 41, 3),
    ('c972699d-4eee-4d11-8778-99c1eb4776d0', '40a3151e-07c0-4ddc-a1ad-8978fe6a3c64', 'Bài 2.1: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 34, 1),
    ('4b226bac-59b1-4742-b3e8-fb0795f11940', '40a3151e-07c0-4ddc-a1ad-8978fe6a3c64', 'Bài 2.2: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 48, 2),
    ('9fe1efa5-1c8d-4e23-b03f-f88587dd093a', '40a3151e-07c0-4ddc-a1ad-8978fe6a3c64', 'Bài 2.3: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 27, 3),
    ('f2cb01b4-c7b9-4fdb-94d5-8f39c1de6a2d', '7ba14d0b-84d3-4f22-b712-4d76d8ea89af', 'Bài 3.1: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 41, 1),
    ('dd7f90a2-6493-425e-9198-7d3ebeaf7a62', '7ba14d0b-84d3-4f22-b712-4d76d8ea89af', 'Bài 3.2: Thực chiến HTML5, CSS3 & Tailwind CSS Chuyên sâu', 27, 2),
    ('ef692e58-5297-4352-9972-b5e74b97a688', '850c1d7e-6b86-4481-91c2-7d4145d08a81', 'Bài 1.1: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 27, 1),
    ('6364e33e-bdae-4103-9632-246787d7aa1b', '850c1d7e-6b86-4481-91c2-7d4145d08a81', 'Bài 1.2: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 34, 2),
    ('4c66206f-03f5-4f2a-901c-c281e976c2a2', '850c1d7e-6b86-4481-91c2-7d4145d08a81', 'Bài 1.3: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 41, 3),
    ('1565a08b-bec3-4d43-8836-1dfb279d2afd', 'fddeb368-94a9-458a-851b-295c5db5c237', 'Bài 2.1: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 34, 1),
    ('69e81580-a255-48c6-8324-2bad71a6b40f', 'fddeb368-94a9-458a-851b-295c5db5c237', 'Bài 2.2: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 48, 2),
    ('28739dfe-9a60-4374-ad0a-9f19766961af', 'fddeb368-94a9-458a-851b-295c5db5c237', 'Bài 2.3: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 27, 3),
    ('28128b2e-c77c-49e6-882a-73212142dd0d', 'b39297ac-f823-4fb1-ac71-9e844baa2532', 'Bài 3.1: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 41, 1),
    ('6a0567c6-2639-4ff1-b80f-12c8dd1f5ecc', 'b39297ac-f823-4fb1-ac71-9e844baa2532', 'Bài 3.2: Thực chiến JavaScript ES6+ & Lập trình Bất đồng bộ', 27, 2),
    ('d6f701b1-553c-45a8-a98b-3c9a3d50937d', '44fbd565-5e49-4be7-85b6-3986e2267448', 'Bài 1.1: Thực chiến TypeScript Nâng cao & Design System cho Web', 27, 1),
    ('71824df8-e86c-4d92-bdd6-3620d28ea1c5', '44fbd565-5e49-4be7-85b6-3986e2267448', 'Bài 1.2: Thực chiến TypeScript Nâng cao & Design System cho Web', 34, 2),
    ('303af261-e678-4a83-bd7b-8ca7a122a532', '44fbd565-5e49-4be7-85b6-3986e2267448', 'Bài 1.3: Thực chiến TypeScript Nâng cao & Design System cho Web', 41, 3),
    ('b2955fb5-632f-45fe-8ee9-bff7832315b1', '8096fcef-efcc-4fb9-b56e-5669e83b300f', 'Bài 2.1: Thực chiến TypeScript Nâng cao & Design System cho Web', 34, 1),
    ('9cb53f43-c63b-449a-907c-bfe5d274676b', '8096fcef-efcc-4fb9-b56e-5669e83b300f', 'Bài 2.2: Thực chiến TypeScript Nâng cao & Design System cho Web', 48, 2),
    ('073e484f-2abe-4b1f-8143-58dced433bc6', '8096fcef-efcc-4fb9-b56e-5669e83b300f', 'Bài 2.3: Thực chiến TypeScript Nâng cao & Design System cho Web', 27, 3),
    ('72b55561-2fd8-4428-982c-41fd00531a97', '195a9e95-a626-4337-981f-394ae6675cb6', 'Bài 3.1: Thực chiến TypeScript Nâng cao & Design System cho Web', 41, 1),
    ('c092cbdb-f431-419d-bace-612dfc84ac9c', '195a9e95-a626-4337-981f-394ae6675cb6', 'Bài 3.2: Thực chiến TypeScript Nâng cao & Design System cho Web', 27, 2),
    ('827ce5d5-a301-4553-a81c-fa522c67c894', '2b9ec3d4-b406-4db7-b269-336a22941647', 'Bài 1.1: Thực chiến Vue 3 & Pinia State Management Hiện đại', 27, 1),
    ('de80212e-bc97-437b-9097-85fa677413a8', '2b9ec3d4-b406-4db7-b269-336a22941647', 'Bài 1.2: Thực chiến Vue 3 & Pinia State Management Hiện đại', 34, 2),
    ('79ad8be2-e5e6-4258-856e-3df5882c66ac', '2b9ec3d4-b406-4db7-b269-336a22941647', 'Bài 1.3: Thực chiến Vue 3 & Pinia State Management Hiện đại', 41, 3),
    ('25833b49-7755-489c-a986-8dc1fde71678', '9f94368d-1d85-4900-82d2-a031cedfc5a5', 'Bài 2.1: Thực chiến Vue 3 & Pinia State Management Hiện đại', 34, 1),
    ('2017464a-5f46-47f9-98be-5ffa5432b6fe', '9f94368d-1d85-4900-82d2-a031cedfc5a5', 'Bài 2.2: Thực chiến Vue 3 & Pinia State Management Hiện đại', 48, 2),
    ('0c9f6ffb-e9f7-49e5-9acc-6d78bb0c2120', '9f94368d-1d85-4900-82d2-a031cedfc5a5', 'Bài 2.3: Thực chiến Vue 3 & Pinia State Management Hiện đại', 27, 3),
    ('6f7c768a-7db6-4608-8c8d-89b2a0aea9cd', '15824269-45e5-47ea-90ea-5d55993238f9', 'Bài 3.1: Thực chiến Vue 3 & Pinia State Management Hiện đại', 41, 1),
    ('f9edab52-c0c5-4b69-bcf7-8d428ddaadd6', '15824269-45e5-47ea-90ea-5d55993238f9', 'Bài 3.2: Thực chiến Vue 3 & Pinia State Management Hiện đại', 27, 2),
    ('0c7b2341-5cae-43d4-ad1c-50766cafbb18', '446b2338-7f98-4732-a623-af383c94edd8', 'Bài 1.1: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 27, 1),
    ('facaa61b-0a1f-4925-b84c-4b6911e2e4d9', '446b2338-7f98-4732-a623-af383c94edd8', 'Bài 1.2: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 34, 2),
    ('6dc5a0c9-95eb-44da-bfe9-0c23bb72cccb', '446b2338-7f98-4732-a623-af383c94edd8', 'Bài 1.3: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 41, 3),
    ('ecb3b6ed-0d7d-4fee-ac59-c654b0eabffa', 'd592eeb0-e7a4-4726-bd7d-2ff7e99d577a', 'Bài 2.1: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 34, 1),
    ('75c88b14-56ea-47c8-82b9-0e50b01e2c26', 'd592eeb0-e7a4-4726-bd7d-2ff7e99d577a', 'Bài 2.2: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 48, 2),
    ('3b5b2ce6-e3d2-498c-8cb1-0cda8bd9469b', 'd592eeb0-e7a4-4726-bd7d-2ff7e99d577a', 'Bài 2.3: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 27, 3),
    ('b51c5d4e-c337-4a60-adeb-9ece68f15e02', 'fc6e1dde-caef-4cff-b1ef-eb1f7cb67339', 'Bài 3.1: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 41, 1),
    ('1e564220-3091-4d9a-ac91-6c113c310100', 'fc6e1dde-caef-4cff-b1ef-eb1f7cb67339', 'Bài 3.2: Thực chiến DevOps cơ bản: Docker, CI/CD và Linux', 27, 2),
    ('2bd99181-f697-4d31-ae60-804d147f839d', '54eb96d6-1e04-40ec-8e1e-d64396888a50', 'Bài 1.1: Thực chiến Kubernetes Production & Helm Charts', 27, 1),
    ('d6d62106-90da-4e33-a466-b79cab239ae6', '54eb96d6-1e04-40ec-8e1e-d64396888a50', 'Bài 1.2: Thực chiến Kubernetes Production & Helm Charts', 34, 2),
    ('43d99c17-166f-4d3c-930b-08210b962753', '54eb96d6-1e04-40ec-8e1e-d64396888a50', 'Bài 1.3: Thực chiến Kubernetes Production & Helm Charts', 41, 3),
    ('4a6628dd-9b86-49c8-a6d1-984254fa45ed', '1d423af7-7446-4a9a-b479-5847810319df', 'Bài 2.1: Thực chiến Kubernetes Production & Helm Charts', 34, 1),
    ('968386a3-a078-41e6-8aa8-195202103870', '1d423af7-7446-4a9a-b479-5847810319df', 'Bài 2.2: Thực chiến Kubernetes Production & Helm Charts', 48, 2),
    ('22f5c281-9bbc-4871-8142-d05293c59061', '1d423af7-7446-4a9a-b479-5847810319df', 'Bài 2.3: Thực chiến Kubernetes Production & Helm Charts', 27, 3),
    ('2942a9be-47c2-4300-a703-e3d4c6b5db89', '1487b5a6-f28d-422c-a733-a9295275668a', 'Bài 3.1: Thực chiến Kubernetes Production & Helm Charts', 41, 1),
    ('4f497e4d-a8ce-41e4-80c4-fe1f132e75f3', '1487b5a6-f28d-422c-a733-a9295275668a', 'Bài 3.2: Thực chiến Kubernetes Production & Helm Charts', 27, 2),
    ('48e628f9-490b-4037-b999-a35e6b201395', 'a2908f83-896a-46b2-a1eb-53032185daa9', 'Bài 1.1: Thực chiến AWS Cloud Architecture & Terraform IaC', 27, 1),
    ('7dd51142-3406-4e27-9612-9facee4bd0f9', 'a2908f83-896a-46b2-a1eb-53032185daa9', 'Bài 1.2: Thực chiến AWS Cloud Architecture & Terraform IaC', 34, 2),
    ('47ff5347-c81c-4e4e-a0c6-b6f8f2e87e08', 'a2908f83-896a-46b2-a1eb-53032185daa9', 'Bài 1.3: Thực chiến AWS Cloud Architecture & Terraform IaC', 41, 3),
    ('f2fa5371-3274-4ec2-b176-17cc56787796', 'be1aa2d2-ac63-461a-8da9-04dabb5787c1', 'Bài 2.1: Thực chiến AWS Cloud Architecture & Terraform IaC', 34, 1),
    ('9c4c0878-ce18-4163-beb9-a8028b44bb1d', 'be1aa2d2-ac63-461a-8da9-04dabb5787c1', 'Bài 2.2: Thực chiến AWS Cloud Architecture & Terraform IaC', 48, 2),
    ('f787c459-5e02-4056-8e6e-0a95a74bb103', 'be1aa2d2-ac63-461a-8da9-04dabb5787c1', 'Bài 2.3: Thực chiến AWS Cloud Architecture & Terraform IaC', 27, 3),
    ('513d0a8b-20c0-4b35-8ce7-88f5476a98a1', 'f9e26482-4f23-41c1-98f9-767c2d07f841', 'Bài 3.1: Thực chiến AWS Cloud Architecture & Terraform IaC', 41, 1),
    ('f8e59382-5cfd-4797-8173-26de02b2123f', 'f9e26482-4f23-41c1-98f9-767c2d07f841', 'Bài 3.2: Thực chiến AWS Cloud Architecture & Terraform IaC', 27, 2),
    ('1c885679-a16f-46b2-9939-85b8f9228afb', '18299577-efc3-4a79-bf93-d2f15b6f5564', 'Bài 1.1: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 27, 1),
    ('5e2915fb-3dc5-4671-ad36-91fa0343a3b7', '18299577-efc3-4a79-bf93-d2f15b6f5564', 'Bài 1.2: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 34, 2),
    ('f33ef755-80af-4d45-904a-6f430d6c95c4', '18299577-efc3-4a79-bf93-d2f15b6f5564', 'Bài 1.3: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 41, 3),
    ('b1ff7f0f-2953-47cd-921a-2cffead2f763', '45955581-fd0c-4f83-8279-7352afae6dcd', 'Bài 2.1: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 34, 1),
    ('f7e1ed67-f0da-45bb-959b-a097cb3da47e', '45955581-fd0c-4f83-8279-7352afae6dcd', 'Bài 2.2: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 48, 2),
    ('4070dc2a-2746-4598-83f8-abb24e4c9175', '45955581-fd0c-4f83-8279-7352afae6dcd', 'Bài 2.3: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 27, 3),
    ('4ba71604-d1af-4604-b8ae-d675f2f43b93', '829e173f-6aa7-437d-a3df-b182f88d61ef', 'Bài 3.1: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 41, 1),
    ('81c7f182-bf68-45fc-96aa-c428648c2a06', '829e173f-6aa7-437d-a3df-b182f88d61ef', 'Bài 3.2: Thực chiến Tối ưu hóa CSDL quan hệ PostgreSQL', 27, 2),
    ('43251dc6-d585-449f-832e-77226b893ef9', '4b814d6c-0ca1-46f1-b006-7ff81099fa75', 'Bài 1.1: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 27, 1),
    ('6965dd13-f4cb-4cf2-a33d-eb3ab8624c2a', '4b814d6c-0ca1-46f1-b006-7ff81099fa75', 'Bài 1.2: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 34, 2),
    ('2f2e841f-dd39-49de-a0b5-f8b6164cc73e', '4b814d6c-0ca1-46f1-b006-7ff81099fa75', 'Bài 1.3: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 41, 3),
    ('fcf557f9-3c9c-4c66-8e80-70b671b4875f', 'f1fd40ee-7266-49d8-829e-9ea0a00c521d', 'Bài 2.1: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 34, 1),
    ('29ca3fe4-c659-4455-bdc1-8751fe855abd', 'f1fd40ee-7266-49d8-829e-9ea0a00c521d', 'Bài 2.2: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 48, 2),
    ('c4660a65-c0d3-436e-b663-6991b3eb3ebf', 'f1fd40ee-7266-49d8-829e-9ea0a00c521d', 'Bài 2.3: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 27, 3),
    ('517529b3-5a94-4ab6-bbde-63ad1dbc242d', 'd907a1a0-2c1e-4c11-ac54-e1c3a7c9cea3', 'Bài 3.1: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 41, 1),
    ('3b9b3741-e735-477f-8514-5dcf850d2095', 'd907a1a0-2c1e-4c11-ac54-e1c3a7c9cea3', 'Bài 3.2: Thực chiến Thiết kế CSDL MySQL & Truy vấn SQL Nâng cao', 27, 2),
    ('e88a86e3-3954-46ed-9dbd-35f38657bc16', '7d6c6bf0-d36d-420b-b6ca-2df6d857b3e1', 'Bài 1.1: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 27, 1),
    ('3cd271c7-57d4-4087-9f44-7a0aca35c715', '7d6c6bf0-d36d-420b-b6ca-2df6d857b3e1', 'Bài 1.2: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 34, 2),
    ('bf0d57e3-ab80-4f2d-a4b6-53830a668f42', '7d6c6bf0-d36d-420b-b6ca-2df6d857b3e1', 'Bài 1.3: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 41, 3),
    ('b0c5cc2f-7275-46b8-ac32-eecb06fb9a58', 'd3ac9601-00ad-4ebc-84f4-c9e1ec77fefd', 'Bài 2.1: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 34, 1),
    ('f5f8bdd6-643e-420b-ab81-c67021dc7b06', 'd3ac9601-00ad-4ebc-84f4-c9e1ec77fefd', 'Bài 2.2: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 48, 2),
    ('ceb1a13f-17db-4c2e-b251-41948b63560c', 'd3ac9601-00ad-4ebc-84f4-c9e1ec77fefd', 'Bài 2.3: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 27, 3),
    ('3465ace7-e8e8-4eea-a08d-c0797e18e0c1', '668e86c1-b934-4ebc-a1c1-b67b91f0d826', 'Bài 3.1: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 41, 1),
    ('862f9800-f034-463f-9c7c-f2731d055594', '668e86c1-b934-4ebc-a1c1-b67b91f0d826', 'Bài 3.2: Thực chiến NoSQL với MongoDB & Caching tốc độ cao Redis', 27, 2),
    ('4d237ab0-ae76-4ad4-b9fc-c8535c94218a', '1ea92a9f-d2cb-433a-9b13-69540918b946', 'Bài 1.1: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 27, 1),
    ('d1f6b1ed-8d95-4367-b4c4-86ee21d485ad', '1ea92a9f-d2cb-433a-9b13-69540918b946', 'Bài 1.2: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 34, 2),
    ('7f14e31f-29df-4e91-b66d-3ff72c935390', '1ea92a9f-d2cb-433a-9b13-69540918b946', 'Bài 1.3: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 41, 3),
    ('242ac707-ffe6-4922-8b94-d13c092e4d52', '3b8a2657-e9ba-4900-934d-e16838a4164f', 'Bài 2.1: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 34, 1),
    ('294992d4-0793-4f11-8980-d12d7dfe418f', '3b8a2657-e9ba-4900-934d-e16838a4164f', 'Bài 2.2: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 48, 2),
    ('74b280f1-fb19-493e-8664-5c81963d98e6', '3b8a2657-e9ba-4900-934d-e16838a4164f', 'Bài 2.3: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 27, 3),
    ('c32bdf7f-d94d-47b0-a8a4-330d91d610d4', '2e85b554-b20e-4c51-b3d4-abb5707598c5', 'Bài 3.1: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 41, 1),
    ('a719c9ae-fdcd-4b14-ae6d-f36505c60cf3', '2e85b554-b20e-4c51-b3d4-abb5707598c5', 'Bài 3.2: Thực chiến Kiểm thử tự động & Chất lượng phần mềm', 27, 2),
    ('e893031b-b70e-42ad-b73b-5eea4fa8990c', '16215a7f-8ef4-4cee-a9a6-3ba7869c66ee', 'Bài 1.1: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 27, 1),
    ('031b70bb-b442-4ed0-a907-9ee058553d21', '16215a7f-8ef4-4cee-a9a6-3ba7869c66ee', 'Bài 1.2: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 34, 2),
    ('3d29fb9e-7ad1-4806-b2c3-fd5dbdf472ce', '16215a7f-8ef4-4cee-a9a6-3ba7869c66ee', 'Bài 1.3: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 41, 3),
    ('38d704a1-a04a-4786-adfb-2b91242f2d87', '61c220d9-2617-4513-b66a-391734b2f25c', 'Bài 2.1: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 34, 1),
    ('d133f90c-c0a4-43d9-8511-3b69e971fa64', '61c220d9-2617-4513-b66a-391734b2f25c', 'Bài 2.2: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 48, 2),
    ('4e42c251-c64c-4945-ae4c-dc29528b7ffb', '61c220d9-2617-4513-b66a-391734b2f25c', 'Bài 2.3: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 27, 3),
    ('618a6a17-f361-41d2-b73d-47afb8da8b85', 'e5a1beef-f356-4157-95d9-16f2419d1c58', 'Bài 3.1: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 41, 1),
    ('7b8296f7-e140-49df-8133-dc6e48a3acf2', 'e5a1beef-f356-4157-95d9-16f2419d1c58', 'Bài 3.2: Thực chiến Kỹ năng Giao tiếp, Làm việc nhóm & Phỏng vấn ', 27, 2)
ON CONFLICT (module_id, order_index) DO NOTHING;

-- 5. 40 Candidate Targets & 120 Gap Results
INSERT INTO candidate_targets (user_id, target_type, position_id, job_id, chosen_at) VALUES
    ('ed854c07-f3eb-48f1-a186-4e7135f54645', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '20 days'),
    ('797c5663-386b-4e96-9112-6d3e414b4449', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '19 days'),
    ('4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '18 days'),
    ('f848319f-9e6e-4815-a697-2fe2400fd32f', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '17 days'),
    ('6257a31f-e618-4ec1-ad25-67574b41a04e', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '16 days'),
    ('c633fa6f-51e7-4257-a13b-9bc051acab55', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '15 days'),
    ('41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '14 days'),
    ('7d26abe3-98c9-4af8-a904-e57f81783d54', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '13 days'),
    ('6f7d791c-8ef0-4404-ab53-5871eb562621', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '12 days'),
    ('1c1cae0c-2691-4876-a05e-e1c133a1f013', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '11 days'),
    ('84e1f7fb-48da-480c-ab2a-799f6d931982', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '10 days'),
    ('47687ff7-7211-405f-a081-230fad46f127', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '9 days'),
    ('a3692669-b304-40c2-a999-50695e5fedb6', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '20 days'),
    ('f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '19 days'),
    ('988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '18 days'),
    ('56de20bd-ad10-4cd8-a732-7d41b40b6734', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '17 days'),
    ('7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '16 days'),
    ('984682cd-5097-4e07-aea4-7f6e423a80d5', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '15 days'),
    ('0539425d-c9ea-42ad-a526-af16615ee51e', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '14 days'),
    ('4b20ea51-eba8-4087-a312-b142beefc5b7', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '13 days'),
    ('0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '12 days'),
    ('550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '11 days'),
    ('315a14b5-f83c-42bc-a038-39cf8340bf3a', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '10 days'),
    ('be569c29-d7e0-482e-a485-654f2511098e', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '9 days'),
    ('85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '20 days'),
    ('33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '19 days'),
    ('03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '18 days'),
    ('e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '17 days'),
    ('26dc3569-5b76-4c65-aaf9-eafcc3fd2412', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '16 days'),
    ('5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '15 days'),
    ('98da37df-6ea3-439a-a76b-356a87e3c39b', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '14 days'),
    ('55c98170-fe5f-49bf-af88-812dc97231fa', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '13 days'),
    ('a68f2877-ef24-461c-abbf-090bb2a7c579', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '12 days'),
    ('f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '11 days'),
    ('79518343-7d11-463f-a6ef-b199d83106da', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '10 days'),
    ('f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '9 days'),
    ('e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'POSITION', '11111111-1111-1111-1111-111111111101', NULL, NOW() - interval '20 days'),
    ('754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', 'POSITION', '11111111-1111-1111-1111-111111111102', NULL, NOW() - interval '19 days'),
    ('172c2f01-2008-4e41-a7cb-12a83c6fd75f', 'POSITION', '11111111-1111-1111-1111-111111111103', NULL, NOW() - interval '18 days'),
    ('1a56c767-f6f9-4bd7-aced-b8edca9dffbb', 'POSITION', '11111111-1111-1111-1111-111111111104', NULL, NOW() - interval '17 days')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO gap_results (id, user_id, target_type, target_id, skill_id, skill_name, required, current, gap, priority, computed_at) VALUES
    ('1c6548c0-5856-4991-a754-59c63f9110b3', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'POSITION', '11111111-1111-1111-1111-111111111101', 'JAVA_CORE', 'Java Core', 80, 55, 25, 'HIGH', NOW() - interval '5 days'),
    ('469a5572-db21-4c5b-8863-de747189ca2f', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SPRING_BOOT', 'Spring Boot', 80, 66, 14, 'MEDIUM', NOW() - interval '5 days'),
    ('d9771ef8-0946-452f-aa36-48cc21520da6', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SQL', 'SQL', 80, 77, 3, 'LOW', NOW() - interval '5 days'),
    ('5cc88c61-b6c2-4f98-b495-0672f0d8c197', '797c5663-386b-4e96-9112-6d3e414b4449', 'POSITION', '11111111-1111-1111-1111-111111111102', 'REACT', 'React', 80, 62, 18, 'MEDIUM', NOW() - interval '5 days'),
    ('0a5ec002-6caf-42e4-86f4-3ed886dd9c90', '797c5663-386b-4e96-9112-6d3e414b4449', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TYPESCRIPT', 'TypeScript', 80, 73, 7, 'LOW', NOW() - interval '5 days'),
    ('8363c21b-2ba2-4ed8-9497-a04b5eea5f8e', '797c5663-386b-4e96-9112-6d3e414b4449', 'POSITION', '11111111-1111-1111-1111-111111111102', 'JAVASCRIPT', 'JavaScript', 80, 84, 0, 'LOW', NOW() - interval '5 days'),
    ('8aa93e8d-530a-4b88-ab28-be4a2204bc43', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'POSITION', '11111111-1111-1111-1111-111111111103', 'JAVA_CORE', 'Java Core', 80, 69, 11, 'MEDIUM', NOW() - interval '5 days'),
    ('2f89d516-9298-420f-95e8-cdfe27b5fd18', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'POSITION', '11111111-1111-1111-1111-111111111103', 'SPRING_BOOT', 'Spring Boot', 80, 80, 0, 'LOW', NOW() - interval '5 days'),
    ('ae017748-7855-4c57-acd4-0d73c209789d', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'POSITION', '11111111-1111-1111-1111-111111111103', 'REACT', 'React', 80, 61, 19, 'MEDIUM', NOW() - interval '5 days'),
    ('e9c57b1b-3967-46b6-bd35-e5211afc81b4', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 'POSITION', '11111111-1111-1111-1111-111111111104', 'PYTHON', 'Python', 80, 76, 4, 'LOW', NOW() - interval '5 days'),
    ('51c89e73-5180-48fa-b7c7-a9504d3326c7', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 'POSITION', '11111111-1111-1111-1111-111111111104', 'MACHINE_LEARNING', 'Machine Learning', 80, 57, 23, 'HIGH', NOW() - interval '5 days'),
    ('8b123b69-0fd3-4a34-94f2-cb164df2b90f', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 'POSITION', '11111111-1111-1111-1111-111111111104', 'DATA_ANALYSIS', 'Data Analysis', 80, 68, 12, 'MEDIUM', NOW() - interval '5 days'),
    ('f32ec41d-a9c5-4cef-8d22-26fe923994b5', '6257a31f-e618-4ec1-ad25-67574b41a04e', 'POSITION', '11111111-1111-1111-1111-111111111101', 'FLUTTER', 'Flutter', 80, 83, 0, 'LOW', NOW() - interval '5 days'),
    ('fae07238-8535-4c2d-9f88-1da964d3dd01', '6257a31f-e618-4ec1-ad25-67574b41a04e', 'POSITION', '11111111-1111-1111-1111-111111111101', 'REACT_NATIVE', 'React Native', 80, 64, 16, 'MEDIUM', NOW() - interval '5 days'),
    ('f87f5178-7373-4799-aa68-8cbf514fc2c1', '6257a31f-e618-4ec1-ad25-67574b41a04e', 'POSITION', '11111111-1111-1111-1111-111111111101', 'TYPESCRIPT', 'TypeScript', 80, 75, 5, 'LOW', NOW() - interval '5 days'),
    ('851d002e-9c78-4550-b58b-888359371077', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TESTING', 'Testing', 80, 60, 20, 'HIGH', NOW() - interval '5 days'),
    ('8fb9cb25-8fc9-47bf-9ea3-7e1edf25ad92', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 'POSITION', '11111111-1111-1111-1111-111111111102', 'SELENIUM', 'Selenium', 80, 71, 9, 'LOW', NOW() - interval '5 days'),
    ('07fbbcec-fa9c-494c-9690-33107b7a56fb', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 'POSITION', '11111111-1111-1111-1111-111111111102', 'PLAYWRIGHT', 'Playwright', 80, 82, 0, 'LOW', NOW() - interval '5 days'),
    ('3beb6746-d4a2-49ff-bad8-1cc517cfe4ed', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'POSITION', '11111111-1111-1111-1111-111111111103', 'DOCKER', 'Docker', 80, 67, 13, 'MEDIUM', NOW() - interval '5 days'),
    ('ea7142ba-72f0-4085-9a54-b8b61e5111b4', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'POSITION', '11111111-1111-1111-1111-111111111103', 'KUBERNETES', 'Kubernetes', 80, 78, 2, 'LOW', NOW() - interval '5 days'),
    ('ca433c68-0ab5-4a8f-ad02-99092a68b0c4', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'POSITION', '11111111-1111-1111-1111-111111111103', 'AWS', 'AWS Cloud', 80, 59, 21, 'HIGH', NOW() - interval '5 days'),
    ('e20c616f-7fad-447f-8988-7cd9b78ef124', '7d26abe3-98c9-4af8-a904-e57f81783d54', 'POSITION', '11111111-1111-1111-1111-111111111104', 'GOLANG', 'Golang', 80, 74, 6, 'LOW', NOW() - interval '5 days'),
    ('808192c5-9fdc-4a20-b9f5-b0df0fea10c0', '7d26abe3-98c9-4af8-a904-e57f81783d54', 'POSITION', '11111111-1111-1111-1111-111111111104', 'POSTGRESQL', 'PostgreSQL', 80, 55, 25, 'HIGH', NOW() - interval '5 days'),
    ('2a26a027-b31e-441e-a2b4-4b945c57bc65', '7d26abe3-98c9-4af8-a904-e57f81783d54', 'POSITION', '11111111-1111-1111-1111-111111111104', 'REDIS', 'Redis', 80, 66, 14, 'MEDIUM', NOW() - interval '5 days'),
    ('073a7d0e-97e0-4767-afc6-e37e2ded0602', '6f7d791c-8ef0-4404-ab53-5871eb562621', 'POSITION', '11111111-1111-1111-1111-111111111101', 'JAVA_CORE', 'Java Core', 80, 81, 0, 'LOW', NOW() - interval '5 days'),
    ('922fcbed-d206-4c20-87a8-f5ddb01e23db', '6f7d791c-8ef0-4404-ab53-5871eb562621', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SPRING_BOOT', 'Spring Boot', 80, 62, 18, 'MEDIUM', NOW() - interval '5 days'),
    ('949700c0-39c9-4e9c-ad2a-f0a6daa638c0', '6f7d791c-8ef0-4404-ab53-5871eb562621', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SQL', 'SQL', 80, 73, 7, 'LOW', NOW() - interval '5 days'),
    ('e5a3dc77-78aa-4f13-8f8b-ad48c88538c5', '1c1cae0c-2691-4876-a05e-e1c133a1f013', 'POSITION', '11111111-1111-1111-1111-111111111102', 'REACT', 'React', 80, 58, 22, 'HIGH', NOW() - interval '5 days'),
    ('bf3abd3e-4344-46fa-b9b1-c3fb885519b0', '1c1cae0c-2691-4876-a05e-e1c133a1f013', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TYPESCRIPT', 'TypeScript', 80, 69, 11, 'MEDIUM', NOW() - interval '5 days'),
    ('5141dc0b-37b3-4ed3-9530-9e87469ca83e', '1c1cae0c-2691-4876-a05e-e1c133a1f013', 'POSITION', '11111111-1111-1111-1111-111111111102', 'JAVASCRIPT', 'JavaScript', 80, 80, 0, 'LOW', NOW() - interval '5 days'),
    ('9305b670-5b05-4bcb-92bd-318684f94fda', '84e1f7fb-48da-480c-ab2a-799f6d931982', 'POSITION', '11111111-1111-1111-1111-111111111103', 'JAVA_CORE', 'Java Core', 80, 65, 15, 'MEDIUM', NOW() - interval '5 days'),
    ('0a781763-46e3-4bda-bd3f-ac528cd7c10a', '84e1f7fb-48da-480c-ab2a-799f6d931982', 'POSITION', '11111111-1111-1111-1111-111111111103', 'SPRING_BOOT', 'Spring Boot', 80, 76, 4, 'LOW', NOW() - interval '5 days'),
    ('0e639f69-63ef-49e1-ad3b-32fd16144512', '84e1f7fb-48da-480c-ab2a-799f6d931982', 'POSITION', '11111111-1111-1111-1111-111111111103', 'REACT', 'React', 80, 57, 23, 'HIGH', NOW() - interval '5 days'),
    ('282d4c7d-0ef4-411d-8aaa-6648555e802f', '47687ff7-7211-405f-a081-230fad46f127', 'POSITION', '11111111-1111-1111-1111-111111111104', 'PYTHON', 'Python', 80, 72, 8, 'LOW', NOW() - interval '5 days'),
    ('c37fe1f3-f0b2-4d21-bcb5-5f94397ae1c1', '47687ff7-7211-405f-a081-230fad46f127', 'POSITION', '11111111-1111-1111-1111-111111111104', 'MACHINE_LEARNING', 'Machine Learning', 80, 83, 0, 'LOW', NOW() - interval '5 days'),
    ('cf9da40a-2350-43f7-b1e6-0565781ae414', '47687ff7-7211-405f-a081-230fad46f127', 'POSITION', '11111111-1111-1111-1111-111111111104', 'DATA_ANALYSIS', 'Data Analysis', 80, 64, 16, 'MEDIUM', NOW() - interval '5 days'),
    ('b4b8f65b-0fa2-4dc0-9199-0474f6ab76c5', 'a3692669-b304-40c2-a999-50695e5fedb6', 'POSITION', '11111111-1111-1111-1111-111111111101', 'FLUTTER', 'Flutter', 80, 79, 1, 'LOW', NOW() - interval '5 days'),
    ('ef7664c4-c301-4869-a1b9-5586a3ad4615', 'a3692669-b304-40c2-a999-50695e5fedb6', 'POSITION', '11111111-1111-1111-1111-111111111101', 'REACT_NATIVE', 'React Native', 80, 60, 20, 'HIGH', NOW() - interval '5 days'),
    ('6db36181-c95e-460f-8efe-360bd84bd0e2', 'a3692669-b304-40c2-a999-50695e5fedb6', 'POSITION', '11111111-1111-1111-1111-111111111101', 'TYPESCRIPT', 'TypeScript', 80, 71, 9, 'LOW', NOW() - interval '5 days'),
    ('f75dae0a-daff-4849-a762-7a1f3e8b0c88', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TESTING', 'Testing', 80, 56, 24, 'HIGH', NOW() - interval '5 days'),
    ('a9ec726b-94a2-48da-bafd-2de021d308db', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'SELENIUM', 'Selenium', 80, 67, 13, 'MEDIUM', NOW() - interval '5 days'),
    ('e74faca7-e35c-4e6f-9d73-609165fa3901', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'PLAYWRIGHT', 'Playwright', 80, 78, 2, 'LOW', NOW() - interval '5 days'),
    ('e5498e43-4530-4567-a547-60e45c956c33', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'POSITION', '11111111-1111-1111-1111-111111111103', 'DOCKER', 'Docker', 80, 63, 17, 'MEDIUM', NOW() - interval '5 days'),
    ('91615d28-3078-4071-a7b1-025ad9230b40', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'POSITION', '11111111-1111-1111-1111-111111111103', 'KUBERNETES', 'Kubernetes', 80, 74, 6, 'LOW', NOW() - interval '5 days'),
    ('812f3f89-756a-4b98-8393-08f893ce1993', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'POSITION', '11111111-1111-1111-1111-111111111103', 'AWS', 'AWS Cloud', 80, 55, 25, 'HIGH', NOW() - interval '5 days'),
    ('842355ff-8986-4c65-a11e-82b53a9b6c8b', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 'POSITION', '11111111-1111-1111-1111-111111111104', 'GOLANG', 'Golang', 80, 70, 10, 'MEDIUM', NOW() - interval '5 days'),
    ('0b59e08f-8f99-4312-9ba8-6bd0219b0a06', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 'POSITION', '11111111-1111-1111-1111-111111111104', 'POSTGRESQL', 'PostgreSQL', 80, 81, 0, 'LOW', NOW() - interval '5 days'),
    ('dc5f507e-0bb0-472e-b848-df5914888359', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 'POSITION', '11111111-1111-1111-1111-111111111104', 'REDIS', 'Redis', 80, 62, 18, 'MEDIUM', NOW() - interval '5 days'),
    ('aed2fe38-e276-478a-8c92-a382882ec2ab', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'POSITION', '11111111-1111-1111-1111-111111111101', 'JAVA_CORE', 'Java Core', 80, 77, 3, 'LOW', NOW() - interval '5 days'),
    ('8e23af4e-f362-4ef7-b86d-fcc4b5bf343b', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SPRING_BOOT', 'Spring Boot', 80, 58, 22, 'HIGH', NOW() - interval '5 days'),
    ('2b43b771-1b78-4f4b-87f6-469ec6391e33', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SQL', 'SQL', 80, 69, 11, 'MEDIUM', NOW() - interval '5 days'),
    ('9e4dfcb5-9e9b-4f87-8438-c5e62b762db1', '984682cd-5097-4e07-aea4-7f6e423a80d5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'REACT', 'React', 80, 84, 0, 'LOW', NOW() - interval '5 days'),
    ('99b7c63c-e407-4a69-9e15-4196a0d34133', '984682cd-5097-4e07-aea4-7f6e423a80d5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TYPESCRIPT', 'TypeScript', 80, 65, 15, 'MEDIUM', NOW() - interval '5 days'),
    ('d4f0fc99-c3b2-4919-9e16-7d9ad1710f35', '984682cd-5097-4e07-aea4-7f6e423a80d5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'JAVASCRIPT', 'JavaScript', 80, 76, 4, 'LOW', NOW() - interval '5 days'),
    ('ca70b1fc-a54c-4659-bb0b-82816956e01c', '0539425d-c9ea-42ad-a526-af16615ee51e', 'POSITION', '11111111-1111-1111-1111-111111111103', 'JAVA_CORE', 'Java Core', 80, 61, 19, 'MEDIUM', NOW() - interval '5 days'),
    ('aa316a2b-26b4-4108-a4b3-9be241984e1c', '0539425d-c9ea-42ad-a526-af16615ee51e', 'POSITION', '11111111-1111-1111-1111-111111111103', 'SPRING_BOOT', 'Spring Boot', 80, 72, 8, 'LOW', NOW() - interval '5 days'),
    ('1c26c8c2-79dc-4e3c-8ae7-a5b044bc4a1f', '0539425d-c9ea-42ad-a526-af16615ee51e', 'POSITION', '11111111-1111-1111-1111-111111111103', 'REACT', 'React', 80, 83, 0, 'LOW', NOW() - interval '5 days'),
    ('854a6ed7-243c-4d0e-8f5e-e8c7a14e6e10', '4b20ea51-eba8-4087-a312-b142beefc5b7', 'POSITION', '11111111-1111-1111-1111-111111111104', 'PYTHON', 'Python', 80, 68, 12, 'MEDIUM', NOW() - interval '5 days'),
    ('50fea642-31ad-4ef7-8ef9-599972f57b3f', '4b20ea51-eba8-4087-a312-b142beefc5b7', 'POSITION', '11111111-1111-1111-1111-111111111104', 'MACHINE_LEARNING', 'Machine Learning', 80, 79, 1, 'LOW', NOW() - interval '5 days'),
    ('9a617012-5d75-4851-945f-fba3f30ec4da', '4b20ea51-eba8-4087-a312-b142beefc5b7', 'POSITION', '11111111-1111-1111-1111-111111111104', 'DATA_ANALYSIS', 'Data Analysis', 80, 60, 20, 'HIGH', NOW() - interval '5 days'),
    ('9bf379d9-fc99-4cfa-81ae-0007612a3740', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'POSITION', '11111111-1111-1111-1111-111111111101', 'FLUTTER', 'Flutter', 80, 75, 5, 'LOW', NOW() - interval '5 days'),
    ('b87a609d-cf62-4479-b9d9-932e91fe69eb', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'POSITION', '11111111-1111-1111-1111-111111111101', 'REACT_NATIVE', 'React Native', 80, 56, 24, 'HIGH', NOW() - interval '5 days'),
    ('04b45c53-978a-40ce-9326-87e155348eac', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'POSITION', '11111111-1111-1111-1111-111111111101', 'TYPESCRIPT', 'TypeScript', 80, 67, 13, 'MEDIUM', NOW() - interval '5 days'),
    ('2be6b617-8f22-494f-b0fd-39c214c006de', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TESTING', 'Testing', 80, 82, 0, 'LOW', NOW() - interval '5 days'),
    ('62dad5ce-a292-4137-888e-423c56772c7a', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'POSITION', '11111111-1111-1111-1111-111111111102', 'SELENIUM', 'Selenium', 80, 63, 17, 'MEDIUM', NOW() - interval '5 days'),
    ('8c85542f-6617-48d2-b88a-809be916dd0d', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'POSITION', '11111111-1111-1111-1111-111111111102', 'PLAYWRIGHT', 'Playwright', 80, 74, 6, 'LOW', NOW() - interval '5 days'),
    ('cdc74dd1-f968-429f-8088-b4fd192b81a9', '315a14b5-f83c-42bc-a038-39cf8340bf3a', 'POSITION', '11111111-1111-1111-1111-111111111103', 'DOCKER', 'Docker', 80, 59, 21, 'HIGH', NOW() - interval '5 days'),
    ('b2685cc4-cb3a-46a9-822b-9654e1dbe02e', '315a14b5-f83c-42bc-a038-39cf8340bf3a', 'POSITION', '11111111-1111-1111-1111-111111111103', 'KUBERNETES', 'Kubernetes', 80, 70, 10, 'MEDIUM', NOW() - interval '5 days'),
    ('693f3506-5250-484c-b7ca-b70cd7958247', '315a14b5-f83c-42bc-a038-39cf8340bf3a', 'POSITION', '11111111-1111-1111-1111-111111111103', 'AWS', 'AWS Cloud', 80, 81, 0, 'LOW', NOW() - interval '5 days'),
    ('1f430c82-e6c9-4fbf-b708-684df2b1a411', 'be569c29-d7e0-482e-a485-654f2511098e', 'POSITION', '11111111-1111-1111-1111-111111111104', 'GOLANG', 'Golang', 80, 66, 14, 'MEDIUM', NOW() - interval '5 days'),
    ('2e646f3a-ffe2-4567-acac-81de107bc856', 'be569c29-d7e0-482e-a485-654f2511098e', 'POSITION', '11111111-1111-1111-1111-111111111104', 'POSTGRESQL', 'PostgreSQL', 80, 77, 3, 'LOW', NOW() - interval '5 days'),
    ('7f9638be-489e-4905-ac23-32a2a59fb372', 'be569c29-d7e0-482e-a485-654f2511098e', 'POSITION', '11111111-1111-1111-1111-111111111104', 'REDIS', 'Redis', 80, 58, 22, 'HIGH', NOW() - interval '5 days'),
    ('efd0f6c1-c014-4224-b9c4-1ea1c6cb6706', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'POSITION', '11111111-1111-1111-1111-111111111101', 'JAVA_CORE', 'Java Core', 80, 73, 7, 'LOW', NOW() - interval '5 days'),
    ('3fb722ad-9e2a-435a-8e89-80bbef5de672', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SPRING_BOOT', 'Spring Boot', 80, 84, 0, 'LOW', NOW() - interval '5 days'),
    ('98c303ed-08cc-4cb0-8503-4c2abdaee483', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SQL', 'SQL', 80, 65, 15, 'MEDIUM', NOW() - interval '5 days'),
    ('1c95c277-1cdc-40a9-8d0c-110d60c4f25e', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'POSITION', '11111111-1111-1111-1111-111111111102', 'REACT', 'React', 80, 80, 0, 'LOW', NOW() - interval '5 days'),
    ('b08acf42-d172-4f32-8d1c-1c1386c8370d', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TYPESCRIPT', 'TypeScript', 80, 61, 19, 'MEDIUM', NOW() - interval '5 days'),
    ('39fb2cfd-3d6d-4cfd-a4b5-af840987b27e', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'POSITION', '11111111-1111-1111-1111-111111111102', 'JAVASCRIPT', 'JavaScript', 80, 72, 8, 'LOW', NOW() - interval '5 days'),
    ('a94a6ee1-46a4-45a4-8020-a11d8d6cf368', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'POSITION', '11111111-1111-1111-1111-111111111103', 'JAVA_CORE', 'Java Core', 80, 57, 23, 'HIGH', NOW() - interval '5 days'),
    ('f6fc1bd0-fbab-4bf1-b290-5f28126b7285', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'POSITION', '11111111-1111-1111-1111-111111111103', 'SPRING_BOOT', 'Spring Boot', 80, 68, 12, 'MEDIUM', NOW() - interval '5 days'),
    ('4da5a8e3-edfe-4afb-876e-36f8ce0187b4', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'POSITION', '11111111-1111-1111-1111-111111111103', 'REACT', 'React', 80, 79, 1, 'LOW', NOW() - interval '5 days'),
    ('d8073ab5-c51e-452e-ad8c-c340845b50a5', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'POSITION', '11111111-1111-1111-1111-111111111104', 'PYTHON', 'Python', 80, 64, 16, 'MEDIUM', NOW() - interval '5 days'),
    ('e62d1ef2-c427-43f9-a204-37cfdb47a997', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'POSITION', '11111111-1111-1111-1111-111111111104', 'MACHINE_LEARNING', 'Machine Learning', 80, 75, 5, 'LOW', NOW() - interval '5 days'),
    ('af9b3fd4-27a1-42c8-a9c9-d565d6947e66', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'POSITION', '11111111-1111-1111-1111-111111111104', 'DATA_ANALYSIS', 'Data Analysis', 80, 56, 24, 'HIGH', NOW() - interval '5 days'),
    ('74b9da56-1615-475c-b898-1d6641f54449', '26dc3569-5b76-4c65-aaf9-eafcc3fd2412', 'POSITION', '11111111-1111-1111-1111-111111111101', 'FLUTTER', 'Flutter', 80, 71, 9, 'LOW', NOW() - interval '5 days'),
    ('b7ecebdf-ae30-494b-8b09-d0807ab2cd1a', '26dc3569-5b76-4c65-aaf9-eafcc3fd2412', 'POSITION', '11111111-1111-1111-1111-111111111101', 'REACT_NATIVE', 'React Native', 80, 82, 0, 'LOW', NOW() - interval '5 days'),
    ('99dda2d1-a55c-4c79-b6a0-f9c47cee8a57', '26dc3569-5b76-4c65-aaf9-eafcc3fd2412', 'POSITION', '11111111-1111-1111-1111-111111111101', 'TYPESCRIPT', 'TypeScript', 80, 63, 17, 'MEDIUM', NOW() - interval '5 days'),
    ('66aacfa8-2d98-4de6-909b-4979707b3a66', '5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TESTING', 'Testing', 80, 78, 2, 'LOW', NOW() - interval '5 days'),
    ('b0ea23e0-7f08-46c0-b461-e19aade4432b', '5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', 'POSITION', '11111111-1111-1111-1111-111111111102', 'SELENIUM', 'Selenium', 80, 59, 21, 'HIGH', NOW() - interval '5 days'),
    ('7f22e2bb-454f-463d-8707-85c5727b5bc4', '5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', 'POSITION', '11111111-1111-1111-1111-111111111102', 'PLAYWRIGHT', 'Playwright', 80, 70, 10, 'MEDIUM', NOW() - interval '5 days'),
    ('c94d73f9-4dd8-42ff-a2dd-e5671dcc3f8d', '98da37df-6ea3-439a-a76b-356a87e3c39b', 'POSITION', '11111111-1111-1111-1111-111111111103', 'DOCKER', 'Docker', 80, 55, 25, 'HIGH', NOW() - interval '5 days'),
    ('87fa0305-aa7c-48d8-b997-8e60217eae1b', '98da37df-6ea3-439a-a76b-356a87e3c39b', 'POSITION', '11111111-1111-1111-1111-111111111103', 'KUBERNETES', 'Kubernetes', 80, 66, 14, 'MEDIUM', NOW() - interval '5 days'),
    ('397667f6-f8c6-4ddb-9fe5-b899542c9136', '98da37df-6ea3-439a-a76b-356a87e3c39b', 'POSITION', '11111111-1111-1111-1111-111111111103', 'AWS', 'AWS Cloud', 80, 77, 3, 'LOW', NOW() - interval '5 days'),
    ('6a723ca2-5b8b-4a56-9fd3-713e7e0629f6', '55c98170-fe5f-49bf-af88-812dc97231fa', 'POSITION', '11111111-1111-1111-1111-111111111104', 'GOLANG', 'Golang', 80, 62, 18, 'MEDIUM', NOW() - interval '5 days'),
    ('e94a12b9-38fa-49cb-9725-fce202a41f92', '55c98170-fe5f-49bf-af88-812dc97231fa', 'POSITION', '11111111-1111-1111-1111-111111111104', 'POSTGRESQL', 'PostgreSQL', 80, 73, 7, 'LOW', NOW() - interval '5 days'),
    ('f1386137-31d5-4c08-bb53-1be5f4493376', '55c98170-fe5f-49bf-af88-812dc97231fa', 'POSITION', '11111111-1111-1111-1111-111111111104', 'REDIS', 'Redis', 80, 84, 0, 'LOW', NOW() - interval '5 days'),
    ('44d795f1-dfea-4500-9836-674b29bf1000', 'a68f2877-ef24-461c-abbf-090bb2a7c579', 'POSITION', '11111111-1111-1111-1111-111111111101', 'JAVA_CORE', 'Java Core', 80, 69, 11, 'MEDIUM', NOW() - interval '5 days'),
    ('44238f8c-0d02-4785-961d-b5fd9f0674af', 'a68f2877-ef24-461c-abbf-090bb2a7c579', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SPRING_BOOT', 'Spring Boot', 80, 80, 0, 'LOW', NOW() - interval '5 days'),
    ('0d33fc9b-3208-4c3c-8bab-aa141375a5ad', 'a68f2877-ef24-461c-abbf-090bb2a7c579', 'POSITION', '11111111-1111-1111-1111-111111111101', 'SQL', 'SQL', 80, 61, 19, 'MEDIUM', NOW() - interval '5 days'),
    ('73539070-ed30-4143-b116-6b03c890ea9f', 'f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'POSITION', '11111111-1111-1111-1111-111111111102', 'REACT', 'React', 80, 76, 4, 'LOW', NOW() - interval '5 days'),
    ('e6de8486-6114-4987-a2b7-cab3d786dfab', 'f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TYPESCRIPT', 'TypeScript', 80, 57, 23, 'HIGH', NOW() - interval '5 days'),
    ('7618b89f-9737-46cc-aa7e-9c12e4daedd2', 'f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'POSITION', '11111111-1111-1111-1111-111111111102', 'JAVASCRIPT', 'JavaScript', 80, 68, 12, 'MEDIUM', NOW() - interval '5 days'),
    ('23eb73f7-46b1-40bd-b142-47d9109d010b', '79518343-7d11-463f-a6ef-b199d83106da', 'POSITION', '11111111-1111-1111-1111-111111111103', 'JAVA_CORE', 'Java Core', 80, 83, 0, 'LOW', NOW() - interval '5 days'),
    ('7a481f15-9bb9-45bc-a94b-2d948a081d64', '79518343-7d11-463f-a6ef-b199d83106da', 'POSITION', '11111111-1111-1111-1111-111111111103', 'SPRING_BOOT', 'Spring Boot', 80, 64, 16, 'MEDIUM', NOW() - interval '5 days'),
    ('ababa51a-5316-4327-877a-2e8672e3be5a', '79518343-7d11-463f-a6ef-b199d83106da', 'POSITION', '11111111-1111-1111-1111-111111111103', 'REACT', 'React', 80, 75, 5, 'LOW', NOW() - interval '5 days'),
    ('2ddb0f91-384a-4b50-ab1c-9af61a2d2302', 'f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', 'POSITION', '11111111-1111-1111-1111-111111111104', 'PYTHON', 'Python', 80, 60, 20, 'HIGH', NOW() - interval '5 days'),
    ('1d1ca596-bf5b-478d-a38a-2116b5aff0af', 'f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', 'POSITION', '11111111-1111-1111-1111-111111111104', 'MACHINE_LEARNING', 'Machine Learning', 80, 71, 9, 'LOW', NOW() - interval '5 days'),
    ('80ed8552-ca7a-4519-87d5-84b04e3b9c71', 'f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', 'POSITION', '11111111-1111-1111-1111-111111111104', 'DATA_ANALYSIS', 'Data Analysis', 80, 82, 0, 'LOW', NOW() - interval '5 days'),
    ('c2b0811f-ca18-484e-b4b3-da9a03949884', 'e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'POSITION', '11111111-1111-1111-1111-111111111101', 'FLUTTER', 'Flutter', 80, 67, 13, 'MEDIUM', NOW() - interval '5 days'),
    ('ee734875-2070-403e-b1ec-8d1098720e60', 'e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'POSITION', '11111111-1111-1111-1111-111111111101', 'REACT_NATIVE', 'React Native', 80, 78, 2, 'LOW', NOW() - interval '5 days'),
    ('57804052-6921-4c97-abc3-3c1ce9628fb1', 'e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'POSITION', '11111111-1111-1111-1111-111111111101', 'TYPESCRIPT', 'TypeScript', 80, 59, 21, 'HIGH', NOW() - interval '5 days'),
    ('00e31bcf-de5c-4981-bb85-ee7fe1f4774f', '754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'TESTING', 'Testing', 80, 74, 6, 'LOW', NOW() - interval '5 days'),
    ('421a5e0b-9010-4a9a-bd65-23f4a43af68d', '754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'SELENIUM', 'Selenium', 80, 55, 25, 'HIGH', NOW() - interval '5 days'),
    ('4d1de702-d775-4b0e-a2e4-fc2a9281655d', '754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', 'POSITION', '11111111-1111-1111-1111-111111111102', 'PLAYWRIGHT', 'Playwright', 80, 66, 14, 'MEDIUM', NOW() - interval '5 days'),
    ('7c2ff074-f627-4573-992d-41f7780edabc', '172c2f01-2008-4e41-a7cb-12a83c6fd75f', 'POSITION', '11111111-1111-1111-1111-111111111103', 'DOCKER', 'Docker', 80, 81, 0, 'LOW', NOW() - interval '5 days'),
    ('548bd034-eb3e-4bba-90c8-01d813bed415', '172c2f01-2008-4e41-a7cb-12a83c6fd75f', 'POSITION', '11111111-1111-1111-1111-111111111103', 'KUBERNETES', 'Kubernetes', 80, 62, 18, 'MEDIUM', NOW() - interval '5 days'),
    ('dfbc65b5-e4c1-42b9-848e-0690200af4b6', '172c2f01-2008-4e41-a7cb-12a83c6fd75f', 'POSITION', '11111111-1111-1111-1111-111111111103', 'AWS', 'AWS Cloud', 80, 73, 7, 'LOW', NOW() - interval '5 days'),
    ('02ab58e4-dbb8-4683-8bd1-e300c7ab6454', '1a56c767-f6f9-4bd7-aced-b8edca9dffbb', 'POSITION', '11111111-1111-1111-1111-111111111104', 'GOLANG', 'Golang', 80, 58, 22, 'HIGH', NOW() - interval '5 days'),
    ('f341894f-1e56-44e8-a51e-f63820c4b437', '1a56c767-f6f9-4bd7-aced-b8edca9dffbb', 'POSITION', '11111111-1111-1111-1111-111111111104', 'POSTGRESQL', 'PostgreSQL', 80, 69, 11, 'MEDIUM', NOW() - interval '5 days'),
    ('7b0f1704-e9a8-4ed8-adb6-02291b57bcec', '1a56c767-f6f9-4bd7-aced-b8edca9dffbb', 'POSITION', '11111111-1111-1111-1111-111111111104', 'REDIS', 'Redis', 80, 80, 0, 'LOW', NOW() - interval '5 days')
ON CONFLICT (id) DO NOTHING;

-- 6. 68 Enrollments & 176 Learning Progress
INSERT INTO enrollments (id, user_id, lms_course_id, skill_id, status, progress, score, enrolled_at, completed_at) VALUES
    ('8f9cf5ac-0e9a-46cc-ba22-8a4cbfbe19ae', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'CRS-01', 'SPRING_BOOT', 'COMPLETED', 100.00, 80.00, NOW() - interval '25 days', NOW() - interval '1 days'),
    ('1bc3344f-4f1f-4605-b25d-effc6312c3b8', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'CRS-02', 'REACT', 'IN_PROGRESS', 35.00, NULL, NOW() - interval '25 days', NULL),
    ('f8e883fa-a677-429d-8bb2-5f6837ab9af9', '797c5663-386b-4e96-9112-6d3e414b4449', 'CRS-07', 'JAVA_CORE', 'IN_PROGRESS', 48.00, NULL, NOW() - interval '24 days', NULL),
    ('fc75aa3f-70d3-4693-92fe-ad3ee4b8fd46', '797c5663-386b-4e96-9112-6d3e414b4449', 'CRS-11', 'HTML_CSS', 'ENROLLED', 0.00, NULL, NOW() - interval '24 days', NULL),
    ('50fbfd9a-42cd-41cf-8b4b-0331964925d9', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'CRS-08', 'NODEJS', 'ENROLLED', 0.00, NULL, NOW() - interval '23 days', NULL),
    ('6fa5f959-e58a-4e25-a028-c2d0e5c006ce', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'CRS-12', 'JAVASCRIPT', 'IN_PROGRESS', 61.00, NULL, NOW() - interval '23 days', NULL),
    ('88721941-7dd1-4242-8923-9c16b957f067', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 'CRS-09', 'GOLANG', 'IN_PROGRESS', 74.00, NULL, NOW() - interval '22 days', NULL),
    ('59c542c8-2cb4-4028-b2fb-74eefffd4b79', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 'CRS-13', 'TYPESCRIPT', 'COMPLETED', 100.00, 83.00, NOW() - interval '22 days', NOW() - interval '4 days'),
    ('7777d07c-bc74-43f8-98f3-95b837f15494', '6257a31f-e618-4ec1-ad25-67574b41a04e', 'CRS-10', 'APACHE_KAFKA', 'COMPLETED', 100.00, 84.00, NOW() - interval '21 days', NOW() - interval '5 days'),
    ('7bbd698f-d2a0-47ce-9081-bf6b927f2c6e', '6257a31f-e618-4ec1-ad25-67574b41a04e', 'CRS-14', 'VUEJS', 'FAILED', 45.00, 48.00, NOW() - interval '21 days', NULL),
    ('1bc901eb-fbab-4842-b770-ca4fd5f0c221', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 'CRS-02', 'REACT', 'FAILED', 45.00, 48.00, NOW() - interval '20 days', NULL),
    ('76f7577b-a5ea-4b94-a2a2-e2683f813ed9', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 'CRS-04', 'DOCKER', 'COMPLETED', 100.00, 85.00, NOW() - interval '20 days', NOW() - interval '6 days'),
    ('e8a606ed-3193-4a98-854f-cc7bd798eab3', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'CRS-11', 'HTML_CSS', 'COMPLETED', 100.00, 86.00, NOW() - interval '19 days', NOW() - interval '7 days'),
    ('96bf5dc8-ba62-48c3-a021-95406e055127', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'CRS-15', 'KUBERNETES', 'IN_PROGRESS', 58.00, NULL, NOW() - interval '19 days', NULL),
    ('7023c59f-9611-49e5-938e-63a9f470ed16', '7d26abe3-98c9-4af8-a904-e57f81783d54', 'CRS-12', 'JAVASCRIPT', 'IN_PROGRESS', 71.00, NULL, NOW() - interval '18 days', NULL),
    ('d457ab3b-6625-4d7f-ae95-ae495fb74144', '7d26abe3-98c9-4af8-a904-e57f81783d54', 'CRS-16', 'AWS', 'ENROLLED', 0.00, NULL, NOW() - interval '18 days', NULL),
    ('c5e54879-49ad-497b-8096-8932113142f0', '6f7d791c-8ef0-4404-ab53-5871eb562621', 'CRS-13', 'TYPESCRIPT', 'ENROLLED', 0.00, NULL, NOW() - interval '17 days', NULL),
    ('796e2691-4715-47dc-8fc8-959700650de0', '6f7d791c-8ef0-4404-ab53-5871eb562621', 'CRS-03', 'SQL', 'IN_PROGRESS', 84.00, NULL, NOW() - interval '17 days', NULL),
    ('857451f4-8e96-4be6-b74e-f75a2193767f', '1c1cae0c-2691-4876-a05e-e1c133a1f013', 'CRS-14', 'VUEJS', 'IN_PROGRESS', 42.00, NULL, NOW() - interval '16 days', NULL),
    ('41ebdf88-c535-420a-8f3c-fec3190e743c', '1c1cae0c-2691-4876-a05e-e1c133a1f013', 'CRS-17', 'MYSQL', 'COMPLETED', 100.00, 89.00, NOW() - interval '16 days', NOW() - interval '10 days'),
    ('e3971adc-840b-4b62-8f67-e87c1d2cbf12', '84e1f7fb-48da-480c-ab2a-799f6d931982', 'CRS-04', 'DOCKER', 'COMPLETED', 100.00, 90.00, NOW() - interval '15 days', NOW() - interval '1 days'),
    ('f8f62590-f518-4863-91f8-6f594157efe8', '84e1f7fb-48da-480c-ab2a-799f6d931982', 'CRS-18', 'MONGODB', 'FAILED', 45.00, 48.00, NOW() - interval '15 days', NULL),
    ('871944f7-35c7-4223-bdef-11ff606bc345', '47687ff7-7211-405f-a081-230fad46f127', 'CRS-15', 'KUBERNETES', 'FAILED', 45.00, 48.00, NOW() - interval '14 days', NULL),
    ('eba2e097-f2d6-4815-a7c0-d71c14999a64', '47687ff7-7211-405f-a081-230fad46f127', 'CRS-05', 'TESTING', 'COMPLETED', 100.00, 91.00, NOW() - interval '14 days', NOW() - interval '2 days'),
    ('69b8875a-c57e-473d-84ff-a1ec031dfb97', 'a3692669-b304-40c2-a999-50695e5fedb6', 'CRS-16', 'AWS', 'COMPLETED', 100.00, 92.00, NOW() - interval '13 days', NOW() - interval '3 days'),
    ('9f933fb7-a59d-4191-9f46-4b1ca3b22711', 'a3692669-b304-40c2-a999-50695e5fedb6', 'CRS-19', 'COMMUNICATION', 'IN_PROGRESS', 81.00, NULL, NOW() - interval '13 days', NULL),
    ('d8e0f151-65bf-4bf2-a59f-3ebecc6ca803', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'CRS-03', 'SQL', 'IN_PROGRESS', 39.00, NULL, NOW() - interval '12 days', NULL),
    ('7e616969-63d7-4433-93b3-8327a62c4568', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'CRS-01', 'SPRING_BOOT', 'ENROLLED', 0.00, NULL, NOW() - interval '12 days', NULL),
    ('a3870f7b-9c02-4e14-8316-f369ec6279db', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'CRS-17', 'MYSQL', 'ENROLLED', 0.00, NULL, NOW() - interval '11 days', NULL),
    ('3ce0abed-1565-4920-a6af-61bfa469edc8', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'CRS-07', 'JAVA_CORE', 'IN_PROGRESS', 52.00, NULL, NOW() - interval '11 days', NULL),
    ('1f4061ae-989d-4373-9d6d-c8fd57a6234d', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 'CRS-18', 'MONGODB', 'IN_PROGRESS', 65.00, NULL, NOW() - interval '25 days', NULL),
    ('fc442f61-d0d5-4195-993d-048914612875', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 'CRS-08', 'NODEJS', 'COMPLETED', 100.00, 95.00, NOW() - interval '25 days', NOW() - interval '6 days'),
    ('852193a6-4517-4869-83de-76bbbe610a20', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'CRS-05', 'TESTING', 'COMPLETED', 100.00, 96.00, NOW() - interval '24 days', NOW() - interval '7 days'),
    ('50b4ac3e-d4a7-4386-ad73-53c0884add3a', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'CRS-09', 'GOLANG', 'FAILED', 45.00, 48.00, NOW() - interval '24 days', NULL),
    ('88c832f6-d0b3-47dc-b8af-1d55207fdd47', '984682cd-5097-4e07-aea4-7f6e423a80d5', 'CRS-19', 'COMMUNICATION', 'FAILED', 45.00, 48.00, NOW() - interval '23 days', NULL),
    ('4fcb2dde-fc91-41f5-a4a3-57f09de985b3', '984682cd-5097-4e07-aea4-7f6e423a80d5', 'CRS-10', 'APACHE_KAFKA', 'COMPLETED', 100.00, 97.00, NOW() - interval '23 days', NOW() - interval '8 days'),
    ('55b321c6-6868-40a2-a251-a9efc5c0ca97', '0539425d-c9ea-42ad-a526-af16615ee51e', 'CRS-01', 'SPRING_BOOT', 'COMPLETED', 100.00, 80.00, NOW() - interval '22 days', NOW() - interval '9 days'),
    ('73ea1a1e-693b-4ca2-b664-3611500ce61f', '0539425d-c9ea-42ad-a526-af16615ee51e', 'CRS-02', 'REACT', 'IN_PROGRESS', 49.00, NULL, NOW() - interval '22 days', NULL),
    ('7675c702-c767-4ea8-9d2a-8f15510651e7', '4b20ea51-eba8-4087-a312-b142beefc5b7', 'CRS-07', 'JAVA_CORE', 'IN_PROGRESS', 62.00, NULL, NOW() - interval '21 days', NULL),
    ('d126be9a-2ebd-4f95-9bae-ded93d086305', '4b20ea51-eba8-4087-a312-b142beefc5b7', 'CRS-11', 'HTML_CSS', 'ENROLLED', 0.00, NULL, NOW() - interval '21 days', NULL),
    ('88317c4b-b932-4504-8efd-a9edbccf6274', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'CRS-08', 'NODEJS', 'ENROLLED', 0.00, NULL, NOW() - interval '20 days', NULL),
    ('b781ae52-ab27-4cbd-9339-b4309805da21', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'CRS-12', 'JAVASCRIPT', 'IN_PROGRESS', 75.00, NULL, NOW() - interval '20 days', NULL),
    ('c1fb6a1a-8865-41ab-9671-0fbf3fb9eda0', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'CRS-09', 'GOLANG', 'IN_PROGRESS', 88.00, NULL, NOW() - interval '19 days', NULL),
    ('de86c58e-e7d3-45c4-9ccb-a238d1c4d1c8', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'CRS-13', 'TYPESCRIPT', 'COMPLETED', 100.00, 83.00, NOW() - interval '19 days', NOW() - interval '2 days'),
    ('c3c286a8-e7db-4292-b8e4-c2e627dd7680', '315a14b5-f83c-42bc-a038-39cf8340bf3a', 'CRS-10', 'APACHE_KAFKA', 'COMPLETED', 100.00, 84.00, NOW() - interval '18 days', NOW() - interval '3 days'),
    ('2d42b4e1-583a-4c6a-b0b7-3fa94f97b902', '315a14b5-f83c-42bc-a038-39cf8340bf3a', 'CRS-14', 'VUEJS', 'FAILED', 45.00, 48.00, NOW() - interval '18 days', NULL),
    ('44417351-68d8-47e8-960d-07034b329cc0', 'be569c29-d7e0-482e-a485-654f2511098e', 'CRS-02', 'REACT', 'FAILED', 45.00, 48.00, NOW() - interval '17 days', NULL),
    ('92512d12-2457-4793-aca9-22feceda949a', 'be569c29-d7e0-482e-a485-654f2511098e', 'CRS-04', 'DOCKER', 'COMPLETED', 100.00, 85.00, NOW() - interval '17 days', NOW() - interval '4 days'),
    ('85234302-35bd-4d8a-b85d-2ac4a567210a', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'CRS-11', 'HTML_CSS', 'COMPLETED', 100.00, 86.00, NOW() - interval '16 days', NOW() - interval '5 days'),
    ('4580c63e-94b8-4b13-80a8-58d01d076495', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'CRS-15', 'KUBERNETES', 'IN_PROGRESS', 72.00, NULL, NOW() - interval '16 days', NULL),
    ('d1695051-586e-4e1f-a275-dde5af8608ee', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'CRS-12', 'JAVASCRIPT', 'IN_PROGRESS', 85.00, NULL, NOW() - interval '15 days', NULL),
    ('b1b728c9-569c-43ee-8dba-c737c336a726', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'CRS-16', 'AWS', 'ENROLLED', 0.00, NULL, NOW() - interval '15 days', NULL),
    ('7a7c93c0-a6d7-4e72-be5d-18f8d37a6dd2', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'CRS-13', 'TYPESCRIPT', 'ENROLLED', 0.00, NULL, NOW() - interval '14 days', NULL),
    ('94b646d2-1991-48be-a199-f007ce2f880d', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'CRS-03', 'SQL', 'IN_PROGRESS', 43.00, NULL, NOW() - interval '14 days', NULL),
    ('278a7c7e-3792-40a7-a681-e4138ac75f9e', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'CRS-14', 'VUEJS', 'IN_PROGRESS', 56.00, NULL, NOW() - interval '13 days', NULL),
    ('00cb8278-b456-4808-84f9-52b8e70efe65', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'CRS-17', 'MYSQL', 'COMPLETED', 100.00, 89.00, NOW() - interval '13 days', NOW() - interval '8 days'),
    ('344dd3a6-67bd-4e22-9a51-f7173e26415e', '26dc3569-5b76-4c65-aaf9-eafcc3fd2412', 'CRS-04', 'DOCKER', 'COMPLETED', 100.00, 90.00, NOW() - interval '12 days', NOW() - interval '9 days'),
    ('15e0592a-7870-486d-82e1-5832a1bcd3e5', '5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', 'CRS-15', 'KUBERNETES', 'FAILED', 45.00, 48.00, NOW() - interval '11 days', NULL),
    ('ff33c60a-2dff-4f9f-8a1c-280f672f2310', '98da37df-6ea3-439a-a76b-356a87e3c39b', 'CRS-16', 'AWS', 'COMPLETED', 100.00, 92.00, NOW() - interval '25 days', NOW() - interval '1 days'),
    ('672c060f-3104-4215-ad0a-56761c466bac', '55c98170-fe5f-49bf-af88-812dc97231fa', 'CRS-03', 'SQL', 'IN_PROGRESS', 53.00, NULL, NOW() - interval '24 days', NULL),
    ('9e4ba31d-7f76-4208-a583-fd2acfb71abf', 'a68f2877-ef24-461c-abbf-090bb2a7c579', 'CRS-17', 'MYSQL', 'ENROLLED', 0.00, NULL, NOW() - interval '23 days', NULL),
    ('0c059763-2d55-4714-995c-b2732557bb2e', 'f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'CRS-18', 'MONGODB', 'IN_PROGRESS', 79.00, NULL, NOW() - interval '22 days', NULL),
    ('c43451c3-e782-4c35-adce-9bb00abbb486', '79518343-7d11-463f-a6ef-b199d83106da', 'CRS-05', 'TESTING', 'COMPLETED', 100.00, 96.00, NOW() - interval '21 days', NOW() - interval '5 days'),
    ('85ca09c7-1a7f-4306-8342-4bcc2571f2f4', 'f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', 'CRS-19', 'COMMUNICATION', 'FAILED', 45.00, 48.00, NOW() - interval '20 days', NULL),
    ('dfcb07db-5d83-43b8-a58b-74e506aeede1', 'e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'CRS-01', 'SPRING_BOOT', 'COMPLETED', 100.00, 80.00, NOW() - interval '19 days', NOW() - interval '7 days'),
    ('f135c677-3145-43a2-86fd-eebc4762de18', '754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', 'CRS-07', 'JAVA_CORE', 'IN_PROGRESS', 76.00, NULL, NOW() - interval '18 days', NULL),
    ('b8bd9c66-a14f-4546-8462-ae9d8f2aedd6', '172c2f01-2008-4e41-a7cb-12a83c6fd75f', 'CRS-08', 'NODEJS', 'ENROLLED', 0.00, NULL, NOW() - interval '17 days', NULL),
    ('3ca13e2d-e9ab-42f6-8f89-fa49792f1fbf', '1a56c767-f6f9-4bd7-aced-b8edca9dffbb', 'CRS-09', 'GOLANG', 'IN_PROGRESS', 47.00, NULL, NOW() - interval '16 days', NULL)
ON CONFLICT (user_id, lms_course_id) DO NOTHING;

INSERT INTO learning_progress (id, enrollment_id, user_id, lesson_id, completed, watched_seconds, updated_at) VALUES
    ('501af6ff-4aa0-4978-aac0-91992d2952a3', '8f9cf5ac-0e9a-46cc-ba22-8a4cbfbe19ae', 'ed854c07-f3eb-48f1-a186-4e7135f54645', '4de21448-5c86-4e4e-80a2-05359ed85b11', true, 1800, NOW() - interval '0 days'),
    ('5347c8c5-c09b-48ee-a1fc-1c3ab81555a1', '8f9cf5ac-0e9a-46cc-ba22-8a4cbfbe19ae', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'e87c0d9c-44d5-487d-9d4c-571140fc2d66', true, 1800, NOW() - interval '1 days'),
    ('408714a2-41bf-4c7d-9c3d-9219ad81ed18', '8f9cf5ac-0e9a-46cc-ba22-8a4cbfbe19ae', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'a5a46664-e5e2-4b6c-b251-5f0f283f9a38', true, 1800, NOW() - interval '2 days'),
    ('a75179d7-1fd4-4dd7-b22f-48e48f669440', '1bc3344f-4f1f-4605-b25d-effc6312c3b8', 'ed854c07-f3eb-48f1-a186-4e7135f54645', '687f897e-c866-4c30-8e3f-b770d5903375', true, 1800, NOW() - interval '0 days'),
    ('d28bbbc5-4af4-4b48-a1c8-b0c6cb8b8f1a', '1bc3344f-4f1f-4605-b25d-effc6312c3b8', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 'd82ca277-dd92-44d2-b812-29bd413277b0', false, 600, NOW() - interval '1 days'),
    ('ed01c351-9817-4319-82cf-b19c4b58ba34', 'f8e883fa-a677-429d-8bb2-5f6837ab9af9', '797c5663-386b-4e96-9112-6d3e414b4449', '3a3321ec-3382-4d07-b592-6332a1b096e4', true, 1800, NOW() - interval '1 days'),
    ('2834bf50-c87f-4d10-8773-9896ee305be1', 'f8e883fa-a677-429d-8bb2-5f6837ab9af9', '797c5663-386b-4e96-9112-6d3e414b4449', 'b2c7014e-cf31-47c2-9936-b39166f459d0', false, 600, NOW() - interval '2 days'),
    ('5390d761-d1ef-4a83-8212-64cb665afcf7', 'f8e883fa-a677-429d-8bb2-5f6837ab9af9', '797c5663-386b-4e96-9112-6d3e414b4449', '24442a86-ad51-4945-88fd-e7057f51d14e', false, 600, NOW() - interval '3 days'),
    ('28f28aa7-1070-4f56-bc7a-3a9ee1a57059', 'fc75aa3f-70d3-4693-92fe-ad3ee4b8fd46', '797c5663-386b-4e96-9112-6d3e414b4449', 'a8c9358a-6262-43cd-8fd0-fb884624a3d3', false, 600, NOW() - interval '1 days'),
    ('f2bb7944-4cac-42a0-9112-bc97b51516be', 'fc75aa3f-70d3-4693-92fe-ad3ee4b8fd46', '797c5663-386b-4e96-9112-6d3e414b4449', '570e1361-2384-4ff8-8acb-05afd98aca9e', false, 600, NOW() - interval '2 days'),
    ('156f56ac-af08-4b4a-b4b7-d9a05535954a', '50fbfd9a-42cd-41cf-8b4b-0331964925d9', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', '16e621db-bbd8-4650-ac6f-f30cc1356857', false, 600, NOW() - interval '2 days'),
    ('91f45b62-c13d-4c89-878b-84656a5bd3f9', '50fbfd9a-42cd-41cf-8b4b-0331964925d9', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', '8dc8c301-e387-49a7-b1e3-1d374e92253a', false, 600, NOW() - interval '3 days'),
    ('9ccb2a7a-0001-4016-bf22-5fcf4d5b011c', '50fbfd9a-42cd-41cf-8b4b-0331964925d9', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', '891995ad-2c74-4170-b635-20af72e533fe', false, 600, NOW() - interval '4 days'),
    ('f03404f3-c1f2-4445-bbbd-46a6a3d2c737', '6fa5f959-e58a-4e25-a028-c2d0e5c006ce', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 'ef692e58-5297-4352-9972-b5e74b97a688', true, 1800, NOW() - interval '2 days'),
    ('6ad9d562-8c67-464c-90aa-65681239defc', '6fa5f959-e58a-4e25-a028-c2d0e5c006ce', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', '6364e33e-bdae-4103-9632-246787d7aa1b', false, 600, NOW() - interval '3 days'),
    ('913dbc80-8e14-4e7f-bf31-724d386965e6', '88721941-7dd1-4242-8923-9c16b957f067', 'f848319f-9e6e-4815-a697-2fe2400fd32f', '771cb6c7-aa59-469e-8561-6967881d6039', true, 1800, NOW() - interval '3 days'),
    ('962cc4b8-7ceb-486b-891c-1d85c9a8046d', '88721941-7dd1-4242-8923-9c16b957f067', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 'ca86fdc0-2f1d-44b8-8658-e8d12520efd7', false, 600, NOW() - interval '4 days'),
    ('e4a2ad71-2a5b-44b2-88f0-1ad2077aad6f', '88721941-7dd1-4242-8923-9c16b957f067', 'f848319f-9e6e-4815-a697-2fe2400fd32f', '8cb63ebe-f54f-4fff-ad85-7443fd6b57ca', false, 600, NOW() - interval '5 days'),
    ('c6d8da07-a6d8-4456-89d3-c65b839f9bd4', '59c542c8-2cb4-4028-b2fb-74eefffd4b79', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 'd6f701b1-553c-45a8-a98b-3c9a3d50937d', true, 1800, NOW() - interval '3 days'),
    ('2efaff09-236d-4c90-b86a-5b4f996ee248', '59c542c8-2cb4-4028-b2fb-74eefffd4b79', 'f848319f-9e6e-4815-a697-2fe2400fd32f', '71824df8-e86c-4d92-bdd6-3620d28ea1c5', true, 1800, NOW() - interval '4 days'),
    ('ce46aa5c-6049-46b7-9fbe-b25252ff013a', '7777d07c-bc74-43f8-98f3-95b837f15494', '6257a31f-e618-4ec1-ad25-67574b41a04e', '52a677b9-72f7-4dfa-8608-372d1c1480ad', true, 1800, NOW() - interval '4 days'),
    ('74aad82b-fc09-40f2-8388-6a65080d201a', '7777d07c-bc74-43f8-98f3-95b837f15494', '6257a31f-e618-4ec1-ad25-67574b41a04e', '5579e66d-24e1-4f40-bd9f-27c8b92e3497', true, 1800, NOW() - interval '5 days'),
    ('bfde1bca-64fe-48fe-ab68-d0520a0909cd', '7777d07c-bc74-43f8-98f3-95b837f15494', '6257a31f-e618-4ec1-ad25-67574b41a04e', '336d5330-a3d3-46ee-9001-87a538fa602f', true, 1800, NOW() - interval '6 days'),
    ('31684e68-3a9b-4708-9a1f-f79aa535cc52', '7bbd698f-d2a0-47ce-9081-bf6b927f2c6e', '6257a31f-e618-4ec1-ad25-67574b41a04e', '827ce5d5-a301-4553-a81c-fa522c67c894', false, 600, NOW() - interval '4 days'),
    ('db1f96b6-7722-453f-8d2b-5e5fdb47c5f8', '7bbd698f-d2a0-47ce-9081-bf6b927f2c6e', '6257a31f-e618-4ec1-ad25-67574b41a04e', 'de80212e-bc97-437b-9097-85fa677413a8', false, 600, NOW() - interval '5 days'),
    ('09e93db5-5ba0-4561-a33e-e4ebee18e59e', '1bc901eb-fbab-4842-b770-ca4fd5f0c221', 'c633fa6f-51e7-4257-a13b-9bc051acab55', '687f897e-c866-4c30-8e3f-b770d5903375', false, 600, NOW() - interval '5 days'),
    ('61202bd8-2338-4f9c-b4a6-48212577f289', '1bc901eb-fbab-4842-b770-ca4fd5f0c221', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 'd82ca277-dd92-44d2-b812-29bd413277b0', false, 600, NOW() - interval '6 days'),
    ('8df6bd75-fa26-47c6-aef2-f2c66061d87b', '1bc901eb-fbab-4842-b770-ca4fd5f0c221', 'c633fa6f-51e7-4257-a13b-9bc051acab55', '247449ba-0648-4293-be8e-fe20e3d6f92c', false, 600, NOW() - interval '7 days'),
    ('7c199651-b423-4edf-9547-c5c1ad24f7f1', '76f7577b-a5ea-4b94-a2a2-e2683f813ed9', 'c633fa6f-51e7-4257-a13b-9bc051acab55', '0c7b2341-5cae-43d4-ad1c-50766cafbb18', true, 1800, NOW() - interval '5 days'),
    ('b8da20ff-9a74-423b-9f75-9384ef6f7dfe', '76f7577b-a5ea-4b94-a2a2-e2683f813ed9', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 'facaa61b-0a1f-4925-b84c-4b6911e2e4d9', true, 1800, NOW() - interval '6 days'),
    ('1ab1bad0-6473-4229-9d1d-0635ce85c638', 'e8a606ed-3193-4a98-854f-cc7bd798eab3', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'a8c9358a-6262-43cd-8fd0-fb884624a3d3', true, 1800, NOW() - interval '6 days'),
    ('b253e8d0-5f14-4f6f-b4d0-078ca097d34b', 'e8a606ed-3193-4a98-854f-cc7bd798eab3', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', '570e1361-2384-4ff8-8acb-05afd98aca9e', true, 1800, NOW() - interval '7 days'),
    ('d20af128-a7a5-4176-b179-b9158a20f529', 'e8a606ed-3193-4a98-854f-cc7bd798eab3', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', '95c8bc69-bffa-4745-9b3d-fe4b871c2dda', true, 1800, NOW() - interval '8 days'),
    ('ce59c665-0232-4150-a2df-965206db5737', '96bf5dc8-ba62-48c3-a021-95406e055127', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', '2bd99181-f697-4d31-ae60-804d147f839d', true, 1800, NOW() - interval '6 days'),
    ('44c340ca-09e7-455a-a972-c2c1aa63b7ed', '96bf5dc8-ba62-48c3-a021-95406e055127', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 'd6d62106-90da-4e33-a466-b79cab239ae6', false, 600, NOW() - interval '7 days'),
    ('5fda620e-d8bc-4720-9d19-d17259650ce6', '7023c59f-9611-49e5-938e-63a9f470ed16', '7d26abe3-98c9-4af8-a904-e57f81783d54', 'ef692e58-5297-4352-9972-b5e74b97a688', true, 1800, NOW() - interval '7 days'),
    ('fbb5f83d-098b-4431-bf82-2ab97c284c04', '7023c59f-9611-49e5-938e-63a9f470ed16', '7d26abe3-98c9-4af8-a904-e57f81783d54', '6364e33e-bdae-4103-9632-246787d7aa1b', false, 600, NOW() - interval '8 days'),
    ('4a70e1e3-52a3-42fe-9e5d-cd2ae202475f', '7023c59f-9611-49e5-938e-63a9f470ed16', '7d26abe3-98c9-4af8-a904-e57f81783d54', '4c66206f-03f5-4f2a-901c-c281e976c2a2', false, 600, NOW() - interval '9 days'),
    ('0ff33f5a-3d84-4215-a0d7-4d16a6d0fcc7', 'd457ab3b-6625-4d7f-ae95-ae495fb74144', '7d26abe3-98c9-4af8-a904-e57f81783d54', '48e628f9-490b-4037-b999-a35e6b201395', false, 600, NOW() - interval '7 days'),
    ('99469400-9dd0-4910-b49f-c3ae8779a193', 'd457ab3b-6625-4d7f-ae95-ae495fb74144', '7d26abe3-98c9-4af8-a904-e57f81783d54', '7dd51142-3406-4e27-9612-9facee4bd0f9', false, 600, NOW() - interval '8 days'),
    ('6d2444c0-e622-4dbb-9330-f6eaf415b7a6', 'c5e54879-49ad-497b-8096-8932113142f0', '6f7d791c-8ef0-4404-ab53-5871eb562621', 'd6f701b1-553c-45a8-a98b-3c9a3d50937d', false, 600, NOW() - interval '8 days'),
    ('6138dd37-fb15-452d-ab4e-c1757a237dc5', 'c5e54879-49ad-497b-8096-8932113142f0', '6f7d791c-8ef0-4404-ab53-5871eb562621', '71824df8-e86c-4d92-bdd6-3620d28ea1c5', false, 600, NOW() - interval '9 days'),
    ('c32c3495-dade-433e-bf9a-5f1075354eb1', 'c5e54879-49ad-497b-8096-8932113142f0', '6f7d791c-8ef0-4404-ab53-5871eb562621', '303af261-e678-4a83-bd7b-8ca7a122a532', false, 600, NOW() - interval '0 days'),
    ('d86dc0aa-d77d-4bd4-b813-0b834770c9d4', '796e2691-4715-47dc-8fc8-959700650de0', '6f7d791c-8ef0-4404-ab53-5871eb562621', '1c885679-a16f-46b2-9939-85b8f9228afb', true, 1800, NOW() - interval '8 days'),
    ('a7a12dc9-5d80-4da8-be6e-595226ad3fb7', '796e2691-4715-47dc-8fc8-959700650de0', '6f7d791c-8ef0-4404-ab53-5871eb562621', '5e2915fb-3dc5-4671-ad36-91fa0343a3b7', false, 600, NOW() - interval '9 days'),
    ('f205ee0d-8d5e-49c9-9248-cc7de617eadd', '857451f4-8e96-4be6-b74e-f75a2193767f', '1c1cae0c-2691-4876-a05e-e1c133a1f013', '827ce5d5-a301-4553-a81c-fa522c67c894', true, 1800, NOW() - interval '9 days'),
    ('0c6d00bd-8766-4e20-8c8f-72d308c3bc87', '857451f4-8e96-4be6-b74e-f75a2193767f', '1c1cae0c-2691-4876-a05e-e1c133a1f013', 'de80212e-bc97-437b-9097-85fa677413a8', false, 600, NOW() - interval '0 days'),
    ('ab7be488-09bf-4453-b5a4-0ae0056ab2a2', '857451f4-8e96-4be6-b74e-f75a2193767f', '1c1cae0c-2691-4876-a05e-e1c133a1f013', '79ad8be2-e5e6-4258-856e-3df5882c66ac', false, 600, NOW() - interval '1 days'),
    ('ab2c5a0c-234f-4140-88b9-8a9f461741ec', '41ebdf88-c535-420a-8f3c-fec3190e743c', '1c1cae0c-2691-4876-a05e-e1c133a1f013', '43251dc6-d585-449f-832e-77226b893ef9', true, 1800, NOW() - interval '9 days'),
    ('5485c605-455b-4151-a64e-9322c7a83b7b', '41ebdf88-c535-420a-8f3c-fec3190e743c', '1c1cae0c-2691-4876-a05e-e1c133a1f013', '6965dd13-f4cb-4cf2-a33d-eb3ab8624c2a', true, 1800, NOW() - interval '0 days'),
    ('90f0df65-858e-41db-b284-d40e12e1d1b6', 'e3971adc-840b-4b62-8f67-e87c1d2cbf12', '84e1f7fb-48da-480c-ab2a-799f6d931982', '0c7b2341-5cae-43d4-ad1c-50766cafbb18', true, 1800, NOW() - interval '0 days'),
    ('6861019b-84ce-4ef4-a9ee-0de5088c03ad', 'e3971adc-840b-4b62-8f67-e87c1d2cbf12', '84e1f7fb-48da-480c-ab2a-799f6d931982', 'facaa61b-0a1f-4925-b84c-4b6911e2e4d9', true, 1800, NOW() - interval '1 days'),
    ('83b48ad0-719c-4a86-b1a8-f05f7c5d6be1', 'e3971adc-840b-4b62-8f67-e87c1d2cbf12', '84e1f7fb-48da-480c-ab2a-799f6d931982', '6dc5a0c9-95eb-44da-bfe9-0c23bb72cccb', true, 1800, NOW() - interval '2 days'),
    ('48bb46f5-bc4a-425f-a4d1-dac7b1060a43', 'f8f62590-f518-4863-91f8-6f594157efe8', '84e1f7fb-48da-480c-ab2a-799f6d931982', 'e88a86e3-3954-46ed-9dbd-35f38657bc16', false, 600, NOW() - interval '0 days'),
    ('2f0ca246-7843-4a2a-a578-a263f4d55bac', 'f8f62590-f518-4863-91f8-6f594157efe8', '84e1f7fb-48da-480c-ab2a-799f6d931982', '3cd271c7-57d4-4087-9f44-7a0aca35c715', false, 600, NOW() - interval '1 days'),
    ('29e7ecf5-a9a2-4ccd-a3a6-a578a8241cdc', '871944f7-35c7-4223-bdef-11ff606bc345', '47687ff7-7211-405f-a081-230fad46f127', '2bd99181-f697-4d31-ae60-804d147f839d', false, 600, NOW() - interval '1 days'),
    ('64a0e575-e772-4410-a9f6-3c9aba780cb0', '871944f7-35c7-4223-bdef-11ff606bc345', '47687ff7-7211-405f-a081-230fad46f127', 'd6d62106-90da-4e33-a466-b79cab239ae6', false, 600, NOW() - interval '2 days'),
    ('99a1307e-dca7-4912-975e-a3875baa1757', '871944f7-35c7-4223-bdef-11ff606bc345', '47687ff7-7211-405f-a081-230fad46f127', '43d99c17-166f-4d3c-930b-08210b962753', false, 600, NOW() - interval '3 days'),
    ('7d752eb8-2a07-410b-ae1c-ade444b4b93a', 'eba2e097-f2d6-4815-a7c0-d71c14999a64', '47687ff7-7211-405f-a081-230fad46f127', '4d237ab0-ae76-4ad4-b9fc-c8535c94218a', true, 1800, NOW() - interval '1 days'),
    ('84f65f9f-ea2f-456b-b6c9-7cfafa87f1f6', 'eba2e097-f2d6-4815-a7c0-d71c14999a64', '47687ff7-7211-405f-a081-230fad46f127', 'd1f6b1ed-8d95-4367-b4c4-86ee21d485ad', true, 1800, NOW() - interval '2 days'),
    ('936e05d7-2d46-4762-8a8d-f212523a7195', '69b8875a-c57e-473d-84ff-a1ec031dfb97', 'a3692669-b304-40c2-a999-50695e5fedb6', '48e628f9-490b-4037-b999-a35e6b201395', true, 1800, NOW() - interval '2 days'),
    ('053d5172-cbdc-4a41-9ed7-afdfc68dfdc3', '69b8875a-c57e-473d-84ff-a1ec031dfb97', 'a3692669-b304-40c2-a999-50695e5fedb6', '7dd51142-3406-4e27-9612-9facee4bd0f9', true, 1800, NOW() - interval '3 days'),
    ('14becf61-19d6-4df4-9065-d0e2e7a55f34', '69b8875a-c57e-473d-84ff-a1ec031dfb97', 'a3692669-b304-40c2-a999-50695e5fedb6', '47ff5347-c81c-4e4e-a0c6-b6f8f2e87e08', true, 1800, NOW() - interval '4 days'),
    ('260f970f-6f3d-44d0-8cb0-b9f84842b545', '9f933fb7-a59d-4191-9f46-4b1ca3b22711', 'a3692669-b304-40c2-a999-50695e5fedb6', 'e893031b-b70e-42ad-b73b-5eea4fa8990c', true, 1800, NOW() - interval '2 days'),
    ('ec9fce67-e50a-4b37-b724-5ac940070dc4', '9f933fb7-a59d-4191-9f46-4b1ca3b22711', 'a3692669-b304-40c2-a999-50695e5fedb6', '031b70bb-b442-4ed0-a907-9ee058553d21', false, 600, NOW() - interval '3 days'),
    ('bb9b4d42-c63c-467f-8b68-0436daaca828', 'd8e0f151-65bf-4bf2-a59f-3ebecc6ca803', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', '1c885679-a16f-46b2-9939-85b8f9228afb', true, 1800, NOW() - interval '3 days'),
    ('968c5bb2-c9fd-4418-addc-fecc5cf1a4cd', 'd8e0f151-65bf-4bf2-a59f-3ebecc6ca803', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', '5e2915fb-3dc5-4671-ad36-91fa0343a3b7', false, 600, NOW() - interval '4 days'),
    ('1eac9484-6376-46c9-b18f-95a00fe9f4a2', 'd8e0f151-65bf-4bf2-a59f-3ebecc6ca803', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'f33ef755-80af-4d45-904a-6f430d6c95c4', false, 600, NOW() - interval '5 days'),
    ('2e23d96d-bd84-4f10-9e4a-29536169aed7', '7e616969-63d7-4433-93b3-8327a62c4568', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', '4de21448-5c86-4e4e-80a2-05359ed85b11', false, 600, NOW() - interval '3 days'),
    ('b0044ca3-1c5d-42fb-9243-1d7e1bc527ff', '7e616969-63d7-4433-93b3-8327a62c4568', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 'e87c0d9c-44d5-487d-9d4c-571140fc2d66', false, 600, NOW() - interval '4 days'),
    ('4254c9dd-9f24-4363-b9ed-a6b9043e6a92', 'a3870f7b-9c02-4e14-8316-f369ec6279db', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', '43251dc6-d585-449f-832e-77226b893ef9', false, 600, NOW() - interval '4 days'),
    ('99319e00-64f3-4304-9dee-47aaff68a904', 'a3870f7b-9c02-4e14-8316-f369ec6279db', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', '6965dd13-f4cb-4cf2-a33d-eb3ab8624c2a', false, 600, NOW() - interval '5 days'),
    ('8e17415c-75b7-4de2-ab51-712644bdd269', 'a3870f7b-9c02-4e14-8316-f369ec6279db', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', '2f2e841f-dd39-49de-a0b5-f8b6164cc73e', false, 600, NOW() - interval '6 days'),
    ('8e128130-1b7b-4d4d-9295-643994a80ca9', '3ce0abed-1565-4920-a6af-61bfa469edc8', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', '3a3321ec-3382-4d07-b592-6332a1b096e4', true, 1800, NOW() - interval '4 days'),
    ('4c08f1db-808d-4396-b31a-cd2cdddf1643', '3ce0abed-1565-4920-a6af-61bfa469edc8', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', 'b2c7014e-cf31-47c2-9936-b39166f459d0', false, 600, NOW() - interval '5 days'),
    ('049d9555-97a7-4021-96e4-8f4f65a95a41', '1f4061ae-989d-4373-9d6d-c8fd57a6234d', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 'e88a86e3-3954-46ed-9dbd-35f38657bc16', true, 1800, NOW() - interval '5 days'),
    ('ffdd0449-d1f8-4612-8f76-a274fa291660', '1f4061ae-989d-4373-9d6d-c8fd57a6234d', '56de20bd-ad10-4cd8-a732-7d41b40b6734', '3cd271c7-57d4-4087-9f44-7a0aca35c715', false, 600, NOW() - interval '6 days'),
    ('bcff859b-b079-4490-8b59-7ada458263f4', '1f4061ae-989d-4373-9d6d-c8fd57a6234d', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 'bf0d57e3-ab80-4f2d-a4b6-53830a668f42', false, 600, NOW() - interval '7 days'),
    ('84f0743e-c0c2-4a89-8928-f1c46c0c033d', 'fc442f61-d0d5-4195-993d-048914612875', '56de20bd-ad10-4cd8-a732-7d41b40b6734', '16e621db-bbd8-4650-ac6f-f30cc1356857', true, 1800, NOW() - interval '5 days'),
    ('c72b2b31-7eac-4121-a0eb-f754dc944b28', 'fc442f61-d0d5-4195-993d-048914612875', '56de20bd-ad10-4cd8-a732-7d41b40b6734', '8dc8c301-e387-49a7-b1e3-1d374e92253a', true, 1800, NOW() - interval '6 days'),
    ('a937e2dc-7364-4ae9-9b82-a9f639d0657b', '852193a6-4517-4869-83de-76bbbe610a20', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', '4d237ab0-ae76-4ad4-b9fc-c8535c94218a', true, 1800, NOW() - interval '6 days'),
    ('bcb17ae4-2d2b-4d03-9871-d7733b39e3b9', '852193a6-4517-4869-83de-76bbbe610a20', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'd1f6b1ed-8d95-4367-b4c4-86ee21d485ad', true, 1800, NOW() - interval '7 days'),
    ('64c8ae1c-d224-4404-9402-b8b75d44db03', '852193a6-4517-4869-83de-76bbbe610a20', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', '7f14e31f-29df-4e91-b66d-3ff72c935390', true, 1800, NOW() - interval '8 days'),
    ('aed79b9f-6344-4bba-8d31-3ea30ef0ea1c', '50b4ac3e-d4a7-4386-ad73-53c0884add3a', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', '771cb6c7-aa59-469e-8561-6967881d6039', false, 600, NOW() - interval '6 days'),
    ('456ee700-8213-4f4b-be7c-720215f827f8', '50b4ac3e-d4a7-4386-ad73-53c0884add3a', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 'ca86fdc0-2f1d-44b8-8658-e8d12520efd7', false, 600, NOW() - interval '7 days'),
    ('03c88e6f-aded-4777-b5b4-0097985d37a9', '88c832f6-d0b3-47dc-b8af-1d55207fdd47', '984682cd-5097-4e07-aea4-7f6e423a80d5', 'e893031b-b70e-42ad-b73b-5eea4fa8990c', false, 600, NOW() - interval '7 days'),
    ('f6464337-5eb7-489f-862c-2b8861b347ae', '88c832f6-d0b3-47dc-b8af-1d55207fdd47', '984682cd-5097-4e07-aea4-7f6e423a80d5', '031b70bb-b442-4ed0-a907-9ee058553d21', false, 600, NOW() - interval '8 days'),
    ('d58bc3a6-1fc6-491e-ae5b-8357eaa6d97f', '88c832f6-d0b3-47dc-b8af-1d55207fdd47', '984682cd-5097-4e07-aea4-7f6e423a80d5', '3d29fb9e-7ad1-4806-b2c3-fd5dbdf472ce', false, 600, NOW() - interval '9 days'),
    ('b395d2a3-b951-47e7-8857-53b42bea3822', '4fcb2dde-fc91-41f5-a4a3-57f09de985b3', '984682cd-5097-4e07-aea4-7f6e423a80d5', '52a677b9-72f7-4dfa-8608-372d1c1480ad', true, 1800, NOW() - interval '7 days'),
    ('c6e271f8-09fb-4ce7-bbb3-6e17eb168201', '4fcb2dde-fc91-41f5-a4a3-57f09de985b3', '984682cd-5097-4e07-aea4-7f6e423a80d5', '5579e66d-24e1-4f40-bd9f-27c8b92e3497', true, 1800, NOW() - interval '8 days'),
    ('905ab1f9-fa3d-4e27-8278-67bb71cb72f3', '55b321c6-6868-40a2-a251-a9efc5c0ca97', '0539425d-c9ea-42ad-a526-af16615ee51e', '4de21448-5c86-4e4e-80a2-05359ed85b11', true, 1800, NOW() - interval '8 days'),
    ('6566963c-4822-488c-b7d3-8c5474a809d7', '55b321c6-6868-40a2-a251-a9efc5c0ca97', '0539425d-c9ea-42ad-a526-af16615ee51e', 'e87c0d9c-44d5-487d-9d4c-571140fc2d66', true, 1800, NOW() - interval '9 days'),
    ('d3ab39d1-e614-4a0d-b1db-5baf02d344e4', '55b321c6-6868-40a2-a251-a9efc5c0ca97', '0539425d-c9ea-42ad-a526-af16615ee51e', 'a5a46664-e5e2-4b6c-b251-5f0f283f9a38', true, 1800, NOW() - interval '0 days'),
    ('9e8bc7ab-89c7-46d0-9258-8f50c510cab2', '73ea1a1e-693b-4ca2-b664-3611500ce61f', '0539425d-c9ea-42ad-a526-af16615ee51e', '687f897e-c866-4c30-8e3f-b770d5903375', true, 1800, NOW() - interval '8 days'),
    ('a821a470-b74b-4f61-97f5-4843000f443f', '73ea1a1e-693b-4ca2-b664-3611500ce61f', '0539425d-c9ea-42ad-a526-af16615ee51e', 'd82ca277-dd92-44d2-b812-29bd413277b0', false, 600, NOW() - interval '9 days'),
    ('70f7474f-5944-4261-88fb-37dec9a168a7', '7675c702-c767-4ea8-9d2a-8f15510651e7', '4b20ea51-eba8-4087-a312-b142beefc5b7', '3a3321ec-3382-4d07-b592-6332a1b096e4', true, 1800, NOW() - interval '9 days'),
    ('6f20c8e8-b245-49cb-aced-6e09980ce686', '7675c702-c767-4ea8-9d2a-8f15510651e7', '4b20ea51-eba8-4087-a312-b142beefc5b7', 'b2c7014e-cf31-47c2-9936-b39166f459d0', false, 600, NOW() - interval '0 days'),
    ('9cc91ba3-280a-435b-8174-3ce6f0a8ac0d', '7675c702-c767-4ea8-9d2a-8f15510651e7', '4b20ea51-eba8-4087-a312-b142beefc5b7', '24442a86-ad51-4945-88fd-e7057f51d14e', false, 600, NOW() - interval '1 days'),
    ('23f537df-a91f-4de1-8933-2e832c910ff0', 'd126be9a-2ebd-4f95-9bae-ded93d086305', '4b20ea51-eba8-4087-a312-b142beefc5b7', 'a8c9358a-6262-43cd-8fd0-fb884624a3d3', false, 600, NOW() - interval '9 days'),
    ('e9d86c3a-64dd-4e94-9b89-9215cf3d5340', 'd126be9a-2ebd-4f95-9bae-ded93d086305', '4b20ea51-eba8-4087-a312-b142beefc5b7', '570e1361-2384-4ff8-8acb-05afd98aca9e', false, 600, NOW() - interval '0 days'),
    ('0a010cfe-97da-478f-bf3b-5a1a345aa965', '88317c4b-b932-4504-8efd-a9edbccf6274', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', '16e621db-bbd8-4650-ac6f-f30cc1356857', false, 600, NOW() - interval '0 days'),
    ('ed527ca7-284e-43c3-84ac-a57f8be8c429', '88317c4b-b932-4504-8efd-a9edbccf6274', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', '8dc8c301-e387-49a7-b1e3-1d374e92253a', false, 600, NOW() - interval '1 days'),
    ('0b2030ed-57b6-4835-879a-dcfb18e48887', '88317c4b-b932-4504-8efd-a9edbccf6274', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', '891995ad-2c74-4170-b635-20af72e533fe', false, 600, NOW() - interval '2 days'),
    ('05b5d26b-87ce-4929-8858-58a228bdfa2a', 'b781ae52-ab27-4cbd-9339-b4309805da21', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 'ef692e58-5297-4352-9972-b5e74b97a688', true, 1800, NOW() - interval '0 days'),
    ('0fa94a76-4341-4b45-85bb-b0dba92b2219', 'b781ae52-ab27-4cbd-9339-b4309805da21', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', '6364e33e-bdae-4103-9632-246787d7aa1b', false, 600, NOW() - interval '1 days'),
    ('0685d928-7dbc-4f0c-bfe1-064c9ab67e69', 'c1fb6a1a-8865-41ab-9671-0fbf3fb9eda0', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', '771cb6c7-aa59-469e-8561-6967881d6039', true, 1800, NOW() - interval '1 days'),
    ('a8a1b958-632d-4eb9-aa91-d25965963a17', 'c1fb6a1a-8865-41ab-9671-0fbf3fb9eda0', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'ca86fdc0-2f1d-44b8-8658-e8d12520efd7', false, 600, NOW() - interval '2 days'),
    ('607380ca-8d75-4888-b82d-62d91c2027d9', 'c1fb6a1a-8865-41ab-9671-0fbf3fb9eda0', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', '8cb63ebe-f54f-4fff-ad85-7443fd6b57ca', false, 600, NOW() - interval '3 days'),
    ('644a46e7-b560-4ad3-9f76-4185ff4b7932', 'de86c58e-e7d3-45c4-9ccb-a238d1c4d1c8', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 'd6f701b1-553c-45a8-a98b-3c9a3d50937d', true, 1800, NOW() - interval '1 days'),
    ('c3bfa0b1-87f0-45c2-b4f1-4aabc320e840', 'de86c58e-e7d3-45c4-9ccb-a238d1c4d1c8', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', '71824df8-e86c-4d92-bdd6-3620d28ea1c5', true, 1800, NOW() - interval '2 days'),
    ('c7cf4ff4-2c46-4f2b-a538-f100e003b040', 'c3c286a8-e7db-4292-b8e4-c2e627dd7680', '315a14b5-f83c-42bc-a038-39cf8340bf3a', '52a677b9-72f7-4dfa-8608-372d1c1480ad', true, 1800, NOW() - interval '2 days'),
    ('19417e5f-3a64-4573-ba2e-6f5c377d0ab4', 'c3c286a8-e7db-4292-b8e4-c2e627dd7680', '315a14b5-f83c-42bc-a038-39cf8340bf3a', '5579e66d-24e1-4f40-bd9f-27c8b92e3497', true, 1800, NOW() - interval '3 days'),
    ('4d1fb776-e26c-48a6-8665-204c992cf523', 'c3c286a8-e7db-4292-b8e4-c2e627dd7680', '315a14b5-f83c-42bc-a038-39cf8340bf3a', '336d5330-a3d3-46ee-9001-87a538fa602f', true, 1800, NOW() - interval '4 days'),
    ('b7edc461-4216-423f-b128-40a269677ce5', '2d42b4e1-583a-4c6a-b0b7-3fa94f97b902', '315a14b5-f83c-42bc-a038-39cf8340bf3a', '827ce5d5-a301-4553-a81c-fa522c67c894', false, 600, NOW() - interval '2 days'),
    ('eb3d68fd-b365-4e92-8b32-0b9dbf68572c', '2d42b4e1-583a-4c6a-b0b7-3fa94f97b902', '315a14b5-f83c-42bc-a038-39cf8340bf3a', 'de80212e-bc97-437b-9097-85fa677413a8', false, 600, NOW() - interval '3 days'),
    ('9f8a6f56-770d-4fe6-b1e2-756c18b85e9b', '44417351-68d8-47e8-960d-07034b329cc0', 'be569c29-d7e0-482e-a485-654f2511098e', '687f897e-c866-4c30-8e3f-b770d5903375', false, 600, NOW() - interval '3 days'),
    ('ec49f784-27c0-4f7d-a564-3f5b83774a8f', '44417351-68d8-47e8-960d-07034b329cc0', 'be569c29-d7e0-482e-a485-654f2511098e', 'd82ca277-dd92-44d2-b812-29bd413277b0', false, 600, NOW() - interval '4 days'),
    ('b4ecb96c-e64e-445c-b1dc-ef539cf8fd95', '44417351-68d8-47e8-960d-07034b329cc0', 'be569c29-d7e0-482e-a485-654f2511098e', '247449ba-0648-4293-be8e-fe20e3d6f92c', false, 600, NOW() - interval '5 days'),
    ('245dde38-97be-4eaa-a8ca-de0bd7cb807d', '92512d12-2457-4793-aca9-22feceda949a', 'be569c29-d7e0-482e-a485-654f2511098e', '0c7b2341-5cae-43d4-ad1c-50766cafbb18', true, 1800, NOW() - interval '3 days'),
    ('6c04b3e8-5e61-4a92-8d5c-d0aeb52f247d', '92512d12-2457-4793-aca9-22feceda949a', 'be569c29-d7e0-482e-a485-654f2511098e', 'facaa61b-0a1f-4925-b84c-4b6911e2e4d9', true, 1800, NOW() - interval '4 days'),
    ('931f7739-5c4a-4c13-b782-66d8a0a480fb', '85234302-35bd-4d8a-b85d-2ac4a567210a', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'a8c9358a-6262-43cd-8fd0-fb884624a3d3', true, 1800, NOW() - interval '4 days'),
    ('f7d78d5e-8bc7-4dec-aaec-f58e3a52ee35', '85234302-35bd-4d8a-b85d-2ac4a567210a', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', '570e1361-2384-4ff8-8acb-05afd98aca9e', true, 1800, NOW() - interval '5 days'),
    ('400b507c-f8bd-4639-b83c-e3d17788b678', '85234302-35bd-4d8a-b85d-2ac4a567210a', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', '95c8bc69-bffa-4745-9b3d-fe4b871c2dda', true, 1800, NOW() - interval '6 days'),
    ('717f9c55-6145-450f-92bc-fb1831dc7a0b', '4580c63e-94b8-4b13-80a8-58d01d076495', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', '2bd99181-f697-4d31-ae60-804d147f839d', true, 1800, NOW() - interval '4 days'),
    ('6af3b4d9-ac87-4f1e-903d-5da7374fb1c3', '4580c63e-94b8-4b13-80a8-58d01d076495', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 'd6d62106-90da-4e33-a466-b79cab239ae6', false, 600, NOW() - interval '5 days'),
    ('d82fc8d8-474a-43a5-b73e-deae7187a4d2', 'd1695051-586e-4e1f-a275-dde5af8608ee', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', 'ef692e58-5297-4352-9972-b5e74b97a688', true, 1800, NOW() - interval '5 days'),
    ('f3cfed03-b50e-494b-9802-bc8166ba11e5', 'd1695051-586e-4e1f-a275-dde5af8608ee', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', '6364e33e-bdae-4103-9632-246787d7aa1b', false, 600, NOW() - interval '6 days'),
    ('a61758bd-be3c-4b5e-91c9-dc2f62d06700', 'd1695051-586e-4e1f-a275-dde5af8608ee', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', '4c66206f-03f5-4f2a-901c-c281e976c2a2', false, 600, NOW() - interval '7 days'),
    ('47b0eec1-302b-44cc-9048-ed77e84d21cf', 'b1b728c9-569c-43ee-8dba-c737c336a726', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', '48e628f9-490b-4037-b999-a35e6b201395', false, 600, NOW() - interval '5 days'),
    ('a833d16c-85d0-4193-b13b-73166425d0e1', 'b1b728c9-569c-43ee-8dba-c737c336a726', '33393f21-cf38-4f15-a2f3-6c75660dcb7f', '7dd51142-3406-4e27-9612-9facee4bd0f9', false, 600, NOW() - interval '6 days'),
    ('01b248a7-2aa4-4b55-bf73-76d96a60e63b', '7a7c93c0-a6d7-4e72-be5d-18f8d37a6dd2', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', 'd6f701b1-553c-45a8-a98b-3c9a3d50937d', false, 600, NOW() - interval '6 days'),
    ('31925e03-46db-434f-b52b-8d35b6bcd298', '7a7c93c0-a6d7-4e72-be5d-18f8d37a6dd2', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', '71824df8-e86c-4d92-bdd6-3620d28ea1c5', false, 600, NOW() - interval '7 days'),
    ('967f9c60-8042-42d1-9c14-518b6e578fb1', '7a7c93c0-a6d7-4e72-be5d-18f8d37a6dd2', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', '303af261-e678-4a83-bd7b-8ca7a122a532', false, 600, NOW() - interval '8 days'),
    ('5625138f-6e37-4906-9671-69388d838063', '94b646d2-1991-48be-a199-f007ce2f880d', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', '1c885679-a16f-46b2-9939-85b8f9228afb', true, 1800, NOW() - interval '6 days'),
    ('585360e0-1386-4a1d-9a61-3eb613318f19', '94b646d2-1991-48be-a199-f007ce2f880d', '03f675fd-aaf0-46f9-ab0b-d51bbc2ff494', '5e2915fb-3dc5-4671-ad36-91fa0343a3b7', false, 600, NOW() - interval '7 days'),
    ('fe4c8653-f0ce-4d12-b030-d48a49d0f701', '278a7c7e-3792-40a7-a681-e4138ac75f9e', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', '827ce5d5-a301-4553-a81c-fa522c67c894', true, 1800, NOW() - interval '7 days'),
    ('d5efc0e2-002a-4b25-bb17-b0cd0f2b0a55', '278a7c7e-3792-40a7-a681-e4138ac75f9e', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', 'de80212e-bc97-437b-9097-85fa677413a8', false, 600, NOW() - interval '8 days'),
    ('930eb092-2aa4-43df-b470-9774d4fdc772', '278a7c7e-3792-40a7-a681-e4138ac75f9e', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', '79ad8be2-e5e6-4258-856e-3df5882c66ac', false, 600, NOW() - interval '9 days'),
    ('c3dc44d3-a54e-4cb2-8e2e-cd459ae45f6e', '00cb8278-b456-4808-84f9-52b8e70efe65', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', '43251dc6-d585-449f-832e-77226b893ef9', true, 1800, NOW() - interval '7 days'),
    ('84380e58-3810-4bb0-94f5-13da3dfb8232', '00cb8278-b456-4808-84f9-52b8e70efe65', 'e4d6bd22-1eda-4998-a875-ed3bdd7c0b3a', '6965dd13-f4cb-4cf2-a33d-eb3ab8624c2a', true, 1800, NOW() - interval '8 days'),
    ('4a4fd709-fc8c-4e06-b089-d593be77e91b', '344dd3a6-67bd-4e22-9a51-f7173e26415e', '26dc3569-5b76-4c65-aaf9-eafcc3fd2412', '0c7b2341-5cae-43d4-ad1c-50766cafbb18', true, 1800, NOW() - interval '8 days'),
    ('c7f3d1f2-2adf-4a62-b2a9-dedfa4567ace', '344dd3a6-67bd-4e22-9a51-f7173e26415e', '26dc3569-5b76-4c65-aaf9-eafcc3fd2412', 'facaa61b-0a1f-4925-b84c-4b6911e2e4d9', true, 1800, NOW() - interval '9 days'),
    ('7ef6c38e-01ff-4569-9880-d3b58aaf6f5b', '344dd3a6-67bd-4e22-9a51-f7173e26415e', '26dc3569-5b76-4c65-aaf9-eafcc3fd2412', '6dc5a0c9-95eb-44da-bfe9-0c23bb72cccb', true, 1800, NOW() - interval '0 days'),
    ('62586c52-9f72-447d-9e9f-3d5e41ae7b7d', '15e0592a-7870-486d-82e1-5832a1bcd3e5', '5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', '2bd99181-f697-4d31-ae60-804d147f839d', false, 600, NOW() - interval '9 days'),
    ('b6e43ce4-ecce-4eb7-bcbf-bbd0e05f3dc1', '15e0592a-7870-486d-82e1-5832a1bcd3e5', '5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', 'd6d62106-90da-4e33-a466-b79cab239ae6', false, 600, NOW() - interval '0 days'),
    ('69ee8d16-a17c-41d7-b715-2657020614dd', '15e0592a-7870-486d-82e1-5832a1bcd3e5', '5e9cf1e1-4eb4-48ca-ab5c-49344462a6b0', '43d99c17-166f-4d3c-930b-08210b962753', false, 600, NOW() - interval '1 days'),
    ('2fb3d2b3-e2d7-4e26-abd7-daeb9e951dd7', 'ff33c60a-2dff-4f9f-8a1c-280f672f2310', '98da37df-6ea3-439a-a76b-356a87e3c39b', '48e628f9-490b-4037-b999-a35e6b201395', true, 1800, NOW() - interval '0 days'),
    ('80b47e2c-963c-4ae6-b8be-468708b83ae4', 'ff33c60a-2dff-4f9f-8a1c-280f672f2310', '98da37df-6ea3-439a-a76b-356a87e3c39b', '7dd51142-3406-4e27-9612-9facee4bd0f9', true, 1800, NOW() - interval '1 days'),
    ('4c24860a-abbc-44db-9957-193e4d5e305c', 'ff33c60a-2dff-4f9f-8a1c-280f672f2310', '98da37df-6ea3-439a-a76b-356a87e3c39b', '47ff5347-c81c-4e4e-a0c6-b6f8f2e87e08', true, 1800, NOW() - interval '2 days'),
    ('0b745994-e4ce-48dd-b014-7846692ca546', '672c060f-3104-4215-ad0a-56761c466bac', '55c98170-fe5f-49bf-af88-812dc97231fa', '1c885679-a16f-46b2-9939-85b8f9228afb', true, 1800, NOW() - interval '1 days'),
    ('232d04ec-4f01-4dcb-882c-2f1b21328aaa', '672c060f-3104-4215-ad0a-56761c466bac', '55c98170-fe5f-49bf-af88-812dc97231fa', '5e2915fb-3dc5-4671-ad36-91fa0343a3b7', false, 600, NOW() - interval '2 days'),
    ('654fbd9e-b92c-4f1b-ba75-64fa45220110', '672c060f-3104-4215-ad0a-56761c466bac', '55c98170-fe5f-49bf-af88-812dc97231fa', 'f33ef755-80af-4d45-904a-6f430d6c95c4', false, 600, NOW() - interval '3 days'),
    ('a3c74fdc-7015-4bc4-8973-e024239d4e72', '9e4ba31d-7f76-4208-a583-fd2acfb71abf', 'a68f2877-ef24-461c-abbf-090bb2a7c579', '43251dc6-d585-449f-832e-77226b893ef9', false, 600, NOW() - interval '2 days'),
    ('edf8aa62-322b-4d59-be99-d5da7c205898', '9e4ba31d-7f76-4208-a583-fd2acfb71abf', 'a68f2877-ef24-461c-abbf-090bb2a7c579', '6965dd13-f4cb-4cf2-a33d-eb3ab8624c2a', false, 600, NOW() - interval '3 days'),
    ('9e82dcb1-ce15-48b8-8561-77bb6245d35d', '9e4ba31d-7f76-4208-a583-fd2acfb71abf', 'a68f2877-ef24-461c-abbf-090bb2a7c579', '2f2e841f-dd39-49de-a0b5-f8b6164cc73e', false, 600, NOW() - interval '4 days'),
    ('73b12a4c-d9ff-429a-b74e-215e7498de85', '0c059763-2d55-4714-995c-b2732557bb2e', 'f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'e88a86e3-3954-46ed-9dbd-35f38657bc16', true, 1800, NOW() - interval '3 days'),
    ('854ec977-be8c-4a88-a764-7385d78087df', '0c059763-2d55-4714-995c-b2732557bb2e', 'f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', '3cd271c7-57d4-4087-9f44-7a0aca35c715', false, 600, NOW() - interval '4 days'),
    ('7fb7881c-7624-4c13-a487-8fe2ec4a4063', '0c059763-2d55-4714-995c-b2732557bb2e', 'f79a2b3d-98dc-4b54-aaa7-e00cd4ab7913', 'bf0d57e3-ab80-4f2d-a4b6-53830a668f42', false, 600, NOW() - interval '5 days'),
    ('30cecae4-e54f-4984-ab04-956b0264619b', 'c43451c3-e782-4c35-adce-9bb00abbb486', '79518343-7d11-463f-a6ef-b199d83106da', '4d237ab0-ae76-4ad4-b9fc-c8535c94218a', true, 1800, NOW() - interval '4 days'),
    ('c959894c-404f-43ac-a20f-7916d2d6ad9d', 'c43451c3-e782-4c35-adce-9bb00abbb486', '79518343-7d11-463f-a6ef-b199d83106da', 'd1f6b1ed-8d95-4367-b4c4-86ee21d485ad', true, 1800, NOW() - interval '5 days'),
    ('87f10897-e750-4f53-95d9-8145960efec6', 'c43451c3-e782-4c35-adce-9bb00abbb486', '79518343-7d11-463f-a6ef-b199d83106da', '7f14e31f-29df-4e91-b66d-3ff72c935390', true, 1800, NOW() - interval '6 days'),
    ('96fc965e-3ca5-4614-bcef-73fa2cbf818b', '85ca09c7-1a7f-4306-8342-4bcc2571f2f4', 'f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', 'e893031b-b70e-42ad-b73b-5eea4fa8990c', false, 600, NOW() - interval '5 days'),
    ('96da3a55-d0e6-479d-a081-74cd7c01ef3e', '85ca09c7-1a7f-4306-8342-4bcc2571f2f4', 'f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', '031b70bb-b442-4ed0-a907-9ee058553d21', false, 600, NOW() - interval '6 days'),
    ('b38d4367-3f65-4844-9e7c-1ffc9ac87502', '85ca09c7-1a7f-4306-8342-4bcc2571f2f4', 'f0307b7e-fad9-4a3e-a842-24dfdebb2c8e', '3d29fb9e-7ad1-4806-b2c3-fd5dbdf472ce', false, 600, NOW() - interval '7 days'),
    ('80fc2822-f4fc-49a8-82a8-ffc2959c51af', 'dfcb07db-5d83-43b8-a58b-74e506aeede1', 'e3674d01-28c5-4ffb-a190-3ae044bf71ba', '4de21448-5c86-4e4e-80a2-05359ed85b11', true, 1800, NOW() - interval '6 days'),
    ('13ebb215-382b-4e09-acce-74167302481b', 'dfcb07db-5d83-43b8-a58b-74e506aeede1', 'e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'e87c0d9c-44d5-487d-9d4c-571140fc2d66', true, 1800, NOW() - interval '7 days'),
    ('4b349ff6-a1f3-499a-a40f-5c159271598c', 'dfcb07db-5d83-43b8-a58b-74e506aeede1', 'e3674d01-28c5-4ffb-a190-3ae044bf71ba', 'a5a46664-e5e2-4b6c-b251-5f0f283f9a38', true, 1800, NOW() - interval '8 days'),
    ('29575b26-8687-4787-a9eb-1ac4bba20ac1', 'f135c677-3145-43a2-86fd-eebc4762de18', '754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', '3a3321ec-3382-4d07-b592-6332a1b096e4', true, 1800, NOW() - interval '7 days'),
    ('9a6e8190-8599-4b1a-b7ad-d45508193ed6', 'f135c677-3145-43a2-86fd-eebc4762de18', '754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', 'b2c7014e-cf31-47c2-9936-b39166f459d0', false, 600, NOW() - interval '8 days'),
    ('f7c6da44-6481-4484-bb4f-719e33c8f3a6', 'f135c677-3145-43a2-86fd-eebc4762de18', '754f9f7b-c58b-4ad4-a758-a70a8ba18fe5', '24442a86-ad51-4945-88fd-e7057f51d14e', false, 600, NOW() - interval '9 days'),
    ('54a0b53d-5f04-451a-ac41-20ab690f8797', 'b8bd9c66-a14f-4546-8462-ae9d8f2aedd6', '172c2f01-2008-4e41-a7cb-12a83c6fd75f', '16e621db-bbd8-4650-ac6f-f30cc1356857', false, 600, NOW() - interval '8 days'),
    ('0b768ee1-8a57-4935-ad81-d32f5343ec65', 'b8bd9c66-a14f-4546-8462-ae9d8f2aedd6', '172c2f01-2008-4e41-a7cb-12a83c6fd75f', '8dc8c301-e387-49a7-b1e3-1d374e92253a', false, 600, NOW() - interval '9 days'),
    ('838d465b-9b96-4610-ae5a-e1c5c1e10621', 'b8bd9c66-a14f-4546-8462-ae9d8f2aedd6', '172c2f01-2008-4e41-a7cb-12a83c6fd75f', '891995ad-2c74-4170-b635-20af72e533fe', false, 600, NOW() - interval '0 days'),
    ('79279c4e-9247-4255-9344-76678027ff72', '3ca13e2d-e9ab-42f6-8f89-fa49792f1fbf', '1a56c767-f6f9-4bd7-aced-b8edca9dffbb', '771cb6c7-aa59-469e-8561-6967881d6039', true, 1800, NOW() - interval '9 days'),
    ('899e165f-abed-4adf-8519-c495f9ed9004', '3ca13e2d-e9ab-42f6-8f89-fa49792f1fbf', '1a56c767-f6f9-4bd7-aced-b8edca9dffbb', 'ca86fdc0-2f1d-44b8-8658-e8d12520efd7', false, 600, NOW() - interval '0 days'),
    ('a36aba00-a3e5-465e-a295-109ec7395816', '3ca13e2d-e9ab-42f6-8f89-fa49792f1fbf', '1a56c767-f6f9-4bd7-aced-b8edca9dffbb', '8cb63ebe-f54f-4fff-ad85-7443fd6b57ca', false, 600, NOW() - interval '1 days')
ON CONFLICT (enrollment_id, lesson_id) DO NOTHING;

-- 7. 25 Course Reviews
INSERT INTO course_reviews (id, course_id, user_id, rating, comment, created_at) VALUES
    ('71d00bee-5883-456d-af04-5d799c4d47fa', '22222222-2222-2222-2222-222222222201', 'ed854c07-f3eb-48f1-a186-4e7135f54645', 4, 'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!', NOW() - interval '0 days'),
    ('45eb4c95-1a8b-4967-b5f5-4bd916c4ceff', '22222222-2222-2222-2222-222222222207', '797c5663-386b-4e96-9112-6d3e414b4449', 5, 'Giảng viên giải thích rõ ràng, phần thực hành Microservices cực kỳ hữu ích.', NOW() - interval '1 days'),
    ('55b5c3d5-e9b0-488d-8120-7fa594640655', '22222222-2222-2222-2222-222222222208', '4a09b051-d7ab-40a5-8322-963ec9ebdbd7', 5, 'Nội dung cô đọng, giúp mình lấp nhanh khoảng trống kỹ năng sau đánh giá.', NOW() - interval '2 days'),
    ('1cdf1bc5-d94a-49bb-ae4f-4a86c2c5355e', '22222222-2222-2222-2222-222222222209', 'f848319f-9e6e-4815-a697-2fe2400fd32f', 5, 'Ví dụ trực quan và dễ áp dụng ngay vào dự án IOC.', NOW() - interval '3 days'),
    ('1c42bdb3-7f60-4189-b3ce-04d77ea025a9', '22222222-2222-2222-2222-222222222210', '6257a31f-e618-4ec1-ad25-67574b41a04e', 4, 'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!', NOW() - interval '4 days'),
    ('e8731777-509a-4ddf-aac5-46c1494bda27', '22222222-2222-2222-2222-222222222202', 'c633fa6f-51e7-4257-a13b-9bc051acab55', 5, 'Giảng viên giải thích rõ ràng, phần thực hành Microservices cực kỳ hữu ích.', NOW() - interval '5 days'),
    ('8089aff1-e22a-47d3-b941-98a9feb188d4', '22222222-2222-2222-2222-222222222211', '41c15f25-065b-4c8c-a3b6-ed4eab80c201', 5, 'Nội dung cô đọng, giúp mình lấp nhanh khoảng trống kỹ năng sau đánh giá.', NOW() - interval '6 days'),
    ('24613ef0-6e24-45a8-8137-6c165925ecc6', '22222222-2222-2222-2222-222222222212', '7d26abe3-98c9-4af8-a904-e57f81783d54', 5, 'Ví dụ trực quan và dễ áp dụng ngay vào dự án IOC.', NOW() - interval '7 days'),
    ('9c872b16-2fa6-429d-a3e7-9fbc67342679', '22222222-2222-2222-2222-222222222213', '6f7d791c-8ef0-4404-ab53-5871eb562621', 4, 'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!', NOW() - interval '8 days'),
    ('32823374-2df7-4492-83f1-e8f1a0c1f3b5', '22222222-2222-2222-2222-222222222214', '1c1cae0c-2691-4876-a05e-e1c133a1f013', 5, 'Giảng viên giải thích rõ ràng, phần thực hành Microservices cực kỳ hữu ích.', NOW() - interval '9 days'),
    ('231b2a4d-8d9e-45dc-a78a-8d759665affd', '22222222-2222-2222-2222-222222222204', '84e1f7fb-48da-480c-ab2a-799f6d931982', 5, 'Nội dung cô đọng, giúp mình lấp nhanh khoảng trống kỹ năng sau đánh giá.', NOW() - interval '10 days'),
    ('4cc7e796-bc97-44db-a478-85a9e2792cc3', '22222222-2222-2222-2222-222222222215', '47687ff7-7211-405f-a081-230fad46f127', 5, 'Ví dụ trực quan và dễ áp dụng ngay vào dự án IOC.', NOW() - interval '11 days'),
    ('aef97390-d3fe-4cd2-a65d-6320563edd38', '22222222-2222-2222-2222-222222222216', 'a3692669-b304-40c2-a999-50695e5fedb6', 4, 'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!', NOW() - interval '0 days'),
    ('bd88c664-f28a-4d22-b50b-69995df68b9a', '22222222-2222-2222-2222-222222222203', 'f0c71f68-3609-42b6-a61f-e677da5ac4e5', 5, 'Giảng viên giải thích rõ ràng, phần thực hành Microservices cực kỳ hữu ích.', NOW() - interval '1 days'),
    ('5a10b4b7-ede7-4cf4-a700-5ac0172ae824', '22222222-2222-2222-2222-222222222217', '988e0870-4ce5-472d-a17d-44c17fa0f9a1', 5, 'Nội dung cô đọng, giúp mình lấp nhanh khoảng trống kỹ năng sau đánh giá.', NOW() - interval '2 days'),
    ('6dd53bae-037f-4153-9e00-1d0e988c24b9', '22222222-2222-2222-2222-222222222218', '56de20bd-ad10-4cd8-a732-7d41b40b6734', 5, 'Ví dụ trực quan và dễ áp dụng ngay vào dự án IOC.', NOW() - interval '3 days'),
    ('7fea4abf-5081-45b6-8c5d-cdf72af238f2', '22222222-2222-2222-2222-222222222205', '7b23aa92-148f-4b72-ab21-5b9f69e2b159', 4, 'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!', NOW() - interval '4 days'),
    ('938ce1b1-b2a5-471e-a902-72c928d19bc1', '22222222-2222-2222-2222-222222222219', '984682cd-5097-4e07-aea4-7f6e423a80d5', 5, 'Giảng viên giải thích rõ ràng, phần thực hành Microservices cực kỳ hữu ích.', NOW() - interval '5 days'),
    ('d5871306-9384-4eed-b0d3-61dcd70bce13', '22222222-2222-2222-2222-222222222201', '0539425d-c9ea-42ad-a526-af16615ee51e', 5, 'Nội dung cô đọng, giúp mình lấp nhanh khoảng trống kỹ năng sau đánh giá.', NOW() - interval '6 days'),
    ('02b0e23c-d93b-4f79-b933-7cc6a5b1eb71', '22222222-2222-2222-2222-222222222207', '4b20ea51-eba8-4087-a312-b142beefc5b7', 5, 'Ví dụ trực quan và dễ áp dụng ngay vào dự án IOC.', NOW() - interval '7 days'),
    ('ba8220ad-9e0f-4419-9d26-f1a22adda9de', '22222222-2222-2222-2222-222222222208', '0ae6c4c0-3ee7-4148-a529-49e38af22bb9', 4, 'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!', NOW() - interval '8 days'),
    ('823dfa43-a4fa-4e54-ad01-cf851290fe1b', '22222222-2222-2222-2222-222222222209', '550df6cc-fd53-40e6-a44e-ad0d09db70fd', 5, 'Giảng viên giải thích rõ ràng, phần thực hành Microservices cực kỳ hữu ích.', NOW() - interval '9 days'),
    ('d9f694e0-910b-4ece-b296-1abbad814781', '22222222-2222-2222-2222-222222222210', '315a14b5-f83c-42bc-a038-39cf8340bf3a', 5, 'Nội dung cô đọng, giúp mình lấp nhanh khoảng trống kỹ năng sau đánh giá.', NOW() - interval '10 days'),
    ('b3cbf01c-d34d-41ac-bb94-6037f8ee18b5', '22222222-2222-2222-2222-222222222202', 'be569c29-d7e0-482e-a485-654f2511098e', 5, 'Ví dụ trực quan và dễ áp dụng ngay vào dự án IOC.', NOW() - interval '11 days'),
    ('44e5233f-74a0-4fb1-b470-9a226f38710c', '22222222-2222-2222-2222-222222222211', '85763110-0099-4a4c-a7fc-fb5ebbcc3ae9', 4, 'Khóa học rất thực tế, bài tập sát với phỏng vấn doanh nghiệp!', NOW() - interval '0 days')
ON CONFLICT (course_id, user_id) DO NOTHING;


-- ==============================================================================
-- DT-04 SEED DATA & INDEXES: Bảng candidate_profiles và chỉ mục truy vấn gộp
-- ==============================================================================

CREATE TABLE IF NOT EXISTS candidate_profiles (
    user_id UUID PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(200),
    phone VARCHAR(50),
    class_code VARCHAR(50),
    course_name VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_enrollments_status ON enrollments(status);
CREATE INDEX IF NOT EXISTS idx_candidate_profiles_class_course ON candidate_profiles(class_code, course_name);

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
