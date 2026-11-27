import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Mail, Send, CheckCircle2 } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "../common/Modal";
import { Input } from "../common/Input";
import { Button } from "../common/Button";
import { authApi } from "../../api/endpoints/auth.api";

const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, "Vui lòng nhập địa chỉ email")
    .email("Email không đúng định dạng"),
});

type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;

export interface ForgotPasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  defaultEmail?: string;
}

export function ForgotPasswordModal({
  isOpen,
  onClose,
  defaultEmail = "",
}: ForgotPasswordModalProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [submittedEmail, setSubmittedEmail] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    values: {
      email: defaultEmail || "",
    },
  });

  const handleClose = () => {
    setIsSuccess(false);
    reset({ email: defaultEmail || "" });
    onClose();
  };

  const onSubmit = async (data: ForgotPasswordFormValues) => {
    setIsSubmitting(true);
    try {
      await authApi.forgotPassword(data.email.trim().toLowerCase());
      setSubmittedEmail(data.email.trim());
      setIsSuccess(true);
      toast.success("Yêu cầu đặt lại mật khẩu đã được xử lý!");
    } catch (err: any) {
      toast.error(
        err?.response?.data?.message ||
          "Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau."
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={isSuccess ? "Kiểm tra hộp thư" : "Quên mật khẩu"}
      description={
        isSuccess
          ? "Hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn"
          : "Nhập địa chỉ email tài khoản HR / Cán bộ tuyển dụng để nhận liên kết đặt lại mật khẩu."
      }
      size="md"
    >
      {isSuccess ? (
        <div className="space-y-4 py-2">
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl space-y-2">
            <div className="flex items-center gap-2 text-emerald-800 font-bold text-sm">
              <CheckCircle2 className="h-5 w-5 text-emerald-600 shrink-0" />
              <span>Đã gửi email khôi phục mật khẩu!</span>
            </div>
            <p className="text-xs text-emerald-700 leading-relaxed">
              Chúng tôi đã gửi thư chứa liên kết bảo mật tới{" "}
              <strong>{submittedEmail}</strong>.
            </p>
          </div>

          <div className="space-y-2 text-xs text-muted leading-relaxed bg-slate-50 border border-slate-200 p-3.5 rounded-xl">
            <p className="font-semibold text-foreground">Lưu ý bảo mật:</p>
            <ul className="list-disc list-inside space-y-1">
              <li>Liên kết đặt lại mật khẩu có hiệu lực trong <strong>30 phút</strong>.</li>
              <li>Mỗi liên kết chỉ sử dụng được <strong>1 lần duy nhất</strong>.</li>
              <li>Nếu không tìm thấy thư, vui lòng kiểm tra hộp thư Spam hoặc Thùng rác.</li>
            </ul>
          </div>

          <div className="pt-2">
            <Button
              type="button"
              variant="primary"
              className="w-full justify-center text-xs py-2.5"
              onClick={handleClose}
            >
              Đã hiểu, quay lại Đăng nhập
            </Button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-1">
          <Input
            label="Địa chỉ Email tài khoản"
            type="email"
            placeholder="vd: hr1@rikkeijobs.test"
            required
            autoFocus
            leftIcon={<Mail className="h-4 w-4" />}
            error={errors.email?.message}
            {...register("email")}
          />

          <div className="text-[11px] text-muted leading-relaxed">
            Hệ thống sẽ gửi một email chứa liên kết an toàn giúp bạn thiết lập mật khẩu mới ngay lập tức.
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-border">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={isSubmitting}
              leftIcon={<Send className="h-3.5 w-3.5" />}
            >
              Gửi liên kết khôi phục
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
