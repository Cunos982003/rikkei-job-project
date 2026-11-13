import { BriefcaseBusiness, Search, Sparkles } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";

export function CandidateJobsPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Việc làm phù hợp</h1>
        <p className="text-xs text-muted mt-1">Danh sách đề xuất theo hồ sơ năng lực và trọng số kỹ năng hiện tại.</p>
      </div>

      <Card>
        <CardHeader>
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <BriefcaseBusiness className="h-5 w-5 text-primary" />
              <CardTitle>Java Backend Developer (Junior)</CardTitle>
            </div>
            <Badge variant="self">Khớp 87%</Badge>
          </div>
          <CardDescription>Công ty Cổ phần Rikkeisoft • Hà Nội • 1 năm kinh nghiệm</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap gap-2 text-[11px]">
            <Badge variant="default">Java</Badge>
            <Badge variant="default">Spring Boot</Badge>
            <Badge variant="default">REST API</Badge>
          </div>
          <div className="flex justify-end">
            <Button variant="primary" size="sm" leftIcon={<Search className="h-4 w-4" />}>
              Xem chi tiết
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card className="border-dashed border-primary/30 bg-primary-light/20">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary" />
            <CardTitle>Đề xuất AI</CardTitle>
          </div>
          <CardDescription>Hệ thống đang gợi ý vị trí phù hợp dựa trên hồ sơ kỹ năng và tín chỉ đã xác thực.</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted">Trang danh sách việc làm đang được giữ nguyên theo cấu trúc hiện có; đây là màn hình tối thiểu cho phân hệ Candidate.</p>
        </CardContent>
      </Card>
    </div>
  );
}
