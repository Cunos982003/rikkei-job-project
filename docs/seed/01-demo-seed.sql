
-- =============================================================================
-- DEMO PHASE SEED DATA - Rikkei Jobs Platform
-- Target: ~2,500–4,000 records across all services
-- Run order: 01-demo-seed.sql → 02-profile-seed.sql → 03-job-seed.sql → 04-recruitment-seed.sql → 05-learning-seed.sql → 06-assessment-seed.sql → 07-ai-career-seed.sql
-- =============================================================================

-- This script seeds the Auth Service (users, roles, permissions, etc.)
-- Run on auth_service database

-- =============================================================================
-- ROLES (4)
-- =============================================================================
INSERT INTO roles (id, name, description, created_at, updated_at) VALUES
('role-admin', 'ADMIN', 'System administrator with full access', NOW(), NOW()),
('role-staff', 'STAFF', 'Recruitment staff', NOW(), NOW()),
('role-hr', 'HR', 'HR/Recruiter from company', NOW(), NOW()),
('role-candidate', 'CANDIDATE', 'Job seeker / student', NOW(), NOW()),
('role-mentor', 'MENTOR', 'Mentor / instructor', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- PERMISSIONS (25)
-- =============================================================================
INSERT INTO permissions (id, name, resource, action, description) VALUES
-- User management
('perm-user-read', 'user:read', 'user', 'read', 'View user profiles'),
('perm-user-write', 'user:write', 'user', 'write', 'Create/update users'),
('perm-user-delete', 'user:delete', 'user', 'delete', 'Delete users'),
-- Profile management
('perm-profile-read', 'profile:read', 'profile', 'read', 'View candidate profiles'),
('perm-profile-write', 'profile:write', 'profile', 'write', 'Create/update profiles'),
('perm-profile-approve', 'profile:approve', 'profile', 'approve', 'Approve/reject profiles'),
('perm-profile-edit-self', 'profile:self-edit', 'profile', 'edit_self_declared', 'Edit self-declared fields'),
-- Job management
('perm-job-read', 'job:read', 'job', 'read', 'View job postings'),
('perm-job-write', 'job:write', 'job', 'write', 'Create/update jobs'),
('perm-job-approve', 'job:approve', 'job', 'approve', 'Approve/reject jobs'),
('perm-job-delete', 'job:delete', 'job', 'delete', 'Delete jobs'),
-- Recruitment
('perm-recruitment-read', 'recruitment:read', 'recruitment', 'read', 'View applications'),
('perm-recruitment-write', 'recruitment:write', 'recruitment', 'write', 'Manage applications'),
('perm-interview-schedule', 'interview:schedule', 'interview', 'schedule', 'Schedule interviews'),
('perm-interview-feedback', 'interview:feedback', 'interview', 'feedback', 'Submit interview feedback'),
('perm-offer-manage', 'offer:manage', 'offer', 'manage', 'Manage job offers'),
-- Learning
('perm-learning-read', 'learning:read', 'learning', 'read', 'View courses'),
('perm-learning-write', 'learning:write', 'learning', 'write', 'Create/update courses'),
('perm-enrollment-manage', 'enrollment:manage', 'enrollment', 'manage', 'Manage enrollments'),
-- Assessment
('perm-assessment-read', 'assessment:read', 'assessment', 'read', 'View assessments'),
('perm-assessment-write', 'assessment:write', 'assessment', 'write', 'Create assessments'),
('perm-assessment-take', 'assessment:take', 'assessment', 'take', 'Take assessments'),
('perm-assessment-grade', 'assessment:grade', 'assessment', 'grade', 'Grade assessments'),
-- AI Career
('perm-ai-read', 'ai:read', 'ai', 'read', 'View AI recommendations'),
('perm-ai-chat', 'ai:chat', 'ai', 'chat', 'Use AI chat'),
('perm-ai-cv-review', 'ai:cv_review', 'ai', 'cv_review', 'Use AI CV review')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- ROLE-PERMISSIONS (50)
-- =============================================================================
-- ADMIN: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-admin', id FROM permissions;

-- STAFF: profile approval, job approval, recruitment management
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-staff', 'perm-user-read'),
('role-staff', 'perm-profile-read'),
('role-staff', 'perm-profile-approve'),
('role-staff', 'perm-profile-edit-self'),
('role-staff', 'perm-job-read'),
('role-staff', 'perm-job-approve'),
('role-staff', 'perm-recruitment-read'),
('role-staff', 'perm-recruitment-write'),
('role-staff', 'perm-interview-schedule'),
('role-staff', 'perm-interview-feedback'),
('role-staff', 'perm-offer-manage'),
('role-staff', 'perm-learning-read'),
('role-staff', 'perm-assessment-read'),
('role-staff', 'perm-ai-read')
ON CONFLICT DO NOTHING;

-- HR: job management, recruitment, view profiles
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-hr', 'perm-user-read'),
('role-hr', 'perm-profile-read'),
('role-hr', 'perm-job-read'),
('role-hr', 'perm-job-write'),
('role-hr', 'perm-recruitment-read'),
('role-hr', 'perm-recruitment-write'),
('role-hr', 'perm-interview-schedule'),
('role-hr', 'perm-interview-feedback'),
('role-hr', 'perm-offer-manage'),
('role-hr', 'perm-learning-read'),
('role-hr', 'perm-assessment-read'),
('role-hr', 'perm-ai-read')
ON CONFLICT DO NOTHING;

-- CANDIDATE: own profile, apply to jobs, learning, assessments, AI
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-candidate', 'perm-user-read'),
('role-candidate', 'perm-profile-read'),
('role-candidate', 'perm-profile-write'),
('role-candidate', 'perm-job-read'),
('role-candidate', 'perm-recruitment-read'),
('role-candidate', 'perm-recruitment-write'),
('role-candidate', 'perm-learning-read'),
('role-candidate', 'perm-enrollment-manage'),
('role-candidate', 'perm-assessment-read'),
('role-candidate', 'perm-assessment-take'),
('role-candidate', 'perm-ai-read'),
('role-candidate', 'perm-ai-chat'),
('role-candidate', 'perm-ai-cv-review')
ON CONFLICT DO NOTHING;

-- MENTOR: similar to HR but focused on learning/assessment
INSERT INTO role_permissions (role_id, permission_id) VALUES
('role-mentor', 'perm-user-read'),
('role-mentor', 'perm-profile-read'),
('role-mentor', 'perm-learning-read'),
('role-mentor', 'perm-learning-write'),
('role-mentor', 'perm-assessment-read'),
('role-mentor', 'perm-assessment-write'),
('role-mentor', 'perm-assessment-grade'),
('role-mentor', 'perm-ai-read')
ON CONFLICT DO NOTHING;

-- =============================================================================
-- USERS (50)
-- 1 System Admin, 2 Admin, 5 Recruiter/Mentor, 40 Candidate, 2 Staff
-- =============================================================================
-- System Admin
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-admin-001', 'admin@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'System Admin', true, true, NOW(), NOW());

