import React from "react";
import { AlertOctagon, AlertTriangle, Info, ArrowUpRight, Award, GraduationCap } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../../components/common/Card";
import { Badge } from "../../../components/common/Badge";
import { Button } from "../../../components/common/Button";
import type { GapItem, GapPriority } from "../../../api/endpoints/learning.api";

interface SkillGapTableProps {
  items: GapItem[];
}

export const SkillGapTable: React.FC<SkillGapTableProps> = ({ items }) => {
  const renderPriorityBadge = (priority: GapPriority) => {
    switch (priority) {
      case "HIGH":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertOctagon className="h-3 w-3 text-rose-600" />
            Ưu tiên cao
          </span>
        );
      case "MEDIUM":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <AlertTriangle className="h-3 w-3 text-amber-600" />
            Ưu tiên vừa
          </span>
        );
      case "LOW":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
            <Info className="h-3 w-3 text-slate-500" />
            Ưu tiên thấp
          </span>
        );
    }
  };

  // If no missing skills -> 100% readiness!
  if (!items || items.length === 0) {
    return (
      <Card className="border-emerald-200 bg-emerald-50/30 shadow-xs">
        <CardContent className="p-8 sm:p-12 text-center space-y-4">
          <div className="mx-auto w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 shadow-xs">
            <Award className="h-8 w-8" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-slate-900">
              Tuyệt vời! Bạn đã đạt 100% kỹ năng chuẩn
            </h3>
            <p className="text-sm text-slate-600 max-w-md mx-auto mt-1 leading-relaxed">
              Hồ sơ năng lực của bạn đã đáp ứng đầy đủ tất cả các yêu cầu điểm sàn của vị trí này. Bạn có thể tự tin nộp hồ sơ ứng tuyển ngay hôm nay!
            </p>
          </div>
          <div className="pt-2 flex justify-center gap-3">
            <a
              href="/candidate/jobs"
              className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-white text-sm font-medium hover:bg-primary-hover transition-colors shadow-xs"
            >
              Xem danh sách việc làm phù hợp
              <ArrowUpRight className="h-4 w-4" />
            </a>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border-border shadow-xs overflow-hidden">
      <CardHeader className="pb-3 border-b border-border/60">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <CardTitle className="text-base font-bold text-foreground flex items-center gap-2">
              <span>Danh sách kỹ năng cần bồi dưỡng</span>
              <span className="text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 px-2 py-0.5 rounded-full">
                {items.length} kỹ năng
              </span>
            </CardTitle>
            <CardDescription className="text-xs text-muted mt-0.5">
              Danh sách được sắp xếp tự động theo khoảng thiếu hụt (Gap) giảm dần. Điểm hiện tại lấy từ điểm đã kiểm định (LMS / Khảo thí).
            </CardDescription>
          </div>
        </div>
      </CardHeader>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50/80 border-b border-border text-[11px] font-semibold uppercase tracking-wider text-muted">
              <th className="py-3 px-4">Kỹ năng</th>
              <th className="py-3 px-3 text-center">Bắt buộc</th>
              <th className="py-3 px-3 text-center">Điểm yêu cầu</th>
              <th className="py-3 px-3 text-center">Điểm hiện tại</th>
              <th className="py-3 px-3 text-center">Khoảng thiếu</th>
              <th className="py-3 px-4 text-center">Mức độ ưu tiên</th>
              <th className="py-3 px-4 text-right">Lộ trình bồi dưỡng</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border text-sm">
            {items.map((item) => {
              const currentPercent = Math.min(
                100,
                Math.round((item.current / Math.max(item.required, 1)) * 100)
              );

              return (
                <tr
                  key={item.skillId || item.skillName}
                  className="hover:bg-slate-50/60 transition-colors"
                >
                  {/* Skill Name */}
                  <td className="py-3.5 px-4 font-semibold text-foreground">
                    <div className="flex items-center gap-2">
                      <GraduationCap className="h-4 w-4 text-primary shrink-0" />
                      <span>{item.skillName || item.skill}</span>
                    </div>
                  </td>

                  {/* Mandatory flag */}
                  <td className="py-3.5 px-3 text-center">
                    {item.isMandatory ? (
                      <Badge variant="warning" size="sm">
                        Bắt buộc
                      </Badge>
                    ) : (
                      <span className="text-xs text-muted">Khuyến khích</span>
                    )}
                  </td>

                  {/* Required Score */}
                  <td className="py-3.5 px-3 text-center font-bold text-slate-800">
                    <span className="px-2 py-0.5 bg-slate-100 rounded text-xs font-semibold">
                      {item.required}
                    </span>
                  </td>

                  {/* Current Score */}
                  <td className="py-3.5 px-3 text-center">
                    <div className="inline-flex flex-col items-center">
                      <span
                        className={`text-xs font-bold px-2 py-0.5 rounded ${
                          item.current > 0
                            ? "bg-blue-50 text-blue-700 border border-blue-200"
                            : "bg-slate-100 text-slate-500"
                        }`}
                      >
                        {item.current} điểm
                      </span>
                      {item.current > 0 && (
                        <span className="text-[10px] text-muted mt-0.5">
                          ({currentPercent}%)
                        </span>
                      )}
                    </div>
                  </td>

                  {/* Gap */}
                  <td className="py-3.5 px-3 text-center">
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                      -{item.gap}
                    </span>
                  </td>

                  {/* Priority */}
                  <td className="py-3.5 px-4 text-center">
                    {renderPriorityBadge(item.priority)}
                  </td>

                  {/* Recommendation / Action */}
                  <td className="py-3.5 px-4 text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-xs text-primary hover:text-primary-hover hover:bg-primary-light"
                      rightIcon={<ArrowUpRight className="h-3.5 w-3.5" />}
                      onClick={() => {
                        window.open("http://localhost:9000", "_blank");
                      }}
                    >
                      Học trên LMS
                    </Button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </Card>
  );
};
