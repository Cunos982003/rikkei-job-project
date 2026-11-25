import { useState, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw, Search, Calendar, Download } from "lucide-react";
import { Button } from "@/components/common/Button";
import { Card, CardContent } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Input } from "@/components/common/Input";
import { contactApi, type ContactAccessLog, type StaffContactAccessLogsParams, type ContactAccessVia } from "@/api/endpoints/contact.api";

const VIA_BADGE: Record<ContactAccessVia, { label: string; variant: "default" | "primary" | "verified" }> = {
  APPLICATION: { label: "Ứng tuyển", variant: "verified" },
  APPROVED_REQUEST: { label: "Yêu cầu được duyệt", variant: "primary" },
};

export function StaffContactAccessLogsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();

  const [companyIdFilter, setCompanyIdFilter] = useState(searchParams.get("companyId") || "");
  const [userIdFilter, setUserIdFilter] = useState(searchParams.get("userId") || "");
  const [fromFilter, setFromFilter] = useState(searchParams.get("from") || "");
  const [toFilter, setToFilter] = useState(searchParams.get("to") || "");
  const [searchQuery, setSearchQuery] = useState(searchParams.get("q") || "");
  const [page, setPage] = useState(() => parseInt(searchParams.get("page") || "0", 10));
  const pageSize = 10;

  const params: StaffContactAccessLogsParams = useMemo(() => ({
    companyId: companyIdFilter || undefined,
    userId: userIdFilter || undefined,
    from: fromFilter || undefined,
    to: toFilter || undefined,
  }), [companyIdFilter, userIdFilter, fromFilter, toFilter]);

  const { data, isLoading } = useQuery({
    queryKey: ["staff", "contact-access-logs", params, searchQuery, page],
    queryFn: () => contactApi.getAccessLogs(params),
    staleTime: 30_000,
  });

  const logs: ContactAccessLog[] = data?.data || [];

  // Client-side filtering for search query
  const filteredLogs = useMemo(() => {
    if (!searchQuery) return logs;
    const query = searchQuery.toLowerCase();
    return logs.filter(log =>
      log.hrId.toLowerCase().includes(query) ||
      log.userId.toLowerCase().includes(query) ||
      log.companyId.toLowerCase().includes(query)
    );
  }, [logs, searchQuery]);

  // Pagination
  const totalPages = Math.ceil(filteredLogs.length / pageSize);
  const paginatedLogs = filteredLogs.slice(page * pageSize, (page + 1) * pageSize);

  // Update URL when filters change
  const updateUrl = () => {
    const params = new URLSearchParams();
    if (companyIdFilter) params.set("companyId", companyIdFilter);
    if (userIdFilter) params.set("userId", userIdFilter);
    if (fromFilter) params.set("from", fromFilter);
    if (toFilter) params.set("to", toFilter);
    if (searchQuery) params.set("q", searchQuery);
    if (page > 0) params.set("page", page.toString());
    setSearchParams(params, { replace: true });
  };

  const handleFilterChange = () => {
    setPage(0);
    updateUrl();
  };

  const handleSearchChange = (query: string) => {
    setSearchQuery(query);
    setPage(0);
    updateUrl();
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Nhật ký xem thông tin liên hệ</h1>
          <p className="text-sm text-muted">Theo dõi lịch sử HR xem email/số điện thoại ứng viên</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => queryClient.invalidateQueries({ queryKey: ["staff", "contact-access-logs"] })}>
            <RefreshCw className="h-3.5 w-3.5" /> Làm mới
          </Button>
          <Button variant="outline" size="sm">
            <Download className="h-3.5 w-3.5" /> Xuất Excel
          </Button>
        </div>
      </div>

      <Card>
        <CardContent className="space-y-4">
          <div className="flex flex-col sm:flex-row gap-4 flex-wrap">
            <div className="relative flex-1 min-w-[200px]">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted" />
              <Input
                type="search"
                placeholder="Tìm kiếm HR, ứng viên, công ty..."
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
                className="pl-10"
              />
            </div>
            <div className="flex gap-2 flex-wrap">
              <Input
                type="text"
                placeholder="Company ID"
                value={companyIdFilter}
                onChange={(e) => { setCompanyIdFilter(e.target.value); handleFilterChange(); }}
                className="w-48"
              />
              <Input
                type="text"
                placeholder="User ID (ứng viên)"
                value={userIdFilter}
                onChange={(e) => { setUserIdFilter(e.target.value); handleFilterChange(); }}
                className="w-48"
              />
              <div className="relative">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted" />
                <Input
                  type="date"
                  placeholder="Từ ngày"
                  value={fromFilter}
                  onChange={(e) => { setFromFilter(e.target.value); handleFilterChange(); }}
                  className="pl-10 w-40"
                />
              </div>
              <div className="relative">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted" />
                <Input
                  type="date"
                  placeholder="Đến ngày"
                  value={toFilter}
                  onChange={(e) => { setToFilter(e.target.value); handleFilterChange(); }}
                  className="pl-10 w-40"
                />
              </div>
            </div>
          </div>

          {isLoading ? (
            <div className="flex items-center justify-center py-12">Đang tải...</div>
          ) : filteredLogs.length === 0 ? (
            <p className="text-center text-muted py-8">Không có nhật ký nào</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    <th className="text-left py-2 px-3">HR</th>
                    <th className="text-left py-2 px-3">Ứng viên</th>
                    <th className="text-left py-2 px-3">Công ty</th>
                    <th className="text-left py-2 px-3">Cách thức truy cập</th>
                    <th className="text-left py-2 px-3">Thời gian</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedLogs.map((log) => (
                    <tr key={log.id} className="border-b border-border/50 hover:bg-slate-50">
                      <td className="py-3 px-3">{log.hrId}</td>
                      <td className="py-3 px-3">{log.userId}</td>
                      <td className="py-3 px-3">{log.companyId}</td>
                      <td className="py-3 px-3">
                        <Badge variant={VIA_BADGE[log.via].variant}>
                          {VIA_BADGE[log.via].label}
                        </Badge>
                      </td>
                      <td className="py-3 px-3">{new Date(log.createdAt).toLocaleString("vi-VN")}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-4">
              <Button
                variant="outline"
                size="sm"
                onClick={() => { setPage(page - 1); updateUrl(); }}
                disabled={page === 0}
              >
                Trước
              </Button>
              <span className="text-sm text-muted">
                Trang {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => { setPage(page + 1); updateUrl(); }}
                disabled={page >= totalPages - 1}
              >
                Sau
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}