import { Sparkles, Bot, MessageSquareText } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../components/common/Card";
import { Button } from "../../components/common/Button";

export function CandidateAiAssistantPage() {
  return (
    <div className="space-y-6">
      <div className="pb-4 border-b border-border">
        <h1 className="text-2xl font-bold text-foreground">Trợ lý AI</h1>
        <p className="text-xs text-muted mt-1">Hỗ trợ tư vấn hồ sơ, viết lại mô tả, và đề xuất kỹ năng cần bổ sung.</p>
      </div>

      <Card className="border-dashed border-primary/40 bg-primary-light/20">
        <CardHeader>
          <div className="flex items-center gap-2">
            <Bot className="h-5 w-5 text-primary" />
            <CardTitle>Trợ lý đang chuẩn bị</CardTitle>
          </div>
          <CardDescription>Hiện tại giao diện này là phiên bản tối thiểu để đảm bảo người dùng có thể truy cập vào nhóm chức năng Trợ lý AI.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-2 text-sm text-muted">
            <MessageSquareText className="h-4 w-4" />
            <span>Hệ thống có thể hỗ trợ đóng vai trò gợi ý CV, câu trả lời phỏng vấn và roadmap học tập.</span>
          </div>
          <Button variant="primary" size="sm" leftIcon={<Sparkles className="h-4 w-4" />}>
            Mở trợ lý AI
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
