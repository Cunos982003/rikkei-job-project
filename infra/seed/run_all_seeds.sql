-- =============================================================================
-- MASTER SEED RUNNER CHO TOÀN BỘ 10 MICROSERVICES (PostgreSQL / psql)
-- Chạy từ thư mục gốc dự án:
--   psql -U postgres -f infra/seed/run_all_seeds.sql
-- Hoặc mở từng file tương ứng chạy trực tiếp trên từng DB trong IntelliJ / DBeaver
-- =============================================================================

\echo '>>> [1] Seeding auth_db (51 users + 100 login_histories + 35 tokens)...'
\c auth_db
\i infra/seed/01_seed_auth_db.sql

\echo '>>> [2] Seeding profile_db (40 profiles, 50 skills, 180 skill_scores, 70 educations, 60 experiences, 28 certs, 40 portfolios)...'
\c profile_db
\i infra/seed/02_seed_profile_db.sql

\echo '>>> [3] Seeding job_db (10 companies, 12 categories, 24 locations, 120 jobs, 360 job_skills)...'
\c job_db
\i infra/seed/03_seed_job_db.sql

\echo '>>> [3b] Seeding contact_access_control (UT-03 contact requests & access logs)...'
\c profile_db
\i infra/seed/09_seed_contact_access_control.sql

\echo '>>> [3c] Seeding cvs & cv_sections (47 CVs, 282 sections)...'
\c profile_db
\i infra/seed/10_seed_cvs.sql

\echo '>>> [4] Seeding recruitment_db (180 recruitments, 420+ history, 46 rounds, 34 feedbacks, 18 offers, 6 employment_records + UT-06/UT-07)...'
\c recruitment_db
\i infra/seed/04_seed_recruitment_db.sql
\i infra/seed/ut06_interview_scheduling_seed.sql
\i infra/seed/ut07_offer_seed.sql

\echo '>>> [5] Seeding learning_db (18 courses, 54 modules, 144 lessons, 68 enrollments, 164 progress + DT-03 learning_paths)...'
\c learning_db
\i infra/seed/05_seed_learning_db.sql
\i backend/learning-service/src/main/resources/db/migration/V4__init_dt03_learning_paths.sql

\echo '>>> [6] Seeding assessment_db (8 types, 18 assessments, 180 questions/720 options, 64 attempts/640 answers)...'
\c assessment_db
\i infra/seed/06_seed_assessment_db.sql

\echo '>>> [7] Seeding evidence_db (evidence reports)...'
\c evidence_db
\i infra/seed/03_evidence_db_ddl.sql

\echo '>>> [8] Seeding notification_db (notifications)...'
\c notification_db
\i infra/seed/05_notification_db_ddl.sql

\echo '>>> [9] Seeding audit_db (edit history & audit logs)...'
\c audit_db
\i infra/seed/06_audit_db_ddl.sql

\echo '>>> [10] Seeding ai_db (160 recommendations, 16 chat sessions, 128 messages, 16 CV reviews, 140 usage logs, 51 quotas)...'
\c ai_db
\i infra/seed/07_seed_ai_db.sql
\i infra/seed/11_seed_ai_drafts.sql

\echo '>>> DONE! Toàn bộ 10 databases đã được đồng bộ cấu trúc và dữ liệu mẫu khớp 100%!'
