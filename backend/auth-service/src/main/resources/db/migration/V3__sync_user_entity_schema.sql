-- Migration V3: Synchronize users table with updated User entity
-- Preserves existing data by renaming columns if they exist, or adding them

DO $$
BEGIN
    -- Rename password to password_hash if password exists and password_hash does not
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='password')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='password_hash') THEN
        ALTER TABLE users RENAME COLUMN password TO password_hash;
    ELSE
        ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
    END IF;

    -- Rename failed_login_attempts to failed_login_count if it exists
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='failed_login_attempts')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='failed_login_count') THEN
        ALTER TABLE users RENAME COLUMN failed_login_attempts TO failed_login_count;
    ELSE
        ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_count INT NOT NULL DEFAULT 0;
    END IF;
END $$;

-- Add new columns: company_id, must_change_password, status
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_id UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Create helpful indexes
CREATE INDEX IF NOT EXISTS idx_users_company_id ON users (company_id);
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);

