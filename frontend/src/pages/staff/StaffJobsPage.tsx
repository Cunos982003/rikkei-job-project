import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Modal } from "../../components/common/Modal";
import { CheckSquare, Check, X, RefreshCw, AlertCircle, Briefcase, MapPin, Calendar } from "lucide-react";
import { staffJobsApi } from "../../api/endpoints/staffJobs.api";
import type { JobResponse } from "../../api/endpoints/hrJobs.api";

function formatSalary(min?: number | null, max?: number | null): string {
  if (min == null && max == null) return "Thoả thuận";
  if (min != null && max != null) {
    return `${min.toLocaleString("vi-VN")} - ${max.toLocaleString("vi-VN")} đ`;
  }
  if (min != null) return `Từ ${min.toLocaleString("vi-VN")} đ`;
  return `Lên đến ${max!.toLocaleString("vi-VN")} đ`;
}

export function StaffJobsPage() {
  const queryClient = useQueryClient();

  // Modal từ chối
  const [rejectingJob, setRejectingJob] = useState<JobResponse | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  // Gọi API lấy danh sách tin PENDING
  const {
    data: jobsResponse,
    isLoading,
    isError,
    refetch,
    isFetching,
  } = useQuery({
    queryKey: ["staff", "jobs", "PENDING"],
    queryFn: async () => {
      const res = await staffJobsApi.getStaffJobs({ status: "PENDING", page: 0, size: 50 });
      return res.data;
    },
    refetchOnWindowFocus: true,
  });

  const pendingJobs: JobResponse[] = jobsResponse?.items || [];

  // Mutation phê duyệt
  const approveMutation = useMutation({
    mutationFn: (jobId: string) => staffJobsApi.approveJob(jobId),
    onSuccess: () => {
      toast.success("Phê duyệt tin tuyển dụng thành công!");
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Kiểm Duyệt Tin Tuyển Dụng & Vận Hành</h1>
          <p className="text-xs text-muted mt-1">
            Phân hệ dành cho Cán bộ tuyển sinh & hướng nghiệp (STAFF)
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

      {/* Danh sách tin tuyển dụng chờ duyệt */}
      <div className="space-y-4">
        <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
          <CheckSquare className="h-5 w-5 text-warning" />
          <span>Tin tuyển dụng (JD) đang chờ duyệt ({pendingJobs.length} tin)</span>
        </h2>

        {isLoading ? (
          <div className="py-12 text-center text-sm text-muted">
            <RefreshCw className="h-5 w-5 animate-spin mx-auto mb-2 text-primary" />
            Đang tải danh sách tin chờ duyệt...
          </div>
        ) : isError ? (
          <Card>
            <CardContent className="py-8 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-danger mx-auto" />
              <p className="text-sm text-danger font-medium">Không thể tải danh sách tin tuyển dụng.</p>
              <Button size="sm" variant="outline" onClick={() => refetch()}>
                Thử lại
              </Button>
            </CardContent>
          </Card>
        ) : pendingJobs.length === 0 ? (
          <Card>
            <CardContent className="py-12 text-center text-sm text-muted">
              <Briefcase className="h-8 w-8 mx-auto mb-2 text-muted/50" />
              Hiện không có tin tuyển dụng nào đang chờ duyệt.
            </CardContent>
          </Card>
        ) : (
          pendingJobs.map((job) => {
            const requiredSkills = (job.skills || []).filter((s) => s.required);
            const preferredSkills = (job.skills || []).filter((s) => !s.required);

            return (
              <Card key={job.id} className="transition-all hover:border-primary/40">
                <CardHeader>
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <CardTitle className="text-base">{job.title}</CardTitle>
                      <CardDescription className="flex items-center gap-2 mt-1">
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
                    <Badge variant="warning">Chờ duyệt</Badge>
                  </div>
                </CardHeader>

                <CardContent className="space-y-4">
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
                            {s.skillName || s.skillCode} (≥{s.minScore}đ)
                          </Badge>
                        ))}
                      </div>
                    )}

                    {preferredSkills.length > 0 && (
                      <div className="flex flex-wrap items-center gap-1.5 pt-0.5">
                        <span className="font-medium text-muted">Kỹ năng ưu tiên:</span>
                        {preferredSkills.map((s) => (
                          <Badge key={s.skillId} variant="outline" size="sm">
                            {s.skillName || s.skillCode} (≥{s.minScore}đ)
                          </Badge>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="flex items-center justify-end gap-2 pt-2 border-t border-border-subtle">
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
            className="w-full p-2.5 text-xs rounded-lg border border-border bg-surface text-foreground outline-none focus:border-danger focus:ring-1 focus:ring-danger/20 resize-none h-24"
            placeholder="Ví dụ: Mô tả công việc chưa rõ ràng, mức lương chưa phù hợp với cấp bậc..."
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </div>
      </Modal>
    </div>
  );
}
