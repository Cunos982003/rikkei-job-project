-- ==============================================================================
-- V6__add_candidate_profiles_class_course_index.sql: Thêm chỉ mục (class_code, course_name)
-- ==============================================================================

CREATE INDEX IF NOT EXISTS idx_candidate_profiles_class_course 
ON candidate_profiles(class_code, course_name);
