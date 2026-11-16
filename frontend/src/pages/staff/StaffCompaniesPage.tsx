import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Building2,
  Search,
  ChevronLeft,
  ChevronRight,
  Eye,
  RefreshCw,
  Users,
  Briefcase,
  AlertCircle,
  Plus,
  KeyRound,
} from "lucide-react";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";
import { Card, CardContent } from "../../components/common/Card";
import { Skeleton } from "../../components/feedback/Skeleton";
import { CreateCompanyModal } from "../../components/staff/CreateCompanyModal";
import { CreateHrModal } from "../../components/staff/CreateHrModal";
import { staffCompaniesApi, type StaffCompanyListItem } from "../../api/endpoints/staffCompanies.api";
import { resolveAssetUrl } from "../../api/client";

export function StaffCompaniesPage() {
  const navigate = useNavigate();

  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [selectedStatus, setSelectedStatus] = useState<string>("");
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  const [isCreateCompanyOpen, setIsCreateCompanyOpen] = useState(false);
  const [isCreateHrOpen, setIsCreateHrOpen] = useState(false);

  // Debounce search input (350ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchInput.trim());
      setCurrentPage(0);
    }, 350);
    return () => clearTimeout(handler);
  }, [searchInput]);

  const {
    data: response,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["staff", "companies", { q: debouncedSearch, status: selectedStatus, page: currentPage, size: pageSize }],
    queryFn: async () => {
      return (
        await staffCompaniesApi.list({
          q: debouncedSearch || undefined,
          status: selectedStatus || undefined,
          page: currentPage,
          size: pageSize,
        })
      ).data;
    },
  });

  const companies: StaffCompanyListItem[] = response?.items ?? [];
  const pagination = response?.pagination;
  const totalElements = pagination?.totalElements ?? 0;
  const totalPages = pagination?.totalPages ?? 0;

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Tiêu đề trang */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Doanh nghiệp</h1>
          <p className="mt-1 text-sm text-muted">
            Quản lý danh sách doanh nghiệp đối tác và các tài khoản HR trực thuộc.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<KeyRound className="h-4 w-4" />}
            onClick={() => setIsCreateHrOpen(true)}
          >
            Cấp tài khoản HR
          </Button>
          <Button
            variant="primary"
            size="sm"
            leftIcon={<Plus className="h-4 w-4" />}
            onClick={() => setIsCreateCompanyOpen(true)}
          >
            Tạo doanh nghiệp mới
          </Button>
        </div>
      </div>

      <CreateCompanyModal
        isOpen={isCreateCompanyOpen}
        onClose={() => setIsCreateCompanyOpen(false)}
        onSuccess={(created) => {
          if (created?.id) navigate(`/staff/companies/${created.id}`);
        }}
      />

      <CreateHrModal
        isOpen={isCreateHrOpen}
        onClose={() => setIsCreateHrOpen(false)}
      />

      {/* Thanh tìm kiếm & bộ lọc */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            {/* Input tìm kiếm */}
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted pointer-events-none" />
              <input
                type="text"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Tìm kiếm theo tên doanh nghiệp hoặc mã số thuế..."
                className="w-full pl-9 pr-4 py-2 text-sm bg-surface rounded-lg border border-border text-foreground placeholder:text-muted outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
              />
            </div>

            {/* Lọc trạng thái */}
            <div className="flex items-center gap-2">
              <label htmlFor="status-filter" className="text-xs font-medium text-muted shrink-0">
                Trạng thái:
              </label>
              <select
                id="status-filter"
                value={selectedStatus}
                onChange={(e) => {
                  setSelectedStatus(e.target.value);
                  setCurrentPage(0);
                }}
                className="rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
              >
                <option value="">Tất cả trạng thái</option>
                <option value="ACTIVE">Hoạt động</option>
                <option value="LOCKED">Đã khóa</option>
              </select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Bảng danh sách doanh nghiệp */}
      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 space-y-4">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <Skeleton className="h-10 w-10 rounded-lg" />
                    <div className="space-y-1">
                      <Skeleton className="h-4 w-48" />
                      <Skeleton className="h-3 w-32" />
                    </div>
                  </div>
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-16" />
                  <Skeleton className="h-6 w-20 rounded-full" />
                  <Skeleton className="h-8 w-24 rounded-lg" />
                </div>
              ))}
            </div>
          ) : isError ? (
            <div className="flex flex-col items-center justify-center p-12 text-center">
              <div className="rounded-full bg-danger-light p-3 text-danger mb-3">
                <AlertCircle className="h-6 w-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">Không thể tải danh sách doanh nghiệp</h3>
              <p className="mt-1 text-sm text-muted max-w-md">
                Đã có sự cố kết nối tới máy chủ. Vui lòng kiểm tra lại đường truyền mạng hoặc thử lại sau.
              </p>
              <Button
                variant="outline"
                size="sm"
                className="mt-4"
                leftIcon={<RefreshCw className="h-4 w-4" />}
                onClick={() => void refetch()}
              >
                Thử lại
              </Button>
            </div>
          ) : companies.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center">
              <div className="rounded-full bg-slate-100 p-3 text-slate-400 mb-3">
                <Building2 className="h-6 w-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">Không tìm thấy doanh nghiệp nào</h3>
              <p className="mt-1 text-sm text-muted max-w-md">
                {searchInput || selectedStatus
                  ? "Không có doanh nghiệp nào khớp với tiêu chí tìm kiếm hoặc bộ lọc hiện tại."
                  : "Chưa có doanh nghiệp nào được tạo trên hệ thống."}
              </p>
              {(searchInput || selectedStatus) && (
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-4"
                  onClick={() => {
                    setSearchInput("");
                    setSelectedStatus("");
                    setCurrentPage(0);
                  }}
                >
                  Xóa bộ lọc
                </Button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs font-semibold text-muted uppercase tracking-wider border-b border-border">
                  <tr>
                    <th scope="col" className="px-6 py-3.5">Doanh nghiệp</th>
                    <th scope="col" className="px-6 py-3.5">Mã số thuế</th>
                    <th scope="col" className="px-6 py-3.5 text-center">Số lượng HR</th>
                    <th scope="col" className="px-6 py-3.5 text-center">Số lượng JD</th>
                    <th scope="col" className="px-6 py-3.5">Trạng thái</th>
                    <th scope="col" className="px-6 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {companies.map((company) => (
                    <tr
                      key={company.id}
                      className="hover:bg-slate-50/60 transition-colors"
                    >
                      {/* Doanh nghiệp (logo + tên + lĩnh vực) */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-border bg-surface">
                            {company.logoUrl ? (
                              <img
                                src={resolveAssetUrl(company.logoUrl)}
                                alt={`Logo ${company.name}`}
                                className="h-full w-full object-contain p-0.5"
                              />
                            ) : (
                              <Building2 className="h-5 w-5 text-slate-300" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <p className="font-semibold text-foreground truncate max-w-xs sm:max-w-md">
                              {company.name}
                            </p>
                            <p className="text-xs text-muted truncate max-w-xs">
                              {company.industry || "Chưa cập nhật ngành nghề"}
                              {company.city ? ` • ${company.city}` : ""}
                            </p>
                          </div>
                        </div>
                      </td>

                      {/* Mã số thuế */}
                      <td className="px-6 py-4 font-mono text-xs text-foreground whitespace-nowrap">
                        {company.taxCode}
                      </td>

                      {/* Số lượng HR */}
                      <td className="px-6 py-4 text-center whitespace-nowrap">
                        <span className="inline-flex items-center gap-1 font-medium text-foreground">
                          <Users className="h-3.5 w-3.5 text-muted" />
                          {company.totalHr}
                        </span>
                      </td>

                      {/* Số lượng JD */}
                      <td className="px-6 py-4 text-center whitespace-nowrap">
                        <span className="inline-flex items-center gap-1 font-medium text-foreground">
                          <Briefcase className="h-3.5 w-3.5 text-muted" />
                          {company.totalJd}
                        </span>
                      </td>

                      {/* Trạng thái */}
                      <td className="px-6 py-4 whitespace-nowrap">
                        {company.status === "ACTIVE" ? (
                          <Badge variant="verified" size="sm">
                            Hoạt động
                          </Badge>
                        ) : (
                          <Badge variant="danger" size="sm">
                            Đã khóa
                          </Badge>
                        )}
                      </td>

                      {/* Thao tác */}
                      <td className="px-6 py-4 text-right whitespace-nowrap">
                        <Button
                          variant="outline"
                          size="sm"
                          leftIcon={<Eye className="h-3.5 w-3.5" />}
                          onClick={() => navigate(`/staff/companies/${company.id}`)}
                        >
                          Xem chi tiết
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Phân trang */}
          {!isLoading && !isError && totalElements > 0 && (
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-6 py-4 border-t border-border text-xs text-muted">
              <div>
                Hiển thị{" "}
                <span className="font-semibold text-foreground">
                  {currentPage * pageSize + 1}
                </span>{" "}
                -{" "}
                <span className="font-semibold text-foreground">
                  {Math.min((currentPage + 1) * pageSize, totalElements)}
                </span>{" "}
                trong tổng số{" "}
                <span className="font-semibold text-foreground">{totalElements}</span> doanh nghiệp
              </div>

              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  leftIcon={<ChevronLeft className="h-4 w-4" />}
                >
                  Trang trước
                </Button>
                <span className="px-2 text-foreground font-medium">
                  Trang {currentPage + 1} / {Math.max(1, totalPages)}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => setCurrentPage((p) => p + 1)}
                  rightIcon={<ChevronRight className="h-4 w-4" />}
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

