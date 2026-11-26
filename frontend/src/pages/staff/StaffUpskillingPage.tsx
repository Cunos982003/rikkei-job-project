import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { GraduationCap, RefreshCw } from "lucide-react";
import { toast } from "sonner";
import {
  learningApi,
  type StaffUpskillingFilterParams,
  type UpskillingCandidateItem,
} from "../../api/endpoints/learning.api";
import { UpskillingSummaryCards } from "../../features/learning/components/staff/UpskillingSummaryCards";
import { TopMissingSkillsChart } from "../../features/learning/components/staff/TopMissingSkillsChart";
import { UpskillingFilterBar } from "../../features/learning/components/staff/UpskillingFilterBar";
import { UpskillingTable } from "../../features/learning/components/staff/UpskillingTable";
import { CandidateLearningPathDrawer } from "../../features/learning/components/staff/CandidateLearningPathDrawer";

const DEFAULT_FILTERS: StaffUpskillingFilterParams = {
  classCode: "",
  courseName: "",
  status: "",
  skillId: "",
  page: 0,
  size: 10,
};

export function StaffUpskillingPage() {
  const [filters, setFilters] = useState<StaffUpskillingFilterParams>(DEFAULT_FILTERS);
  const [selectedCandidate, setSelectedCandidate] =
    useState<UpskillingCandidateItem | null>(null);
  const [isExporting, setIsExporting] = useState(false);

  const cleanParams: StaffUpskillingFilterParams = {
    ...(filters.classCode ? { classCode: filters.classCode } : {}),
    ...(filters.courseName?.trim() ? { courseName: filters.courseName.trim() } : {}),
    ...(filters.status ? { status: filters.status } : {}),
    ...(filters.skillId ? { skillId: filters.skillId } : {}),
    page: filters.page ?? 0,
    size: filters.size ?? 10,
  };

  const summaryQuery = useQuery({
    queryKey: ["staff", "upskilling", "summary"],
    queryFn: async () => {
      const res = await learningApi.getStaffUpskillingSummary();
      return res.data;
    },
  });

  const listQuery = useQuery({
    queryKey: ["staff", "upskilling", "list", cleanParams],
    queryFn: async () => {
      const res = await learningApi.getStaffUpskillingList(cleanParams);
      return Array.isArray(res.data) ? res.data : [];
    },
  });

  const handleExportExcel = async () => {
    try {
      setIsExporting(true);
      const exportParams: StaffUpskillingFilterParams = {
        ...(filters.classCode ? { classCode: filters.classCode } : {}),
        ...(filters.courseName?.trim() ? { courseName: filters.courseName.trim() } : {}),
        ...(filters.status ? { status: filters.status } : {}),
        ...(filters.skillId ? { skillId: filters.skillId } : {}),
      };
      const res = await learningApi.exportStaffUpskillingExcel(exportParams);
      const blob = res.data;
      if (!(blob instanceof Blob)) {
        toast.error("Định dạng tệp Excel trả về không hợp lệ.");
        return;
      }

      if (blob.type === "application/json" || blob.type.includes("json")) {
        const text = await blob.text();
        try {
          const errJson = JSON.parse(text);
          toast.error(errJson.message || "Không thể xuất tệp Excel.");
        } catch {
          toast.error("Không thể xuất tệp Excel.");
        }
        return;
      }

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      const dateStr = new Date().toISOString().slice(0, 10);
      a.download = `bao-cao-dao-tao-bo-sung-${dateStr}.xlsx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      toast.success("Xuất tệp Excel báo cáo đào tạo bổ sung thành công!");
    } catch {
      toast.error("Không thể xuất tệp Excel. Vui lòng thử lại sau.");
    } finally {
      setIsExporting(false);
    }
  };

  const handleRefresh = () => {
    summaryQuery.refetch();
    listQuery.refetch();
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-blue-600 text-white shadow-sm">
            <GraduationCap className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900">
              Đào tạo bổ sung
            </h1>
            <p className="text-sm text-slate-500">
              Theo dõi tiến độ học bổ sung của học viên theo lớp, khoá và các kỹ năng còn thiếu
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleRefresh}
          disabled={summaryQuery.isFetching || listQuery.isFetching}
          className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 text-sm font-medium text-slate-700 shadow-sm transition-colors disabled:opacity-60"
        >
          <RefreshCw
            className={`w-4 h-4 ${
              summaryQuery.isFetching || listQuery.isFetching ? "animate-spin" : ""
            }`}
          />
          Làm mới
        </button>
      </div>

      {/* Thẻ số tổng */}
      <UpskillingSummaryCards
        summary={summaryQuery.data}
        isLoading={summaryQuery.isLoading}
      />

      {/* Biểu đồ cột Top 5 kỹ năng thiếu */}
      <TopMissingSkillsChart
        skills={summaryQuery.data?.topMissingSkills || []}
        selectedSkillId={filters.skillId}
        onSelectSkill={(skillId) =>
          setFilters((prev) => ({ ...prev, skillId, page: 0 }))
        }
        isLoading={summaryQuery.isLoading}
      />

      {/* Bộ lọc & Nút Xuất Excel */}
      <UpskillingFilterBar
        filters={filters}
        onChangeFilters={setFilters}
        onReset={() => setFilters(DEFAULT_FILTERS)}
        onExportExcel={handleExportExcel}
        isExporting={isExporting}
      />

      {/* Bảng danh sách */}
      <UpskillingTable
        items={listQuery.data || []}
        isLoading={listQuery.isLoading}
        page={filters.page ?? 0}
        pageSize={filters.size ?? 10}
        onPageChange={(nextPage) =>
          setFilters((prev) => ({ ...prev, page: nextPage }))
        }
        onSelectCandidate={setSelectedCandidate}
      />

      {/* Drawer xem chi tiết lộ trình của ứng viên */}
      <CandidateLearningPathDrawer
        candidate={selectedCandidate}
        onClose={() => setSelectedCandidate(null)}
      />
    </div>
  );
}

export default StaffUpskillingPage;
