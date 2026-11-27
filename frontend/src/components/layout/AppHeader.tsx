import { useState, useRef, useEffect, useCallback, useMemo } from "react";
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
  Building2,
  CheckSquare,
  Layers,
  UserCheck,
  RefreshCw,
  Sliders,
  AlertOctagon,
  ShieldCheck,
  FileEdit,
  PlusCircle,
  Users,
} from "lucide-react";
import { useAuthStore, normalizeRole } from "../../store/useAuthStore";
import { candidateKeys } from "../../api/queryKeys";
import { candidateJobsApi } from "../../api/endpoints/candidateJobs.api";
import { jobsApi } from "../../api/endpoints/jobs.api";
import { ChangePasswordModal } from "../auth/ChangePasswordModal";
import { NotificationBell } from "../notification/NotificationBell";
import { cn } from "../../utils/cn";
import type { Role } from "../../api/endpoints/auth.api";

export interface NavGroupItem {
  title: string;
  description: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: string;
}

export interface NavGroup {
  id: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  items: NavGroupItem[];
}

interface RoleConfig {
  brandTitle: string;
  brandSubtitle: string;
  homePath: string;
  roleDisplayName: string;
  roleBadgeText: string;
  drawerTitle: string;
  profileAction: {
    label: string;
    path: string;
    icon: React.ComponentType<{ className?: string }>;
  };
  quickAction?: {
    label: string;
    path: string;
    icon: React.ComponentType<{ className?: string }>;
  };
  groups: NavGroup[];
}

