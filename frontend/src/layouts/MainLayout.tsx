import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { AppHeader } from "../components/layout/AppHeader";
import { Footer } from "../components/layout/Footer";
import { ForceChangePasswordModal } from "../components/auth/ForceChangePasswordModal";
import { RouteLoadingFallback } from "../components/feedback/RouteLoadingFallback";
import { useAuthStore } from "../store/useAuthStore";

export function MainLayout() {
  const { user } = useAuthStore();

  return (
    <div className="h-screen flex flex-col bg-background text-foreground antialiased selection:bg-primary-light selection:text-primary overflow-hidden">
      {/* Modal bắt buộc đổi mật khẩu lần đầu nếu tài khoản đang mang mật khẩu tạm */}
      <ForceChangePasswordModal isOpen={Boolean(user?.mustChangePassword)} />

      {/* 1. Header / Top Navigation (Thanh điều hướng trên cùng chuẩn hóa kiểu TopCV cho mọi vai trò: Candidate, HR, Staff, Admin, Mentor) */}
      <AppHeader />

      {/* 2. Main Workspace (Full-width, thoáng đãng không còn Sidebar bên trái gây chật chội) */}
      <div className="flex-1 flex overflow-hidden min-h-0">
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
