import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, Check, X } from "lucide-react";
import { toast } from "sonner";
import { hrJobsApi } from "../../api/endpoints/hrJobs.api";
import { staffJobsApi } from "../../api/endpoints/staffJobs.api";
import { JobDetailView } from "../../components/job/JobDetailView";
import { Button } from "../../components/common/Button";
import { Modal } from "../../components/common/Modal";
import { Skeleton } from "../../components/feedback/Skeleton";

export function StaffJobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isRejectOpen, setIsRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState("");

  const { data: response, isLoading, isError, refetch } = useQuery({
    queryKey: ["staff", "jobs", "detail", id],
    queryFn: async () => {
      const res = await hrJobsApi.getJob(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });

  const approveMutation = useMutation({
    mutationFn: () => staffJobsApi.approveJob(id!),
    onSuccess: () => {
      toast.success("Phê duyệt tin tuyển dụng thành công!");
      queryClient.invalidateQueries({ queryKey: ["staff", "jobs"] });
      navigate("/staff/jobs");
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.detail || err?.response?.data?.message || "Không thể phê duyệt tin";
      toast.error(msg);
    },
  });

  const rejectMutation = useMutation({
    mutationFn: (reason: string) => staffJobsApi.rejectJob(id!, reason),
    onSuccess: () => {
      toast.success("Đã từ chối tin tuyển dụng.");
      setIsRejectOpen(false);
      setRejectReason("");
      queryClient.invalidateQueries({ queryKey: ["staff", "jobs"] });
      navigate("/staff/jobs");
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.detail || err?.response?.data?.message || "Không thể từ chối tin";
      toast.error(msg);
    },
  });

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6 py-6">
        <Skeleton className="h-6 w-52" />
        <Skeleton className="h-56 rounded-3xl" />
        <div className="grid gap-6 lg:grid-cols-3"><Skeleton className="h-96 rounded-3xl lg:col-span-2" /><Skeleton className="h-72 rounded-3xl" /></div>
      </div>
    );
  }

  if (isError || !response) {
    return (
      <div className="mx-auto max-w-2xl space-y-4 py-16 text-center">
        <AlertCircle className="mx-auto h-12 w-12 text-danger" />
        <h2 className="text-lg font-semibold text-foreground">Không tìm thấy tin tuyển dụng</h2>
        <p className="text-xs text-muted">Tin không tồn tại hoặc bạn không có quyền xem thông tin này.</p>
        <div className="flex justify-center gap-2">
          <Button variant="outline" size="sm" onClick={() => navigate("/staff/jobs")}>Quay lại</Button>
          <Button variant="primary" size="sm" onClick={() => refetch()}>Thử lại</Button>
        </div>
      </div>
    );
  }

  const openReject = () => {
    setRejectReason("");
    setIsRejectOpen(true);
  };

  return (
    <>
      <JobDetailView
        job={response}
        viewer="staff"
        backTo={{ label: "Quay lại danh sách", onClick: () => navigate("/staff/jobs") }}
        actions={response.status === "PENDING" ? (
          <>
            <Button
              variant="outline"
              size="sm"
              className="border-rose-200 text-danger hover:bg-rose-50"
              leftIcon={<X className="h-4 w-4" />}
              disabled={approveMutation.isPending || rejectMutation.isPending}
              onClick={openReject}
            >
              Từ chối (kèm lý do)
            </Button>
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Check className="h-4 w-4" />}
              isLoading={approveMutation.isPending}
              disabled={rejectMutation.isPending}
              onClick={() => approveMutation.mutate()}
            >
              Phê duyệt tin
            </Button>
          </>
        ) : undefined}
      />

      <Modal
        isOpen={isRejectOpen}
        onClose={() => {
          if (!rejectMutation.isPending) {
            setIsRejectOpen(false);
            setRejectReason("");
          }
        }}
        title="Từ chối tin tuyển dụng"
        description={`Nhập lý do từ chối cho tin: "${response.title}"`}
        footer={
          <div className="flex justify-end gap-2">
            <Button variant="outline" size="sm" disabled={rejectMutation.isPending} onClick={() => { setIsRejectOpen(false); setRejectReason(""); }}>Hủy</Button>
            <Button
              variant="danger"
              size="sm"
              isLoading={rejectMutation.isPending}
              disabled={!rejectReason.trim()}
              onClick={() => rejectMutation.mutate(rejectReason.trim())}
            >
              Xác nhận từ chối
            </Button>
          </div>
        }
      >
        <div className="space-y-2">
          <label className="block text-xs font-semibold text-foreground">Lý do từ chối <span className="text-danger">*</span></label>
          <textarea
            maxLength={1000}
            rows={4}
            className="w-full resize-none rounded-lg border border-border bg-surface p-2.5 text-xs text-foreground outline-none focus:border-danger focus:ring-1 focus:ring-danger/20"
            placeholder="Ví dụ: Mô tả công việc chưa rõ ràng, mức lương chưa phù hợp với cấp bậc..."
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
          <p className="text-right text-[11px] text-muted">{rejectReason.length}/1000</p>
        </div>
      </Modal>
    </>
  );
}

export default StaffJobDetailPage;

