import { GraduationCap, BookOpenCheck } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";

export function CandidateLearningPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Học bổ sung LMS</h1>
        <p className="text-xs text-muted mt-1">Gợi ý lộ trình học và đề cương khóa học phù hợp với vị trí mong muốn.</p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <GraduationCap className="h-5 w-5 text-primary" />
              <CardTitle>Khóa học nâng cao: Java Spring Boot</CardTitle>
            </div>
            <Badge variant="verified">Đã đăng ký</Badge>
          </div>
          <CardDescription>3 tuần • 12 bài • Điểm mục tiêu 85%</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-2 text-sm text-muted">
            <BookOpenCheck className="h-4 w-4" />
            <span>Khóa học này hỗ trợ bổ sung kỹ năng backend và API testing để tăng điểm khớp với vị trí ứng tuyển.</span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