const ROLE_NAV_CONFIGS: Record<Role, RoleConfig> = {
  CANDIDATE: {
    brandTitle: "Rikkei Jobs",
    brandSubtitle: "Cổng Việc Làm & Năng Lực IT",
    homePath: "/candidate/jobs",
    roleDisplayName: "Ứng viên",
    roleBadgeText: "Tài khoản LMS đã xác thực",
    drawerTitle: "Menu Ứng Viên",
    profileAction: {
      label: "Hồ sơ năng lực LMS",
      path: "/candidate/profile",
      icon: UserIcon,
    },
    groups: [
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
            title: "Luyện phỏng vấn AI",
            description: "Mô phỏng phỏng vấn thực tế với trợ lý ảo AI",
            path: "/candidate/interview-practice",
            icon: Sparkles,
            badge: "HOT",
          },
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
            title: "CV của tôi",
            description: "Tạo và quản lý CV, xuất PDF với mã QR công khai",
            path: "/candidate/cv",
            icon: FileText,
          },
          {
            title: "Thư mời nhận việc (Offers)",
            description: "Xem xét đãi ngộ và phản hồi thư mời làm việc",
            path: "/candidate/offers",
            icon: Gift,
          },
        ],
      },
    ],
  },

  HR: {
    brandTitle: "Rikkei Jobs",
    brandSubtitle: "Cổng Nhà Tuyển Dụng",
    homePath: "/hr/company",
    roleDisplayName: "Nhà Tuyển Dụng",
    roleBadgeText: "Doanh nghiệp đối tác Rikkei",
    drawerTitle: "Menu Nhà Tuyển Dụng",
    profileAction: {
      label: "Hồ sơ công ty",
      path: "/hr/company",
      icon: Building2,
    },
    quickAction: {
      label: "Đăng tin mới",
      path: "/hr/jobs/new",
      icon: PlusCircle,
    },
    groups: [
      {
        id: "company_recruitment",
        label: "Công ty & Tin tuyển dụng",
        icon: Building2,
        items: [
          {
            title: "Hồ sơ công ty",
            description: "Cập nhật thông tin cơ bản & thương hiệu doanh nghiệp",
            path: "/hr/company",
            icon: Building2,
          },
          {
            title: "Tin tuyển dụng",
            description: "Quản lý và theo dõi các tin đăng tuyển dụng hiện có",
            path: "/hr/jobs",
            icon: Briefcase,
          },
          {
            title: "Đăng tin mới",
            description: "Tạo tin tuyển dụng mới để tiếp cận nhân tài IT",
            path: "/hr/jobs/new",
            icon: FileEdit,
            badge: "Mới",
          },
        ],
      },
      {
        id: "candidates_applications",
        label: "Ứng viên & Tuyển chọn",
        icon: Users,
        items: [
          {
            title: "Đơn ứng tuyển",
            description: "Tiếp nhận và sàng lọc hồ sơ ứng viên nộp vào",
            path: "/hr/applications",
            icon: FileText,
          },
          {
            title: "Tìm kiếm ứng viên",
            description: "Tra cứu hồ sơ nhân tài IT và đánh giá năng lực",
            path: "/hr/candidates",
            icon: Search,
            badge: "AI Match",
          },
        ],
      },
      {
        id: "interviews_offers",
        label: "Phỏng vấn & Đãi ngộ",
        icon: CalendarCheck,
        items: [
          {
            title: "Lịch phỏng vấn & Offer",
            description: "Sắp xếp ca phỏng vấn và gửi thư mời làm việc",
            path: "/hr/interviews",
            icon: CalendarCheck,
          },
        ],
      },
      {
        id: "ioc_internships",
        label: "Thực tập IOC",
        icon: Layers,
        items: [
          {
            title: "Đợt thực tập IOC",
            description: "Đăng ký tiếp nhận sinh viên theo các đợt thực tập Rikkei",
            path: "/hr/ioc",
            icon: Layers,
          },
          {
            title: "Thực tập sinh của tôi",
            description: "Theo dõi tiến độ và đánh giá thực tập sinh tại công ty",
            path: "/hr/interns",
            icon: UserCheck,
          },
        ],
      },
    ],
  },

  STAFF: {
    brandTitle: "Rikkei Jobs",
    brandSubtitle: "Cổng Cán Bộ Vận Hành",
    homePath: "/staff/companies",
    roleDisplayName: "Cán Bộ Vận Hành",
    roleBadgeText: "Ban Tuyển Sinh & Đào Tạo",
    drawerTitle: "Menu Cán Bộ Vận Hành",
    profileAction: {
      label: "Quản lý doanh nghiệp",
      path: "/staff/companies",
      icon: Building2,
    },
    quickAction: {
      label: "Đồng bộ LMS",
      path: "/staff/sync",
      icon: RefreshCw,
    },
    groups: [
      {
        id: "operations_review",
        label: "Kiểm duyệt & Vận hành",
        icon: ClipboardCheck,
        items: [
          {
            title: "Duyệt hồ sơ ứng viên",
            description: "Kiểm duyệt hồ sơ năng lực và thông tin ứng viên",
            path: "/staff/candidates",
            icon: ClipboardCheck,
          },
          {
            title: "Duyệt tin tuyển dụng",
            description: "Phê duyệt các tin tuyển dụng từ doanh nghiệp",
            path: "/staff/jobs",
            icon: CheckSquare,
          },
          {
            title: "Quản lý đơn tuyển dụng",
            description: "Theo dõi toàn bộ đơn ứng tuyển trên hệ thống",
            path: "/staff/recruitments",
            icon: FileText,
          },
        ],
      },
      {
        id: "companies_security",
        label: "Doanh nghiệp & Bảo mật",
        icon: Building2,
        items: [
          {
            title: "Doanh nghiệp & HR",
            description: "Quản lý đối tác doanh nghiệp và tài khoản HR",
            path: "/staff/companies",
            icon: Building2,
          },
          {
            title: "Mở khóa liên hệ PII",
            description: "Phê duyệt quyền mở khóa thông tin liên hệ nhạy cảm",
            path: "/staff/unlock-contacts",
            icon: KeyRound,
            badge: "Bảo mật",
          },
        ],
      },
      {
        id: "placement_sync",
        label: "Thực tập & Dữ liệu",
        icon: Layers,
        items: [
          {
            title: "Ghép đợt IOC",
            description: "Điều phối và phân bổ chỉ tiêu thực tập doanh nghiệp",
            path: "/staff/ioc",
            icon: Layers,
          },
          {
            title: "Ghép thực tập",
            description: "Ghép nối sinh viên thực tập vào các doanh nghiệp",
            path: "/staff/placements",
            icon: UserCheck,
          },
          {
            title: "Đào tạo bổ sung",
            description: "Theo dõi tiến độ học bổ sung theo lớp, khoá",
            path: "/staff/upskilling",
            icon: GraduationCap,
          },
          {
            title: "Đồng bộ dữ liệu LMS/EXAM/PM",
            description: "Kéo dữ liệu học tập, thi cử và can thiệp sửa tay",
            path: "/staff/sync",
            icon: RefreshCw,
            badge: "HOT",
          },
        ],
      },
    ],
  },

  ADMIN: {
    brandTitle: "Rikkei Jobs",
    brandSubtitle: "Cổng Quản Trị Hệ Thống",
    homePath: "/admin/skills",
    roleDisplayName: "Quản Trị Viên",
    roleBadgeText: "Toàn quyền quản trị hệ thống",
    drawerTitle: "Menu Quản Trị Viên",
    profileAction: {
      label: "Cấu hình hệ thống",
      path: "/admin/skills",
      icon: Sliders,
    },
    quickAction: {
      label: "Nhật ký Audit",
      path: "/admin/audit-logs",
      icon: ShieldCheck,
    },
    groups: [
      {
        id: "admin_standards",
        label: "Hệ thống & Kỹ năng",
        icon: Sliders,
        items: [
          {
            title: "Danh mục & Trọng số",
            description: "Cấu hình chuẩn năng lực, kỹ năng và trọng số đánh giá",
            path: "/admin/skills",
            icon: Sliders,
          },
          {
            title: "Quản lý Huy hiệu",
            description: "Thiết lập danh hiệu và điều kiện cấp huy hiệu năng lực",
            path: "/admin/badges",
            icon: Award,
            badge: "DG-03",
          },
        ],
      },
      {
        id: "admin_audit",
        label: "Giám sát & Kiểm toán",
        icon: ShieldCheck,
        items: [
          {
            title: "Báo sai bằng chứng",
            description: "Xử lý khiếu nại và các bản ghi can thiệp điểm số",
            path: "/admin/overrides",
            icon: AlertOctagon,
            badge: "Khiếu nại",
          },
          {
            title: "Nhật ký Audit Log",
            description: "Truy vết toàn bộ thao tác hệ thống append-only",
            path: "/admin/audit-logs",
            icon: ShieldCheck,
          },
        ],
      },
    ],
  },

  MENTOR: {
    brandTitle: "Rikkei Jobs",
    brandSubtitle: "Cổng Giảng Viên & Mentor",
    homePath: "/mentor/students",
    roleDisplayName: "Giảng Viên / Mentor",
    roleBadgeText: "Chuyên gia đào tạo Rikkei",
    drawerTitle: "Menu Giảng Viên & Mentor",
    profileAction: {
      label: "Danh sách sinh viên",
      path: "/mentor/students",
      icon: Users,
    },
    quickAction: {
      label: "Tạo đánh giá",
      path: "/mentor/evaluations/create",
      icon: FileEdit,
    },
    groups: [
      {
        id: "mentor_training",
        label: "Đào tạo & Thực tập",
        icon: Users,
        items: [
          {
            title: "Thực tập sinh IOC",
            description: "Theo dõi tiến độ học tập và thực tập của sinh viên",
            path: "/mentor/students",
            icon: Users,
          },
        ],
      },
      {
        id: "mentor_evaluation",
        label: "Đánh giá năng lực",
        icon: ClipboardCheck,
        items: [
          {
            title: "Phiếu đánh giá",
            description: "Danh sách phiếu đánh giá năng lực học viên",
            path: "/mentor/evaluations",
            icon: ClipboardCheck,
          },
          {
            title: "Tạo đánh giá mới",
            description: "Lập phiếu đánh giá năng lực thực tế cho sinh viên",
            path: "/mentor/evaluations/create",
            icon: FileEdit,
            badge: "Mới",
          },
        ],
      },
    ],
  },
};

