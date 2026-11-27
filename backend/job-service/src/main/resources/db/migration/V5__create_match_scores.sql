CREATE TABLE IF NOT EXISTS match_scores (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    score NUMERIC(5,1) NOT NULL,
    below_minimum BOOLEAN NOT NULL DEFAULT FALSE,
    detail TEXT,
    stale BOOLEAN NOT NULL DEFAULT FALSE,
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (job_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_match_scores_user ON match_scores(user_id);

