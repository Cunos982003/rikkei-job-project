import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Bell,
  CalendarCheck,
  CheckCircle2,
  Clock,
  ExternalLink,
  Mail,
  RefreshCw,
  Video,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Skeleton } from "@/components/feedback/Skeleton";
import {
  interviewsApi,
  type CandidateInterviewItem,
  type InterviewSlotItem,
} from "@/api/endpoints/interviews.api";
import { useAuthStore } from "@/store/useAuthStore";

function formatRange(startIso: string, endIso: string): string {
  const start = new Date(startIso);
  const end = new Date(endIso);
  const dateStr = start.toLocaleDateString("vi-VN", {
    weekday: "short",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
  const startTime = start.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
  const endTime = end.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
  return `${dateStr} • ${startTime} - ${endTime}`;
}

export function CandidateInterviewsPage() {
  const { user } = useAuthStore();
  const queryClient = useQueryClient();
  const [selectedSlotByRound, setSelectedSlotByRound] = useState<Record<string, string>>({});

  const { data: res, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ["myInterviews", user?.id],
    queryFn: () => interviewsApi.listMyInterviews(),
    enabled: Boolean(user?.id),
  });

  const pickMutation = useMutation({
    mutationFn: ({ roundId, slotId }: { roundId: string; slotId: string }) =>
      interviewsApi.pickSlot(roundId, slotId),
    onSuccess: (response) => {
      toast.success(
        `Đã chốt lịch phỏng vấn! Link phòng họp: ${response.data.meetUrl}`
      );
      queryClient.invalidateQueries({ queryKey: ["myInterviews"] });
    },
    onError: (err: any) => {
      toast.error(
        err?.response?.data?.message ||
          err?.message ||
          "Không thể xác nhận khung giờ phỏng vấn"
      );
    },
  });

  const interviews: CandidateInterviewItem[] = res?.data ?? [];

  const handlePickSlot = (roundId: string) => {
    const slotId = selectedSlotByRound[roundId];
    if (!slotId) {
      toast.error("Vui lòng chọn một khung giờ phỏng vấn trước khi xác nhận");
      return;
    }
    pickMutation.mutate({ roundId, slotId });
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto pb-12">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-border">
        <div>
          <h1 className="text-xl font-bold text-foreground flex items-center gap-2">
            <CalendarCheck className="h-5 w-5 text-primary" />
            Lịch phỏng vấn & Lời mời của tôi
          </h1>
          <p className="text-xs text-muted mt-1">
            Chọn khung giờ phù hợp từ đề xuất của nhà tuyển dụng để hệ thống tự động tạo phòng họp Rikkei Meet và gửi lịch (.ics).
          </p>
        </div>
        <Button
          size="sm"
          variant="outline"
          leftIcon={<RefreshCw className={`h-3.5 w-3.5 ${isFetching ? "animate-spin" : ""}`} />}
          onClick={() => void refetch()}
        >
          Làm mới
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-44 rounded-xl" />
          <Skeleton className="h-44 rounded-xl" />
        </div>
      ) : isError ? (
        <Card>
          <CardContent className="py-10 text-center space-y-2">
            <p className="text-sm text-danger font-medium">
              Không thể tải danh sách lịch phỏng vấn. Vui lòng thử lại sau.
            </p>
            <Button size="sm" variant="outline" onClick={() => void refetch()}>
              Thử lại
            </Button>
          </CardContent>
        </Card>
      ) : interviews.length === 0 ? (
        <Card>
          <CardContent className="py-14 text-center space-y-2">
            <CalendarCheck className="h-10 w-10 text-muted/50 mx-auto" />
            <h3 className="text-sm font-semibold text-foreground">
              Chưa có lời mời phỏng vấn nào
            </h3>
            <p className="text-xs text-muted max-w-md mx-auto">
              Khi nhà tuyển dụng đề xuất khung giờ phỏng vấn cho đơn ứng tuyển của bạn, thẻ lời mời sẽ xuất hiện tại đây.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-5">
          {interviews.map((item) => {
            const activeSchedule =
              item.schedule && item.schedule.status === "SCHEDULED" ? item.schedule : null;
            const openSlots: InterviewSlotItem[] = item.slots.filter((s) => s.status === "OPEN");
            const selectedSlotId = selectedSlotByRound[item.roundId] || "";

            return (
              <Card key={item.roundId} className="overflow-hidden">
                <CardHeader className="bg-slate-50/70 border-b border-border-subtle flex flex-row items-center justify-between gap-3">
                  <div>
                    <CardTitle className="text-base flex items-center gap-2">
                      <span>
                        Vòng {item.roundNo}: {item.roundName}
                      </span>
                      {activeSchedule ? (
                        <Badge variant="verified" size="sm">
                          Đã chốt lịch
                        </Badge>
                      ) : openSlots.length > 0 ? (
                        <Badge variant="warning" size="sm">
                          Chờ bạn chọn khung giờ ({openSlots.length})
                        </Badge>
                      ) : (
                        <Badge variant="default" size="sm">
                          Đã hủy / Chờ cập nhật
                        </Badge>
                      )}
                    </CardTitle>
                    {item.interviewerEmail && (
                      <p className="text-xs text-muted flex items-center gap-1.5 mt-1">
                        <Mail className="h-3.5 w-3.5" />
                        Người phỏng vấn: {item.interviewerEmail}
                      </p>
                    )}
                  </div>
                </CardHeader>

                <CardContent className="p-5 space-y-4">
                  {/* Confirmed Schedule Block */}
                  {activeSchedule && (
                    <div className="p-4 rounded-xl bg-emerald-50/80 border border-emerald-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="space-y-1.5">
                        <div className="text-xs font-bold text-emerald-800 flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4" />
                          Thời gian phỏng vấn đã xác nhận
                        </div>
                        <div className="text-sm font-semibold text-foreground flex items-center gap-1.5">
                          <Clock className="h-4 w-4 text-emerald-700" />
                          {formatRange(activeSchedule.startAt, activeSchedule.endAt)}
                        </div>
                        <div className="flex flex-wrap items-center gap-2 text-[11px] text-emerald-800">
                          <span className="truncate max-w-md">{activeSchedule.meetUrl}</span>
                          {activeSchedule.reminded24hAt && (
                            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-white border border-emerald-200">
                              <Bell className="h-3 w-3" /> Đã nhắc 24h
                            </span>
                          )}
                          {activeSchedule.reminded1hAt && (
                            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-white border border-emerald-200">
                              <Bell className="h-3 w-3" /> Đã nhắc 1h
                            </span>
                          )}
                        </div>
                      </div>

                      <a
                        href={activeSchedule.meetUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-primary text-white text-xs font-semibold hover:bg-primary/90 transition-colors shrink-0 shadow-xs"
                      >
                        <Video className="h-4 w-4" />
                        Vào phòng họp
                        <ExternalLink className="h-3.5 w-3.5" />
                      </a>
                    </div>
                  )}

                  {/* Slot Selection Block */}
                  {!activeSchedule && openSlots.length > 0 && (
                    <div className="space-y-3">
                      <p className="text-xs font-semibold text-foreground">
                        Chọn 1 khung giờ thuận tiện nhất cho bạn:
                      </p>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                        {openSlots.map((slot) => {
                          const isSelected = selectedSlotId === slot.id;
                          return (
                            <button
                              key={slot.id}
                              type="button"
                              onClick={() =>
                                setSelectedSlotByRound((prev) => ({
                                  ...prev,
                                  [item.roundId]: slot.id,
                                }))
                              }
                              className={`p-3 rounded-xl border text-left transition-all flex items-center justify-between gap-2 ${
                                isSelected
                                  ? "border-primary bg-primary-light/50 ring-2 ring-primary/20"
                                  : "border-border bg-surface hover:border-primary/50 hover:bg-slate-50"
                              }`}
                            >
                              <div className="flex items-center gap-2 text-xs font-medium text-foreground">
                                <Clock className="h-4 w-4 text-primary shrink-0" />
                                <span>{formatRange(slot.startAt, slot.endAt)}</span>
                              </div>
                              <span
                                className={`h-4 w-4 rounded-full border-2 flex items-center justify-center shrink-0 ${
                                  isSelected ? "border-primary bg-primary" : "border-slate-300"
                                }`}
                              >
                                {isSelected && (
                                  <span className="h-1.5 w-1.5 rounded-full bg-white" />
                                )}
                              </span>
                            </button>
                          );
                        })}
                      </div>

                      <div className="flex justify-end pt-1">
                        <Button
                          size="sm"
                          variant="primary"
                          isLoading={pickMutation.isPending}
                          disabled={!selectedSlotId || pickMutation.isPending}
                          onClick={() => handlePickSlot(item.roundId)}
                        >
                          Xác nhận chọn khung giờ & Tạo phòng họp
                        </Button>
                      </div>
                    </div>
                  )}

                  {/* Cancelled / History Slots */}
                  {item.slots.some((s) => s.status === "CANCELLED") && (
                    <div className="pt-2 border-t border-border-subtle">
                      <p className="text-[11px] font-medium text-muted mb-1.5">
                        Các khung giờ đã hủy:
                      </p>
                      <div className="flex flex-wrap gap-2">
                        {item.slots
                          .filter((s) => s.status === "CANCELLED")
                          .map((slot) => (
                            <span
                              key={slot.id}
                              className="inline-flex items-center gap-1 text-[11px] text-muted line-through px-2.5 py-1 rounded-lg bg-slate-100 border border-border-subtle"
                            >
                              <XCircle className="h-3 w-3" />
                              {formatRange(slot.startAt, slot.endAt)}
                            </span>
                          ))}
                      </div>
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
