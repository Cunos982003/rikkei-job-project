import { useState, useEffect, useMemo } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  AlertCircle,
  Users,
} from "lucide-react";
import { Button } from "@/components/common/Button";
import { Card, CardContent } from "@/components/common/Card";
import { Skeleton } from "@/components/feedback/Skeleton";
import { StaffCandidateTable } from "@/components/staff/StaffCandidateTable";
import { StaffCandidateFilters } from "@/components/staff/StaffCandidateFilters";
import { profileApi, type StaffProfileReviewsParams, type StaffPendingReview, type Visibility } from "@/api/endpoints/profile.api";

export function StaffCandidatesPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // State
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [classCode, setClassCode] = useState("");
  const [status, setStatus] = useState("PENDING");
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  // Get class codes from URL or use empty array initially
  const [classCodes, setClassCodes] = useState<string[]>([]);

  // Debounce search input (350ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearch(searchInput.trim());
      setCurrentPage(0);
      updateUrlParams();
    }, 350);
    return () => clearTimeout(handler);
  }, [searchInput, classCode, status]);

  // Build params for API
  const apiParams: StaffProfileReviewsParams = useMemo(() => ({
    q: debouncedSearch || undefined,
    classCode: classCode || undefined,
    status: (status as Visibility) || undefined,
    page: currentPage,
    size: pageSize,
  }), [debouncedSearch, classCode, status, currentPage]);

  // Fetch staff profile reviews
  const {
    data: response,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ["staff", "profile-reviews", apiParams],
    queryFn: async () => {
      return (await profileApi.getStaffProfileReviews(apiParams)).data;
    },
    staleTime: 30 * 1000,
  });

  const candidates: StaffPendingReview[] = response?.content ?? [];
  const totalElements = response?.totalElements ?? 0;
  const totalPages = response?.totalPages ?? 0;

  // Extract unique class codes from candidates for filter dropdown
  useEffect(() => {
    const codes = candidates.map(c => c.classCode).filter(Boolean) as string[];
    setClassCodes([...new Set(codes)].sort());
  }, [candidates]);

  // Update URL params for shareable links
  const updateUrlParams = () => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("q", debouncedSearch);
    if (classCode) params.set("classCode", classCode);
    if (status && status !== "PENDING") params.set("status", status);
    if (currentPage > 0) params.set("page", currentPage.toString());
    setSearchParams(params, { replace: true });
  };

  // Initialize from URL params on mount
  useEffect(() => {
    const q = searchParams.get("q");
    const cc = searchParams.get("classCode");
    const st = searchParams.get("status");
    const pg = searchParams.get("page");

    if (q) {
      setSearchInput(q);
      setDebouncedSearch(q);
    }
    if (cc) setClassCode(cc);
    if (st) setStatus(st);
    if (pg) setCurrentPage(parseInt(pg, 10));
  }, [searchParams]);

  const handleClearFilters = () => {
    setSearchInput("");
    setDebouncedSearch("");
    setClassCode("");
    setStatus("PENDING");
    setCurrentPage(0);
  };

  const hasActiveFilters = debouncedSearch || classCode || status !== "PENDING";

  const handleRowClick = (candidate: StaffPendingReview) => {
    navigate(`/staff/candidates/${candidate.userId}`);
  };

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      {/* Page Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Duyệt hồ sơ ứng viên</h1>
          <p className="mt-1 text-sm text-muted">
            Xem và duyệt các hồ sơ ứng viên đang chờ phê duyệt. Sắp xếp theo thời gian gửi cũ nhất trước.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            leftIcon={<RefreshCw className="h-4 w-4" />}
            onClick={() => void refetch()}
            disabled={isLoading}
          >
            Làm mới
          </Button>
        </div>
      </div>

      {/* Filters */}
      <StaffCandidateFilters
        q={debouncedSearch}
        onQChange={(q) => {
          setSearchInput(q);
          setDebouncedSearch(q);
          setCurrentPage(0);
        }}
        classCode={classCode}
        onClassCodeChange={(cc) => {
          setClassCode(cc);
          setCurrentPage(0);
        }}
        classCodes={classCodes}
        status={status}
        onStatusChange={(st) => {
          setStatus(st);
          setCurrentPage(0);
        }}
        isLoading={isLoading}
      />

      {/* Candidate Table */}
      <Card>
        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6 space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 border border-border-subtle rounded-lg" />
              ))}
            </div>
          ) : isError ? (
            <div className="flex flex-col items-center justify-center p-12 text-center">
              <div className="rounded-full bg-danger-light p-3 text-danger mb-3">
                <AlertCircle className="h-6 w-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">Không thể tải danh sách hồ sơ</h3>
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
          ) : candidates.length === 0 ? (
            <div className="flex flex-col items-center justify-center p-12 text-center">
              <div className="rounded-full bg-slate-100 p-3 text-slate-400 mb-3">
                <Users className="h-6 w-6" />
              </div>
              <h3 className="text-base font-semibold text-foreground">Không tìm thấy hồ sơ nào</h3>
              <p className="mt-1 text-sm text-muted max-w-md">
                {hasActiveFilters
                  ? "Không có hồ sơ nào khớp với tiêu chí tìm kiếm hoặc bộ lọc hiện tại."
                  : "Không có hồ sơ nào đang chờ duyệt."}
              </p>
              {hasActiveFilters && (
                <Button
                  variant="outline"
                  size="sm"
                  className="mt-4"
                  onClick={handleClearFilters}
                >
                  Xóa bộ lọc
                </Button>
              )}
            </div>
          ) : (
            <>
              <StaffCandidateTable
                candidates={candidates}
                sortBy="submittedAt"
                sortOrder="asc"
                onSort={() => {}} // Sorting handled by API
                onRowClick={handleRowClick}
                isLoading={false}
                emptyMessage="Không có hồ sơ nào trong hàng chờ duyệt"
              />

              {/* Pagination */}
              {totalElements > 0 && (
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
                    <span className="font-semibold text-foreground">{totalElements}</span> hồ sơ
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={currentPage === 0 || isLoading}
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
                      disabled={currentPage >= totalPages - 1 || isLoading}
                      onClick={() => setCurrentPage((p) => p + 1)}
                      rightIcon={<ChevronRight className="h-4 w-4" />}
                    >
                      Trang sau
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}