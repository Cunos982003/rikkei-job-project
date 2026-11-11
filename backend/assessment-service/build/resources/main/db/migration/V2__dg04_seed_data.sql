-- Sample data for DG-04 acceptance (mentor1 only sees own assignments)
-- mentor1: 11111111-1111-1111-1111-111111111111 / mentor1@rikkei.com
-- mentor2: 22222222-2222-2222-2222-222222222222 / mentor2@rikkei.com

INSERT INTO internship_batches (id, code, name, starts_on, ends_on) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'IOC-2026-01', 'Đợt thực tập IOC 2026-01', '2026-01-15', '2026-04-15'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'IOC-2026-02', 'Đợt thực tập IOC 2026-02', '2026-05-01', '2026-08-01');

INSERT INTO evaluation_criteria (id, context, code, name, description, sort_order) VALUES
    ('c1111111-1111-1111-1111-111111111101', 'INTERNSHIP', 'TECH', 'Kỹ năng chuyên môn', 'Mức độ nắm vững kỹ thuật được giao', 1),
    ('c1111111-1111-1111-1111-111111111102', 'INTERNSHIP', 'SOFT', 'Kỹ năng mềm', 'Giao tiếp, làm việc nhóm, chủ động', 2),
    ('c1111111-1111-1111-1111-111111111103', 'INTERNSHIP', 'DELIVERY', 'Tiến độ & chất lượng', 'Hoàn thành task đúng hạn, chất lượng deliverable', 3),
    ('c2222222-2222-2222-2222-222222222201', 'INTERVIEW', 'PROBLEM', 'Tư duy giải quyết vấn đề', 'Phân tích và đề xuất giải pháp', 1),
    ('c2222222-2222-2222-2222-222222222202', 'INTERVIEW', 'COMM', 'Giao tiếp', 'Trình bày rõ ràng, lắng nghe', 2),
    ('c2222222-2222-2222-2222-222222222203', 'INTERVIEW', 'FIT', 'Phù hợp văn hóa / vị trí', 'Mức độ phù hợp với JD và team', 3);

-- mentor1 internships
INSERT INTO internships (id, batch_id, candidate_id, candidate_name, mentor_id, mentor_email, due_at, skills_summary, internship_tasks) VALUES
    ('b1111111-1111-1111-1111-111111111101',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
     'd1111111-1111-1111-1111-111111111101',
     'Nguyễn Văn An',
     '11111111-1111-1111-1111-111111111111',
     'mentor1@rikkei.com',
     '2026-04-10 17:00:00+00',
     'Java, Spring Boot, SQL, Git',
     'Xây API hồ sơ ứng viên; viết unit test; demo sprint 3'),
    ('b1111111-1111-1111-1111-111111111102',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
     'd1111111-1111-1111-1111-111111111102',
     'Trần Thị Bình',
     '11111111-1111-1111-1111-111111111111',
     'mentor1@rikkei.com',
     '2026-04-12 17:00:00+00',
     'React, TypeScript, Tailwind',
     'Màn hình Skill Profile; component biểu đồ kỹ năng'),
    ('b1111111-1111-1111-1111-111111111103',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
     'd1111111-1111-1111-1111-111111111103',
     'Lê Minh Cường',
     '11111111-1111-1111-1111-111111111111',
     'mentor1@rikkei.com',
     '2026-07-20 17:00:00+00',
     'Docker, CI/CD, PostgreSQL',
     'Pipeline GitHub Actions; docker-compose cho staging');

-- mentor2 internship (must NOT appear for mentor1)
INSERT INTO internships (id, batch_id, candidate_id, candidate_name, mentor_id, mentor_email, due_at, skills_summary, internship_tasks) VALUES
    ('b2222222-2222-2222-2222-222222222201',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
     'd2222222-2222-2222-2222-222222222201',
     'Phạm Quốc Dũng',
     '22222222-2222-2222-2222-222222222222',
     'mentor2@rikkei.com',
     '2026-04-11 17:00:00+00',
     'Kotlin, Android',
     'Module đăng nhập SSO trên app mobile');

-- mentor1 interview rounds
INSERT INTO interview_rounds (id, batch_id, round_name, candidate_id, candidate_name, interviewer_id, interviewer_email, due_at, skills_summary) VALUES
    ('e1111111-1111-1111-1111-111111111101',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
     'Vòng kỹ thuật 1',
     'd3333333-3333-3333-3333-333333333301',
     'Hoàng Thu Hà',
     '11111111-1111-1111-1111-111111111111',
     'mentor1@rikkei.com',
     '2026-03-20 10:00:00+00',
     'Java, System Design cơ bản'),
    ('e1111111-1111-1111-1111-111111111102',
     NULL,
     'Vòng văn hóa',
     'd3333333-3333-3333-3333-333333333302',
     'Vũ Đức Khoa',
     '11111111-1111-1111-1111-111111111111',
     'mentor1@rikkei.com',
     '2026-03-22 14:00:00+00',
     'Giao tiếp, teamwork');

-- mentor2 interview (hidden from mentor1)
INSERT INTO interview_rounds (id, batch_id, round_name, candidate_id, candidate_name, interviewer_id, interviewer_email, due_at, skills_summary) VALUES
    ('e2222222-2222-2222-2222-222222222201',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
     'Vòng kỹ thuật 1',
     'd3333333-3333-3333-3333-333333333399',
     'Ngô Bảo Long',
     '22222222-2222-2222-2222-222222222222',
     'mentor2@rikkei.com',
     '2026-03-21 09:00:00+00',
     'Node.js, microservices');
