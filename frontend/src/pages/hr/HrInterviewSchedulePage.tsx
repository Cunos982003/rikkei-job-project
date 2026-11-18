import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  Clock,
  ExternalLink,
  Mail,
  RefreshCw,
  Video,
  FileText,
  Bell,
  ArrowRight,
  Briefcase,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Skeleton } from "@/components/feedback/Skeleton";
import {
  interviewsApi,
  type CompanyInterviewItem,
  type InterviewScheduleStatus,
} from "@/api/endpoints/interviews.api";
import {
  recruitmentApi,
  type RecruitmentStaffSummary,
  type RecruitmentStatus,
} from "@/api/endpoints/recruitment.api";

const WEEKDAY_LABELS = [
  "Thứ 2",
  "Thứ 3",
  "Thứ 4",
  "Thứ 5",
  "Thứ 6",
  "Thứ 7",
  "Chủ nhật",
];

const SCHEDULE_STATUS_META: Record<
  InterviewScheduleStatus,
  { label: string; variant: "primary" | "verified" | "danger" }
> = {
  SCHEDULED: { label: "Đã lên lịch", variant: "primary" },
  DONE: { label: "Đã hoàn thành", variant: "verified" },
  CANCELLED: { label: "Đã hủy", variant: "danger" },
};

const APP_STATUS_META: Record<
  RecruitmentStatus,
  { label: string; variant: "default" | "primary" | "warning" | "danger" | "verified" }
> = {
  SUBMITTED: { label: "Mới nộp", variant: "default" },
  SCREENING: { label: "Sàng lọc", variant: "warning" },
  INTERVIEW: { label: "Đang phỏng vấn", variant: "primary" },
  OFFER: { label: "Đã gửi Offer", variant: "verified" },
  HIRED: { label: "Đã tuyển", variant: "verified" },
  REJECTED: { label: "Từ chối", variant: "danger" },
  WITHDRAWN: { label: "Đã rút đơn", variant: "danger" },
};

const FILTER_TABS: { value: RecruitmentStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "Tất cả trạng thái" },
  { value: "INTERVIEW", label: "Đang phỏng vấn (INTERVIEW)" },
  { value: "OFFER", label: "Đã gửi Offer (OFFER)" },
  { value: "HIRED", label: "Đã tuyển (HIRED)" },
  { value: "SCREENING", label: "Sàng lọc (SCREENING)" },
  { value: "REJECTED", label: "Từ chối (REJECTED)" },
];

