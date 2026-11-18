-- ==============================================================================
-- V4__init_dt03_learning_paths.sql: Khởi tạo bảng cho Lộ trình học bổ sung (DT-03)
-- ==============================================================================

-- 1. Bảng lưu lộ trình học tập của ứng viên (Learning Paths)
CREATE TABLE IF NOT EXISTS learning_paths (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    readiness_at_start NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_learning_path_target_type CHECK (target_type IN ('POSITION', 'JOB')),
    CONSTRAINT chk_learning_path_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_learning_path_readiness CHECK (readiness_at_start >= 0.00 AND readiness_at_start <= 100.00)
);

CREATE INDEX IF NOT EXISTS idx_learning_paths_user_id ON learning_paths(user_id);
CREATE INDEX IF NOT EXISTS idx_learning_paths_user_status ON learning_paths(user_id, status);

-- 2. Bảng lưu các bước học tập trong lộ trình (Learning Path Steps)
CREATE TABLE IF NOT EXISTS learning_path_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    path_id UUID NOT NULL,
    step_order INT NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    lms_course_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    done_at TIMESTAMPTZ,
    CONSTRAINT fk_learning_path_steps_path FOREIGN KEY (path_id) REFERENCES learning_paths(id) ON DELETE CASCADE,
    CONSTRAINT chk_step_status CHECK (status IN ('TODO', 'DOING', 'DONE')),
    CONSTRAINT chk_step_order CHECK (step_order > 0),
    CONSTRAINT uq_path_step_order UNIQUE (path_id, step_order)
);

CREATE INDEX IF NOT EXISTS idx_learning_path_steps_path_id ON learning_path_steps(path_id);
CREATE INDEX IF NOT EXISTS idx_learning_path_steps_path_order ON learning_path_steps(path_id, step_order);

