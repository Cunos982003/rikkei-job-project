import { useMemo } from "react";
import { ChevronUp, ChevronDown, Clock, Mail, Building2, XCircle, Check } from "lucide-react";
import { Badge } from "@/components/common/Badge";
import type { StaffPendingReview } from "@/api/endpoints/profile.api";
import { cn } from "@/utils/cn";

interface StaffCandidateTableProps {
  candidates: StaffPendingReview[];
  sortBy: keyof StaffPendingReview;
  sortOrder: "asc" | "desc";
  onSort: (field: keyof StaffPendingReview) => void;
  onRowClick: (candidate: StaffPendingReview) => void;
  isLoading?: boolean;
  emptyMessage?: string;
}

function formatDateTime(value?: string): string {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function SortableHeader({ field, label, sortBy, sortOrder, onSort }: {
  field: keyof StaffPendingReview;
  label: string;
  sortBy: keyof StaffPendingReview;
  sortOrder: "asc" | "desc";
  onSort: (field: keyof StaffPendingReview) => void;
}) {
  const isActive = sortBy === field;
  return (
    <th
      className={cn(
        "cursor-pointer select-none whitespace-nowrap px-6 py-3.5",
        "hover:bg-slate-50",
        isActive && "bg-slate-100"
      )}
      onClick={() => onSort(field)}
    >
      <div className="flex items-center gap-1.5">
        <span>{label}</span>
        {isActive ? (
          sortOrder === "asc" ? <ChevronUp className="h-3.5 w-3.5 text-primary" /> : <ChevronDown className="h-3.5 w-3.5 text-primary" />
        ) : (
          <div className="flex flex-col gap-0">
            <ChevronUp className="h-3 w-3 text-muted/30" />
            <ChevronDown className="h-3 w-3 text-muted/30" />
          </div>
        )}
      </div>
    </th>
  );
}

export function StaffCandidateTable({
  candidates,
  sortBy,
  sortOrder,
  onSort,
  onRowClick,
  isLoading = false,
  emptyMessage = "Không có hồ sơ nào trong hàng chờ duyệt",
}: StaffCandidateTableProps) {
  const sortedCandidates = useMemo(() => {
    return [...candidates].sort((a, b) => {
      const aVal = a[sortBy];
      const bVal = b[sortBy];
      if (aVal == null && bVal == null) return 0;
      if (aVal == null) return 1;
      if (bVal == null) return -1;
      const comparison = String(aVal).localeCompare(String(bVal), "vi", { numeric: true });
      return sortOrder === "asc" ? comparison : -comparison;
    });
  }, [candidates, sortBy, sortOrder]);

  if (isLoading) {
    return (
      <div className="rounded-lg border border-border-subtle overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs font-semibold text-muted uppercase tracking-wider border-b border-border">
            <tr>
              <th scope="col" className="px-6 py-3.5 w-8"></th>
              <th scope="col" className="px-6 py-3.5">Họ tên</th>
              <th scope="col" className="px-6 py-3.5">Email</th>
              <th scope="col" className="px-6 py-3.5">Mã lớp</th>
              <th scope="col" className="px-6 py-3.5">Ngày gửi</th>
              <th scope="col" className="px-6 py-3.5">Số lần bị trả lại</th>
            </tr>
          </thead>
          <tbody>
            {[...Array(5)].map((_, i) => (
              <tr key={i} className="animate-pulse">
                <td className="px-6 py-4"></td>
                <td className="px-6 py-4"><div className="h-4 w-48 bg-slate-200 rounded" /></td>
                <td className="px-6 py-4"><div className="h-4 w-40 bg-slate-200 rounded" /></td>
                <td className="px-6 py-4"><div className="h-4 w-24 bg-slate-200 rounded" /></td>
                <td className="px-6 py-4"><div className="h-4 w-36 bg-slate-200 rounded" /></td>
                <td className="px-6 py-4"><div className="h-4 w-16 bg-slate-200 rounded" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  if (sortedCandidates.length === 0) {
    return (
      <div className="rounded-lg border border-border-subtle p-8 text-center">
        <XCircle className="h-12 w-12 text-muted mx-auto mb-3 opacity-50" />
        <p className="text-muted">{emptyMessage}</p>
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-border-subtle overflow-hidden">
      <table className="w-full text-left text-sm">
        <thead className="bg-slate-50 text-xs font-semibold text-muted uppercase tracking-wider border-b border-border">
          <tr>
            <th scope="col" className="px-6 py-3.5 w-8"></th>
            <SortableHeader field="fullName" label="Họ tên" sortBy={sortBy} sortOrder={sortOrder} onSort={onSort} />
            <SortableHeader field="email" label="Email" sortBy={sortBy} sortOrder={sortOrder} onSort={onSort} />
            <SortableHeader field="classCode" label="Mã lớp" sortBy={sortBy} sortOrder={sortOrder} onSort={onSort} />
            <SortableHeader field="submittedAt" label="Ngày gửi" sortBy={sortBy} sortOrder={sortOrder} onSort={onSort} />
            <SortableHeader field="rejectionCount" label="Số lần bị trả lại" sortBy={sortBy} sortOrder={sortOrder} onSort={onSort} />
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {sortedCandidates.map((candidate, index) => (
            <tr
              key={candidate.userId}
              className={cn(
                "cursor-pointer hover:bg-slate-50/60 transition-colors",
                index % 2 === 0 ? "bg-white" : "bg-slate-50/50"
              )}
              onClick={() => onRowClick(candidate)}
            >
              <td className="px-6 py-4 text-center text-muted text-xs">
                <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-slate-100 font-medium">
                  {index + 1}
                </span>
              </td>
              <td className="px-6 py-4 font-medium text-foreground">{candidate.fullName || "—"}</td>
              <td className="px-6 py-4 text-muted">
                <div className="flex items-center gap-1.5">
                  <Mail className="h-3.5 w-3.5 text-muted" />
                  {candidate.email || "—"}
                </div>
              </td>
              <td className="px-6 py-4 text-muted">
                <Badge variant="outline" size="sm" className="gap-1">
                  <Building2 className="h-3 w-3" />
                  {candidate.classCode || "—"}
                </Badge>
              </td>
              <td className="px-6 py-4 text-muted whitespace-nowrap">
                <div className="flex items-center gap-1.5">
                  <Clock className="h-3.5 w-3.5 text-muted" />
                  {formatDateTime(candidate.submittedAt)}
                </div>
              </td>
              <td className="px-6 py-4 text-center">
                {candidate.rejectionCount > 0 ? (
                  <Badge variant="danger" className="gap-1">
                    <XCircle className="h-3 w-3" />
                    {candidate.rejectionCount}
                  </Badge>
                ) : (
                  <Badge variant="verified" className="gap-1">
                    <Check className="h-3 w-3" />
                    0
                  </Badge>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}