-- Admins (2)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-admin-002', 'admin1@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Quản trị hệ thống 1', true, true, NOW(), NOW()),
('user-admin-003', 'admin2@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Quản trị hệ thống 2', true, true, NOW(), NOW());

-- Staff (2)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-staff-001', 'staff1@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Hoàng Thu Hà', true, true, NOW(), NOW()),
('user-staff-002', 'staff2@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Trần Minh Tuấn', true, true, NOW(), NOW());

-- HR/Recruiters (5) - linked to companies
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-hr-001', 'hr1@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'HR Rikkeisoft', true, true, NOW(), NOW()),
('user-hr-002', 'hr2@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'HR Rikkei Digital', true, true, NOW(), NOW()),
('user-hr-003', 'hr3@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'HR FPT Software', true, true, NOW(), NOW()),
('user-hr-004', 'hr4@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'HR VNPT Technology', true, true, NOW(), NOW()),
('user-hr-005', 'hr5@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'HR Viettel Solutions', true, true, NOW(), NOW());

-- Mentors (5)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-mentor-001', 'mentor1@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Phạm Minh Đức', true, true, NOW(), NOW()),
('user-mentor-002', 'mentor2@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Nguyễn Thị Lan', true, true, NOW(), NOW()),
('user-mentor-003', 'mentor3@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Lê Văn Hùng', true, true, NOW(), NOW()),
('user-mentor-004', 'mentor4@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Trần Thị Mai', true, true, NOW(), NOW()),
('user-mentor-005', 'mentor5@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Hoàng Văn Nam', true, true, NOW(), NOW());

-- Candidates (40) - will have profiles in profile service
-- Batch 1: Java Fullstack (RA-JAVA-2026)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-cand-001', 'candidate1@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Nguyễn Văn An', true, true, NOW(), NOW()),
('user-cand-002', 'candidate2@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Trần Thị Bình', true, true, NOW(), NOW()),
('user-cand-003', 'candidate3@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Lê Hoàng Cường', true, true, NOW(), NOW()),
('user-cand-004', 'candidate4@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Phạm Thị Dung', true, true, NOW(), NOW()),
('user-cand-005', 'candidate5@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Võ Minh Đức', true, true, NOW(), NOW()),
('user-cand-006', 'candidate6@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Ngô Thị Giang', true, true, NOW(), NOW()),
('user-cand-007', 'candidate7@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đặng Văn Hiếu', true, true, NOW(), NOW()),
('user-cand-008', 'candidate8@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Bùi Thị Hoa', true, true, NOW(), NOW());

-- Batch 2: Frontend (RA-FE-2026)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-cand-009', 'candidate9@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Huỳnh Văn Khang', true, true, NOW(), NOW()),
('user-cand-010', 'candidate10@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đỗ Thị Lan', true, true, NOW(), NOW()),
('user-cand-011', 'candidate11@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Phan Văn Minh', true, true, NOW(), NOW()),
('user-cand-012', 'candidate12@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Nguyễn Thị Nga', true, true, NOW(), NOW()),
('user-cand-013', 'candidate13@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Trần Văn Oanh', true, true, NOW(), NOW()),
('user-cand-014', 'candidate14@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Lê Thị Phương', true, true, NOW(), NOW()),
('user-cand-015', 'candidate15@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Võ Văn Quang', true, true, NOW(), NOW()),
('user-cand-016', 'candidate16@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Ngô Thị Quyên', true, true, NOW(), NOW());

-- Batch 3: DevOps/Cloud (RA-DO-2026)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-cand-017', 'candidate17@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đặng Văn Sơn', true, true, NOW(), NOW()),
('user-cand-018', 'candidate18@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Bùi Thị Thảo', true, true, NOW(), NOW()),
('user-cand-019', 'candidate19@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Huỳnh Văn Út', true, true, NOW(), NOW()),
('user-cand-020', 'candidate20@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đỗ Thị Vân', true, true, NOW(), NOW()),
('user-cand-021', 'candidate21@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Phan Văn Xuân', true, true, NOW(), NOW()),
('user-cand-022', 'candidate22@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Nguyễn Thị Yến', true, true, NOW(), NOW()),
('user-cand-023', 'candidate23@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Trần Văn Ánh', true, true, NOW(), NOW()),
('user-cand-024', 'candidate24@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Lê Thị Bình', true, true, NOW(), NOW());

-- Batch 4: Data/AI (RA-DA-2026)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-cand-025', 'candidate25@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Võ Văn Cường', true, true, NOW(), NOW()),
('user-cand-026', 'candidate26@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Ngô Thị Diễm', true, true, NOW(), NOW()),
('user-cand-027', 'candidate27@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đặng Văn Em', true, true, NOW(), NOW()),
('user-cand-028', 'candidate28@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Bùi Thị Phượng', true, true, NOW(), NOW()),
('user-cand-029', 'candidate29@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Huỳnh Văn Giang', true, true, NOW(), NOW()),
('user-cand-030', 'candidate30@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đỗ Thị Hương', true, true, NOW(), NOW()),
('user-cand-031', 'candidate31@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Phan Văn Hùng', true, true, NOW(), NOW()),
('user-cand-032', 'candidate32@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Nguyễn Thị Loan', true, true, NOW(), NOW());

-- Batch 5: Mobile/QA (RA-MQ-2026)
INSERT INTO users (id, email, password_hash, full_name, is_active, email_verified, created_at, updated_at) VALUES
('user-cand-033', 'candidate33@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Trần Văn Minh', true, true, NOW(), NOW()),
('user-cand-034', 'candidate34@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Lê Thị Nga', true, true, NOW(), NOW()),
('user-cand-035', 'candidate35@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Võ Văn Oanh', true, true, NOW(), NOW()),
('user-cand-036', 'candidate36@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Ngô Thị Quyên', true, true, NOW(), NOW()),
('user-cand-037', 'candidate37@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đặng Văn Sơn', true, true, NOW(), NOW()),
('user-cand-038', 'candidate38@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Bùi Thị Thảo', true, true, NOW(), NOW()),
('user-cand-039', 'candidate39@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Huỳnh Văn Út', true, true, NOW(), NOW()),
('user-cand-040', 'candidate40@rikkeijobs.test', '$2a$10$X7UrH5YxX5YxX5YxX5YxXeX5YxX5YxX5YxX5YxX5YxX5YxX5YxX5Y', 'Đỗ Thị Vân', true, true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- USER-ROLES (60)
-- =============================================================================
-- System Admin
INSERT INTO user_roles (user_id, role_id) VALUES
('user-admin-001', 'role-admin');

-- Admins
INSERT INTO user_roles (user_id, role_id) VALUES
('user-admin-002', 'role-admin'),
('user-admin-003', 'role-admin');

-- Staff
INSERT INTO user_roles (user_id, role_id) VALUES
('user-staff-001', 'role-staff'),
('user-staff-002', 'role-staff');

-- HR
INSERT INTO user_roles (user_id, role_id) VALUES
('user-hr-001', 'role-hr'),
('user-hr-002', 'role-hr'),
('user-hr-003', 'role-hr'),
('user-hr-004', 'role-hr'),
('user-hr-005', 'role-hr');

-- Mentors
INSERT INTO user_roles (user_id, role_id) VALUES
('user-mentor-001', 'role-mentor'),
('user-mentor-002', 'role-mentor'),
('user-mentor-003', 'role-mentor'),
('user-mentor-004', 'role-mentor'),
('user-mentor-005', 'role-mentor');

-- Candidates (40)
INSERT INTO user_roles (user_id, role_id)
SELECT id, 'role-candidate' FROM users WHERE id LIKE 'user-cand-%'
ON CONFLICT DO NOTHING;

-- =============================================================================
-- COMPANIES (10) - for HR users
-- =============================================================================
INSERT INTO companies (id, name, tax_code, industry, size, address, city, website, description, status, created_at, updated_at) VALUES
('comp-001', 'Công ty Cổ phần Rikkeisoft', '0101234567', 'Công nghệ thông tin', '201-1000', 'Tầng 21, Tháp Keangnam, Mễ Trì', 'Hà Nội', 'https://rikkeisoft.com', 'Doanh nghiệp phát triển phần mềm và đào tạo công nghệ', 'ACTIVE', NOW(), NOW()),
('comp-002', 'Rikkei Digital Solutions', '0107654321', 'Công nghệ thông tin', '51-200', 'Quận Hải Châu', 'Đà Nẵng', 'https://rikkeidigital.com', 'Doanh nghiệp phát triển giải pháp số', 'ACTIVE', NOW(), NOW()),
('comp-003', 'FPT Software', '0102345678', 'Công nghệ thông tin', '1000+', 'Tòa nhà FPT, Đường Hòa Lạc', 'Hà Nội', 'https://fpt.com.vn', 'Tổ chức công nghệ hàng đầu Việt Nam', 'ACTIVE', NOW(), NOW()),
('comp-004', 'VNPT Technology', '0103456789', 'Viễn thông & CNTT', '1000+', '123 Nguyễn Văn Cừ', 'Hà Nội', 'https://vnpt.vn', 'Tập đoàn Viễn thông Bưu chính', 'ACTIVE', NOW(), NOW()),
('comp-005', 'Viettel Solutions', '0104567890', 'Viễn thông & CNTT', '1000+', '285 Cách Mạng Tháng 8', 'Hà Nội', 'https://viettelsolutions.com', 'Đơn vị công nghệ của Viettel', 'ACTIVE', NOW(), NOW()),
('comp-006', 'CMC Corporation', '0105678901', 'Công nghệ thông tin', '501-1000', 'Tòa nhà CMC, Đường Duy Tân', 'Hà Nội', 'https://cmc.com.vn', 'Tập đoàn công nghệ CMC', 'ACTIVE', NOW(), NOW()),
('comp-007', 'KMS Technology', '0106789012', 'Công nghệ thông tin', '201-500', 'Tòa nhà Keangnam', 'Hà Nội', 'https://kms-technology.com', 'Phát triển sản phẩm phần mềm', 'ACTIVE', NOW(), NOW()),
('comp-008', 'NashTech Vietnam', '0107890123', 'Công nghệ thông tin', '1000+', 'Tòa nhà Lotte Center', 'Hà Nội', 'https://nashtechglobal.com', 'Giải pháp chuyển đổi số', 'ACTIVE', NOW(), NOW()),
('comp-009', 'Sotatek', '0108901234', 'Công nghệ thông tin', '201-500', 'Tòa nhà Capital Place', 'Hà Nội', 'https://sotatek.com', 'Blockchain & AI solutions', 'ACTIVE', NOW(), NOW()),
('comp-010', 'TMA Solutions', '0109012345', 'Công nghệ thông tin', '1000+', 'Tòa nhà IT Park', 'Hồ Chí Minh', 'https://tmasolutions.com', 'Outsourcing phần mềm', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Update HR users with company info
UPDATE users SET company_id = 'comp-001', company_name = 'Công ty Cổ phần Rikkeisoft' WHERE id = 'user-hr-001';
UPDATE users SET company_id = 'comp-002', company_name = 'Rikkei Digital Solutions' WHERE id = 'user-hr-002';
UPDATE users SET company_id = 'comp-003', company_name = 'FPT Software' WHERE id = 'user-hr-003';
UPDATE users SET company_id = 'comp-004', company_name = 'VNPT Technology' WHERE id = 'user-hr-004';
UPDATE users SET company_id = 'comp-005', company_name = 'Viettel Solutions' WHERE id = 'user-hr-005';

-- =============================================================================
-- REFRESH TOKENS (30) - sample tokens for active users
-- =============================================================================
INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at, revoked) VALUES
('rt-001', 'user-admin-001', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-002', 'user-admin-002', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-003', 'user-admin-003', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-004', 'user-staff-001', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-005', 'user-staff-002', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-006', 'user-hr-001', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-007', 'user-hr-002', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-008', 'user-hr-003', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-009', 'user-hr-004', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-010', 'user-hr-005', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-011', 'user-mentor-001', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-012', 'user-mentor-002', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-013', 'user-mentor-003', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-014', 'user-mentor-004', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false),
('rt-015', 'user-mentor-005', 'hash-' || gen_random_uuid(), NOW() + INTERVAL '30 days', NOW(), false)
ON CONFLICT (id) DO NOTHING;

-- Add tokens for some candidates
INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, created_at, revoked)
SELECT
    'rt-' || LPAD((ROW_NUMBER() OVER())::text, 3, '0'),
    id,
    'hash-' || gen_random_uuid(),
    NOW() + INTERVAL '30 days',
    NOW(),
    false
FROM users WHERE id LIKE 'user-cand-%' LIMIT 15
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- LOGIN HISTORY (100)
-- =============================================================================
INSERT INTO login_history (id, user_id, ip_address, user_agent, success, created_at)
SELECT
    'lh-' || LPAD((ROW_NUMBER() OVER())::text, 4, '0'),
    id,
    ('192.168.' || (RANDOM() * 255)::int || '.' || (RANDOM() * 255)::int)::inet,
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    true,
    NOW() - (RANDOM() * INTERVAL '30 days')
FROM users, generate_series(1, 2)
WHERE id LIKE 'user-%'
LIMIT 100
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- VERIFICATION
-- =============================================================================
SELECT 'roles' as table_name, COUNT(*) FROM roles
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'user_roles', COUNT(*) FROM user_roles
UNION ALL SELECT 'companies', COUNT(*) FROM companies
UNION ALL SELECT 'refresh_tokens', COUNT(*) FROM refresh_tokens
UNION ALL SELECT 'login_history', COUNT(*) FROM login_history;