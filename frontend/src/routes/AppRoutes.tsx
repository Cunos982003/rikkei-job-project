import { Routes, Route, Navigate } from "react-router-dom";
import { useAuthStore, normalizeRole } from "../store/useAuthStore";
import { MainLayout } from "../layouts/MainLayout";
import { AuthLayout } from "../layouts/AuthLayout";
import { ProtectedRoute } from "../components/auth/ProtectedRoute";
import { RoleGuard } from "../components/auth/RoleGuard";

// Pages
import { LoginPage } from "../pages/auth/LoginPage";
import { ResetPasswordPage } from "../pages/auth/ResetPasswordPage";
import { UnauthorizedPage } from "../pages/common/UnauthorizedPage";
import { NotFoundPage } from "../pages/common/NotFoundPage";

// Feature Profile Pages
import EvaluationsPage from "../features/profile/EvaluationsPage";
import InternshipsPage from "../features/profile/InternshipsPage";
import { SkillProfilePage } from "../features/profile/SkillProfilePage";

// DG-02: Self Assessment Pages
import SelfAssessmentPage from "../features/profile/SelfAssessmentPage";
import SkillComparisonPage from "../features/profile/SkillComparisonPage";

// DG-04: Mentor Evaluation Pages
import MentorTaskListPage from "../features/profile/MentorTaskListPage";
import MentorEvaluationFormPage from "../features/profile/MentorEvaluationFormPage";

// Role Dashboard Pages
import { CandidateProfilePage } from "../pages/candidate/CandidateProfilePage";
import CandidateJobsPage from "../pages/candidate/CandidateJobsPage";
import CandidateLearningPage from "../pages/candidate/CandidateLearningPage";
import { CandidateInterviewsPage } from "../pages/candidate/CandidateInterviewsPage";
import { CandidateOffersPage } from "../pages/candidate/CandidateOffersPage";
import { CandidateRecruitmentsPage } from "../pages/candidate/CandidateRecruitmentsPage";
import { CareerLayout } from "../features/career/CareerLayout";
import { OverviewPage } from "../pages/career/OverviewPage";
import { RecommendationsPage } from "../pages/career/RecommendationsPage";
import { SkillsPage } from "../pages/career/SkillsPage";
import { HrCompanyPage } from "../pages/hr/HrCompanyPage";
import { HrInterviewSchedulePage } from "../pages/hr/HrInterviewSchedulePage";
import { MentorStudentsPage } from "../pages/mentor/MentorStudentsPage";
import { StaffJobsPage } from "../pages/staff/StaffJobsPage";
import { StaffCandidatesPage } from "../pages/staff/StaffCandidatesPage";
import { StaffCandidateDetailPage } from "../pages/staff/StaffCandidateDetailPage";
import { StaffRecruitmentsPage } from "../pages/staff/StaffRecruitmentsPage";
import { RecruitmentDetailPage } from "../pages/hr/RecruitmentDetailPage";
import { AdminSkillsPage } from "../pages/admin/AdminSkillsPage";

function RootRedirect() {
    const { user, isAuthenticated } = useAuthStore();
    if (!isAuthenticated || !user) return <Navigate to="/login" replace />;
    switch (normalizeRole(user.role)) {
        case "HR": return <Navigate to="/hr/company" replace />;
        case "MENTOR": return <Navigate to="/mentor/students" replace />;
        case "STAFF": return <Navigate to="/staff/candidates" replace />;
        case "ADMIN": return <Navigate to="/admin/skills" replace />;
        case "CANDIDATE":
        default: return <Navigate to="/candidate/profile" replace />;
    }
}

