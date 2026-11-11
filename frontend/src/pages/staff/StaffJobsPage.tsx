import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { CheckSquare, Check, X, Building2, KeyRound } from "lucide-react";

export function StaffJobsPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Kiểm Duyệt Tin Tuyển Dụng & Vận Hành</h1>
          <p className="text-xs text-muted mt-1">
            Phân hệ dành cho Cán bộ tuyển sinh & hướng nghiệp (STAFF)
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" leftIcon={<Building2 className="h-4 w-4" />}>
            Tạo doanh nghiệp mới
          </Button>
          <Button variant="outline" size="sm" leftIcon={<KeyRound className="h-4 w-4" />}>
            Cấp tài khoản HR
          </Button>
        </div>
      </div>

      <div className="space-y-4">
        <h2 className="text-base font-semibold text-foreground flex items-center gap-2">
          <CheckSquare className="h-5 w-5 text-warning" />
          <span>Tin tuyển dụng (JD) đang chờ duyệt (1 tin)</span>
        </h2>

        <Card>
          <CardHeader>
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <CardTitle>Java Backend Developer (Fresher / Junior)</CardTitle>
                <CardDescription>Doanh nghiệp: Công ty Cổ phần Rikkeisoft • Địa điểm: Hà Nội</CardDescription>
              </div>
              <Badge variant="warning">Chờ duyệt</Badge>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-xs text-muted leading-relaxed">
              Mức lương: <strong>12.000.000 - 18.000.000 đ</strong> • Kỹ năng bắt buộc: Java, Spring Boot, SQL, Git, REST API.
            </p>

            <div className="flex items-center justify-end gap-2 pt-2 border-t border-border-subtle">
              <Button variant="outline" size="sm" className="text-danger border-rose-200" leftIcon={<X className="h-4 w-4" />}>
                Từ chối (kèm lý do)
              </Button>
              <Button variant="primary" size="sm" leftIcon={<Check className="h-4 w-4" />}>
                Phê duyệt tin JD
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

