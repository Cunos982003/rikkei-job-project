import { ChevronLeft, ChevronRight, Eye } from "lucide-react";
import type { UpskillingCandidateItem } from "../../../../api/endpoints/learning.api";

interface UpskillingTableProps {
  items: UpskillingCandidateItem[];
  isLoading: boolean;
  page: number;
  pageSize: number;
  onPageChange: (nextPage: number) => void;
  onSelectCandidate: (item: UpskillingCandidateItem) => void;
}

const STATUS_BADGE: Record<string, { label: string; className: string }> = {
  COMPLETED: {
    label: "Đã hoàn thành",
    className: "bg-emerald-50 text-emerald-700 border-emerald-200",
  },
  IN_PROGRESS: {
    label: "Đang học",
    className: "bg-blue-50 text-blue-700 border-blue-200",
  },
  ENROLLED: {
    label: "Đã ghi danh",
    className: "bg-amber-50 text-amber-700 border-amber-200",
  },
  FAILED: {
    label: "Chưa đạt",
    className: "bg-rose-50 text-rose-700 border-rose-200",
  },
  NOT_STARTED: {
    label: "Chưa bắt đầu",
    className: "bg-slate-100 text-slate-600 border-slate-200",
  },
};

export function UpskillingTable({
  items,
  isLoading,
  page,
  pageSize,
  onPageChange,
  onSelectCandidate,
}: UpskillingTableProps) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200 text-xs font-semibold uppercase tracking-wider text-slate-600">
              <th className="py-3.5 px-4">Ứng viên</th>
              <th className="py-3.5 px-4">Lớp / Khoá</th>
              <th className="py-3.5 px-4">Mục tiêu</th>
              <th className="py-3.5 px-4">Kỹ năng thiếu</th>
              <th className="py-3.5 px-4">Tiến độ môn học</th>
              <th className="py-3.5 px-4 text-center">Sẵn sàng</th>
              <th className="py-3.5 px-4 text-center">Trạng thái</th>
              <th className="py-3.5 px-4 text-right">Lộ trình</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-sm">
            {isLoading ? (
              <tr>
                <td colSpan={8} className="py-12 text-center text-slate-400">
                  Đang tải dữ liệu đào tạo bổ sung...
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-12 text-center text-slate-400">
                  Không tìm thấy học viên nào phù hợp với bộ lọc hiện tại.
                </td>
              </tr>
            ) : (
              items.map((item) => {
                const statusInfo =
                  STATUS_BADGE[item.status] || STATUS_BADGE.IN_PROGRESS;
                const avgProgress =
                  item.enrollments.length > 0
                    ? Math.round(
                        item.enrollments.reduce((acc, e) => acc + (e.progress || 0), 0) /
                          item.enrollments.length
                      )
                    : 0;

                return (
                  <tr
                    key={item.candidate.userId}
                    onClick={() => onSelectCandidate(item)}
                    className="hover:bg-blue-50/40 cursor-pointer transition-colors"
                  >
                    <td className="py-3.5 px-4">
                      <p className="font-semibold text-slate-900">
                        {item.candidate.fullName}
                      </p>
                      <p className="text-xs text-slate-500">
                        {item.candidate.email}
                      </p>
                    </td>

                    <td className="py-3.5 px-4">
                      <span className="inline-block px-2 py-0.5 rounded bg-slate-100 text-slate-800 text-xs font-semibold">
                        {item.classCode}
                      </span>
                      <p className="text-xs text-slate-500 mt-1 line-clamp-1 max-w-[200px]">
                        {item.courseName}
                      </p>
                    </td>

                    <td className="py-3.5 px-4 text-slate-700 font-medium">
                      {item.target}
                    </td>

                    <td className="py-3.5 px-4">
                      {item.missingSkills.length === 0 ? (
                        <span className="text-xs text-emerald-600 font-medium">
                          Đã đạt chuẩn
                        </span>
                      ) : (
                        <div className="flex flex-wrap gap-1 max-w-[220px]">
                          {item.missingSkills.slice(0, 2).map((sk) => (
                            <span
                              key={sk.skillId}
                              className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200"
                            >
                              {sk.skillName} (-{sk.gap})
                            </span>
                          ))}
                          {item.missingSkills.length > 2 && (
                            <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600">
                              +{item.missingSkills.length - 2}
                            </span>
                          )}
                        </div>
                      )}
                    </td>

                    <td className="py-3.5 px-4">
                      <div className="w-32">
                        <div className="flex items-center justify-between text-xs text-slate-600 mb-1">
                          <span>{item.enrollments.length} môn học</span>
                          <span className="font-semibold">{avgProgress}%</span>
                        </div>
                        <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-blue-600 rounded-full transition-all"
                            style={{ width: `${avgProgress}%` }}
                          />
                        </div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      <span className="font-bold text-slate-900">
                        {item.readiness}%
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-center">
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${statusInfo.className}`}
                      >
                        {statusInfo.label}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-right">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectCandidate(item);
                        }}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded-md text-xs font-medium text-blue-600 hover:bg-blue-50 transition-colors"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Phân trang */}
      <div className="flex items-center justify-between px-4 py-3 border-t border-slate-200 bg-slate-50/50 text-xs text-slate-600">
        <span>
          Hiển thị <strong>{items.length}</strong> học viên (Trang {page + 1})
        </span>
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            disabled={page === 0 || isLoading}
            onClick={() => onPageChange(Math.max(0, page - 1))}
            className="p-1.5 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 disabled:opacity-40"
            title="Trang trước"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="px-2.5 font-semibold text-slate-800">
            Trang {page + 1}
          </span>
          <button
            type="button"
            disabled={items.length < pageSize || isLoading}
            onClick={() => onPageChange(page + 1)}
            className="p-1.5 rounded-lg border border-slate-300 bg-white hover:bg-slate-50 disabled:opacity-40"
            title="Trang sau"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
