-- HS-04 CV Generation Feature
-- Add CVs and CV sections tables

-- CV table
CREATE TABLE IF NOT EXISTS cvs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    public_token VARCHAR(64) NOT NULL UNIQUE,
    is_public BOOLEAN NOT NULL DEFAULT false,
    pdf_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp()
);

-- CV sections table
CREATE TABLE IF NOT EXISTS cv_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cv_id UUID NOT NULL REFERENCES cvs(id) ON DELETE CASCADE,
    section_type VARCHAR(50) NOT NULL CHECK (section_type IN ('personal_info','education','experience','skills','projects','certificates','summary')),
    content_json JSONB NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_cvs_user_id ON cvs(user_id);
CREATE INDEX IF NOT EXISTS idx_cvs_public_token ON cvs(public_token);
CREATE INDEX IF NOT EXISTS idx_cv_sections_cv_id ON cv_sections(cv_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_cv_sections_cv_type ON cv_sections(cv_id, section_type);