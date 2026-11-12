import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { Building2, Camera, CheckCircle2, ImagePlus, RefreshCw, Save } from "lucide-react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "../../components/common/Button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../../components/common/Card";
import { Input } from "../../components/common/Input";
import { Skeleton } from "../../components/feedback/Skeleton";
import { companiesApi, type CompanyProfile, type UpdateCompanyProfileRequest } from "../../api/endpoints/companies.api";

const companyProfileSchema = z.object({
  name: z.string().trim().min(1, "Vui lòng nhập tên doanh nghiệp").max(255, "Tên doanh nghiệp tối đa 255 ký tự"),
  industry: z.string().max(255, "Lĩnh vực tối đa 255 ký tự"),
  size: z.enum(["", "1-50", "51-200", "201-1000", "1000+"], {
    error: "Vui lòng chọn quy mô doanh nghiệp",
  }),
  address: z.string().max(500, "Địa chỉ tối đa 500 ký tự"),
  city: z.string().max(100, "Tỉnh/thành phố tối đa 100 ký tự"),
  website: z.string().max(255, "Website tối đa 255 ký tự").refine(
    (value) => value === "" || /^https?:\/\/.+\..+/.test(value),
    "Website phải bắt đầu bằng http:// hoặc https://",
  ),
  description: z.string(),
});

type CompanyProfileFormValues = z.infer<typeof companyProfileSchema>;
type CompanySize = CompanyProfileFormValues["size"];

const companySizes: CompanySize[] = ["", "1-50", "51-200", "201-1000", "1000+"];

const emptyFormValues: CompanyProfileFormValues = {
  name: "",
  industry: "",
  size: "",
  address: "",
  city: "",
  website: "",
  description: "",
};

const companyQueryKey = ["companies", "me"] as const;
const MAX_LOGO_SIZE = 2 * 1024 * 1024;
const ALLOWED_LOGO_TYPES = ["image/jpeg", "image/png"];

function toFormValues(company: CompanyProfile): CompanyProfileFormValues {
  return {
    name: company.name,
    industry: company.industry ?? "",
    size: companySizes.find((size) => size === company.size) ?? "",
    address: company.address ?? "",
    city: company.city ?? "",
    website: company.website ?? "",
    description: company.description ?? "",
  };
}

function getLogoUrl(logoUrl: string | null): string | null {
  if (!logoUrl) return null;
  if (/^https?:\/\//i.test(logoUrl)) return logoUrl;
  return new URL(logoUrl, window.location.origin).toString();
}

function CompanyProfileSkeleton() {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-6 w-56" />
        <Skeleton className="mt-2 h-4 w-80 max-w-full" />
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid gap-5 sm:grid-cols-2">
          {Array.from({ length: 6 }, (_, index) => (
            <div className="space-y-2" key={index}>
              <Skeleton className="h-4 w-28" />
              <Skeleton className="h-10 w-full" />
            </div>
          ))}
        </div>
        <Skeleton className="h-28 w-full" />
      </CardContent>
    </Card>
  );
}

