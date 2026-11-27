-- Fix overly restrictive unique constraints on profile_reviews
-- The unique partial indexes on APPROVE/REJECT prevent valid workflows where
-- a profile is rejected, resubmitted, and approved again.
-- The application-level state machine in CandidateProfile already enforces valid transitions.

DROP INDEX IF EXISTS uq_profile_reviews_user_approve;
DROP INDEX IF EXISTS uq_profile_reviews_user_reject;

-- Keep the regular indexes for query performance
-- CREATE INDEX IF NOT EXISTS idx_profile_reviews_user_id ON profile_reviews(user_id);
-- CREATE INDEX IF NOT EXISTS idx_profile_reviews_user_action_created ON profile_reviews(user_id, action, created_at DESC);