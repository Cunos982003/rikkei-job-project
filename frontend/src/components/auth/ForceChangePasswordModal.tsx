import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Lock, Eye, EyeOff, KeyRound, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "../common/Modal";
import { Input } from "../common/Input";
import { Button } from "../common/Button";
import { authApi } from "../../api/endpoints/auth.api";
import { useAuthStore } from "../../store/useAuthStore";

const firstTimePasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, "Mật khẩu mới phải có ít nhất 8 ký tự")
      .regex(/[A-Za-z]/, "Mật khẩu mới phải chứa ít nhất 1 chữ cái")
      .regex(/[0-9]/, "Mật khẩu mới phải chứa ít nhất 1 chữ số"),
    confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu mới"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Xác nhận mật khẩu mới không khớp",
    path: ["confirmPassword"],
  });

type FirstTimePasswordFormValues = z.infer<typeof firstTimePasswordSchema>;

interface ForceChangePasswordModalProps {
  isOpen: boolean;
}

export function ForceChangePasswordModal({ isOpen }: ForceChangePasswordModalProps) {
  const { updateUser } = useAuthStore();
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FirstTimePasswordFormValues>({
    resolver: zodResolver(firstTimePasswordSchema),
    defaultValues: {
      newPassword: "",
      confirmPassword: "",
    },
  });

  const newPasswordValue = watch("newPassword", "");
  const hasMinLength = newPasswordValue.length >= 8;
  const hasLetters = /[A-Za-z]/.test(newPasswordValue);
  const hasNumbers = /[0-9]/.test(newPasswordValue);

  const onSubmit = async (values: FirstTimePasswordFormValues) => {
    setIsSubmitting(true);
    try {
      await authApi.firstTimePassword(values.newPassword.trim());
      updateUser({ mustChangePassword: false });
      toast.success("Thiết lập mật khẩu mới thành công! Tài khoản của bạn đã được kích hoạt.");
    } catch (error: any) {
      const serverMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.message;

      let displayMessage = serverMessage;
      if (serverMessage === "An unexpected internal error occurred" || error?.response?.status === 404) {
        displayMessage = "Máy chủ backend auth-service chưa cập nhật endpoint mới. Vui lòng Restart lại AuthService trong IntelliJ và bấm thử lại.";
      } else if (!displayMessage) {
        displayMessage = "Không thể thiết lập mật khẩu. Vui lòng kiểm tra lại thông tin.";
      }

      toast.error(displayMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={() => {}} // Không cho đóng tự do
      closeOnOverlayClick={false}
      title={
        <span className="flex items-center gap-2">
          <KeyRound className="h-5 w-5 text-primary" />
          <span>Thiết lập mật khẩu lần đầu</span>
        </span>
      }
      description="Tài khoản của bạn vừa đăng nhập bằng mật khẩu tạm thời. Để bảo mật thông tin, bạn vui lòng đặt mật khẩu mới trước khi tiếp tục."
      size="sm"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Mật khẩu mới */}
        <Input
          label="Mật khẩu mới"
          type={showNewPassword ? "text" : "password"}
          placeholder="Tối thiểu 8 ký tự, gồm cả chữ và số"
          required
          leftIcon={<Lock className="h-4 w-4" />}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowNewPassword(!showNewPassword)}
              aria-label={showNewPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              className="text-muted hover:text-foreground cursor-pointer transition-colors"
            >
              {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          }
          error={errors.newPassword?.message}
          {...register("newPassword")}
        />

        {/* Xác nhận mật khẩu mới */}
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

        {/* Tiêu chí mật khẩu đồng bộ 100% với ChangePasswordModal */}
        <div className="rounded-lg bg-slate-50 border border-slate-200/80 p-3 text-[11px] text-muted space-y-1">
          <p className="font-semibold text-foreground">Yêu cầu bảo mật mật khẩu:</p>
          <div className="flex items-center gap-1.5">
            <CheckCircle2
              className={`h-3.5 w-3.5 shrink-0 transition-colors ${
                hasMinLength ? "text-emerald-600" : "text-slate-300"
              }`}
            />
            <span className={hasMinLength ? "text-emerald-700 font-medium" : ""}>
              Tối thiểu 8 ký tự
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <CheckCircle2
              className={`h-3.5 w-3.5 shrink-0 transition-colors ${
                hasLetters && hasNumbers ? "text-emerald-600" : "text-slate-300"
              }`}
            />
            <span className={hasLetters && hasNumbers ? "text-emerald-700 font-medium" : ""}>
              Bao gồm cả chữ cái và chữ số
            </span>
          </div>
        </div>

        {/* Footer actions */}
        <div className="pt-2 border-t border-border-subtle flex justify-end">
          <Button
            type="submit"
            variant="primary"
            size="sm"
            isLoading={isSubmitting}
            leftIcon={<KeyRound className="h-4 w-4" />}
            className="w-full sm:w-auto font-semibold shadow-xs"
          >
            Đặt mật khẩu mới & Kích hoạt tài khoản
          </Button>
        </div>
      </form>
    </Modal>
  );
}
