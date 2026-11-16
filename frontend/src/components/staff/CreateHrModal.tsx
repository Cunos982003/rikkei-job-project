import { useState, useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { UserPlus, Building2, CheckCircle2, Copy, Check, Key, Mail } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "../common/Modal";
import { Input } from "../common/Input";
import { Button } from "../common/Button";
import {
  staffCompaniesApi,
  type CreateHrAccountRequest,
} from "../../api/endpoints/staffCompanies.api";

interface CreateHrModalProps {
  isOpen: boolean;
  onClose: () => void;
  companyId?: string;
  companyName?: string;
  onSuccess?: () => void;
}

export function CreateHrModal({
  isOpen,
  onClose,
  companyId: initialCompanyId,
  companyName: initialCompanyName,
  onSuccess,
}: CreateHrModalProps) {
  const queryClient = useQueryClient();

  const [selectedCompanyId, setSelectedCompanyId] = useState<string>(initialCompanyId || "");
  const [form, setForm] = useState<CreateHrAccountRequest>({ fullName: "", email: "" });
  const [errors, setErrors] = useState<{ companyId?: string; fullName?: string; email?: string }>({});
  const [createdResult, setCreatedResult] = useState<{
    email: string;
    fullName: string;
    temporaryPassword?: string;
    emailSent?: boolean;
  } | null>(null);
  const [isCopied, setIsCopied] = useState(false);

  // Lấy danh sách doanh nghiệp nếu chưa có sẵn companyId
  const { data: companiesResponse } = useQuery({
    queryKey: ["staff", "companies", "all-for-select"],
    queryFn: async () => {
      const res = await staffCompaniesApi.list({ size: 100 });
      return res.data?.items ?? [];
    },
    enabled: isOpen && !initialCompanyId,
  });

  const companies = companiesResponse ?? [];

  useEffect(() => {
    if (isOpen) {
      setSelectedCompanyId(initialCompanyId || "");
      setForm({ fullName: "", email: "" });
      setErrors({});
      setCreatedResult(null);
      setIsCopied(false);
    }
  }, [isOpen, initialCompanyId]);

  const createHrMutation = useMutation({
    mutationFn: (targetCompanyId: string) =>
      staffCompaniesApi.createHrAccount(targetCompanyId, {
        fullName: form.fullName.trim(),
        email: form.email.trim(),
      }),
    onSuccess: (res, targetCompanyId) => {
      const data = (res as any)?.data ?? res;
      // Refresh the list of companies
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies"] });
      // Refresh the detail of the target company (if known)
      if (targetCompanyId) {
        void queryClient.invalidateQueries({ queryKey: ["staff", "companies", targetCompanyId] });
      }

      const emailSent = data?.emailSent !== false;

      setCreatedResult({
        email: form.email.trim(),
        fullName: form.fullName.trim(),
        temporaryPassword: data?.temporaryPassword,
        emailSent: emailSent,
      });

      if (emailSent) {
        toast.success(`Cấp tài khoản HR thành công! Mật khẩu đã được gửi tới email ${form.email.trim()}`);
      } else {
        toast.warning("Tài khoản đã tạo nhưng gửi email thất bại. Hãy sao chép mật khẩu thủ công.");
      }
    },
    onError: (err: any) => {
      const status = err.response?.status;
      const detail = err.response?.data?.detail;
      if (status === 409) {
        setErrors((prev) => ({
          ...prev,
          email: detail || "Email đã được sử dụng bởi người dùng khác",
        }));
      }
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const newErrors: { companyId?: string; fullName?: string; email?: string } = {};

    const targetCompanyId = initialCompanyId || selectedCompanyId;
    if (!targetCompanyId) {
      newErrors.companyId = "Vui lòng chọn doanh nghiệp";
    }
    if (!form.fullName.trim()) {
      newErrors.fullName = "Vui lòng nhập họ và tên HR";
    }
    if (!form.email.trim()) {
      newErrors.email = "Vui lòng nhập email đăng nhập";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      newErrors.email = "Email không đúng định dạng";
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    createHrMutation.mutate(targetCompanyId);
  };

  const handleCopyPassword = () => {
    if (createdResult?.temporaryPassword) {
      void navigator.clipboard.writeText(createdResult.temporaryPassword);
      setIsCopied(true);
      toast.success("Đã sao chép mật khẩu tạm vào clipboard!");
      setTimeout(() => setIsCopied(false), 2000);
    }
  };

  const handleFinish = () => {
    onClose();
    if (onSuccess) {
      onSuccess();
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={createdResult ? handleFinish : onClose}
      title={createdResult ? "Cấp tài khoản thành công" : "Cấp tài khoản HR mới"}
      description={
        createdResult
          ? "Thông tin tài khoản quản trị nhân sự mới."
          : "Tạo tài khoản quản trị nhân sự cho doanh nghiệp. Mật khẩu khởi tạo sẽ được hệ thống sinh ngẫu nhiên và gửi tới email của HR."
      }
      size="md"
    >
      {createdResult ? (
        <div className="space-y-5 py-2">
          <div className="flex items-center gap-3 p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800">
            <CheckCircle2 className="h-6 w-6 text-emerald-600 shrink-0" />
            <div className="text-sm">
              <p className="font-semibold text-emerald-900">Cấp tài khoản thành công!</p>
              <p className="text-xs text-emerald-700 mt-0.5">
                Tài khoản cho <strong>{createdResult.fullName}</strong> đã được khởi tạo trên hệ thống.
              </p>
            </div>
          </div>

          {createdResult.emailSent ? (
            <div className="space-y-3 rounded-xl border border-border bg-slate-50 p-4 text-sm">
              <div className="flex items-start gap-3">
                <Mail className="h-5 w-5 text-primary shrink-0 mt-0.5" />
                <div>
                  <p className="text-xs text-muted">Email nhận thông tin đăng nhập:</p>
                  <p className="font-semibold text-foreground mt-0.5">{createdResult.email}</p>
                  <p className="text-xs text-muted mt-2 leading-relaxed">
                    Hệ thống đã tự động gửi mật khẩu ngẫu nhiên tới hòm thư trên. Nhân sự HR vui lòng kiểm tra hộp thư đến (hoặc hòm thư Spam) để lấy mật khẩu đăng nhập lần đầu.
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-3 rounded-xl border border-border bg-slate-50 p-4 text-sm">
              <div>
                <span className="text-xs text-muted block mb-1">Email đăng nhập:</span>
                <span className="font-semibold text-foreground select-all">{createdResult.email}</span>
              </div>

              <div className="border-t border-border pt-3">
                <span className="text-xs text-muted block mb-1 flex items-center gap-1">
                  <Key className="h-3.5 w-3.5 text-primary" />
                  Mật khẩu tạm thời (gửi mail thất bại, hãy copy thủ công):
                </span>
                <div className="flex items-center gap-2 mt-1">
                  <code className="flex-1 px-3 py-2 bg-white rounded-lg border border-border font-mono text-base font-bold text-primary tracking-wider select-all">
                    {createdResult.temporaryPassword}
                  </code>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={handleCopyPassword}
                    leftIcon={isCopied ? <Check className="h-4 w-4 text-emerald-600" /> : <Copy className="h-4 w-4" />}
                  >
                    {isCopied ? "Đã copy" : "Sao chép"}
                  </Button>
                </div>
              </div>
            </div>
          )}

          <p className="text-xs text-muted italic">
            * Trong lần đăng nhập đầu tiên, cán bộ HR sẽ bắt buộc phải đặt lại mật khẩu mới để kích hoạt tài khoản.
          </p>

          <div className="flex justify-end pt-3 border-t border-border">
            <Button variant="primary" onClick={handleFinish}>
              Hoàn tất & Đóng
            </Button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          {initialCompanyName ? (
            <div className="flex items-center gap-2 p-3 rounded-xl border border-border bg-slate-50 text-sm">
              <Building2 className="h-4 w-4 text-primary shrink-0" />
              <div className="min-w-0">
                <span className="text-xs text-muted block">Doanh nghiệp:</span>
                <strong className="text-foreground truncate block">{initialCompanyName}</strong>
              </div>
            </div>
          ) : (
            <div className="space-y-1.5">
              <label htmlFor="company-picker" className="block text-sm font-medium text-foreground">
                Doanh nghiệp trực thuộc <span className="text-danger">*</span>
              </label>
              <select
                id="company-picker"
                value={selectedCompanyId}
                onChange={(e) => setSelectedCompanyId(e.target.value)}
                className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
              >
                <option value="">-- Chọn doanh nghiệp --</option>
                {companies.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.taxCode})
                  </option>
                ))}
              </select>
              {errors.companyId && <p className="text-xs text-danger font-medium">{errors.companyId}</p>}
            </div>
          )}

          <Input
            label="Họ và tên HR"
            required
            placeholder="Ví dụ: Nguyễn Văn An"
            value={form.fullName}
            error={errors.fullName}
            onChange={(e) => {
              setForm({ ...form, fullName: e.target.value });
              if (errors.fullName) {
                setErrors((prev) => ({ ...prev, fullName: undefined }));
              }
            }}
          />

          <Input
            label="Email đăng nhập"
            required
            type="email"
            placeholder="hr@doanhnghiep.vn"
            value={form.email}
            error={errors.email}
            onChange={(e) => {
              setForm({ ...form, email: e.target.value });
              if (errors.email) {
                setErrors((prev) => ({ ...prev, email: undefined }));
              }
            }}
          />

          <div className="flex justify-end gap-2 pt-4 border-t border-border">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              disabled={createHrMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              isLoading={createHrMutation.isPending}
              leftIcon={<UserPlus className="h-4 w-4" />}
            >
              Cấp tài khoản
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
