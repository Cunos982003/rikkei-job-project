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

