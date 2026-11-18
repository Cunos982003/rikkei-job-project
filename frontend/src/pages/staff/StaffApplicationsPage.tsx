import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Search,
  Filter,
  Eye,
  ChevronLeft,
  ChevronRight,
  AlertCircle,
  Building,
  Briefcase,
} from "lucide-react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";
import { Skeleton } from "@/components/feedback/Skeleton";
import {
  recruitmentApi,
  type RecruitmentStatus,
  type StaffRecruitmentFilterParams,
} from "@/api/endpoints/recruitment.api";

const STATUS_CONFIG: Record<
  RecruitmentStatus,
  { label: string; variant: "default" | "primary" | "warning" | "danger" | "verified" }
> = {
  SUBMITTED: { label: "Mới nộp", variant: "default" },
  SCREENING: { label: "Sàng lọc", variant: "warning" },
  INTERVIEW: { label: "Phỏng vấn", variant: "primary" },
  OFFER: { label: "Đề nghị tuyển dụng", variant: "verified" },
  HIRED: { label: "Đã tuyển", variant: "verified" },
  REJECTED: { label: "Từ chối", variant: "danger" },
  WITHDRAWN: { label: "Rút đơn", variant: "danger" },
};

export function StaffApplicationsPage() {
  const navigate = useNavigate();

  // Filters state
  const [companyId, setCompanyId] = useState("");
  const [jobId, setJobId] = useState("");
  const [status, setStatus] = useState<RecruitmentStatus | "">("");
  const [page, setPage] = useState(0);

  // Active filter params
  const [activeParams, setActiveParams] = useState<StaffRecruitmentFilterParams>({
    companyId: undefined,
    jobId: undefined,
    status: undefined,
    page: 0,
  });

  const {
    data: response,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["staff", "recruitments", activeParams],
    queryFn: () => recruitmentApi.listForStaff(activeParams),
  });

  const pageData = response?.data;
  const recruitments = pageData?.content || [];
  const totalPages = pageData?.totalPages || 0;
  const totalElements = pageData?.totalElements || 0;

  const handleApplyFilter = (e: React.FormEvent) => {
    e.preventDefault();
    setActiveParams({
      companyId: companyId.trim() || undefined,
      jobId: jobId.trim() || undefined,
      status: (status as RecruitmentStatus) || undefined,
      page: 0,
    });
    setPage(0);
  };

  const handleResetFilter = () => {
    setCompanyId("");
    setJobId("");
    setStatus("");
    setActiveParams({ page: 0 });
    setPage(0);
  };

  const handleChangePage = (newPage: number) => {
    setPage(newPage);
    setActiveParams((prev) => ({ ...prev, page: newPage }));
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">
            Quản Lý & Theo Dõi Đơn Ứng Tuyển
          </h1>
          <p className="text-xs text-muted mt-1">
            Phân hệ Cán bộ vận hành (STAFF) - Giám sát toàn bộ tiến trình ứng tuyển của học viên
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="primary" size="md">
            Tổng cộng: {totalElements} đơn
          </Badge>
        </div>
      </div>

      {/* Filter Toolbar */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm font-semibold flex items-center gap-2">
            <Filter className="h-4 w-4 text-primary" />
            Bộ lọc tìm kiếm
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleApplyFilter} className="grid grid-cols-1 sm:grid-cols-4 gap-4 items-end">
            <Input
              label="Mã Doanh Nghiệp (Company ID)"
              placeholder="UUID công ty..."
              leftIcon={<Building className="h-3.5 w-3.5 text-muted" />}
              value={companyId}
              onChange={(e) => setCompanyId(e.target.value)}
            />
            <Input
              label="Mã Tin Tuyển Dụng (Job ID)"
              placeholder="UUID tin JD..."
              leftIcon={<Briefcase className="h-3.5 w-3.5 text-muted" />}
              value={jobId}
              onChange={(e) => setJobId(e.target.value)}
            />
            <div>
              <label className="block text-xs font-medium text-foreground mb-1.5">
                Trạng thái tuyển dụng
              </label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value as RecruitmentStatus | "")}
                className="w-full rounded-lg border border-border p-2 text-xs bg-surface text-foreground outline-none focus:border-primary"
              >
                <option value="">Tất cả trạng thái</option>
                <option value="SUBMITTED">Mới nộp (SUBMITTED)</option>
                <option value="SCREENING">Sàng lọc (SCREENING)</option>
                <option value="INTERVIEW">Phỏng vấn (INTERVIEW)</option>
                <option value="OFFER">Đề nghị tuyển dụng (OFFER)</option>
                <option value="HIRED">Đã tuyển dụng (HIRED)</option>
                <option value="REJECTED">Từ chối (REJECTED)</option>
                <option value="WITHDRAWN">Ứng viên rút đơn (WITHDRAWN)</option>
              </select>
            </div>
            <div className="flex items-center gap-2">
              <Button type="submit" variant="primary" size="sm" className="flex-1" leftIcon={<Search className="h-3.5 w-3.5" />}>
                Áp dụng
              </Button>
              <Button type="button" variant="outline" size="sm" onClick={handleResetFilter}>
                Đặt lại
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {/* Table of Applications */}
      <Card>
        <CardContent className="p-0 overflow-x-auto">
          {isLoading ? (
            <div className="p-6 space-y-3">
              {Array.from({ length: 5 }).map((_, idx) => (
                <Skeleton key={idx} className="h-12 w-full" />
              ))}
            </div>
          ) : isError ? (
            <div className="text-center py-12 space-y-3">
              <AlertCircle className="h-10 w-10 text-danger mx-auto" />
              <p className="text-sm font-medium text-foreground">Không thể tải danh sách đơn ứng tuyển</p>
              <Button size="sm" variant="outline" onClick={() => refetch()}>
                Thử lại
              </Button>
            </div>
          ) : recruitments.length === 0 ? (
            <div className="text-center py-12 text-xs text-muted">
              Không tìm thấy đơn ứng tuyển nào phù hợp với bộ lọc.
            </div>
          ) : (
            <table className="w-full text-left text-xs text-foreground">
              <thead className="bg-slate-50 border-b border-border text-muted font-medium uppercase text-[11px]">
                <tr>
                  <th className="py-3 px-4">Mã đơn</th>
                  <th className="py-3 px-4">Email ứng viên</th>
                  <th className="py-3 px-4">Mã JD (Job ID)</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4">Ngày nộp</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border-subtle">
                {recruitments.map((item) => {
                  const cfg = STATUS_CONFIG[item.status] || {
                    label: item.status,
                    variant: "default",
                  };
                  return (
                    <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-4 font-mono font-medium">
                        #{item.id.substring(0, 8)}
                      </td>
                      <td className="py-3 px-4 font-medium text-foreground">
                        {item.candidateEmail}
                      </td>
                      <td className="py-3 px-4 font-mono text-muted text-[11px]">
                        {item.jobId.substring(0, 8)}...
                      </td>
                      <td className="py-3 px-4">
                        <Badge variant={cfg.variant} size="sm">
                          {cfg.label}
                        </Badge>
                      </td>
                      <td className="py-3 px-4 text-muted">
                        {new Date(item.createdAt).toLocaleDateString("vi-VN")}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <Button
                          size="sm"
                          variant="ghost"
                          leftIcon={<Eye className="h-3.5 w-3.5" />}
                          onClick={() => navigate(`/staff/applications/${item.id}`)}
                        >
                          Chi tiết
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>

      {/* Pagination Footer */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-muted pt-2">
          <span>
            Trang {page + 1} / {totalPages} (Tổng {totalElements} kết quả)
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => handleChangePage(page - 1)}
              leftIcon={<ChevronLeft className="h-4 w-4" />}
            >
              Trước
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page >= totalPages - 1}
              onClick={() => handleChangePage(page + 1)}
              rightIcon={<ChevronRight className="h-4 w-4" />}
            >
              Sau
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
