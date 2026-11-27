-- =============================================================================
-- Assessment DB: Skill Assessments & Tests
-- =============================================================================
-- Run against assessment_db

CREATE TABLE IF NOT EXISTS assessment_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    duration_minutes INT,
    max_score SMALLINT DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_id UUID NOT NULL REFERENCES assessment_types(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,
    passing_score SMALLINT DEFAULT 70,
    time_limit_minutes INT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_assessments_type ON assessments(type_id);
CREATE INDEX IF NOT EXISTS idx_assessments_active ON assessments(is_active);

CREATE TABLE IF NOT EXISTS assessment_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL CHECK (question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'SHORT_ANSWER', 'CODE')),
    options JSONB,
    correct_answer JSONB,
    explanation TEXT,
    points SMALLINT DEFAULT 1,
    order_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_assessment_questions_assessment ON assessment_questions(assessment_id);

CREATE TABLE IF NOT EXISTS assessment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assessment_id UUID NOT NULL REFERENCES assessments(id),
    user_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    score SMALLINT,
    max_score SMALLINT,
    percentage DECIMAL(5,2),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED', 'EXPIRED')),
    answers JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_assessment_attempts_user ON assessment_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_assessment_attempts_assessment ON assessment_attempts(assessment_id);
CREATE INDEX IF NOT EXISTS idx_assessment_attempts_status ON assessment_attempts(status);

-- ==============================================================================
-- SEED DATA
-- ==============================================================================

INSERT INTO assessment_types (id, code, name, description, duration_minutes, max_score) VALUES
('a1000000-0000-0000-0000-000000000001', 'JAVA_CORE', 'Java Core Assessment', 'Đánh giá kiến thức Java Core cơ bản đến nâng cao', 60, 100),
('a1000000-0000-0000-0000-000000000002', 'SPRING_BOOT', 'Spring Boot Assessment', 'Đánh giá kỹ năng Spring Boot, Spring Data, Spring Security', 90, 100),
('a1000000-0000-0000-0000-000000000003', 'SQL', 'SQL Assessment', 'Đánh giá truy vấn SQL, thiết kế DB, tối ưu hóa', 45, 100),
('a1000000-0000-0000-0000-000000000004', 'REACT', 'React Assessment', 'Đánh giá React, Hooks, State Management, TypeScript', 60, 100),
('a1000000-0000-0000-0000-000000000005', 'FULLSTACK', 'Fullstack Assessment', 'Đánh giá toàn diện Java + React + DB + DevOps', 120, 100),
('a1000000-0000-0000-0000-000000000006', 'DEVOPS', 'DevOps Assessment', 'Đánh giá Docker, Kubernetes, CI/CD, Cloud', 60, 100)
ON CONFLICT (code) DO NOTHING;

INSERT INTO assessments (id, type_id, title, description, instructions, passing_score, time_limit_minutes, is_active, created_by, created_at) VALUES
(gen_random_uuid(), 'a1000000-0000-0000-0000-000000000001', 'Java Core Fundamentals', 'Kiểm tra kiến thức Java Core cơ bản', 'Trả lời 20 câu hỏi trong 60 phút', 70, 60, true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '30 days'),
(gen_random_uuid(), 'a1000000-0000-0000-0000-000000000001', 'Java Advanced Concepts', 'Kiểm tra Java nâng cao: Concurrency, JVM, Design Patterns', 'Trả lời 15 câu hỏi trong 90 phút', 75, 90, true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '25 days'),
(gen_random_uuid(), 'a1000000-0000-0000-0000-000000000002', 'Spring Boot Professional', 'Kiểm tra Spring Boot, JPA, Security, Testing', 'Trả lời 25 câu hỏi trong 90 phút', 70, 90, true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '20 days'),
(gen_random_uuid(), 'a1000000-0000-0000-0000-000000000003', 'SQL Mastery', 'Kiểm tra SQL nâng cao: Window functions, CTE, Index, Execution Plan', 'Trả lời 20 câu hỏi trong 45 phút', 80, 45, true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '15 days'),
(gen_random_uuid(), 'a1000000-0000-0000-0000-000000000004', 'React & TypeScript', 'Kiểm tra React 18, Hooks, Redux/Zustand, TypeScript', 'Trả lời 20 câu hỏi trong 60 phút', 70, 60, true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '10 days'),
(gen_random_uuid(), 'a1000000-0000-0000-0000-000000000005', 'Fullstack Capstone', 'Đánh giá toàn diện dự án Fullstack Java + React', 'Hoàn thành bài tập thực hành trong 120 phút', 75, 120, true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now() - interval '5 days'),
(gen_random_uuid(), 'a1000000-0000-0000-0000-000000000006', 'DevOps Fundamentals', 'Kiểm tra Docker, K8s, CI/CD, Monitoring', 'Trả lời 15 câu hỏi trong 60 phút', 70, 60, true, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', now())
ON CONFLICT (id) DO NOTHING;

-- Sample questions for first assessment
INSERT INTO assessment_questions (id, assessment_id, question_text, question_type, options, correct_answer, explanation, points, order_index) VALUES
(gen_random_uuid(), (SELECT id FROM assessments WHERE title='Java Core Fundamentals' LIMIT 1), 'Đâu là từ khóa để khai báo hằng số trong Java?', 'SINGLE_CHOICE', '["const", "final", "static", "immutable"]', '"final"', 'Từ khóa final dùng để khai báo hằng số', 1, 1),
(gen_random_uuid(), (SELECT id FROM assessments WHERE title='Java Core Fundamentals' LIMIT 1), 'Collection nào không cho phép phần tử trùng lặp?', 'SINGLE_CHOICE', '["List", "Set", "Queue", "Deque"]', '"Set"', 'Set là collection không cho phép trùng lặp', 1, 2),
(gen_random_uuid(), (SELECT id FROM assessments WHERE title='Java Core Fundamentals' LIMIT 1), 'Interface nào kế thừa từ Collection?', 'MULTIPLE_CHOICE', '["List", "Set", "Map", "Queue"]', '["List", "Set", "Queue"]', 'List, Set, Queue kế thừa từ Collection. Map không kế thừa từ Collection', 1, 3),
(gen_random_uuid(), (SELECT id FROM assessments WHERE title='Java Core Fundamentals' LIMIT 1), 'Java có hỗ trợ đa kế thừa không?', 'TRUE_FALSE', '["true", "false"]', '"false"', 'Java không hỗ trợ đa kế thừa cho class, nhưng interface có thể extends nhiều interface', 1, 4),
(gen_random_uuid(), (SELECT id FROM assessments WHERE title='Java Core Fundamentals' LIMIT 1), 'Viết code tạo một ArrayList chứa các số nguyên từ 1 đến 5', 'CODE', '{}', '{"code": "List<Integer> list = new ArrayList<>();\nfor (int i = 1; i <= 5; i++) {\n    list.add(i);\n}"}', 'Sử dụng vòng lặp for để thêm phần tử vào ArrayList', 2, 5)
ON CONFLICT (id) DO NOTHING;