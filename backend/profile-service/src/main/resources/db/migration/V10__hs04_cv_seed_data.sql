-- HS-04 CV Seed Data
-- Add sample CVs for testing

-- Sample user IDs (these should match existing users in candidate_profiles)
-- Assuming we have users with these UUIDs from V1__init_profile_schema.sql
-- user_id: '550e8400-e29b-41d4-a716-446655440000' (Nguyễn Văn An)
-- user_id: '550e8400-e29b-41d4-a716-446655440001' (Trần Thị Bình)
-- user_id: '550e8400-e29b-41d4-a716-446655440002' (Lê Văn Cường)

-- Sample CVs for user '550e8400-e29b-41d4-a716-446655440000'
INSERT INTO cvs (id, user_id, title, target_position, template, public_token, is_public, pdf_url, created_at, updated_at) VALUES
(
    'c0000000-0000-0000-0000-000000000001',
    '550e8400-e29b-41d4-a716-446655440000',
    'CV Java Backend Developer',
    'Java Backend Developer',
    'RIKKEI_STANDARD',
    'c0000000000000000000000000000001',
    true,
    'cvs/c0000000-0000-0000-0000-000000000001.pdf',
    now() - interval '5 days',
    now() - interval '5 days'
),
(
    'c0000000-0000-0000-0000-000000000002',
    '550e8400-e29b-41d4-a716-446655440000',
    'CV Spring Boot Developer',
    'Spring Boot Developer',
    'RIKKEI_STANDARD',
    'c0000000000000000000000000000002',
    false,
    null,
    now() - interval '3 days',
    now() - interval '3 days'
),
(
    'c0000000-0000-0000-0000-000000000003',
    '550e8400-e29b-41d4-a716-446655440000',
    'CV Fullstack Java/React',
    'Fullstack Developer (Java/React)',
    'RIKKEI_STANDARD',
    'c0000000000000000000000000000003',
    true,
    'cvs/c0000000-0000-0000-0000-000000000003.pdf',
    now() - interval '1 day',
    now() - interval '1 day'
)
ON CONFLICT (id) DO NOTHING;

-- Sample CV for user '550e8400-e29b-41d4-a716-446655440001'
INSERT INTO cvs (id, user_id, title, target_position, template, public_token, is_public, pdf_url, created_at, updated_at) VALUES
(
    'c0000000-0000-0000-0000-000000000004',
    '550e8400-e29b-41d4-a716-446655440001',
    'CV Frontend Developer',
    'Frontend Developer (React/TypeScript)',
    'RIKKEI_STANDARD',
    'c0000000000000000000000000000004',
    true,
    'cvs/c0000000-0000-0000-0000-000000000004.pdf',
    now() - interval '2 days',
    now() - interval '2 days'
)
ON CONFLICT (id) DO NOTHING;

-- Sample CV for user '550e8400-e29b-41d4-a716-446655440002'
INSERT INTO cvs (id, user_id, title, target_position, template, public_token, is_public, pdf_url, created_at, updated_at) VALUES
(
    'c0000000-0000-0000-0000-000000000005',
    '550e8400-e29b-41d4-a716-446655440002',
    'CV DevOps Engineer',
    'DevOps Engineer',
    'RIKKEI_STANDARD',
    'c0000000000000000000000000000005',
    false,
    null,
    now() - interval '4 days',
    now() - interval '4 days'
)
ON CONFLICT (id) DO NOTHING;

-- Sample CV Sections for CV 'c0000000-0000-0000-0000-000000000001' (Java Backend Developer)
-- Assuming education, skills, projects, certificates exist for this user
INSERT INTO cv_sections (id, cv_id, section_type, item_ids, included, sort_order) VALUES
(
    'a0000000-0000-0000-0000-000000000001',
    'c0000000-0000-0000-0000-000000000001',
    'SUMMARY',
    '[]'::jsonb,
    true,
    0
),
(
    'a0000000-0000-0000-0000-000000000002',
    'c0000000-0000-0000-0000-000000000001',
    'SKILLS',
    '["550e8400-e29b-41d4-a716-446655440100", "550e8400-e29b-41d4-a716-446655440101"]'::jsonb,
    true,
    1
),
(
    'a0000000-0000-0000-0000-000000000003',
    'c0000000-0000-0000-0000-000000000001',
    'EDUCATION',
    '["550e8400-e29b-41d4-a716-446655440200"]'::jsonb,
    true,
    2
),
(
    'a0000000-0000-0000-0000-000000000004',
    'c0000000-0000-0000-0000-000000000001',
    'PROJECTS',
    '["550e8400-e29b-41d4-a716-446655440300", "550e8400-e29b-41d4-a716-446655440301"]'::jsonb,
    true,
    3
),
(
    'a0000000-0000-0000-0000-000000000005',
    'c0000000-0000-0000-0000-000000000001',
    'CERTIFICATES',
    '["550e8400-e29b-41d4-a716-446655440400"]'::jsonb,
    true,
    4
)
ON CONFLICT (id) DO NOTHING;

