import { useState } from "react";
import { KeyRound, Eye, EyeOff, ShieldCheck } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "../common/Modal";
import { Input } from "../common/Input";
import { Button } from "../common/Button";
import { authApi } from "../../api/endpoints/auth.api";
import { useAuthStore } from "../../store/useAuthStore";

interface ForceChangePasswordModalProps {
  isOpen: boolean;
}

export function ForceChangePasswordModal({ isOpen }: ForceChangePasswordModalProps) {
  const { updateUser } = useAuthStore();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const newErrors: Record<string, string> = {};

    if (!currentPassword.trim()) {
      newErrors.currentPassword = "Vui lòng nhập mật khẩu tạm thời nhận từ email";
    }
    if (!newPassword.trim()) {
      newErrors.newPassword = "Vui lòng nhập mật khẩu mới";
    } else if (newPassword.length < 8) {
      newErrors.newPassword = "Mật khẩu mới phải có tối thiểu 8 ký tự";
    } else if (!/[A-Za-z]/.test(newPassword) || !/[0-9]/.test(newPassword)) {
      newErrors.newPassword = "Mật khẩu mới phải bao gồm cả chữ cái và chữ số";
    }

    if (newPassword !== confirmPassword) {
      newErrors.confirmPassword = "Xác nhận mật khẩu mới không trùng khớp";
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    setIsSubmitting(true);

    try {
      await authApi.changePassword(currentPassword.trim(), newPassword.trim());
      // Cập nhật trạng thái người dùng trong store: không còn phải đổi mật khẩu nữa
      updateUser({ mustChangePassword: false });
      toast.success("Đổi mật khẩu thành công! Tài khoản của bạn đã được kích hoạt.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.";
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={() => {}} // Không cho đóng tự do
      closeOnOverlayClick={false}
      title="Yêu cầu đổi mật khẩu lần đầu"
      description="Tài khoản của bạn đang sử dụng mật khẩu ngẫu nhiên tạm thời. Để bảo mật thông tin, bạn bắt buộc phải đặt mật khẩu mới trước khi tiếp tục."
      size="md"
    >
      <form onSubmit={handleSubmit} className="space-y-4 pt-1">
        <div className="flex items-center gap-3 p-3.5 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 text-xs leading-relaxed">
          <ShieldCheck className="h-5 w-5 text-amber-600 shrink-0" />
          <span>
            Mật khẩu mới phải có <strong>tối thiểu 8 ký tự</strong>, bao gồm <strong>cả chữ cái và chữ số</strong>.
          </span>
        </div>

        <div className="relative">
          <Input
            label="Mật khẩu tạm thời (nhận qua email)"
            type={showCurrent ? "text" : "password"}
            required
            placeholder="Nhập mật khẩu tạm thời"
            value={currentPassword}
            error={errors.currentPassword}
            onChange={(e) => {
              setCurrentPassword(e.target.value);
              if (errors.currentPassword) setErrors((prev) => ({ ...prev, currentPassword: "" }));
            }}
          />
          <button
            type="button"
            onClick={() => setShowCurrent(!showCurrent)}
            className="absolute right-3 top-9 text-muted hover:text-foreground cursor-pointer"
            tabIndex={-1}
          >
            {showCurrent ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>

        <div className="relative">
          <Input
            label="Mật khẩu mới"
            type={showNew ? "text" : "password"}
            required
            placeholder="Tối thiểu 8 ký tự gồm chữ và số"
            value={newPassword}
            error={errors.newPassword}
            onChange={(e) => {
              setNewPassword(e.target.value);
              if (errors.newPassword) setErrors((prev) => ({ ...prev, newPassword: "" }));
            }}
          />
          <button
            type="button"
            onClick={() => setShowNew(!showNew)}
            className="absolute right-3 top-9 text-muted hover:text-foreground cursor-pointer"
            tabIndex={-1}
          >
            {showNew ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>

        <div className="relative">
          <Input
            label="Xác nhận mật khẩu mới"
            type={showConfirm ? "text" : "password"}
            required
            placeholder="Nhập lại mật khẩu mới"
            value={confirmPassword}
            error={errors.confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value);
              if (errors.confirmPassword) setErrors((prev) => ({ ...prev, confirmPassword: "" }));
            }}
          />
          <button
            type="button"
            onClick={() => setShowConfirm(!showConfirm)}
            className="absolute right-3 top-9 text-muted hover:text-foreground cursor-pointer"
            tabIndex={-1}
          >
            {showConfirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>

        <div className="pt-3 border-t border-border flex justify-end">
          <Button
            type="submit"
            variant="primary"
            isLoading={isSubmitting}
            leftIcon={<KeyRound className="h-4 w-4" />}
            className="w-full sm:w-auto"
          >
            Đổi mật khẩu & Kích hoạt tài khoản
          </Button>
        </div>
      </form>
    </Modal>
  );
}
