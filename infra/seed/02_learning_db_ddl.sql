-- =============================================================================
-- Learning DB: Courses & Course Skill Map
-- =============================================================================
-- Run against learning_db

CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    level VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS course_skill_map (
    course_id UUID NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    weight NUMERIC(3,2) NOT NULL CHECK (weight > 0 AND weight <= 1),
    PRIMARY KEY (course_id, skill_id),
    CONSTRAINT fk_course_skill_map_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_skill_map_course ON course_skill_map(course_id);

-- Seed: 6 courses
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

INSERT INTO course_skill_map (course_id, skill_id, weight) VALUES
('22222222-2222-2222-2222-222222222201', 'SPRING_BOOT', 0.35),
('22222222-2222-2222-2222-222222222201', 'MICROSERVICES', 0.30),
('22222222-2222-2222-2222-222222222201', 'DOCKER', 0.20),
('22222222-2222-2222-2222-222222222201', 'APACHE_KAFKA', 0.15),
('22222222-2222-2222-2222-222222222202', 'REACT', 0.40),
('22222222-2222-2222-2222-222222222202', 'TYPESCRIPT', 0.35),
('22222222-2222-2222-2222-222222222202', 'TAILWIND_CSS', 0.25),
('22222222-2222-2222-2222-222222222203', 'SQL', 0.50),
('22222222-2222-2222-2222-222222222203', 'POSTGRESQL', 0.30),
('22222222-2222-2222-2222-222222222203', 'DATABASE_OPTIMIZATION', 0.20),
('22222222-2222-2222-2222-222222222204', 'DOCKER', 0.40),
('22222222-2222-2222-2222-222222222204', 'CI_CD', 0.35),
('22222222-2222-2222-2222-222222222204', 'LINUX_BASH', 0.25),
('22222222-2222-2222-2222-222222222205', 'TESTING', 0.50),
('22222222-2222-2222-2222-222222222205', 'JAVA_CORE', 0.30),
('22222222-2222-2222-2222-222222222205', 'CI_CD', 0.20),
('22222222-2222-2222-2222-222222222206', 'PYTHON', 0.60),
('22222222-2222-2222-2222-222222222206', 'SQL', 0.40)
ON CONFLICT (course_id, skill_id) DO NOTHING;