import { BarChart3, TrendingUp } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";

export function StaffReportsPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Báo cáo vận hành</h1>
        <p className="text-xs text-muted mt-1">Tổng hợp số liệu tuyển dụng, hồ sơ, và kết quả đánh giá theo kỳ vận hành.</p>
      </div>

      <div className="grid sm:grid-cols-3 gap-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><BarChart3 className="h-5 w-5 text-primary" />Tin tuyển dụng</CardTitle>
            <CardDescription>24 tin đang hoạt động</CardDescription>
          </CardHeader>
          <CardContent>
            <Badge variant="verified">+12% so với tuần trước</Badge>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><TrendingUp className="h-5 w-5 text-success" />Hồ sơ ứng viên</CardTitle>
            <CardDescription>184 hồ sơ đã được xử lý</CardDescription>
          </CardHeader>
          <CardContent>
            <Badge variant="self">97% trong tầm kiểm duyệt</Badge>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><BarChart3 className="h-5 w-5 text-warning" />Đợt IOC</CardTitle>
            <CardDescription>8 nhóm đang triển khai</CardDescription>
          </CardHeader>
          <CardContent>
            <Badge variant="warning">1 nhóm cần theo dõi</Badge>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
