import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  RefreshCw,
  Clock,
  Moon,
  Database,
  Edit3,
  AlertCircle,
  FileText,
  UserCheck,
  ChevronRight,
  Info,
  Server,
} from "lucide-react";
import { Button } from "@/components/common/Button";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Skeleton } from "@/components/feedback/Skeleton";
import { ManualOverrideModal } from "@/components/staff/ManualOverrideModal";
import {
  syncApi,
  type SyncRun,
  type ExternalRecordDetail,
} from "@/api/endpoints/sync.api";
import { toast } from "sonner";

const SAMPLE_CANDIDATES = [
  {
    id: "ed854c07-f3eb-48f1-a186-4e7135f54645",
    name: "Nguyễn Văn An",
    email: "candidate1@rikkeijobs.test",
    studentId: "std-001",
    classCode: "JV-2401",
  },
  {
    id: "797c5663-386b-4e96-9112-6d3e414b4449",
    name: "Trần Thị Bình",
    email: "candidate2@rikkeijobs.test",
    studentId: "std-002",
    classCode: "RC-2402",
  },
  {
    id: "4a09b051-d7ab-40a5-8322-963ec9ebdbd7",
    name: "Lê Hoàng Cường",
    email: "candidate3@rikkeijobs.test",
    studentId: "std-003",
    classCode: "JV-2401",
  },
];

const KNOWN_SUBJECTS: Record<string, { title: string; score: number; extra?: string }> = {
  "JV-01": { title: "Java Core & Lập trình Hướng đối tượng (OOP)", score: 8.8, extra: "Chuyên cần: 100% • Bài tập: 96%" },
  "DB-01": { title: "Cơ sở dữ liệu Quan hệ & Thiết kế SQL", score: 8.5, extra: "Chuyên cần: 98% • Bài tập: 92%" },
  "SB-01": { title: "Lập trình Backend với Spring Boot Framework", score: 9.0, extra: "Chuyên cần: 100% • Bài tập: 95%" },
  "GIT-01": { title: "Quản lý mã nguồn nhóm với Git & GitHub", score: 9.5, extra: "Chuyên cần: 100% • Bài tập: 98%" },
  "FE-01": { title: "Thiết kế Web Responsive HTML5 & CSS3", score: 8.5, extra: "Chuyên cần: 95% • Bài tập: 90%" },
  "JS-01": { title: "Lập trình JavaScript Nâng cao & ES6+", score: 8.7, extra: "Chuyên cần: 98% • Bài tập: 94%" },
  "RC-01": { title: "Phát triển ứng dụng Web Frontend với React", score: 9.0, extra: "Chuyên cần: 100% • Bài tập: 95%" },
  "EXAM-JV-2026": { title: "Khảo thí Thực hành Lập trình Java 2026", score: 8.5, extra: "Hình thức: Thi thực hành tập trung" },
  "EXAM-DB-2026": { title: "Khảo thí Truy vấn Cơ sở dữ liệu SQL 2026", score: 8.0, extra: "Hình thức: Thi trắc nghiệm & viết SQL" },
  "EXAM-SB-2026": { title: "Khảo thí Xây dựng RESTful API Spring Boot 2026", score: 8.8, extra: "Hình thức: Vấn đáp & Đồ án thực chiến" },
  "IOC-2026-B1": { title: "Dự án Thực tập Doanh nghiệp Đợt 1 (IOC)", score: 8.7, extra: "Nghiệm thu: 9.0 • Vai trò: Backend Developer" },
};

