import { useState, useMemo, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Building2,
  ChevronLeft,
  Users,
  Globe,
  MapPin,
  Lock,
  Unlock,
  UserPlus,
  RefreshCw,
  AlertTriangle,
  Mail,
  Calendar,
  AlertCircle,
  ImagePlus,
  Camera,
  X,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Modal } from "../../components/common/Modal";
import { Skeleton } from "../../components/feedback/Skeleton";
import { CreateHrModal } from "../../components/staff/CreateHrModal";
import {
  staffCompaniesApi,
  type StaffHrAccount,
} from "../../api/endpoints/staffCompanies.api";
import { resolveAssetUrl } from "../../api/client";

export function StaffCompanyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Modals state
  const [isAddHrModalOpen, setIsAddHrModalOpen] = useState(false);

  // Logo upload state
  const [selectedLogo, setSelectedLogo] = useState<File | null>(null);
  const [logoError, setLogoError] = useState<string | null>(null);

  const previewLogoUrl = useMemo(
    () => (selectedLogo ? URL.createObjectURL(selectedLogo) : null),
    [selectedLogo]
  );

  useEffect(() => {
    return () => {
      if (previewLogoUrl) URL.revokeObjectURL(previewLogoUrl);
    };
  }, [previewLogoUrl]);

  const [isLockModalOpen, setIsLockModalOpen] = useState(false);
  const [selectedHrForLock, setSelectedHrForLock] = useState<StaffHrAccount | null>(null);
  const [lockReason, setLockReason] = useState("");
  const [lockReasonError, setLockReasonError] = useState<string | null>(null);

  const [isUnlockModalOpen, setIsUnlockModalOpen] = useState(false);
  const [selectedHrForUnlock, setSelectedHrForUnlock] = useState<StaffHrAccount | null>(null);

  // Query company detail
  const {
    data: response,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["staff", "companies", id],
    queryFn: async () => {
      if (!id) throw new Error("ID không hợp lệ");
      return (await staffCompaniesApi.getById(id)).data;
    },
    enabled: Boolean(id),
  });

  const company = response;

  const onLogoSelected = (file: File | undefined) => {
    if (!file) return;
    if (!["image/jpeg", "image/png"].includes(file.type)) {
      setSelectedLogo(null);
      setLogoError("Logo phải là ảnh PNG hoặc JPG");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setSelectedLogo(null);
      setLogoError("Dung lượng logo không được vượt quá 2MB");
      return;
    }
    setLogoError(null);
    setSelectedLogo(file);
  };

  const uploadLogoMutation = useMutation({
    mutationFn: (file: File) => {
      if (!id) throw new Error("ID không hợp lệ");
      return staffCompaniesApi.uploadLogo(id, file);
    },
    onSuccess: () => {
      toast.success("Cập nhật logo doanh nghiệp thành công");
      setSelectedLogo(null);
      setLogoError(null);
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies", id] });
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies"] });
    },
  });

  // Mutation: Khóa HR
  const lockHrMutation = useMutation({
    mutationFn: ({ hrId, reason }: { hrId: string; reason: string }) =>
      staffCompaniesApi.lockHrAccount(hrId, reason),
    onSuccess: () => {
      toast.success("Đã khóa tài khoản HR thành công");
      setIsLockModalOpen(false);
      setSelectedHrForLock(null);
      setLockReason("");
      setLockReasonError(null);
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies", id] });
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies"] });
    },
  });

  // Mutation: Mở khóa HR
  const unlockHrMutation = useMutation({
    mutationFn: (hrId: string) => staffCompaniesApi.unlockHrAccount(hrId),
    onSuccess: () => {
      toast.success("Đã mở khóa tài khoản HR thành công");
      setIsUnlockModalOpen(false);
      setSelectedHrForUnlock(null);
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies", id] });
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies"] });
    },
  });

  // Handlers
  const handleOpenLock = (hr: StaffHrAccount) => {
    setSelectedHrForLock(hr);
    setLockReason("");
    setLockReasonError(null);
    setIsLockModalOpen(true);
  };

  const handleConfirmLock = () => {
    if (!lockReason.trim()) {
      setLockReasonError("Lý do khóa tài khoản không được để trống");
      return;
    }
    if (!selectedHrForLock) return;
    lockHrMutation.mutate({ hrId: selectedHrForLock.id, reason: lockReason.trim() });
  };

  const handleOpenUnlock = (hr: StaffHrAccount) => {
    setSelectedHrForUnlock(hr);
    setIsUnlockModalOpen(true);
  };

  const handleConfirmUnlock = () => {
    if (!selectedHrForUnlock) return;
    unlockHrMutation.mutate(selectedHrForUnlock.id);
  };

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return "-";
    try {
      return new Date(dateStr).toLocaleString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <Skeleton className="h-9 w-48" />
        <Card>
          <CardContent className="p-6 space-y-4">
            <div className="flex items-center gap-4">
              <Skeleton className="h-16 w-16 rounded-xl" />
              <div className="space-y-2">
                <Skeleton className="h-6 w-64" />
                <Skeleton className="h-4 w-40" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 mt-6">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (isError || !company) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <Button
          variant="outline"
          size="sm"
          leftIcon={<ChevronLeft className="h-4 w-4" />}
          onClick={() => navigate("/staff/companies")}
        >
          Quay lại danh sách
        </Button>
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12 text-center">
            <div className="rounded-full bg-danger-light p-3 text-danger mb-3">
              <AlertCircle className="h-6 w-6" />
            </div>
            <h2 className="text-base font-semibold text-foreground">Không tìm thấy doanh nghiệp</h2>
            <p className="mt-1 text-sm text-muted max-w-md">
              Doanh nghiệp không tồn tại hoặc đã bị xóa khỏi hệ thống.
            </p>
            <Button
              variant="outline"
              size="sm"
              className="mt-4"
              leftIcon={<RefreshCw className="h-4 w-4" />}
              onClick={() => void refetch()}
            >
              Thử lại
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Nút quay lại & Tiêu đề */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-border pb-4">
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<ChevronLeft className="h-4 w-4" />}
            onClick={() => navigate("/staff/companies")}
          >
            Quay lại
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Chi tiết doanh nghiệp</h1>
            <p className="text-xs text-muted">ID: {company.id}</p>
          </div>
        </div>

        <div>
          {company.status === "ACTIVE" ? (
            <Badge variant="verified" size="md">
              Doanh nghiệp hoạt động
            </Badge>
          ) : (
            <Badge variant="danger" size="md">
              Doanh nghiệp đã khóa
            </Badge>
          )}
        </div>
      </div>

      {/* Thông tin hồ sơ doanh nghiệp */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5 text-primary" />
            Thông tin doanh nghiệp
          </CardTitle>
          <CardDescription>
            Hồ sơ pháp lý và thông tin hoạt động của doanh nghiệp trên hệ thống.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5 pb-6 border-b border-border">
            <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-2xl border border-border bg-surface shadow-xs">
              {previewLogoUrl || company.logoUrl ? (
                <img
                  src={previewLogoUrl || resolveAssetUrl(company.logoUrl)}
                  alt={`Logo ${company.name}`}
                  className="h-full w-full object-contain p-1.5"
                />
              ) : (
                <Building2 className="h-10 w-10 text-slate-300" />
              )}
            </div>
            <div className="min-w-0 flex-1 space-y-2">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h2 className="text-xl font-bold text-foreground">{company.name}</h2>
                  <p className="text-xs text-muted flex items-center gap-2 mt-0.5">
                    <span>Mã số thuế: <strong className="font-mono text-foreground">{company.taxCode}</strong></span>
                    {company.industry && <span>• {company.industry}</span>}
                  </p>
                </div>

                {/* Tải lên / Thay đổi logo công ty */}
                <div className="flex items-center gap-2">
                  <label className="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-foreground hover:bg-slate-50 transition-colors">
                    <ImagePlus className="h-3.5 w-3.5" />
                    {company.logoUrl ? "Thay đổi logo" : "Tải logo lên"}
                    <input
                      type="file"
                      accept="image/png,image/jpeg"
                      className="sr-only"
                      onChange={(e) => onLogoSelected(e.target.files?.[0])}
                    />
                  </label>
                  {selectedLogo && (
                    <>
                      <Button
                        size="sm"
                        isLoading={uploadLogoMutation.isPending}
                        leftIcon={<Camera className="h-3.5 w-3.5" />}
                        onClick={() => uploadLogoMutation.mutate(selectedLogo)}
                      >
                        Lưu logo
                      </Button>
                      <button
                        type="button"
                        onClick={() => setSelectedLogo(null)}
                        className="p-1.5 text-muted hover:text-danger rounded-lg transition-colors cursor-pointer"
                        title="Hủy chọn ảnh"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    </>
                  )}
                </div>
              </div>
              {logoError && <p className="text-xs text-danger font-medium">{logoError}</p>}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 text-sm">
            <div className="space-y-1">
              <span className="text-xs font-medium text-muted">Ngành nghề hoạt động</span>
              <p className="font-medium text-foreground">{company.industry || "Chưa cập nhật"}</p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-medium text-muted">Quy mô nhân sự</span>
              <p className="font-medium text-foreground">
                {company.size ? `${company.size} nhân viên` : "Chưa cập nhật"}
              </p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-medium text-muted">Tỉnh/thành phố</span>
              <p className="font-medium text-foreground">{company.city || "Chưa cập nhật"}</p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-medium text-muted">Địa chỉ</span>
              <p className="font-medium text-foreground flex items-center gap-1">
                <MapPin className="h-3.5 w-3.5 text-muted shrink-0" />
                {company.address || "Chưa cập nhật"}
              </p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-medium text-muted">Website</span>
              <p className="font-medium text-foreground">
                {company.website ? (
                  <a
                    href={company.website}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 text-primary hover:underline"
                  >
                    <Globe className="h-3.5 w-3.5" />
                    {company.website}
                  </a>
                ) : (
                  "Chưa cập nhật"
                )}
              </p>
            </div>

            <div className="space-y-1">
              <span className="text-xs font-medium text-muted">Ngày khởi tạo</span>
              <p className="font-medium text-foreground flex items-center gap-1">
                <Calendar className="h-3.5 w-3.5 text-muted shrink-0" />
                {formatDate(company.createdAt)}
              </p>
            </div>
          </div>

          {/* Mô tả */}
          <div className="space-y-2 pt-2 border-t border-border">
            <span className="text-xs font-medium text-muted">Mô tả doanh nghiệp</span>
            <div className="p-4 rounded-xl border border-border bg-slate-50/50 text-sm text-foreground whitespace-pre-line leading-relaxed">
              {company.description || "Chưa có nội dung giới thiệu."}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Danh sách tài khoản HR trực thuộc */}
      <Card>
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Users className="h-5 w-5 text-primary" />
                Danh sách tài khoản HR trực thuộc ({company.hrAccounts?.length ?? 0})
              </CardTitle>
              <CardDescription>
                Quản lý các tài khoản nhân sự có quyền đăng tin và tương tác hồ sơ ứng viên của doanh nghiệp.
              </CardDescription>
            </div>

            <Button
              size="sm"
              leftIcon={<UserPlus className="h-4 w-4" />}
              onClick={() => setIsAddHrModalOpen(true)}
            >
              Thêm tài khoản HR
            </Button>
          </div>
        </CardHeader>

        <CardContent className="p-0">
          {(!company.hrAccounts || company.hrAccounts.length === 0) ? (
            <div className="flex flex-col items-center justify-center p-12 text-center">
              <div className="rounded-full bg-slate-100 p-3 text-slate-400 mb-3">
                <Users className="h-6 w-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">Chưa có tài khoản HR nào</h3>
              <p className="mt-1 text-sm text-muted max-w-md">
                Doanh nghiệp này chưa được gán tài khoản HR nào. Bạn có thể nhấn nút "Thêm tài khoản HR" để tạo mới.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs font-semibold text-muted uppercase tracking-wider border-b border-border">
                  <tr>
                    <th scope="col" className="px-6 py-3.5">Họ và tên</th>
                    <th scope="col" className="px-6 py-3.5">Email</th>
                    <th scope="col" className="px-6 py-3.5">Trạng thái</th>
                    <th scope="col" className="px-6 py-3.5">Lý do khóa</th>
                    <th scope="col" className="px-6 py-3.5">Thời gian khóa</th>
                    <th scope="col" className="px-6 py-3.5">Ngày tạo</th>
                    <th scope="col" className="px-6 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {company.hrAccounts.map((hr) => (
                    <tr key={hr.id} className="hover:bg-slate-50/60 transition-colors">
                      <td className="px-6 py-4 font-semibold text-foreground whitespace-nowrap">
                        {hr.fullName}
                      </td>

                      <td className="px-6 py-4 text-muted whitespace-nowrap">
                        <span className="flex items-center gap-1.5 font-mono text-xs">
                          <Mail className="h-3.5 w-3.5 text-muted" />
                          {hr.email}
                        </span>
                      </td>

                      <td className="px-6 py-4 whitespace-nowrap">
                        {hr.status === "ACTIVE" ? (
                          <Badge variant="verified" size="sm">
                            Hoạt động
                          </Badge>
                        ) : (
                          <Badge variant="danger" size="sm">
                            Đã khóa
                          </Badge>
                        )}
                      </td>

                      <td className="px-6 py-4 text-xs text-danger max-w-xs truncate">
                        {hr.lockedReason || "-"}
                      </td>

                      <td className="px-6 py-4 text-xs text-muted whitespace-nowrap">
                        {formatDate(hr.lockedAt)}
                      </td>

                      <td className="px-6 py-4 text-xs text-muted whitespace-nowrap">
                        {formatDate(hr.createdAt)}
                      </td>

                      <td className="px-6 py-4 text-right whitespace-nowrap">
                        {hr.status === "ACTIVE" ? (
                          <Button
                            variant="danger"
                            size="sm"
                            leftIcon={<Lock className="h-3.5 w-3.5" />}
                            onClick={() => handleOpenLock(hr)}
                          >
                            Khóa
                          </Button>
                        ) : (
                          <Button
                            variant="outline"
                            size="sm"
                            leftIcon={<Unlock className="h-3.5 w-3.5" />}
                            onClick={() => handleOpenUnlock(hr)}
                          >
                            Mở khóa
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* MODAL 1: Cấp tài khoản HR */}
      <CreateHrModal
        isOpen={isAddHrModalOpen}
        onClose={() => setIsAddHrModalOpen(false)}
        companyId={company.id}
        companyName={company.name}
      />

      {/* MODAL 2: Khóa tài khoản HR */}
      <Modal
        isOpen={isLockModalOpen}
        onClose={() => {
          setIsLockModalOpen(false);
          setSelectedHrForLock(null);
          setLockReason("");
          setLockReasonError(null);
        }}
        title="Xác nhận khóa tài khoản HR"
        size="md"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 rounded-xl border border-danger/20 bg-danger-light p-4 text-sm text-danger">
            <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Bạn sắp khóa tài khoản:</p>
              <p className="mt-1 font-medium">
                {selectedHrForLock?.fullName} ({selectedHrForLock?.email})
              </p>
              <p className="mt-1 text-xs text-muted">
                Sau khi khóa, người dùng này sẽ không thể tiếp tục thực hiện bất kỳ thao tác nào và bị đăng xuất khỏi hệ thống ngay lập tức.
              </p>
            </div>
          </div>

          <div className="space-y-1.5">
            <label htmlFor="lock-reason" className="block text-sm font-medium text-foreground">
              Lý do khóa tài khoản <span className="text-danger">*</span>
            </label>
            <textarea
              id="lock-reason"
              rows={4}
              placeholder="Nhập lý do khóa tài khoản (bắt buộc)..."
              value={lockReason}
              onChange={(e) => {
                setLockReason(e.target.value);
                if (e.target.value.trim()) setLockReasonError(null);
              }}
              className="w-full resize-y rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors placeholder:text-muted focus:border-danger focus:ring-2 focus:ring-danger/20"
            />
            {lockReasonError && (
              <p className="text-xs font-medium text-danger">{lockReasonError}</p>
            )}
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t border-border">
            <Button
              variant="outline"
              onClick={() => setIsLockModalOpen(false)}
              disabled={lockHrMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant="danger"
              isLoading={lockHrMutation.isPending}
              disabled={!lockReason.trim()}
              leftIcon={<Lock className="h-4 w-4" />}
              onClick={handleConfirmLock}
            >
              Xác nhận khóa
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL 3: Mở khóa tài khoản HR */}
      <Modal
        isOpen={isUnlockModalOpen}
        onClose={() => {
          setIsUnlockModalOpen(false);
          setSelectedHrForUnlock(null);
        }}
        title="Xác nhận mở khóa tài khoản HR"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Bạn có chắc chắn muốn mở khóa cho tài khoản{" "}
            <strong>{selectedHrForUnlock?.fullName}</strong> (
            <span className="font-mono">{selectedHrForUnlock?.email}</span>)?
          </p>
          <p className="text-xs text-muted">
            Sau khi mở khóa, người dùng sẽ có thể đăng nhập và tiếp tục quản lý các thông tin tuyển dụng bình thường.
          </p>

          <div className="flex justify-end gap-2 pt-4 border-t border-border">
            <Button
              variant="outline"
              onClick={() => setIsUnlockModalOpen(false)}
              disabled={unlockHrMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant="primary"
              isLoading={unlockHrMutation.isPending}
              leftIcon={<Unlock className="h-4 w-4" />}
              onClick={handleConfirmUnlock}
            >
              Xác nhận mở khóa
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
