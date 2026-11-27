import { Suspense, lazy } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { useAuthStore, normalizeRole } from "../store/useAuthStore";
import { MainLayout } from "../layouts/MainLayout";
import { AuthLayout } from "../layouts/AuthLayout";
import { ProtectedRoute } from "../components/auth/ProtectedRoute";
import { RoleGuard } from "../components/auth/RoleGuard";
import { RouteLoadingFallback } from "../components/feedback/RouteLoadingFallback";
import { FeaturePlaceholderPage } from "../pages/common/FeaturePlaceholderPage";

// Pages
const LoginPage = lazy(() => import("../pages/auth/LoginPage").then(m => ({ default: m.LoginPage })));
const ResetPasswordPage = lazy(() => import("../pages/auth/ResetPasswordPage").then(m => ({ default: m.ResetPasswordPage })));
const UnauthorizedPage = lazy(() => import("../pages/common/UnauthorizedPage").then(m => ({ default: m.UnauthorizedPage })));
const NotFoundPage = lazy(() => import("../pages/common/NotFoundPage").then(m => ({ default: m.NotFoundPage })));

// HS-04 Public CV Page (no auth required)
const PublicCVPage = lazy(() => import("../pages/public/PublicCVPage").then(m => ({ default: m.PublicCVPage })));

// Feature Profile Pages
const EvaluationsPage = lazy(() => import("../features/profile/EvaluationsPage"));
const InternshipsPage = lazy(() => import("../features/profile/InternshipsPage"));
const SkillProfilePage = lazy(() => import("../features/profile/SkillProfilePage").then(m => ({ default: m.SkillProfilePage })));

// DG-02: Self Assessment Pages
const SelfAssessmentPage = lazy(() => import("../features/profile/SelfAssessmentPage"));
const SkillComparisonPage = lazy(() => import("../features/profile/SkillComparisonPage"));

// DG-04: Mentor Evaluation Pages
const MentorTaskListPage = lazy(() => import("../features/profile/MentorTaskListPage"));
const MentorEvaluationFormPage = lazy(() => import("../features/profile/MentorEvaluationFormPage"));

// Role Dashboard Pages
const CandidateProfilePage = lazy(() => import("../pages/candidate/CandidateProfilePage").then(m => ({ default: m.CandidateProfilePage })));
const CandidateJobsPage = lazy(() => import("../pages/candidate/CandidateJobsPage").then(m => ({ default: m.default || m.CandidateJobsPage })));
const CandidateJobDetailPage = lazy(() => import("../pages/candidate/CandidateJobDetailPage").then(m => ({ default: m.default || m.CandidateJobDetailPage })));
const CandidateSavedJobsPage = lazy(() => import("../pages/candidate/CandidateSavedJobsPage").then(m => ({ default: m.default || m.CandidateSavedJobsPage })));
const CandidateLearningPage = lazy(() => import("../pages/candidate/CandidateLearningPage"));
const CandidateInterviewsPage = lazy(() => import("../pages/candidate/CandidateInterviewsPage").then(m => ({ default: m.CandidateInterviewsPage })));
const CandidateOffersPage = lazy(() => import("../pages/candidate/CandidateOffersPage").then(m => ({ default: m.CandidateOffersPage })));
const CandidateRecruitmentsPage = lazy(() => import("../pages/candidate/CandidateRecruitmentsPage").then(m => ({ default: m.CandidateRecruitmentsPage })));
const CandidateCVPage = lazy(() => import("../pages/candidate/CandidateCVPage").then(m => ({ default: m.CandidateCVPage })));
const CareerLayout = lazy(() => import("../features/career/CareerLayout").then(m => ({ default: m.CareerLayout })));
const OverviewPage = lazy(() => import("../pages/career/OverviewPage").then(m => ({ default: m.OverviewPage })));
const RecommendationsPage = lazy(() => import("../pages/career/RecommendationsPage").then(m => ({ default: m.RecommendationsPage })));
const SkillsPage = lazy(() => import("../pages/career/SkillsPage").then(m => ({ default: m.SkillsPage })));
const HrCompanyPage = lazy(() => import("../pages/hr/HrCompanyPage").then(m => ({ default: m.HrCompanyPage })));
const HrJobsPage = lazy(() => import("../pages/hr/HrJobsPage").then(m => ({ default: m.HrJobsPage })));
const HrJobFormPage = lazy(() => import("../pages/hr/HrJobFormPage").then(m => ({ default: m.HrJobFormPage })));
const HrInterviewSchedulePage = lazy(() => import("../pages/hr/HrInterviewSchedulePage").then(m => ({ default: m.HrInterviewSchedulePage })));
const HrApplicationsPage = lazy(() => import("../pages/hr/HrApplicationsPage").then(m => ({ default: m.HrApplicationsPage })));
const InterviewPracticeListPage = lazy(() => import("../pages/candidate/InterviewPracticeListPage").then(m => ({ default: m.InterviewPracticeListPage })));
const InterviewPracticePage = lazy(() => import("../pages/candidate/InterviewPracticePage").then(m => ({ default: m.InterviewPracticePage })));
const MentorStudentsPage = lazy(() => import("../pages/mentor/MentorStudentsPage").then(m => ({ default: m.MentorStudentsPage })));
const StaffJobsPage = lazy(() => import("../pages/staff/StaffJobsPage").then(m => ({ default: m.StaffJobsPage })));
const StaffCompaniesPage = lazy(() => import("../pages/staff/StaffCompaniesPage").then(m => ({ default: m.StaffCompaniesPage })));
const StaffCompanyDetailPage = lazy(() => import("../pages/staff/StaffCompanyDetailPage").then(m => ({ default: m.StaffCompanyDetailPage })));
const StaffCandidatesPage = lazy(() => import("../pages/staff/StaffCandidatesPage").then(m => ({ default: m.StaffCandidatesPage })));
const StaffCandidateDetailPage = lazy(() => import("../pages/staff/StaffCandidateDetailPage").then(m => ({ default: m.StaffCandidateDetailPage })));
const StaffRecruitmentsPage = lazy(() => import("../pages/staff/StaffRecruitmentsPage").then(m => ({ default: m.StaffRecruitmentsPage })));
const RecruitmentDetailPage = lazy(() => import("../pages/hr/RecruitmentDetailPage").then(m => ({ default: m.RecruitmentDetailPage })));
const StaffContactRequestsPage = lazy(() => import("../pages/staff/StaffContactRequestsPage").then(m => ({ default: m.StaffContactRequestsPage })));
const StaffContactAccessLogsPage = lazy(() => import("../pages/staff/StaffContactAccessLogsPage").then(m => ({ default: m.StaffContactAccessLogsPage })));