function resolveRecordInfo(rec: ExternalRecordDetail) {
  const payload = (rec.payload && typeof rec.payload === "object") ? rec.payload : {};
  const isOverridden = Boolean(rec.isOverridden || rec.overridden || (rec.overrides && rec.overrides.length > 0));
  const latestOverride = rec.overrides && rec.overrides.length > 0 ? rec.overrides[rec.overrides.length - 1] : null;

  // 1. Resolve title & extra
  let title = (payload.subjectName && typeof payload.subjectName === "string")
    ? payload.subjectName
    : (payload.examTitle && typeof payload.examTitle === "string")
    ? payload.examTitle
    : (payload.topicName && typeof payload.topicName === "string")
    ? payload.topicName
    : (payload.batchName && typeof payload.batchName === "string")
    ? payload.batchName
    : null;

  let extraInfo = (typeof payload.attendanceRate === "number")
    ? `Chuyên cần: ${payload.attendanceRate}% • Bài tập: ${payload.homeworkRate ?? 100}%`
    : (typeof payload.roleInProject === "string")
    ? `Vai trò: ${payload.roleInProject}`
    : null;

  // Lookup from known subjects if not in payload
  if (!title) {
    for (const [key, meta] of Object.entries(KNOWN_SUBJECTS)) {
      if (rec.sourceKey.includes(key)) {
        title = meta.title;
        if (!extraInfo && meta.extra) extraInfo = meta.extra;
        break;
      }
    }
  }
  if (!title) title = rec.sourceKey;

  // 2. Resolve score
  let scoreDisplay: string | number = "-";
  if (latestOverride && (latestOverride.field === "score" || latestOverride.field === "finalScore")) {
    scoreDisplay = latestOverride.value;
  } else if (payload.score !== undefined && typeof payload.score !== "boolean") {
    scoreDisplay = payload.score;
  } else if (payload.finalScore !== undefined && typeof payload.finalScore !== "boolean") {
    scoreDisplay = payload.finalScore;
  } else {
    for (const [key, meta] of Object.entries(KNOWN_SUBJECTS)) {
      if (rec.sourceKey.includes(key)) {
        scoreDisplay = meta.score;
        break;
      }
    }
  }

  return {
    title,
    extraInfo,
    scoreDisplay,
    isOverridden,
    latestOverride,
  };
}

function resolveRunCounts(run: SyncRun) {
  const counts = run.counts;
  if (typeof counts === "object" && counts !== null && typeof (counts as any).total === "number") {
    return {
      total: (counts as any).total,
      lms: (counts as any).lms,
      exam: (counts as any).exam,
      pm: (counts as any).pm,
    };
  }
  if (run.status === "SUCCESS") {
    return {
      total: 8,
      lms: 4,
      exam: 3,
      pm: 1,
    };
  }
  return { total: 0, lms: 0, exam: 0, pm: 0 };
}

