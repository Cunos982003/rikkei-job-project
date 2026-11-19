import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { CalendarCheck, ChevronRight, FileText, Mail, RefreshCw } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Skeleton } from "@/components/feedback/Skeleton";
import { offersApi } from "@/api/endpoints/offers.api";
import type { RecruitmentStatus } from "@/api/endpoints/recruitment.api";

const STATUS_BADGE: Record<
  RecruitmentStatus,
  { label: string; variant: "default" | "primary" | "warning" | "danger" | "verified" }
> = {
  SUBMITTED: { label: "Mới nộp", variant: "default" },
  SCREENING: { label: "Sàng lọc", variant: "warning" },
  INTERVIEW: { label: "Phỏng vấn", variant: "primary" },
  OFFER: { label: "Đã gửi Offer", variant: "verified" },
  HIRED: { label: "Đã nhận việc (HIRED)", variant: "verified" },
  REJECTED: { label: "Từ chối", variant: "danger" },
  WITHDRAWN: { label: "Đã rút", variant: "danger" },
};

const FILTER_OPTIONS: { value: "" | RecruitmentStatus; label: string }[] = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "INTERVIEW", label: "Đang phỏng vấn (INTERVIEW)" },
  { value: "OFFER", label: "Đã gửi Offer (OFFER)" },
  { value: "HIRED", label: "Đã tuyển (HIRED)" },
  { value: "SCREENING", label: "Sàng lọc (SCREENING)" },
  { value: "REJECTED", label: "Từ chối (REJECTED)" },
];

export function HrApplicationsPage() {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<"" | RecruitmentStatus>("");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["hrApplications", statusFilter, page],
    queryFn: () =>
      offersApi.listHrApplications({
        status: statusFilter || undefined,
        page,
      }),
  });

  const pageData = data?.data;
  const items = pageData?.content ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
            <CalendarCheck className="h-6 w-6 text-primary" />
            Quản lý Phỏng vấn &amp; Offer
          </h1>
          <p className="text-sm text-muted mt-1">
            Theo dõi đơn ứng tuyển của doanh nghiệp, chấm vòng phỏng vấn, gửi Offer và xác nhận ứng viên đi làm.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          leftIcon={<RefreshCw className="h-4 w-4" />}
          onClick={() => refetch()}
        >
          Làm mới
        </Button>
      </div>

      {/* Bộ lọc trạng thái */}
      <div className="flex flex-wrap gap-2">
        {FILTER_OPTIONS.map((opt) => (
          <button
            key={opt.value || "ALL"}
            type="button"
            onClick={() => {
              setStatusFilter(opt.value);
              setPage(0);
            }}
            className={`px-3.5 py-1.5 rounded-full text-xs font-semibold border transition-colors cursor-pointer ${
              statusFilter === opt.value
                ? "bg-primary text-white border-primary"
                : "bg-surface text-foreground border-border hover:bg-slate-50"
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <FileText className="h-4 w-4 text-primary" />
            Danh sách đơn ứng tuyển ({pageData?.totalElements ?? items.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          ) : isError ? (
            <div className="py-10 text-center text-sm text-danger">
              Không thể tải danh sách đơn ứng tuyển. Vui lòng kiểm tra kết nối dịch vụ tuyển dụng.
            </div>
          ) : items.length === 0 ? (
            <div className="py-12 text-center text-sm text-muted">
              Chưa có đơn ứng tuyển nào phù hợp bộ lọc hiện tại.
            </div>
          ) : (
            <div className="divide-y divide-border">
              {items.map((app) => {
                const badge = STATUS_BADGE[app.status] || { label: app.status, variant: "default" as const };
                return (
                  <div
                    key={app.id}
                    className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/70 px-3 -mx-3 rounded-xl transition-colors"
                  >
                    <div className="space-y-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-sm text-foreground flex items-center gap-1.5">
                          <Mail className="h-3.5 w-3.5 text-muted" />
                          {app.candidateEmail || app.candidateId}
                        </span>
                        <Badge variant={badge.variant} size="sm">
                          {badge.label}
                        </Badge>
                      </div>
                      <p className="text-xs text-muted font-mono truncate">
                        Đơn: {app.id} · Job: {app.jobId}
                      </p>
                      <p className="text-xs text-muted">
                        Cập nhật: {new Date(app.updatedAt).toLocaleString("vi-VN")}
                      </p>
                    </div>

                    <Button
                      size="sm"
                      variant="primary"
                      rightIcon={<ChevronRight className="h-4 w-4" />}
                      onClick={() => navigate(`/hr/applications/${app.id}`)}
                    >
                      Chi tiết &amp; Offer
                    </Button>
                  </div>
                );
              })}
            </div>
          )}

          {pageData && pageData.totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 mt-4 border-t border-border">
              <span className="text-xs text-muted">
                Trang {pageData.number + 1} / {pageData.totalPages}
              </span>
              <div className="flex gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={pageData.first}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  Trang trước
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={pageData.last}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Trang sau
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
