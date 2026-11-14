-- =============================================================================
-- Profile DB: Seed Data từ mock/data/*.json (≥5 dòng/bảng)
-- =============================================================================
-- Run against profile_db

-- Kỹ năng (skills.json - 24 skills, added missing ones)
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
(gen_random_uuid(), 'DESIGN_PATTERNS', 'Design Patterns', 'Architecture'),
(gen_random_uuid(), 'TAILWIND_CSS', 'Tailwind CSS', 'Frontend'),
(gen_random_uuid(), 'TYPESCRIPT', 'TypeScript', 'Programming'),
(gen_random_uuid(), 'REST_API', 'REST API', 'Backend'),
(gen_random_uuid(), 'APACHE_KAFKA', 'Apache Kafka', 'DevOps')
ON CONFLICT (code) DO NOTHING;

-- Skill mappings (từ lms.json courses + manual)
INSERT INTO skill_mappings (source, source_code, skill_id, weight) VALUES
('LMS', 'JAVA01', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.70),
('LMS', 'JAVA01', (SELECT id FROM skills WHERE code='SQL'), 0.30),
('LMS', 'SPRING01', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 0.80),
('LMS', 'SPRING01', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.20),
('EXAM', 'JAVA_MID', (SELECT id FROM skills WHERE code='JAVA_CORE'), 1.00),
('EXAM', 'SQL_FINAL', (SELECT id FROM skills WHERE code='SQL'), 1.00),
('PM', 'IOC_JAVA', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.60),
('PM', 'IOC_JAVA', (SELECT id FROM skills WHERE code='TEAMWORK'), 0.40),
('PM', 'JAVA_BACKEND', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.40),
('PM', 'JAVA_BACKEND', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 0.35),
('PM', 'JAVA_BACKEND', (SELECT id FROM skills WHERE code='SQL'), 0.25),
('PM', 'JAVA', (SELECT id FROM skills WHERE code='JAVA_CORE'), 0.50),
('PM', 'JAVA', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 0.25),
('PM', 'JAVA', (SELECT id FROM skills WHERE code='SQL'), 0.25),
('LMS', 'FE-01', (SELECT id FROM skills WHERE code='HTML_CSS'), 1.00),
('LMS', 'JS-01', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 1.00),
('LMS', 'RC-01', (SELECT id FROM skills WHERE code='REACT'), 1.00),
('LMS', 'TW-01', (SELECT id FROM skills WHERE code='TAILWIND_CSS'), 1.00)
ON CONFLICT (source, source_code, skill_id) DO NOTHING;

-- Source weights
INSERT INTO score_source_weights (source, weight) VALUES
('LMS', 0.30), ('EXAM', 0.25), ('PM', 0.20), ('INTERNSHIP', 0.15), ('MENTOR', 0.05), ('SELF', 0.05)
ON CONFLICT (source) DO NOTHING;

-- Badge rules
INSERT INTO badge_rules (id, code, skill_id, min_score, need_internship_task) VALUES
('f0000000-0000-0000-0000-000000000001', 'BR-JAVA-EXPERT', (SELECT id FROM skills WHERE code='JAVA_CORE'), 85.00, TRUE),
('f0000000-0000-0000-0000-000000000002', 'BR-SPRING-MASTER', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 80.00, TRUE),
('f0000000-0000-0000-0000-000000000003', 'BR-REACT-PRO', (SELECT id FROM skills WHERE code='REACT'), 75.00, FALSE),
('f0000000-0000-0000-0000-000000000004', 'BR-SQL-GURU', (SELECT id FROM skills WHERE code='SQL'), 85.00, FALSE),
('f0000000-0000-0000-0000-000000000005', 'BR-TEAM-LEADER', (SELECT id FROM skills WHERE code='TEAMWORK'), 80.00, TRUE),
('f0000000-0000-0000-0000-000000000006', 'BR-FULLSTACK', (SELECT id FROM skills WHERE code='JAVA_CORE'), 70.00, TRUE)
ON CONFLICT (code) DO NOTHING;

-- =============================================================================
-- 3 CANDIDATES TỪ candidates.json + 2 SYNTHETIC
-- Fixed UUIDs for user-002, user-003
-- =============================================================================

-- Candidate 1: user-001 (Nguyễn Văn An) - fixed UUID
INSERT INTO candidate_profiles (user_id, full_name, dob, gender, phone, email, address, avatar_url, headline, summary, program, class_code, course_name, visibility, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'Nguyễn Văn An', '2003-04-12', 'MALE', '0987654321', 'candidate1@rikkeijobs.test', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate1', 'Java Backend Developer', 'Học viên xuất sắc khóa Java Fullstack Rikkei Academy, có kinh nghiệm thực tập IOC tại Rikkeisoft', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Lập trình Java Fullstack Chuyên sâu', 'PUBLIC', now())
ON CONFLICT (user_id) DO NOTHING;

-- Candidate 2: user-002 (Trần Thị Bình) - fixed UUID
INSERT INTO candidate_profiles (user_id, full_name, dob, gender, phone, email, address, avatar_url, headline, summary, program, class_code, course_name, visibility, updated_at) VALUES
('660e8400-e29b-41d4-a716-446655440000', 'Trần Thị Bình', '2003-04-12', 'FEMALE', '0912345678', 'candidate2@rikkeijobs.test', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate2', 'Frontend React Developer', 'Đam mê xây dựng trải nghiệm người dùng hiện đại với React 19, TypeScript và Tailwind CSS', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Lập trình Web Frontend Chuyên nghiệp', 'PUBLIC', now())
ON CONFLICT (user_id) DO NOTHING;

-- Candidate 3: user-003 (Lê Hoàng Cường) - fixed UUID
INSERT INTO candidate_profiles (user_id, full_name, dob, gender, phone, email, address, avatar_url, headline, summary, program, class_code, course_name, visibility, updated_at) VALUES
('770e8400-e29b-41d4-a716-446655440000', 'Lê Hoàng Cường', '2003-04-12', 'MALE', '0934567890', 'candidate3@rikkeijobs.test', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate3', 'Fullstack Web Intern (IOC)', 'Thực tập sinh IOC nhóm dự án Rikkei Jobs MVP, hoàn thành xuất sắc các module CSDL và Kafka', 'Đào tạo Kỹ sư Phần mềm Toàn diện', 'RA-FS-2026', 'Đào tạo Kỹ sư Phần mềm Toàn diện', 'PUBLIC', now())
ON CONFLICT (user_id) DO NOTHING;

-- Synthetic candidate 4
INSERT INTO candidate_profiles (user_id, full_name, dob, gender, phone, email, address, avatar_url, headline, summary, program, class_code, course_name, visibility, updated_at) VALUES
('880e8400-e29b-41d4-a716-446655440000', 'Phạm Minh Tuấn', '2002-08-15', 'MALE', '0909123456', 'candidate4@rikkeijobs.test', '789 Nguyễn Trãi, Thanh Xuân, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate4', 'DevOps Engineer Fresher', 'Chuyên Docker, Kubernetes, CI/CD, Linux', 'Lập trình Java Fullstack Chuyên sâu', 'RA-JAVA-2026', 'Lập trình Java Fullstack Chuyên sâu', 'PRIVATE', now()),
('990e8400-e29b-41d4-a716-446655440000', 'Ngô Thị Lan', '2004-01-20', 'FEMALE', '0987123456', 'candidate5@rikkeijobs.test', '456 Trần Hưng Đạo, Cầu Giấy, Hà Nội', 'https://api.dicebear.com/7.x/avataaars/svg?seed=candidate5', 'Tester Fresher', 'JUnit, Mockito, Selenium, Postman', 'Lập trình Web Frontend Chuyên nghiệp', 'RA-FE-2026', 'Lập trình Web Frontend Chuyên nghiệp', 'PRIVATE', now())
ON CONFLICT (user_id) DO NOTHING;

-- =============================================================================
-- EDUCATIONS (từ candidates.json educations)
-- =============================================================================
INSERT INTO educations (id, user_id, school, major, from_date, to_date, gpa, note) VALUES
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'Đại học Bách Khoa Hà Nội', 'Kỹ thuật phần mềm', '2021-09-01', '2025-06-30', 3.45, ''),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'Rikkei Academy', 'Lập trình Java Fullstack Chuyên sâu', '2026-01-15', '2026-09-30', 3.80, ''),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'Đại học Công nghệ Thông tin', 'Khoa học máy tính', '2021-09-01', '2025-06-30', 3.60, ''),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'Rikkei Academy', 'Lập trình Web Frontend Chuyên nghiệp', '2026-01-15', '2026-09-30', 3.75, ''),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'Đại học Bách Khoa TP.HCM', 'Kỹ thuật phần mềm', '2021-09-01', '2025-06-30', 3.85, ''),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'Rikkei Academy', 'Đào tạo Kỹ sư Phần mềm Toàn diện', '2026-01-15', '2026-09-30', 3.90, '')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- CERTIFICATES (từ candidates.json certificates)
-- =============================================================================
INSERT INTO certificates (id, user_id, name, issuer, issued_at, expires_at, file_url, file_type) VALUES
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'Oracle Certified Professional, Java SE 11 Developer', 'Oracle', '2026-05-20', NULL, 'https://credly.com/badges/ocp-java-11', 'PDF'),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'AWS Certified Cloud Practitioner', 'Amazon Web Services', '2026-07-11', NULL, 'https://credly.com/badges/aws-ccp', 'PDF'),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'Meta Front-End Developer Professional Certificate', 'Meta', '2026-06-15', NULL, 'https://credly.com/badges/meta-fe', 'PDF'),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'Google UX Design Professional Certificate', 'Google', '2026-08-01', NULL, 'https://credly.com/badges/google-ux', 'PDF'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'AWS Certified Solutions Architect - Associate', 'Amazon Web Services', '2026-09-10', NULL, 'https://credly.com/badges/aws-saa', 'PDF'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'CKAD: Certified Kubernetes Application Developer', 'CNCF', '2026-10-05', NULL, 'https://credly.com/badges/ckad', 'PDF')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- INTERNSHIPS (từ pm.json internships)
-- =============================================================================
INSERT INTO internships (id, user_id, batch_id, batch_name, group_name, topic_name, mentor_email, start_date, end_date, final_score, synced_at) VALUES
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'IOC-2026-B1', 'Đợt thực tập IOC Mùa Thu 2026', 'Nhóm 01 - Java Banking Gateway', 'Xây dựng cổng thanh toán an toàn cho hệ thống tài chính', 'mentor1@rikkeijobs.test', '2026-09-15', '2026-11-30', 85.50, now() - interval '5 days'),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'IOC-2026-B1', 'Đợt thực tập IOC Mùa Thu 2026', 'Nhóm 02 - Rikkei E-Commerce UI', 'Xây dựng giao diện sàn thương mại điện tử chuyên biệt', 'mentor1@rikkeijobs.test', '2026-09-15', '2026-11-30', 81.00, now() - interval '4 days'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'IOC-2026-B1', 'Đợt thực tập IOC Mùa Thu 2026', 'Nhóm 03 - Rikkei Jobs MVP', 'Xây dựng cổng việc làm Rikkei Jobs dựa trên năng lực xác thực', 'mentor1@rikkeijobs.test', '2026-09-15', '2026-11-30', 91.00, now() - interval '3 days'),
(gen_random_uuid(), '880e8400-e29b-41d4-a716-446655440000', 'IOC-2026-B2', 'Đợt thực tập IOC Mùa Đông 2026', 'Nhóm 04 - AI Chatbot', 'Xây dựng chatbot hỗ trợ tuyển dụng', 'mentor1@rikkeijobs.test', '2026-12-01', '2027-02-28', NULL, now()),
(gen_random_uuid(), '990e8400-e29b-41d4-a716-446655440000', 'IOC-2026-B2', 'Đợt thực tập IOC Mùa Đông 2026', 'Nhóm 05 - Mobile App', 'Ứng dụng di động tìm việc làm', 'mentor1@rikkeijobs.test', '2026-12-01', '2027-02-28', NULL, now())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- INTERNSHIP TASKS
-- =============================================================================
INSERT INTO internship_tasks (id, internship_id, title, accepted_score, accepted_at) VALUES
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='550e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Phân tích và thiết kế mô hình thực thể ERD', 90.0, '2026-10-10T17:00:00Z'),
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='550e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Xây dựng RESTful API xử lý thanh toán và đối soát', 85.0, '2026-10-22T17:00:00Z'),
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='550e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Viết Unit Test độ bao phủ 80% bằng Mockito', 82.0, '2026-10-30T17:00:00Z'),
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='660e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Thiết kế Responsive UI với Tailwind CSS và shadcn', 85.0, '2026-10-12T17:00:00Z'),
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='660e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Tích hợp state management và giỏ hàng thời gian thực', 80.0, '2026-10-25T17:00:00Z'),
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='770e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Xây dựng kiến trúc Microservices và API Gateway', 95.0, '2026-10-15T17:00:00Z'),
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='770e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Tích hợp Outbox pattern và Kafka Event Streaming', 90.0, '2026-10-28T17:00:00Z'),
(gen_random_uuid(), (SELECT id FROM internships WHERE user_id='770e8400-e29b-41d4-a716-446655440000' AND batch_id='IOC-2026-B1' LIMIT 1), 'Xây dựng Mock server và kiểm thử tự động', 88.0, '2026-11-05T17:00:00Z')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- PORTFOLIO PROJECTS (synthetic)
-- =============================================================================
INSERT INTO portfolio_projects (id, user_id, source, source_ref, name, role, created_at) VALUES
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'PM', 'IOC_JAVA_BANKING', 'Java Banking Gateway', 'Backend Developer', now() - interval '30 days'),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'SELF', 'PERSONAL_BLOG', 'Personal Tech Blog (Spring Boot + React)', 'Fullstack Developer', now() - interval '60 days'),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'PM', 'IOC_FE_ECOMMERCE', 'Rikkei E-Commerce UI', 'Frontend Developer', now() - interval '25 days'),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'SELF', 'PORTFOLIO_WEBSITE', 'Personal Portfolio Website', 'Frontend Developer', now() - interval '40 days'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'PM', 'IOC_FULLSTACK_JOBS', 'Rikkei Jobs MVP', 'Fullstack Developer', now() - interval '20 days'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'SELF', 'KAFKA_DEMO', 'Kafka Event Streaming Demo', 'Backend Developer', now() - interval '30 days')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- SKILL EVIDENCES (từ candidates.json skills + evidences)
-- =============================================================================
INSERT INTO skill_evidences (id, user_id, skill_id, source, source_ref, title, score, recorded_at, verified) VALUES
-- User 1 evidences
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 'LMS', 'JAVA01', 'Java Core Course', 85, '2026-10-15T10:30:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 'EXAM', 'JAVA_MID', 'Java Midterm', 90, '2026-09-20T14:00:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 'LMS', 'SPRING01', 'Spring Boot Course', 80, '2026-10-10T10:00:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 'PM', 'IOC_JAVA', 'IOC Java Project', 70, '2026-08-15T16:00:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SQL'), 'LMS', 'JAVA01', 'Java Core Course (SQL part)', 75, '2026-10-15T10:30:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SQL'), 'EXAM', 'SQL_FINAL', 'SQL Final Exam', 75, '2026-09-25T10:00:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), 'SELF', 'SELF_REACT', 'Self-reported React', 88, '2026-10-01T12:00:00Z', false),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 'LMS', 'WEB01', 'JavaScript Basics', 65, '2026-09-01T10:00:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 'SELF', 'SELF_JS', 'Self-reported JavaScript', 70, '2026-10-01T12:00:00Z', false),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='DOCKER'), 'LMS', 'DK-01', 'Docker Fundamentals', 60, '2026-08-20T10:00:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TEAMWORK'), 'PM', 'IOC_JAVA', 'IOC Java Project (Teamwork)', 75, '2026-08-15T16:00:00Z', true),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='GIT'), 'LMS', 'GIT-01', 'Version Control with Git', 70, '2026-09-10T10:00:00Z', true),
-- User 2 evidences
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='HTML_CSS'), 'LMS', 'FE-01', 'HTML/CSS Course', 85, '2026-10-15T08:00:00Z', true),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 'LMS', 'JS-01', 'JavaScript ES6+', 80, '2026-10-10T10:00:00Z', true),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), 'LMS', 'RC-01', 'React 19 & TypeScript', 78, '2026-10-05T10:00:00Z', true),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TYPESCRIPT'), 'LMS', 'RC-01', 'React 19 & TypeScript', 70, '2026-10-05T10:00:00Z', true),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TAILWIND_CSS'), 'LMS', 'TW-01', 'Tailwind CSS & UI Components', 82, '2026-10-01T10:00:00Z', true),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='GIT'), 'LMS', 'FE-01', 'HTML/CSS Course', 68, '2026-10-15T08:00:00Z', true),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REST_API'), 'LMS', 'RC-01', 'React 19 & TypeScript', 65, '2026-10-05T10:00:00Z', true),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='ENGLISH'), 'LMS', 'FE-01', 'HTML/CSS Course', 72, '2026-10-15T08:00:00Z', true),
-- User 3 evidences
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 'LMS', 'JV-01', 'Java Core & OOP', 90, '2026-10-10T10:00:00Z', true),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 'LMS', 'SB-01', 'Spring Boot & Cloud Native', 88, '2026-10-05T10:00:00Z', true),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), 'LMS', 'RC-01', 'React 19 & State Management', 84, '2026-09-28T10:00:00Z', true),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TYPESCRIPT'), 'LMS', 'RC-01', 'React 19 & State Management', 80, '2026-09-28T10:00:00Z', true),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SQL'), 'LMS', 'JV-01', 'Java Core & OOP', 92, '2026-10-10T10:00:00Z', true),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='DOCKER'), 'LMS', 'DK-01', 'Docker & Kafka', 85, '2026-09-25T10:00:00Z', true),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='APACHE_KAFKA'), 'LMS', 'DK-01', 'Docker & Kafka', 82, '2026-09-25T10:00:00Z', true),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TEAMWORK'), 'PM', 'IOC_FULLSTACK', 'IOC Fullstack Project', 95, '2026-10-15T16:00:00Z', true)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- SKILL SCORES (từ candidates.json skills - verified/self)
-- =============================================================================
INSERT INTO skill_scores (user_id, skill_id, verified_score, self_score, level, updated_at) VALUES
-- User 1
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 85, NULL, 'Nâng cao', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 78, NULL, 'Vững', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SQL'), 72, NULL, 'Vững', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), NULL, 88, 'Nâng cao', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 65, 70, 'Khá', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='DOCKER'), 60, NULL, 'Khá', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TEAMWORK'), 75, NULL, 'Vững', now()),
('550e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='GIT'), 70, NULL, 'Vững', now()),
-- User 2
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='HTML_CSS'), 85, NULL, 'Nâng cao', now()),
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVASCRIPT'), 80, NULL, 'Vững', now()),
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), 78, NULL, 'Vững', now()),
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TYPESCRIPT'), 70, NULL, 'Vững', now()),
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TAILWIND_CSS'), 82, NULL, 'Vững', now()),
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='GIT'), 68, NULL, 'Khá', now()),
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REST_API'), 65, NULL, 'Khá', now()),
('660e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='ENGLISH'), 72, NULL, 'Vững', now()),
-- User 3
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='JAVA_CORE'), 90, NULL, 'Nâng cao', now()),
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SPRING_BOOT'), 88, NULL, 'Nâng cao', now()),
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='REACT'), 84, NULL, 'Vững', now()),
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TYPESCRIPT'), 80, NULL, 'Vững', now()),
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='SQL'), 92, NULL, 'Nâng cao', now()),
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='DOCKER'), 85, NULL, 'Nâng cao', now()),
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='APACHE_KAFKA'), 82, NULL, 'Vững', now()),
('770e8400-e29b-41d4-a716-446655440000', (SELECT id FROM skills WHERE code='TEAMWORK'), 95, NULL, 'Nâng cao', now())
ON CONFLICT (user_id, skill_id) DO NOTHING;

-- =============================================================================
-- PROFILE BADGES
-- =============================================================================
INSERT INTO profile_badges (id, user_id, badge_rule_id, granted_type, granted_at, revoked_at) VALUES
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'f0000000-0000-0000-0000-000000000001', 'AUTO', now() - interval '10 days', NULL),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'f0000000-0000-0000-0000-000000000002', 'AUTO', now() - interval '8 days', NULL),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'f0000000-0000-0000-0000-000000000001', 'AUTO', now() - interval '5 days', NULL),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'f0000000-0000-0000-0000-000000000004', 'AUTO', now() - interval '3 days', NULL),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'f0000000-0000-0000-0000-000000000005', 'AUTO', now() - interval '2 days', NULL),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'f0000000-0000-0000-0000-000000000003', 'AUTO', now() - interval '4 days', NULL)
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- SYNC RUNS & SOURCES (using proper UUIDs)
-- =============================================================================
INSERT INTO sync_runs (id, started_at, completed_at, status, error) VALUES
(gen_random_uuid(), now() - interval '1 day', now() - interval '23 hours', 'SUCCESS', NULL),
(gen_random_uuid(), now() - interval '2 days', now() - interval '47 hours', 'SUCCESS', NULL),
(gen_random_uuid(), now() - interval '3 days', now() - interval '71 hours', 'PARTIAL', 'PM source timeout'),
(gen_random_uuid(), now() - interval '4 days', now() - interval '95 hours', 'SUCCESS', NULL),
(gen_random_uuid(), now() - interval '5 days', now() - interval '119 hours', 'FAILED', 'LMS connection refused')
ON CONFLICT (id) DO NOTHING;

-- Need to reference sync_runs IDs - use CTE to get them
WITH runs AS (
  SELECT id, started_at, completed_at, row_number() over (ORDER BY started_at) as rn FROM sync_runs
)
INSERT INTO sync_run_sources (id, run_id, source, status, started_at, completed_at, records_processed, error)
SELECT gen_random_uuid(), id, 'LMS', 'SUCCESS', started_at, completed_at - interval '10 minutes', 45, NULL FROM runs WHERE rn = 1
UNION ALL
SELECT gen_random_uuid(), id, 'EXAM', 'SUCCESS', started_at, completed_at - interval '15 minutes', 32, NULL FROM runs WHERE rn = 1
UNION ALL
SELECT gen_random_uuid(), id, 'PM', 'SUCCESS', started_at, completed_at - interval '20 minutes', 50, NULL FROM runs WHERE rn = 1
UNION ALL
SELECT gen_random_uuid(), id, 'LMS', 'SUCCESS', started_at, completed_at - interval '10 minutes', 45, NULL FROM runs WHERE rn = 2
UNION ALL
SELECT gen_random_uuid(), id, 'EXAM', 'SUCCESS', started_at, completed_at - interval '15 minutes', 32, NULL FROM runs WHERE rn = 2
UNION ALL
SELECT gen_random_uuid(), id, 'PM', 'SUCCESS', started_at, completed_at - interval '20 minutes', 50, NULL FROM runs WHERE rn = 2
UNION ALL
SELECT gen_random_uuid(), id, 'LMS', 'SUCCESS', started_at, completed_at - interval '10 minutes', 45, NULL FROM runs WHERE rn = 3
UNION ALL
SELECT gen_random_uuid(), id, 'EXAM', 'SUCCESS', started_at, completed_at - interval '15 minutes', 32, NULL FROM runs WHERE rn = 3
UNION ALL
SELECT gen_random_uuid(), id, 'PM', 'FAILED', started_at, started_at, 0, 'PM source timeout' FROM runs WHERE rn = 3
UNION ALL
SELECT gen_random_uuid(), id, 'LMS', 'SUCCESS', started_at, completed_at - interval '10 minutes', 45, NULL FROM runs WHERE rn = 4
UNION ALL
SELECT gen_random_uuid(), id, 'EXAM', 'SUCCESS', started_at, completed_at - interval '15 minutes', 32, NULL FROM runs WHERE rn = 4
UNION ALL
SELECT gen_random_uuid(), id, 'PM', 'SUCCESS', started_at, completed_at - interval '20 minutes', 50, NULL FROM runs WHERE rn = 4
UNION ALL
SELECT gen_random_uuid(), id, 'LMS', 'FAILED', started_at, started_at, 0, 'LMS connection refused' FROM runs WHERE rn = 5
UNION ALL
SELECT gen_random_uuid(), id, 'EXAM', 'SUCCESS', started_at, completed_at - interval '15 minutes', 32, NULL FROM runs WHERE rn = 5
UNION ALL
SELECT gen_random_uuid(), id, 'PM', 'SUCCESS', started_at, completed_at - interval '20 minutes', 50, NULL FROM runs WHERE rn = 5
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- FILES (synthetic)
-- =============================================================================
INSERT INTO files (id, user_id, filename, content_type, size_bytes, storage_path, purpose, created_at) VALUES
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'OCP_Java11.pdf', 'application/pdf', 1024000, '/files/certs/ocp-java11.pdf', 'CERTIFICATE', now() - interval '100 days'),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'AWS_CCP.pdf', 'application/pdf', 2048000, '/files/certs/aws-ccp.pdf', 'CERTIFICATE', now() - interval '80 days'),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'Meta_FE_Cert.pdf', 'application/pdf', 1536000, '/files/certs/meta-fe.pdf', 'CERTIFICATE', now() - interval '90 days'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'AWS_SAA.pdf', 'application/pdf', 2048000, '/files/certs/aws-saa.pdf', 'CERTIFICATE', now() - interval '40 days'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'CKAD.pdf', 'application/pdf', 1024000, '/files/certs/ckad.pdf', 'CERTIFICATE', now() - interval '25 days'),
(gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440000', 'avatar.jpg', 'image/jpeg', 512000, '/files/avatars/user-001.jpg', 'AVATAR', now() - interval '30 days'),
(gen_random_uuid(), '660e8400-e29b-41d4-a716-446655440000', 'avatar.jpg', 'image/jpeg', 512000, '/files/avatars/user-002.jpg', 'AVATAR', now() - interval '25 days'),
(gen_random_uuid(), '770e8400-e29b-41d4-a716-446655440000', 'avatar.jpg', 'image/jpeg', 512000, '/files/avatars/user-003.jpg', 'AVATAR', now() - interval '20 days')
ON CONFLICT (id) DO NOTHING;