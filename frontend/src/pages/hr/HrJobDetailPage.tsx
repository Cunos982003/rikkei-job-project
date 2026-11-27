import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertCircle, Lock, Pencil, Eye } from "lucide-react";
import { toast } from "sonner";
import { hrJobsApi } from "../../api/endpoints/hrJobs.api";
import { JobDetailView } from "../../components/job/JobDetailView";
import { Button } from "../../components/common/Button";
import { Modal } from "../../components/common/Modal";
import { Skeleton } from "../../components/feedback/Skeleton";

export function HrJobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isCloseOpen, setIsCloseOpen] = useState(false);

  const { data: response, isLoading, isError, refetch } = useQuery({
    queryKey: ["hr", "jobs", "detail", id],
    queryFn: async () => {
      const res = await hrJobsApi.getJob(id!);
      return res.data;
    },
    enabled: Boolean(id),
  });

  const closeMutation = useMutation({
    mutationFn: () => hrJobsApi.closeJob(id!),
    onSuccess: () => {
      toast.success("Đã đóng tin tuyển dụng thành công");
      setIsCloseOpen(false);
      queryClient.invalidateQueries({ queryKey: ["hr", "jobs"] });
      refetch();
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
          <Button variant="outline" size="sm" onClick={() => navigate("/hr/jobs")}>Quay lại</Button>
          <Button variant="primary" size="sm" onClick={() => refetch()}>Thử lại</Button>
        </div>
      </div>
    );
  }

  const canEdit = response.status !== "PENDING" && response.status !== "CLOSED";

  return (
    <>
      <JobDetailView
        job={response}
        viewer="hr"
        backTo={{ label: "Quay lại danh sách", onClick: () => navigate("/hr/jobs") }}
        actions={
          <>
            <Button
              variant="outline"
              size="sm"
              leftIcon={canEdit ? <Pencil className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              onClick={() => navigate(`/hr/jobs/${id}/edit`)}
            >
              {canEdit ? "Sửa tin" : "Xem biểu mẫu"}
            </Button>
            {response.status !== "CLOSED" && (
              <Button
                variant="danger"
                size="sm"
                className="border border-rose-300 bg-surface text-danger hover:bg-rose-50"
                leftIcon={<Lock className="h-4 w-4" />}
                onClick={() => setIsCloseOpen(true)}
              >
                Đóng tin
              </Button>
            )}
          </>
        }
      />

      <Modal
        isOpen={isCloseOpen}
        onClose={() => setIsCloseOpen(false)}
        title="Xác nhận đóng tin tuyển dụng"
        description="Bạn có chắc chắn muốn đóng tin tuyển dụng này không?"
        footer={
          <div className="flex justify-end gap-2">
            <Button variant="outline" size="sm" disabled={closeMutation.isPending} onClick={() => setIsCloseOpen(false)}>Hủy</Button>
            <Button variant="danger" size="sm" isLoading={closeMutation.isPending} onClick={() => closeMutation.mutate()}>Xác nhận đóng tin</Button>
          </div>
        }
      >
        <p className="text-sm text-foreground">Tin tuyển dụng: <strong>{response.title}</strong></p>
        <p className="mt-2 text-xs text-muted">Sau khi đóng, tin sẽ không còn hiển thị cho ứng viên ứng tuyển.</p>
      </Modal>
    </>
  );
}

export default HrJobDetailPage;