function getMonday(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date: Date, days: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

function toDayKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(
    date.getDate()
  ).padStart(2, "0")}`;
}

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatDayShort(date: Date): string {
  return date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
}

export function HrInterviewSchedulePage() {
  const navigate = useNavigate();
  const [weekStart, setWeekStart] = useState<Date>(() => getMonday(new Date()));
  const [statusFilter, setStatusFilter] = useState<RecruitmentStatus | "ALL">("ALL");
  const [appPage, setAppPage] = useState(0);

  const weekEnd = useMemo(() => {
    const end = addDays(weekStart, 6);
    end.setHours(23, 59, 59, 999);
    return end;
  }, [weekStart]);

  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart]
  );

  // 1. Lịch phỏng vấn theo tuần (UT-06)
  const {
    data: scheduleRes,
    isLoading: scheduleLoading,
    isError: scheduleError,
    refetch: refetchSchedules,
    isFetching: scheduleFetching,
  } = useQuery({
    queryKey: ["companyInterviews", weekStart.toISOString(), weekEnd.toISOString()],
    queryFn: () =>
      interviewsApi.listCompanyInterviews(weekStart.toISOString(), weekEnd.toISOString()),
  });

  // 2. Danh sách chi tiết các đơn ứng tuyển & Offer của doanh nghiệp
  const {
    data: appsRes,
    isLoading: appsLoading,
    isError: appsError,
    refetch: refetchApps,
    isFetching: appsFetching,
  } = useQuery({
    queryKey: ["hrApplications", statusFilter, appPage],
    queryFn: () =>
      recruitmentApi.listForHr({
        status: statusFilter === "ALL" ? undefined : statusFilter,
        page: appPage,
      }),
  });

  const schedules: CompanyInterviewItem[] = scheduleRes?.data ?? [];
  const appsPageData = appsRes?.data;
  const applications: RecruitmentStaffSummary[] = appsPageData?.content ?? [];
  const totalApplications = appsPageData?.totalElements ?? 0;
  const totalPages = appsPageData?.totalPages ?? 0;

  const schedulesByDay = useMemo(() => {
    const map: Record<string, CompanyInterviewItem[]> = {};
    for (const day of weekDays) {
      map[toDayKey(day)] = [];
    }
    for (const item of schedules) {
      const key = toDayKey(new Date(item.startAt));
      if (!map[key]) map[key] = [];
      map[key].push(item);
    }
    return map;
  }, [schedules, weekDays]);

  const todayKey = toDayKey(new Date());

  const handleRefreshAll = () => {
    void refetchSchedules();
    void refetchApps();
  };

  return (
    <div className="space-y-8 pb-12">
      {/* ==================== PHẦN 1: LỊCH PHỎNG VẤN TUẦN (UT-06) ==================== */}
      <section className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-border">
          <div>
            <h1 className="text-xl font-bold text-foreground flex items-center gap-2">
              <Calendar className="h-5 w-5 text-primary" />
              Lịch phỏng vấn doanh nghiệp (Theo tuần)
            </h1>
            <p className="text-xs text-muted mt-1">
              Theo dõi tất cả buổi phỏng vấn đã xác nhận của doanh nghiệp, link Rikkei Meet và trạng thái nhắc lịch tự động.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              size="sm"
              variant="outline"
              leftIcon={<ChevronLeft className="h-4 w-4" />}
              onClick={() => setWeekStart((prev) => addDays(prev, -7))}
            >
              Tuần trước
            </Button>
            <Button
              size="sm"
              variant="secondary"
              onClick={() => setWeekStart(getMonday(new Date()))}
            >
              Tuần này
            </Button>
            <Button
              size="sm"
              variant="outline"
              rightIcon={<ChevronRight className="h-4 w-4" />}
              onClick={() => setWeekStart((prev) => addDays(prev, 7))}
            >
              Tuần sau
            </Button>
            <Button
              size="sm"
              variant="ghost"
              leftIcon={
                <RefreshCw
                  className={`h-3.5 w-3.5 ${
                    scheduleFetching || appsFetching ? "animate-spin" : ""
                  }`}
                />
              }
              onClick={handleRefreshAll}
            >
              Làm mới
            </Button>
          </div>
        </div>

        {/* Week Range Banner */}
        <Card>
          <CardContent className="py-3 px-4 flex flex-wrap items-center justify-between gap-3">
            <div className="text-sm font-semibold text-foreground">
              Tuần từ {formatDayShort(weekStart)}/{weekStart.getFullYear()} đến{" "}
              {formatDayShort(weekEnd)}/{weekEnd.getFullYear()}
            </div>
            <div className="flex items-center gap-2 text-xs text-muted">
              <Badge variant="primary" size="sm">
                Tổng số buổi: {schedules.length}
              </Badge>
              <Badge variant="verified" size="sm">
                Đã lên lịch: {schedules.filter((s) => s.status === "SCHEDULED").length}
              </Badge>
            </div>
          </CardContent>
        </Card>

        {/* Weekly Calendar Grid */}
        {scheduleLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-7 gap-3">
            {Array.from({ length: 7 }).map((_, i) => (
              <Skeleton key={i} className="h-64 rounded-xl" />
            ))}
          </div>
        ) : scheduleError ? (
          <Card>
            <CardContent className="py-10 text-center space-y-2">
              <p className="text-sm font-medium text-danger">
                Không thể tải lịch phỏng vấn doanh nghiệp. Vui lòng kiểm tra kết nối dịch vụ tuyển dụng.
              </p>
              <Button size="sm" variant="outline" onClick={() => void refetchSchedules()}>
                Thử lại
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-7 gap-3">
            {weekDays.map((day, idx) => {
              const key = toDayKey(day);
              const dayItems = schedulesByDay[key] ?? [];
              const isToday = key === todayKey;

              return (
                <Card
                  key={key}
                  className={`flex flex-col min-h-[250px] ${
                    isToday ? "ring-2 ring-primary/40 border-primary" : ""
                  }`}
                >
                  <CardHeader
                    className={`py-2.5 px-3 border-b border-border-subtle ${
                      isToday ? "bg-primary-light/60" : "bg-slate-50/70"
                    }`}
                  >
                    <CardTitle className="text-xs font-bold flex items-center justify-between">
                      <span className={isToday ? "text-primary" : "text-foreground"}>
                        {WEEKDAY_LABELS[idx]}
                      </span>
                      <span className="text-[11px] font-medium text-muted">
                        {formatDayShort(day)}
                      </span>
                    </CardTitle>
                  </CardHeader>

                  <CardContent className="p-2.5 space-y-2.5 flex-1">
                    {dayItems.length === 0 ? (
                      <div className="h-full flex items-center justify-center text-[11px] text-muted/70 italic py-8">
                        Trống lịch
                      </div>
                    ) : (
                      dayItems.map((item) => {
                        const statusCfg = SCHEDULE_STATUS_META[item.status] || {
                          label: item.status,
                          variant: "primary" as const,
                        };
                        return (
                          <div
                            key={item.scheduleId}
                            className={`p-2.5 rounded-lg border text-xs space-y-1.5 transition-shadow hover:shadow-sm ${
                              item.status === "CANCELLED"
                                ? "bg-slate-50 border-border opacity-60"
                                : "bg-surface border-primary/25"
                            }`}
                          >
                            <div className="flex flex-wrap items-center justify-between gap-1">
                              <span className="font-bold text-primary flex items-center gap-1">
                                <Clock className="h-3 w-3 shrink-0" />
                                {formatTime(item.startAt)} - {formatTime(item.endAt)}
                              </span>
                              <Badge variant={statusCfg.variant} size="sm">
                                {statusCfg.label}
                              </Badge>
                            </div>

                            <div className="font-semibold text-foreground leading-snug">
                              Vòng {item.roundNo}: {item.roundName}
                            </div>

                            {item.candidateEmail && (
                              <div className="text-[11px] text-muted flex items-center gap-1 truncate">
                                <Mail className="h-3 w-3 shrink-0" />
                                <span className="truncate">{item.candidateEmail}</span>
                              </div>
                            )}

                            {(item.reminded24hAt || item.reminded1hAt) && (
                              <div className="flex flex-wrap gap-1 pt-0.5">
                                {item.reminded24hAt && (
                                  <span className="inline-flex items-center gap-0.5 text-[10px] px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-700 border border-emerald-200">
                                    <Bell className="h-2.5 w-2.5" /> Đã nhắc 24h
                                  </span>
                                )}
                                {item.reminded1hAt && (
                                  <span className="inline-flex items-center gap-0.5 text-[10px] px-1.5 py-0.5 rounded bg-amber-50 text-amber-700 border border-amber-200">
                                    <Bell className="h-2.5 w-2.5" /> Đã nhắc 1h
                                  </span>
                                )}
                              </div>
                            )}

                            <div className="pt-1.5 border-t border-border-subtle flex flex-wrap items-center justify-between gap-1.5">
                              {item.status === "SCHEDULED" && item.meetUrl && (
                                <a
                                  href={item.meetUrl}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="inline-flex items-center gap-1 text-[11px] font-semibold text-primary hover:underline"
                                >
                                  <Video className="h-3 w-3" />
                                  Phòng họp
                                  <ExternalLink className="h-2.5 w-2.5" />
                                </a>
                              )}
                              <button
                                type="button"
                                onClick={() => navigate(`/hr/applications/${item.recruitmentId}`)}
                                className="inline-flex items-center gap-1 text-[11px] text-muted hover:text-foreground ml-auto"
                              >
                                <FileText className="h-3 w-3" />
                                Chi tiết
                              </button>
                            </div>
                          </div>
                        );
                      })
                    )}
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </section>

      {/* ==================== PHẦN 2: DANH SÁCH ĐƠN ỨNG TUYỂN & OFFER ==================== */}
      <section className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-2 border-t border-border">
          <div>
            <h2 className="text-lg font-bold text-foreground flex items-center gap-2">
              <Briefcase className="h-5 w-5 text-primary" />
              Danh sách chi tiết đơn ứng tuyển & Offer ({totalApplications})
            </h2>
            <p className="text-xs text-muted mt-0.5">
              Chọn bất kỳ đơn ứng tuyển nào để xem vòng phỏng vấn, đề xuất / đổi khung giờ, chấm kết quả hoặc theo dõi trạng thái Offer.
            </p>
          </div>
        </div>

        {/* Status Filter Chips */}
        <div className="flex flex-wrap gap-2">
          {FILTER_TABS.map((tab) => {
            const active = statusFilter === tab.value;
            return (
              <button
                key={tab.value}
                type="button"
                onClick={() => {
                  setStatusFilter(tab.value);
                  setAppPage(0);
                }}
                className={`px-3.5 py-1.5 rounded-full text-xs font-medium border transition-colors ${
                  active
                    ? "bg-primary text-white border-primary shadow-xs"
                    : "bg-surface text-foreground border-border hover:bg-slate-50"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        <Card>
          <CardHeader className="border-b border-border-subtle py-3 px-4">
            <CardTitle className="text-sm font-semibold flex items-center gap-2">
              <FileText className="h-4 w-4 text-primary" />
              Đơn ứng tuyển của doanh nghiệp ({totalApplications})
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {appsLoading ? (
              <div className="p-4 space-y-3">
                <Skeleton className="h-14 rounded-lg" />
                <Skeleton className="h-14 rounded-lg" />
                <Skeleton className="h-14 rounded-lg" />
              </div>
            ) : appsError ? (
              <div className="p-8 text-center space-y-2">
                <p className="text-xs text-danger font-medium">
                  Không thể tải danh sách đơn ứng tuyển. Vui lòng kiểm tra kết nối dịch vụ tuyển dụng.
                </p>
                <Button size="sm" variant="outline" onClick={() => void refetchApps()}>
                  Thử lại
                </Button>
              </div>
            ) : applications.length === 0 ? (
              <div className="p-10 text-center text-xs text-muted">
                Không có đơn ứng tuyển nào khớp bộ lọc trạng thái hiện tại.
              </div>
            ) : (
              <div className="divide-y divide-border-subtle">
                {applications.map((app) => {
                  const statusCfg = APP_STATUS_META[app.status] || {
                    label: app.status,
                    variant: "default" as const,
                  };
                  return (
                    <div
                      key={app.id}
                      className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/70 transition-colors"
                    >
                      <div className="space-y-1 min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-sm font-semibold text-foreground">
                            Đơn #{app.id.substring(0, 8)}
                          </span>
                          <Badge variant={statusCfg.variant} size="sm">
                            {statusCfg.label}
                          </Badge>
                        </div>
                        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted">
                          <span className="flex items-center gap-1">
                            <Mail className="h-3.5 w-3.5" />
                            {app.candidateEmail || "Chưa cập nhật email"}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="h-3.5 w-3.5" />
                            Nộp lúc: {new Date(app.createdAt).toLocaleString("vi-VN")}
                          </span>
                          <span className="text-[11px] font-mono text-muted/80">
                            Job: {app.jobId.substring(0, 8)}...
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <Button
                          size="sm"
                          variant="outline"
                          rightIcon={<ArrowRight className="h-3.5 w-3.5" />}
                          onClick={() => navigate(`/hr/applications/${app.id}`)}
                        >
                          Xem chi tiết & Đặt lịch
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="p-3 border-t border-border-subtle flex items-center justify-between text-xs text-muted">
                <span>
                  Trang {appPage + 1} / {totalPages}
                </span>
                <div className="flex items-center gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={appPage <= 0}
                    onClick={() => setAppPage((p) => Math.max(0, p - 1))}
                  >
                    Trang trước
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={appPage + 1 >= totalPages}
                    onClick={() => setAppPage((p) => p + 1)}
                  >
                    Trang sau
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
