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

const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại"),
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
  })
  .refine((data) => data.currentPassword !== data.newPassword, {
    message: "Mật khẩu mới không được trùng với mật khẩu hiện tại",
    path: ["newPassword"],
  });

type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;

export interface ChangePasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function ChangePasswordModal({ isOpen, onClose }: ChangePasswordModalProps) {
  const { updateUser } = useAuthStore();
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: "",
      newPassword: "",
      confirmPassword: "",
    },
  });

  const handleClose = () => {
    reset();
    setShowCurrentPassword(false);
    setShowNewPassword(false);
    setShowConfirmPassword(false);
    onClose();
  };

  const onSubmit = async (values: ChangePasswordFormValues) => {
    setIsSubmitting(true);
    try {
      await authApi.changePassword(values.currentPassword, values.newPassword);
      toast.success("Đổi mật khẩu thành công!");
      updateUser({ mustChangePassword: false });
      handleClose();
    } catch (error: any) {
      const serverMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.message;

      if (serverMessage?.toLowerCase().includes("current password") || serverMessage?.toLowerCase().includes("mật khẩu hiện tại")) {
        setError("currentPassword", {
          type: "manual",
          message: "Mật khẩu hiện tại không chính xác",
        });
      } else if (serverMessage) {
        toast.error(serverMessage);
      } else {
        toast.error("Không thể đổi mật khẩu. Vui lòng kiểm tra lại thông tin");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={
        <span className="flex items-center gap-2">
          <KeyRound className="h-5 w-5 text-primary" />
          <span>Thay đổi mật khẩu</span>
        </span>
      }
      description="Cập nhật mật khẩu để bảo vệ an toàn cho tài khoản của bạn."
      size="sm"
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {/* Mật khẩu hiện tại */}
        <Input
          label="Mật khẩu hiện tại"
          type={showCurrentPassword ? "text" : "password"}
          placeholder="Nhập mật khẩu hiện tại"
          required
          leftIcon={<Lock className="h-4 w-4" />}
          rightIcon={
            <button
              type="button"
              onClick={() => setShowCurrentPassword(!showCurrentPassword)}
              aria-label={showCurrentPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              className="text-muted hover:text-foreground cursor-pointer transition-colors"
            >
              {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          }
          error={errors.currentPassword?.message}
          {...register("currentPassword")}
        />

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

        {/* Tiêu chí mật khẩu */}
        <div className="rounded-lg bg-slate-50 border border-slate-200/80 p-3 text-[11px] text-muted space-y-1">
          <p className="font-semibold text-foreground">Yêu cầu bảo mật mật khẩu:</p>
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="h-3 w-3 text-emerald-600 shrink-0" />
            <span>Tối thiểu 8 ký tự</span>
          </div>
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="h-3 w-3 text-emerald-600 shrink-0" />
            <span>Bao gồm cả chữ cái và chữ số</span>
          </div>
        </div>

        {/* Footer buttons */}
        <div className="flex items-center justify-end gap-2.5 pt-2 border-t border-border-subtle">
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
            leftIcon={<KeyRound className="h-4 w-4" />}
          >
            Đổi mật khẩu
          </Button>
        </div>
      </form>
    </Modal>
  );
}

