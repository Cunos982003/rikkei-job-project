import { BarChart3 } from "lucide-react";
import type { TopMissingSkillItem } from "../../../../api/endpoints/learning.api";

interface TopMissingSkillsChartProps {
  skills: TopMissingSkillItem[];
  selectedSkillId?: string;
  onSelectSkill?: (skillId: string) => void;
  isLoading: boolean;
}

const BAR_COLORS = [
  "bg-rose-500 hover:bg-rose-600",
  "bg-orange-500 hover:bg-orange-600",
  "bg-amber-500 hover:bg-amber-600",
  "bg-blue-500 hover:bg-blue-600",
  "bg-indigo-500 hover:bg-indigo-600",
];

export function TopMissingSkillsChart({
  skills,
  selectedSkillId,
  onSelectSkill,
  isLoading,
}: TopMissingSkillsChartProps) {
  const maxCount = skills.length > 0 ? Math.max(...skills.map((s) => s.missingCount), 1) : 1;

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-rose-50 text-rose-600">
            <BarChart3 className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">
              Top 5 kỹ năng bị thiếu nhiều nhất
            </h2>
            <p className="text-xs text-slate-500">
              Bấm vào một cột kỹ năng để lọc nhanh danh sách ứng viên đang thiếu kỹ năng đó
            </p>
          </div>
        </div>
        {selectedSkillId && onSelectSkill && (
          <button
            type="button"
            onClick={() => onSelectSkill("")}
            className="text-xs font-medium text-blue-600 hover:text-blue-700 underline"
          >
            Bỏ lọc kỹ năng ({selectedSkillId})
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="h-48 flex items-center justify-center text-sm text-slate-400">
          Đang tải biểu đồ thống kê kỹ năng...
        </div>
      ) : skills.length === 0 ? (
        <div className="h-48 flex items-center justify-center text-sm text-slate-400">
          Chưa có dữ liệu kỹ năng thiếu hụt.
        </div>
      ) : (
        <div className="grid grid-cols-5 gap-3 sm:gap-6 items-end h-52 pt-6 pb-2 px-2 border-b border-slate-100">
          {skills.map((skill, index) => {
            const heightPercent = Math.max(12, Math.round((skill.missingCount / maxCount) * 100));
            const isSelected = selectedSkillId?.toUpperCase() === skill.skillId.toUpperCase();
            const colorClass = BAR_COLORS[index % BAR_COLORS.length];

            return (
              <button
                key={skill.skillId}
                type="button"
                onClick={() =>
                  onSelectSkill?.(isSelected ? "" : skill.skillId)
                }
                className={`group flex flex-col items-center h-full justify-end focus:outline-none transition-opacity ${
                  selectedSkillId && !isSelected ? "opacity-45 hover:opacity-80" : "opacity-100"
                }`}
                title={`${skill.skillName}: ${skill.missingCount} ứng viên (${skill.percentage}%)`}
              >
                <span className="text-xs font-bold text-slate-700 mb-1.5">
                  {skill.missingCount} HV
                  <span className="hidden sm:inline text-[11px] font-normal text-slate-400 ml-1">
                    ({skill.percentage}%)
                  </span>
                </span>
                <div className="w-full max-w-[56px] flex-1 flex items-end justify-center">
                  <div
                    style={{ height: `${heightPercent}%` }}
                    className={`w-full rounded-t-lg transition-all duration-300 shadow-sm ${colorClass} ${
                      isSelected ? "ring-2 ring-offset-2 ring-slate-900" : ""
                    }`}
                  />
                </div>
                <span className="mt-2.5 text-xs font-semibold text-slate-700 text-center line-clamp-1">
                  {skill.skillName}
                </span>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
