import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { Navbar } from "../components/layout/Navbar";
import { CandidateHeader } from "../components/layout/CandidateHeader";
import { Sidebar } from "../components/layout/Sidebar";
import { Footer } from "../components/layout/Footer";
import { ForceChangePasswordModal } from "../components/auth/ForceChangePasswordModal";
import { RouteLoadingFallback } from "../components/feedback/RouteLoadingFallback";
import { useAuthStore, normalizeRole } from "../store/useAuthStore";

export function MainLayout() {
  const { user } = useAuthStore();
  const isCandidate = normalizeRole(user?.role) === "CANDIDATE";

  return (
    <div className="h-screen flex flex-col bg-background text-foreground antialiased selection:bg-primary-light selection:text-primary overflow-hidden">
      {/* Modal bắt buộc đổi mật khẩu lần đầu nếu tài khoản đang mang mật khẩu tạm */}
      <ForceChangePasswordModal isOpen={Boolean(user?.mustChangePassword)} />

      {/* 1. Header / Top Navigation (Candidate dùng Top Navigation kiểu TopCV, các vai trò khác dùng Navbar chuẩn) */}
      {isCandidate ? <CandidateHeader /> : <Navbar />}

      {/* 2. Main Workspace (Ẩn Sidebar với Candidate để tối ưu không gian tìm việc) */}
      <div className="flex-1 flex overflow-hidden min-h-0">
        {!isCandidate && <Sidebar />}

        <main className="flex-1 flex flex-col overflow-y-auto min-h-0 min-w-0 bg-slate-50/50">
          <div className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
            <Suspense fallback={<RouteLoadingFallback />}>
              <Outlet />
            </Suspense>
          </div>

          <Footer />
        </main>
      </div>
    </div>
  );
}
