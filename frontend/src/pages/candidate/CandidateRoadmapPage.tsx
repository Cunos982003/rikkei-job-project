import { Route, CheckCircle2, CircleDashed } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";

export function CandidateRoadmapPage() {
  const roadmap = [
    { label: "Làm rõ mục tiêu nghề nghiệp", status: "done" },
    { label: "Cập nhật hồ sơ kỹ năng & bằng chứng", status: "done" },
    { label: "Hoàn thiện kỹ năng backend / API", status: "active" },
    { label: "Nộp đơn vị trí phù hợp", status: "pending" },
  ];

  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Lộ trình học tập & ứng tuyển</h1>
        <p className="text-xs text-muted mt-1">Theo dõi các bước cần hoàn thành để nâng điểm năng lực và tăng cơ hội phù hợp với vị trí thực tế.</p>
      </div>

      <div className="space-y-4">
        {roadmap.map((step) => (
          <Card key={step.label}>
            <CardHeader>
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  {step.status === "done" ? <CheckCircle2 className="h-4 w-4 text-success" /> : step.status === "active" ? <Route className="h-4 w-4 text-primary" /> : <CircleDashed className="h-4 w-4 text-muted" />}
                  <CardTitle className="text-base">{step.label}</CardTitle>
                </div>
                <Badge variant={step.status === "done" ? "verified" : step.status === "active" ? "self" : "default"}>
                  {step.status === "done" ? "Hoàn thành" : step.status === "active" ? "Đang thực hiện" : "Chờ bắt đầu"}
                </Badge>
              </div>
            </CardHeader>
            <CardContent>
              <CardDescription>
                {step.status === "done" ? "Đã hoàn thành đúng tiến độ hiện tại của hồ sơ ứng viên." : step.status === "active" ? "Đây là mục tiêu ưu tiên tiếp theo để cải thiện điểm khớp vị trí." : "Chưa bắt đầu; hệ thống sẽ gợi ý khi đủ điều kiện."}
              </CardDescription>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
