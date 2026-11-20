import React, { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  FileText,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  Briefcase,
  Building2,
  Calendar,
  History,
  ArrowRight,
} from "lucide-react";
import { toast } from "sonner";
import {
  recruitmentApi,
  type CandidateRecruitmentSummary,
  type CandidateRecruitmentDetails,
  type RecruitmentStatus,
} from "../../api/endpoints/recruitment.api";
import { Card, CardContent } from "../../components/common/Card";
import { Badge } from "../../components/common/Badge";
import { Button } from "../../components/common/Button";
import { Modal } from "../../components/common/Modal";

// ─────────────────────────── Status config ──────────────────────────────────

const STATUS_STEPS: RecruitmentStatus[] = [
  "SUBMITTED",
  "SCREENING",
  "INTERVIEW",
  "OFFER",
  "HIRED",
];

type StatusMeta = {
  label: string;
  color: string;
  bg: string;
  badgeVariant: "default" | "verified" | "warning" | "danger" | "outline" | "primary" | "self";
  icon: React.ComponentType<{ className?: string }>;
};

const STATUS_META: Record<RecruitmentStatus, StatusMeta> = {
  SUBMITTED: {
    label: "Đã gửi",
    color: "text-blue-700",
    bg: "bg-blue-50 border-blue-200",
    badgeVariant: "default",
    icon: FileText,
  },
  SCREENING: {
    label: "Sàng lọc",
    color: "text-amber-700",
    bg: "bg-amber-50 border-amber-200",
    badgeVariant: "warning",
    icon: Clock,
  },
  INTERVIEW: {
    label: "Phỏng vấn",
    color: "text-purple-700",
    bg: "bg-purple-50 border-purple-200",
    badgeVariant: "primary",
    icon: Calendar,
  },
  OFFER: {
    label: "Nhận offer",
    color: "text-teal-700",
    bg: "bg-teal-50 border-teal-200",
    badgeVariant: "verified",
    icon: CheckCircle2,
  },
  HIRED: {
    label: "Nhận việc",
    color: "text-emerald-700",
    bg: "bg-emerald-50 border-emerald-200",
    badgeVariant: "verified",
    icon: CheckCircle2,
  },
  REJECTED: {
    label: "Không đạt",
    color: "text-red-700",
    bg: "bg-red-50 border-red-200",
    badgeVariant: "danger",
    icon: XCircle,
  },
  WITHDRAWN: {
    label: "Đã rút",
    color: "text-slate-600",
    bg: "bg-slate-50 border-slate-200",
    badgeVariant: "outline",
    icon: AlertCircle,
  },
};

// ─────────────────────────── Progress Bar ───────────────────────────────────

