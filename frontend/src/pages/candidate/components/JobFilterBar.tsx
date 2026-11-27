import React, { useMemo } from "react";
import {
  Search,
  MapPin,
  DollarSign,
  Briefcase,
  Layers,
  RotateCcw,
  SlidersHorizontal,
  X,
  ChevronDown,
} from "lucide-react";
import { cn } from "../../../utils/cn";

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

  const handleFieldChange = (field: keyof JobFilters, value: string) => {
    onChange({
      ...filters,
      [field]: value,
    });
  };

  const handleRemoveFilter = (field: keyof JobFilters) => {
    onChange({
      ...filters,
      [field]: "",
    });
  };

  // Trích xuất danh sách các tiêu chí lọc đang được kích hoạt để hiển thị tag cụ thể
  const activeTags = useMemo(() => {
    const tags: Array<{
      key: keyof JobFilters;
      label: string;
      displayValue: string;
    }> = [];

    if (filters.keyword.trim()) {
      tags.push({
        key: "keyword",
        label: "Từ khóa",
        displayValue: `"${filters.keyword.trim()}"`,
      });
    }

    if (filters.location) {
      const opt = LOCATION_OPTIONS.find((o) => o.value === filters.location);
      tags.push({
        key: "location",
        label: "Địa điểm",
        displayValue: opt?.label || filters.location,
      });
    }

    if (filters.salaryRange) {
      const opt = SALARY_OPTIONS.find((o) => o.value === filters.salaryRange);
      tags.push({
        key: "salaryRange",
        label: "Mức lương",
        displayValue: opt?.label || filters.salaryRange,
      });
    }

    if (filters.jobType) {
      const opt = JOB_TYPE_OPTIONS.find((o) => o.value === filters.jobType);
      tags.push({
        key: "jobType",
        label: "Hình thức",
        displayValue: opt?.label || filters.jobType,
      });
    }

    if (filters.level) {
      const opt = LEVEL_OPTIONS.find((o) => o.value === filters.level);
      tags.push({
        key: "level",
        label: "Cấp bậc",
        displayValue: opt?.label || filters.level,
      });
    }

    return tags;
  }, [filters]);

  const getSelectClass = (isActive: boolean) =>
    cn(
      "w-full pl-9 pr-8 py-2 text-xs font-medium rounded-xl border transition-all cursor-pointer appearance-none focus:outline-none focus:ring-2 focus:ring-primary/20",
      isActive
        ? "border-primary text-primary bg-primary-50/40 font-semibold"
        : "border-border bg-surface hover:border-slate-300 text-foreground"
    );

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
            className="absolute right-3.5 p-1 text-muted hover:text-foreground rounded-md text-xs cursor-pointer flex items-center justify-center"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* 2. Secondary Filter Selects */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Địa điểm */}
        <div className="relative">
          <MapPin
            className={cn(
              "absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 pointer-events-none transition-colors",
              filters.location ? "text-primary" : "text-muted"
            )}
          />
          <select
            value={filters.location}
            onChange={(e) => handleFieldChange("location", e.target.value)}
            className={getSelectClass(Boolean(filters.location))}
          >
            {LOCATION_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <ChevronDown
            className={cn(
              "absolute right-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 pointer-events-none transition-colors",
              filters.location ? "text-primary" : "text-muted"
            )}
          />
        </div>

        {/* Mức lương */}
        <div className="relative">
          <DollarSign
            className={cn(
              "absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 pointer-events-none transition-colors",
              filters.salaryRange ? "text-primary" : "text-emerald-600"
            )}
          />
          <select
            value={filters.salaryRange}
            onChange={(e) => handleFieldChange("salaryRange", e.target.value)}
            className={getSelectClass(Boolean(filters.salaryRange))}
          >
            {SALARY_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <ChevronDown
            className={cn(
              "absolute right-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 pointer-events-none transition-colors",
              filters.salaryRange ? "text-primary" : "text-muted"
            )}
          />
        </div>

        {/* Hình thức làm việc */}
        <div className="relative">
          <Briefcase
            className={cn(
              "absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 pointer-events-none transition-colors",
              filters.jobType ? "text-primary" : "text-muted"
            )}
          />
          <select
            value={filters.jobType}
            onChange={(e) => handleFieldChange("jobType", e.target.value)}
            className={getSelectClass(Boolean(filters.jobType))}
          >
            {JOB_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <ChevronDown
            className={cn(
              "absolute right-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 pointer-events-none transition-colors",
              filters.jobType ? "text-primary" : "text-muted"
            )}
          />
        </div>

        {/* Cấp bậc */}
        <div className="relative">
          <Layers
            className={cn(
              "absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 pointer-events-none transition-colors",
              filters.level ? "text-primary" : "text-muted"
            )}
          />
          <select
            value={filters.level}
            onChange={(e) => handleFieldChange("level", e.target.value)}
            className={getSelectClass(Boolean(filters.level))}
          >
            {LEVEL_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <ChevronDown
            className={cn(
              "absolute right-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 pointer-events-none transition-colors",
              filters.level ? "text-primary" : "text-muted"
            )}
          />
        </div>
      </div>

      {/* 3. Bottom Status & Active Filters Tag Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-3 border-t border-border-subtle text-xs">
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1.5 text-muted shrink-0">
            <SlidersHorizontal className="h-3.5 w-3.5 text-primary" />
            <span>
              Tìm thấy <strong className="text-foreground font-bold">{totalResults}</strong> việc làm phù hợp
            </span>
          </div>

          {/* Danh sách các tag tiêu chí cụ thể đã chọn */}
          {activeTags.length > 0 && (
            <div className="flex flex-wrap items-center gap-1.5 ml-0 sm:ml-2">
              {activeTags.map((tag) => (
                <span
                  key={tag.key}
                  className="inline-flex items-center gap-1.5 pl-2.5 pr-1.5 py-1 rounded-full text-xs font-medium bg-primary-light text-primary border border-primary/20 shadow-2xs group transition-all"
                >
                  <span className="text-primary/70 font-normal">{tag.label}:</span>
                  <span className="font-semibold text-primary">{tag.displayValue}</span>
                  <button
                    type="button"
                    onClick={() => handleRemoveFilter(tag.key)}
                    aria-label={`Xóa tiêu chí ${tag.label}`}
                    title={`Xóa tiêu chí ${tag.label}`}
                    className="p-0.5 rounded-full hover:bg-primary/20 text-primary/70 hover:text-primary transition-colors cursor-pointer flex items-center justify-center"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </span>
              ))}
            </div>
          )}
        </div>

        {hasActiveFilters && (
          <button
            type="button"
            onClick={onReset}
            className="inline-flex items-center gap-1.5 text-xs font-medium text-danger hover:text-red-700 transition-colors cursor-pointer shrink-0 ml-auto sm:ml-0"
            title="Xóa tất cả các tiêu chí lọc đang áp dụng"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Xóa bộ lọc</span>
          </button>
        )}
      </div>
    </div>
  );
};

