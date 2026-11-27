-- =============================================================================
-- V3__ai_drafts.sql : AI CV DRAFTING (SUMMARY & PROJECT) WITH ANTI-HALLUCINATION
-- =============================================================================

CREATE TABLE IF NOT EXISTS ai_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    cv_id UUID NOT NULL,
    section VARCHAR(30) NOT NULL CHECK (section IN ('SUMMARY', 'PROJECT')),
    project_id UUID,
    target_job_id UUID,
    input_snapshot JSONB NOT NULL,
    output TEXT NOT NULL,
    warnings JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT', 'ACCEPTED', 'DISCARDED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_drafts_cv_user_created
    ON ai_drafts(cv_id, user_id, created_at DESC);
