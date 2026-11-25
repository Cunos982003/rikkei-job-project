-- UT-03 Contact Access Control
-- Contact requests from HR to access candidate contact info
CREATE TABLE IF NOT EXISTS contact_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    user_id UUID NOT NULL,          -- candidate user_id
    job_id UUID,                    -- optional, JD the HR is hiring for
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by UUID,               -- staff user_id who reviewed
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp()
);

-- Access logs - every time HR views contact info
CREATE TABLE IF NOT EXISTS contact_access_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    hr_id UUID NOT NULL,            -- HR user who viewed
    user_id UUID NOT NULL,          -- candidate whose contact was viewed
    via VARCHAR(30) NOT NULL CHECK (via IN ('APPLICATION', 'APPROVED_REQUEST')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_contact_requests_company ON contact_requests(company_id);
CREATE INDEX IF NOT EXISTS idx_contact_requests_user ON contact_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_contact_requests_job ON contact_requests(job_id);
CREATE INDEX IF NOT EXISTS idx_contact_requests_status ON contact_requests(status);
CREATE INDEX IF NOT EXISTS idx_contact_access_logs_company ON contact_access_logs(company_id);
CREATE INDEX IF NOT EXISTS idx_contact_access_logs_hr ON contact_access_logs(hr_id);
CREATE INDEX IF NOT EXISTS idx_contact_access_logs_user ON contact_access_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_contact_access_logs_created ON contact_access_logs(created_at);