-- Sample CV Sections for CV 'c0000000-0000-0000-0000-000000000002' (Spring Boot Developer)
INSERT INTO cv_sections (id, cv_id, section_type, item_ids, included, sort_order) VALUES
(
    'a0000000-0000-0000-0000-000000000006',
    'c0000000-0000-0000-0000-000000000002',
    'SUMMARY',
    '[]'::jsonb,
    true,
    0
),
(
    'a0000000-0000-0000-0000-000000000007',
    'c0000000-0000-0000-0000-000000000002',
    'SKILLS',
    '["550e8400-e29b-41d4-a716-446655440100", "550e8400-e29b-41d4-a716-446655440102"]'::jsonb,
    true,
    1
),
(
    'a0000000-0000-0000-0000-000000000008',
    'c0000000-0000-0000-0000-000000000002',
    'EDUCATION',
    '["550e8400-e29b-41d4-a716-446655440200"]'::jsonb,
    true,
    2
),
(
    'a0000000-0000-0000-0000-000000000009',
    'c0000000-0000-0000-0000-000000000002',
    'PROJECTS',
    '["550e8400-e29b-41d4-a716-446655440300"]'::jsonb,
    true,
    3
)
ON CONFLICT (id) DO NOTHING;

-- Sample CV Sections for CV 'c0000000-0000-0000-0000-000000000003' (Fullstack Developer)
INSERT INTO cv_sections (id, cv_id, section_type, item_ids, included, sort_order) VALUES
(
    'a0000000-0000-0000-0000-000000000010',
    'c0000000-0000-0000-0000-000000000003',
    'SUMMARY',
    '[]'::jsonb,
    true,
    0
),
(
    'a0000000-0000-0000-0000-000000000011',
    'c0000000-0000-0000-0000-000000000003',
    'SKILLS',
    '["550e8400-e29b-41d4-a716-446655440100", "550e8400-e29b-41d4-a716-446655440101", "550e8400-e29b-41d4-a716-446655440103"]'::jsonb,
    true,
    1
),
(
    'a0000000-0000-0000-0000-000000000012',
    'c0000000-0000-0000-0000-000000000003',
    'EDUCATION',
    '["550e8400-e29b-41d4-a716-446655440200"]'::jsonb,
    true,
    2
),
(
    'a0000000-0000-0000-0000-000000000013',
    'c0000000-0000-0000-0000-000000000003',
    'PROJECTS',
    '["550e8400-e29b-41d4-a716-446655440300", "550e8400-e29b-41d4-a716-446655440301", "550e8400-e29b-41d4-a716-446655440302"]'::jsonb,
    true,
    3
),
(
    'a0000000-0000-0000-0000-000000000014',
    'c0000000-0000-0000-0000-000000000003',
    'CERTIFICATES',
    '["550e8400-e29b-41d4-a716-446655440400", "550e8400-e29b-41d4-a716-446655440401"]'::jsonb,
    true,
    4
),
(
    'a0000000-0000-0000-0000-000000000015',
    'c0000000-0000-0000-0000-000000000003',
    'INTERNSHIP',
    '["550e8400-e29b-41d4-a716-446655440500"]'::jsonb,
    true,
    5
)
ON CONFLICT (id) DO NOTHING;

-- Sample CV Sections for CV 'c0000000-0000-0000-0000-000000000004' (Frontend Developer)
INSERT INTO cv_sections (id, cv_id, section_type, item_ids, included, sort_order) VALUES
(
    'a0000000-0000-0000-0000-000000000016',
    'c0000000-0000-0000-0000-000000000004',
    'SUMMARY',
    '[]'::jsonb,
    true,
    0
),
(
    'a0000000-0000-0000-0000-000000000017',
    'c0000000-0000-0000-0000-000000000004',
    'SKILLS',
    '["550e8400-e29b-41d4-a716-446655440103", "550e8400-e29b-41d4-a716-446655440104"]'::jsonb,
    true,
    1
),
(
    'a0000000-0000-0000-0000-000000000018',
    'c0000000-0000-0000-0000-000000000004',
    'EDUCATION',
    '["550e8400-e29b-41d4-a716-446655440201"]'::jsonb,
    true,
    2
),
(
    'a0000000-0000-0000-0000-000000000019',
    'c0000000-0000-0000-0000-000000000004',
    'PROJECTS',
    '["550e8400-e29b-41d4-a716-446655440302", "550e8400-e29b-41d4-a716-446655440303"]'::jsonb,
    true,
    3
)
ON CONFLICT (id) DO NOTHING;

-- Sample CV Sections for CV 'c0000000-0000-0000-0000-000000000005' (DevOps Engineer)
INSERT INTO cv_sections (id, cv_id, section_type, item_ids, included, sort_order) VALUES
(
    'a0000000-0000-0000-0000-000000000020',
    'c0000000-0000-0000-0000-000000000005',
    'SUMMARY',
    '[]'::jsonb,
    true,
    0
),
(
    'a0000000-0000-0000-0000-000000000021',
    'c0000000-0000-0000-0000-000000000005',
    'SKILLS',
    '["550e8400-e29b-41d4-a716-446655440105", "550e8400-e29b-41d4-a716-446655440106"]'::jsonb,
    true,
    1
),
(
    'a0000000-0000-0000-0000-000000000022',
    'c0000000-0000-0000-0000-000000000005',
    'EDUCATION',
    '["550e8400-e29b-41d4-a716-446655440202"]'::jsonb,
    true,
    2
),
(
    'a0000000-0000-0000-0000-000000000023',
    'c0000000-0000-0000-0000-000000000005',
    'CERTIFICATES',
    '["550e8400-e29b-41d4-a716-446655440402"]'::jsonb,
    true,
    3
),
(
    'a0000000-0000-0000-0000-000000000024',
    'c0000000-0000-0000-0000-000000000005',
    'INTERNSHIP',
    '["550e8400-e29b-41d4-a716-446655440501"]'::jsonb,
    false,
    4
)
ON CONFLICT (id) DO NOTHING;