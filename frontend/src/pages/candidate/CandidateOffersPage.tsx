import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AlertCircle,
  Briefcase,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  DollarSign,
  Gift,
  MapPin,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Modal } from "@/components/common/Modal";
import { Skeleton } from "@/components/feedback/Skeleton";
import { offersApi, type OfferItem, type OfferStatus } from "@/api/endpoints/offers.api";
import { useAuthStore } from "@/store/useAuthStore";

const OFFER_STATUS_CONFIG: Record<
  OfferStatus,
  { label: string; variant: "primary" | "verified" | "danger" | "warning" }
> = {
  SENT: { label: "Chờ bạn phản hồi", variant: "primary" },
  ACCEPTED: { label: "Đã chấp nhận (HIRED)", variant: "verified" },
  DECLINED: { label: "Đã từ chối", variant: "danger" },
  EXPIRED: { label: "Đã hết hạn (EXPIRED)", variant: "warning" },
};

function formatCurrencyVnd(amount: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(amount);
}

function formatCountdown(msRemaining: number): string {
  if (msRemaining <= 0) return "Đã hết hạn phản hồi";
  const totalSeconds = Math.floor(msRemaining / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const parts: string[] = [];
  if (days > 0) parts.push(`${days} ngày`);
  parts.push(`${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`);
  return `Còn ${parts.join(" ")}`;
}

export function CandidateOffersPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();

  const [nowMs, setNowMs] = useState(() => Date.now());
  const [acceptTarget, setAcceptTarget] = useState<OfferItem | null>(null);
  const [declineTarget, setDeclineTarget] = useState<OfferItem | null>(null);
  const [declineReason, setDeclineReason] = useState("");
  const [declineError, setDeclineError] = useState("");

  useEffect(() => {
    const timer = window.setInterval(() => setNowMs(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["myOffers", user?.id],
    queryFn: () => offersApi.listMyOffers(),
  });

  const acceptMutation = useMutation({
    mutationFn: (offerId: string) => offersApi.acceptOffer(offerId),
    onSuccess: () => {
      toast.success("Chúc mừng! Bạn đã chấp nhận Offer. Kinh nghiệm mới đã được ghi nhận vào hồ sơ của bạn.");
      setAcceptTarget(null);
      queryClient.invalidateQueries({ queryKey: ["myOffers"] });
      queryClient.invalidateQueries({ queryKey: ["myEmploymentRecords"] });
    },
  });

  const declineMutation = useMutation({
    mutationFn: ({ offerId, reason }: { offerId: string; reason: string }) =>
      offersApi.declineOffer(offerId, reason),
    onSuccess: () => {
      toast.success("Bạn đã từ chối Offer và gửi phản hồi tới nhà tuyển dụng.");
      setDeclineTarget(null);
      setDeclineReason("");
      setDeclineError("");
      queryClient.invalidateQueries({ queryKey: ["myOffers"] });
    },
  });

  const offers = data?.data ?? [];

  const handleDeclineSubmit = () => {
    if (!declineTarget) return;
    const trimmed = declineReason.trim();
    if (!trimmed) {
      setDeclineError("Vui lòng nhập lý do từ chối trước khi xác nhận.");
      return;
    }
    setDeclineError("");
    declineMutation.mutate({ offerId: declineTarget.id, reason: trimmed });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
            <Gift className="h-6 w-6 text-primary" />
            Thư mời nhận việc (Offer)
          </h1>
          <p className="text-sm text-muted mt-1">
            Xem chi tiết đề nghị tuyển dụng, theo dõi thời hạn phản hồi và xác nhận nhận việc. Khi bạn chấp nhận Offer, công việc mới sẽ tự động hiển thị trên hồ sơ của bạn.
          </p>
        </div>

        <Button variant="outline" size="sm" onClick={() => navigate("/candidate/profile")}>
          Xem hồ sơ cá nhân
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-44 w-full" />
          <Skeleton className="h-44 w-full" />
        </div>
      ) : isError ? (
        <Card>
          <CardContent className="py-10 text-center text-sm text-danger">
            Không thể tải danh sách thư mời nhận việc. Vui lòng thử lại sau.
          </CardContent>
        </Card>
      ) : offers.length === 0 ? (
        <Card>
          <CardContent className="py-14 text-center space-y-2">
            <Gift className="h-10 w-10 text-muted mx-auto opacity-60" />
            <p className="text-sm font-medium text-foreground">Bạn chưa có thư mời nhận việc nào</p>
            <p className="text-xs text-muted">
              Khi doanh nghiệp gửi Offer sau vòng phỏng vấn đạt (PASS), thông tin sẽ xuất hiện tại đây.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-5">
          {offers.map((offer) => {
            const deadlineMs = new Date(offer.responseDeadline).getTime();
            const remainingMs = deadlineMs - nowMs;
            const isExpiredNow = offer.status === "EXPIRED" || (offer.status === "SENT" && remainingMs <= 0);
            const effectiveStatus: OfferStatus = isExpiredNow ? "EXPIRED" : offer.status;
            const statusCfg = OFFER_STATUS_CONFIG[effectiveStatus];
            const canRespond = offer.status === "SENT" && !isExpiredNow;

            return (
              <Card key={offer.id} className={effectiveStatus === "ACCEPTED" ? "border-emerald-300" : ""}>
                <CardHeader>
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                    <div className="space-y-1">
                      <CardTitle className="text-lg flex items-center gap-2 flex-wrap">
                        <Briefcase className="h-5 w-5 text-primary" />
                        <span>{offer.position}</span>
                        <Badge variant={statusCfg.variant} size="sm">
                          {statusCfg.label}
                        </Badge>
                      </CardTitle>
                      <p className="text-sm text-muted flex items-center gap-1.5">
                        <Building2 className="h-4 w-4" />
                        {offer.companyName || "Doanh nghiệp tuyển dụng"}
                      </p>
                    </div>

                    {/* Đếm ngược hạn trả lời */}
                    {offer.status === "SENT" && (
                      <div
                        className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold border ${
                          isExpiredNow
                            ? "bg-amber-50 text-amber-800 border-amber-200"
                            : "bg-primary-light text-primary border-primary-border"
                        }`}
                      >
                        <Clock className="h-3.5 w-3.5" />
                        <span>{formatCountdown(remainingMs)}</span>
                      </div>
                    )}
                  </div>
                </CardHeader>

                <CardContent className="space-y-4">
                  <div className="grid sm:grid-cols-3 gap-3 text-sm">
                    <div className="rounded-xl bg-slate-50 border border-border p-3">
                      <span className="text-xs text-muted flex items-center gap-1">
                        <DollarSign className="h-3.5 w-3.5 text-emerald-600" />
                        Mức lương đề nghị
                      </span>
                      <p className="font-bold text-foreground mt-1">{formatCurrencyVnd(offer.salary)}</p>
                    </div>
                    <div className="rounded-xl bg-slate-50 border border-border p-3">
                      <span className="text-xs text-muted flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5 text-primary" />
                        Ngày bắt đầu dự kiến
                      </span>
                      <p className="font-semibold text-foreground mt-1">
                        {new Date(offer.startDate).toLocaleDateString("vi-VN")}
                      </p>
                    </div>
                    <div className="rounded-xl bg-slate-50 border border-border p-3">
                      <span className="text-xs text-muted flex items-center gap-1">
                        <Clock className="h-3.5 w-3.5 text-amber-600" />
                        Hạn phản hồi
                      </span>
                      <p className="font-semibold text-foreground mt-1">
                        {new Date(offer.responseDeadline).toLocaleString("vi-VN")}
                      </p>
                    </div>
                  </div>

                  <div className="text-sm text-foreground flex items-start gap-2">
                    <MapPin className="h-4 w-4 text-muted shrink-0 mt-0.5" />
                    <span>
                      <span className="text-muted">Địa điểm làm việc: </span>
                      {offer.workLocation}
                    </span>
                  </div>

                  {offer.note && (
                    <div className="rounded-xl bg-slate-50 border border-border p-3 text-xs text-foreground/90 leading-relaxed">
                      <span className="font-semibold text-foreground">Lời nhắn từ HR: </span>
                      {offer.note}
                    </div>
                  )}

                  {effectiveStatus === "EXPIRED" && (
                    <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 text-xs text-amber-900 flex items-center gap-2">
                      <AlertCircle className="h-4 w-4 shrink-0" />
                      <span>Offer này đã quá hạn trả lời nên không thể chấp nhận hoặc từ chối được nữa.</span>
                    </div>
                  )}

                  {effectiveStatus === "DECLINED" && offer.declineReason && (
                    <div className="rounded-xl bg-rose-50 border border-rose-200 p-3 text-xs text-rose-900">
                      <span className="font-semibold">Lý do bạn đã từ chối: </span>
                      {offer.declineReason}
                    </div>
                  )}

                  {effectiveStatus === "ACCEPTED" && (
                    <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-3 text-xs text-emerald-900 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                      <span className="flex items-center gap-1.5 font-medium">
                        <CheckCircle2 className="h-4 w-4 shrink-0" />
                        Đã ghi nhận vào hồ sơ: Kinh nghiệm: {offer.position} tại {offer.companyName || "Doanh nghiệp tuyển dụng"}
                        {offer.confirmedAt
                          ? ` · HR đã xác nhận đi làm (${new Date(offer.confirmedAt).toLocaleDateString("vi-VN")})`
                          : " · Đang chờ HR xác nhận ngày đi làm"}
                      </span>
                      <Button size="sm" variant="outline" onClick={() => navigate("/candidate/profile")}>
                        Xem trên hồ sơ
                      </Button>
                    </div>
                  )}
                </CardContent>

                <CardFooter className="justify-end gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!canRespond}
                    leftIcon={<XCircle className="h-4 w-4" />}
                    onClick={() => {
                      setDeclineTarget(offer);
                      setDeclineReason("");
                      setDeclineError("");
                    }}
                  >
                    Từ chối Offer
                  </Button>
                  <Button
                    size="sm"
                    variant="primary"
                    disabled={!canRespond}
                    leftIcon={<CheckCircle2 className="h-4 w-4" />}
                    onClick={() => setAcceptTarget(offer)}
                  >
                    Chấp nhận Offer
                  </Button>
                </CardFooter>
              </Card>
            );
          })}
        </div>
      )}

      {/* Modal: Xác nhận Chấp nhận Offer */}
      <Modal
        isOpen={!!acceptTarget}
        onClose={() => setAcceptTarget(null)}
        title="Xác nhận chấp nhận Offer"
        description={
          acceptTarget
            ? `Bạn xác nhận nhận việc ở vị trí "${acceptTarget.position}" tại ${acceptTarget.companyName || "doanh nghiệp"}? Các đơn ứng tuyển khác của bạn vẫn được giữ nguyên để bạn tự quyết định.`
            : ""
        }
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setAcceptTarget(null)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={acceptMutation.isPending}
              onClick={() => {
                if (acceptTarget) acceptMutation.mutate(acceptTarget.id);
              }}
            >
              Xác nhận nhận việc
            </Button>
          </>
        }
      >
        {acceptTarget && (
          <div className="text-xs text-muted space-y-1.5">
            <p>
              • Mức lương: <strong className="text-foreground">{formatCurrencyVnd(acceptTarget.salary)}</strong>
            </p>
            <p>
              • Ngày bắt đầu: <strong className="text-foreground">{new Date(acceptTarget.startDate).toLocaleDateString("vi-VN")}</strong>
            </p>
            <p>
              • Sau khi xác nhận, đơn sẽ chuyển sang <strong>HIRED</strong> và hồ sơ của bạn sẽ hiển thị mục{" "}
              <strong>&quot;Kinh nghiệm: {acceptTarget.position} tại {acceptTarget.companyName || "Doanh nghiệp"}&quot;</strong>.
            </p>
          </div>
        )}
      </Modal>

      {/* Modal: Từ chối Offer (Bắt buộc nhập lý do) */}
      <Modal
        isOpen={!!declineTarget}
        onClose={() => setDeclineTarget(null)}
        title="Từ chối thư mời nhận việc"
        description="Vui lòng cho nhà tuyển dụng biết lý do bạn từ chối Offer này (bắt buộc)."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={() => setDeclineTarget(null)}>
              Hủy
            </Button>
            <Button
              variant="danger"
              size="sm"
              isLoading={declineMutation.isPending}
              onClick={handleDeclineSubmit}
            >
              Xác nhận từ chối
            </Button>
          </>
        }
      >
        <div className="space-y-2">
          <label htmlFor="declineReason" className="block text-sm font-medium text-foreground">
            Lý do từ chối <span className="text-danger">*</span>
          </label>
          <textarea
            id="declineReason"
            rows={4}
            value={declineReason}
            onChange={(e) => {
              setDeclineReason(e.target.value);
              if (declineError) setDeclineError("");
            }}
            placeholder="Ví dụ: Em đã nhận đề nghị phù hợp hơn về khoảng cách địa lý / thời gian bắt đầu..."
            className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
          />
          {declineError && <p className="text-xs text-danger font-medium">{declineError}</p>}
        </div>
      </Modal>
    </div>
  );
}
