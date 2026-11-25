import { useState, useRef, useEffect, useCallback } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Briefcase,
  GraduationCap,
  Sparkles,
  CalendarCheck,
  Award,
  Gift,
  Heart,
  Bell,
  LogOut,
  ChevronDown,
  User as UserIcon,
  KeyRound,
  Menu,
  X,
  Search,
  CheckCircle2,
  FileText,
  Star,
  ClipboardCheck,
} from "lucide-react";
import { useAuthStore } from "../../store/useAuthStore";
import { candidateJobsApi } from "../../api/endpoints/candidateJobs.api";
import { jobsApi } from "../../api/endpoints/jobs.api";
import { ChangePasswordModal } from "../auth/ChangePasswordModal";
import { cn } from "../../utils/cn";

interface NavGroupItem {
  title: string;
  description: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
}

interface NavGroup {
  id: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  items: NavGroupItem[];
}

const CANDIDATE_NAV_GROUPS: NavGroup[] = [
  {
    id: "jobs",
    label: "Việc làm",
    icon: Briefcase,
    items: [
      {
        title: "Tìm việc làm",
        description: "Khám phá hàng trăm cơ hội việc làm IT mới nhất",
        path: "/candidate/jobs",
        icon: Search,
      },
      {
        title: "Việc làm phù hợp (AI Match)",
        description: "Gợi ý việc làm sát nhất với năng lực của bạn",
        path: "/candidate/career",
        icon: Sparkles,
        badge: "AI",
      },
      {
        title: "Việc làm đã ứng tuyển",
        description: "Theo dõi tiến độ hồ sơ và kết quả tuyển dụng",
        path: "/candidate/recruitments",
        icon: FileText,
      },
      {
        title: "Việc làm đã lưu",
        description: "Danh sách công việc bạn đã thả tim theo dõi",
        path: "/candidate/jobs/saved",
        icon: Heart,
      },
    ],
  },
  {
    id: "learning",
    label: "Năng lực & Học tập",
    icon: GraduationCap,
    items: [
      {
        title: "Đánh giá năng lực",
        description: "Xem phiếu đánh giá kết quả từ Mentor & Giảng viên",
        path: "/profile/evaluations",
        icon: Star,
      },
      {
        title: "Tự đánh giá năng lực",
        description: "Đo lường kỹ năng và đối chiếu với tiêu chuẩn nghề",
        path: "/profile/self-assessment",
        icon: ClipboardCheck,
      },
      {
        title: "Cổng học bổ sung LMS",
        description: "Khóa học ngắn hạn bổ sung khoảng thiếu hụt kỹ năng",
        path: "/candidate/learning",
        icon: GraduationCap,
        badge: "LMS",
      },
    ],
  },
  {
    id: "interview",
    label: "Phỏng vấn",
    icon: CalendarCheck,
    items: [
      {
        title: "Lịch phỏng vấn doanh nghiệp",
        description: "Quản lý ca phỏng vấn và xác nhận thời gian",
        path: "/candidate/interviews",
        icon: CalendarCheck,
      },
    ],
  },
  {
    id: "profile_offers",
    label: "Hồ sơ & Đãi ngộ",
    icon: Award,
    items: [
      {
        title: "Hồ sơ kỹ năng LMS",
        description: "Hồ sơ năng lực cá nhân đã đồng bộ từ học viện LMS",
        path: "/candidate/profile",
        icon: UserIcon,
      },
      {
        title: "Thư mời nhận việc (Offers)",
        description: "Xem xét đãi ngộ và phản hồi thư mời làm việc",
        path: "/candidate/offers",
        icon: Gift,
      },
    ],
  },
];