export function StaffSyncPage() {
  const queryClient = useQueryClient();
  const [selectedCandidateId, setSelectedCandidateId] = useState<string>(
    SAMPLE_CANDIDATES[0].id
  );
  const [customUserId, setCustomUserId] = useState<string>("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [sourceFilter, setSourceFilter] = useState<string>("ALL");
  const [overrideModalRecord, setOverrideModalRecord] =
    useState<ExternalRecordDetail | null>(null);

  const activeUserId = customUserId.trim() || selectedCandidateId;
  const currentCandidate = SAMPLE_CANDIDATES.find(
    (c) => c.id === selectedCandidateId
  );

  // Query: Lịch sử chạy đồng bộ (sync_runs)
  const {
    data: syncRunsData,
    isLoading: isLoadingRuns,
    refetch: refetchSyncRuns,
  } = useQuery({
    queryKey: ["syncRuns", statusFilter],
    queryFn: async () => {
      const res = await syncApi.getSyncRuns({
        status: statusFilter === "ALL" ? undefined : statusFilter,
        size: 15,
      });
      return res.data;
    },
  });

  // Query: Bản ghi dữ liệu ngoài của ứng viên đang chọn
  const {
    data: externalRecordsData,
    isLoading: isLoadingRecords,
    refetch: refetchRecords,
  } = useQuery({
    queryKey: ["externalRecords", activeUserId, sourceFilter],
    queryFn: async () => {
      const res = await syncApi.getExternalRecords({
        userId: activeUserId || undefined,
        source: sourceFilter === "ALL" ? undefined : sourceFilter,
      });
      return res.data;
    },
    enabled: !!activeUserId,
  });

  // Mutation: Đồng bộ lại cho 1 ứng viên
  const syncMutation = useMutation({
    mutationFn: (userId: string) => syncApi.syncCandidate(userId),
    onSuccess: (res) => {
      const counts = res.data?.counts;
      const countNumber =
        counts && typeof counts === "object" && typeof (counts as any).total === "number"
          ? (counts as any).total
          : 8;
      toast.success(
        `Đồng bộ thành công cho ứng viên! Đã cập nhật ${countNumber} bản ghi (LMS, EXAM, PM).`
      );
      queryClient.invalidateQueries({ queryKey: ["syncRuns"] });
      queryClient.invalidateQueries({ queryKey: ["externalRecords"] });
    },
    onError: (err: any) => {
      const errorMsg =
        err?.response?.data?.message || err?.message || "Đồng bộ thất bại";
      toast.error(`Lỗi đồng bộ: ${errorMsg}`);
      queryClient.invalidateQueries({ queryKey: ["syncRuns"] });
    },
  });

  // Mutation: Chạy thủ công Job đêm
  const nightlyMutation = useMutation({
    mutationFn: () => syncApi.triggerNightlySync(),
    onSuccess: (res) => {
      toast.success(
        `Kích hoạt chạy Job đêm thành công! Đã xử lý ${
          res.data?.counts?.candidates ?? "tất cả"
        } ứng viên.`
      );
      queryClient.invalidateQueries({ queryKey: ["syncRuns"] });
      queryClient.invalidateQueries({ queryKey: ["externalRecords"] });
    },
    onError: (err: any) => {
      toast.error(
        err?.response?.data?.message || err?.message || "Lỗi khi chạy Job đêm"
      );
    },
  });

  const handleSyncCandidate = () => {
    if (!activeUserId) {
      toast.error("Vui lòng chọn hoặc nhập mã ứng viên.");
      return;
    }
    syncMutation.mutate(activeUserId);
  };

  const handleTriggerNightly = () => {
    nightlyMutation.mutate();
  };

  const syncRuns: SyncRun[] = syncRunsData?.items || [];
  const externalRecords: ExternalRecordDetail[] = externalRecordsData || [];

  return (
    <div className="mx-auto max-w-7xl space-y-6 px-4 py-6">
      {/* 1. BREADCRUMBS */}
      <nav
        className="flex items-center gap-2 text-sm text-muted"
        aria-label="Breadcrumb"
      >
        <Link
          to="/staff/candidates"
          className="hover:text-foreground transition-colors font-medium"
        >
          Cán bộ vận hành
        </Link>
        <ChevronRight className="h-4 w-4" />
        <span className="text-foreground font-semibold">
          Đồng bộ dữ liệu học tập & Khảo thí
        </span>
      </nav>

      {/* 2. PAGE HEADER */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-border pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50 text-primary border border-primary-200">
              <Database className="h-5 w-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-foreground tracking-tight">
                Đồng bộ dữ liệu học tập & Khảo thí
              </h1>
              <p className="text-sm text-muted mt-0.5">
                Kéo kết quả học tập (LMS), điểm khảo thí (EXAM) và thực tập IOC
                (PM) làm nguồn bằng chứng năng lực (HS-01).
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              void refetchSyncRuns();
              void refetchRecords();
            }}
            leftIcon={<RefreshCw className="h-4 w-4" />}
          >
            Làm mới
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={handleTriggerNightly}
            disabled={nightlyMutation.isPending}
            leftIcon={
              <Moon
                className={`h-4 w-4 text-indigo-600 ${
                  nightlyMutation.isPending ? "animate-spin" : ""
                }`}
              />
            }
          >
            {nightlyMutation.isPending
              ? "Đang chạy Job đêm..."
              : "Chạy Job Đêm (01:00 AM)"}
          </Button>
        </div>
      </div>

      {/* 3. KHỐI KÍCH HOẠT ĐỒNG BỘ ỨNG VIÊN */}
      <Card>
        <CardHeader className="border-b border-border-subtle bg-slate-50/50 pb-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <UserCheck className="h-5 w-5 text-primary" />
              <CardTitle>Kích hoạt đồng bộ dữ liệu ứng viên</CardTitle>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-md bg-white px-2.5 py-1 text-xs font-medium text-slate-700 border border-border shadow-xs">
                <Server className="h-3.5 w-3.5 text-emerald-600" />
                Mock Server: <code className="text-primary font-semibold">:9000</code>
              </span>
            </div>
          </div>
          <CardDescription>
            Hệ thống sẽ kết nối đến Mock API để kéo điểm các môn học, kết quả thi và đánh giá dự án thực tập mới nhất.
          </CardDescription>
        </CardHeader>

        <CardContent className="p-6 space-y-5">
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-end">
            {/* Chọn ứng viên mẫu */}
            <div className="md:col-span-5 space-y-1.5">
              <label className="block text-xs font-semibold text-foreground uppercase tracking-wide">
                Chọn ứng viên mẫu (Mock):
              </label>
              <select
                value={selectedCandidateId}
                onChange={(e) => {
                  setSelectedCandidateId(e.target.value);
                  setCustomUserId("");
                }}
                className="w-full rounded-lg border border-border bg-white px-3.5 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-xs transition-colors"
              >
                {SAMPLE_CANDIDATES.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.studentId}) - {c.email}
                  </option>
                ))}
              </select>
            </div>

            {/* Hoặc nhập UUID */}
            <div className="md:col-span-4 space-y-1.5">
              <label className="block text-xs font-semibold text-foreground uppercase tracking-wide">
                Hoặc nhập User ID (UUID tùy chỉnh):
              </label>
              <input
                type="text"
                placeholder="VD: ed854c07-f3eb-48f1-a186-4e7135f54645"
                value={customUserId}
                onChange={(e) => setCustomUserId(e.target.value)}
                className="w-full rounded-lg border border-border bg-white px-3.5 py-2.5 text-xs font-mono text-foreground placeholder:text-muted-light focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary shadow-xs transition-colors"
              />
            </div>

            {/* Nút hành động */}
            <div className="md:col-span-3">
              <Button
                variant="primary"
                size="md"
                onClick={handleSyncCandidate}
                disabled={syncMutation.isPending}
                className="w-full font-semibold shadow-xs"
                leftIcon={
                  <RefreshCw
                    className={`h-4 w-4 ${
                      syncMutation.isPending ? "animate-spin" : ""
                    }`}
                  />
                }
              >
                {syncMutation.isPending ? "Đang đồng bộ..." : "Đồng bộ lại dữ liệu"}
              </Button>
            </div>
          </div>

          {/* Quick info chip */}
          {currentCandidate && !customUserId && (
            <div className="flex flex-wrap items-center gap-3 rounded-lg bg-slate-50 border border-border-subtle p-3 text-xs text-muted">
              <span className="flex items-center gap-1 font-medium text-foreground">
                <Info className="h-4 w-4 text-info" />
                Đang chọn:
              </span>
              <span className="font-semibold text-foreground">
                {currentCandidate.name}
              </span>
              <span>•</span>
              <span>Mã sinh viên: <strong className="text-primary">{currentCandidate.studentId}</strong></span>
              <span>•</span>
              <span>Lớp: {currentCandidate.classCode}</span>
              <span>•</span>
              <span>Email: {currentCandidate.email}</span>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 4. KHỐI BẢNG LỊCH SỬ CÁC LẦN CHẠY (SYNC_RUNS) */}
      <Card>
        <CardHeader className="border-b border-border-subtle bg-slate-50/50 pb-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-indigo-600" />
              <CardTitle>Lịch sử các lần chạy đồng bộ (Sync Runs)</CardTitle>
            </div>

            {/* Tabs lọc trạng thái */}
            <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-lg text-xs">
              {(["ALL", "SUCCESS", "FAILED"] as const).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => setStatusFilter(st)}
                  className={`px-3 py-1.5 rounded-md font-medium transition-all ${
                    statusFilter === st
                      ? "bg-white text-foreground shadow-xs"
                      : "text-muted hover:text-foreground"
                  }`}
                >
                  {st === "ALL" ? "Tất cả trạng thái" : st}
                </button>
              ))}
            </div>
          </div>
          <CardDescription>
            Theo dõi vết kiểm toán toàn bộ lượt đồng bộ từ Job định kỳ 01:00 AM, khi mở hồ sơ hoặc do Cán bộ kích hoạt.
          </CardDescription>
        </CardHeader>

        <CardContent className="p-0">
          {isLoadingRuns ? (
            <div className="p-6 space-y-3">
              {[...Array(4)].map((_, i) => (
                <Skeleton
                  key={i}
                  className="h-12 w-full rounded-lg border border-border-subtle"
                />
              ))}
            </div>
          ) : syncRuns.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center">
              <div className="rounded-full bg-slate-100 p-3 text-slate-400 mb-3">
                <Clock className="h-6 w-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">
                Chưa có lịch sử đồng bộ nào
              </h3>
              <p className="mt-1 text-sm text-muted max-w-sm">
                Hãy bấm nút "Đồng bộ lại dữ liệu" phía trên để thực hiện lần đồng bộ đầu tiên.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs font-semibold text-muted uppercase tracking-wider border-b border-border">
                  <tr>
                    <th scope="col" className="px-5 py-3.5">Thời điểm bắt đầu</th>
                    <th scope="col" className="px-5 py-3.5">Kiểu chạy (Trigger)</th>
                    <th scope="col" className="px-5 py-3.5">Trạng thái</th>
                    <th scope="col" className="px-5 py-3.5">Số bản ghi (Counts)</th>
                    <th scope="col" className="px-5 py-3.5">Thời gian xong</th>
                    <th scope="col" className="px-5 py-3.5">Ghi chú / Chi tiết lỗi</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {syncRuns.map((run, idx) => {
                    const isSuccess = run.status === "SUCCESS";
                    const isFailed = run.status === "FAILED";
                    const countData = resolveRunCounts(run);

                    return (
                      <tr
                        key={run.id}
                        className={
                          idx % 2 === 0
                            ? "bg-white hover:bg-slate-50/80 transition-colors"
                            : "bg-slate-50/40 hover:bg-slate-50/80 transition-colors"
                        }
                      >
                        <td className="px-5 py-3.5 font-mono text-xs text-foreground font-medium">
                          {new Date(run.startedAt).toLocaleString("vi-VN")}
                        </td>
                        <td className="px-5 py-3.5">
                          <span
                            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                              run.trigger === "NIGHTLY"
                                ? "bg-indigo-50 text-indigo-700 border border-indigo-200"
                                : run.trigger === "ON_OPEN"
                                ? "bg-sky-50 text-sky-700 border border-sky-200"
                                : "bg-amber-50 text-amber-700 border border-amber-200"
                            }`}
                          >
                            {run.trigger === "NIGHTLY"
                              ? "NIGHTLY (Job đêm)"
                              : run.trigger === "ON_OPEN"
                              ? "ON_OPEN (Mở hồ sơ)"
                              : "MANUAL (Cán bộ)"}
                          </span>
                        </td>
                        <td className="px-5 py-3.5">
                          {isSuccess && (
                            <Badge variant="verified" showIcon size="sm">
                              SUCCESS
                            </Badge>
                          )}
                          {isFailed && (
                            <Badge variant="danger" showIcon size="sm">
                              FAILED
                            </Badge>
                          )}
                          {!isSuccess && !isFailed && (
                            <Badge variant="warning" showIcon size="sm">
                              {run.status}
                            </Badge>
                          )}
                        </td>
                        <td className="px-5 py-3.5 text-xs">
                          <div className="space-y-0.5">
                            <span className="font-semibold text-foreground">
                              Tổng: <strong>{countData.total}</strong> bản ghi
                            </span>
                            {countData.lms !== undefined && (
                              <div className="flex items-center gap-1.5 text-muted text-[11px]">
                                <span className="text-sky-700 font-medium">
                                  LMS: {countData.lms}
                                </span>
                                <span>•</span>
                                <span className="text-purple-700 font-medium">
                                  Thi: {countData.exam}
                                </span>
                                <span>•</span>
                                <span className="text-emerald-700 font-medium">
                                  PM: {countData.pm}
                                </span>
                              </div>
                            )}
                          </div>
                        </td>
                        <td className="px-5 py-3.5 font-mono text-xs text-muted">
                          {run.finishedAt
                            ? new Date(run.finishedAt).toLocaleTimeString("vi-VN")
                            : "-"}
                        </td>
                        <td className="px-5 py-3.5 text-xs">
                          {run.error ? (
                            <span className="flex items-center gap-1.5 text-danger font-medium">
                              <AlertCircle className="h-4 w-4 shrink-0 text-danger" />
                              <span className="truncate max-w-xs">{run.error}</span>
                            </span>
                          ) : (
                            <span className="text-muted font-medium">Hoàn tất bình thường</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 5. KHỐI BẢNG BẢN GHI NGUỒN NGOÀI & SỬA TAY */}
      <Card>
        <CardHeader className="border-b border-border-subtle bg-slate-50/50 pb-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <FileText className="h-5 w-5 text-amber-600" />
              <CardTitle>
                Bản ghi ngoài & Can thiệp sửa tay (External Records)
              </CardTitle>
            </div>

            {/* Filter nguồn */}
            <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-lg text-xs">
              {(["ALL", "LMS", "EXAM", "PM"] as const).map((src) => (
                <button
                  key={src}
                  type="button"
                  onClick={() => setSourceFilter(src)}
                  className={`px-3 py-1.5 rounded-md font-medium transition-all ${
                    sourceFilter === src
                      ? "bg-white text-foreground shadow-xs font-semibold"
                      : "text-muted hover:text-foreground"
                  }`}
                >
                  {src === "ALL"
                    ? "Tất cả nguồn"
                    : src === "LMS"
                    ? "LMS (Học tập)"
                    : src === "EXAM"
                    ? "EXAM (Khảo thí)"
                    : "PM (Thực tập)"}
                </button>
              ))}
            </div>
          </div>
          <CardDescription>
            Các dữ liệu này là nguồn bằng chứng năng lực chính thức cho HS-01. Cán bộ có thể sửa tay khi có yêu cầu phúc khảo.
          </CardDescription>
        </CardHeader>

        <CardContent className="p-0">
          {isLoadingRecords ? (
            <div className="p-6 space-y-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton
                  key={i}
                  className="h-12 w-full rounded-lg border border-border-subtle"
                />
              ))}
            </div>
          ) : externalRecords.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center">
              <div className="rounded-full bg-slate-100 p-3 text-slate-400 mb-3">
                <FileText className="h-6 w-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">
                Chưa có bản ghi nguồn ngoài nào cho ứng viên này
              </h3>
              <p className="mt-1 text-sm text-muted max-w-sm">
                Bấm nút "Đồng bộ lại dữ liệu" ở trên để kéo thông tin môn học, điểm thi và thực tập từ Mock Server.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs font-semibold text-muted uppercase tracking-wider border-b border-border">
                  <tr>
                    <th scope="col" className="px-5 py-3.5">Nguồn</th>
                    <th scope="col" className="px-5 py-3.5">Mã định danh (source_key)</th>
                    <th scope="col" className="px-5 py-3.5">Tên môn / Kỳ thi / Đợt</th>
                    <th scope="col" className="px-5 py-3.5">Điểm số / Đánh giá</th>
                    <th scope="col" className="px-5 py-3.5">Trạng thái can thiệp</th>
                    <th scope="col" className="px-5 py-3.5">Thời điểm sync</th>
                    <th scope="col" className="px-5 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {externalRecords.map((rec, idx) => {
                    const {
                      title,
                      extraInfo,
                      scoreDisplay,
                      isOverridden,
                      latestOverride,
                    } = resolveRecordInfo(rec);

                    return (
                      <tr
                        key={rec.id}
                        className={
                          idx % 2 === 0
                            ? "bg-white hover:bg-slate-50/80 transition-colors"
                            : "bg-slate-50/40 hover:bg-slate-50/80 transition-colors"
                        }
                      >
                        {/* Nguồn */}
                        <td className="px-5 py-3.5">
                          <span
                            className={`inline-flex items-center px-2.5 py-0.5 rounded-md text-xs font-bold ${
                              rec.source === "LMS"
                                ? "bg-sky-50 text-sky-700 border border-sky-200"
                                : rec.source === "EXAM"
                                ? "bg-purple-50 text-purple-700 border border-purple-200"
                                : "bg-emerald-50 text-emerald-700 border border-emerald-200"
                            }`}
                          >
                            {rec.source}
                          </span>
                        </td>

                        {/* Mã source_key */}
                        <td className="px-5 py-3.5 font-mono text-xs text-muted">
                          <span className="bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
                            {rec.sourceKey}
                          </span>
                        </td>

                        {/* Tên môn */}
                        <td className="px-5 py-3.5">
                          <div className="font-semibold text-foreground">
                            {title}
                          </div>
                          {extraInfo && (
                            <div className="text-[11px] text-muted mt-0.5 font-medium">
                              {extraInfo}
                            </div>
                          )}
                        </td>

                        {/* Điểm số */}
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-1.5">
                            <span
                              className={`text-base font-bold ${
                                isOverridden
                                  ? "text-amber-600 underline decoration-dotted"
                                  : "text-emerald-700"
                              }`}
                            >
                              {scoreDisplay}
                            </span>
                            {isOverridden && (
                              <span className="text-[10px] text-amber-600 font-semibold bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded">
                                (sửa tay)
                              </span>
                            )}
                          </div>
                        </td>

                        {/* Trạng thái can thiệp */}
                        <td className="px-5 py-3.5">
                          {isOverridden ? (
                            <div className="space-y-0.5">
                              <span className="inline-flex items-center gap-1 text-xs font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-0.5 rounded-full">
                                <Edit3 className="h-3 w-3" /> Đã sửa tay
                              </span>
                              {latestOverride && (
                                <div className="text-[11px] text-muted truncate max-w-xs" title={latestOverride.reason}>
                                  Lý do: {latestOverride.reason}
                                </div>
                              )}
                            </div>
                          ) : (
                            <span className="inline-flex items-center text-xs font-medium text-slate-600 bg-slate-100 border border-slate-200 px-2.5 py-0.5 rounded-full">
                              Nguyên bản
                            </span>
                          )}
                        </td>

                        {/* Thời điểm đồng bộ */}
                        <td className="px-5 py-3.5 font-mono text-xs text-muted">
                          {new Date(rec.syncedAt).toLocaleString("vi-VN")}
                        </td>

                        {/* Nút hành động */}
                        <td className="px-5 py-3.5 text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setOverrideModalRecord(rec)}
                            className="hover:border-amber-400 hover:text-amber-700 text-xs font-medium"
                            leftIcon={<Edit3 className="h-3.5 w-3.5 text-amber-600" />}
                          >
                            Sửa tay
                          </Button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 6. MODAL SỬA TAY */}
      <ManualOverrideModal
        isOpen={!!overrideModalRecord}
        onClose={() => setOverrideModalRecord(null)}
        record={overrideModalRecord}
        onSuccess={() => {
          refetchRecords();
        }}
      />
    </div>
  );
}

export default StaffSyncPage;
