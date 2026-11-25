-- =============================================================================
-- V1__init_ai_db.sql : AI CAREER SERVICE INITIAL SCHEMA
-- =============================================================================

CREATE TABLE IF NOT EXISTS job_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    job_id UUID NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    explanation VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    input_hash VARCHAR(64) NOT NULL,
    CONSTRAINT uk_job_recommendations_user_job UNIQUE (user_id, job_id)
);

CREATE TABLE IF NOT EXISTS interview_practices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    job_id UUID NOT NULL,
    job_title VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    overall_score DOUBLE PRECISION,
    summary TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS practice_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    practice_id UUID NOT NULL REFERENCES interview_practices(id) ON DELETE CASCADE,
    q_order INT NOT NULL,
    question TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    answer TEXT,
    score INT,
    feedback TEXT,
    suggestion TEXT
);

CREATE INDEX IF NOT EXISTS idx_job_recommendations_user_id ON job_recommendations(user_id);
CREATE INDEX IF NOT EXISTS idx_interview_practices_user_id ON interview_practices(user_id);
CREATE INDEX IF NOT EXISTS idx_practice_questions_practice_id ON practice_questions(practice_id);
