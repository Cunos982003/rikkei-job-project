-- Migration V2: Add status and account lockout columns to users table for HR management (DN-05)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS locked_reason TEXT;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users
    ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED'));

UPDATE users
SET status = 'ACTIVE'
WHERE status IS NULL;

CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

