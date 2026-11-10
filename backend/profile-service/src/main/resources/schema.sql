CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    "group" VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS skill_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(20) NOT NULL CHECK (source IN ('LMS','EXAM','PM')),
    source_code VARCHAR(50) NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    weight NUMERIC(3,2) NOT NULL CHECK (weight > 0 AND weight <= 1),
    UNIQUE (source, source_code, skill_id)
);

CREATE TABLE IF NOT EXISTS skill_evidences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    source VARCHAR(20) NOT NULL CHECK (source IN ('LMS','EXAM','PM','INTERNSHIP','MENTOR','SELF')),
    source_ref VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    score SMALLINT NOT NULL CHECK (score BETWEEN 0 AND 100),
    recorded_at TIMESTAMPTZ NOT NULL,
    verified BOOLEAN NOT NULL,
    UNIQUE (user_id, skill_id, source, source_ref)
);

CREATE TABLE IF NOT EXISTS skill_scores (
    user_id UUID NOT NULL,
    skill_id UUID NOT NULL REFERENCES skills(id),
    verified_score SMALLINT,
    self_score SMALLINT,
    level VARCHAR(20) NOT NULL CHECK (level IN ('Cơ bản','Khá','Vững','Nâng cao')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_skill_evidences_user_skill ON skill_evidences(user_id, skill_id);
CREATE INDEX IF NOT EXISTS idx_skill_mappings_source_code ON skill_mappings(source, source_code);