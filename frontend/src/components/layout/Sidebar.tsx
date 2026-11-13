import { NavLink } from "react-router-dom";
import {
  Award,
  Briefcase,
  FileText,
  GraduationCap,
  Building2,
  Users,
  CalendarCheck,
  Layers,
  ClipboardCheck,
  CheckSquare,
  KeyRound,
  Sliders,
  AlertOctagon,
  ShieldCheck,
  X,
  BookOpen,
  Sparkles,
} from "lucide-react";
import { useAuthStore } from "../../store/useAuthStore";
import { useUiStore } from "../../store/useUiStore";
import { cn } from "../../utils/cn";
import type { Role } from "../../api/endpoints/auth.api";

interface NavItem {
  title: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
}

const ROLE_NAV_ITEMS: Record<Role, { groupTitle: string; items: NavItem[] }> = {
  CANDIDATE: {
    groupTitle: "Ứng Viên & Học Viên",
    items: [
      { title: "Hồ sơ", path: "/candidate/profile", icon: Award },
      { title: "Việc làm", path: "/candidate/jobs", icon: Briefcase },
      { title: "Ứng tuyển", path: "/candidate/applications", icon: FileText },
      { title: "Lộ trình", path: "/candidate/roadmap", icon: GraduationCap },
      { title: "Học bổ sung LMS", path: "/candidate/learning", icon: BookOpen },
      { title: "Trợ lý AI", path: "/candidate/ai-assistant", icon: Sparkles },
    ],
  },
  HR: {
    groupTitle: "Nhà Tuyển Dụng",
    items: [
      { title: "Công ty", path: "/hr/company", icon: Building2 },
      { title: "JD", path: "/hr/jobs", icon: Briefcase },
      { title: "Tuyển dụng", path: "/hr/candidates", icon: Users },
      { title: "Phỏng vấn & Offer", path: "/hr/interviews", icon: CalendarCheck },
      { title: "Thực tập sinh", path: "/hr/ioc", icon: Layers },
    ],
  },
  MENTOR: {
    groupTitle: "Giảng Viên / Mentor",
    items: [
      { title: "Cần đánh giá", path: "/mentor/evaluations", icon: ClipboardCheck },
      { title: "Thực tập sinh IOC", path: "/mentor/students", icon: Users },
    ],
  },
  STAFF: {
    groupTitle: "Cán Bộ Vận Hành",
    items: [
      { title: "Duyệt hồ sơ", path: "/staff/profiles", icon: CheckSquare },
      { title: "Duyệt JD", path: "/staff/jobs", icon: FileText },
      { title: "Doanh nghiệp", path: "/staff/companies", icon: Building2 },
      { title: "Ghép thực tập", path: "/staff/ioc", icon: Layers },
      { title: "Báo cáo", path: "/staff/reports", icon: ClipboardCheck },
      { title: "Mở khóa liên hệ PII", path: "/staff/unlock-contacts", icon: KeyRound },
    ],
  },
  ADMIN: {
    groupTitle: "Quản Trị Hệ Thống",
    items: [
      { title: "Quản trị", path: "/admin/skills", icon: ShieldCheck },
      { title: "Danh mục & Trọng số", path: "/admin/skills", icon: Sliders },
      { title: "Quản lý Huy hiệu", path: "/admin/badges", icon: Award },
      { title: "Báo sai bằng chứng", path: "/admin/overrides", icon: AlertOctagon },
      { title: "Nhật ký Audit Log", path: "/admin/audit-logs", icon: ShieldCheck },
    ],
  },
};

export function Sidebar() {
  const { user } = useAuthStore();
  const { isSidebarOpen, isMobileSidebarOpen, closeMobileSidebar } = useUiStore();

  const role = user?.role || "CANDIDATE";
  const navConfig = ROLE_NAV_ITEMS[role] || ROLE_NAV_ITEMS.CANDIDATE;

  const renderNavLinks = () => (
    <div className="space-y-6">
      <div>
        <p
          className={cn(
            "text-[11px] font-semibold tracking-wider text-muted uppercase px-3 mb-2 transition-opacity",
            !isSidebarOpen && "lg:hidden"
          )}
        >
          {navConfig.groupTitle}
        </p>

        <nav className="space-y-1">
          {navConfig.items.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={closeMobileSidebar}
                title={item.title}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all duration-150",
                    isActive
                      ? "bg-primary-light text-primary font-semibold shadow-2xs border border-primary-border"
                      : "text-foreground hover:bg-slate-100 hover:text-foreground",
                    !isSidebarOpen && "lg:justify-center lg:px-2.5"
                  )
                }
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span className={cn("truncate", !isSidebarOpen && "lg:hidden")}>
                  {item.title}
                </span>
              </NavLink>
            );
          })}
        </nav>
      </div>
    </div>
  );

  return (
    <>
      {/* 1. Desktop Sidebar */}
      <aside
        className={cn(
          "hidden lg:flex flex-col border-r border-border bg-surface shrink-0 transition-all duration-200 select-none",
          isSidebarOpen ? "w-64" : "w-20"
        )}
      >
        <div className="p-4 overflow-y-auto flex-1">
          {renderNavLinks()}
        </div>

        {/* Footer info in sidebar */}
        <div className={cn("p-4 border-t border-border-subtle text-[11px] text-muted", !isSidebarOpen && "hidden")}>
          <p className="font-medium text-foreground">Rikkei Jobs Platform</p>
          <p className="text-[10px] mt-0.5">Phiên bản 1.0 (MVP)</p>
        </div>
      </aside>

      {/* 2. Mobile Drawer Sidebar */}
      {isMobileSidebarOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs transition-opacity"
            onClick={closeMobileSidebar}
          />

          {/* Drawer content */}
          <div className="relative w-72 max-w-[80vw] h-full bg-surface border-r border-border p-5 flex flex-col z-10 shadow-xl animate-in slide-in-from-left duration-200">
            <div className="flex items-center justify-between pb-4 border-b border-border-subtle mb-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center text-white font-bold text-sm">
                  RJ
                </div>
                <span className="font-bold text-sm text-foreground">
                  Rikkei <span className="text-primary">Jobs</span>
                </span>
              </div>
              <button
                type="button"
                onClick={closeMobileSidebar}
                aria-label="Đóng thanh điều hướng"
                className="p-1.5 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="overflow-y-auto flex-1">
              {renderNavLinks()}
            </div>
          </div>
        </div>
      )}
    </>
  );
}

