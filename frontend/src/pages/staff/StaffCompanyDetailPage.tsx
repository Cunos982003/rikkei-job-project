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
  History,
  FileText,
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
  type CompanyActionItem,
} from "../../api/endpoints/staffCompanies.api";
import { resolveAssetUrl } from "../../api/client";

export function StaffCompanyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Active tab: HR Accounts or Action History
  const [activeTab, setActiveTab] = useState<"HR" | "HISTORY">("HR");

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

  // HR Lock/Unlock state
  const [isLockModalOpen, setIsLockModalOpen] = useState(false);
  const [selectedHrForLock, setSelectedHrForLock] = useState<StaffHrAccount | null>(null);
  const [lockReason, setLockReason] = useState("");
  const [lockReasonError, setLockReasonError] = useState<string | null>(null);

  const [isUnlockModalOpen, setIsUnlockModalOpen] = useState(false);
  const [selectedHrForUnlock, setSelectedHrForUnlock] = useState<StaffHrAccount | null>(null);

  // Company Lock/Unlock state
  const [isCompanyLockModalOpen, setIsCompanyLockModalOpen] = useState(false);
  const [companyLockReason, setCompanyLockReason] = useState("");
  const [companyLockError, setCompanyLockError] = useState<string | null>(null);

  const [isCompanyUnlockModalOpen, setIsCompanyUnlockModalOpen] = useState(false);

  // History pagination state
  const [historyPage, setHistoryPage] = useState(0);

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

  // Query company action history
  const {
    data: historyResponse,
    isLoading: isHistoryLoading,
    isError: isHistoryError,
    refetch: refetchHistory,
  } = useQuery({
    queryKey: ["staff", "company-history", id, historyPage],
    queryFn: async () => {
      if (!id) throw new Error("ID không hợp lệ");
      const res = await staffCompaniesApi.getHistory(id, { page: historyPage, size: 10 });
      return res.data;
    },
    enabled: Boolean(id) && activeTab === "HISTORY",
  });

  const historyItems: CompanyActionItem[] = historyResponse?.items || [];
  const historyPagination = historyResponse?.pagination;

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
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.response?.data?.detail || err?.message || "Không thể khóa tài khoản HR";
      toast.error(msg);
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
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.response?.data?.detail || err?.message || "Không thể mở khóa tài khoản HR";
      toast.error(msg);
    },
  });

  // Mutation: Khóa công ty
  const lockCompanyMutation = useMutation({
    mutationFn: (reason: string) => {
      if (!id) throw new Error("ID không hợp lệ");
      return staffCompaniesApi.lockCompany(id, reason);
    },
    onSuccess: () => {
      toast.success("Đã khóa doanh nghiệp thành công");
      setIsCompanyLockModalOpen(false);
      setCompanyLockReason("");
      setCompanyLockError(null);
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies", id] });
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies"] });
      void queryClient.invalidateQueries({ queryKey: ["staff", "company-history", id] });
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.response?.data?.detail || err?.message || "Không thể khóa doanh nghiệp";
      toast.error(msg);
    },
  });

  // Mutation: Mở khóa công ty
  const unlockCompanyMutation = useMutation({
    mutationFn: () => {
      if (!id) throw new Error("ID không hợp lệ");
      return staffCompaniesApi.unlockCompany(id);
    },
    onSuccess: () => {
      toast.success("Đã mở khóa doanh nghiệp thành công");
      setIsCompanyUnlockModalOpen(false);
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies", id] });
      void queryClient.invalidateQueries({ queryKey: ["staff", "companies"] });
      void queryClient.invalidateQueries({ queryKey: ["staff", "company-history", id] });
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.response?.data?.detail || err?.message || "Không thể mở khóa doanh nghiệp";
      toast.error(msg);
    },
  });

  // Handlers for HR
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

  // Handlers for Company Lock/Unlock
  const handleConfirmCompanyLock = () => {
    const trimmed = companyLockReason.trim();
    if (!trimmed) {
      setCompanyLockError("Vui lòng nhập lý do khóa doanh nghiệp");
      return;
    }
    if (trimmed.length > 500) {
      setCompanyLockError("Lý do khóa doanh nghiệp không được vượt quá 500 ký tự");
      return;
    }
    lockCompanyMutation.mutate(trimmed);
  };

  const handleConfirmCompanyUnlock = () => {
    unlockCompanyMutation.mutate();
  };

  const formatDate = (dateStr: string | null | undefined) => {
    if (!dateStr) return "-";
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      const pad = (n: number) => String(n).padStart(2, "0");
      const day = pad(d.getDate());
      const month = pad(d.getMonth() + 1);
      const year = d.getFullYear();
      const hours = pad(d.getHours());
      const minutes = pad(d.getMinutes());
      return `${day}/${month}/${year} ${hours}:${minutes}`;
    } catch {
      return dateStr;
    }
  };

  const getActionBadge = (action: string) => {
    switch (action) {
      case "LOCK":
        return <Badge variant="danger" size="sm">Khóa doanh nghiệp</Badge>;
      case "UNLOCK":
        return <Badge variant="verified" size="sm">Mở khóa doanh nghiệp</Badge>;
      case "APPROVE_JD":
        return <Badge variant="verified" size="sm">Duyệt tin tuyển dụng</Badge>;
      case "REJECT_JD":
        return <Badge variant="warning" size="sm">Trả lại tin tuyển dụng</Badge>;
      default:
        return <Badge variant="outline" size="sm">{action}</Badge>;
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

        {/* Trạng thái & Nút Khoá / Mở khoá công ty */}
        <div className="flex items-center gap-3">
          {company.status === "ACTIVE" ? (
            <>
              <Badge variant="verified" size="md">
                Doanh nghiệp hoạt động
              </Badge>
              <Button
                variant="danger"
                size="sm"
                leftIcon={<Lock className="h-4 w-4" />}
                onClick={() => {
                  setCompanyLockReason("");
                  setCompanyLockError(null);
                  setIsCompanyLockModalOpen(true);
                }}
              >
                Khóa doanh nghiệp
              </Button>
            </>
          ) : (
            <>
              <Badge variant="danger" size="md">
                Đã khóa
              </Badge>
              <Button
                variant="outline"
                size="sm"
                leftIcon={<Unlock className="h-4 w-4" />}
                onClick={() => setIsCompanyUnlockModalOpen(true)}
              >
                Mở khóa doanh nghiệp
              </Button>
            </>
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

      {/* Tabs Chuyển đổi giữa Tài khoản HR và Lịch sử thao tác */}
      <div className="space-y-4">
        <div className="flex items-center gap-2 border-b border-border">
          <button
            type="button"
            onClick={() => setActiveTab("HR")}
            className={`pb-3 px-3 text-sm font-semibold border-b-2 transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === "HR"
                ? "border-primary text-primary"
                : "border-transparent text-muted hover:text-foreground"
            }`}
          >
            <Users className="h-4 w-4" />
            Tài khoản HR trực thuộc ({company.hrAccounts?.length ?? 0})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("HISTORY")}
            className={`pb-3 px-3 text-sm font-semibold border-b-2 transition-all cursor-pointer flex items-center gap-2 ${
              activeTab === "HISTORY"
                ? "border-primary text-primary"
                : "border-transparent text-muted hover:text-foreground"
            }`}
          >
            <History className="h-4 w-4" />
            Lịch sử thao tác
          </button>
        </div>

        {/* TAB 1: Danh sách tài khoản HR trực thuộc */}
        {activeTab === "HR" && (
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
        )}

        {/* TAB 2: Lịch sử thao tác doanh nghiệp */}
        {activeTab === "HISTORY" && (
          <Card>
            <CardHeader>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <History className="h-5 w-5 text-primary" />
                    Lịch sử thao tác doanh nghiệp
                  </CardTitle>
                  <CardDescription>
                    Nhật ký các thao tác khóa, mở khóa doanh nghiệp và duyệt, từ chối tin tuyển dụng.
                  </CardDescription>
                </div>

                <Button
                  variant="outline"
                  size="sm"
                  leftIcon={<RefreshCw className={`h-4 w-4 ${isHistoryLoading ? "animate-spin" : ""}`} />}
                  onClick={() => void refetchHistory()}
                >
                  Làm mới
                </Button>
              </div>
            </CardHeader>

            <CardContent className="p-0">
              {isHistoryLoading ? (
                <div className="py-12 text-center text-sm text-muted">
                  <RefreshCw className="h-5 w-5 animate-spin mx-auto mb-2 text-primary" />
                  Đang tải lịch sử thao tác...
                </div>
              ) : isHistoryError ? (
                <div className="py-8 text-center space-y-3">
                  <AlertCircle className="h-8 w-8 text-danger mx-auto" />
                  <p className="text-sm text-danger font-medium">Không thể tải lịch sử thao tác.</p>
                  <Button size="sm" variant="outline" onClick={() => void refetchHistory()}>
                    Thử lại
                  </Button>
                </div>
              ) : historyItems.length === 0 ? (
                <div className="flex flex-col items-center justify-center p-12 text-center">
                  <div className="rounded-full bg-slate-100 p-3 text-slate-400 mb-3">
                    <History className="h-6 w-6" />
                  </div>
                  <h3 className="text-base font-semibold text-foreground">Chưa có thao tác nào</h3>
                  <p className="mt-1 text-sm text-muted max-w-md">
                    Chưa có nhật ký hoạt động nào được ghi nhận cho doanh nghiệp này.
                  </p>
                </div>
              ) : (
                <>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-slate-50 text-xs font-semibold text-muted uppercase tracking-wider border-b border-border">
                        <tr>
                          <th scope="col" className="px-6 py-3.5">Thời điểm</th>
                          <th scope="col" className="px-6 py-3.5">Hành động</th>
                          <th scope="col" className="px-6 py-3.5">Đối tượng</th>
                          <th scope="col" className="px-6 py-3.5">Lý do</th>
                          <th scope="col" className="px-6 py-3.5">Người thực hiện</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border">
                        {historyItems.map((item) => (
                          <tr key={item.id} className="hover:bg-slate-50/60 transition-colors">
                            <td className="px-6 py-4 text-xs font-mono text-muted whitespace-nowrap">
                              {formatDate(item.createdAt)}
                            </td>

                            <td className="px-6 py-4 whitespace-nowrap">
                              {getActionBadge(item.action)}
                            </td>

                            <td className="px-6 py-4 text-foreground whitespace-nowrap">
                              {item.targetTitle ? (
                                <span className="flex items-center gap-1.5 font-medium">
                                  <FileText className="h-3.5 w-3.5 text-primary shrink-0" />
                                  {item.targetTitle}
                                </span>
                              ) : item.targetId ? (
                                <span className="text-xs font-mono text-muted">ID: {item.targetId}</span>
                              ) : (
                                <span className="text-muted">-</span>
                              )}
                            </td>

                            <td className="px-6 py-4 text-xs text-muted max-w-xs break-words">
                              {item.reason || "-"}
                            </td>

                            <td className="px-6 py-4 text-xs font-mono text-muted whitespace-nowrap">
                              {item.staffId || "-"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Phân trang Lịch sử */}
                  {historyPagination && (
                    <div className="flex items-center justify-between px-6 py-3.5 border-t border-border bg-slate-50/50">
                      <span className="text-xs text-muted">
                        Trang {historyPage + 1} / {Math.max(1, historyPagination.totalPages)} ({historyPagination.totalElements} thao tác)
                      </span>
                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={historyPage <= 0 || isHistoryLoading}
                          onClick={() => setHistoryPage((p) => Math.max(0, p - 1))}
                        >
                          Trước
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={!historyPagination.hasNext || isHistoryLoading}
                          onClick={() => setHistoryPage((p) => p + 1)}
                        >
                          Sau
                        </Button>
                      </div>
                    </div>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        )}
      </div>

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

      {/* MODAL 4: Khóa Doanh nghiệp */}
      <Modal
        isOpen={isCompanyLockModalOpen}
        onClose={() => {
          if (!lockCompanyMutation.isPending) {
            setIsCompanyLockModalOpen(false);
            setCompanyLockReason("");
            setCompanyLockError(null);
          }
        }}
        title="Xác nhận khóa doanh nghiệp"
        size="md"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-3 rounded-xl border border-danger/20 bg-danger-light p-4 text-sm text-danger">
            <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold">Cảnh báo khóa doanh nghiệp: {company.name}</p>
              <p className="mt-1 text-xs leading-relaxed">
                Toàn bộ tài khoản HR sẽ không đăng nhập được và tin tuyển dụng sẽ bị ẩn khỏi trang việc làm.
              </p>
            </div>
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label htmlFor="company-lock-reason" className="block text-sm font-medium text-foreground">
                Lý do khóa doanh nghiệp <span className="text-danger">*</span>
              </label>
              <span className={`text-[11px] ${companyLockReason.length > 500 ? "text-danger font-semibold" : "text-muted"}`}>
                {companyLockReason.length}/500 ký tự
              </span>
            </div>
            <textarea
              id="company-lock-reason"
              rows={4}
              maxLength={500}
              placeholder="Nhập lý do khóa doanh nghiệp (bắt buộc, tối đa 500 ký tự)..."
              value={companyLockReason}
              onChange={(e) => {
                setCompanyLockReason(e.target.value);
                if (e.target.value.trim()) setCompanyLockError(null);
              }}
              className="w-full resize-y rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors placeholder:text-muted focus:border-danger focus:ring-2 focus:ring-danger/20"
            />
            {companyLockError && (
              <p className="text-xs font-medium text-danger">{companyLockError}</p>
            )}
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t border-border">
            <Button
              variant="outline"
              onClick={() => setIsCompanyLockModalOpen(false)}
              disabled={lockCompanyMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant="danger"
              isLoading={lockCompanyMutation.isPending}
              disabled={!companyLockReason.trim() || companyLockReason.length > 500}
              leftIcon={<Lock className="h-4 w-4" />}
              onClick={handleConfirmCompanyLock}
            >
              Xác nhận khóa doanh nghiệp
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL 5: Mở khóa Doanh nghiệp */}
      <Modal
        isOpen={isCompanyUnlockModalOpen}
        onClose={() => {
          if (!unlockCompanyMutation.isPending) {
            setIsCompanyUnlockModalOpen(false);
          }
        }}
        title="Xác nhận mở khóa doanh nghiệp"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-sm text-foreground leading-relaxed">
            Bạn có chắc chắn muốn mở khóa cho doanh nghiệp <strong>{company.name}</strong> (MST: <span className="font-mono">{company.taxCode}</span>)?
          </p>
          <div className="rounded-lg bg-slate-50 p-3 text-xs text-muted space-y-1">
            <p>• Doanh nghiệp sẽ chuyển về trạng thái <strong>Hoạt động</strong>.</p>
            <p>• Các tài khoản HR bị khóa theo doanh nghiệp sẽ được mở lại.</p>
            <p>• Các tin tuyển dụng đã phê duyệt sẽ hiển thị lại trên trang tìm việc làm.</p>
          </div>

          <div className="flex justify-end gap-2 pt-4 border-t border-border">
            <Button
              variant="outline"
              onClick={() => setIsCompanyUnlockModalOpen(false)}
              disabled={unlockCompanyMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant="primary"
              isLoading={unlockCompanyMutation.isPending}
              leftIcon={<Unlock className="h-4 w-4" />}
              onClick={handleConfirmCompanyUnlock}
            >
              Xác nhận mở khóa
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
