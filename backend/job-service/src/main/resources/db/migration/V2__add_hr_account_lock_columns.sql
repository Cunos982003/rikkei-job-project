-- Migration V2: Add status and account lockout columns to users table for HR management (DN-05)
-- Guarded with to_regclass('users') so baseline-on-migrate environments (where V1 was skipped or users was dropped in V3) do not fail.

DO $$
BEGIN
    IF to_regclass('users') IS NOT NULL THEN
        ALTER TABLE users
            ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

        ALTER TABLE users
            ADD COLUMN IF NOT EXISTS locked_reason TEXT;

        ALTER TABLE users
            ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP WITH TIME ZONE;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'chk_users_status'
              AND conrelid = to_regclass('users')
        ) THEN
            ALTER TABLE users
                ADD CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'LOCKED'));
        END IF;

        UPDATE users
        SET status = 'ACTIVE'
        WHERE status IS NULL;

        CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
    END IF;
END $$;

