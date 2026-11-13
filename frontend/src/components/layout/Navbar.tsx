import { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Menu,
  Bell,
  LogOut,
  ChevronDown,
  User as UserIcon,
  Shield,
  Briefcase,
  GraduationCap,
  Users,
  KeyRound,
} from "lucide-react";
import { useAuthStore } from "../../store/useAuthStore";
import { useUiStore } from "../../store/useUiStore";
import { Badge } from "../common/Badge";
import { ChangePasswordModal } from "../auth/ChangePasswordModal";
import type { Role } from "../../api/endpoints/auth.api";

export function Navbar() {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuthStore();
  const { toggleSidebar, openMobileSidebar } = useUiStore();
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  // Close user dropdown on click outside
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
    navigate("/login");
  };

  const getRoleBadgeVariant = (role?: Role) => {
    switch (role) {
      case "CANDIDATE":
        return "self";
      case "HR":
        return "primary";
      case "MENTOR":
        return "verified";
      case "STAFF":
        return "warning";
      case "ADMIN":
        return "danger";
      default:
        return "default";
    }
  };

  const getRoleIcon = (role?: Role) => {
    switch (role) {
      case "CANDIDATE":
        return <GraduationCap className="h-3 w-3" />;
      case "HR":
        return <Briefcase className="h-3 w-3" />;
      case "MENTOR":
        return <Users className="h-3 w-3" />;
      case "STAFF":
      case "ADMIN":
        return <Shield className="h-3 w-3" />;
      default:
        return null;
    }
  };

  return (
    <header className="sticky top-0 z-30 h-16 bg-surface border-b border-border shadow-xs flex items-center justify-between px-4 sm:px-6">
      {/* Left: Hamburger & Brand */}
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={openMobileSidebar}
          aria-label="Mở menu di động"
          className="p-2 -ml-2 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg lg:hidden transition-colors cursor-pointer"
        >
          <Menu className="h-5 w-5" />
        </button>

        <button
          type="button"
          onClick={toggleSidebar}
          aria-label="Thu gọn/mở rộng thanh bên"
          className="p-2 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg hidden lg:flex transition-colors cursor-pointer"
        >
          <Menu className="h-5 w-5" />
        </button>

        <Link to="/" className="flex items-center gap-2.5 group">
          <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white font-bold text-base shadow-xs group-hover:bg-primary-hover transition-colors">
            RJ
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-base text-foreground tracking-tight leading-tight">
              Rikkei <span className="text-primary">Jobs</span>
            </span>
            <span className="text-[10px] text-muted font-medium tracking-wider uppercase">
              Xác thực năng lực
            </span>
          </div>
        </Link>
      </div>

      {/* Right Actions: Notifications & User profile */}
      <div className="flex items-center gap-3 sm:gap-4">
        {/* Notification Bell */}
        <button
          type="button"
          aria-label="Thông báo hệ thống"
          className="relative p-2 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg transition-colors cursor-pointer"
        >
          <Bell className="h-5 w-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-primary" />
        </button>

        {/* User Account / Login */}
        {isAuthenticated && user ? (
          <div className="relative" ref={userMenuRef}>
            <button
              type="button"
              onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
              className="flex items-center gap-2.5 p-1.5 pr-2.5 rounded-xl hover:bg-slate-50 transition-colors cursor-pointer border border-transparent hover:border-border"
            >
              {user.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.fullName}
                  className="w-8 h-8 rounded-full object-cover border border-border"
                />
              ) : (
                <div className="w-8 h-8 rounded-full bg-primary-light text-primary font-semibold text-xs flex items-center justify-center border border-primary-border">
                  {user.fullName.slice(0, 2).toUpperCase()}
                </div>
              )}

              <div className="hidden md:flex flex-col text-left">
                <span className="text-xs font-semibold text-foreground truncate max-w-[140px]">
                  {user.fullName}
                </span>
                <span className="text-[11px] text-muted truncate max-w-[140px]">
                  {user.email}
                </span>
              </div>

              <ChevronDown className="h-3.5 w-3.5 text-muted hidden sm:block" />
            </button>

            {/* Dropdown Menu */}
            {isUserMenuOpen && (
              <div className="absolute right-0 mt-2 w-64 rounded-xl bg-surface border border-border shadow-lg py-2 z-50 animate-in fade-in zoom-in-95 duration-150">
                <div className="px-4 py-3 border-b border-border-subtle">
                  <p className="text-xs font-semibold text-foreground truncate">{user.fullName}</p>
                  <p className="text-[11px] text-muted truncate mt-0.5">{user.email}</p>
                  <div className="mt-2 flex items-center gap-1.5">
                    <Badge variant={getRoleBadgeVariant(user.role)} size="sm">
                      <span className="flex items-center gap-1">
                        {getRoleIcon(user.role)}
                        {user.role}
                      </span>
                    </Badge>
                  </div>
                </div>

                <div className="py-1">
                  <Link
                    to="/profile"
                    onClick={() => setIsUserMenuOpen(false)}
                    className="flex items-center gap-2.5 px-4 py-2 text-xs text-foreground hover:bg-slate-50 transition-colors"
                  >
                    <UserIcon className="h-4 w-4 text-muted" />
                    <span>Hồ sơ cá nhân</span>
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
                    <span>Đổi mật khẩu</span>
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
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Link
              to="/login"
              className="px-3.5 py-1.5 text-xs font-medium text-primary hover:bg-primary-light rounded-lg transition-colors"
            >
              Đăng nhập
            </Link>
          </div>
        )}
      </div>

      <ChangePasswordModal
        isOpen={isChangePasswordOpen}
        onClose={() => setIsChangePasswordOpen(false)}
      />
    </header>
  );
}

