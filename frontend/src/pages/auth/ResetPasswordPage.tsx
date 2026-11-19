import { useState } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  Lock,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertTriangle,
  ShieldAlert,
  ArrowLeft,
  KeyRound,
  RefreshCw,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "../../components/common/Button";
import { Input } from "../../components/common/Input";
import { authApi } from "../../api/endpoints/auth.api";

const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, "Mật khẩu phải có ít nhất 8 ký tự")
      .regex(/[A-Za-z]/, "Mật khẩu phải chứa ít nhất một chữ cái")
      .regex(/[0-9]/, "Mật khẩu phải chứa ít nhất một chữ số"),
    confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu mới"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Xác nhận mật khẩu không khớp",
    path: ["confirmPassword"],
  });

type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;

export function ResetPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token")?.trim() || "";

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [tokenError, setTokenError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      newPassword: "",
      confirmPassword: "",
    },
  });

  const newPasswordValue = watch("newPassword") || "";
  const hasMinLength = newPasswordValue.length >= 8;
  const hasLetter = /[A-Za-z]/.test(newPasswordValue);
  const hasNumber = /[0-9]/.test(newPasswordValue);

  const onSubmit = async (data: ResetPasswordFormValues) => {
    if (!token) {
      setTokenError("Liên kết đặt lại mật khẩu không hợp lệ hoặc thiếu mã bảo mật (token).");
      return;
    }

    setIsSubmitting(true);
    setTokenError(null);
    try {
      await authApi.resetPassword({
        token,
        newPassword: data.newPassword,
      });
      setIsSuccess(true);
      toast.success("Đặt lại mật khẩu thành công!");
    } catch (err: any) {
      const errorMsg =
        err?.response?.data?.message ||
        err?.message ||
        "Không thể đặt lại mật khẩu.";

      // Kiểm tra các trường hợp token đã dùng hoặc hết hạn
      const isAlreadyUsed =
        /already been used/i.test(errorMsg) ||
        /đã được sử dụng/i.test(errorMsg) ||
        err?.response?.data?.errorCode === "RESET_TOKEN_ALREADY_USED";
      const isExpired =
        /expired/i.test(errorMsg) ||
        /hết hạn/i.test(errorMsg) ||
        err?.response?.data?.errorCode === "RESET_TOKEN_EXPIRED";
      const isInvalid =
        /invalid/i.test(errorMsg) ||
        err?.response?.data?.errorCode === "RESET_TOKEN_INVALID";

      if (isAlreadyUsed || isExpired || isInvalid) {
        setTokenError(
          isAlreadyUsed
            ? "Liên kết đặt lại mật khẩu này đã được sử dụng trước đó. Mỗi liên kết chỉ có hiệu lực một lần duy nhất."
            : isExpired
            ? "Liên kết đặt lại mật khẩu đã hết hạn (chỉ có hiệu lực trong vòng 30 phút)."
            : "Liên kết đặt lại mật khẩu không hợp lệ hoặc không tồn tại trên hệ thống."
        );
      } else {
        toast.error(errorMsg);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Trường hợp 1: Không có token trên URL
  if (!token) {
    return (
      <div className="w-full max-w-md mx-auto space-y-6">
        <div className="text-left space-y-1.5">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
            Đặt lại mật khẩu
          </h1>
          <p className="text-xs sm:text-sm text-muted">
            Thiết lập mật khẩu mới cho tài khoản Rikkei Jobs
          </p>
        </div>

        <div className="p-6 bg-rose-50 border border-rose-200 rounded-2xl text-center space-y-4 shadow-xs">
          <div className="w-12 h-12 rounded-xl bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-rose-950">
              Liên kết không hợp lệ
            </h3>
            <p className="text-xs text-rose-800 leading-relaxed max-w-sm mx-auto">
              Không tìm thấy mã xác thực bảo mật (token) trên đường dẫn. Vui lòng sử dụng liên kết đầy đủ được gửi trong email của bạn.
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            className="w-full justify-center text-xs"
            onClick={() => navigate("/login")}
            leftIcon={<ArrowLeft className="h-4 w-4" />}
          >
            Quay lại trang Đăng nhập
          </Button>
        </div>
      </div>
    );
  }

  // Trường hợp 2: Token đã sử dụng hoặc hết hạn (Tiêu chí nghiệm thu mở lại link lần 2)
  if (tokenError) {
    return (
      <div className="w-full max-w-md mx-auto space-y-6">
        <div className="text-left space-y-1.5">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
            Đặt lại mật khẩu
          </h1>
          <p className="text-xs sm:text-sm text-muted">
            Xác thực liên kết khôi phục tài khoản
          </p>
        </div>

        <div className="p-6 bg-amber-50 border border-amber-200 rounded-2xl text-center space-y-4 shadow-xs">
          <div className="w-12 h-12 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center mx-auto">
            <ShieldAlert className="h-6 w-6" />
          </div>
          <div className="space-y-2">
            <span className="inline-block px-2.5 py-0.5 rounded-full bg-amber-200/80 text-amber-900 text-[11px] font-bold">
              Liên kết hết hiệu lực
            </span>
            <h3 className="text-base font-bold text-amber-950">
              Liên kết đã hết hạn hoặc đã được sử dụng
            </h3>
            <p className="text-xs text-amber-900/90 leading-relaxed max-w-sm mx-auto">
              {tokenError}
            </p>
          </div>

          <div className="p-3 bg-white/70 border border-amber-200/80 rounded-xl text-left text-[11px] text-muted space-y-1">
            <span className="font-semibold text-foreground">Chính sách bảo mật:</span>
            <p>
              Để bảo vệ an toàn tài khoản, mỗi liên kết đặt lại mật khẩu chỉ cho phép đổi mật khẩu <strong>1 lần duy nhất</strong> trong vòng <strong>30 phút</strong>.
            </p>
          </div>

          <div className="space-y-2 pt-2">
            <Button
              type="button"
              variant="primary"
              className="w-full justify-center text-xs py-2.5"
              onClick={() => navigate("/login")}
              leftIcon={<RefreshCw className="h-4 w-4" />}
            >
              Yêu cầu gửi lại liên kết mới
            </Button>
            <Button
              type="button"
              variant="outline"
              className="w-full justify-center text-xs"
              onClick={() => navigate("/login")}
              leftIcon={<ArrowLeft className="h-4 w-4" />}
            >
              Quay về trang Đăng nhập
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // Trường hợp 3: Đổi mật khẩu thành công
  if (isSuccess) {
    return (
      <div className="w-full max-w-md mx-auto space-y-6">
        <div className="text-left space-y-1.5">
          <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
            Đặt lại mật khẩu
          </h1>
          <p className="text-xs sm:text-sm text-muted">
            Hoàn tất thiết lập tài khoản
          </p>
        </div>

        <div className="p-6 bg-emerald-50 border border-emerald-200 rounded-2xl text-center space-y-4 shadow-xs">
          <div className="w-14 h-14 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-xs">
            <CheckCircle2 className="h-8 w-8" />
          </div>
          <div className="space-y-1.5">
            <h3 className="text-lg font-bold text-emerald-950">
              Đặt lại mật khẩu thành công!
            </h3>
            <p className="text-xs text-emerald-800 leading-relaxed max-w-sm mx-auto">
              Mật khẩu mới của bạn đã được cập nhật an toàn. Bạn có thể sử dụng mật khẩu này để đăng nhập ngay bây giờ.
            </p>
          </div>

          <div className="pt-2">
            <Button
              type="button"
              variant="primary"
              size="lg"
              className="w-full justify-center font-semibold text-sm py-2.5 shadow-xs"
              onClick={() => navigate("/login", { replace: true })}
              leftIcon={<KeyRound className="h-4 w-4" />}
            >
              Đăng nhập ngay
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // Trường hợp 4: Form nhập mật khẩu mới
  return (
    <div className="w-full max-w-md mx-auto space-y-6">
      {/* Header */}
      <div className="text-left space-y-1.5">
        <h1 className="text-2xl sm:text-3xl font-extrabold text-foreground tracking-tight">
          Đặt lại mật khẩu
        </h1>
        <p className="text-xs sm:text-sm text-muted leading-relaxed">
          Vui lòng nhập mật khẩu mới cho tài khoản của bạn
        </p>
      </div>

      <div className="p-3.5 bg-blue-50/80 border border-blue-200/90 rounded-xl text-xs text-blue-900 leading-relaxed flex items-start gap-2.5">
        <KeyRound className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-blue-950 block">Bảo mật tài khoản</span>
          <p className="text-[11px] text-blue-800 mt-0.5">
            Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số.
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Mật khẩu mới"
          type={showPassword ? "text" : "password"}
          placeholder="Nhập mật khẩu mới"
          required
          autoFocus
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
          error={errors.newPassword?.message}
          {...register("newPassword")}
        />

        <Input
          label="Xác nhận mật khẩu mới"
          type={showConfirmPassword ? "text" : "password"}
          placeholder="Nhập lại mật khẩu mới"
          required
          leftIcon={<Lock className="h-4 w-4" />}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              aria-label={showConfirmPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              className="text-muted hover:text-foreground cursor-pointer transition-colors"
            >
              {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          }
          error={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />

        {/* Tiêu chí mật khẩu */}
        <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-1.5 text-xs text-muted">
          <span className="text-[11px] font-semibold text-foreground uppercase tracking-wider block">
            Yêu cầu mật khẩu:
          </span>
          <div className="flex items-center gap-2">
            <div
              className={`h-3 w-3 rounded-full flex items-center justify-center text-[9px] font-bold ${
                hasMinLength ? "bg-emerald-500 text-white" : "bg-slate-200 text-slate-500"
              }`}
            >
              ✓
            </div>
            <span className={hasMinLength ? "text-emerald-700 font-medium" : ""}>
              Tối thiểu 8 ký tự
            </span>
          </div>
          <div className="flex items-center gap-2">
            <div
              className={`h-3 w-3 rounded-full flex items-center justify-center text-[9px] font-bold ${
                hasLetter && hasNumber ? "bg-emerald-500 text-white" : "bg-slate-200 text-slate-500"
              }`}
            >
              ✓
            </div>
            <span className={hasLetter && hasNumber ? "text-emerald-700 font-medium" : ""}>
              Bao gồm cả chữ cái và chữ số
            </span>
          </div>
        </div>

        <div className="pt-2 space-y-2">
          <Button
            type="submit"
            variant="primary"
            size="lg"
            className="w-full font-semibold shadow-xs text-sm py-2.5 justify-center"
            isLoading={isSubmitting}
            leftIcon={<KeyRound className="h-4 w-4" />}
          >
            Xác nhận đổi mật khẩu
          </Button>

          <Link
            to="/login"
            className="inline-flex items-center justify-center gap-1.5 text-xs text-muted hover:text-foreground transition-colors w-full py-1 text-center"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            <span>Quay lại Đăng nhập</span>
          </Link>
        </div>
      </form>
    </div>
  );
}

export default ResetPasswordPage;