function ProgressBar({ status }: { status: RecruitmentStatus }) {
  if (status === "REJECTED" || status === "WITHDRAWN") {
    const meta = STATUS_META[status];
    const Icon = meta.icon;
    return (
      <div className={`flex items-center gap-2 text-xs font-medium px-3 py-2 rounded-lg border ${meta.bg} ${meta.color}`}>
        <Icon className="h-4 w-4 shrink-0" />
        <span>{meta.label} — Hồ sơ đã kết thúc.</span>
      </div>
    );
  }

  const currentIdx = STATUS_STEPS.indexOf(status);

  return (
    <div className="flex items-center gap-0.5 w-full">
      {STATUS_STEPS.map((step, idx) => {
        const meta = STATUS_META[step];
        const isActive = idx === currentIdx;
        const isDone = idx < currentIdx;
        const Icon = meta.icon;

        return (
          <React.Fragment key={step}>
            <div className="flex flex-col items-center gap-1 flex-1 min-w-0">
              <div
                className={`flex items-center justify-center h-7 w-7 rounded-full border-2 transition-all ${
                  isDone
                    ? "bg-primary border-primary text-white"
                    : isActive
                    ? "bg-white border-primary text-primary shadow-sm"
                    : "bg-slate-100 border-slate-200 text-slate-400"
                }`}
              >
                {isDone ? (
                  <CheckCircle2 className="h-4 w-4" />
                ) : (
                  <Icon className="h-3.5 w-3.5" />
                )}
              </div>
              <span
                className={`text-[10px] font-medium text-center leading-tight ${
                  isActive ? "text-primary" : isDone ? "text-slate-600" : "text-slate-400"
                }`}
              >
                {meta.label}
              </span>
            </div>
            {idx < STATUS_STEPS.length - 1 && (
              <div
                className={`h-0.5 flex-1 mt-[-14px] transition-all ${
                  idx < currentIdx ? "bg-primary" : "bg-slate-200"
                }`}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}

// ─────────────────────────── History Timeline ────────────────────────────────

function HistoryTimeline({ history }: { history: CandidateRecruitmentDetails["history"] }) {
  if (!history || history.length === 0) {
    return <p className="text-xs text-muted italic">Chưa có lịch sử thay đổi.</p>;
  }
  return (
    <div className="space-y-2">
      {[...history].reverse().map((h) => {
        const toMeta = STATUS_META[h.toStatus];
        const Icon = toMeta.icon;
        return (
          <div key={h.id} className="flex items-start gap-2.5">
            <div className={`mt-0.5 flex items-center justify-center h-6 w-6 rounded-full border shrink-0 ${toMeta.bg} ${toMeta.color}`}>
              <Icon className="h-3 w-3" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className={`text-xs font-semibold ${toMeta.color}`}>{toMeta.label}</span>
                {h.fromStatus && (
                  <span className="text-xs text-muted">
                    từ <span className="font-medium">{STATUS_META[h.fromStatus]?.label ?? h.fromStatus}</span>
                  </span>
                )}
                <span className="text-[10px] text-muted ml-auto">
                  {new Date(h.createdAt).toLocaleString("vi-VN")}
                </span>
              </div>
              {h.note && <p className="text-xs text-muted mt-0.5 italic">{h.note}</p>}
            </div>
          </div>
        );
      })}
    </div>
  );
}

// ─────────────────────────── Detail Modal ───────────────────────────────────

function DetailModal({
  applicationId,
  onClose,
  onWithdraw,
}: {
  applicationId: string;
  onClose: () => void;
  onWithdraw: (id: string) => void;
}) {
  const { data: res, isLoading } = useQuery({
    queryKey: ["my-recruitment-detail", applicationId],
    queryFn: () => recruitmentApi.getMyRecruitmentDetail(applicationId),
    enabled: !!applicationId,
  });

  const rawData = res?.data as any;
  const detail = rawData?.recruitment
    ? {
        ...rawData.recruitment,
        history: rawData.history || [],
      }
    : rawData;

  const canWithdraw =
    detail &&
    detail.status &&
    !["OFFER", "HIRED", "REJECTED", "WITHDRAWN"].includes(detail.status);

  return (
    <Modal
      isOpen={!!applicationId}
      onClose={onClose}
      title="Chi tiết đơn ứng tuyển"
      description={detail?.createdAt ? `Nộp ngày ${new Date(detail.createdAt).toLocaleDateString("vi-VN")}` : ""}
      size="lg"
      footer={
        <div className="flex items-center justify-between w-full gap-2">
          <div>
            {canWithdraw && (
              <Button
                variant="outline"
                size="sm"
                className="text-red-600 border-red-200 hover:bg-red-50"
                onClick={() => onWithdraw(applicationId)}
                leftIcon={<XCircle className="h-4 w-4" />}
              >
                Rút đơn
              </Button>
            )}
          </div>
          <Button variant="outline" size="sm" onClick={onClose}>
            Đóng
          </Button>
        </div>
      }
    >
      {isLoading ? (
        <div className="space-y-3 animate-pulse">
          <div className="h-14 bg-slate-100 rounded-lg" />
          <div className="h-20 bg-slate-100 rounded-lg" />
          <div className="h-32 bg-slate-100 rounded-lg" />
        </div>
      ) : detail && detail.id ? (
        <div className="space-y-5">
          {/* Progress */}
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-muted mb-3">
              Tiến trình hồ sơ
            </p>
            <ProgressBar status={detail.status || "SUBMITTED"} />
          </div>

          {/* Info grid */}
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
              <span className="text-muted block mb-0.5">Mã đơn</span>
              <span className="font-mono font-semibold text-slate-700">{detail.id ? `${String(detail.id).slice(0, 8)}…` : "—"}</span>
            </div>
            <div className="p-3 rounded-lg bg-slate-50 border border-slate-200">
              <span className="text-muted block mb-0.5">Cập nhật lần cuối</span>
              <span className="font-semibold">{detail.updatedAt ? new Date(detail.updatedAt).toLocaleString("vi-VN") : "—"}</span>
            </div>
          </div>

          {/* Cover letter */}
          {detail.coverLetter && (
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-muted mb-1.5">
                Thư giới thiệu
              </p>
              <div className="text-xs text-slate-700 leading-relaxed whitespace-pre-line p-3 rounded-lg bg-slate-50 border border-slate-200">
                {detail.coverLetter}
              </div>
            </div>
          )}

          {/* CV snapshot */}
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-muted mb-1.5">
              CV đã nộp
            </p>
            {detail.cvSnapshotUrl ? (
              <a
                href={recruitmentApi.getCvSnapshotUrl(detail.id)}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
              >
                <FileText className="h-3.5 w-3.5" />
                Xem bản CV đã nộp
              </a>
            ) : (
              <span className="text-xs text-muted italic">Chưa có bản snapshot CV đính kèm</span>
            )}
          </div>

          {/* History */}
          <div>
            <div className="flex items-center gap-1.5 mb-2">
              <History className="h-4 w-4 text-muted" />
              <p className="text-xs font-semibold uppercase tracking-wider text-muted">
                Lịch sử trạng thái
              </p>
            </div>
            <HistoryTimeline history={detail.history} />
          </div>
        </div>
      ) : (
        <p className="text-sm text-muted text-center py-4">Không tải được chi tiết đơn.</p>
      )}
    </Modal>
  );
}

// ─────────────────────────── Withdraw Modal ─────────────────────────────────

function WithdrawModal({
  applicationId,
  onClose,
  onConfirm,
  isPending,
}: {
  applicationId: string | null;
  onClose: () => void;
  onConfirm: (id: string, reason: string) => void;
  isPending: boolean;
}) {
  const [reason, setReason] = useState("");

  return (
    <Modal
      isOpen={!!applicationId}
      onClose={onClose}
      title="Rút đơn ứng tuyển"
      description="Hành động này không thể hoàn tác. Bạn vẫn có thể nộp lại sau."
      size="sm"
      footer={
        <div className="flex items-center justify-end gap-2 w-full">
          <Button variant="outline" size="sm" onClick={onClose} disabled={isPending}>
            Hủy
          </Button>
          <Button
            variant="primary"
            size="sm"
            className="bg-red-600 hover:bg-red-700"
            onClick={() => applicationId && onConfirm(applicationId, reason)}
            isLoading={isPending}
          >
            Xác nhận rút
          </Button>
        </div>
      }
    >
      <div className="space-y-3">
        <label className="block text-xs font-medium text-foreground">
          Lý do rút đơn (tuỳ chọn)
        </label>
        <textarea
          className="w-full border border-border rounded-lg px-3 py-2 text-sm text-foreground bg-surface resize-none focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary"
          rows={3}
          placeholder="Nhập lý do nếu muốn..."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </div>
    </Modal>
  );
}

// ─────────────────────────── Application Card ───────────────────────────────

function ApplicationCard({
  app,
  onViewDetail,
  onWithdraw,
}: {
  app: CandidateRecruitmentSummary;
  onViewDetail: (id: string) => void;
  onWithdraw: (id: string) => void;
}) {
  const meta = STATUS_META[app.status];
  const Icon = meta.icon;
  const canWithdraw = !["OFFER", "HIRED", "REJECTED", "WITHDRAWN"].includes(app.status);

  return (
    <Card hoverable className="border-border">
      <CardContent className="p-5">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          {/* Left info */}
          <div className="space-y-2 flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <Badge variant={meta.badgeVariant} size="sm">
                <Icon className="h-3 w-3 mr-1" />
                {meta.label}
              </Badge>
              <span className="text-[10px] text-muted font-mono">
                #{app.id ? app.id.slice(0, 8) : "—"}
              </span>
            </div>

            {/* Job & Company */}
            <div>
              <h3 className="text-sm font-semibold text-foreground flex items-center gap-1.5">
                <Briefcase className="h-4 w-4 text-primary shrink-0" />
                {app.jobTitle ?? <span className="text-muted italic">Mã JD: {app.jobId ? `${app.jobId.slice(0, 8)}…` : "—"}</span>}
              </h3>
              {app.companyName && (
                <p className="text-xs text-muted flex items-center gap-1 mt-0.5">
                  <Building2 className="h-3 w-3" />
                  {app.companyName}
                </p>
              )}
            </div>

            {/* Progress */}
            <div className="pt-1">
              <ProgressBar status={app.status} />
            </div>

            {/* Date */}
            <p className="text-[11px] text-muted flex items-center gap-1">
              <Clock className="h-3 w-3" />
              Nộp ngày {new Date(app.createdAt).toLocaleDateString("vi-VN")}
              {app.updatedAt !== app.createdAt && (
                <span className="ml-1">
                  · Cập nhật {new Date(app.updatedAt).toLocaleDateString("vi-VN")}
                </span>
              )}
            </p>
          </div>

          {/* Actions */}
          <div className="flex sm:flex-col items-center sm:items-end gap-2 shrink-0">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onViewDetail(app.id)}
              rightIcon={<ArrowRight className="h-3.5 w-3.5" />}
              className="text-xs"
            >
              Chi tiết
            </Button>
            {canWithdraw && (
              <Button
                variant="outline"
                size="sm"
                className="text-red-600 border-red-200 hover:bg-red-50 text-xs"
                onClick={() => onWithdraw(app.id)}
              >
                Rút đơn
              </Button>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

// ─────────────────────────── Main Page ──────────────────────────────────────

const STATUS_FILTERS: Array<{ value: RecruitmentStatus | "ALL"; label: string }> = [
  { value: "ALL", label: "Tất cả" },
  { value: "SUBMITTED", label: "Đã gửi" },
  { value: "SCREENING", label: "Sàng lọc" },
  { value: "INTERVIEW", label: "Phỏng vấn" },
  { value: "OFFER", label: "Nhận offer" },
  { value: "HIRED", label: "Nhận việc" },
  { value: "REJECTED", label: "Không đạt" },
  { value: "WITHDRAWN", label: "Đã rút" },
];

export function CandidateRecruitmentsPage() {
  const queryClient = useQueryClient();
  const [filterStatus, setFilterStatus] = useState<RecruitmentStatus | "ALL">("ALL");
  const [detailId, setDetailId] = useState<string | null>(null);
  const [withdrawId, setWithdrawId] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);

  const { data: res, isLoading, isError, refetch } = useQuery({
    queryKey: ["my-recruitments", filterStatus],
    queryFn: () =>
      recruitmentApi.getMyRecruitments(filterStatus !== "ALL" ? filterStatus : undefined),
    staleTime: 60_000,
  });

  const withdrawMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      recruitmentApi.withdrawRecruitment(id, { reason }),
    onSuccess: () => {
      toast.success("Đã rút đơn ứng tuyển thành công.");
      setWithdrawId(null);
      setDetailId(null);
      queryClient.invalidateQueries({ queryKey: ["my-recruitments"] });
    },
  });

  const applications: CandidateRecruitmentSummary[] = Array.isArray(res?.data)
    ? res.data
    : [];

  // Group by status for counts
  const statusCounts = applications.reduce<Record<string, number>>((acc, a) => {
    acc[a.status] = (acc[a.status] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <div className="space-y-6 pb-12">
      {/* Header */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-2">
          <span className="p-2 rounded-lg bg-primary-light text-primary">
            <FileText className="h-6 w-6" />
          </span>
          <div>
            <h1 className="text-2xl font-bold text-foreground">Đơn ứng tuyển của tôi</h1>
            <p className="text-xs text-muted mt-0.5">
              Theo dõi trạng thái và lịch sử tất cả đơn đã nộp
            </p>
          </div>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => refetch()}
          leftIcon={<RefreshCw className="h-4 w-4" />}
        >
          Làm mới
        </Button>
      </div>

      {/* Stats */}
      {!isLoading && applications.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {(["SUBMITTED", "INTERVIEW", "OFFER", "REJECTED"] as RecruitmentStatus[]).map((s) => {
            const meta = STATUS_META[s];
            const Icon = meta.icon;
            return (
              <button
                key={s}
                onClick={() => setFilterStatus(s)}
                className={`p-3 rounded-xl border text-left transition-all hover:shadow-sm ${
                  filterStatus === s ? meta.bg + " ring-1 ring-primary" : "bg-surface border-border"
                }`}
              >
                <Icon className={`h-5 w-5 mb-1 ${meta.color}`} />
                <div className={`text-lg font-bold ${meta.color}`}>{statusCounts[s] ?? 0}</div>
                <div className="text-[11px] text-muted">{meta.label}</div>
              </button>
            );
          })}
        </div>
      )}

      {/* Filter */}
      <div>
        <button
          className="flex items-center gap-1.5 text-xs font-medium text-muted hover:text-foreground transition-colors mb-2"
          onClick={() => setShowFilters((p) => !p)}
        >
          Lọc theo trạng thái
          {showFilters ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
        </button>
        {showFilters && (
          <div className="flex flex-wrap gap-2">
            {STATUS_FILTERS.map((f) => (
              <button
                key={f.value}
                onClick={() => setFilterStatus(f.value)}
                className={`px-3 py-1.5 rounded-full text-xs font-medium border transition-all ${
                  filterStatus === f.value
                    ? "bg-primary text-white border-primary"
                    : "bg-surface text-foreground border-border hover:border-primary/50"
                }`}
              >
                {f.label}
                {f.value !== "ALL" && statusCounts[f.value] !== undefined && (
                  <span className="ml-1 opacity-70">({statusCounts[f.value]})</span>
                )}
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="space-y-4 animate-pulse">
          <div className="h-32 bg-slate-100 rounded-xl" />
          <div className="h-32 bg-slate-100 rounded-xl" />
          <div className="h-32 bg-slate-100 rounded-xl" />
        </div>
      ) : isError ? (
        <div className="text-center py-16 text-muted">
          <AlertCircle className="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p className="text-sm">Không thể tải danh sách đơn ứng tuyển. Vui lòng thử lại.</p>
          <Button variant="outline" size="sm" className="mt-3" onClick={() => refetch()}>
            Thử lại
          </Button>
        </div>
      ) : applications.length === 0 ? (
        <div className="text-center py-16 text-muted">
          <FileText className="h-12 w-12 mx-auto mb-3 opacity-30" />
          <p className="font-medium text-sm text-foreground">Chưa có đơn ứng tuyển nào</p>
          <p className="text-xs mt-1">
            {filterStatus !== "ALL"
              ? "Không có đơn nào ở trạng thái này."
              : "Hãy tìm kiếm việc làm phù hợp và nộp đơn ngay!"}
          </p>
          {filterStatus !== "ALL" && (
            <Button
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={() => setFilterStatus("ALL")}
            >
              Xem tất cả
            </Button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          <p className="text-xs text-muted">
            Hiển thị <span className="font-semibold text-foreground">{applications.length}</span> đơn
          </p>
          {applications.map((app) => (
            <ApplicationCard
              key={app.id}
              app={app}
              onViewDetail={setDetailId}
              onWithdraw={setWithdrawId}
            />
          ))}
        </div>
      )}

      {/* Detail Modal */}
      {detailId && (
        <DetailModal
          applicationId={detailId}
          onClose={() => setDetailId(null)}
          onWithdraw={(id) => {
            setDetailId(null);
            setWithdrawId(id);
          }}
        />
      )}

      {/* Withdraw Modal */}
      <WithdrawModal
        applicationId={withdrawId}
        onClose={() => setWithdrawId(null)}
        onConfirm={(id, reason) => withdrawMutation.mutate({ id, reason })}
        isPending={withdrawMutation.isPending}
      />
    </div>
  );
}

export default CandidateRecruitmentsPage;