// Admin Pages
const AdminSkillsPage = lazy(() => import("../pages/admin/AdminSkillsPage").then(m => ({ default: m.AdminSkillsPage })));

// DG-03: Admin Badge Management
const AdminBadgesPage = lazy(() => import("../pages/admin/AdminBadgesPage"));

// TT-02: BỔ SUNG IMPORT 2 TRANG MỚI
const HrInternsPage = lazy(() => import("../pages/hr/HrInternsPage"));
const StaffPlacementPage = lazy(() => import("../pages/staff/StaffPlacementPage"));
const HrJobDetailPage = lazy(() => import("../pages/hr/HrJobDetailPage").then(m => ({ default: m.HrJobDetailPage })));
const StaffJobDetailPage = lazy(() => import("../pages/staff/StaffJobDetailPage").then(m => ({ default: m.StaffJobDetailPage })));

function RootRedirect() {
    const { user, isAuthenticated } = useAuthStore();

    if (!isAuthenticated || !user) {
        return <Navigate to="/login" replace />;
    }

    switch (normalizeRole(user.role)) {
        case "HR":
            return <Navigate to="/hr/company" replace />;

        case "MENTOR":
            return <Navigate to="/mentor/students" replace />;

        case "STAFF":
            return <Navigate to="/staff/companies" replace />;

        case "ADMIN":
            return <Navigate to="/admin/skills" replace />;

        case "CANDIDATE":
        default:
            return <Navigate to="/candidate/jobs" replace />;
    }
}

