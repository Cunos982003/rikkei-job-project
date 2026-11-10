-- Seed ~20 skills from task framework
INSERT INTO skills (id, code, name, "group") VALUES
(gen_random_uuid(), 'JAVA_CORE', 'Java Core', 'Programming'),
(gen_random_uuid(), 'SPRING_BOOT', 'Spring Boot', 'Framework'),
(gen_random_uuid(), 'SQL', 'SQL', 'Database'),
(gen_random_uuid(), 'JAVASCRIPT', 'JavaScript', 'Programming'),
(gen_random_uuid(), 'REACT', 'React', 'Frontend'),
(gen_random_uuid(), 'HTML_CSS', 'HTML/CSS', 'Frontend'),
(gen_random_uuid(), 'GIT', 'Git', 'Tool'),
(gen_random_uuid(), 'DOCKER', 'Docker', 'DevOps'),
(gen_random_uuid(), 'KUBERNETES', 'Kubernetes', 'DevOps'),
(gen_random_uuid(), 'CI_CD', 'CI/CD', 'DevOps'),
(gen_random_uuid(), 'TESTING', 'Testing', 'Quality'),
(gen_random_uuid(), 'AGILE', 'Agile/Scrum', 'Process'),
(gen_random_uuid(), 'COMMUNICATION', 'Communication', 'Soft Skill'),
(gen_random_uuid(), 'PROBLEM_SOLVING', 'Problem Solving', 'Soft Skill'),
(gen_random_uuid(), 'TEAMWORK', 'Teamwork', 'Soft Skill'),
(gen_random_uuid(), 'ENGLISH', 'English', 'Language'),
(gen_random_uuid(), 'PYTHON', 'Python', 'Programming'),
(gen_random_uuid(), 'DATA_STRUCTURES', 'Data Structures', 'CS Fundamental'),
(gen_random_uuid(), 'ALGORITHMS', 'Algorithms', 'CS Fundamental'),
(gen_random_uuid(), 'DESIGN_PATTERNS', 'Design Patterns', 'Architecture')
ON CONFLICT (code) DO NOTHING;

-- Example mappings (JAVA01 -> Java Core 0.7, SQL 0.3)
INSERT INTO skill_mappings (source, source_code, skill_id, weight) VALUES
('LMS', 'JAVA01', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.70),
('LMS', 'JAVA01', (SELECT id FROM skills WHERE code='SQL'), 0.30),
('LMS', 'SPRING01', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 0.80),
('LMS', 'SPRING01', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.20),
('EXAM', 'JAVA_MID', (SELECT id FROM skills WHERE code='JAVA_CORE'), 1.00),
('EXAM', 'SQL_FINAL', (SELECT id FROM skills WHERE code='SQL'), 1.00),
('PM', 'IOC_JAVA', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.60),
('PM', 'IOC_JAVA', (SELECT id FROM skills WHERE code='TEAMWORK'), 0.40)
ON CONFLICT (source, source_code, skill_id) DO NOTHING;

-- Seed skill evidences for demo user (550e8400-e29b-41d4-a716-446655440000)
INSERT INTO skill_evidences (user_id, skill_id, source, source_ref, title, score, recorded_at, verified) VALUES
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 'LMS', 'JAVA01', 'Java Core Course', 85, now() - interval '30 days', true),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 'EXAM', 'JAVA_MID', 'Java Midterm Exam', 90, now() - interval '50 days', true),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 'LMS', 'SPRING01', 'Spring Boot Course', 80, now() - interval '20 days', true),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SQL'), 'EXAM', 'SQL_FINAL', 'SQL Final Exam', 75, now() - interval '45 days', true),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), 'SELF', 'SELF_REACT', 'Self-reported React', 88, now() - interval '10 days', false),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 'LMS', 'WEB01', 'JavaScript Basics', 65, now() - interval '60 days', true),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 'SELF', 'SELF_JS', 'Self-reported JavaScript', 70, now() - interval '10 days', false)
ON CONFLICT (user_id, skill_id, source, source_ref) DO NOTHING;

-- Seed skill scores for demo user
INSERT INTO skill_scores (user_id, skill_id, verified_score, self_score, level, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 85, NULL, 'Nâng cao', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 78, NULL, 'Vững', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SQL'), 72, NULL, 'Vững', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), NULL, 88, 'Nâng cao', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 65, 70, 'Khá', now())
ON CONFLICT (user_id, skill_id) DO NOTHING;