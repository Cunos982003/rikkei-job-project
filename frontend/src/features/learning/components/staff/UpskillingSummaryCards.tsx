import { BookOpen, CheckCircle2, AlertTriangle, TrendingUp } from "lucide-react";
import type { UpskillingSummaryData } from "../../../../api/endpoints/learning.api";

interface UpskillingSummaryCardsProps {
  summary?: UpskillingSummaryData;
  isLoading: boolean;
}

export function UpskillingSummaryCards({ summary, isLoading }: UpskillingSummaryCardsProps) {
  const inProgressCount = summary?.inProgressCount ?? 0;
  const completedCount = summary?.completedCount ?? 0;
  const totalTracked = inProgressCount + completedCount;
  const completionRate =
    totalTracked > 0 ? Math.round((completedCount / totalTracked) * 100) : 0;
  const topSkill = summary?.topMissingSkills?.[0];

  const cards = [
    {
      label: "Ứng viên đang học",
      value: isLoading ? "..." : String(inProgressCount),
      subtext: "Đang tham gia khoá bổ sung LMS",
      icon: BookOpen,
      iconBg: "bg-blue-50 text-blue-600",
      borderAccent: "border-l-4 border-l-blue-500",
    },
    {
      label: "Đã hoàn thành",
      value: isLoading ? "..." : String(completedCount),
      subtext: "Hoàn thành toàn bộ môn học mục tiêu",
      icon: CheckCircle2,
      iconBg: "bg-emerald-50 text-emerald-600",
      borderAccent: "border-l-4 border-l-emerald-500",
    },
    {
      label: "Kỹ năng thiếu nhiều nhất",
      value: isLoading ? "..." : topSkill ? topSkill.skillName : "Không có",
      subtext: topSkill
        ? `${topSkill.missingCount} ứng viên đang thiếu (${topSkill.percentage}%)`
        : "Chưa có dữ liệu thiếu hụt",
      icon: AlertTriangle,
      iconBg: "bg-amber-50 text-amber-600",
      borderAccent: "border-l-4 border-l-amber-500",
    },
    {
      label: "Tỷ lệ hoàn thành",
      value: isLoading ? "..." : `${completionRate}%`,
      subtext: `Trên tổng ${totalTracked} học viên đang theo dõi`,
      icon: TrendingUp,
      iconBg: "bg-indigo-50 text-indigo-600",
      borderAccent: "border-l-4 border-l-indigo-500",
    },
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
      {cards.map((card) => {
        const Icon = card.icon;
        return (
          <div
            key={card.label}
            className={`bg-white rounded-xl border border-slate-200 p-4 shadow-sm hover:shadow-md transition-shadow ${card.borderAccent}`}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  {card.label}
                </p>
                <p className="mt-1.5 text-2xl font-bold text-slate-900 truncate">
                  {card.value}
                </p>
                <p className="mt-1 text-xs text-slate-500 truncate">
                  {card.subtext}
                </p>
              </div>
              <div className={`p-2.5 rounded-lg shrink-0 ${card.iconBg}`}>
                <Icon className="w-5 h-5" />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
