import { Download, RotateCcw, Search } from "lucide-react";
import type { StaffUpskillingFilterParams } from "../../../../api/endpoints/learning.api";

interface UpskillingFilterBarProps {
  filters: StaffUpskillingFilterParams;
  onChangeFilters: (next: StaffUpskillingFilterParams) => void;
  onReset: () => void;
  onExportExcel: () => void;
  isExporting: boolean;
}

const CLASS_OPTIONS = [
  { value: "", label: "Tất cả các lớp" },
  { value: "RA-JAVA-2026", label: "RA-JAVA-2026 (Java Backend)" },
  { value: "RA-FE-2026", label: "RA-FE-2026 (Frontend React)" },
  { value: "RA-FS-2026", label: "RA-FS-2026 (Fullstack Web)" },
  { value: "RA-AI-2026", label: "RA-AI-2026 (AI & Data)" },
  { value: "RA-MOB-2026", label: "RA-MOB-2026 (Mobile)" },
  { value: "RA-QA-2026", label: "RA-QA-2026 (QA Automation)" },
  { value: "RA-CLOUD-2026", label: "RA-CLOUD-2026 (DevOps & Cloud)" },
  { value: "RA-GO-2026", label: "RA-GO-2026 (Golang Backend)" },
];

const STATUS_OPTIONS = [
  { value: "", label: "Tất cả trạng thái" },
  { value: "IN_PROGRESS", label: "Đang học" },
  { value: "COMPLETED", label: "Đã hoàn thành" },
  { value: "ENROLLED", label: "Đã ghi danh" },
  { value: "FAILED", label: "Chưa đạt" },
];

const SKILL_OPTIONS = [
  { value: "", label: "Tất cả kỹ năng thiếu" },
  { value: "TYPESCRIPT", label: "TypeScript" },
  { value: "JAVA_CORE", label: "Java Core" },
  { value: "REACT", label: "React" },
  { value: "SPRING_BOOT", label: "Spring Boot" },
  { value: "SQL", label: "SQL" },
  { value: "DOCKER", label: "Docker" },
  { value: "TESTING", label: "Testing" },
  { value: "PYTHON", label: "Python" },
  { value: "GOLANG", label: "Golang" },
];

export function UpskillingFilterBar({
  filters,
  onChangeFilters,
  onReset,
  onExportExcel,
  isExporting,
}: UpskillingFilterBarProps) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-3 items-end">
        {/* Lọc theo lớp */}
        <div className="lg:col-span-3">
          <label className="block text-xs font-semibold text-slate-600 mb-1">
            Mã lớp
          </label>
          <select
            value={filters.classCode || ""}
            onChange={(e) =>
              onChangeFilters({ ...filters, classCode: e.target.value, page: 0 })
            }
            className="w-full h-9 rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          >
            {CLASS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Lọc theo tên khoá */}
        <div className="lg:col-span-3">
          <label className="block text-xs font-semibold text-slate-600 mb-1">
            Khoá đào tạo
          </label>
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-2.5 top-2.5" />
            <input
              type="text"
              value={filters.courseName || ""}
              onChange={(e) =>
                onChangeFilters({ ...filters, courseName: e.target.value, page: 0 })
              }
              placeholder="Tìm tên khoá học..."
              className="w-full h-9 rounded-lg border border-slate-300 bg-white pl-8 pr-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
            />
          </div>
        </div>

        {/* Lọc theo trạng thái */}
        <div className="lg:col-span-2">
          <label className="block text-xs font-semibold text-slate-600 mb-1">
            Trạng thái
          </label>
          <select
            value={filters.status || ""}
            onChange={(e) =>
              onChangeFilters({ ...filters, status: e.target.value, page: 0 })
            }
            className="w-full h-9 rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Lọc theo kỹ năng thiếu */}
        <div className="lg:col-span-2">
          <label className="block text-xs font-semibold text-slate-600 mb-1">
            Kỹ năng còn thiếu
          </label>
          <select
            value={filters.skillId || ""}
            onChange={(e) =>
              onChangeFilters({ ...filters, skillId: e.target.value, page: 0 })
            }
            className="w-full h-9 rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-800 focus:border-blue-500 focus:outline-none"
          >
            {SKILL_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Nút thao tác */}
        <div className="lg:col-span-2 flex items-center gap-2 justify-end">
          <button
            type="button"
            onClick={onReset}
            className="h-9 px-3 rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 text-sm font-medium inline-flex items-center gap-1.5 transition-colors"
            title="Đặt lại bộ lọc"
          >
            <RotateCcw className="w-4 h-4" />
            <span className="hidden xl:inline">Đặt lại</span>
          </button>

          <button
            type="button"
            onClick={onExportExcel}
            disabled={isExporting}
            className="h-9 px-3.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 disabled:opacity-60 text-white text-sm font-semibold inline-flex items-center gap-1.5 shadow-sm transition-colors whitespace-nowrap"
          >
            <Download className="w-4 h-4" />
            <span>{isExporting ? "Đang xuất..." : "Xuất Excel"}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
