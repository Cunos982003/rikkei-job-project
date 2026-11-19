import React, { useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft,
  Calendar,
  Clock,
  Mail,
  User,
  FileText,
  Sparkles,
  Plus,
  Edit2,
  AlertCircle,
  ChevronRight,
  ExternalLink,
  CalendarPlus,
  RefreshCw,
  Video,
  Trash2,
  Gift,
  Send,
  CheckCircle2,
  MapPin,
  DollarSign,
} from "lucide-react";
import { toast } from "sonner";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Modal } from "@/components/common/Modal";
import { Input } from "@/components/common/Input";
import { Skeleton } from "@/components/feedback/Skeleton";
import { useAuthStore } from "@/store/useAuthStore";
import {
  recruitmentApi,
  type RecruitmentStatus,
  type InterviewRoundResult,
  type InterviewRoundSummary,
  type CreateInterviewRoundRequest,
  type UpdateInterviewRoundRequest,
} from "@/api/endpoints/recruitment.api";
import {
  interviewsApi,
  type InterviewSlotInput,
  type InterviewSlotStatus,
} from "@/api/endpoints/interviews.api";
import { offersApi, type OfferStatus, type SendOfferPayload } from "@/api/endpoints/offers.api";

const OFFER_STATUS_BADGE: Record<
  OfferStatus,
  { label: string; variant: "primary" | "verified" | "danger" | "warning" }
> = {
  SENT: { label: "Đã gửi (Chờ ứng viên phản hồi)", variant: "primary" },
  ACCEPTED: { label: "Ứng viên đã chấp nhận (ACCEPTED)", variant: "verified" },
  DECLINED: { label: "Ứng viên đã từ chối (DECLINED)", variant: "danger" },
  EXPIRED: { label: "Quá hạn phản hồi (EXPIRED)", variant: "warning" },
};

// Mapping status badge variant & label
const STATUS_CONFIG: Record<
  RecruitmentStatus,
  { label: string; variant: "default" | "primary" | "warning" | "danger" | "verified" }
> = {
  SUBMITTED: { label: "Mới nộp", variant: "default" },
  SCREENING: { label: "Sàng lọc CV", variant: "warning" },
  INTERVIEW: { label: "Phỏng vấn", variant: "primary" },
  OFFER: { label: "Đề nghị tuyển dụng", variant: "verified" },
  HIRED: { label: "Đã tuyển dụng", variant: "verified" },
  REJECTED: { label: "Từ chối", variant: "danger" },
  WITHDRAWN: { label: "Ứng viên rút đơn", variant: "danger" },
};

// Next allowed status transitions for HR according to business policy
const ALLOWED_HR_TRANSITIONS: Record<RecruitmentStatus, { status: RecruitmentStatus; label: string; variant: "primary" | "danger" }[]> = {
  SUBMITTED: [
    { status: "SCREENING", label: "Chuyển Sàng Lọc (SCREENING)", variant: "primary" },
    { status: "REJECTED", label: "Từ chối hồ sơ (REJECTED)", variant: "danger" },
  ],
  SCREENING: [
    { status: "INTERVIEW", label: "Chuyển Phỏng Vấn (INTERVIEW)", variant: "primary" },
    { status: "REJECTED", label: "Từ chối hồ sơ (REJECTED)", variant: "danger" },
  ],
  INTERVIEW: [
    { status: "REJECTED", label: "Từ chối sau phỏng vấn (REJECTED)", variant: "danger" },
  ],
  OFFER: [],
  HIRED: [],
  REJECTED: [],
  WITHDRAWN: [],
};

const ROUND_RESULT_CONFIG: Record<
  InterviewRoundResult,
  { label: string; variant: "warning" | "verified" | "danger" }
> = {
  PENDING: { label: "Chờ kết quả", variant: "warning" },
  PASS: { label: "Đạt (PASS)", variant: "verified" },
  FAIL: { label: "Không đạt (FAIL)", variant: "danger" },
};

const SLOT_STATUS_CONFIG: Record<
  InterviewSlotStatus,
  { label: string; variant: "primary" | "verified" | "danger" }
> = {
  OPEN: { label: "Đang mở", variant: "primary" },
  PICKED: { label: "Đã chọn", variant: "verified" },
  CANCELLED: { label: "Đã hủy", variant: "danger" },
};

function defaultSlotDraft(): { startAt: string; endAt: string } {
  const start = new Date(Date.now() + 24 * 60 * 60 * 1000);
  start.setMinutes(0, 0, 0);
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  const toLocal = (d: Date) =>
    new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  return { startAt: toLocal(start), endAt: toLocal(end) };
}

