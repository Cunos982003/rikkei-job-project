CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    lms_user_id VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS used_sso_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket VARCHAR(255) NOT NULL UNIQUE,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_lms_user_id ON users (lms_user_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_used_tickets_ticket ON used_sso_tickets (ticket);