export function AppHeader() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { user, isAuthenticated, logout } = useAuthStore();
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [openMobileAccordion, setOpenMobileAccordion] = useState<string | null>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);

  // Xác định cấu hình theo vai trò người dùng (mặc định CANDIDATE)
  const currentRole = (normalizeRole(user?.role) as Role) || "CANDIDATE";
  const roleConfig = useMemo(() => {
    return ROLE_NAV_CONFIGS[currentRole] || ROLE_NAV_CONFIGS.CANDIDATE;
  }, [currentRole]);

  const isCandidate = currentRole === "CANDIDATE";

  // Mở accordion đầu tiên trên mobile khi mở drawer
  useEffect(() => {
    if (isMobileMenuOpen && roleConfig.groups.length > 0 && !openMobileAccordion) {
      setOpenMobileAccordion(roleConfig.groups[0].id);
    }
  }, [isMobileMenuOpen, roleConfig, openMobileAccordion]);

  // Đếm số lượng việc làm đã lưu (chỉ bật với ứng viên)
  const { data: savedJobsRes } = useQuery({
    queryKey: candidateKeys.jobs.saved(),
    queryFn: () =>
      candidateJobsApi.getSavedJobs().catch(() => ({ success: true, statusCode: 200, message: "", data: [] })),
    staleTime: 30 * 1000,
    retry: false,
    enabled: isAuthenticated && isCandidate,
  });

  const savedCount = useMemo(() => {
    if (Array.isArray(savedJobsRes?.data)) return savedJobsRes.data.length;
    if (Array.isArray(savedJobsRes)) return savedJobsRes.length;
    return 0;
  }, [savedJobsRes]);

  // Prefetch dữ liệu khi người dùng hover vào menu
  const prefetchSavedJobs = useCallback(() => {
    if (!isCandidate) return;
    queryClient.prefetchQuery({
      queryKey: candidateKeys.jobs.saved(),
      queryFn: () =>
        candidateJobsApi.getSavedJobs().catch(() => ({ success: true, statusCode: 200, message: "", data: [] })),
      staleTime: 30 * 1000,
    });
  }, [queryClient, isCandidate]);

  const prefetchJobs = useCallback(() => {
    if (!isCandidate) return;
    queryClient.prefetchQuery({
      queryKey: ["candidateJobs"],
      queryFn: () => jobsApi.getJobs().catch(() => null),
      staleTime: 5 * 60 * 1000,
    });
  }, [queryClient, isCandidate]);

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
    return group.items.some(
      (item) => location.pathname === item.path || (item.path !== "/" && location.pathname.startsWith(`${item.path}/`))
    );
  };

  const QuickActionIcon = roleConfig.quickAction?.icon;

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

          <Link to={roleConfig.homePath} className="flex items-center gap-2.5 group">
            <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white font-extrabold text-base shadow-xs group-hover:bg-primary-hover transition-colors">
              RJ
            </div>
            <div className="flex flex-col">
              <span className="font-extrabold text-base text-foreground tracking-tight leading-tight">
                Rikkei <span className="text-primary">Jobs</span>
              </span>
              <span className="text-[10px] text-muted font-medium tracking-wider uppercase truncate max-w-[170px] sm:max-w-none">
                {roleConfig.brandSubtitle}
              </span>
            </div>
          </Link>
        </div>

        {/* 2. Center: Desktop Top Navigation với cửa sổ trượt thả xuống khi hover */}
        <nav className="hidden lg:flex items-center gap-1 xl:gap-2">
          {roleConfig.groups.map((group) => {
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
                  <IconComponent
                    className={cn(
                      "h-4 w-4",
                      active ? "text-primary" : "text-slate-500 group-hover/menu:text-primary"
                    )}
                  />
                  <span>{group.label}</span>
                  <ChevronDown className="h-3.5 w-3.5 text-slate-400 group-hover/menu:text-primary group-hover/menu:rotate-180 transition-transform duration-200" />
                </button>

                {/* Cửa sổ trượt thả xuống khi hover (Sliding Hover Dropdown) */}
                <div className="absolute left-0 top-[calc(100%-8px)] w-80 rounded-2xl bg-surface border border-border shadow-xl p-2 z-50 transition-all duration-200 opacity-0 translate-y-2 invisible pointer-events-none group-hover/menu:opacity-100 group-hover/menu:translate-y-0 group-hover/menu:visible group-hover/menu:pointer-events-auto">
                  <div className="space-y-1">
                    {group.items.map((item) => {
                      const isItemActive =
                        location.pathname === item.path ||
                        (item.path !== "/" && location.pathname.startsWith(`${item.path}/`));
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
                              <span
                                className={cn(
                                  "text-xs font-semibold truncate",
                                  isItemActive ? "text-primary font-bold" : "group-hover/item:text-primary"
                                )}
                              >
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

        {/* 3. Right: Quick Actions, Notifications & User Profile */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Quick CTA Button theo vai trò (HR -> Đăng tin, Staff -> Đồng bộ, Mentor -> Tạo đánh giá, Admin -> Audit) */}
          {roleConfig.quickAction && QuickActionIcon && (
            <Link
              to={roleConfig.quickAction.path}
              className="hidden md:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-primary bg-primary-light/80 hover:bg-primary hover:text-white border border-primary-border transition-colors shadow-2xs"
            >
              <QuickActionIcon className="h-3.5 w-3.5" />
              <span>{roleConfig.quickAction.label}</span>
            </Link>
          )}

          {/* Nút Việc làm đã lưu (Dành cho CANDIDATE) */}
          {isCandidate && (
            <Link
              to="/candidate/jobs/saved"
              aria-label="Việc làm đã lưu"
              onMouseEnter={prefetchSavedJobs}
              className={cn(
                "relative p-2 rounded-lg transition-colors cursor-pointer flex items-center justify-center",
                location.pathname === "/candidate/jobs/saved"
                  ? "bg-primary-light text-primary"
                  : savedCount > 0
                    ? "text-primary hover:bg-primary-light/70"
                    : "text-muted hover:text-foreground hover:bg-slate-100"
              )}
              title={savedCount > 0 ? `Việc làm đã lưu (${savedCount})` : "Việc làm đã lưu"}
            >
              <Heart
                className={cn(
                  "h-5 w-5 transition-colors",
                  savedCount > 0
                    ? "fill-primary text-primary"
                    : ""
                )}
              />
            </Link>
          )}

          {/* Chuông thông báo */}
          <NotificationBell />

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
                    {user.fullName ? user.fullName.slice(0, 2).toUpperCase() : "U"}
                  </div>
                )}

                <div className="hidden md:flex flex-col text-left">
                  <span className="text-xs font-semibold text-foreground truncate max-w-[120px]">
                    {user.fullName}
                  </span>
                  <span className="text-[10px] text-muted truncate max-w-[120px]">
                    {roleConfig.roleDisplayName}
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
                        {roleConfig.roleBadgeText}
                      </span>
                    </div>
                  </div>

                  <div className="py-1">
                    <Link
                      to={roleConfig.profileAction.path}
                      onClick={() => setIsUserMenuOpen(false)}
                      className="flex items-center gap-2.5 px-4 py-2 text-xs text-foreground hover:bg-slate-50 transition-colors"
                    >
                      <roleConfig.profileAction.icon className="h-4 w-4 text-muted" />
                      <span>{roleConfig.profileAction.label}</span>
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
                <div className="flex flex-col">
                  <span className="font-bold text-sm text-foreground">{roleConfig.drawerTitle}</span>
                  <span className="text-[10px] text-muted">{roleConfig.brandSubtitle}</span>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsMobileMenuOpen(false)}
                className="p-1.5 text-muted hover:text-foreground rounded-lg"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Quick Action Button for Mobile */}
            {roleConfig.quickAction && (
              <div className="px-4 pt-3">
                <Link
                  to={roleConfig.quickAction.path}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl text-xs font-bold text-white bg-primary hover:bg-primary-hover shadow-xs transition-colors"
                >
                  <roleConfig.quickAction.icon className="h-4 w-4" />
                  <span>{roleConfig.quickAction.label}</span>
                </Link>
              </div>
            )}

            {/* Accordion Menu List */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3">
              {roleConfig.groups.map((group) => {
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
                      <ChevronDown
                        className={cn("h-4 w-4 text-muted transition-transform", isOpen ? "rotate-180" : "")}
                      />
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
                            {item.badge && (
                              <span className="ml-auto text-[9px] font-bold px-1.5 py-0.2 rounded bg-amber-100 text-amber-800">
                                {item.badge}
                              </span>
                            )}
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
