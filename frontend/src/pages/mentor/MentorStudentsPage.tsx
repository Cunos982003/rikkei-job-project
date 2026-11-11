import { useAuthStore } from "../../store/useAuthStore";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Users, ClipboardCheck, ArrowRight } from "lucide-react";

export function MentorStudentsPage() {
  const { user } = useAuthStore();

  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Thực tập sinh IOC & Đánh giá năng lực</h1>
        <p className="text-xs text-muted mt-1">
          Mentor: <strong className="text-foreground">{user?.fullName}</strong> • Quản lý học viên thực tập và điền phiếu đánh giá năng lực
        </p>
      </div>

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <Card hoverable>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Lê Hoàng Cường</CardTitle>
              <Badge variant="self">Thực tập sinh IOC</Badge>
            </div>
            <CardDescription>Đề tài: Hệ thống Quản trị Tuyển dụng Rikkei Jobs</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1 text-xs">
              <div className="flex justify-between">
                <span className="text-muted">Tiến độ task nghiệm thu:</span>
                <span className="font-semibold text-foreground">8/10 task (80%)</span>
              </div>
              <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full bg-primary rounded-full" style={{ width: "80%" }} />
              </div>
            </div>

            <Button variant="primary" size="sm" className="w-full" leftIcon={<ClipboardCheck className="h-4 w-4" />}>
              Điền phiếu đánh giá Mentor
            </Button>
          </CardContent>
        </Card>

        <Card hoverable>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Nguyễn Văn An</CardTitle>
              <Badge variant="verified">Đã nghiệm thu</Badge>
            </div>
            <CardDescription>Đề tài: Tối ưu kiến trúc Microservices & CSDL</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-1 text-xs">
              <div className="flex justify-between">
                <span className="text-muted">Tiến độ task nghiệm thu:</span>
                <span className="font-semibold text-success">10/10 task (100%)</span>
              </div>
              <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full bg-success rounded-full" style={{ width: "100%" }} />
              </div>
            </div>

            <Button variant="outline" size="sm" className="w-full" rightIcon={<ArrowRight className="h-4 w-4" />}>
              Xem lại phiếu đã gửi
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="h-5 w-5 text-muted" />
              <span>Thống kê đợt IOC</span>
            </CardTitle>
            <CardDescription>Tổng quan nhiệm vụ hướng dẫn</CardDescription>
          </CardHeader>
          <CardContent className="space-y-2 text-xs">
            <div className="flex justify-between py-1 border-b border-border-subtle">
              <span className="text-muted">Tổng số thực tập sinh:</span>
              <strong className="text-foreground">2 học viên</strong>
            </div>
            <div className="flex justify-between py-1 border-b border-border-subtle">
              <span className="text-muted">Phiếu đánh giá chờ hoàn thành:</span>
              <strong className="text-warning">1 phiếu</strong>
            </div>
            <div className="flex justify-between py-1">
              <span className="text-muted">Phiếu đã gửi (không thể sửa):</span>
              <strong className="text-success">1 phiếu</strong>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

