import { Outlet } from "react-router-dom";
import { Navbar } from "../components/layout/Navbar";
import { Sidebar } from "../components/layout/Sidebar";
import { Footer } from "../components/layout/Footer";
import { ForceChangePasswordModal } from "../components/auth/ForceChangePasswordModal";
import { useAuthStore } from "../store/useAuthStore";

export function MainLayout() {
  const { user } = useAuthStore();

  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground antialiased selection:bg-primary-light selection:text-primary">
      {/* Modal bắt buộc đổi mật khẩu lần đầu nếu tài khoản đang mang mật khẩu tạm */}
      <ForceChangePasswordModal isOpen={Boolean(user?.mustChangePassword)} />

      {/* 1. Header / Top Navigation */}
      <Navbar />

      {/* 2. Main Workspace (Sidebar + Scrollable Content) */}
      <div className="flex-1 flex overflow-hidden">
        <Sidebar />

        <main className="flex-1 flex flex-col overflow-y-auto min-w-0">
          <div className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
            <Outlet />
          </div>

          <Footer />
        </main>
      </div>
    </div>
  );
}
