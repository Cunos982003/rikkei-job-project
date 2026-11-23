import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Plus,
  Briefcase,
  AlertCircle,
  RefreshCw,
  Eye,
  Lock,
  ChevronLeft,
  ChevronRight,
  Filter,
  FileText,
} from "lucide-react";
import { toast } from "sonner";
import { hrJobsApi, type JobListItem } from "../../api/endpoints/hrJobs.api";
import { Button } from "../../components/common/Button";
import { Card, CardContent } from "../../components/common/Card";
import { Modal } from "../../components/common/Modal";
import { JobStatusBadge } from "../../components/hr/JobStatusBadge";
import { JobDetailModal } from "../../components/hr/JobDetailModal";
import { LEVEL_LABELS, TYPE_LABELS } from "../../utils/jobLabels";
import { cn } from "../../utils/cn";

export function HrJobsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [selectedStatus, setSelectedStatus] = useState<string>("ALL");
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  // Modal xem chi tiết tin
  const [viewingJobId, setViewingJobId] = useState<string | null>(null);

  const { data: viewingJobResponse } = useQuery({
    queryKey: ["hr", "jobs", "detail", viewingJobId],
    queryFn: async () => {
      if (!viewingJobId) return null;
      const res = await hrJobsApi.getJob(viewingJobId);
      return res.data;
    },
    enabled: !!viewingJobId,
  });

  // Modal đóng tin
  const [closingJob, setClosingJob] = useState<JobListItem | null>(null);

  const {
    data: response,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["hr", "jobs", { status: selectedStatus, page: currentPage, size: pageSize }],
    queryFn: async () => {
      const res = await hrJobsApi.getMyJobs({
        status: selectedStatus === "ALL" ? undefined : selectedStatus,
        page: currentPage,
        size: pageSize,
      });
      return res.data;
    },
  });

  const jobs: JobListItem[] = response?.items ?? [];
  const pagination = response?.pagination;
  const totalElements = pagination?.totalElements ?? 0;
  const totalPages = pagination?.totalPages ?? 0;

  // Mutation đóng tin
  const closeMutation = useMutation({
    mutationFn: async (id: string) => {
      return await hrJobsApi.closeJob(id);
    },
    onSuccess: () => {
      toast.success("Đã đóng tin tuyển dụng thành công");
      setClosingJob(null);
      queryClient.invalidateQueries({ queryKey: ["hr", "jobs"] });
    },
  });

  const handleConfirmClose = () => {
    if (closingJob) {
      closeMutation.mutate(closingJob.id);
    }
  };

  const statusOptions: { value: string; label: string }[] = [
    { value: "ALL", label: "Tất cả trạng thái" },
    { value: "DRAFT", label: "Bản nháp" },
    { value: "PENDING", label: "Chờ duyệt" },
    { value: "APPROVED", label: "Đã duyệt" },
    { value: "REJECTED", label: "Bị trả lại" },
    { value: "CLOSED", label: "Đã đóng" },
  ];

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Tiêu đề trang */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Tin Tuyển Dụng</h1>
          <p className="mt-1 text-sm text-muted">
            Quản lý các tin tuyển dụng (JD) và kỹ năng yêu cầu của doanh nghiệp.
          </p>
        </div>

        <Button
          variant="primary"
          leftIcon={<Plus className="h-4 w-4" />}
          onClick={() => navigate("/hr/jobs/new")}
        >
          Tạo tin mới
        </Button>
      </div>

      {/* Thanh lọc trạng thái */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        <Filter className="h-4 w-4 text-muted shrink-0 ml-1 mr-1" />
        {statusOptions.map((opt) => (
          <button
            key={opt.value}
            type="button"
            onClick={() => {
              setSelectedStatus(opt.value);
              setCurrentPage(0);
            }}
            className={cn(
              "px-3 py-1.5 text-xs font-semibold rounded-lg border transition-all whitespace-nowrap cursor-pointer",
              selectedStatus === opt.value
                ? "bg-primary text-white border-primary shadow-xs"
                : "bg-surface text-muted hover:text-foreground border-border hover:bg-slate-50"
            )}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {/* Nội dung danh sách */}
      {isLoading ? (
        <Card>
          <CardContent className="py-12 text-center text-sm text-muted">
            Đang tải danh sách tin tuyển dụng...
          </CardContent>
        </Card>
      ) : isError ? (
        <div className="p-4 rounded-xl border border-rose-200 bg-rose-50 flex items-center justify-between gap-3 text-danger text-sm">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-5 w-5 shrink-0" />
            <span>
              Lỗi khi tải dữ liệu:{" "}
              {error instanceof Error ? error.message : "Vui lòng kiểm tra lại kết nối"}
            </span>
          </div>
          <Button
            variant="outline"
            size="sm"
            leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
            onClick={() => refetch()}
          >
            Thử lại
          </Button>
        </div>
      ) : jobs.length === 0 ? (
        <Card>
          <CardContent className="py-16 text-center space-y-3">
            <Briefcase className="h-10 w-10 text-muted mx-auto" />
            <h3 className="text-base font-semibold text-foreground">
              Không có tin tuyển dụng nào
            </h3>
            <p className="text-sm text-muted max-w-md mx-auto">
              {selectedStatus === "ALL"
                ? "Doanh nghiệp của bạn chưa tạo tin tuyển dụng nào. Hãy bắt đầu tạo tin ngay hôm nay!"
                : "Không tìm thấy tin tuyển dụng nào phù hợp với bộ lọc đã chọn."}
            </p>
            {selectedStatus === "ALL" && (
              <Button
                variant="primary"
                size="sm"
                leftIcon={<Plus className="h-4 w-4" />}
                onClick={() => navigate("/hr/jobs/new")}
                className="mt-2"
              >
                Tạo tin tuyển dụng đầu tiên
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-border bg-slate-50 text-xs font-semibold text-muted uppercase">
                <tr>
                  <th className="px-5 py-3.5">Tiêu đề tin</th>
                  <th className="px-4 py-3.5">Vị trí</th>
                  <th className="px-4 py-3.5">Cấp bậc</th>
                  <th className="px-4 py-3.5">Hình thức</th>
                  <th className="px-4 py-3.5">Hạn nộp</th>
                  <th className="px-4 py-3.5">Trạng thái</th>
                  <th className="px-4 py-3.5">Cập nhật</th>
                  <th className="px-5 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {jobs.map((job) => (
                  <tr key={job.id} className="hover:bg-slate-50/60 transition-colors">
                    <td
                      className="px-5 py-4 font-semibold text-foreground max-w-xs truncate cursor-pointer hover:text-primary transition-colors"
                      onClick={() => setViewingJobId(job.id)}
                    >
                      {job.title}
                    </td>
                    <td className="px-4 py-4 text-muted">{job.position}</td>
                    <td className="px-4 py-4 text-muted">
                      {LEVEL_LABELS[job.level] || job.level}
                    </td>
                    <td className="px-4 py-4 text-muted">
                      {TYPE_LABELS[job.type] || job.type}
                    </td>
                    <td className="px-4 py-4 text-muted font-mono text-xs">
                      {job.deadline}
                    </td>
                    <td className="px-4 py-4">
                      <JobStatusBadge status={job.status} />
                    </td>
                    <td className="px-4 py-4 text-muted text-xs">
                      {job.updatedAt ? new Date(job.updatedAt).toLocaleDateString("vi-VN") : "—"}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<FileText className="h-3.5 w-3.5" />}
                          onClick={() => setViewingJobId(job.id)}
                        >
                          Chi tiết
                        </Button>

                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="h-3.5 w-3.5" />}
                          onClick={() => navigate(`/hr/jobs/${job.id}/edit`)}
                        >
                          {job.status === "PENDING" || job.status === "CLOSED" ? "Xem" : "Sửa"}
                        </Button>

                        {job.status !== "CLOSED" && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-danger hover:bg-rose-50 hover:text-danger"
                            leftIcon={<Lock className="h-3.5 w-3.5" />}
                            onClick={() => setClosingJob(job)}
                          >
                            Đóng
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Phân trang */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-border px-5 py-3 text-xs text-muted">
              <div>
                Tổng số <strong>{totalElements}</strong> tin tuyển dụng
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  leftIcon={<ChevronLeft className="h-4 w-4" />}
                >
                  Trước
                </Button>
                <span>
                  Trang <strong>{currentPage + 1}</strong> / {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => setCurrentPage((p) => p + 1)}
                  rightIcon={<ChevronRight className="h-4 w-4" />}
                >
                  Sau
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* Modal chi tiết tin tuyển dụng */}
      <JobDetailModal
        job={viewingJobResponse ?? null}
        onClose={() => setViewingJobId(null)}
        footer={
          <div className="flex items-center justify-end gap-2 w-full">
            <Button variant="outline" size="sm" onClick={() => setViewingJobId(null)}>
              Đóng
            </Button>
            {viewingJobResponse &&
              viewingJobResponse.status !== "PENDING" &&
              viewingJobResponse.status !== "CLOSED" && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    const id = viewingJobResponse.id;
                    setViewingJobId(null);
                    navigate(`/hr/jobs/${id}/edit`);
                  }}
                >
                  Sửa tin
                </Button>
              )}
          </div>
        }
      />

      {/* Modal xác nhận đóng tin */}
      <Modal
        isOpen={closingJob !== null}
        onClose={() => setClosingJob(null)}
        title="Xác nhận đóng tin tuyển dụng"
        description="Bạn có chắc chắn muốn đóng tin tuyển dụng này không?"
        footer={
          <div className="flex items-center justify-end gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setClosingJob(null)}
              disabled={closeMutation.isPending}
            >
              Hủy
            </Button>
            <Button
              variant="danger"
              size="sm"
              isLoading={closeMutation.isPending}
              onClick={handleConfirmClose}
            >
              Xác nhận đóng tin
            </Button>
          </div>
        }
      >
        <p className="text-sm text-foreground">
          Tin tuyển dụng: <strong>{closingJob?.title}</strong>
        </p>
        <p className="mt-2 text-xs text-muted">
          Sau khi đóng, tin sẽ không còn hiển thị cho ứng viên ứng tuyển và không thể chỉnh sửa lại.
        </p>
      </Modal>
    </div>
  );
}
