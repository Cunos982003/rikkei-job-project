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

