-- =============================================================================
-- MASTER SEED RUNNER CHO TOÀN BỘ 7 MICROSERVICES (PostgreSQL / psql)
-- Chạy từ thư mục gốc dự án:
--   psql -U postgres -f infra/seed/run_all_seeds.sql
-- Hoặc mở từng file 01..07 chạy trực tiếp trên DB tương ứng trong DataGrip / DBeaver
-- =============================================================================

\echo '>>> [A] Seeding auth_db (51 users + 100 login_histories + 35 tokens)...'
\c auth_db
\i infra/seed/01_seed_auth_db.sql

\echo '>>> [B] Seeding profile_db (40 profiles, 50 skills, 180 skill_scores, 70 educations, 60 experiences, 28 certs, 40 portfolios)...'
\c profile_db
\i infra/seed/02_seed_profile_db.sql

\echo '>>> [C] Seeding job_db (10 companies, 12 categories, 24 locations, 120 jobs, 360 job_skills)...'
\c job_db
\i infra/seed/03_seed_job_db.sql

\echo '>>> [D] Seeding recruitment_db (180 recruitments, 420+ history, 46 rounds, 34 feedbacks, 18 offers, 6 employment_records)...'
\c recruitment_db
\i infra/seed/04_seed_recruitment_db.sql

\echo '>>> [E] Seeding learning_db (18 courses, 54 modules, 144 lessons, 68 enrollments, 164 progress, 25 reviews)...'
\c learning_db
\i infra/seed/05_seed_learning_db.sql

\echo '>>> [F] Seeding assessment_db (8 types, 18 assessments, 180 questions/720 options, 64 attempts/640 answers)...'
\c assessment_db
\i infra/seed/06_seed_assessment_db.sql

\echo '>>> [G] Seeding ai_db (160 recommendations, 16 chat sessions, 128 messages, 16 CV reviews, 140 usage logs, 51 quotas)...'
\c ai_db
\i infra/seed/07_seed_ai_db.sql

\echo '>>> DONE! Toàn bộ 7 databases đã được đồng bộ dữ liệu mẫu khớp 100% khóa liên kết.'
