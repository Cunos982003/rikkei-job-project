CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id),
    title VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    city VARCHAR(100),
    address VARCHAR(500),
    work_mode VARCHAR(20) NOT NULL,
    salary_min NUMERIC(15,0),
    salary_max NUMERIC(15,0),
    description TEXT,
    requirements TEXT,
    benefits TEXT,
    deadline DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    rejected_reason VARCHAR(1000),
    created_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jobs_company_status ON jobs(company_id, status);

CREATE TABLE job_skills (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_id VARCHAR(64) NOT NULL,
    skill_code VARCHAR(64),
    skill_name VARCHAR(255) NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    min_score INT NOT NULL DEFAULT 0,
    PRIMARY KEY (job_id, skill_id)
);

CREATE INDEX idx_job_skills_job ON job_skills(job_id);

