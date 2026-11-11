import { useAuthStore } from "../../store/useAuthStore";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Award, BookOpen, Briefcase, Sparkles, CheckCircle2 } from "lucide-react";

export function CandidateProfilePage() {
  const { user } = useAuthStore();

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="rounded-2xl bg-gradient-to-r from-slate-900 to-slate-800 text-white p-6 sm:p-8 relative overflow-hidden shadow-sm">
        <div className="relative z-10 max-w-2xl space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/30 border border-primary/40 text-red-300 text-xs font-semibold">
            <Sparkles className="h-3.5 w-3.5" />
            Hồ sơ năng lực xác thực
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
            Xin chào, {user?.fullName || "Ứng viên"}!
          </h1>
          <p className="text-slate-300 text-xs sm:text-sm leading-relaxed">
            Hồ sơ kỹ năng của bạn được đồng bộ tự động từ điểm thi Khảo thí, kết quả học tập Rikkei LMS và nghiệm thu thực tập IOC.
          </p>
          <div className="pt-2 flex flex-wrap gap-2.5">
            <Button
              variant="primary"
              size="sm"
              leftIcon={<Briefcase className="h-4 w-4" />}
            >
              Xem việc làm phù hợp
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="bg-white/10 text-white border-white/20 hover:bg-white/20"
              leftIcon={<BookOpen className="h-4 w-4" />}
            >
              Lộ trình học bổ sung
            </Button>
          </div>
        </div>
      </div>

      {/* Grid Overview */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <Card hoverable>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Award className="h-5 w-5 text-primary" />
                <span>Kỹ năng nổi bật</span>
              </CardTitle>
              <Badge variant="verified" showIcon>Đã xác thực</Badge>
            </div>
            <CardDescription>Các kỹ năng đạt thang điểm Tốt và Xuất sắc</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-medium">
                <span>Java Core</span>
                <span className="text-primary font-bold">88/100 (Xuất sắc)</span>
              </div>
              <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full bg-primary rounded-full" style={{ width: "88%" }} />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-medium">
                <span>Spring Boot</span>
                <span className="text-success font-bold">85/100 (Xuất sắc)</span>
              </div>
              <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full bg-success rounded-full" style={{ width: "85%" }} />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex justify-between text-xs font-medium">
                <span>PostgreSQL / SQL</span>
                <span className="text-success font-bold">82/100 (Tốt)</span>
              </div>
              <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-500 rounded-full" style={{ width: "82%" }} />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card hoverable>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CheckCircle2 className="h-5 w-5 text-success" />
              <span>Trạng thái công khai</span>
            </CardTitle>
            <CardDescription>Kiểm soát quyền hiển thị hồ sơ với doanh nghiệp</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="p-3 bg-slate-50 rounded-lg border border-border-subtle text-xs text-muted leading-relaxed">
              Hồ sơ đang ở trạng thái <strong className="text-foreground">Công khai</strong> (đã được Cán bộ kiểm duyệt). Doanh nghiệp có thể tìm thấy hồ sơ của bạn nhưng email và số điện thoại luôn được che giấu an toàn.
            </div>
            <Button variant="outline" size="sm" className="w-full text-danger border-rose-200 hover:bg-rose-50">
              Rút công khai hồ sơ
            </Button>
          </CardContent>
        </Card>

        <Card hoverable>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <BookOpen className="h-5 w-5 text-info" />
              <span>Học tập bổ sung (LMS)</span>
            </CardTitle>
            <CardDescription>Bổ sung kỹ năng còn thiếu theo yêu cầu JD</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-xs text-muted leading-relaxed">
              Bạn có <strong className="text-foreground">2 kỹ năng</strong> có thể nâng bậc để tăng Điểm khớp (Matching Score) lên trên 90%.
            </p>
            <Button variant="secondary" size="sm" className="w-full">
              Khám phá khóa học bổ sung
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

