-- DG-01 / DG-04: mentor evaluations (DRAFT / SUBMITTED)
CREATE TABLE evaluation_criteria (
    id              UUID         NOT NULL PRIMARY KEY,
    context         VARCHAR(32)  NOT NULL,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    sort_order      INT          NOT NULL DEFAULT 0,
    CONSTRAINT uk_evaluation_criteria_context_code UNIQUE (context, code),
    CONSTRAINT ck_evaluation_criteria_context CHECK (context IN ('INTERNSHIP', 'INTERVIEW'))
);

CREATE TABLE internship_batches (
    id          UUID         NOT NULL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    starts_on   DATE,
    ends_on     DATE
);

-- TT-01 fields needed by DG-04 (local read model until recruitment owns IOC)
CREATE TABLE internships (
    id                  UUID         NOT NULL PRIMARY KEY,
    batch_id            UUID         NOT NULL REFERENCES internship_batches (id),
    candidate_id        UUID         NOT NULL,
    candidate_name      VARCHAR(255) NOT NULL,
    mentor_id           UUID         NOT NULL,
    mentor_email        VARCHAR(255) NOT NULL,
    due_at              TIMESTAMPTZ,
    skills_summary      TEXT,
    internship_tasks    TEXT
);

CREATE INDEX idx_internships_mentor_email ON internships (mentor_email);
CREATE INDEX idx_internships_batch_id ON internships (batch_id);

-- UT-02 fields needed by DG-04 (local read model until recruitment owns interviews)
CREATE TABLE interview_rounds (
    id                  UUID         NOT NULL PRIMARY KEY,
    batch_id            UUID         REFERENCES internship_batches (id),
    round_name          VARCHAR(255) NOT NULL,
    candidate_id        UUID         NOT NULL,
    candidate_name      VARCHAR(255) NOT NULL,
    interviewer_id      UUID         NOT NULL,
    interviewer_email   VARCHAR(255) NOT NULL,
    due_at              TIMESTAMPTZ,
    skills_summary      TEXT
);

CREATE INDEX idx_interview_rounds_interviewer_email ON interview_rounds (interviewer_email);
CREATE INDEX idx_interview_rounds_batch_id ON interview_rounds (batch_id);

CREATE TABLE evaluations (
    id               UUID         NOT NULL PRIMARY KEY,
    mentor_id        UUID         NOT NULL,
    mentor_email     VARCHAR(255) NOT NULL,
    context          VARCHAR(32)  NOT NULL,
    subject_id       UUID         NOT NULL,
    candidate_id     UUID         NOT NULL,
    candidate_name   VARCHAR(255) NOT NULL,
    batch_id         UUID,
    status           VARCHAR(32)  NOT NULL,
    overall_comment  TEXT,
    submitted_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_evaluations_context CHECK (context IN ('INTERNSHIP', 'INTERVIEW')),
    CONSTRAINT ck_evaluations_status CHECK (status IN ('DRAFT', 'SUBMITTED')),
    CONSTRAINT uk_evaluations_context_subject_mentor UNIQUE (context, subject_id, mentor_id)
);

CREATE INDEX idx_evaluations_mentor_status ON evaluations (mentor_id, status);

CREATE TABLE evaluation_item_scores (
    id              UUID NOT NULL PRIMARY KEY,
    evaluation_id   UUID NOT NULL REFERENCES evaluations (id) ON DELETE CASCADE,
    criterion_id    UUID NOT NULL REFERENCES evaluation_criteria (id),
    score           INT,
    comment         TEXT,
    CONSTRAINT uk_evaluation_item UNIQUE (evaluation_id, criterion_id),
    CONSTRAINT ck_evaluation_item_score CHECK (score IS NULL OR (score >= 0 AND score <= 100))
);