export function AppRoutes() {
    return (
        <Routes>
            <Route element={<AuthLayout />}>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/reset-password" element={<ResetPasswordPage />} />
            </Route>
            <Route path="/unauthorized" element={<UnauthorizedPage />} />

            <Route element={<ProtectedRoute />}>
                <Route element={<MainLayout />}>
                    <Route path="/" element={<RootRedirect />} />
                    <Route path="/profile" element={<RootRedirect />} />

                    {/* CANDIDATE */}
                    <Route element={<RoleGuard allowedRoles={["CANDIDATE"]} />}>
                        <Route path="/candidate/profile" element={<CandidateProfilePage />} />
                        <Route path="/candidate/jobs" element={<CandidateJobsPage />} />
                        <Route path="/candidate/career" element={<CareerLayout />}>
                            <Route index element={<OverviewPage />} />
                            <Route path="recommendations" element={<RecommendationsPage />} />
                            <Route path="skills" element={<SkillsPage />} />
                        </Route>
                        <Route path="/candidate/recruitments" element={<CandidateRecruitmentsPage />} />
                        <Route path="/candidate/applications" element={<CandidateRecruitmentsPage />} />
                        <Route path="/candidate/interviews" element={<CandidateInterviewsPage />} />
                        <Route path="/candidate/offers" element={<CandidateOffersPage />} />
                        <Route path="/candidate/learning" element={<CandidateLearningPage />} />
                        <Route path="/candidate/evaluations" element={<EvaluationsPage />} />
                        <Route path="/profile/evaluations" element={<EvaluationsPage />} />
                        <Route path="/profile/skills" element={<SkillProfilePage />} />
                        <Route path="/profile/internships" element={<InternshipsPage />} />
                        <Route path="/profile/self-assessment" element={<SelfAssessmentPage />} />
                        <Route path="/profile/skill-comparison" element={<SkillComparisonPage />} />
                    </Route>

                    {/* HR */}
                    <Route element={<RoleGuard allowedRoles={["HR"]} />}>
                        <Route path="/hr/company" element={<HrCompanyPage />} />
                        <Route path="/hr/jobs" element={<HrCompanyPage />} />
                        <Route path="/hr/candidates" element={<HrCompanyPage />} />
                        <Route path="/hr/interviews" element={<HrInterviewSchedulePage />} />
                        <Route path="/hr/applications/:id" element={<RecruitmentDetailPage />} />
                        <Route path="/hr/recruitments/:id" element={<RecruitmentDetailPage />} />
                        <Route path="/hr/ioc" element={<HrCompanyPage />} />
                    </Route>

                    {/* MENTOR */}
                    <Route element={<RoleGuard allowedRoles={["MENTOR"]} />}>
                        <Route path="/mentor/students" element={<MentorStudentsPage />} />
                        <Route path="/mentor/evaluations" element={<MentorTaskListPage />} />
                        <Route path="/mentor/evaluations/:id" element={<MentorEvaluationFormPage />} />
                    </Route>

                    {/* STAFF & ADMIN */}
                    <Route element={<RoleGuard allowedRoles={["STAFF", "ADMIN"]} />}>
                        <Route path="/staff/jobs" element={<StaffJobsPage />} />
                        <Route path="/staff/companies" element={<StaffJobsPage />} />
                        <Route path="/staff/recruitments" element={<StaffRecruitmentsPage />} />
                        <Route path="/staff/recruitments/:id" element={<RecruitmentDetailPage />} />
                        <Route path="/staff/ioc" element={<StaffJobsPage />} />
                        <Route path="/staff/unlock-contacts" element={<StaffJobsPage />} />
                        <Route path="/staff/candidates" element={<StaffCandidatesPage />} />
                        <Route path="/staff/candidates/:userId" element={<StaffCandidateDetailPage />} />
                    </Route>
                    <Route element={<RoleGuard allowedRoles={["ADMIN"]} />}>
                        <Route path="/admin/skills" element={<AdminSkillsPage />} />
                        <Route path="/admin/badges" element={<AdminSkillsPage />} />
                        <Route path="/admin/overrides" element={<AdminSkillsPage />} />
                        <Route path="/admin/audit-logs" element={<AdminSkillsPage />} />
                    </Route>
                </Route>
            </Route>
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
}