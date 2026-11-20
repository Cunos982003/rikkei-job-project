
-- DG-03: Admin badge management
-- Giữ lại lịch sử trao/thu hồi và cho phép trao lại huy hiệu.

ALTER TABLE profile_badges
DROP CONSTRAINT IF EXISTS profile_badges_user_id_badge_rule_id_key;

ALTER TABLE profile_badges
    ADD COLUMN IF NOT EXISTS grant_reason TEXT,
    ADD COLUMN IF NOT EXISTS grant_admin_id UUID,
    ADD COLUMN IF NOT EXISTS revoke_reason TEXT,
    ADD COLUMN IF NOT EXISTS revoke_admin_id UUID;

-- Mỗi ứng viên chỉ có tối đa một bản ghi huy hiệu đang hiệu lực.
CREATE UNIQUE INDEX IF NOT EXISTS uq_profile_badges_active
    ON profile_badges(user_id, badge_rule_id)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_profile_badges_history
    ON profile_badges(user_id, granted_at DESC);

CREATE INDEX IF NOT EXISTS idx_profile_badges_admin
    ON profile_badges(grant_admin_id, revoke_admin_id);
