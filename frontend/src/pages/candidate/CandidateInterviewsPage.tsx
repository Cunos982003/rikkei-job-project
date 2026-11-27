import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertTriangle,
  Award,
  Bell,
  CalendarCheck,
  CheckCircle2,
  Clock,
  ExternalLink,
  Gift,
  Mail,
  MessageSquareText,
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
import { candidateKeys } from "@/api/queryKeys";
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

function rangesOverlap(aStartIso: string, aEndIso: string, bStartIso: string, bEndIso: string): boolean {
  const aStart = new Date(aStartIso).getTime();
  const aEnd = new Date(aEndIso).getTime();
  const bStart = new Date(bStartIso).getTime();
  const bEnd = new Date(bEndIso).getTime();
  return aStart < bEnd && aEnd > bStart;
}

const EMPTY_INTERVIEWS: CandidateInterviewItem[] = [];

export function CandidateInterviewsPage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const queryClient = useQueryClient();
  const [selectedSlotByRound, setSelectedSlotByRound] = useState<Record<string, string>>({});

  const { data: res, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: candidateKeys.interviews.list(user?.id),
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
      queryClient.invalidateQueries({ queryKey: candidateKeys.interviews.all() });
    },
    onError: (err: any) => {
      toast.error(
        err?.response?.data?.message ||
          err?.message ||
          "Không thể xác nhận khung giờ phỏng vấn"
      );
    },
  });

  const interviews: CandidateInterviewItem[] = res?.data ?? EMPTY_INTERVIEWS;

  const scheduledInterviews = useMemo(
    () =>
      interviews.filter(
        (i): i is CandidateInterviewItem & { schedule: NonNullable<CandidateInterviewItem["schedule"]> } =>
          Boolean(i.schedule && i.schedule.status === "SCHEDULED")
      ),
    [interviews]
  );

  const findScheduledConflict = (roundId: string, slot: InterviewSlotItem) => {
    return scheduledInterviews.find(
      (other) =>
        other.roundId !== roundId &&
        rangesOverlap(slot.startAt, slot.endAt, other.schedule.startAt, other.schedule.endAt)
    );
  };

  const findOpenInvitationConflict = (roundId: string, slot: InterviewSlotItem) => {
    return interviews.find(
      (other) =>
        other.roundId !== roundId &&
        (!other.schedule || other.schedule.status !== "SCHEDULED") &&
        other.slots.some(
          (otherSlot) =>
            otherSlot.status === "OPEN" &&
            rangesOverlap(slot.startAt, slot.endAt, otherSlot.startAt, otherSlot.endAt)
        )
    );
  };

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
              item.schedule &&
              (item.schedule.status === "SCHEDULED" || item.schedule.status === "DONE")
                ? item.schedule
                : null;
            const openSlots: InterviewSlotItem[] = item.slots.filter((s) => s.status === "OPEN");
            const selectedSlotId = selectedSlotByRound[item.roundId] || "";
            const hasResultReport =
              item.result !== "PENDING" || item.score !== null || Boolean(item.note);

            return (
              <Card key={item.roundId} className="overflow-hidden">
                <CardHeader className="bg-slate-50/70 border-b border-border-subtle flex flex-row items-center justify-between gap-3">
                  <div>
                    <CardTitle className="text-base flex flex-wrap items-center gap-2">
                      <span>
                        Vòng {item.roundNo}: {item.roundName}
                      </span>
                      {item.result === "PASS" ? (
                        <Badge variant="verified" size="sm">
                          Đạt (PASS)
                        </Badge>
                      ) : item.result === "FAIL" ? (
                        <Badge variant="danger" size="sm">
                          Không đạt (FAIL)
                        </Badge>
                      ) : activeSchedule ? (
                        <Badge variant="verified" size="sm">
                          {activeSchedule.status === "DONE" ? "Đã phỏng vấn" : "Đã chốt lịch"}
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
                      {item.score !== null && item.score !== undefined && (
                        <Badge variant="primary" size="sm">
                          Điểm: {item.score}/100
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
                  {/* Báo cáo điểm & kết quả phỏng vấn từ HR */}
                  {hasResultReport && (
                    <div
                      className={`p-4 rounded-xl border space-y-3 ${
                        item.result === "PASS"
                          ? "bg-emerald-50/80 border-emerald-200"
                          : item.result === "FAIL"
                          ? "bg-rose-50/80 border-rose-200"
                          : "bg-amber-50/70 border-amber-200"
                      }`}
                    >
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="flex items-center gap-2">
                          <Award
                            className={`h-5 w-5 shrink-0 ${
                              item.result === "PASS"
                                ? "text-emerald-600"
                                : item.result === "FAIL"
                                ? "text-rose-600"
                                : "text-amber-600"
                            }`}
                          />
                          <div>
                            <div className="text-xs font-bold text-foreground">
                              Báo cáo kết quả phỏng vấn từ Nhà tuyển dụng
                            </div>
                            <div className="text-[11px] text-muted">
                              {item.result === "PASS"
                                ? "Chúc mừng! Bạn đã vượt qua vòng phỏng vấn này."
                                : item.result === "FAIL"
                                ? "Rất tiếc, kết quả vòng phỏng vấn này chưa đạt yêu cầu."
                                : "Nhà tuyển dụng đã cập nhật đánh giá cho vòng phỏng vấn."}
                            </div>
                          </div>
                        </div>

                        {item.score !== null && item.score !== undefined && (
                          <div className="flex items-center gap-2">
                            <div className="text-right">
                              <span className="text-lg font-extrabold text-primary">
                                {item.score}
                              </span>
                              <span className="text-xs text-muted font-medium"> / 100 điểm</span>
                            </div>
                          </div>
                        )}
                      </div>

                      {item.score !== null && item.score !== undefined && (
                        <div className="w-full h-2 rounded-full bg-white/80 overflow-hidden border border-border-subtle">
                          <div
                            className={`h-full rounded-full transition-all ${
                              item.result === "FAIL"
                                ? "bg-rose-500"
                                : item.score >= 70
                                ? "bg-emerald-500"
                                : "bg-amber-500"
                            }`}
                            style={{ width: `${Math.min(100, Math.max(0, item.score))}%` }}
                          />
                        </div>
                      )}

                      {item.note && (
                        <div className="rounded-lg bg-white/80 border border-border-subtle p-3 text-xs text-foreground flex items-start gap-2">
                          <MessageSquareText className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                          <div>
                            <span className="font-semibold block text-[11px] text-muted mb-0.5">
                              Nhận xét / Phản hồi từ người phỏng vấn:
                            </span>
                            <p className="whitespace-pre-wrap leading-relaxed">{item.note}</p>
                          </div>
                        </div>
                      )}

                      {(item.applicationStatus === "OFFER" ||
                        item.applicationStatus === "HIRED") && (
                        <div className="flex justify-end pt-1">
                          <Button
                            size="sm"
                            variant="primary"
                            leftIcon={<Gift className="h-3.5 w-3.5" />}
                            onClick={() => navigate("/candidate/offers")}
                          >
                            Xem thư mời nhận việc (Offer)
                          </Button>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Confirmed Schedule Block */}
                  {activeSchedule && (
                    <div className="p-4 rounded-xl bg-emerald-50/80 border border-emerald-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                      <div className="space-y-1.5">
                        <div className="text-xs font-bold text-emerald-800 flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4" />
                          {activeSchedule.status === "DONE"
                            ? "Buổi phỏng vấn đã hoàn thành"
                            : "Thời gian phỏng vấn đã xác nhận"}
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

                      {activeSchedule.status === "SCHEDULED" && (
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
                      )}
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
                          const scheduledConflict = findScheduledConflict(item.roundId, slot);
                          const openConflict = !scheduledConflict
                            ? findOpenInvitationConflict(item.roundId, slot)
                            : undefined;
                          const isBlocked = Boolean(scheduledConflict);
                          const isSelected = !isBlocked && selectedSlotId === slot.id;
                          return (
                            <button
                              key={slot.id}
                              type="button"
                              disabled={isBlocked}
                              onClick={() => {
                                if (isBlocked) return;
                                setSelectedSlotByRound((prev) => ({
                                  ...prev,
                                  [item.roundId]: slot.id,
                                }));
                              }}
                              className={`p-3 rounded-xl border text-left transition-all flex flex-col gap-1.5 ${
                                isBlocked
                                  ? "border-danger/40 bg-danger/5 opacity-70 cursor-not-allowed"
                                  : isSelected
                                  ? "border-primary bg-primary-light/50 ring-2 ring-primary/20"
                                  : "border-border bg-surface hover:border-primary/50 hover:bg-slate-50"
                              }`}
                            >
                              <div className="flex items-center justify-between gap-2 w-full">
                                <div className="flex items-center gap-2 text-xs font-medium text-foreground">
                                  <Clock className={`h-4 w-4 shrink-0 ${isBlocked ? "text-danger" : "text-primary"}`} />
                                  <span className={isBlocked ? "line-through text-muted" : ""}>
                                    {formatRange(slot.startAt, slot.endAt)}
                                  </span>
                                </div>
                                <span
                                  className={`h-4 w-4 rounded-full border-2 flex items-center justify-center shrink-0 ${
                                    isBlocked
                                      ? "border-danger/50 bg-transparent"
                                      : isSelected
                                      ? "border-primary bg-primary"
                                      : "border-slate-300"
                                  }`}
                                >
                                  {isSelected && (
                                    <span className="h-1.5 w-1.5 rounded-full bg-white" />
                                  )}
                                </span>
                              </div>
                              {scheduledConflict && (
                                <div className="flex items-center gap-1 text-[11px] font-medium text-danger">
                                  <AlertTriangle className="h-3 w-3 shrink-0" />
                                  <span>
                                    Trùng lịch đã chốt: Vòng {scheduledConflict.roundNo} ({scheduledConflict.roundName})
                                  </span>
                                </div>
                              )}
                              {openConflict && (
                                <div className="flex items-center gap-1 text-[11px] font-medium text-amber-600">
                                  <AlertTriangle className="h-3 w-3 shrink-0" />
                                  <span>
                                    Trùng giờ với lời mời khác: Vòng {openConflict.roundNo} ({openConflict.roundName})
                                  </span>
                                </div>
                              )}
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
