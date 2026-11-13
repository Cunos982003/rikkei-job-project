import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Lock, Mail, Eye, EyeOff, LogIn, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "../../store/useAuthStore";
import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { authApi, type DemoAccount } from "@/api";

const loginSchema = z.object({
  email: z.string().min(1, "Vui lòng nhập địa chỉ email").email("Email không đúng định dạng"),
  password: z.string().min(1, "Vui lòng nhập mật khẩu"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

const DEFAULT_DEMO_ACCOUNTS: DemoAccount[] = [
  { role: "CANDIDATE", email: "candidate1@rikkeijobs.test", label: "Ứng viên (Nguyễn Văn An)" },
  { role: "HR", email: "hr1@rikkeijobs.test", label: "Doanh nghiệp HR (Rikkeisoft)" },
  { role: "MENTOR", email: "mentor1@rikkeijobs.test", label: "Mentor (Phạm Minh Đức)" },
  { role: "STAFF", email: "staff1@rikkeijobs.test", label: "Cán bộ tuyển dụng" },
  { role: "ADMIN", email: "admin1@rikkeijobs.test", label: "Quản trị hệ thống" },
];

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuthStore();
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { data: demoAccounts } = useQuery<DemoAccount[]>({
    queryKey: ["demo-accounts"],
    queryFn: async () => {
      try {
        const res = await authApi.getDemoAccounts();
        return res.data && res.data.length > 0 ? res.data : DEFAULT_DEMO_ACCOUNTS;
      } catch {
        return DEFAULT_DEMO_ACCOUNTS;
      }
    },
    initialData: DEFAULT_DEMO_ACCOUNTS,
    staleTime: Infinity,
  });

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const onSubmit = async (values: LoginFormValues) => {
    setIsSubmitting(true);
    try {
      const data = await login(values.email, values.password);
      toast.success(`Đăng nhập thành công! Chào mừng ${data.user.fullName}`);

      // Điều hướng về trang trước đó hoặc trang chủ của vai trò
      const homePathByRole = {
        HR: "/hr/company",
        MENTOR: "/mentor/students",
        STAFF: "/staff/jobs",
        ADMIN: "/admin/skills",
        CANDIDATE: "/candidate/profile",
      } as const;

      const rolePrefixByRole: Record<string, string> = {
        HR: "/hr",
        MENTOR: "/mentor",
        STAFF: "/staff",
        ADMIN: "/admin",
        CANDIDATE: "/candidate",
      };

      const userRole = data.user.role;
      const expectedPrefix = rolePrefixByRole[userRole];
      const fallbackHome = homePathByRole[userRole] ?? "/candidate/profile";

      const fromPath = (location.state as { from?: { pathname: string } })?.from?.pathname;
      if (
        fromPath &&
        fromPath !== "/login" &&
        fromPath !== "/unauthorized" &&
        (!expectedPrefix || fromPath.startsWith(expectedPrefix))
      ) {
        navigate(fromPath, { replace: true });
      } else {
        navigate(fallbackHome, { replace: true });
      }
    } catch {
      // Lỗi đã được Axios response interceptor hiển thị qua toast
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleQuickFill = (email: string) => {
    setValue("email", email, { shouldValidate: true });
    setValue("password", "Test@123", { shouldValidate: true });
  };

  return (
    <div className="w-full max-w-md mx-auto space-y-7">
      <div className="text-left space-y-2">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
          Đăng nhập tài khoản
        </h1>
        <p className="text-sm text-muted leading-relaxed">
          Nhập thông tin xác thực để truy cập hệ thống việc làm và hồ sơ năng lực Rikkei Jobs
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4.5">
        <Input
          label="Địa chỉ Email"
          type="email"
          placeholder="vd: candidate1@rikkeijobs.test"
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
            onClick={() => toast.info("Vui lòng liên hệ Ban Đào tạo để đặt lại mật khẩu.")}
          >
            Quên mật khẩu?
          </button>
        </div>

        <Button
          type="submit"
          variant="primary"
          size="lg"
          className="w-full font-semibold shadow-sm text-base py-3"
          isLoading={isSubmitting}
          leftIcon={<LogIn className="h-5 w-5" />}
        >
          Đăng nhập ngay
        </Button>
      </form>

      {/* Quick Fill Test Accounts */}
      <div className="pt-5 border-t border-border space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-muted">
            <Sparkles className="h-3.5 w-3.5 text-primary" />
            <span>Tài khoản kiểm thử nhanh (Click để điền)</span>
          </div>
          <span className="text-[11px] text-muted-light">Mật khẩu: Test@123</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {(demoAccounts ?? []).map((acc) => (
            <button
              key={acc.email}
              type="button"
              onClick={() => handleQuickFill(acc.email)}
              className="text-xs px-3 py-1.5 bg-slate-50 hover:bg-slate-100 hover:border-slate-300 text-slate-700 rounded-lg border border-slate-200 transition-all cursor-pointer font-medium flex items-center gap-1.5 shadow-2xs hover:shadow-xs active:scale-98"
            >
              <span className="w-1.5 h-1.5 rounded-full bg-primary/70 shrink-0" />
              <span>{acc.label}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

