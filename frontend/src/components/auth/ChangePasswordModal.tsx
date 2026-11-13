import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Lock, Eye, EyeOff, KeyRound, Check } from "lucide-react";
import { toast } from "sonner";
import { authApi } from "../../api/endpoints/auth.api";
import { Modal } from "../common/Modal";
import { Button } from "../common/Button";
import { Input } from "../common/Input";

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
    newPassword: z
      .string()
      .min(8, "Mật khẩu mới phải có ít nhất 8 ký tự")
      .max(64, "Mật khẩu không được vượt quá 64 ký tự"),
    confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu mới"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Mật khẩu xác nhận không khớp",
    path: ["confirmPassword"],
  })
  .refine((data) => data.currentPassword !== data.newPassword, {
    message: "Mật khẩu mới không được trùng với mật khẩu hiện tại",
    path: ["newPassword"],
  });

type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;

interface ChangePasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function ChangePasswordModal({ isOpen, onClose }: ChangePasswordModalProps) {
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  const newPasswordValue = watch("newPassword", "");

  const handleClose = () => {
    reset();
    setServerError(null);
    setShowCurrentPassword(false);
    setShowNewPassword(false);
    setShowConfirmPassword(false);
    onClose();
  };

  const onSubmit = async (values: ChangePasswordFormValues) => {
    setServerError(null);
    try {
      await authApi.changePassword(values.currentPassword, values.newPassword);
      toast.success("Đổi mật khẩu thành công! Hãy ghi nhớ mật khẩu mới của bạn.");
      handleClose();
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.";
      setServerError(msg);
    }
  };

  // Các tiêu chí đánh giá độ mạnh của mật khẩu mới
  const hasMinLength = newPasswordValue.length >= 8;
  const hasLetter = /[a-zA-Z]/.test(newPasswordValue);
  const hasNumber = /[0-9]/.test(newPasswordValue);
  const hasSpecial = /[^a-zA-Z0-9]/.test(newPasswordValue);

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      size="md"
      title={
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-primary-light text-primary flex items-center justify-center border border-primary-border">
            <KeyRound className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-foreground">Đổi mật khẩu tài khoản</h3>
            <p className="text-xs text-muted font-normal mt-0.5">
              Cập nhật mật khẩu để bảo mật tài khoản của bạn
            </p>
          </div>
        </div>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {serverError && (
          <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-center gap-2 animate-in fade-in">
            <span className="font-semibold">Lỗi:</span>
            <span>{serverError}</span>
          </div>
        )}

        {/* 1. Mật khẩu hiện tại */}
        <Input
          label="Mật khẩu hiện tại"
          type={showCurrentPassword ? "text" : "password"}
          placeholder="Nhập mật khẩu bạn đang sử dụng"
          required
          autoComplete="current-password"
          disabled={isSubmitting}
          leftIcon={<Lock className="h-4 w-4" />}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowCurrentPassword(!showCurrentPassword)}
              className="text-muted hover:text-foreground transition-colors cursor-pointer"
              tabIndex={-1}
            >
              {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          }
          error={errors.currentPassword?.message}
          {...register("currentPassword")}
        />

        {/* 2. Mật khẩu mới */}
        <div className="space-y-2">
          <Input
            label="Mật khẩu mới"
            type={showNewPassword ? "text" : "password"}
            placeholder="Tối thiểu 8 ký tự"
            required
            autoComplete="new-password"
            disabled={isSubmitting}
            leftIcon={<KeyRound className="h-4 w-4" />}
            rightIcon={
              <button
                type="button"
                onClick={() => setShowNewPassword(!showNewPassword)}
                className="text-muted hover:text-foreground transition-colors cursor-pointer"
                tabIndex={-1}
              >
                {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            }
            error={errors.newPassword?.message}
            {...register("newPassword")}
          />

          {/* Gợi ý độ an toàn */}
          {newPasswordValue.length > 0 && (
            <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-lg space-y-1.5 text-[11px] text-muted">
              <span className="font-semibold text-foreground text-xs block">
                Tiêu chuẩn mật khẩu:
              </span>
              <div className="grid grid-cols-2 gap-1.5">
                <div
                  className={`flex items-center gap-1.5 ${
                    hasMinLength ? "text-emerald-600 font-medium" : "text-slate-400"
                  }`}
                >
                  <Check className="h-3 w-3" />
                  <span>Ít nhất 8 ký tự</span>
                </div>
                <div
                  className={`flex items-center gap-1.5 ${
                    hasLetter ? "text-emerald-600 font-medium" : "text-slate-400"
                  }`}
                >
                  <Check className="h-3 w-3" />
                  <span>Chứa chữ cái (a-z, A-Z)</span>
                </div>
                <div
                  className={`flex items-center gap-1.5 ${
                    hasNumber ? "text-emerald-600 font-medium" : "text-slate-400"
                  }`}
                >
                  <Check className="h-3 w-3" />
                  <span>Chứa chữ số (0-9)</span>
                </div>
                <div
                  className={`flex items-center gap-1.5 ${
                    hasSpecial ? "text-emerald-600 font-medium" : "text-slate-400"
                  }`}
                >
                  <Check className="h-3 w-3" />
                  <span>Ký tự đặc biệt (!@#$)</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 3. Xác nhận mật khẩu mới */}
        <Input
          label="Xác nhận mật khẩu mới"
          type={showConfirmPassword ? "text" : "password"}
          placeholder="Nhập lại mật khẩu mới"
          required
          autoComplete="new-password"
          disabled={isSubmitting}
          leftIcon={<KeyRound className="h-4 w-4" />}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="text-muted hover:text-foreground transition-colors cursor-pointer"
              tabIndex={-1}
            >
              {showConfirmPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          }
          error={errors.confirmPassword?.message}
          {...register("confirmPassword")}
        />

        {/* Action buttons */}
        <div className="flex items-center justify-end gap-2.5 pt-4 border-t border-border-subtle">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleClose}
            disabled={isSubmitting}
          >
            Hủy
          </Button>
          <Button
            type="submit"
            variant="primary"
            size="sm"
            isLoading={isSubmitting}
            disabled={isSubmitting}
          >
            Đổi mật khẩu
          </Button>
        </div>
      </form>
    </Modal>
  );
}

