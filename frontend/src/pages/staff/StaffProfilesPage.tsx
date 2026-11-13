import { FolderCheck, Users, ShieldCheck } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";

export function StaffProfilesPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Duyệt hồ sơ ứng viên</h1>
        <p className="text-xs text-muted mt-1">Xét duyệt hồ sơ năng lực và đăng ký ứng tuyển theo từng vị trí phù hợp.</p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <FolderCheck className="h-5 w-5 text-primary" />
              <CardTitle>Hồ sơ ứng viên – Nguyễn Văn A</CardTitle>
            </div>
            <Badge variant="warning">Cần xét duyệt</Badge>
          </div>
          <CardDescription>Vị trí: Java Backend Developer • Điểm hồ sơ: 85/100</CardDescription>
        </CardHeader>
        <CardContent className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-sm text-muted">
            <ShieldCheck className="h-4 w-4" />
            <span>Hồ sơ đã có đủ bằng chứng và đang chờ xác nhận bởi cán bộ vận hành.</span>
          </div>
          <Button variant="primary" size="sm">Duyệt hồ sơ</Button>
        </CardContent>
      </Card>

      <div className="grid sm:grid-cols-2 gap-4">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><Users className="h-5 w-5 text-muted" />Tổng hồ sơ</CardTitle>
            <CardDescription>12 hồ sơ đang tồn tại trong hệ thống.</CardDescription>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-success" />Đã duyệt</CardTitle>
            <CardDescription>7 hồ sơ đã qua kiểm tra và sẵn sàng truyền cho HR.</CardDescription>
          </CardHeader>
        </Card>
      </div>
    </div>
  );
}
