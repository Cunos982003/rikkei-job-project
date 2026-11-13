import { ClipboardCheck, ListTodo, AlertCircle } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";

export function MentorEvaluationsPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Phiếu đánh giá cần hoàn thành</h1>
        <p className="text-xs text-muted mt-1">Danh sách các phiếu đánh giá thực tập sinh đang chờ Mentor gửi phản hồi.</p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <ListTodo className="h-5 w-5 text-primary" />
              <CardTitle>Phiếu đánh giá – Lê Hoàng Cường</CardTitle>
            </div>
            <Badge variant="warning">Chờ đánh giá</Badge>
          </div>
          <CardDescription>Đề tài: Hệ thống Quản trị Tuyển dụng Rikkei Jobs</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center gap-2 text-sm text-muted">
            <AlertCircle className="h-4 w-4 text-warning" />
            <span>Phiếu này cần Mentor xác nhận tiến độ, kỹ năng phát triển và kết quả nghiệm thu.</span>
          </div>
        </CardContent>
      </Card>

      <div className="rounded-xl border border-dashed border-border bg-slate-50 p-4">
        <div className="flex items-center gap-2 text-sm text-muted">
          <ClipboardCheck className="h-4 w-4" />
          <span>Trang tổng hợp phiếu đánh giá nhằm tái sử dụng đúng chức năng quản lý Mentor hiện có.</span>
        </div>
      </div>
    </div>
  );
}
