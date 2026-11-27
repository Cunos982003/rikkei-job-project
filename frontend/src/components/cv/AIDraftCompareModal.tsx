import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Check, History, Loader2, RefreshCw, Sparkles, X } from "lucide-react";
import { Button } from "../common/Button";
import { Badge } from "../common/Badge";
import {
  acceptAIDraft,
  createAIDraft,
  discardAIDraft,
  getAIDraftHistory,
  type AIDraftResponse,
  type AIDraftSection,
} from "../../api/endpoints/aiDraft.api";
import { api } from "../../api/client";

interface JobOption {
  id: string;
  title: string;
  companyName?: string;
}

interface AIDraftCompareModalProps {
  open: boolean;
  cvId: string;
  section: AIDraftSection;
  sectionTitle: string;
  projectId?: string | null;
  currentText: string;
  onClose: () => void;
  onAccepted: () => void;
}

function extractWarnedTokens(warnings: string[]): string[] {
  const tokens: string[] = [];
  for (const w of warnings) {
    const idx = w.indexOf(":");
    const raw = (idx >= 0 ? w.slice(idx + 1) : w).trim();
    if (raw) {
      tokens.push(raw);
    }
  }
  return Array.from(new Set(tokens)).sort((a, b) => b.length - a.length);
}

function escapeRegExp(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function HighlightedOutput({ text, warnings }: { text: string; warnings: string[] }) {
  const tokens = useMemo(() => extractWarnedTokens(warnings), [warnings]);
  if (!text) {
    return <span className="text-muted italic">Chưa có nội dung</span>;
  }
  if (tokens.length === 0) {
    return <span className="whitespace-pre-wrap">{text}</span>;
  }
  const regex = new RegExp(`(${tokens.map(escapeRegExp).join("|")})`, "gi");
  const parts = text.split(regex);
  return (
    <span className="whitespace-pre-wrap">
      {parts.map((part, idx) => {
        const isWarned = tokens.some((t) => t.toLowerCase() === part.toLowerCase());
        return isWarned ? (
          <mark
            key={idx}
            className="bg-amber-200 text-amber-950 px-1 py-0.5 rounded font-medium border border-amber-400"
            title="Chi tiết chưa tìm thấy trong dữ liệu hồ sơ"
          >
            {part}
          </mark>
        ) : (
          <span key={idx}>{part}</span>
        );
      })}
    </span>
  );
}

export function AIDraftCompareModal({
  open,
  cvId,
  section,
  sectionTitle,
  projectId,
  currentText,
  onClose,
  onAccepted,
}: AIDraftCompareModalProps) {
  const queryClient = useQueryClient();
  const [selectedJobId, setSelectedJobId] = useState<string>("");
  const [activeDraft, setActiveDraft] = useState<AIDraftResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [showHistory, setShowHistory] = useState(false);

  const { data: jobsData } = useQuery({
    queryKey: ["ai-draft-jobs"],
    queryFn: async () => {
      const res = await api.get<{ items?: JobOption[] }>("/jobs", { params: { page: 0, size: 30 } });
      const data = res.data as unknown;
      if (data && typeof data === "object" && "items" in data && Array.isArray((data as { items: JobOption[] }).items)) {
        return (data as { items: JobOption[] }).items;
      }
      return Array.isArray(data) ? (data as JobOption[]) : [];
    },
    enabled: open,
  });

  const { data: history = [], refetch: refetchHistory } = useQuery({
    queryKey: ["ai-drafts", cvId],
    queryFn: () => getAIDraftHistory(cvId),
    enabled: open && !!cvId,
  });

  const filteredHistory = useMemo(
    () =>
      history.filter(
        (d) =>
          d.section === section &&
          (section !== "PROJECT" || !projectId || d.projectId === projectId),
      ),
    [history, section, projectId],
  );

  const generateMutation = useMutation({
    mutationFn: (jobId?: string) =>
      createAIDraft(cvId, {
        section,
        projectId: section === "PROJECT" ? projectId ?? null : null,
        jobId: jobId || null,
      }),
    onSuccess: (draft) => {
      setActiveDraft(draft);
      setErrorMessage(null);
      queryClient.invalidateQueries({ queryKey: ["ai-drafts", cvId] });
      refetchHistory();
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string; detail?: string } } })?.response?.data?.message ||
        (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail ||
        "Không thể tạo bản nháp AI. Vui lòng thử lại.";
      setErrorMessage(msg);
    },
  });

  const acceptMutation = useMutation({
    mutationFn: (draftId: string) => acceptAIDraft(draftId),
    onSuccess: (updated) => {
      setActiveDraft(updated);
      setErrorMessage(null);
      queryClient.invalidateQueries({ queryKey: ["ai-drafts", cvId] });
      onAccepted();
      onClose();
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string; detail?: string } } })?.response?.data?.message ||
        "Không thể áp dụng bản nháp vào CV.";
      setErrorMessage(msg);
    },
  });

  const discardMutation = useMutation({
    mutationFn: (draftId: string) => discardAIDraft(draftId),
    onSuccess: (updated) => {
      setActiveDraft(updated);
      setErrorMessage(null);
      queryClient.invalidateQueries({ queryKey: ["ai-drafts", cvId] });
      onClose();
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string; detail?: string } } })?.response?.data?.message ||
        "Không thể bỏ bản nháp.";
      setErrorMessage(msg);
    },
  });

  useEffect(() => {
    if (open && cvId) {
      setActiveDraft(null);
      setErrorMessage(null);
      generateMutation.mutate(selectedJobId || undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, cvId, section, projectId]);

  if (!open) return null;

  const isBusy = generateMutation.isPending || acceptMutation.isPending || discardMutation.isPending;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-4xl rounded-xl bg-white shadow-2xl border border-border flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-primary/10 text-primary">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-foreground">AI viết giúp: {sectionTitle}</h3>
              <p className="text-xs text-muted">
                Chỉ sử dụng dữ liệu đã xác thực từ hồ sơ của bạn (kỹ năng, dự án, thực tập, học vấn)
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-muted hover:text-foreground hover:bg-slate-100"
            aria-label="Đóng"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {/* Optional target job selector */}
          <div className="flex flex-wrap items-end gap-3 bg-slate-50 p-3 rounded-lg border border-border">
            <div className="flex-1 min-w-[220px]">
              <label className="block text-xs font-medium text-foreground mb-1">
                Định hướng theo kỹ năng của JD (tùy chọn)
              </label>
              <select
                value={selectedJobId}
                onChange={(e) => setSelectedJobId(e.target.value)}
                disabled={isBusy}
                className="w-full rounded-lg border border-border bg-white px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
              >
                <option value="">-- Không chọn JD cụ thể --</option>
                {(jobsData ?? []).map((job) => (
                  <option key={job.id} value={job.id}>
                    {job.title} {job.companyName ? `(${job.companyName})` : ""}
                  </option>
                ))}
              </select>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => generateMutation.mutate(selectedJobId || undefined)}
              disabled={isBusy}
              leftIcon={
                generateMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <RefreshCw className="h-4 w-4" />
                )
              }
            >
              Viết lại
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setShowHistory((prev) => !prev)}
              leftIcon={<History className="h-4 w-4" />}
            >
              Lịch sử ({filteredHistory.length})
            </Button>
          </div>

          {errorMessage && (
            <div className="p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-sm">
              {errorMessage}
            </div>
          )}

          {/* 2-Column Comparison Box */}
          <div className="grid md:grid-cols-2 gap-4">
            <div className="rounded-lg border border-border bg-slate-50 p-4 flex flex-col">
              <div className="flex items-center justify-between mb-2 pb-2 border-b border-border">
                <span className="text-sm font-semibold text-foreground">Hiện tại</span>
                <Badge variant="outline" size="sm">Bản gốc</Badge>
              </div>
              <div className="text-sm text-foreground/80 whitespace-pre-wrap flex-1 min-h-[160px]">
                {currentText?.trim() ? currentText : <span className="text-muted italic">Chưa có nội dung</span>}
              </div>
            </div>

            <div className="rounded-lg border border-primary/30 bg-primary/5 p-4 flex flex-col">
              <div className="flex items-center justify-between mb-2 pb-2 border-b border-primary/20">
                <span className="text-sm font-semibold text-foreground flex items-center gap-1.5">
                  <Sparkles className="h-4 w-4 text-primary" />
                  AI đề xuất
                </span>
                {activeDraft && (
                  <Badge
                    variant={
                      activeDraft.warnings?.length > 0
                        ? "warning"
                        : activeDraft.status === "ACCEPTED"
                          ? "verified"
                          : "default"
                    }
                    size="sm"
                  >
                    {activeDraft.warnings?.length > 0
                      ? `${activeDraft.warnings.length} cảnh báo`
                      : "Khớp dữ liệu hồ sơ"}
                  </Badge>
                )}
              </div>
              <div className="text-sm text-foreground flex-1 min-h-[160px]">
                {generateMutation.isPending ? (
                  <div className="flex items-center justify-center h-full gap-2 text-muted">
                    <Loader2 className="h-5 w-5 animate-spin text-primary" />
                    <span>Đang tổng hợp dữ liệu hồ sơ và viết bản nháp...</span>
                  </div>
                ) : activeDraft ? (
                  <HighlightedOutput text={activeDraft.output} warnings={activeDraft.warnings || []} />
                ) : (
                  <span className="text-muted italic">Nhấn "Viết lại" để tạo bản nháp</span>
                )}
              </div>
            </div>
          </div>

          {/* Warnings Panel */}
          {activeDraft && activeDraft.warnings && activeDraft.warnings.length > 0 && (
            <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 space-y-1.5">
              <div className="flex items-center gap-2 text-amber-900 font-medium text-sm">
                <AlertTriangle className="h-4 w-4 text-amber-600 shrink-0" />
                <span>Cảnh báo kiểm chứng dữ liệu (đã tô vàng trong bản đề xuất)</span>
              </div>
              <ul className="list-disc list-inside text-xs text-amber-900 space-y-0.5">
                {activeDraft.warnings.map((w, idx) => (
                  <li key={idx}>{w}</li>
                ))}
              </ul>
            </div>
          )}

          {/* History Panel */}
          {showHistory && (
            <div className="rounded-lg border border-border p-3 space-y-2 bg-slate-50">
              <p className="text-xs font-semibold text-foreground">Lịch sử bản nháp gần đây</p>
              {filteredHistory.length === 0 ? (
                <p className="text-xs text-muted">Chưa có bản nháp nào.</p>
              ) : (
                <div className="space-y-2 max-h-40 overflow-y-auto">
                  {filteredHistory.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => setActiveDraft(item)}
                      className="w-full text-left p-2 rounded border border-border bg-white hover:border-primary/50 transition-colors"
                    >
                      <div className="flex items-center justify-between text-xs mb-1">
                        <span className="font-medium text-foreground">
                          {new Date(item.createdAt).toLocaleString("vi-VN")}
                        </span>
                        <div className="flex items-center gap-1.5">
                          {item.warnings?.length > 0 && (
                            <Badge variant="warning" size="sm">{item.warnings.length} cảnh báo</Badge>
                          )}
                          <Badge variant="outline" size="sm">{item.status}</Badge>
                        </div>
                      </div>
                      <p className="text-xs text-muted line-clamp-2">{item.output}</p>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 px-6 py-4 border-t border-border bg-slate-50 rounded-b-xl">
          <Button
            type="button"
            variant="outline"
            onClick={() => activeDraft && discardMutation.mutate(activeDraft.id)}
            disabled={!activeDraft || activeDraft.status !== "DRAFT" || isBusy}
            isLoading={discardMutation.isPending}
            leftIcon={<X className="h-4 w-4" />}
          >
            Bỏ
          </Button>
          <Button
            type="button"
            variant="outline"
            onClick={() => generateMutation.mutate(selectedJobId || undefined)}
            disabled={isBusy}
            isLoading={generateMutation.isPending}
            leftIcon={<RefreshCw className="h-4 w-4" />}
          >
            Viết lại
          </Button>
          <Button
            type="button"
            variant="primary"
            onClick={() => activeDraft && acceptMutation.mutate(activeDraft.id)}
            disabled={!activeDraft || activeDraft.status !== "DRAFT" || isBusy}
            isLoading={acceptMutation.isPending}
            leftIcon={<Check className="h-4 w-4" />}
          >
            Dùng bản này
          </Button>
        </div>
      </div>
    </div>
  );
}
