import { useState, useMemo, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Building2, ImagePlus, X } from "lucide-react";
import { toast } from "sonner";
import { Modal } from "../common/Modal";
import { Input } from "../common/Input";
import { Button } from "../common/Button";
import {
  staffCompaniesApi,
  type CreateCompanyRequest,
} from "../../api/endpoints/staffCompanies.api";
import type { CompanyProfile } from "../../api/endpoints/companies.api";

interface CreateCompanyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (createdCompany: CompanyProfile) => void;
}

const MAX_LOGO_SIZE = 2 * 1024 * 1024;
const ALLOWED_LOGO_TYPES = ["image/jpeg", "image/png"];

const initialFormValues: CreateCompanyRequest = {
  name: "",
  taxCode: "",
  industry: "",
  size: "",
  address: "",
  city: "",
  website: "",
  description: "",
};

export function CreateCompanyModal({ isOpen, onClose, onSuccess }: CreateCompanyModalProps) {
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState<CreateCompanyRequest>(initialFormValues);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [selectedLogo, setSelectedLogo] = useState<File | null>(null);
  const [logoError, setLogoError] = useState<string | null>(null);

  const previewUrl = useMemo(
    () => (selectedLogo ? URL.createObjectURL(selectedLogo) : null),
    [selectedLogo]
  );

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  // Reset khi mở lại modal
  useEffect(() => {
    if (isOpen) {
      setFormData(initialFormValues);
      setErrors({});
      setSelectedLogo(null);
      setLogoError(null);
    }
  }, [isOpen]);

  const onLogoSelected = (file: File | undefined) => {
    if (!file) return;
    if (!ALLOWED_LOGO_TYPES.includes(file.type)) {
      setSelectedLogo(null);
      setLogoError("Logo phải là ảnh PNG hoặc JPG");
      return;
    }
    if (file.size > MAX_LOGO_SIZE) {
      setSelectedLogo(null);
      setLogoError("Dung lượng logo không được vượt quá 2MB");
      return;
    }
    setLogoError(null);
    setSelectedLogo(file);
  };

  const createMutation = useMutation({
    mutationFn: async (values: CreateCompanyRequest) => {
      const res = await staffCompaniesApi.createCompany(values);
      const company = (res as any)?.data?.id ? (res as any).data : ((res as any)?.id ? res : (res as any)?.data);

      // Nếu có chọn logo, upload logo ngay
      if (selectedLogo && company?.id) {
        try {
          await staffCompaniesApi.uploadLogo(company.id, selectedLogo, { silentError: true });
        } catch {
          toast.warning("Tạo doanh nghiệp thành công nhưng tải logo lên thất bại");
        }
      }
      return company;
    },
    onSuccess: (createdCompany) => {
      toast.success("Tạo hồ sơ doanh nghiệp mới thành công");
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies"] });
      onClose();
      if (onSuccess) {
        onSuccess(createdCompany);
      }
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = "Vui lòng nhập tên doanh nghiệp";
    }
    const cleanTaxCode = formData.taxCode.trim();
    if (!cleanTaxCode) {
      newErrors.taxCode = "Vui lòng nhập mã số thuế";
    } else if (!/^(\d{10}|\d{13})$/.test(cleanTaxCode)) {
      newErrors.taxCode = "Mã số thuế phải gồm 10 hoặc 13 chữ số";
    }

    if (formData.website && !/^https?:\/\/.+\..+/.test(formData.website.trim())) {
      newErrors.website = "Website phải bắt đầu bằng http:// hoặc https://";
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    const payload: CreateCompanyRequest = {
      name: formData.name.trim(),
      taxCode: cleanTaxCode,
    };

    if (formData.industry?.trim()) payload.industry = formData.industry.trim();
    if (formData.size?.trim()) payload.size = formData.size.trim();
    if (formData.address?.trim()) payload.address = formData.address.trim();
    if (formData.city?.trim()) payload.city = formData.city.trim();
    if (formData.website?.trim()) payload.website = formData.website.trim();
    if (formData.description?.trim()) payload.description = formData.description.trim();

    createMutation.mutate(payload);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Tạo doanh nghiệp mới"
      description="Cán bộ nhập thông tin cơ bản để tạo hồ sơ doanh nghiệp đối tác tuyển dụng trên hệ thống."
      size="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Phần Logo doanh nghiệp */}
        <div className="flex flex-col gap-4 border-b border-border pb-5 sm:flex-row sm:items-center">
          <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-border bg-slate-50">
            {previewUrl ? (
              <img
                src={previewUrl}
                alt="Xem trước logo"
                className="h-full w-full object-contain p-1"
              />
            ) : (
              <Building2 className="h-8 w-8 text-slate-300" />
            )}
          </div>
          <div className="flex-1">
            <label className="text-xs font-semibold text-foreground">Logo doanh nghiệp (tùy chọn)</label>
            <p className="text-xs text-muted mt-0.5">Hỗ trợ JPG/PNG, tối đa 2MB.</p>
            <div className="mt-2 flex items-center gap-2">
              <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-foreground hover:bg-slate-50 transition-colors">
                <ImagePlus className="h-3.5 w-3.5" />
                Chọn ảnh
                <input
                  type="file"
                  accept="image/png,image/jpeg"
                  className="sr-only"
                  onChange={(e) => onLogoSelected(e.target.files?.[0])}
                />
              </label>
              {selectedLogo && (
                <button
                  type="button"
                  onClick={() => setSelectedLogo(null)}
                  className="p-1 text-muted hover:text-danger rounded-md cursor-pointer transition-colors"
                  title="Xóa logo đã chọn"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
            {logoError && <p className="mt-1 text-xs text-danger">{logoError}</p>}
          </div>
        </div>

        {/* Các trường thông tin */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="sm:col-span-2">
            <Input
              label="Tên doanh nghiệp"
              required
              placeholder="Ví dụ: Công ty Cổ phần Rikkeisoft"
              value={formData.name}
              error={errors.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            />
          </div>

          <Input
            label="Mã số thuế"
            required
            placeholder="10 hoặc 13 chữ số"
            value={formData.taxCode}
            error={errors.taxCode}
            onChange={(e) => setFormData({ ...formData, taxCode: e.target.value })}
          />

          <Input
            label="Lĩnh vực hoạt động"
            placeholder="Ví dụ: Công nghệ thông tin"
            value={formData.industry || ""}
            onChange={(e) => setFormData({ ...formData, industry: e.target.value })}
          />

          <div className="space-y-1.5">
            <label htmlFor="company-size-select" className="block text-sm font-medium text-foreground">
              Quy mô nhân sự
            </label>
            <select
              id="company-size-select"
              value={formData.size || ""}
              onChange={(e) => setFormData({ ...formData, size: e.target.value })}
              className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
            >
              <option value="">Chọn quy mô</option>
              <option value="1-50">1–50 nhân viên</option>
              <option value="51-200">51–200 nhân viên</option>
              <option value="201-1000">201–1.000 nhân viên</option>
              <option value="1000+">Trên 1.000 nhân viên</option>
            </select>
          </div>

          <Input
            label="Tỉnh/thành phố"
            placeholder="Ví dụ: Hà Nội"
            value={formData.city || ""}
            onChange={(e) => setFormData({ ...formData, city: e.target.value })}
          />

          <div className="sm:col-span-2">
            <Input
              label="Địa chỉ trụ sở"
              placeholder="Số nhà, đường, quận/huyện..."
              value={formData.address || ""}
              onChange={(e) => setFormData({ ...formData, address: e.target.value })}
            />
          </div>

          <div className="sm:col-span-2">
            <Input
              label="Website"
              type="url"
              placeholder="https://example.com"
              value={formData.website || ""}
              error={errors.website}
              onChange={(e) => setFormData({ ...formData, website: e.target.value })}
            />
          </div>
        </div>

        {/* Giới thiệu doanh nghiệp */}
        <div className="space-y-1.5">
          <label htmlFor="company-desc-input" className="block text-sm font-medium text-foreground">
            Mô tả doanh nghiệp
          </label>
          <textarea
            id="company-desc-input"
            rows={4}
            placeholder="Giới thiệu về quy mô, sản phẩm và môi trường làm việc..."
            value={formData.description || ""}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            className="w-full resize-y rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
        </div>

        <div className="flex justify-end gap-2 pt-4 border-t border-border">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            disabled={createMutation.isPending}
          >
            Hủy
          </Button>
          <Button
            type="submit"
            isLoading={createMutation.isPending}
            leftIcon={<Building2 className="h-4 w-4" />}
          >
            Tạo doanh nghiệp
          </Button>
        </div>
      </form>
    </Modal>
  );
}
