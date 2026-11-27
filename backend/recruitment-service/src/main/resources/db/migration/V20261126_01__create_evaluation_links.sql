-- V20261126_01__create_evaluation_links.sql
CREATE TABLE evaluation_links (
                                  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  token_hash    VARCHAR(64)  NOT NULL UNIQUE,
                                  mentor_email  VARCHAR(255) NOT NULL,
                                  user_id       UUID         NOT NULL,
                                  batch_id      UUID         NOT NULL,
                                  evaluation_id UUID         NULL,
                                  expires_at    TIMESTAMPTZ  NOT NULL,
                                  used_at       TIMESTAMPTZ  NULL,
                                  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_evaluation_links_token_hash ON evaluation_links (token_hash);
CREATE INDEX idx_evaluation_links_mentor_email ON evaluation_links (mentor_email);
CREATE INDEX idx_evaluation_links_user_batch ON evaluation_links (user_id, batch_id);