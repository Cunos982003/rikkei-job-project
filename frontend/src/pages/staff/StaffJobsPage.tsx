import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Modal } from "../../components/common/Modal";
import {
  CheckSquare,
  Check,
  X,
  RefreshCw,
  AlertCircle,
  Briefcase,
  MapPin,
  Calendar,
  Eye,
  UserCheck,
  Clock,
  AlertTriangle,
} from "lucide-react";
import { staffJobsApi } from "../../api/endpoints/staffJobs.api";
import { staffCompaniesApi } from "../../api/endpoints/staffCompanies.api";
import type { JobResponse } from "../../api/endpoints/hrJobs.api";
import { useNavigate } from "react-router-dom";

function formatSalary(min?: number | null, max?: number | null): string {
  if (min == null && max == null) return "Thoả thuận";
  if (min != null && max != null) {
    return `${min.toLocaleString("vi-VN")} - ${max.toLocaleString("vi-VN")} đ`;
  }
  if (min != null) return `Từ ${min.toLocaleString("vi-VN")} đ`;
  return `Lên đến ${max!.toLocaleString("vi-VN")} đ`;
}

function formatDateTime(value?: string | null): string {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

type StaffJobTab = "PENDING" | "APPROVED" | "REJECTED";

export function StaffJobsPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Tabs trạng thái: PENDING (mặc định), APPROVED, REJECTED
  const [activeTab, setActiveTab] = useState<StaffJobTab>("PENDING");

  // Lọc theo doanh nghiệp
  const [selectedCompanyId, setSelectedCompanyId] = useState("");

  // Modal từ chối
  const [rejectingJob, setRejectingJob] = useState<JobResponse | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  // Query danh sách doanh nghiệp cho dropdown lọc
  const { data: companiesData } = useQuery({
    queryKey: ["staff", "companies", "dropdown"],
    queryFn: async () => {
      const res = await staffCompaniesApi.list({ page: 0, size: 100 });
      return res.data?.items || [];
    },
  });

  // Gọi API lấy danh sách tin theo status và companyId
  const {
    data: jobsResponse,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ["staff", "jobs", activeTab, selectedCompanyId],
    queryFn: async () => {
      const res = await staffJobsApi.getStaffJobs({
        status: activeTab,
        companyId: selectedCompanyId || undefined,
        page: 0,
        size: 50,
      });
      return res.data;
    },
    refetchOnWindowFocus: true,
  });

  const jobs: JobResponse[] = jobsResponse?.items || [];

  // Mutation phê duyệt
  const approveMutation = useMutation({
    mutationFn: (jobId: string) => staffJobsApi.approveJob(jobId),
    onSuccess: () => {
      toast.success("Phê duyệt tin tuyển dụng thành công!");
      setRejectingJob(null);
      queryClient.invalidateQueries({ queryKey: ["staff", "jobs"] });
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.detail || err?.response?.data?.message || "Không thể phê duyệt tin";
      toast.error(msg);
    },
  });

  // Mutation từ chối
  const rejectMutation = useMutation({
    mutationFn: ({ jobId, reason }: { jobId: string; reason: string }) =>
      staffJobsApi.rejectJob(jobId, reason),
    onSuccess: () => {
      toast.success("Đã từ chối tin tuyển dụng.");
      setRejectingJob(null);
      setRejectReason("");
      queryClient.invalidateQueries({ queryKey: ["staff", "jobs"] });
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.detail || err?.response?.data?.message || "Không thể từ chối tin";
      toast.error(msg);
    },
  });

  const handleApprove = (job: JobResponse) => {
    approveMutation.mutate(job.id);
  };

  const handleOpenReject = (job: JobResponse) => {
    setRejectingJob(job);
    setRejectReason("");
  };

  const handleConfirmReject = () => {
    if (!rejectingJob) return;
    if (!rejectReason.trim()) {
      toast.error("Vui lòng nhập lý do từ chối");
      return;
    }
    rejectMutation.mutate({ jobId: rejectingJob.id, reason: rejectReason.trim() });
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "PENDING":
        return <Badge variant="warning"><span>Chờ duyệt</span></Badge>;
      case "APPROVED":
        return <Badge variant="verified"><span>Đã duyệt</span></Badge>;
      case "REJECTED":
        return <Badge variant="danger"><span>Đã trả lại</span></Badge>;
      default:
        return <Badge variant="outline"><span>{status}</span></Badge>;
    }
  };

  return (
    <div className="space-y-6 notranslate" translate="no">
      {/* Header */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">
            <span>Kiểm Duyệt Tin Tuyển Dụng & Vận Hành</span>
          </h1>
          <p className="text-xs text-muted mt-1">
            <span>Phân hệ dành cho Cán bộ tuyển sinh & hướng nghiệp (STAFF)</span>
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<RefreshCw className={`h-4 w-4 ${isFetching ? "animate-spin" : ""}`} />}
            onClick={() => refetch()}
          >
            Làm mới
          </Button>
        </div>
      </div>

      {/* Tabs & Filter Bar (Giao diện chuẩn nhóm thống nhất) */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-surface p-3 rounded-xl border border-border">
        {/* Tabs */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 dark:bg-slate-800 rounded-lg w-fit">
          <button
            type="button"
            onClick={() => setActiveTab("PENDING")}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all cursor-pointer ${
              activeTab === "PENDING"
                ? "bg-surface text-primary shadow-xs font-semibold"
                : "text-muted hover:text-foreground"
            }`}
          >
            Chờ duyệt
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("APPROVED")}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all cursor-pointer ${
              activeTab === "APPROVED"
                ? "bg-surface text-primary shadow-xs font-semibold"
                : "text-muted hover:text-foreground"
            }`}
          >
            Đã duyệt
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("REJECTED")}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all cursor-pointer ${
              activeTab === "REJECTED"
                ? "bg-surface text-primary shadow-xs font-semibold"
                : "text-muted hover:text-foreground"
            }`}
          >
            Đã trả lại
          </button>
        </div>

        {/* Bộ lọc theo doanh nghiệp */}
        <div className="flex items-center gap-2">
          <label htmlFor="company-filter" className="text-xs font-medium text-muted whitespace-nowrap">
            Doanh nghiệp:
          </label>
          <select
            id="company-filter"
            value={selectedCompanyId}
            onChange={(e) => setSelectedCompanyId(e.target.value)}
            className="px-3 py-1.5 text-xs rounded-lg border border-border bg-surface text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary/20 min-w-[220px]"
          >
            <option value="">Tất cả doanh nghiệp</option>
            {(companiesData || []).map((comp) => (
              <option key={comp.id} value={comp.id}>
                {comp.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Danh sách tin tuyển dụng */}
      <div className="space-y-4">
        <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
          <CheckSquare className="h-5 w-5 text-warning" />
          <span>
            {activeTab === "PENDING"
              ? `Tin tuyển dụng (JD) đang chờ duyệt (${jobs.length} tin)`
              : activeTab === "APPROVED"
              ? `Tin tuyển dụng đã duyệt (${jobs.length} tin)`
              : `Tin tuyển dụng đã từ chối (${jobs.length} tin)`}
          </span>
        </h2>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted">
            <RefreshCw className="h-5 w-5 animate-spin mx-auto mb-2 text-primary" />
            <span>Đang tải danh sách tin...</span>
          </div>
        ) : isError ? (
          <Card>
            <CardContent className="py-8 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-danger mx-auto" />
              <p className="text-sm text-danger font-medium">
                <span>Không thể tải danh sách tin tuyển dụng.</span>
              </p>
              <Button size="sm" variant="outline" onClick={() => refetch()}>
                Thử lại
              </Button>
            </CardContent>
          </Card>
        ) : jobs.length === 0 ? (
          <Card>
            <CardContent className="py-12 text-center text-sm text-muted">
              <Briefcase className="h-8 w-8 mx-auto mb-2 text-muted/50" />
              <span>
                {activeTab === "PENDING"
                  ? "Hiện không có tin tuyển dụng nào đang chờ duyệt."
                  : activeTab === "APPROVED"
                  ? "Chưa có tin tuyển dụng nào được duyệt."
                  : "Chưa có tin tuyển dụng nào bị từ chối."}
              </span>
            </CardContent>
          </Card>
        ) : (
          jobs.map((job) => {
            const requiredSkills = (job.skills || []).filter((s) => s.required);
            const preferredSkills = (job.skills || []).filter((s) => !s.required);

            return (
              <Card key={job.id} className="transition-all hover:border-primary/40">
                <CardHeader>
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <CardTitle
                        className="text-base cursor-pointer hover:text-primary transition-colors"
                        onClick={() => navigate(`/staff/jobs/${job.id}`)}
                      >
                        <span>{job.title}</span>
                      </CardTitle>
                      <CardDescription className="flex flex-wrap items-center gap-2 mt-1">
                        <span>Doanh nghiệp: <strong>{job.companyName || "Chưa cập nhật"}</strong></span>
                        {job.city && (
                          <span className="flex items-center gap-1">
                            • <MapPin className="h-3 w-3" /> {job.city}
                          </span>
                        )}
                        {job.deadline && (
                          <span className="flex items-center gap-1 text-muted">
                            • <Calendar className="h-3 w-3" /> Hạn: {job.deadline}
                          </span>
                        )}
                      </CardDescription>
                    </div>
                    <div>{getStatusBadge(job.status)}</div>
                  </div>
                </CardHeader>

                <CardContent className="space-y-4">
                  {/* Lý do từ chối nếu ở tab REJECTED hoặc tin có lý do từ chối */}
                  {(activeTab === "REJECTED" || job.status === "REJECTED") && job.rejectedReason && (
                    <div className="flex items-start gap-2 p-3 rounded-lg bg-danger-light border border-danger/20 text-xs text-danger">
                      <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
                      <div>
                        <span className="font-semibold">Lý do trả lại: </span>
                        <span>{job.rejectedReason}</span>
                      </div>
                    </div>
                  )}

                  <div className="text-xs text-muted leading-relaxed space-y-1.5">
                    <p>
                      Mức lương: <strong>{formatSalary(Number(job.salaryMin), Number(job.salaryMax))}</strong>
                      {job.quantity && (
                        <span> • Số lượng: <strong>{job.quantity} người</strong></span>
                      )}
                      {job.level && (
                        <span> • Cấp bậc: <strong>{job.level}</strong></span>
                      )}
                      {job.workMode && (
                        <span> • Chế độ: <strong>{job.workMode}</strong></span>
                      )}
                    </p>

                    {requiredSkills.length > 0 && (
                      <div className="flex flex-wrap items-center gap-1.5 pt-1">
                        <span className="font-semibold text-foreground">Kỹ năng bắt buộc:</span>
                        {requiredSkills.map((s) => (
                          <Badge key={s.skillId} variant="primary" size="sm">
                            <span>{s.skillName || s.skillCode} (≥{s.minScore}đ)</span>
                          </Badge>
                        ))}
                      </div>
                    )}

                    {preferredSkills.length > 0 && (
                      <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                        <span className="font-medium text-muted">Kỹ năng ưu tiên:</span>
                        {preferredSkills.map((s) => (
                          <Badge key={s.skillId} variant="outline" size="sm">
                            <span>{s.skillName || s.skillCode} (≥{s.minScore}đ)</span>
                          </Badge>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Footer của card: Người duyệt & thời điểm bên trái 1 dòng, nút hành động bên phải */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-3 border-t border-border-subtle">
                    {activeTab !== "PENDING" || job.status !== "PENDING" || job.reviewedBy ? (
                      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted">
                        {job.reviewedBy && (
                          <span className="flex items-center gap-1.5 text-foreground font-medium">
                            <UserCheck className="h-3.5 w-3.5 text-primary" />
                            <span>Người kiểm duyệt: <strong>{job.reviewedBy}</strong></span>
                          </span>
                        )}
                        {job.reviewedAt && (
                          <>
                            {job.reviewedBy && <span className="text-muted/60 hidden sm:inline">•</span>}
                            <span className="flex items-center gap-1.5">
                              <Clock className="h-3.5 w-3.5" />
                              <span>Thời gian duyệt: {formatDateTime(job.reviewedAt)}</span>
                            </span>
                          </>
                        )}
                      </div>
                    ) : (
                      <div />
                    )}

                    <div className="flex items-center gap-2 shrink-0">
                      <Button
                        variant="outline"
                        size="sm"
                        leftIcon={<Eye className="h-4 w-4" />}
                        onClick={() => navigate(`/staff/jobs/${job.id}`)}
                      >
                        Xem chi tiết
                      </Button>

                      {job.status === "PENDING" && (
                        <>
                          <Button
                            variant="outline"
                            size="sm"
                            className="text-danger border-rose-200 hover:bg-rose-50"
                            leftIcon={<X className="h-4 w-4" />}
                            disabled={approveMutation.isPending || rejectMutation.isPending}
                            onClick={() => handleOpenReject(job)}
                          >
                            Từ chối (kèm lý do)
                          </Button>
                          <Button
                            variant="primary"
                            size="sm"
                            leftIcon={<Check className="h-4 w-4" />}
                            disabled={approveMutation.isPending || rejectMutation.isPending}
                            onClick={() => handleApprove(job)}
                          >
                            {approveMutation.isPending ? "Đang xử lý..." : "Phê duyệt tin JD"}
                          </Button>
                        </>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })
        )}
      </div>

      {/* Modal từ chối kèm lý do */}
      <Modal
        isOpen={Boolean(rejectingJob)}
        onClose={() => {
          if (!rejectMutation.isPending) {
            setRejectingJob(null);
            setRejectReason("");
          }
        }}
        title="Từ chối tin tuyển dụng"
        description={`Nhập lý do từ chối cho tin: "${rejectingJob?.title}"`}
        footer={
          <div className="flex items-center justify-end gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={rejectMutation.isPending}
              onClick={() => {
                setRejectingJob(null);
                setRejectReason("");
              }}
            >
              Hủy
            </Button>
            <Button
              variant="danger"
              size="sm"
              disabled={rejectMutation.isPending || !rejectReason.trim()}
              onClick={handleConfirmReject}
            >
              {rejectMutation.isPending ? "Đang gửi..." : "Xác nhận từ chối"}
            </Button>
          </div>
        }
      >
        <div className="space-y-3">
          <label className="block text-xs font-semibold text-foreground">
            Lý do từ chối <span className="text-danger">*</span>
          </label>
          <textarea
            maxLength={1000}
            className="w-full p-2.5 text-xs rounded-lg border border-border bg-surface text-foreground outline-none focus:border-danger focus:ring-1 focus:ring-danger/20 resize-none h-24"
            placeholder="Ví dụ: Mô tả công việc chưa rõ ràng, mức lương chưa phù hợp với cấp bậc..."
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
          <p className="text-right text-[11px] text-muted">{rejectReason.length}/1000</p>
        </div>
      </Modal>
    </div>
  );
}