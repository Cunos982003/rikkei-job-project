import React from "react";
import {
  Search,
  MapPin,
  DollarSign,
  Briefcase,
  Layers,
  RotateCcw,
  SlidersHorizontal,
} from "lucide-react";

export interface JobFilters {
  keyword: string;
  location: string;
  salaryRange: string;
  jobType: string;
  level: string;
}

interface JobFilterBarProps {
  filters: JobFilters;
  onChange: (filters: JobFilters) => void;
  onReset: () => void;
  totalResults: number;
}

const LOCATION_OPTIONS = [
  { value: "", label: "Tất cả địa điểm" },
  { value: "Hà Nội", label: "Hà Nội" },
  { value: "Hồ Chí Minh", label: "TP. Hồ Chí Minh" },
  { value: "Đà Nẵng", label: "Đà Nẵng" },
  { value: "Remote", label: "Remote / Làm việc từ xa" },
];

const SALARY_OPTIONS = [
  { value: "", label: "Tất cả mức lương" },
  { value: "under-10", label: "Dưới 10 triệu" },
  { value: "10-15", label: "10 - 15 triệu" },
  { value: "15-20", label: "15 - 20 triệu" },
  { value: "20-30", label: "20 - 30 triệu" },
  { value: "over-30", label: "Trên 30 triệu" },
  { value: "negotiable", label: "Thỏa thuận" },
];

const JOB_TYPE_OPTIONS = [
  { value: "", label: "Tất cả hình thức" },
  { value: "FULL_TIME", label: "Toàn thời gian (Full-time)" },
  { value: "INTERNSHIP", label: "Thực tập (Internship)" },
  { value: "PART_TIME", label: "Bán thời gian (Part-time)" },
];

const LEVEL_OPTIONS = [
  { value: "", label: "Tất cả cấp bậc" },
  { value: "INTERN", label: "Thực tập sinh (Intern)" },
  { value: "FRESHER", label: "Mới tốt nghiệp (Fresher)" },
  { value: "JUNIOR", label: "Junior (1-2 năm)" },
  { value: "MIDDLE_SENIOR", label: "Middle / Senior" },
];

export const JobFilterBar: React.FC<JobFilterBarProps> = ({
  filters,
  onChange,
  onReset,
  totalResults,
}) => {
  const hasActiveFilters = Boolean(
    filters.keyword ||
      filters.location ||
      filters.salaryRange ||
      filters.jobType ||
      filters.level
  );

  const activeFilterCount = [
    Boolean(filters.location),
    Boolean(filters.salaryRange),
    Boolean(filters.jobType),
    Boolean(filters.level),
  ].filter(Boolean).length;

  const handleFieldChange = (field: keyof JobFilters, value: string) => {
    onChange({
      ...filters,
      [field]: value,
    });
  };

  return (
    <div className="bg-surface border border-border rounded-2xl p-4 sm:p-5 shadow-xs space-y-4">
      {/* 1. Main Search Input (Hero Input) */}
      <div className="relative flex items-center">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-muted pointer-events-none" />
        <input
          type="text"
          value={filters.keyword}
          onChange={(e) => handleFieldChange("keyword", e.target.value)}
          placeholder="Tìm việc theo chức danh, kỹ năng (Java, ReactJS, SQL...) hoặc tên công ty..."
          className="w-full pl-12 pr-10 py-3 text-sm rounded-xl border border-border bg-slate-50/50 hover:bg-surface focus:bg-surface focus:outline-none focus:ring-2 focus:ring-primary/25 focus:border-primary transition-all text-foreground placeholder:text-muted"
        />
        {filters.keyword && (
          <button
            type="button"
            onClick={() => handleFieldChange("keyword", "")}
            aria-label="Xóa từ khóa"
            className="absolute right-3.5 p-1 text-muted hover:text-foreground rounded-md text-xs cursor-pointer"
          >
            ✕
          </button>
        )}
      </div>

      {/* 2. Secondary Filter Selects */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Địa điểm */}
        <div className="relative">
          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted pointer-events-none" />
          <select
            value={filters.location}
            onChange={(e) => handleFieldChange("location", e.target.value)}
            className="w-full pl-9 pr-8 py-2 text-xs font-medium rounded-xl border border-border bg-surface hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-foreground transition-all cursor-pointer appearance-none"
          >
            {LOCATION_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Mức lương */}
        <div className="relative">
          <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-emerald-600 pointer-events-none" />
          <select
            value={filters.salaryRange}
            onChange={(e) => handleFieldChange("salaryRange", e.target.value)}
            className="w-full pl-9 pr-8 py-2 text-xs font-medium rounded-xl border border-border bg-surface hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-foreground transition-all cursor-pointer appearance-none"
          >
            {SALARY_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Hình thức làm việc */}
        <div className="relative">
          <Briefcase className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted pointer-events-none" />
          <select
            value={filters.jobType}
            onChange={(e) => handleFieldChange("jobType", e.target.value)}
            className="w-full pl-9 pr-8 py-2 text-xs font-medium rounded-xl border border-border bg-surface hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-foreground transition-all cursor-pointer appearance-none"
          >
            {JOB_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Cấp bậc */}
        <div className="relative">
          <Layers className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted pointer-events-none" />
          <select
            value={filters.level}
            onChange={(e) => handleFieldChange("level", e.target.value)}
            className="w-full pl-9 pr-8 py-2 text-xs font-medium rounded-xl border border-border bg-surface hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary text-foreground transition-all cursor-pointer appearance-none"
          >
            {LEVEL_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 3. Bottom Status & Reset Bar */}
      <div className="flex items-center justify-between pt-1 border-t border-border-subtle text-xs">
        <div className="flex items-center gap-2 text-muted">
          <SlidersHorizontal className="h-3.5 w-3.5" />
          <span>
            Tìm thấy <strong className="text-foreground font-bold">{totalResults}</strong> việc làm phù hợp
          </span>
          {activeFilterCount > 0 && (
            <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-primary-light text-primary">
              {activeFilterCount} bộ lọc đang áp dụng
            </span>
          )}
        </div>

        {hasActiveFilters && (
          <button
            type="button"
            onClick={onReset}
            className="inline-flex items-center gap-1.5 text-xs font-medium text-danger hover:text-red-700 transition-colors cursor-pointer"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Xóa bộ lọc</span>
          </button>
        )}
      </div>
    </div>
  );
};

