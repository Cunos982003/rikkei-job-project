import { useState } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../../components/common/Card";
import { Badge } from "../../../components/common/Badge";
import { Button } from "../../../components/common/Button";
import {
  CheckCircle2,
  Clock,
  EyeOff,
  History,
  Loader2,
  Send,
  ShieldAlert,
  Undo2,
} from "lucide-react";
import type { ReviewAction, Visibility } from "../../../api/endpoints/profile.api";
import {
  usePublishProfile,
  useReviewHistory,
  useWithdrawProfile,
} from "../hooks/useProfileVisibility";

const STATUS_META: Record<
  Visibility,
  { label: string; variant: "default" | "primary" | "warning" | "danger" | "verified"; description: string }
> = {
  PRIVATE: {
    label: "Chưa công khai",
    variant: "default",
    description:
      "Doanh nghiệp không thể tìm thấy hồ sơ của bạn. Gửi duyệt để cán bộ xem xét công khai.",
  },
  PENDING: {
    label: "Đang chờ duyệt",
    variant: "warning",
    description:
      "Hồ sơ đã được gửi và đang chờ cán bộ tuyển dụng duyệt. Bạn có thể rút lại bất cứ lúc nào.",
  },
  PUBLIC: {
    label: "Đã công khai",
    variant: "verified",
    description:
      "Doanh nghiệp có thể tìm thấy hồ sơ của bạn. Rút công khai sẽ ẩn hồ sơ ngay lập tức.",
  },
  REJECTED: {
    label: "Bị trả lại",
    variant: "danger",
    description:
      "Cán bộ đã trả lại hồ sơ. Xem lý do bên dưới, chỉnh sửa rồi gửi duyệt lại.",
  },
  RECRUITER: {
    label: "Chỉ doanh nghiệp",
    variant: "primary",
    description: "Hồ sơ chỉ hiển thị với doanh nghiệp được chỉ định.",
  },
};

const ACTION_LABEL: Record<ReviewAction, string> = {
  SUBMIT: "Gửi duyệt",
  APPROVE: "Duyệt công khai",
  REJECT: "Trả lại hồ sơ",
  WITHDRAW: "Rút công khai",
};

function formatDateTime(value: string): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface VisibilitySectionProps {
  visibility: Visibility | undefined;
  userId: string | undefined;
}

export function VisibilitySection({ visibility, userId }: VisibilitySectionProps) {
  const [consent, setConsent] = useState(false);

  const { data: historyData, isLoading: historyLoading } = useReviewHistory();
  const publishMutation = usePublishProfile();
  const withdrawMutation = useWithdrawProfile();

  const history = historyData?.data ?? [];
  const latestRejection = history.find((item) => item.action === "REJECT");
  const status = STATUS_META[visibility ?? "PRIVATE"];

  const canSubmit = visibility === "PRIVATE" || visibility === "REJECTED";
  const canWithdraw = visibility === "PENDING" || visibility === "PUBLIC";

  const handleSubmit = () => {
    if (!consent) return;
    publishMutation.mutate(true, {
      onSuccess: () => setConsent(false),
    });
  };

  const handleWithdraw = () => {
    withdrawMutation.mutate();
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-3 flex-wrap">
          <CardTitle className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5 text-success" />
            <span>Công khai hồ sơ</span>
          </CardTitle>
          <Badge variant={status.variant} size="sm" showIcon>
            {status.label}
          </Badge>
        </div>
        <CardDescription>
          Doanh nghiệp chỉ thấy hồ sơ khi bạn đồng ý công khai và cán bộ đã duyệt.
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        <div className="p-3 bg-slate-50 rounded-lg border border-border-subtle text-xs text-muted leading-relaxed">
          {status.description}
        </div>

        {visibility === "REJECTED" && latestRejection?.reason && (
          <div className="p-3 rounded-lg border border-rose-200 bg-rose-50 text-xs leading-relaxed">
            <p className="flex items-center gap-1.5 font-semibold text-danger mb-1">
              <ShieldAlert className="h-3.5 w-3.5" />
              Lý do bị trả lại
            </p>
            <p className="text-foreground">{latestRejection.reason}</p>
          </div>
        )}

        {canSubmit && (
          <label className="flex items-start gap-2.5 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={consent}
              onChange={(e) => setConsent(e.target.checked)}
              className="mt-0.5 h-4 w-4 rounded border-border accent-primary cursor-pointer"
            />
            <span className="text-xs text-muted leading-relaxed">
              Tôi đồng ý công khai hồ sơ và cho phép doanh nghiệp xem thông tin năng lực của mình.
            </span>
          </label>
        )}

        {canSubmit && (
          <Button
            variant="primary"
            size="sm"
            className="w-full"
            disabled={!consent}
            isLoading={publishMutation.isPending}
            onClick={handleSubmit}
            leftIcon={<Send className="h-4 w-4" />}
          >
            {visibility === "REJECTED" ? "Gửi duyệt lại" : "Gửi duyệt công khai"}
          </Button>
        )}

        {canWithdraw && (
          <Button
            variant="outline"
            size="sm"
            className="w-full text-danger border-rose-200 hover:bg-rose-50"
            isLoading={withdrawMutation.isPending}
            onClick={handleWithdraw}
            leftIcon={visibility === "PUBLIC" ? <EyeOff className="h-4 w-4" /> : <Undo2 className="h-4 w-4" />}
          >
            {visibility === "PUBLIC" ? "Rút công khai hồ sơ" : "Rút lại yêu cầu duyệt"}
          </Button>
        )}

        <div className="pt-2 space-y-2">
          <h4 className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
            <History className="h-3.5 w-3.5" />
            Lịch sử duyệt hồ sơ
          </h4>

          {historyLoading ? (
            <div className="flex items-center justify-center py-6">
              <Loader2 className="h-5 w-5 animate-spin text-primary" />
            </div>
          ) : history.length === 0 ? (
            <p className="text-xs text-muted py-3 text-center">
              Chưa có lần chuyển trạng thái nào.
            </p>
          ) : (
            <div className="overflow-x-auto rounded-lg border border-border-subtle">
              <table className="w-full text-xs">
                <thead className="bg-slate-50 text-muted">
                  <tr>
                    <th className="text-left font-medium px-3 py-2">Hành động</th>
                    <th className="text-left font-medium px-3 py-2">Lý do</th>
                    <th className="text-left font-medium px-3 py-2">Người thực hiện</th>
                    <th className="text-left font-medium px-3 py-2">Thời gian</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((item) => (
                    <tr key={item.id} className="border-t border-border-subtle">
                      <td className="px-3 py-2">
                        <span className="flex items-center gap-1.5 font-medium text-foreground">
                          <Clock className="h-3 w-3 text-muted" />
                          {ACTION_LABEL[item.action]}
                        </span>
                      </td>
                      <td className="px-3 py-2 text-muted">{item.reason || "—"}</td>
                      <td className="px-3 py-2 text-muted">
                        {item.actorId === userId ? "Bạn" : "Cán bộ tuyển dụng"}
                      </td>
                      <td className="px-3 py-2 text-muted whitespace-nowrap">
                        {formatDateTime(item.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
