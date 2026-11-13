import { Routes, Route, Navigate } from "react-router-dom";
import { useAuthStore } from "../store/useAuthStore";
import { MainLayout } from "../layouts/MainLayout";
import { AuthLayout } from "../layouts/AuthLayout";
import { ProtectedRoute } from "../components/auth/ProtectedRoute";
import { RoleGuard } from "../components/auth/RoleGuard";
import type { Role } from "../api/endpoints/auth.api";

// Pages
import { LoginPage } from "../pages/auth/LoginPage";
import { UnauthorizedPage } from "../pages/common/UnauthorizedPage";
import { NotFoundPage } from "../pages/common/NotFoundPage";
import EvaluationsPage from "../features/profile/EvaluationsPage";
import { SkillProfilePage } from "../features/profile/SkillProfilePage";

// Role Dashboard Pages
import { CandidateProfilePage } from "../pages/candidate/CandidateProfilePage";
import { CandidateJobsPage } from "../pages/candidate/CandidateJobsPage";
import { CandidateApplicationsPage } from "../pages/candidate/CandidateApplicationsPage";
import { CandidateLearningPage } from "../pages/candidate/CandidateLearningPage";
import { CandidateRoadmapPage } from "../pages/candidate/CandidateRoadmapPage";
import { CandidateAiAssistantPage } from "../pages/candidate/CandidateAiAssistantPage";
import { HrCompanyPage } from "../pages/hr/HrCompanyPage";
import { MentorStudentsPage } from "../pages/mentor/MentorStudentsPage";
import { MentorEvaluationsPage } from "../pages/mentor/MentorEvaluationsPage";
import { StaffJobsPage } from "../pages/staff/StaffJobsPage";
import { StaffProfilesPage } from "../pages/staff/StaffProfilesPage";
import { StaffReportsPage } from "../pages/staff/StaffReportsPage";
import { AdminSkillsPage } from "../pages/admin/AdminSkillsPage";

const ROLE_HOME_PATHS: Record<Role, string> = {
  CANDIDATE: "/candidate/profile",
  HR: "/hr/company",
  MENTOR: "/mentor/students",
  STAFF: "/staff/jobs",
  ADMIN: "/admin/skills",
};

function getRoleHomePath(role: Role) {
  return ROLE_HOME_PATHS[role] ?? ROLE_HOME_PATHS.CANDIDATE;
}

/**
 * Điều hướng trang chủ thông minh theo vai trò người dùng
 */
function RootRedirect() {
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  return <Navigate to={getRoleHomePath(user.role)} replace />;
}

export function AppRoutes() {
  return (
    <Routes>
      {/* 1. Public Authentication Routes (Bọc trong AuthLayout) */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* 2. Error Fallback Routes */}
      <Route path="/unauthorized" element={<UnauthorizedPage />} />

      {/* 3. Protected Workspace Routes (Bọc trong MainLayout) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          {/* Trang chủ điều hướng thông minh */}
          <Route path="/" element={<RootRedirect />} />
          <Route path="/profile" element={<RootRedirect />} />

          {/* Phân hệ CANDIDATE */}
          <Route element={<RoleGuard allowedRoles={["CANDIDATE"]} />}>
            <Route path="/candidate/profile" element={<CandidateProfilePage />} />
            <Route path="/candidate/jobs" element={<CandidateJobsPage />} />
            <Route path="/candidate/applications" element={<CandidateApplicationsPage />} />
            <Route path="/candidate/learning" element={<CandidateLearningPage />} />
            <Route path="/candidate/roadmap" element={<CandidateRoadmapPage />} />
            <Route path="/candidate/ai-assistant" element={<CandidateAiAssistantPage />} />
            <Route path="/candidate/evaluations" element={<EvaluationsPage />} />
            <Route path="/profile/evaluations" element={<EvaluationsPage />} />
            <Route path="/profile/skills" element={<SkillProfilePage />} />
          </Route>

          {/* Phân hệ HR (Doanh nghiệp) */}
          <Route element={<RoleGuard allowedRoles={["HR"]} />}>
            <Route path="/hr/company" element={<HrCompanyPage />} />
            <Route path="/hr/jobs" element={<HrCompanyPage />} />
            <Route path="/hr/candidates" element={<HrCompanyPage />} />
            <Route path="/hr/interviews" element={<HrCompanyPage />} />
            <Route path="/hr/ioc" element={<HrCompanyPage />} />
          </Route>

          {/* Phân hệ MENTOR */}
          <Route element={<RoleGuard allowedRoles={["MENTOR"]} />}>
            <Route path="/mentor/students" element={<MentorStudentsPage />} />
            <Route path="/mentor/evaluations" element={<MentorEvaluationsPage />} />
          </Route>

          {/* Phân hệ STAFF (Cán bộ tuyển dụng & Quản trị) */}
          <Route element={<RoleGuard allowedRoles={["STAFF", "ADMIN"]} />}>
            <Route path="/staff" element={<Navigate to="/staff/jobs" replace />} />
            <Route path="/staff/profiles" element={<StaffProfilesPage />} />
            <Route path="/staff/jobs" element={<StaffJobsPage />} />
            <Route path="/staff/companies" element={<StaffJobsPage />} />
            <Route path="/staff/ioc" element={<StaffJobsPage />} />
            <Route path="/staff/reports" element={<StaffReportsPage />} />
            <Route path="/staff/unlock-contacts" element={<StaffJobsPage />} />
          </Route>

          {/* Phân hệ ADMIN (Quản trị hệ thống) */}
          <Route element={<RoleGuard allowedRoles={["ADMIN"]} />}>
            <Route path="/admin" element={<Navigate to="/admin/skills" replace />} />
            <Route path="/admin/skills" element={<AdminSkillsPage />} />
            <Route path="/admin/badges" element={<AdminSkillsPage />} />
            <Route path="/admin/overrides" element={<AdminSkillsPage />} />
            <Route path="/admin/audit-logs" element={<AdminSkillsPage />} />
          </Route>
        </Route>
      </Route>

      {/* 4. Catch-all 404 Route */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