export function HrCompanyPage() {
  const queryClient = useQueryClient();
  const [selectedLogo, setSelectedLogo] = useState<File | null>(null);
  const [logoError, setLogoError] = useState<string | null>(null);
  const previewUrl = useMemo(
    () => (selectedLogo ? URL.createObjectURL(selectedLogo) : null),
    [selectedLogo],
  );

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  const companyQuery = useQuery({
    queryKey: companyQueryKey,
    queryFn: async () => (await companiesApi.getMyCompany()).data,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<CompanyProfileFormValues>({
    resolver: zodResolver(companyProfileSchema),
    defaultValues: emptyFormValues,
  });

  useEffect(() => {
    if (companyQuery.data) reset(toFormValues(companyQuery.data));
  }, [companyQuery.data, reset]);

  const updateMutation = useMutation({
    mutationFn: (values: UpdateCompanyProfileRequest) => companiesApi.updateMyCompany(values),
    onSuccess: async (response) => {
      queryClient.setQueryData(companyQueryKey, response.data);
      reset(toFormValues(response.data));
      toast.success("Cập nhật hồ sơ công ty thành công");
    },
  });

  const logoMutation = useMutation({
    mutationFn: (file: File) => companiesApi.uploadMyCompanyLogo(file),
    onSuccess: async () => {
      setSelectedLogo(null);
      setLogoError(null);
      await queryClient.invalidateQueries({ queryKey: companyQueryKey });
      toast.success("Cập nhật logo công ty thành công");
    },
  });

  const onSubmit = (values: CompanyProfileFormValues) => {
    updateMutation.mutate({ ...values, size: values.size || null });
  };

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

  if (companyQuery.isPending) {
    return (
      <div className="mx-auto max-w-5xl space-y-6">
        <PageHeading />
        <CompanyProfileSkeleton />
      </div>
    );
  }

  if (companyQuery.isError || !companyQuery.data) {
    return (
      <div className="mx-auto max-w-5xl space-y-6">
        <PageHeading />
        <Card>
          <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
            <div className="rounded-full bg-primary-light p-3 text-primary">
              <Building2 className="h-6 w-6" />
            </div>
            <h2 className="text-base font-semibold">Chưa tải được hồ sơ công ty</h2>
            <p className="max-w-md text-sm text-muted">
              Vui lòng thử lại. Nếu tài khoản HR chưa được gán doanh nghiệp, hãy liên hệ cán bộ phụ trách.
            </p>
            <Button
              variant="outline"
              size="sm"
              leftIcon={<RefreshCw className="h-4 w-4" />}
              onClick={() => void companyQuery.refetch()}
            >
              Thử lại
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const company = companyQuery.data;
  const currentLogoUrl = getLogoUrl(company.logoUrl);

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <PageHeading />

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5 text-primary" />
            Hồ sơ công ty
          </CardTitle>
          <CardDescription>
            Cập nhật thông tin cơ bản để ứng viên hiểu rõ hơn về doanh nghiệp của bạn.
          </CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-7">
            <div className="flex flex-col gap-5 border-b border-border-subtle pb-6 sm:flex-row sm:items-center">
              <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-border bg-slate-50">
                {previewUrl || currentLogoUrl ? (
                  <img
                    src={previewUrl ?? currentLogoUrl ?? undefined}
                    alt={`Logo ${company.name}`}
                    className="h-full w-full object-contain"
                  />
                ) : (
                  <Building2 className="h-9 w-9 text-slate-300" />
                )}
              </div>
              <div className="min-w-0 flex-1">
                <h2 className="text-sm font-semibold">Logo doanh nghiệp</h2>
                <p className="mt-1 text-xs text-muted">Ảnh PNG hoặc JPG, tối đa 2MB.</p>
                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <label className="inline-flex cursor-pointer items-center gap-2 rounded-lg border border-border bg-surface px-3 py-2 text-xs font-medium text-foreground transition-colors hover:bg-slate-50">
                    <ImagePlus className="h-4 w-4" />
                    Chọn ảnh
                    <input
                      type="file"
                      accept="image/png,image/jpeg"
                      className="sr-only"
                      onChange={(event) => {
                        onLogoSelected(event.target.files?.[0]);
                        event.currentTarget.value = "";
                      }}
                    />
                  </label>
                  {selectedLogo && (
                    <>
                      <span className="max-w-48 truncate text-xs text-muted">{selectedLogo.name}</span>
                      <Button
                        type="button"
                        size="sm"
                        isLoading={logoMutation.isPending}
                        leftIcon={<Camera className="h-4 w-4" />}
                        onClick={() => logoMutation.mutate(selectedLogo)}
                      >
                        Tải logo lên
                      </Button>
                    </>
                  )}
                </div>
                {logoError && <p className="mt-2 text-xs font-medium text-danger">{logoError}</p>}
              </div>
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <Input
                label="Tên doanh nghiệp"
                required
                maxLength={255}
                placeholder="Nhập tên doanh nghiệp"
                error={errors.name?.message}
                {...register("name")}
              />
              <Input
                label="Mã số thuế"
                value={company.taxCode}
                readOnly
                hint="Mã số thuế do cán bộ quản lý xác minh và không thể chỉnh sửa."
              />
              <Input
                label="Lĩnh vực hoạt động"
                maxLength={255}
                placeholder="Ví dụ: Công nghệ thông tin"
                error={errors.industry?.message}
                {...register("industry")}
              />
              <div className="w-full space-y-1.5">
                <label htmlFor="company-size" className="block text-sm font-medium text-foreground">
                  Quy mô nhân sự
                </label>
                <select
                  id="company-size"
                  className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
                  {...register("size")}
                >
                  <option value="">Chọn quy mô</option>
                  <option value="1-50">1–50 nhân viên</option>
                  <option value="51-200">51–200 nhân viên</option>
                  <option value="201-1000">201–1.000 nhân viên</option>
                  <option value="1000+">Trên 1.000 nhân viên</option>
                </select>
                {errors.size?.message && <p className="text-xs font-medium text-danger">{errors.size.message}</p>}
              </div>
              <Input
                label="Địa chỉ"
                maxLength={500}
                placeholder="Số nhà, đường, quận/huyện"
                error={errors.address?.message}
                {...register("address")}
              />
              <Input
                label="Tỉnh/thành phố"
                maxLength={100}
                placeholder="Ví dụ: Hà Nội"
                error={errors.city?.message}
                {...register("city")}
              />
              <Input
                label="Website"
                type="url"
                maxLength={255}
                placeholder="https://example.com"
                error={errors.website?.message}
                {...register("website")}
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="company-description" className="block text-sm font-medium text-foreground">
                Giới thiệu doanh nghiệp
              </label>
              <textarea
                id="company-description"
                rows={5}
                placeholder="Chia sẻ ngắn gọn về doanh nghiệp, văn hóa và lĩnh vực hoạt động..."
                className="w-full resize-y rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                {...register("description")}
              />
            </div>

            <div className="flex flex-col-reverse gap-3 border-t border-border-subtle pt-5 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-xs text-muted">
                Mã số thuế và trạng thái doanh nghiệp chỉ cán bộ phụ trách mới có thể thay đổi.
              </p>
              <div className="flex justify-end gap-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={!isDirty || updateMutation.isPending}
                  onClick={() => reset(toFormValues(company))}
                >
                  Hủy thay đổi
                </Button>
                <Button
                  type="submit"
                  isLoading={updateMutation.isPending}
                  disabled={!isDirty}
                  leftIcon={<Save className="h-4 w-4" />}
                >
                  Lưu hồ sơ
                </Button>
              </div>
            </div>
          </form>
        </CardContent>
      </Card>

      <div className="flex items-start gap-3 rounded-xl border border-success/20 bg-success-light p-4 text-sm">
        <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-success" />
        <p className="text-foreground">
          Hồ sơ được cập nhật sẽ hiển thị trong thông tin doanh nghiệp. Thông tin pháp lý quan trọng vẫn cần cán bộ xác minh.
        </p>
      </div>
    </div>
  );
}

function PageHeading() {
  return (
    <div className="border-b border-border pb-4">
      <h1 className="text-2xl font-bold text-foreground">Hồ sơ công ty</h1>
      <p className="mt-1 text-sm text-muted">
        Quản lý thông tin cơ bản của doanh nghiệp dành cho tài khoản HR.
      </p>
    </div>
  );
}