export function AppRoutes() {
    return (
        <Suspense fallback={<RouteLoadingFallback />}>
            <Routes>
            {/* Authentication */}
            <Route element={<AuthLayout />}>
                <Route path="/login" element={<LoginPage />} />
                <Route
                    path="/reset-password"
                    element={<ResetPasswordPage />}
                />
            </Route>

            <Route
                path="/unauthorized"
                element={<UnauthorizedPage />}
            />

            {/* Public CV Page - no auth required (HS-04) */}
            <Route path="/p/:publicToken" element={<PublicCVPage />} />

            <Route element={<ProtectedRoute />}>
                <Route element={<MainLayout />}>
                    <Route path="/" element={<RootRedirect />} />
                    <Route path="/profile" element={<RootRedirect />} />

                    {/* =========================
                        CANDIDATE
                    ========================= */}
                    <Route
                        element={
                            <RoleGuard allowedRoles={["CANDIDATE"]} />
                        }
                    >
                        <Route
                            path="/candidate/profile"
                            element={<CandidateProfilePage />}
                        />

                        <Route
                            path="/candidate/jobs"
                            element={<CandidateJobsPage />}
                        />

                        <Route
                            path="/candidate/jobs/:id"
                            element={<CandidateJobDetailPage />}
                        />

                        <Route
                            path="/candidate/jobs/saved"
                            element={<CandidateSavedJobsPage />}
                        />

                        <Route
                            path="/candidate/interview-practice"
                            element={<Navigate to="/candidate/career/interviews" replace />}
                        />

                        <Route
                            path="/candidate/interview-practice/:id"
                            element={<InterviewPracticePage />}
                        />

                        <Route
                            path="/candidate/career/interviews/:id"
                            element={<InterviewPracticePage />}
                        />

                        <Route
                            path="/candidate/interview-practices"
                            element={<Navigate to="/candidate/career/interviews" replace />}
                        />

                        <Route
                            path="/candidate/career"
                            element={<CareerLayout />}
                        >
                            <Route index element={<OverviewPage />} />
                            <Route path="recommendations" element={<RecommendationsPage />} />
                            <Route path="skills" element={<SkillsPage />} />
                            <Route path="interviews" element={<InterviewPracticeListPage />} />
                        </Route>

                        <Route
                            path="/candidate/recruitments"
                            element={<CandidateRecruitmentsPage />}
                        />

                        <Route
                            path="/candidate/applications"
                            element={<CandidateRecruitmentsPage />}
                        />

                        <Route
                            path="/candidate/interviews"
                            element={<CandidateInterviewsPage />}
                        />

                        <Route
                            path="/candidate/offers"
                            element={<CandidateOffersPage />}
                        />

                        <Route
                            path="/candidate/learning"
                            element={<CandidateLearningPage />}
                        />

                        <Route
                            path="/candidate/cv"
                            element={<CandidateCVPage />}
                        />

                        <Route
                            path="/candidate/evaluations"
                            element={<EvaluationsPage />}
                        />

                        <Route
                            path="/profile/evaluations"
                            element={<EvaluationsPage />}
                        />

                        <Route
                            path="/profile/skills"
                            element={<SkillProfilePage />}
                        />

                        <Route
                            path="/profile/internships"
                            element={<InternshipsPage />}
                        />

                        <Route
                            path="/profile/self-assessment"
                            element={<SelfAssessmentPage />}
                        />

                        <Route
                            path="/profile/skill-comparison"
                            element={<SkillComparisonPage />}
                        />
                    </Route>

                    {/* =========================
                        HR
                    ========================= */}
                    <Route
                        element={<RoleGuard allowedRoles={["HR"]} />}
                    >
                        <Route
                            path="/hr/company"
                            element={<HrCompanyPage />}
                        />

                        <Route
                            path="/hr/jobs"
                            element={<HrJobsPage />}
                        />

                        <Route
                            path="/hr/jobs/new"
                            element={<HrJobFormPage />}
                        />

                        <Route
                            path="/hr/jobs/:id/edit"
                            element={<HrJobFormPage />}
                        />

                        <Route
                            path="/hr/jobs/:id"
                            element={<HrJobDetailPage />}
                        />

                        <Route
                            path="/hr/applications"
                            element={<HrApplicationsPage />}
                        />

                        <Route
                            path="/hr/candidates"
                            element={
                                <FeaturePlaceholderPage
                                    title="Tìm kiếm ứng viên công khai"
                                    description="Tính năng tìm kiếm và xem hồ sơ ứng viên theo điểm số kỹ năng đã xác thực."
                                    badge="Đang kết nối Matching Engine"
                                    moduleName="Phân hệ Nhà Tuyển Dụng"
                                    backPath="/hr/jobs"
                                    icon="sparkles"
                                />
                            }
                        />

                        <Route
                            path="/hr/interviews"
                            element={<HrInterviewSchedulePage />}
                        />

                        <Route
                            path="/hr/applications/:id"
                            element={<RecruitmentDetailPage />}
                        />

                        <Route
                            path="/hr/recruitments/:id"
                            element={<RecruitmentDetailPage />}
                        />

                        <Route
                            path="/hr/ioc"
                            element={
                                <FeaturePlaceholderPage
                                    title="Đăng ký nhu cầu thực tập IOC"
                                    description="Tính năng đăng ký số lượng và chỉ tiêu tuyển thực tập sinh theo các đợt IOC từ Rikkei Education."
                                    badge="Chờ kết nối LMS-PM"
                                    moduleName="Phân hệ Thực tập IOC"
                                    backPath="/hr/company"
                                    icon="layers"
                                />
                            }
                        />

                        {/* TT-02: BỔ SUNG ROUTE THỰC TẬP SINH CỦA HR */}
                        <Route
                            path="/hr/interns"
                            element={<HrInternsPage />}
                        />
                    </Route>

                    {/* =========================
                        MENTOR
                    ========================= */}
                    <Route
                        element={
                            <RoleGuard allowedRoles={["MENTOR"]} />
                        }
                    >
                        <Route
                            path="/mentor/students"
                            element={<MentorStudentsPage />}
                        />

                        <Route
                            path="/mentor/evaluations"
                            element={<MentorTaskListPage />}
                        />

                        <Route
                            path="/mentor/evaluations/:id"
                            element={<MentorEvaluationFormPage />}
                        />
                    </Route>

                    {/* =========================
                        STAFF & ADMIN
                    ========================= */}
                    <Route
                        element={
                            <RoleGuard
                                allowedRoles={["STAFF", "ADMIN"]}
                            />
                        }
                    >
                        <Route
                            path="/staff/jobs"
                            element={<StaffJobsPage />}
                        />

                        <Route
                            path="/staff/jobs/:id"
                            element={<StaffJobDetailPage />}
                        />

                        <Route
                            path="/staff/companies"
                            element={<StaffCompaniesPage />}
                        />

                        <Route
                            path="/staff/companies/:id"
                            element={<StaffCompanyDetailPage />}
                        />

                        {/* ĐÃ XÓA DÒNG ROUTE BỊ TRÙNG /staff/companies Ở ĐÂY */}

                        <Route
                            path="/staff/recruitments"
                            element={<StaffRecruitmentsPage />}
                        />

                        <Route
                            path="/staff/recruitments/:id"
                            element={<RecruitmentDetailPage />}
                        />

                        <Route
                            path="/staff/ioc"
                            element={
                                <FeaturePlaceholderPage
                                    title="Quản lý đợt thực tập IOC"
                                    description="Phân hệ quản trị đợt thực tập và tiến độ các nhóm dự án IOC đồng bộ từ LMS-PM."
                                    badge="Chờ kết nối LMS-PM"
                                    moduleName="Cán bộ Vận hành Tuyển sinh"
                                    backPath="/staff/jobs"
                                    icon="layers"
                                />
                            }
                        />

                        <Route
                            path="/staff/unlock-contacts"
                            element={
                                <FeaturePlaceholderPage
                                    title="Mở khóa thông tin liên hệ PII"
                                    description="Quy trình cán bộ phê duyệt mở khóa Email và Số điện thoại của ứng viên cho doanh nghiệp đối tác."
                                    badge="Bảo vệ dữ liệu cá nhân"
                                    moduleName="Cán bộ Tuyển sinh & Hướng nghiệp"
                                    backPath="/staff/candidates"
                                    icon="key"
                                />
                            }
                        />

                        <Route
                            path="/staff/candidates"
                            element={<StaffCandidatesPage />}
                        />

                        <Route
                            path="/staff/candidates/:userId"
                            element={<StaffCandidateDetailPage />}
                        />

                        {/* TT-02: BỔ SUNG ROUTE GHÉP THỰC TẬP CHO STAFF */}
                        <Route
                            path="/staff/placements"
                            element={<StaffPlacementPage />}
                        />

                        {/* UT-03: Contact Access Control */}
                        <Route
                            path="/staff/contact-requests"
                            element={<StaffContactRequestsPage />}
                        />

                        <Route
                            path="/staff/contact-access-logs"
                            element={<StaffContactAccessLogsPage />}
                        />
                    </Route>

                    {/* =========================
                        ADMIN
                    ========================= */}
                    <Route
                        element={
                            <RoleGuard allowedRoles={["ADMIN"]} />
                        }
                    >
                        {/* DG-01: Quản lý kỹ năng */}
                        <Route
                            path="/admin/skills"
                            element={<AdminSkillsPage />}
                        />

                        {/* DG-03: Quản lý huy hiệu Candidate */}
                        <Route
                            path="/admin/badges"
                            element={<AdminBadgesPage />}
                        />

                        {/* Các chức năng quản trị khác */}
                        <Route
                            path="/admin/overrides"
                            element={
                                <FeaturePlaceholderPage
                                    title="Hàng chờ báo sai bằng chứng"
                                    description="Tiếp nhận và xử lý các khiếu nại của ứng viên về điểm số và bằng chứng năng lực."
                                    badge="Chờ kết nối evidence-service"
                                    moduleName="Quản trị Hệ thống"
                                    backPath="/admin/skills"
                                    icon="shield"
                                />
                            }
                        />

                        <Route
                            path="/admin/audit-logs"
                            element={
                                <FeaturePlaceholderPage
                                    title="Nhật ký thao tác hệ thống (Audit Logs)"
                                    description="Truy vấn nhật ký kiểm toán bất biến append-only ghi nhận các thao tác nhạy cảm."
                                    badge="Chờ kết nối audit-service"
                                    moduleName="Quản trị Hệ thống"
                                    backPath="/admin/skills"
                                    icon="shield"
                                />
                            }
                        />
                    </Route>
                </Route>
            </Route>

            {/* 404 */}
            <Route
                path="*"
                element={<NotFoundPage />}
            />
        </Routes>
        </Suspense>
    );
}