import { useMemo } from "react";
import { Search, X } from "lucide-react";
import { Button } from "@/components/common/Button";
import { Input } from "@/components/common/Input";

interface StaffCandidateFiltersProps {
  q: string;
  onQChange: (q: string) => void;
  classCode: string;
  onClassCodeChange: (classCode: string) => void;
  classCodes: string[];
  status: string;
  onStatusChange: (status: string) => void;
  isLoading?: boolean;
}

const STATUS_OPTIONS = [
  { value: "PENDING", label: "Chờ duyệt" },
  { value: "REJECTED", label: "Đã trả lại" },
  { value: "PUBLIC", label: "Đã duyệt" },
  { value: "PRIVATE", label: "Riêng tư" },
  { value: "RECRUITER", label: "Chỉ doanh nghiệp" },
];

export function StaffCandidateFilters({
  q,
  onQChange,
  classCode,
  onClassCodeChange,
  classCodes,
  status,
  onStatusChange,
  isLoading = false,
}: StaffCandidateFiltersProps) {
  const uniqueClassCodes = useMemo(() => {
    const codes = classCodes.filter(Boolean);
    return [...new Set(codes)].sort();
  }, [classCodes]);

  const handleClear = () => {
    onQChange("");
    onClassCodeChange("");
    onStatusChange("PENDING");
  };

  const hasActiveFilters = q || classCode || status !== "PENDING";

  return (
    <div className="flex flex-col sm:flex-row gap-4 p-4 bg-slate-50 rounded-xl border border-border-subtle notranslate" translate="no">
      {/* Search */}
      <div className="flex-1 min-w-[200px] relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted" />
        <Input
          placeholder="Tìm theo tên hoặc email..."
          value={q}
          onChange={(e) => onQChange(e.target.value)}
          className="pl-10"
          disabled={isLoading}
        />
      </div>

      {/* Class Code Filter */}
      <div className="w-full sm:w-[180px]">
        <select
          value={classCode}
          onChange={(e) => onClassCodeChange(e.target.value)}
          disabled={isLoading}
          className="w-full h-10 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
        >
          <option value="">Tất cả lớp</option>
          {uniqueClassCodes.map((code) => (
            <option key={code} value={code}>
              {code}
            </option>
          ))}
        </select>
      </div>

      {/* Status Filter */}
      <div className="w-full sm:w-[160px]">
        <select
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          disabled={isLoading}
          className="w-full h-10 rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground outline-none transition-colors focus:border-primary focus:ring-2 focus:ring-primary/20"
        >
          {STATUS_OPTIONS.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      {/* Clear Filters */}
      {hasActiveFilters && (
        <Button
          variant="ghost"
          size="sm"
          onClick={handleClear}
          disabled={isLoading}
          className="flex items-center gap-1.5 text-muted hover:text-danger h-10"
        >
          <X className="h-3.5 w-3.5" />
          Xóa bộ lọc
        </Button>
      )}
    </div>
  );
}