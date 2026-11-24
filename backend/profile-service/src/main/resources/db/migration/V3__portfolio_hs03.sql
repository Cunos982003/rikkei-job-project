-- HS-03 Portfolio extensions
-- Add new columns to portfolio_projects table
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS internship_id UUID REFERENCES internships(id);
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS tech_stack TEXT;
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS repo_url VARCHAR(500);
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS demo_url VARCHAR(500);
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS start_date DATE;
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS visible BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE portfolio_projects ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;

-- Create indexes for portfolio_projects
CREATE INDEX IF NOT EXISTS idx_portfolio_projects_user_id ON portfolio_projects(user_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_projects_internship_id ON portfolio_projects(internship_id);
CREATE INDEX IF NOT EXISTS idx_portfolio_projects_visible_sort ON portfolio_projects(user_id, visible, sort_order);

-- Create portfolio_media table
CREATE TABLE IF NOT EXISTS portfolio_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES portfolio_projects(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('IMAGE')),
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT transaction_timestamp()
);

CREATE INDEX IF NOT EXISTS idx_portfolio_media_project_id ON portfolio_media(project_id);