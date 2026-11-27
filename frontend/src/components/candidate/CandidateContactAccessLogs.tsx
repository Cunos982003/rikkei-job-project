import { useQuery } from "@tanstack/react-query";
import { Building2, Calendar, Eye } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { contactApi, type ContactAccessLog, type ContactAccessVia } from "@/api/endpoints/contact.api";

const VIA_BADGE: Record<ContactAccessVia, { label: string; variant: "default" | "primary" | "verified" }> = {
  APPLICATION: { label: "Ứng tuyển", variant: "verified" },
  APPROVED_REQUEST: { label: "Yêu cầu được duyệt", variant: "primary" },
};

export function CandidateContactAccessLogs() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ["candidate", "contact-access-logs"],
    queryFn: () => contactApi.getMyAccessLogs(),
    staleTime: 30_000,
  });

  const logs: ContactAccessLog[] = data?.data || [];

  if (isLoading) {
    return (
      <Card>
        <CardContent className="py-8 text-center">Đang tải nhật ký...</CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardContent className="py-8 text-center text-destructive">Không thể tải nhật ký xem liên hệ</CardContent>
      </Card>
    );
  }

  if (logs.length === 0) {
    return (
      <Card>
        <CardContent className="py-8 text-center text-muted">
          Chưa có công ty nào xem thông tin liên hệ của bạn
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Eye className="h-5 w-5" />
          Nhật ký xem thông tin liên hệ
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {logs.map((log) => (
            <div key={log.id} className="flex items-center justify-between p-4 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors">
              <div className="flex items-center gap-4">
                <div className="p-2 bg-white rounded-lg border">
                  <Building2 className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="font-medium text-sm">{log.companyName || log.companyId}</p>
                  <p className="text-xs text-muted">Công ty đã xem thông tin liên hệ</p>
                </div>
              </div>
              <div className="flex items-center gap-3 text-sm">
                <Badge variant={VIA_BADGE[log.via].variant}>
                  {VIA_BADGE[log.via].label}
                </Badge>
                <div className="flex items-center gap-1 text-muted">
                  <Calendar className="h-3.5 w-3.5" />
                  <span>{new Date(log.createdAt).toLocaleString("vi-VN")}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}