import { useState, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { RefreshCw, CheckCircle2, XCircle, Search } from "lucide-react";
import { Button } from "@/components/common/Button";
import { Card, CardContent } from "@/components/common/Card";
import { Badge } from "@/components/common/Badge";
import { Modal } from "@/components/common/Modal";
import { Input } from "@/components/common/Input";
import { contactApi, type ContactRequest, type ContactRequestStatus } from "@/api/endpoints/contact.api";

const STATUS_BADGE: Record<ContactRequestStatus, { label: string; variant: "default" | "primary" | "warning" | "danger" | "verified" }> = {
  PENDING: { label: "Chờ duyệt", variant: "warning" },
  APPROVED: { label: "Đã duyệt", variant: "verified" },
  REJECTED: { label: "Đã từ chối", variant: "danger" },
};

export function StaffContactRequestsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();

  const [statusFilter, setStatusFilter] = useState<ContactRequestStatus | "">(
    (searchParams.get("status") as ContactRequestStatus) || ""
  );
  const [searchQuery, setSearchQuery] = useState(
    searchParams.get("q") || ""
  );
  const [page, setPage] = useState(() => parseInt(searchParams.get("page") || "0", 10));
  const pageSize = 10;

  const { data, isLoading } = useQuery({
    queryKey: ["staff", "contact-requests", statusFilter, searchQuery, page],
    queryFn: () => contactApi.getContactRequests(statusFilter || undefined),
    staleTime: 30_000,
  });

  const requests: ContactRequest[] = data?.data || [];

  // Client-side filtering for search query
  const filteredRequests = useMemo(() => {
    if (!searchQuery) return requests;
    const query = searchQuery.toLowerCase();
    return requests.filter(req =>
      req.reason.toLowerCase().includes(query) ||
      req.userId.toLowerCase().includes(query) ||
      req.companyId.toLowerCase().includes(query)
    );
  }, [requests, searchQuery]);

  const approveMutation = useMutation({
    mutationFn: (id: string) => contactApi.approveContactRequest(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["staff", "contact-requests"] }),
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => contactApi.rejectContactRequest(id, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["staff", "contact-requests"] }),
  });

  const [rejectModal, setRejectModal] = useState<{ id: string; reason: string } | null>(null);

  const handleApprove = (id: string) => {
    if (confirm("Duyệt yêu cầu này?")) approveMutation.mutate(id);
  };

  const handleReject = (id: string) => setRejectModal({ id, reason: "" });

  const confirmReject = () => {
    if (rejectModal) rejectMutation.mutate({ id: rejectModal.id, reason: rejectModal.reason });
    setRejectModal(null);
  };

  // Update URL when filters change
  const updateUrl = () => {
    const params = new URLSearchParams();
    if (statusFilter) params.set("status", statusFilter);
    if (searchQuery) params.set("q", searchQuery);
    if (page > 0) params.set("page", page.toString());
    setSearchParams(params, { replace: true });
  };

  const handleStatusChange = (status: ContactRequestStatus | "") => {
    setStatusFilter(status);
    setPage(0);
    updateUrl();
  };

  const handleSearchChange = (query: string) => {
    setSearchQuery(query);
    setPage(0);
    updateUrl();
  };

  // Pagination
  const totalPages = Math.ceil(filteredRequests.length / pageSize);
  const paginatedRequests = filteredRequests.slice(page * pageSize, (page + 1) * pageSize);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">Quản lý yêu cầu mở liên hệ</h1>
          <p className="text-sm text-muted">Duyệt/từ chối yêu cầu của HR xem liên hệ ứng viên</p>
        </div>
        <Button variant="outline" size="sm" onClick={() => queryClient.invalidateQueries({ queryKey: ["staff", "contact-requests"] })}>
          <RefreshCw className="h-3.5 w-3.5" /> Làm mới
        </Button>
      </div>

      <Card>
        <CardContent className="space-y-4">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted" />
              <Input
                type="search"
                placeholder="Tìm kiếm lý do, ứng viên, công ty..."
                value={searchQuery}
                onChange={(e) => handleSearchChange(e.target.value)}
                className="pl-10"
              />
            </div>
            <select
              value={statusFilter}
              onChange={(e) => handleStatusChange(e.target.value as ContactRequestStatus | "")}
              className="border border-border rounded-lg px-3 py-2 text-sm w-full sm:w-48"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="PENDING">Chờ duyệt</option>
              <option value="APPROVED">Đã duyệt</option>
              <option value="REJECTED">Đã từ chối</option>
            </select>
          </div>

          {isLoading ? (
            <div className="flex items-center justify-center py-12">Đang tải...</div>
          ) : paginatedRequests.length === 0 ? (
            <p className="text-center text-muted py-8">Không có yêu cầu nào</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    <th className="text-left py-2 px-3">HR / Công ty</th>
                    <th className="text-left py-2 px-3">Ứng viên</th>
                    <th className="text-left py-2 px-3">Lý do</th>
                    <th className="text-left py-2 px-3">Trạng thái</th>
                    <th className="text-left py-2 px-3">Ngày tạo</th>
                    <th className="text-right py-2 px-3">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedRequests.map((req) => (
                    <tr key={req.id} className="border-b border-border/50 hover:bg-slate-50">
                      <td className="py-3 px-3">{req.companyId}</td>
                      <td className="py-3 px-3">{req.userId}</td>
                      <td className="py-3 px-3 max-w-xs truncate">{req.reason}</td>
                      <td className="py-3 px-3">
                        <Badge variant={STATUS_BADGE[req.status].variant}>
                          {STATUS_BADGE[req.status].label}
                        </Badge>
                      </td>
                      <td className="py-3 px-3">{new Date(req.createdAt).toLocaleDateString("vi-VN")}</td>
                      <td className="py-3 px-3 text-right">
                        {req.status === "PENDING" && (
                          <div className="flex items-center justify-end gap-2">
                            <Button variant="primary" size="sm" onClick={() => handleApprove(req.id)} isLoading={approveMutation.isPending}>
                              <CheckCircle2 className="h-3.5 w-3.5" /> Duyệt
                            </Button>
                            <Button variant="outline" size="sm" onClick={() => handleReject(req.id)}>
                              <XCircle className="h-3.5 w-3.5" /> Từ chối
                            </Button>
                          </div>
                        )}
                      </td>
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

      {/* Reject Modal */}
      <Modal isOpen={!!rejectModal} onClose={() => setRejectModal(null)} title="Từ chối yêu cầu" size="md">
        <div className="space-y-4">
          <p className="text-sm">Nhập lý do từ chối:</p>
          <textarea
            value={rejectModal?.reason || ""}
            onChange={(e) => setRejectModal({ ...rejectModal!, reason: e.target.value })}
            placeholder="Lý do từ chối..."
            rows={3}
            className="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-muted transition-colors outline-none focus:border-primary focus:ring-2 focus:ring-primary/20 resize-none"
          />
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setRejectModal(null)}>Hủy</Button>
            <Button variant="danger" onClick={confirmReject} isLoading={rejectMutation.isPending}>Từ chối</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}