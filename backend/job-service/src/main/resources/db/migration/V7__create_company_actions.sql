CREATE TABLE IF NOT EXISTS company_actions (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL,
    target_id UUID,
    reason VARCHAR(1000),
    staff_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_company_actions_action
        CHECK (action IN ('LOCK', 'UNLOCK', 'APPROVE_JD', 'REJECT_JD'))
);

CREATE INDEX IF NOT EXISTS idx_company_actions_company_id ON company_actions(company_id);
CREATE INDEX IF NOT EXISTS idx_company_actions_created_at ON company_actions(created_at DESC);
