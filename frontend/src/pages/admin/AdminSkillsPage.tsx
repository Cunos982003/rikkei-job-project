import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Sliders, Award, AlertOctagon, ShieldCheck } from "lucide-react";

export function AdminSkillsPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Quản Trị Hệ Thống & Cấu Hình Trọng Số</h1>
        <p className="text-xs text-muted mt-1">
          Quản trị viên (ADMIN) • Cấu hình trọng số tính điểm, cấp huy hiệu và kiểm tra Audit Log bất biến
        </p>
      </div>

      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card hoverable>
          <CardHeader>
            <Sliders className="h-5 w-5 text-primary mb-2" />
            <CardTitle>Danh mục kỹ năng</CardTitle>
            <CardDescription>20 kỹ năng chuẩn theo khung năng lực Rikkei</CardDescription>
          </CardHeader>
          <CardContent>
            <Button variant="outline" size="sm" className="w-full">
              Cấu hình trọng số
            </Button>
          </CardContent>
        </Card>

        <Card hoverable>
          <CardHeader>
            <Award className="h-5 w-5 text-warning mb-2" />
            <CardTitle>Quản lý Huy hiệu</CardTitle>
            <CardDescription>Cấp và thu hồi Badge theo điều kiện kép</CardDescription>
          </CardHeader>
          <CardContent>
            <Button variant="outline" size="sm" className="w-full">
              Xem danh sách Badge
            </Button>
          </CardContent>
        </Card>

        <Card hoverable>
          <CardHeader>
            <AlertOctagon className="h-5 w-5 text-danger mb-2" />
            <CardTitle>Báo sai bằng chứng</CardTitle>
            <CardDescription>0 yêu cầu khiếu nại đang chờ xử lý</CardDescription>
          </CardHeader>
          <CardContent>
            <Button variant="outline" size="sm" className="w-full">
              Hàng chờ xử lý
            </Button>
          </CardContent>
        </Card>

        <Card hoverable>
          <CardHeader>
            <ShieldCheck className="h-5 w-5 text-success mb-2" />
            <CardTitle>Nhật ký Audit Log</CardTitle>
            <CardDescription>Bảng ghi bất biến append-only</CardDescription>
          </CardHeader>
          <CardContent>
            <Button variant="outline" size="sm" className="w-full">
              Truy vấn nhật ký
            </Button>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Trọng số tính điểm kỹ năng mặc định (Skill Weight Config)</CardTitle>
            <Badge variant="verified">Đã áp dụng</Badge>
          </div>
          <CardDescription>
            Chỉ số kỹ năng (0–100) là trung bình cộng có trọng số của các nguồn bằng chứng đã xác thực
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-3 text-xs">
            <div className="flex justify-between py-2 border-b border-border-subtle">
              <span className="font-medium text-foreground">1. Rikkei LMS (Kết quả môn học, chuyên cần, BTVN, R-point):</span>
              <strong className="text-primary">Trọng số 30%</strong>
            </div>
            <div className="flex justify-between py-2 border-b border-border-subtle">
              <span className="font-medium text-foreground">2. Hệ thống Khảo thí (Kết quả thi sát hạch ca thi):</span>
              <strong className="text-primary">Trọng số 25%</strong>
            </div>
            <div className="flex justify-between py-2 border-b border-border-subtle">
              <span className="font-medium text-foreground">3. Thực tập IOC (LMS-PM - Task nghiệm thu dự án thực tế):</span>
              <strong className="text-primary">Trọng số 25%</strong>
            </div>
            <div className="flex justify-between py-2 border-b border-border-subtle">
              <span className="font-medium text-foreground">4. Đánh giá từ Mentor (Phiếu nhận xét chuyên sâu):</span>
              <strong className="text-primary">Trọng số 20%</strong>
            </div>
            <div className="flex justify-between py-2 text-muted">
              <span>5. Dữ liệu ứng viên tự khai:</span>
              <span className="italic text-danger">0% (Không cộng điểm chính thức, hiển thị đối chiếu riêng)</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

