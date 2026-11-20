import { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Lock, Mail, Eye, EyeOff, LogIn, KeyRound, Ticket, CheckCircle2, ExternalLink, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { useAuthStore } from "../../store/useAuthStore";
import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { ForgotPasswordModal } from "../../components/auth/ForgotPasswordModal";

const loginSchema = z.object({
  email: z.string().min(1, "Vui lòng nhập địa chỉ email").email("Email không đúng định dạng"),
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

// Bộ nhớ đệm lưu các vé SSO đã gửi đi xác thực để ngăn chặn việc gửi trùng lặp (kể cả khi React StrictMode remount)
const processedTickets = new Set<string>();

const isPathAllowedForRole = (pathname: string, role: string): boolean => {
  if (!pathname || pathname === "/login" || pathname === "/unauthorized" || pathname === "/") return false;

  const normalized = role ? role.replace(/^ROLE_/, "").toUpperCase() : "CANDIDATE";
  switch (normalized) {
    case "HR":
      return pathname.startsWith("/hr");
    case "MENTOR":
      return pathname.startsWith("/mentor");
    case "STAFF":
      return pathname.startsWith("/staff");
    case "ADMIN":
      return pathname.startsWith("/admin") || pathname.startsWith("/staff");
    case "CANDIDATE":
      return pathname.startsWith("/candidate") || pathname.startsWith("/profile");
    default:
      return false;
  }
};

const getRoleHomePath = (role: string): string => {
  const normalized = role ? role.replace(/^ROLE_/, "").toUpperCase() : "CANDIDATE";
  switch (normalized) {
    case "HR":
      return "/hr/company";
    case "MENTOR":
      return "/mentor/students";
    case "STAFF":
      return "/staff/jobs";
    case "ADMIN":
      return "/admin/skills";
    case "CANDIDATE":
    default:
      return "/candidate/profile";
  }
};

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, ssoLogin } = useAuthStore();
  const [loginMode, setLoginMode] = useState<"sso" | "password">("sso");
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isForgotPasswordOpen, setIsForgotPasswordOpen] = useState(false);
  const [ssoTicket, setSsoTicket] = useState("");
  const [isSsoSubmitting, setIsSsoSubmitting] = useState(false);
  const [isFetchingMockTicket, setIsFetchingMockTicket] = useState(false);
  const processingRef = useRef(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const emailValue = watch("email");

  const handleRedirectAfterLogin = (role: string) => {
    // Đợi microtask để auth store cập nhật xong (token, userId, user role)
    setTimeout(() => {
      const fromPath = (location.state as { from?: { pathname: string } })?.from?.pathname;
      // Chỉ cho phép chuyển tiếp về fromPath nếu pathname đó thực sự thuộc thẩm quyền của vai trò mới
      if (fromPath && isPathAllowedForRole(fromPath, role)) {
        navigate(fromPath, { replace: true });
      } else {
        // Tự động chuyển thẳng vào phân hệ chính thức của Role đó
        navigate(getRoleHomePath(role), { replace: true });
      }
    }, 0);
  };

  // Tự động nhận diện vé SSO và tham số auto=true khi chuyển hướng từ Cổng LMS (:9000)
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const ticketParam = searchParams.get("ticket")?.trim();
    const autoParam = searchParams.get("auto");

    if (ticketParam) {
      setLoginMode("sso");
      setSsoTicket(ticketParam);

      if (autoParam === "true") {
        // Ngăn chặn gọi trùng lặp (kể cả khi React StrictMode remount trong môi trường dev)
        if (processedTickets.has(ticketParam) || processingRef.current) {
          return;
        }

        processedTickets.add(ticketParam);
        processingRef.current = true;
        setIsSsoSubmitting(true);

        // Xóa vé khỏi thanh địa chỉ URL ngay lập tức để tránh F5/Back gửi lại vé đã dùng
        searchParams.delete("ticket");
        searchParams.delete("auto");
        const cleanSearch = searchParams.toString();
        const cleanUrl = window.location.pathname + (cleanSearch ? `?${cleanSearch}` : "");
        window.history.replaceState({}, "", cleanUrl);

        toast.info("Đang tự động xác thực vé SSO từ Cổng Rikkei LMS...");

        ssoLogin(ticketParam)
          .then((data) => {
            toast.success(`Xác thực SSO thành công! Chào mừng ${data.user.fullName}`);
            handleRedirectAfterLogin(data.user.role);
          })
          .catch(() => {
            // Lỗi đã được Axios toast hiển thị, giữ nguyên vé để người dùng có thể bấm thử lại
          })
          .finally(() => {
            setIsSsoSubmitting(false);
            processingRef.current = false;
          });
      }
    }
  }, [location.search]);

  const onSubmit = async (values: LoginFormValues) => {
    setIsSubmitting(true);
    try {
      const data = await login(values.email, values.password);
      toast.success(`Đăng nhập thành công! Chào mừng ${data.user.fullName}`);
      handleRedirectAfterLogin(data.user.role);
    } catch {
      // Lỗi đã được Axios response interceptor hiển thị qua toast
    } finally {
      setIsSubmitting(false);
    }
  };

  const onSsoSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const ticketToUse = ssoTicket.trim();
    if (!ticketToUse) {
      toast.error("Vui lòng nhập mã vé SSO (Ticket)");
      return;
    }

    if (processedTickets.has(ticketToUse)) {
      toast.error("Mã vé này đã được sử dụng. Vui lòng lấy vé mới từ Cổng LMS hoặc bấm 'Lấy vé mẫu nhanh'");
      return;
    }

    processedTickets.add(ticketToUse);
    setIsSsoSubmitting(true);
    try {
      const data = await ssoLogin(ticketToUse);
      toast.success(`Đăng nhập SSO thành công! Chào mừng ${data.user.fullName}`);
      handleRedirectAfterLogin(data.user.role);
    } catch {
      // Lỗi đã được Axios response interceptor hiển thị qua toast
    } finally {
      setIsSsoSubmitting(false);
    }
  };

  const handleGetMockTicket = async () => {
    setIsFetchingMockTicket(true);
    try {
      const res = await fetch('/lms/sso/tickets', {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Service-Key": "fbc368ffa97a999a71577c552ecc2546815f245fd4ebee014732afcba1b52870",
        },
        body: JSON.stringify({ lmsUserId: "std-001" }),
      });
      const data = await res.json();
      if (data?.ticket) {
        setSsoTicket(data.ticket);
        toast.success(`Đã lấy mã vé SSO: ${data.ticket}`);
      } else {
        toast.error("Không lấy được mã vé từ Mock Server");
      }
    } catch {
      toast.error("Không thể kết nối đến Mock Server (:9000). Vui lòng đảm bảo Mock Server đang chạy");
    } finally {
      setIsFetchingMockTicket(false);
    }
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-6">
      {/* Header */}
      <div className="text-left space-y-1.5">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
          Đăng nhập tài khoản
        </h1>
        <p className="text-xs sm:text-sm text-muted leading-relaxed">
          Truy cập hệ thống việc làm và phân tích năng lực theo chuẩn Rikkei Education
        </p>
      </div>

      {/* Mode Tabs */}
      <div className="grid grid-cols-2 rounded-xl bg-slate-100 p-1 border border-border/80 gap-1">
        <button
          type="button"
          onClick={() => setLoginMode("sso")}
          className={`py-2 px-3 text-xs font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer whitespace-nowrap ${loginMode === "sso"
            ? "bg-white text-primary shadow-xs border border-border font-bold"
            : "text-muted hover:text-foreground"
            }`}
        >
          <Ticket className="h-3.5 w-3.5 shrink-0" />
          <span>Học viên (Vé SSO)</span>
        </button>

        <button
          type="button"
          onClick={() => setLoginMode("password")}
          className={`py-2 px-3 text-xs font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer whitespace-nowrap ${loginMode === "password"
            ? "bg-white text-foreground shadow-xs border border-border font-bold"
            : "text-muted hover:text-foreground"
            }`}
        >
          <KeyRound className="h-3.5 w-3.5 shrink-0" />
          <span>Doanh nghiệp & HR</span>
        </button>
      </div>

      {loginMode === "sso" ? (
        <form onSubmit={onSsoSubmit} className="space-y-4">
          {/* Banner tự động xác thực khi chuyển tiếp từ Cổng LMS */}
          {isSsoSubmitting && (
            <div className="p-4 bg-rose-50 border border-rose-200/90 rounded-xl text-center space-y-2 animate-pulse shadow-xs">
              <div className="flex items-center justify-center gap-2 text-rose-700 font-bold text-sm">
                <div className="h-4 w-4 border-2 border-rose-600 border-t-transparent rounded-full animate-spin" />
                <span>Đang tự động xác thực vé SSO từ Cổng Rikkei LMS...</span>
              </div>
              <p className="text-xs text-rose-600/80">
                Hệ thống đang đồng bộ dữ liệu học viên và tạo phiên đăng nhập.
              </p>
            </div>
          )}

          {/* Banner điều hướng sang Cổng LMS (:9000) */}
          <div className="p-3.5 bg-gradient-to-r from-slate-900 to-slate-800 text-white rounded-xl text-xs space-y-2.5 shadow-sm">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 font-bold text-rose-300">
                <Sparkles className="h-4 w-4 shrink-0 text-rose-400" />
                <span>Cổng Sinh Viên Rikkei LMS (:9000)</span>
              </div>
              <span className="text-[10px] bg-white/10 px-2 py-0.5 rounded text-slate-300 font-medium">Hệ thống ngoài</span>
            </div>
            <p className="text-slate-300 text-[11px] leading-relaxed">
              Mở Cổng LMS để xem bảng điểm học viên, chuyên cần, R-Point và trải nghiệm luồng đăng nhập SSO 1-chạm tự động.
            </p>
            <a
              href="http://localhost:9000/lms/portal"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white transition-all w-full justify-center shadow-xs cursor-pointer"
            >
              <span>Mở Cổng Sinh Viên LMS (:9000)</span>
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          </div>

          <div className="p-3 bg-blue-50/80 border border-blue-200/90 rounded-xl text-xs text-blue-900 leading-relaxed space-y-1">
            <div className="flex items-center gap-1.5 font-bold text-blue-950">
              <CheckCircle2 className="h-4 w-4 text-blue-600 shrink-0" />
              <span>Xác thực học viên qua vé SSO một lần</span>
            </div>
            <p className="text-blue-800 text-[11px] leading-normal">
              Học viên sử dụng mã vé một lần (SSO Ticket) từ hệ thống LMS để xác thực danh tính và đồng bộ hồ sơ năng lực.
            </p>
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label htmlFor="sso-ticket-input" className="block text-xs sm:text-sm font-medium text-foreground">
                Mã vé SSO (Ticket) <span className="text-danger ml-0.5">*</span>
              </label>
              <button
                type="button"
                onClick={handleGetMockTicket}
                disabled={isFetchingMockTicket}
                className="text-xs text-primary hover:text-primary-hover font-semibold cursor-pointer transition-colors flex items-center gap-1 hover:underline"
              >
                <Ticket className="h-3.5 w-3.5 shrink-0" />
                <span>{isFetchingMockTicket ? "Đang lấy..." : "Lấy vé mẫu nhanh"}</span>
              </button>
            </div>

            <div className="relative flex items-center">
              <div className="absolute left-3 text-muted pointer-events-none flex items-center">
                <Ticket className="h-4 w-4 text-muted" />
              </div>
              <input
                id="sso-ticket-input"
                type="text"
                placeholder="Nhập hoặc dán mã vé (vd: TICKET-STD-001-...)"
                required
                value={ssoTicket}
                onChange={(e) => setSsoTicket(e.target.value)}
                className="w-full rounded-lg border border-border bg-surface pl-10 pr-4 py-2.5 text-xs sm:text-sm text-foreground placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-colors shadow-2xs font-mono"
              />
            </div>
            <p className="text-[11px] text-muted leading-tight">
              Gợi ý: Bấm <strong>"Mở Cổng Sinh Viên LMS"</strong> ở trên hoặc <strong>"Lấy vé mẫu nhanh"</strong> để kiểm thử.
            </p>
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full font-semibold shadow-xs text-sm py-2.5"
              isLoading={isSsoSubmitting}
              leftIcon={<LogIn className="h-4 w-4" />}
            >
              Xác thực & Đăng nhập SSO
            </Button>
          </div>
        </form>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-600 leading-relaxed">
            Dành cho Nhà tuyển dụng (HR), Giảng viên (Mentor), Cán bộ tuyển sinh (Staff) và Quản trị viên.
          </div>

          <Input
            label="Địa chỉ Email"
            type="email"
            placeholder="vd: hr1@rikkeijobs.test"
            required
            leftIcon={<Mail className="h-4 w-4" />}
            error={errors.email?.message}
            {...register("email")}
          />

          <Input
            label="Mật khẩu"
            type={showPassword ? "text" : "password"}
            placeholder="Nhập mật khẩu của bạn"
            required
            leftIcon={<Lock className="h-4 w-4" />}
            rightIcon={
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                className="text-muted hover:text-foreground cursor-pointer transition-colors"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            }
            error={errors.password?.message}
            {...register("password")}
          />

          <div className="flex items-center justify-between text-xs sm:text-sm">
            <label className="flex items-center gap-2 cursor-pointer text-muted hover:text-foreground select-none">
              <input
                type="checkbox"
                className="rounded border-border text-primary focus:ring-primary/20 cursor-pointer"
              />
              <span>Ghi nhớ đăng nhập</span>
            </label>
            <button
              type="button"
              className="text-primary hover:underline cursor-pointer font-medium"
              onClick={() => setIsForgotPasswordOpen(true)}
            >
              Quên mật khẩu?
            </button>
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full font-semibold shadow-xs text-sm py-2.5"
              isLoading={isSubmitting}
              leftIcon={<LogIn className="h-4 w-4" />}
            >
              Đăng nhập bằng Mật khẩu
            </Button>
          </div>
        </form>
      )}

      {/* Modal Quên mật khẩu */}
      <ForgotPasswordModal
        isOpen={isForgotPasswordOpen}
        onClose={() => setIsForgotPasswordOpen(false)}
        defaultEmail={emailValue}
      />
    </div>
  );
}

export default LoginPage;