export function RecruitmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const isHr = user?.role === "HR";

  const handleBack = () => {
    if (location.state && (location.state as any).from) {
      navigate((location.state as any).from);
    } else {
      navigate(-1);
    }
  };

  // Transition state modal
  const [transitionTarget, setTransitionTarget] = useState<RecruitmentStatus | null>(null);
  const [transitionNote, setTransitionNote] = useState("");

  // Create round modal
  const [isCreateRoundOpen, setIsCreateRoundOpen] = useState(false);
  const [newRoundName, setNewRoundName] = useState("");
  const [newRoundEmail, setNewRoundEmail] = useState("");
  const [newRoundDate, setNewRoundDate] = useState("");

  // Delete round state
  const [deletingRound, setDeletingRound] = useState<InterviewRoundSummary | null>(null);

  // Update round modal
  const [editingRound, setEditingRound] = useState<InterviewRoundSummary | null>(null);
  const [editRoundResult, setEditRoundResult] = useState<InterviewRoundResult>("PENDING");
  const [editRoundScore, setEditRoundScore] = useState<string>("");
  const [editRoundNote, setEditRoundNote] = useState<string>("");
  const [editRoundDate, setEditRoundDate] = useState<string>("");

  // UT-06: Propose slots / Reschedule modal
  const [slotModalRound, setSlotModalRound] = useState<InterviewRoundSummary | null>(null);
  const [slotModalMode, setSlotModalMode] = useState<"propose" | "reschedule">("propose");
  const [slotDrafts, setSlotDrafts] = useState<{ startAt: string; endAt: string }[]>([
    defaultSlotDraft(),
  ]);

  // UT-07: Send Offer form state
  const [offerPosition, setOfferPosition] = useState("");
  const [offerSalary, setOfferSalary] = useState("");
  const [offerStartDate, setOfferStartDate] = useState("");
  const [offerWorkLocation, setOfferWorkLocation] = useState("");
  const [offerDeadline, setOfferDeadline] = useState("");
  const [offerNote, setOfferNote] = useState("");

  const {
    data: response,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["recruitment", id],
    queryFn: () => recruitmentApi.getReviewDetails(id!),
    enabled: !!id,
  });

  const detail = response?.data;

  // Mutation: Transition status
  const transitionMutation = useMutation({
    mutationFn: (data: { toStatus: RecruitmentStatus; note?: string }) =>
      recruitmentApi.transitionStatus(id!, data),
    onSuccess: () => {
      toast.success("Chuyển trạng thái đơn ứng tuyển thành công");
      setTransitionTarget(null);
      setTransitionNote("");
      queryClient.invalidateQueries({ queryKey: ["recruitment", id] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || "Không thể chuyển trạng thái");
    },
  });

  // Mutation: Create interview round
  const createRoundMutation = useMutation({
    mutationFn: (data: CreateInterviewRoundRequest) => recruitmentApi.createRound(id!, data),
    onSuccess: () => {
      toast.success("Tạo vòng phỏng vấn mới thành công");
      setIsCreateRoundOpen(false);
      setNewRoundName("");
      setNewRoundEmail("");
      setNewRoundDate("");
      queryClient.invalidateQueries({ queryKey: ["recruitment", id] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || "Không thể tạo vòng phỏng vấn");
    },
  });

  // Mutation: Update interview round
  const updateRoundMutation = useMutation({
    mutationFn: (data: { roundId: string; body: UpdateInterviewRoundRequest }) =>
      recruitmentApi.updateRound(id!, data.roundId, data.body),
    onSuccess: () => {
      toast.success("Cập nhật kết quả vòng phỏng vấn thành công");
      setEditingRound(null);
      queryClient.invalidateQueries({ queryKey: ["recruitment", id] });
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || "Không thể cập nhật vòng phỏng vấn");
    },
  });

  // Mutation: Delete interview round
  const deleteRoundMutation = useMutation({
    mutationFn: (roundId: string) => recruitmentApi.deleteRound(id!, roundId),
    onSuccess: () => {
      toast.success("Đã xóa vòng phỏng vấn thành công");
      setDeletingRound(null);
      queryClient.invalidateQueries({ queryKey: ["recruitment", id] });
      queryClient.invalidateQueries({ queryKey: ["companyInterviews"] });
      queryClient.invalidateQueries({ queryKey: ["hrApplications"] });
    },
    onError: (err: any) => {
      toast.error(
        err?.response?.data?.message ||
          err?.message ||
          "Không thể xóa vòng phỏng vấn"
      );
    },
  });

  // UT-06 Mutation: Propose interview slots / Reschedule
  const slotMutation = useMutation({
    mutationFn: async (payload: {
      roundId: string;
      mode: "propose" | "reschedule";
      slots: InterviewSlotInput[];
    }): Promise<unknown> =>
      payload.mode === "reschedule"
        ? interviewsApi.reschedule(payload.roundId, payload.slots)
        : interviewsApi.proposeSlots(payload.roundId, payload.slots),
    onSuccess: (_, variables) => {
      toast.success(
        variables.mode === "reschedule"
          ? "Đã hủy lịch cũ và đề xuất khung giờ mới thành công"
          : "Đã gửi đề xuất khung giờ phỏng vấn cho ứng viên"
      );
      setSlotModalRound(null);
      queryClient.invalidateQueries({ queryKey: ["recruitment", id] });
      queryClient.invalidateQueries({ queryKey: ["companyInterviews"] });
    },
    onError: (err: any) => {
      toast.error(
        err?.response?.data?.message ||
          err?.message ||
          "Không thể lưu khung giờ phỏng vấn"
      );
    },
  });

  const handleOpenSlotModal = (round: InterviewRoundSummary, mode: "propose" | "reschedule") => {
    setSlotModalRound(round);
    setSlotModalMode(mode);
    setSlotDrafts([defaultSlotDraft()]);
  };

  const handleSubmitSlots = (e: React.FormEvent) => {
    e.preventDefault();
    if (!slotModalRound) return;
    if (slotDrafts.length < 1 || slotDrafts.length > 5) {
      toast.error("Vui lòng đề xuất từ 1 đến 5 khung giờ");
      return;
    }
    const nowMs = Date.now();
    const payload: InterviewSlotInput[] = [];
    for (let i = 0; i < slotDrafts.length; i++) {
      const { startAt, endAt } = slotDrafts[i];
      if (!startAt || !endAt) {
        toast.error(`Khung giờ #${i + 1}: Vui lòng nhập đầy đủ thời gian bắt đầu và kết thúc`);
        return;
      }
      const s = new Date(startAt);
      const en = new Date(endAt);
      if (s.getTime() <= nowMs) {
        toast.error(`Khung giờ #${i + 1}: Thời gian bắt đầu phải ở trong tương lai`);
        return;
      }
      const diffMinutes = (en.getTime() - s.getTime()) / 60000;
      if (diffMinutes < 30) {
        toast.error(`Khung giờ #${i + 1}: Thời lượng mỗi khung phải từ 30 phút trở lên`);
        return;
      }
      payload.push({ startAt: s.toISOString(), endAt: en.toISOString() });
    }
    slotMutation.mutate({
      roundId: slotModalRound.id,
      mode: slotModalMode,
      slots: payload,
    });
  };

  // UT-07 Mutation: Send Offer
  const sendOfferMutation = useMutation({
    mutationFn: (payload: SendOfferPayload) => offersApi.sendOffer(id!, payload),
    onSuccess: () => {
      toast.success("Gửi thư mời nhận việc (Offer) tới ứng viên thành công!");
      setOfferPosition("");
      setOfferSalary("");
      setOfferStartDate("");
      setOfferWorkLocation("");
      setOfferDeadline("");
      setOfferNote("");
      queryClient.invalidateQueries({ queryKey: ["recruitment", id] });
      queryClient.invalidateQueries({ queryKey: ["hrApplications"] });
    },
    onError: (err: any) => {
      toast.error(
        err?.response?.data?.message ||
          err?.message ||
          "Không thể gửi Offer (kiểm tra vòng phỏng vấn cuối phải PASS)"
      );
    },
  });

  // UT-07 Mutation: Confirm Onboard
  const confirmOnboardMutation = useMutation({
    mutationFn: () => offersApi.confirmOnboard(id!),
    onSuccess: () => {
      toast.success("Đã xác nhận ứng viên đi làm thành công!");
      queryClient.invalidateQueries({ queryKey: ["recruitment", id] });
      queryClient.invalidateQueries({ queryKey: ["hrApplications"] });
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || err?.message || "Không thể xác nhận đi làm");
    },
  });

  const handleSendOfferSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const salaryNum = Number(offerSalary);
    if (
      !offerPosition.trim() ||
      !offerWorkLocation.trim() ||
      !offerStartDate ||
      !offerDeadline ||
      Number.isNaN(salaryNum) ||
      salaryNum <= 0
    ) {
      toast.error("Vui lòng nhập đầy đủ vị trí, mức lương (> 0), ngày bắt đầu, địa điểm và hạn trả lời.");
      return;
    }
    sendOfferMutation.mutate({
      position: offerPosition.trim(),
      salary: salaryNum,
      startDate: offerStartDate,
      workLocation: offerWorkLocation.trim(),
      responseDeadline: new Date(offerDeadline).toISOString(),
      note: offerNote.trim() || undefined,
    });
  };

  const handleOpenEditRound = (round: InterviewRoundSummary) => {
    setEditingRound(round);
    setEditRoundResult(round.result);
    setEditRoundScore(round.score !== null ? round.score.toString() : "");
    setEditRoundNote(round.note || "");
    setEditRoundDate(round.scheduledAt ? round.scheduledAt.substring(0, 16) : "");
  };

  const handleSaveRound = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingRound) return;
    const scoreNum = editRoundScore !== "" ? parseInt(editRoundScore, 10) : undefined;
    updateRoundMutation.mutate({
      roundId: editingRound.id,
      body: {
        result: editRoundResult,
        score: isNaN(scoreNum as number) ? null : scoreNum,
        note: editRoundNote.trim() || undefined,
        scheduledAt: editRoundDate ? new Date(editRoundDate).toISOString() : undefined,
      },
    });
  };

  const handleCreateRoundSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newRoundName.trim()) {
      toast.error("Vui lòng nhập tên vòng phỏng vấn");
      return;
    }
    createRoundMutation.mutate({
      name: newRoundName.trim(),
      interviewerEmail: newRoundEmail.trim() || undefined,
      scheduledAt: newRoundDate ? new Date(newRoundDate).toISOString() : undefined,
    });
  };

  if (isLoading) {
    return (
      <div className="space-y-6 max-w-5xl mx-auto py-6">
        <Skeleton className="h-8 w-64" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Skeleton className="h-48 col-span-2" />
          <Skeleton className="h-48" />
        </div>
      </div>
    );
  }

  if (isError || !detail) {
    return (
      <div className="text-center py-12 space-y-4">
        <AlertCircle className="h-12 w-12 text-danger mx-auto" />
        <h2 className="text-lg font-semibold text-foreground">Không tìm thấy đơn ứng tuyển</h2>
        <p className="text-xs text-muted">Đơn không tồn tại hoặc bạn không có quyền xem thông tin này.</p>
        <Button variant="outline" size="sm" onClick={handleBack}>
          Quay lại
        </Button>
      </div>
    );
  }

  const currentStatus = detail.recruitment.status;
  const statusCfg = STATUS_CONFIG[currentStatus] || { label: currentStatus, variant: "default" };
  const allowedTransitions = isHr ? ALLOWED_HR_TRANSITIONS[currentStatus] || [] : [];

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-12">
      {/* Top Navigation */}
      <div className="flex items-center justify-between pb-4 border-b border-border">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="sm"
            onClick={handleBack}
            leftIcon={<ArrowLeft className="h-4 w-4" />}
          >
            Quay lại
          </Button>
          <div className="h-4 w-px bg-border" />
          <h1 className="text-xl font-bold text-foreground">
            Chi tiết đơn ứng tuyển #{detail.recruitment.id.substring(0, 8)}
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted">Trạng thái:</span>
          <Badge variant={statusCfg.variant}>{statusCfg.label}</Badge>
        </div>
      </div>

      {/* Main Grid: Candidate Info & AI Score */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Candidate & CV Info */}
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <User className="h-4 w-4 text-primary" />
              Thông tin ứng viên & Hồ sơ
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <span className="text-xs text-muted block">Email ứng viên:</span>
                <span className="text-sm font-medium text-foreground flex items-center gap-1.5 mt-0.5">
                  <Mail className="h-3.5 w-3.5 text-muted" />
                  {detail.candidateEmail || "Chưa cập nhật"}
                </span>
              </div>
              <div>
                <span className="text-xs text-muted block">Thời gian nộp:</span>
                <span className="text-sm font-medium text-foreground flex items-center gap-1.5 mt-0.5">
                  <Calendar className="h-3.5 w-3.5 text-muted" />
                  {new Date(detail.recruitment.createdAt).toLocaleString("vi-VN")}
                </span>
              </div>
            </div>

            {/* Cover letter */}
            <div>
              <span className="text-xs text-muted block mb-1">Thư giới thiệu (Cover letter):</span>
              <div className="p-3 bg-slate-50 border border-border-subtle rounded-lg text-xs text-foreground leading-relaxed whitespace-pre-wrap">
                {detail.coverLetter || "Không có thư giới thiệu kèm theo."}
              </div>
            </div>

            {/* CV Snapshot link */}
            <div className="pt-2 flex items-center justify-between border-t border-border-subtle">
              <span className="text-xs text-muted">CV đính kèm lúc nộp:</span>
              <a
                href={recruitmentApi.getCvSnapshotUrl(detail.recruitment.id)}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline"
              >
                <FileText className="h-3.5 w-3.5" />
                Xem snapshot CV (PDF)
                <ExternalLink className="h-3 w-3" />
              </a>
            </div>
          </CardContent>
        </Card>

        {/* AI Career Match Score */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-amber-500" />
              Điểm khớp năng lực (AI)
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-center">
            <div className="inline-flex flex-col items-center justify-center p-6 rounded-2xl bg-amber-50 border border-amber-200 w-full">
              <span className="text-4xl font-extrabold text-amber-600">
                {detail.score.toFixed(1)}%
              </span>
              <span className="text-xs text-amber-700 font-medium mt-1">
                Độ phù hợp hồ sơ với vị trí
              </span>
            </div>
            <p className="text-[11px] text-muted leading-normal">
              Điểm số tính toán tự động dựa trên từ khóa kỹ năng, kinh nghiệm và yêu cầu công việc.
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Action Bar for HR Transitions */}
      {isHr && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold flex items-center gap-2">
              Chuyển bước quy trình tuyển dụng
            </CardTitle>
          </CardHeader>
          <CardContent>
            {allowedTransitions.length > 0 ? (
              <div className="flex flex-wrap items-center gap-3">
                <span className="text-xs text-muted mr-1">Các thao tác hợp lệ:</span>
                {allowedTransitions.map((t) => (
                  <Button
                    key={t.status}
                    size="sm"
                    variant={t.variant === "danger" ? "danger" : "primary"}
                    onClick={() => setTransitionTarget(t.status)}
                  >
                    {t.label}
                  </Button>
                ))}
              </div>
            ) : (
              <p className="text-xs text-muted italic">
                {currentStatus === "INTERVIEW"
                  ? "Đơn đang trong giai đoạn phỏng vấn. Để chuyển sang OFFER, vui lòng tạo offer thông qua chức năng gửi thư mời nhận việc (UT-07) khi mọi vòng đã đạt."
                  : "Đơn ở trạng thái kết thúc hoặc chỉ được chuyển bước qua các quy trình đặc thù."}
              </p>
            )}
          </CardContent>
        </Card>
      )}

      {/* Block: Vòng phỏng vấn (Interview Rounds) */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="text-base flex items-center gap-2">
              <Calendar className="h-4 w-4 text-primary" />
              Quản lý các vòng phỏng vấn ({detail.rounds.length})
            </CardTitle>
            <p className="text-xs text-muted mt-0.5">
              Theo dõi lịch, kết quả và đánh giá của từng vòng phỏng vấn
            </p>
          </div>
          {isHr && currentStatus === "INTERVIEW" && (
            <Button
              size="sm"
              variant="outline"
              leftIcon={<Plus className="h-3.5 w-3.5" />}
              onClick={() => setIsCreateRoundOpen(true)}
            >
              Thêm vòng phỏng vấn
            </Button>
          )}
        </CardHeader>
        <CardContent>
          {detail.rounds.length === 0 ? (
            <div className="text-center py-8 text-xs text-muted bg-slate-50 border border-dashed border-border rounded-xl">
              Chưa có vòng phỏng vấn nào được thiết lập.
              {isHr && currentStatus === "INTERVIEW" && (
                <div className="mt-2">
                  <Button size="sm" variant="primary" onClick={() => setIsCreateRoundOpen(true)}>
                    Tạo vòng phỏng vấn đầu tiên
                  </Button>
                </div>
              )}
            </div>
          ) : (
            <div className="divide-y divide-border-subtle border border-border rounded-xl overflow-hidden">
              {detail.rounds.map((round) => {
                const resCfg = ROUND_RESULT_CONFIG[round.result] || {
                  label: round.result,
                  variant: "warning",
                };
                return (
                  <div
                    key={round.id}
                    className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-surface hover:bg-slate-50 transition-colors"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-xs text-foreground">
                          Vòng {round.roundNo}: {round.name}
                        </span>
                        <Badge variant={resCfg.variant} size="sm">
                          {resCfg.label}
                        </Badge>
                        {round.score !== null && (
                          <span className="text-xs font-semibold text-primary">
                            Điểm: {round.score}/100
                          </span>
                        )}
                      </div>
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted">
                        {round.interviewerEmail && (
                          <span className="flex items-center gap-1">
                            <Mail className="h-3 w-3" />
                            Người phỏng vấn: {round.interviewerEmail}
                          </span>
                        )}
                        {round.scheduledAt && (
                          <span className="flex items-center gap-1">
                            <Clock className="h-3 w-3" />
                            Lịch hẹn: {new Date(round.scheduledAt).toLocaleString("vi-VN")}
                          </span>
                        )}
                      </div>
                      {round.note && (
                        <p className="text-xs text-foreground/80 mt-1 italic">
                          Ghi chú: {round.note}
                        </p>
                      )}

                      {/* UT-06: Confirmed Rikkei Meet Schedule & Proposed Slots */}
                      {round.schedule && round.schedule.status === "SCHEDULED" && (
                        <div className="mt-2.5 p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 flex flex-wrap items-center justify-between gap-2 text-xs">
                          <div className="space-y-0.5">
                            <div className="font-semibold text-emerald-800 flex items-center gap-1.5">
                              <Video className="h-3.5 w-3.5" />
                              Đã chốt lịch:{" "}
                              {new Date(round.schedule.startAt).toLocaleString("vi-VN")} -{" "}
                              {new Date(round.schedule.endAt).toLocaleTimeString("vi-VN", {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </div>
                          </div>
                          <a
                            href={round.schedule.meetUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-primary text-white text-[11px] font-semibold hover:bg-primary/90"
                          >
                            <Video className="h-3 w-3" />
                            Vào phòng họp
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        </div>
                      )}

                      {round.slots && round.slots.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-1.5">
                          {round.slots.map((slot) => {
                            const slotCfg = SLOT_STATUS_CONFIG[slot.status] || {
                              label: slot.status,
                              variant: "primary" as const,
                            };
                            return (
                              <span
                                key={slot.id}
                                className={`inline-flex items-center gap-1.5 px-2 py-1 rounded-md border text-[11px] ${
                                  slot.status === "CANCELLED"
                                    ? "bg-slate-50 border-border-subtle text-muted line-through"
                                    : slot.status === "PICKED"
                                    ? "bg-emerald-50 border-emerald-200 text-emerald-800 font-medium"
                                    : "bg-primary-light/40 border-primary/20 text-foreground"
                                }`}
                              >
                                <Clock className="h-3 w-3" />
                                {new Date(slot.startAt).toLocaleDateString("vi-VN", {
                                  day: "2-digit",
                                  month: "2-digit",
                                })}{" "}
                                {new Date(slot.startAt).toLocaleTimeString("vi-VN", {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })}
                                -
                                {new Date(slot.endAt).toLocaleTimeString("vi-VN", {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })}
                                <Badge variant={slotCfg.variant} size="sm">
                                  {slotCfg.label}
                                </Badge>
                              </span>
                            );
                          })}
                        </div>
                      )}
                    </div>

                    {isHr && currentStatus === "INTERVIEW" && (
                      <div className="flex flex-wrap items-center gap-1.5 shrink-0">
                        {(!round.schedule || round.schedule.status !== "SCHEDULED") && (
                          <Button
                            size="sm"
                            variant="outline"
                            leftIcon={<CalendarPlus className="h-3.5 w-3.5" />}
                            onClick={() => handleOpenSlotModal(round, "propose")}
                          >
                            Đề xuất khung giờ
                          </Button>
                        )}
                        {((round.slots && round.slots.length > 0) ||
                          (round.schedule && round.schedule.status === "SCHEDULED")) && (
                          <Button
                            size="sm"
                            variant="outline"
                            leftIcon={<RefreshCw className="h-3.5 w-3.5" />}
                            onClick={() => handleOpenSlotModal(round, "reschedule")}
                          >
                            Đổi lịch
                          </Button>
                        )}
                        <Button
                          size="sm"
                          variant="ghost"
                          leftIcon={<Edit2 className="h-3.5 w-3.5" />}
                          onClick={() => handleOpenEditRound(round)}
                        >
                          Nhập kết quả
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="text-danger hover:bg-rose-50 hover:text-danger text-xs"
                          leftIcon={<Trash2 className="h-3.5 w-3.5" />}
                          onClick={() => setDeletingRound(round)}
                        >
                          Xóa
                        </Button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Block: UT-07 Offer & Xác nhận đi làm */}
      {(() => {
        const rounds = detail.rounds || [];
        const hasPendingRound = rounds.some((r) => r.result === "PENDING");
        const lastRound = rounds.length > 0 ? rounds[rounds.length - 1] : null;
        const isEligibleForOffer =
          currentStatus === "INTERVIEW" &&
          rounds.length > 0 &&
          !hasPendingRound &&
          lastRound?.result === "PASS";
        const offer = detail.offer;
        const emp = detail.employmentRecord;
        const confirmedAt = emp?.confirmedAt || offer?.confirmedAt || null;

        if (!offer && !isHr) return null;
        if (!offer && currentStatus !== "INTERVIEW") return null;

        return (
          <Card className="border-primary/30">
            <CardHeader>
              <div className="flex items-center justify-between gap-2 flex-wrap">
                <CardTitle className="text-base flex items-center gap-2">
                  <Gift className="h-4 w-4 text-primary" />
                  Thư mời nhận việc (Offer) &amp; Xác nhận đi làm
                </CardTitle>
                {offer && (
                  <Badge variant={OFFER_STATUS_BADGE[offer.status].variant} size="sm">
                    {OFFER_STATUS_BADGE[offer.status].label}
                  </Badge>
                )}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {offer ? (
                <div className="space-y-4">
                  <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-3 text-sm">
                    <div className="rounded-xl bg-slate-50 border border-border p-3">
                      <span className="text-xs text-muted">Vị trí Offer</span>
                      <p className="font-semibold text-foreground mt-1">{offer.position}</p>
                    </div>
                    <div className="rounded-xl bg-slate-50 border border-border p-3">
                      <span className="text-xs text-muted flex items-center gap-1">
                        <DollarSign className="h-3.5 w-3.5 text-emerald-600" />
                        Mức lương đề nghị
                      </span>
                      <p className="font-bold text-foreground mt-1">
                        {new Intl.NumberFormat("vi-VN", {
                          style: "currency",
                          currency: "VND",
                          maximumFractionDigits: 0,
                        }).format(offer.salary)}
                      </p>
                    </div>
                    <div className="rounded-xl bg-slate-50 border border-border p-3">
                      <span className="text-xs text-muted">Ngày bắt đầu dự kiến</span>
                      <p className="font-semibold text-foreground mt-1">
                        {new Date(offer.startDate).toLocaleDateString("vi-VN")}
                      </p>
                    </div>
                    <div className="rounded-xl bg-slate-50 border border-border p-3">
                      <span className="text-xs text-muted">Hạn trả lời</span>
                      <p className="font-semibold text-foreground mt-1">
                        {new Date(offer.responseDeadline).toLocaleString("vi-VN")}
                      </p>
                    </div>
                  </div>

                  <div className="text-xs text-foreground flex items-center gap-1.5">
                    <MapPin className="h-3.5 w-3.5 text-muted" />
                    <span className="text-muted">Địa điểm làm việc:</span>
                    <span className="font-medium">{offer.workLocation}</span>
                  </div>

                  {offer.note && (
                    <p className="text-xs text-muted italic">Ghi chú Offer: {offer.note}</p>
                  )}

                  {offer.status === "DECLINED" && (
                    <div className="rounded-xl bg-rose-50 border border-rose-200 p-3.5 text-xs text-rose-900 space-y-1">
                      <p className="font-bold flex items-center gap-1.5">
                        <AlertCircle className="h-4 w-4 shrink-0" />
                        Lý do ứng viên từ chối Offer:
                      </p>
                      <p>{offer.declineReason || "Không có lý do chi tiết."}</p>
                      {offer.respondedAt && (
                        <p className="text-[11px] text-rose-700">
                          Thời điểm phản hồi: {new Date(offer.respondedAt).toLocaleString("vi-VN")}
                        </p>
                      )}
                    </div>
                  )}

                  {offer.status === "ACCEPTED" && (
                    <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div className="space-y-1 text-xs text-emerald-900">
                        <p className="font-bold flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4 shrink-0" />
                          Ứng viên đã chấp nhận Offer (Đơn: HIRED)
                        </p>
                        {confirmedAt ? (
                          <p className="text-emerald-800">
                            HR đã xác nhận ứng viên đi làm vào:{" "}
                            <strong>{new Date(confirmedAt).toLocaleString("vi-VN")}</strong>
                          </p>
                        ) : (
                          <p className="text-emerald-800">
                            Chờ HR xác nhận ứng viên đã đi làm chính thức (onboard).
                          </p>
                        )}
                      </div>

                      {isHr && !confirmedAt && (
                        <Button
                          size="sm"
                          variant="primary"
                          leftIcon={<CheckCircle2 className="h-4 w-4" />}
                          isLoading={confirmOnboardMutation.isPending}
                          onClick={() => confirmOnboardMutation.mutate()}
                        >
                          Xác nhận đã đi làm
                        </Button>
                      )}
                    </div>
                  )}
                </div>
              ) : (
                <form onSubmit={handleSendOfferSubmit} className="space-y-4">
                  {!isEligibleForOffer && (
                    <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 text-xs text-amber-900 flex items-start gap-2">
                      <AlertCircle className="h-4 w-4 shrink-0 mt-0.5 text-amber-600" />
                      <span>
                        Đơn phải ở trạng thái <strong>INTERVIEW</strong> và vòng phỏng vấn cuối phải đạt{" "}
                        <strong>PASS</strong> mới được gửi Offer (nếu chưa đạt, hệ thống sẽ trả về lỗi 409).
                      </span>
                    </div>
                  )}

                  <div className="grid sm:grid-cols-2 gap-4">
                    <Input
                      label="Vị trí tuyển dụng *"
                      placeholder="Ví dụ: Java Backend Developer (Junior)"
                      value={offerPosition}
                      onChange={(e) => setOfferPosition(e.target.value)}
                      required
                    />
                    <Input
                      label="Mức lương đề nghị (VND) *"
                      type="number"
                      min={1}
                      placeholder="18000000"
                      value={offerSalary}
                      onChange={(e) => setOfferSalary(e.target.value)}
                      required
                    />
                    <Input
                      label="Ngày bắt đầu làm việc *"
                      type="date"
                      value={offerStartDate}
                      onChange={(e) => setOfferStartDate(e.target.value)}
                      required
                    />
                    <Input
                      label="Hạn ứng viên trả lời *"
                      type="datetime-local"
                      value={offerDeadline}
                      onChange={(e) => setOfferDeadline(e.target.value)}
                      required
                    />
                  </div>

                  <Input
                    label="Địa điểm làm việc *"
                    placeholder="Ví dụ: Tầng 21, Tòa nhà Handico, Nam Từ Liêm, Hà Nội"
                    value={offerWorkLocation}
                    onChange={(e) => setOfferWorkLocation(e.target.value)}
                    required
                  />

                  <div className="space-y-1.5">
                    <label htmlFor="offerNote" className="block text-sm font-medium text-foreground">
                      Ghi chú / Phúc lợi kèm theo
                    </label>
                    <textarea
                      id="offerNote"
                      rows={3}
                      value={offerNote}
                      onChange={(e) => setOfferNote(e.target.value)}
                      placeholder="Mô tả ngắn về gói đãi ngộ, thời gian thử việc hoặc lời nhắn chào mừng..."
                      className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                    />
                  </div>

                  <div className="flex justify-end">
                    <Button
                      type="submit"
                      variant="primary"
                      size="sm"
                      leftIcon={<Send className="h-4 w-4" />}
                      isLoading={sendOfferMutation.isPending}
                    >
                      Gửi thư mời nhận việc (Offer)
                    </Button>
                  </div>
                </form>
              )}
            </CardContent>
          </Card>
        );
      })()}

      {/* Block: Dòng thời gian trạng thái (Status History Timeline) */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Clock className="h-4 w-4 text-primary" />
            Lịch sử & Dòng thời gian chuyển bước ({detail.history.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-border">
            {detail.history.map((item) => (
              <div key={item.id} className="relative group">
                <div className="absolute -left-6 top-1.5 h-3 w-3 rounded-full bg-primary ring-4 ring-primary-light" />
                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-semibold text-foreground">
                      {item.fromStatus}
                    </span>
                    <ChevronRight className="h-3 w-3 text-muted" />
                    <Badge
                      variant={STATUS_CONFIG[item.toStatus]?.variant || "default"}
                      size="sm"
                    >
                      {item.toStatus}
                    </Badge>
                    <span className="text-[11px] text-muted">
                      ({new Date(item.createdAt).toLocaleString("vi-VN")})
                    </span>
                  </div>
                  {item.note && (
                    <p className="text-xs text-muted leading-relaxed pl-1">
                      {item.note}
                    </p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Modal: Xác nhận chuyển trạng thái */}
      <Modal
        isOpen={!!transitionTarget}
        onClose={() => setTransitionTarget(null)}
        title="Xác nhận chuyển bước tuyển dụng"
        description={`Bạn đang chuyển đơn sang trạng thái "${transitionTarget}". Hành động này không thể hoàn tác.`}
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setTransitionTarget(null)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={transitionMutation.isPending}
              onClick={() => {
                if (transitionTarget) {
                  transitionMutation.mutate({
                    toStatus: transitionTarget,
                    note: transitionNote.trim() || undefined,
                  });
                }
              }}
            >
              Xác nhận chuyển
            </Button>
          </>
        }
      >
        <div className="space-y-3">
          <label className="block text-xs font-medium text-foreground">
            Ghi chú chuyển bước (tùy chọn):
          </label>
          <textarea
            value={transitionNote}
            onChange={(e) => setTransitionNote(e.target.value)}
            placeholder="Nhập lý do hoặc thông tin phản hồi..."
            rows={3}
            className="w-full rounded-lg border border-border p-2.5 text-xs text-foreground outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
        </div>
      </Modal>

      {/* Modal: Tạo vòng phỏng vấn mới */}
      <Modal
        isOpen={isCreateRoundOpen}
        onClose={() => setIsCreateRoundOpen(false)}
        title="Thêm vòng phỏng vấn mới"
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setIsCreateRoundOpen(false)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={createRoundMutation.isPending}
              onClick={handleCreateRoundSubmit}
            >
              Tạo vòng
            </Button>
          </>
        }
      >
        <form onSubmit={handleCreateRoundSubmit} className="space-y-4">
          <Input
            label="Tên vòng phỏng vấn"
            required
            placeholder="VD: Phỏng vấn Kỹ thuật, Phỏng vấn Văn hóa..."
            value={newRoundName}
            onChange={(e) => setNewRoundName(e.target.value)}
          />
          <Input
            label="Email người phỏng vấn (Interviewer)"
            type="email"
            placeholder="interviewer@company.com"
            value={newRoundEmail}
            onChange={(e) => setNewRoundEmail(e.target.value)}
          />
          <Input
            label="Thời gian dự kiến"
            type="datetime-local"
            value={newRoundDate}
            onChange={(e) => setNewRoundDate(e.target.value)}
          />
        </form>
      </Modal>

      {/* Modal: Nhập kết quả vòng phỏng vấn */}
      <Modal
        isOpen={!!editingRound}
        onClose={() => setEditingRound(null)}
        title={`Kết quả: Vòng ${editingRound?.roundNo} - ${editingRound?.name}`}
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setEditingRound(null)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={updateRoundMutation.isPending}
              onClick={handleSaveRound}
            >
              Lưu kết quả
            </Button>
          </>
        }
      >
        <form onSubmit={handleSaveRound} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-foreground mb-1">
              Đánh giá kết quả <span className="text-danger">*</span>
            </label>
            <select
              value={editRoundResult}
              onChange={(e) => setEditRoundResult(e.target.value as InterviewRoundResult)}
              className="w-full rounded-lg border border-border p-2 text-xs bg-surface text-foreground outline-none focus:border-primary"
            >
              <option value="PENDING">Chờ kết quả (PENDING)</option>
              <option value="PASS">Đạt (PASS)</option>
              <option value="FAIL">Không đạt (FAIL)</option>
            </select>
          </div>

          <Input
            label="Điểm số (0 - 100)"
            type="number"
            min={0}
            max={100}
            placeholder="VD: 85"
            value={editRoundScore}
            onChange={(e) => setEditRoundScore(e.target.value)}
          />

          <Input
            label="Thời gian thực tế"
            type="datetime-local"
            value={editRoundDate}
            onChange={(e) => setEditRoundDate(e.target.value)}
          />

          <div>
            <label className="block text-xs font-medium text-foreground mb-1">
              Ghi chú / Nhận xét của người phỏng vấn:
            </label>
            <textarea
              value={editRoundNote}
              onChange={(e) => setEditRoundNote(e.target.value)}
              placeholder="Nhập đánh giá chi tiết..."
              rows={3}
              className="w-full rounded-lg border border-border p-2.5 text-xs text-foreground outline-none focus:border-primary"
            />
          </div>
        </form>
      </Modal>

      {/* UT-06 Modal: Đề xuất khung giờ / Đổi lịch phỏng vấn */}
      <Modal
        isOpen={!!slotModalRound}
        onClose={() => setSlotModalRound(null)}
        title={
          slotModalMode === "reschedule"
            ? `Đổi lịch phỏng vấn — Vòng ${slotModalRound?.roundNo}: ${slotModalRound?.name}`
            : `Đề xuất khung giờ — Vòng ${slotModalRound?.roundNo}: ${slotModalRound?.name}`
        }
        size="lg"
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setSlotModalRound(null)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              type="submit"
              form="ut06-slots-form"
              isLoading={slotMutation.isPending}
            >
              {slotModalMode === "reschedule"
                ? "Hủy lịch cũ & Gửi khung mới"
                : "Gửi đề xuất khung giờ"}
            </Button>
          </>
        }
      >
        <form id="ut06-slots-form" onSubmit={handleSubmitSlots} className="space-y-4">
          <p className="text-xs text-muted">
            {slotModalMode === "reschedule"
              ? "Lịch phỏng vấn và các khung giờ hiện tại sẽ bị hủy (CANCELLED). Vui lòng đề xuất 1–5 khung giờ mới trong tương lai (mỗi khung tối thiểu 30 phút)."
              : "Đề xuất từ 1 đến 5 khung giờ trong tương lai (mỗi khung tối thiểu 30 phút) để ứng viên tự chọn và hệ thống tạo phòng Rikkei Meet."}
          </p>

          <div className="space-y-3">
            {slotDrafts.map((slot, index) => (
              <div
                key={index}
                className="p-3 rounded-xl border border-border bg-slate-50/70 grid grid-cols-1 sm:grid-cols-12 gap-3 items-end"
              >
                <div className="sm:col-span-5">
                  <Input
                    label={`Khung #${index + 1} — Bắt đầu *`}
                    type="datetime-local"
                    value={slot.startAt}
                    onChange={(e) => {
                      const val = e.target.value;
                      setSlotDrafts((prev) =>
                        prev.map((item, idx) =>
                          idx === index ? { ...item, startAt: val } : item
                        )
                      );
                    }}
                    required
                  />
                </div>
                <div className="sm:col-span-5">
                  <Input
                    label="Kết thúc *"
                    type="datetime-local"
                    value={slot.endAt}
                    onChange={(e) => {
                      const val = e.target.value;
                      setSlotDrafts((prev) =>
                        prev.map((item, idx) =>
                          idx === index ? { ...item, endAt: val } : item
                        )
                      );
                    }}
                    required
                  />
                </div>
                <div className="sm:col-span-2 flex justify-end">
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    disabled={slotDrafts.length <= 1}
                    onClick={() =>
                      setSlotDrafts((prev) => prev.filter((_, idx) => idx !== index))
                    }
                    leftIcon={<Trash2 className="h-3.5 w-3.5 text-danger" />}
                  >
                    Xóa
                  </Button>
                </div>
              </div>
            ))}
          </div>

          {slotDrafts.length < 5 && (
            <Button
              type="button"
              size="sm"
              variant="outline"
              leftIcon={<Plus className="h-3.5 w-3.5" />}
              onClick={() => setSlotDrafts((prev) => [...prev, defaultSlotDraft()])}
            >
              Thêm khung giờ ({slotDrafts.length}/5)
            </Button>
          )}
        </form>
      </Modal>

      {/* Modal: Xác nhận xóa vòng phỏng vấn */}
      <Modal
        isOpen={!!deletingRound}
        onClose={() => setDeletingRound(null)}
        title="Xóa vòng phỏng vấn"
        size="sm"
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setDeletingRound(null)}>
              Hủy
            </Button>
            <Button
              variant="danger"
              size="sm"
              isLoading={deleteRoundMutation.isPending}
              onClick={() => deletingRound && deleteRoundMutation.mutate(deletingRound.id)}
            >
              Xác nhận xóa
            </Button>
          </>
        }
      >
        <div className="space-y-3 py-1">
          <p className="text-xs text-foreground leading-relaxed">
            Bạn có chắc chắn muốn xóa <strong>Vòng {deletingRound?.roundNo}: {deletingRound?.name}</strong> không?
          </p>
          <p className="text-[11px] text-muted leading-relaxed">
            Hành động này sẽ xóa vĩnh viễn vòng phỏng vấn, các khung giờ đề xuất và phòng họp Rikkei Meet liên quan (nếu có).
          </p>
        </div>
      </Modal>
    </div>
  );
}

