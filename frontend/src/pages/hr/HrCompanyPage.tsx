import { useAuthStore } from "../../store/useAuthStore";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";
import { Building2, Plus, Users, Calendar, Briefcase } from "lucide-react";

export function HrCompanyPage() {
  const { user } = useAuthStore();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-border">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-foreground">Hồ sơ Doanh nghiệp</h1>
            <Badge variant="verified">Đang hoạt động</Badge>
          </div>
          <p className="text-xs text-muted mt-1">
            Quản lý thông tin doanh nghiệp, tài khoản nhân sự và nhu cầu tuyển dụng
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Button variant="primary" size="sm" leftIcon={<Plus className="h-4 w-4" />}>
            Đăng tin tuyển dụng (JD)
          </Button>
          <Button variant="outline" size="sm" leftIcon={<Users className="h-4 w-4" />}>
            Tìm ứng viên
          </Button>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Tin tuyển dụng đang mở</CardDescription>
            <CardTitle className="text-2xl text-primary font-bold">3 JD</CardTitle>
          </CardHeader>
          <CardContent>
            <span className="text-xs text-muted flex items-center gap-1">
              <Briefcase className="h-3.5 w-3.5" /> 1 JD loại Thực tập IOC
            </span>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Đơn ứng tuyển mới</CardDescription>
            <CardTitle className="text-2xl text-foreground font-bold">12</CardTitle>
          </CardHeader>
          <CardContent>
            <span className="text-xs text-success font-medium">4 đơn có điểm khớp &gt; 80%</span>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Lịch phỏng vấn tuần này</CardDescription>
            <CardTitle className="text-2xl text-foreground font-bold">5</CardTitle>
          </CardHeader>
          <CardContent>
            <span className="text-xs text-muted flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5" /> Tự động tạo phòng Rikkei Meet
            </span>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardDescription>Nhân sự phụ trách</CardDescription>
            <CardTitle className="text-sm font-semibold text-foreground truncate">
              {user?.fullName}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <span className="text-xs text-muted truncate block">{user?.email}</span>
          </CardContent>
        </Card>
      </div>

      {/* Company Info Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5 text-primary" />
            <span>Thông tin pháp lý & Liên hệ</span>
          </CardTitle>
          <CardDescription>Thông tin hiển thị công khai trên các tin tuyển dụng</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid sm:grid-cols-2 gap-4 text-xs">
            <div>
              <span className="text-muted block">Tên doanh nghiệp:</span>
              <strong className="text-foreground text-sm font-medium">Công ty Cổ phần Rikkeisoft</strong>
            </div>
            <div>
              <span className="text-muted block">Mã số thuế:</span>
              <span className="text-foreground font-medium">0101234567</span>
            </div>
            <div>
              <span className="text-muted block">Lĩnh vực hoạt động:</span>
              <span className="text-foreground font-medium">Công nghệ thông tin & Xuất khẩu phần mềm</span>
            </div>
            <div>
              <span className="text-muted block">Quy mô nhân sự:</span>
              <span className="text-foreground font-medium">1000+ nhân viên</span>
            </div>
            <div>
              <span className="text-muted block">Trụ sở chính:</span>
              <span className="text-foreground font-medium">Tầng 21 Keangnam Landmark 72, Hà Nội</span>
            </div>
            <div>
              <span className="text-muted block">Website:</span>
              <a href="https://rikkeisoft.com" target="_blank" rel="noreferrer" className="text-primary hover:underline font-medium">
                https://rikkeisoft.com
              </a>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