export function CandidateHeader() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { user, isAuthenticated, logout } = useAuthStore();
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [openMobileAccordion, setOpenMobileAccordion] = useState<string | null>("jobs");
  const userMenuRef = useRef<HTMLDivElement>(null);

  // Đếm số lượng việc làm đã lưu để hiển thị badge trên icon Trái tim
  const { data: savedJobsRes } = useQuery({
    queryKey: ["savedJobs"],
    queryFn: () =>
      candidateJobsApi.getSavedJobs().catch(() => ({ success: true, statusCode: 200, message: "", data: [] })),
    staleTime: 5 * 60 * 1000,
    retry: false,
    enabled: isAuthenticated,
  });

  const savedCount = Array.isArray(savedJobsRes?.data) ? savedJobsRes.data.length : 0;

  // Prefetch dữ liệu khi người dùng hover vào menu để trang tải tức thì
  const prefetchSavedJobs = useCallback(() => {
    queryClient.prefetchQuery({
      queryKey: ["savedJobs"],
      queryFn: () =>
        candidateJobsApi.getSavedJobs().catch(() => ({ success: true, statusCode: 200, message: "", data: [] })),
      staleTime: 5 * 60 * 1000,
    });
  }, [queryClient]);

  const prefetchJobs = useCallback(() => {
    queryClient.prefetchQuery({
      queryKey: ["candidateJobs"],
      queryFn: () => jobsApi.getJobs().catch(() => null),
      staleTime: 5 * 60 * 1000,
    });
  }, [queryClient]);

  // Đóng dropdown tài khoản khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    setIsUserMenuOpen(false);
    navigate("/login", { replace: true, state: null });
  };

  const isGroupActive = (group: NavGroup) => {
    return group.items.some((item) => location.pathname === item.path || location.pathname.startsWith(`${item.path}/`));
  };

  return (
    <header className="sticky top-0 z-40 h-16 shrink-0 bg-surface/95 backdrop-blur-md border-b border-border shadow-xs">
      <div className="max-w-7xl mx-auto h-full px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        {/* 1. Left: Brand Logo & Mobile Trigger */}
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => setIsMobileMenuOpen(true)}
            aria-label="Mở menu"
            className="p-2 -ml-2 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg lg:hidden transition-colors cursor-pointer"
          >
            <Menu className="h-5 w-5" />
          </button>

          <Link to="/candidate/jobs" className="flex items-center gap-2.5 group">
            <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white font-extrabold text-base shadow-xs group-hover:bg-primary-hover transition-colors">
              RJ
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-base text-foreground tracking-tight leading-tight">
                Rikkei <span className="text-primary">Jobs</span>
              </span>
              <span className="text-[10px] text-muted font-medium tracking-wider uppercase">
                Cổng Việc Làm & Năng Lực IT
              </span>
            </div>
          </Link>
        </div>

        {/* 2. Center: Desktop Top Navigation với cửa sổ trượt khi hover */}
        <nav className="hidden lg:flex items-center gap-1 xl:gap-2">
          {CANDIDATE_NAV_GROUPS.map((group) => {
            const active = isGroupActive(group);
            const IconComponent = group.icon;

            return (
              <div key={group.id} className="relative group/menu py-4">
                {/* Menu Trigger Button */}
                <button
                  type="button"
                  className={cn(
                    "flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer",
                    active
                      ? "text-primary bg-primary-light/60 font-bold"
                      : "text-slate-700 hover:text-primary hover:bg-slate-100/80"
                  )}
                >
                  <IconComponent className={cn("h-4 w-4", active ? "text-primary" : "text-slate-500 group-hover/menu:text-primary")} />
                  <span>{group.label}</span>
                  <ChevronDown className="h-3.5 w-3.5 text-slate-400 group-hover/menu:text-primary group-hover/menu:rotate-180 transition-transform duration-200" />
                </button>

                {/* Cửa sổ trượt thả xuống khi hover (Sliding Hover Dropdown) */}
                <div className="absolute left-0 top-[calc(100%-8px)] w-80 rounded-2xl bg-surface border border-border shadow-xl p-2 z-50 transition-all duration-200 opacity-0 translate-y-2 invisible pointer-events-none group-hover/menu:opacity-100 group-hover/menu:translate-y-0 group-hover/menu:visible group-hover/menu:pointer-events-auto">
                  <div className="space-y-1">
                    {group.items.map((item) => {
                      const isItemActive = location.pathname === item.path;
                      const ItemIcon = item.icon;

                      return (
                        <Link
                          key={item.path}
                          to={item.path}
                          onMouseEnter={() => {
                            if (item.path === "/candidate/jobs/saved") prefetchSavedJobs();
                            else if (item.path === "/candidate/jobs") prefetchJobs();
                          }}
                          className={cn(
                            "flex items-start gap-3 p-2.5 rounded-xl transition-all group/item",
                            isItemActive
                              ? "bg-primary-light text-primary"
                              : "hover:bg-slate-50 text-slate-800"
                          )}
                        >
                          <div
                            className={cn(
                              "p-2 rounded-lg shrink-0 mt-0.5 transition-colors",
                              isItemActive
                                ? "bg-primary text-white shadow-xs"
                                : "bg-slate-100 text-slate-600 group-hover/item:bg-primary-light group-hover/item:text-primary"
                            )}
                          >
                            <ItemIcon className="h-4 w-4" />
                          </div>

                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-1.5">
                              <span className={cn("text-xs font-semibold truncate", isItemActive ? "text-primary font-bold" : "group-hover/item:text-primary")}>
                                {item.title}
                              </span>
                              {item.badge && (
                                <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-amber-100 text-amber-800">
                                  {item.badge}
                                </span>
                              )}
                            </div>
                            <p className="text-[11px] text-muted line-clamp-1 mt-0.5 leading-relaxed">
                              {item.description}
                            </p>
                          </div>
                        </Link>
                      );
                    })}
                  </div>
                </div>
              </div>
            );
          })}
        </nav>

        {/* 3. Right: Quick Actions (Saved Jobs, Notifications, User Profile) */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Nút Việc làm đã lưu với Badge số lượng */}
          <Link
            to="/candidate/jobs/saved"
            aria-label="Việc làm đã lưu"
            onMouseEnter={prefetchSavedJobs}
            className={cn(
              "relative p-2 rounded-lg transition-colors cursor-pointer flex items-center justify-center",
              location.pathname === "/candidate/jobs/saved"
                ? "bg-rose-50 text-rose-600"
                : "text-muted hover:text-rose-600 hover:bg-rose-50/70"
            )}
            title="Việc làm đã lưu"
          >
            <Heart className={cn("h-5 w-5", location.pathname === "/candidate/jobs/saved" ? "fill-rose-600 text-rose-600" : "")} />
            {savedCount > 0 && (
              <span className="absolute -top-1 -right-1 px-1.5 py-0.5 text-[10px] font-bold rounded-full bg-rose-600 text-white min-w-[18px] text-center shadow-xs">
                {savedCount > 99 ? "99+" : savedCount}
              </span>
            )}
          </Link>

          {/* Chuông thông báo */}
          <button
            type="button"
            aria-label="Thông báo hệ thống"
            className="relative p-2 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
          >
            <Bell className="h-5 w-5" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-primary" />
          </button>

          {/* User Account Dropdown */}
          {isAuthenticated && user ? (
            <div className="relative" ref={userMenuRef}>
              <button
                type="button"
                onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                className="flex items-center gap-2 p-1 pr-2 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer border border-transparent hover:border-border"
              >
                {user.avatarUrl ? (
                  <img
                    src={user.avatarUrl}
                    alt={user.fullName}
                    className="w-8 h-8 rounded-full object-cover border border-border"
                  />
                ) : (
                  <div className="w-8 h-8 rounded-full bg-primary-light text-primary font-bold text-xs flex items-center justify-center border border-primary-border">
                    {user.fullName.slice(0, 2).toUpperCase()}
                  </div>
                )}

                <div className="hidden md:flex flex-col text-left">
                  <span className="text-xs font-semibold text-foreground truncate max-w-[120px]">
                    {user.fullName}
                  </span>
                  <span className="text-[10px] text-muted truncate max-w-[120px]">
                    Ứng viên
                  </span>
                </div>

                <ChevronDown className="h-3.5 w-3.5 text-muted hidden sm:block" />
              </button>

              {/* User Dropdown */}
              {isUserMenuOpen && (
                <div className="absolute right-0 mt-2 w-64 rounded-2xl bg-surface border border-border shadow-xl py-2 z-50 animate-in fade-in zoom-in-95 duration-150">
                  <div className="px-4 py-3 border-b border-border-subtle">
                    <p className="text-xs font-bold text-foreground truncate">{user.fullName}</p>
                    <p className="text-[11px] text-muted truncate mt-0.5">{user.email}</p>
                    <div className="mt-2 flex items-center gap-1.5">
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-primary-light text-primary flex items-center gap-1">
                        <CheckCircle2 className="h-3 w-3" />
                        Tài khoản LMS đã xác thực
                      </span>
                    </div>
                  </div>

                  <div className="py-1">
                    <Link
                      to="/candidate/profile"
                      onClick={() => setIsUserMenuOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2 text-xs text-foreground hover:bg-slate-50 transition-colors"
                    >
                      <UserIcon className="h-4 w-4 text-muted" />
                      <span>Hồ sơ năng lực LMS</span>
                    </Link>

                    <button
                      type="button"
                      onClick={() => {
                        setIsUserMenuOpen(false);
                        setIsChangePasswordOpen(true);
                      }}
                      className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-foreground hover:bg-slate-50 transition-colors cursor-pointer text-left"
                    >
                      <KeyRound className="h-4 w-4 text-muted" />
                      <span>Thay đổi mật khẩu</span>
                    </button>
                  </div>

                  <div className="border-t border-border-subtle pt-1">
                    <button
                      type="button"
                      onClick={handleLogout}
                      className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-danger hover:bg-rose-50 transition-colors cursor-pointer text-left"
                    >
                      <LogOut className="h-4 w-4" />
                      <span>Đăng xuất</span>
                    </button>
                  </div>
                </div>
              )}

              {/* Change Password Modal */}
              <ChangePasswordModal
                isOpen={isChangePasswordOpen}
                onClose={() => setIsChangePasswordOpen(false)}
              />
            </div>
          ) : (
            <Link
              to="/login"
              className="px-3.5 py-1.5 text-xs font-semibold text-white bg-primary hover:bg-primary-hover rounded-lg transition-colors shadow-xs"
            >
              Đăng nhập
            </Link>
          )}
        </div>
      </div>

      {/* 4. Mobile Drawer Overlay */}
      {isMobileMenuOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs transition-opacity"
            onClick={() => setIsMobileMenuOpen(false)}
          />

          {/* Drawer Content */}
          <div className="fixed inset-y-0 left-0 w-80 bg-surface shadow-2xl flex flex-col z-10 animate-in slide-in-from-left duration-200">
            {/* Header */}
            <div className="h-16 px-4 border-b border-border flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-primary text-white font-bold flex items-center justify-center text-sm">
                  RJ
                </div>
                <span className="font-bold text-sm text-foreground">Menu Ứng Viên</span>
              </div>
              <button
                type="button"
                onClick={() => setIsMobileMenuOpen(false)}
                className="p-1.5 text-muted hover:text-foreground rounded-lg"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Accordion Menu List */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3">
              {CANDIDATE_NAV_GROUPS.map((group) => {
                const isOpen = openMobileAccordion === group.id;
                const IconComponent = group.icon;

                return (
                  <div key={group.id} className="border border-border rounded-xl overflow-hidden">
                    <button
                      type="button"
                      onClick={() => setOpenMobileAccordion(isOpen ? null : group.id)}
                      className="w-full flex items-center justify-between p-3 bg-slate-50/70 text-xs font-bold text-foreground hover:bg-slate-100 transition-colors"
                    >
                      <div className="flex items-center gap-2">
                        <IconComponent className="h-4 w-4 text-primary" />
                        <span>{group.label}</span>
                      </div>
                      <ChevronDown className={cn("h-4 w-4 text-muted transition-transform", isOpen ? "rotate-180" : "")} />
                    </button>

                    {isOpen && (
                      <div className="p-2 space-y-1 bg-surface">
                        {group.items.map((item) => (
                          <Link
                            key={item.path}
                            to={item.path}
                            onClick={() => setIsMobileMenuOpen(false)}
                            className={cn(
                              "flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium transition-colors",
                              location.pathname === item.path
                                ? "bg-primary-light text-primary font-bold"
                                : "text-slate-700 hover:bg-slate-50"
                            )}
                          >
                            <item.icon className="h-3.5 w-3.5 shrink-0" />
                            <span className="truncate">{item.title}</span>
                          </Link>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </header>
  );
}
