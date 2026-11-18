-- ==============================================================================
-- V2__add_offers_and_employment_records.sql: [UT-07] Offer & Employment Records
-- ==============================================================================

CREATE TABLE IF NOT EXISTS offers (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE REFERENCES recruitments(id) ON DELETE CASCADE,
    company_name VARCHAR(255),
    position VARCHAR(255) NOT NULL,
    salary NUMERIC(15, 2) NOT NULL CHECK (salary > 0),
    start_date DATE NOT NULL,
    work_location VARCHAR(500) NOT NULL,
    note TEXT,
    response_deadline TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'ACCEPTED', 'DECLINED', 'EXPIRED')),
    responded_at TIMESTAMPTZ,
    decline_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_offers_status_deadline ON offers(status, response_deadline);

CREATE TABLE IF NOT EXISTS employment_records (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    company_name VARCHAR(255),
    job_id UUID NOT NULL,
    application_id UUID UNIQUE REFERENCES recruitments(id) ON DELETE SET NULL,
    position VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_employment_records_user ON employment_records(user_id);
CREATE INDEX IF NOT EXISTS idx_employment_records_company ON employment_records(company_id);
