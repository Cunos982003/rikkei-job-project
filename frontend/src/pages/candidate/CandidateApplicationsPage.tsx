import { FileText, Clock3, CheckCircle2 } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";

export function CandidateApplicationsPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Đơn đã ứng tuyển</h1>
        <p className="text-xs text-muted mt-1">Theo dõi trạng thái của các đơn ứng tuyển đã gửi qua hệ thống.</p>
      </div>

      <div className="grid gap-4">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between gap-3">
              <CardTitle>Java Backend Developer</CardTitle>
              <Badge variant="warning"><span className="flex items-center gap-1"><Clock3 className="h-3 w-3" />Đang chờ</span></Badge>
            </div>
            <CardDescription>Rikkeisoft • Ứng tuyển ngày 10/11/2026</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted">Đơn ứng tuyển đã được gửi và đang chờ nhà tuyển dụng xem xét.</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between gap-3">
              <CardTitle>Software Engineer (Fullstack)</CardTitle>
              <Badge variant="verified"><span className="flex items-center gap-1"><CheckCircle2 className="h-3 w-3" />Đã phỏng vấn</span></Badge>
            </div>
            <CardDescription>FPT Software • Ứng tuyển ngày 02/11/2026</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted">Đã được mời phỏng vấn vòng 1. Vui lòng kiểm tra email và lịch phỏng vấn.</p>
          </CardContent>
        </Card>

        <div className="rounded-xl border border-dashed border-border bg-slate-50 p-4">
          <div className="flex items-center gap-2 text-sm text-muted">
            <FileText className="h-4 w-4" />
            <span>Trang này chỉ là màn hình tổng quan tối thiểu theo cấu trúc dự án.</span>
          </div>
        </div>
      </div>
    </div>
  );
}
