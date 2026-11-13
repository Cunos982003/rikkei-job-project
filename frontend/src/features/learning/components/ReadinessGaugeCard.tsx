import React from "react";
import { CheckCircle2, AlertTriangle, Sparkles, TrendingUp, Clock, BookOpen } from "lucide-react";
import { Card, CardHeader, CardTitle, CardContent } from "../../../components/common/Card";
import { Badge } from "../../../components/common/Badge";
import type { TargetInfo } from "../../../api/endpoints/learning.api";

interface ReadinessGaugeCardProps {
  target: TargetInfo;
  readiness: number;
  totalRequiredSkills: number;
  passedSkillsCount: number;
  missingSkillsCount: number;
  computedAt: string;
}

export const ReadinessGaugeCard: React.FC<ReadinessGaugeCardProps> = ({
  target,
  readiness,
  totalRequiredSkills,
  passedSkillsCount,
  missingSkillsCount,
  computedAt,
}) => {
  // Format computation date/time
  const formattedTime = React.useMemo(() => {
    if (!computedAt) return "";
    try {
      const d = new Date(computedAt);
      return d.toLocaleString("vi-VN", {
        hour: "2-digit",
        minute: "2-digit",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return computedAt;
    }
  }, [computedAt]);

  // Determine readiness level
  const statusConfig = React.useMemo(() => {
    if (readiness >= 80) {
      return {
        label: "Độ sẵn sàng cao",
        sublabel: "Đạt chuẩn hầu hết các yêu cầu, tự tin ứng tuyển hoặc phỏng vấn!",
        badgeVariant: "verified" as const,
        barColor: "bg-emerald-500",
        textColor: "text-emerald-700",
        bgColor: "bg-emerald-50",
        borderColor: "border-emerald-200",
      };
    }
    if (readiness >= 50) {
      return {
        label: "Cần hoàn thiện thêm",
        sublabel: "Đã có nền tảng tốt, chỉ cần bồi dưỡng thêm các kỹ năng thiếu hụt.",
        badgeVariant: "warning" as const,
        barColor: "bg-amber-500",
        textColor: "text-amber-800",
        bgColor: "bg-amber-50",
        borderColor: "border-amber-200",
      };
    }
    return {
      label: "Cần đào tạo trọng điểm",
      sublabel: "Nhiều kỹ năng cốt lõi chưa đạt chuẩn, nên tập trung học theo lộ trình bổ sung.",
      badgeVariant: "danger" as const,
      barColor: "bg-primary",
      textColor: "text-primary",
      bgColor: "bg-primary-light",
      borderColor: "border-primary-border",
    };
  }, [readiness]);

  return (
    <Card className="border-border shadow-xs">
      <CardHeader className="pb-3 border-b border-border/60">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-[11px] font-bold uppercase tracking-wider text-muted">
                Mục tiêu hiện tại:
              </span>
              <Badge variant={target.type === "JOB" ? "self" : "primary"} size="sm">
                {target.type === "JOB" ? "Tin tuyển dụng (JD)" : "Lộ trình đào tạo chuẩn"}
              </Badge>
            </div>
            <CardTitle className="text-lg font-bold text-foreground mt-1 flex items-center gap-2">
              <BookOpen className="h-5 w-5 text-primary shrink-0" />
              <span>{target.name}</span>
            </CardTitle>
            <p className="text-xs text-muted mt-1 leading-relaxed">
              {target.description || "So sánh hồ sơ năng lực với bộ tiêu chuẩn kỹ năng của vị trí"}
            </p>
          </div>

          <div className="flex sm:flex-col items-center sm:items-end justify-between text-xs text-muted">
            <span className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" />
              <span>Cập nhật: {formattedTime}</span>
            </span>
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-5 pt-4 space-y-5">
        {/* Readiness Meter */}
        <div className={`p-4 rounded-xl border ${statusConfig.borderColor} ${statusConfig.bgColor} transition-all`}>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <TrendingUp className={`h-4 w-4 ${statusConfig.textColor}`} />
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-700">
                  Tỷ lệ sẵn sàng (Readiness Score)
                </span>
                <Badge variant={statusConfig.badgeVariant} size="sm">
                  {statusConfig.label}
                </Badge>
              </div>
              <p className="text-xs text-slate-600 mt-1">
                {statusConfig.sublabel}
              </p>
            </div>

            <div className="flex items-baseline gap-1 self-start sm:self-auto">
              <span className={`text-3xl sm:text-4xl font-extrabold ${statusConfig.textColor}`}>
                {readiness.toFixed(1)}%
              </span>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="mt-3 w-full bg-white/80 rounded-full h-3 overflow-hidden shadow-inner p-0.5 border border-slate-200">
            <div
              className={`h-full rounded-full transition-all duration-700 ease-out ${statusConfig.barColor}`}
              style={{ width: `${Math.min(Math.max(readiness, 0), 100)}%` }}
            />
          </div>
        </div>

        {/* 3 Metric Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="p-3.5 rounded-xl border border-border bg-slate-50/70 flex items-center gap-3">
            <div className="p-2.5 rounded-lg bg-blue-100 text-blue-700 shrink-0">
              <Sparkles className="h-4 w-4" />
            </div>
            <div>
              <p className="text-[11px] font-medium text-muted">Tổng số kỹ năng</p>
              <p className="text-xl font-bold text-foreground">{totalRequiredSkills} <span className="text-xs font-normal text-muted">kỹ năng</span></p>
            </div>
          </div>

          <div className="p-3.5 rounded-xl border border-emerald-200/80 bg-emerald-50/50 flex items-center gap-3">
            <div className="p-2.5 rounded-lg bg-emerald-100 text-emerald-700 shrink-0">
              <CheckCircle2 className="h-4 w-4" />
            </div>
            <div>
              <p className="text-[11px] font-medium text-emerald-800">Đã đạt chuẩn</p>
              <p className="text-xl font-bold text-emerald-900">{passedSkillsCount} <span className="text-xs font-normal text-emerald-700">kỹ năng</span></p>
            </div>
          </div>

          <div className="p-3.5 rounded-xl border border-rose-200/80 bg-rose-50/50 flex items-center gap-3">
            <div className="p-2.5 rounded-lg bg-rose-100 text-rose-700 shrink-0">
              <AlertTriangle className="h-4 w-4" />
            </div>
            <div>
              <p className="text-[11px] font-medium text-rose-800">Còn thiếu hụt</p>
              <p className="text-xl font-bold text-rose-900">{missingSkillsCount} <span className="text-xs font-normal text-rose-700">kỹ năng</span></p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

