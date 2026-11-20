import React from "react";
import {
  TrendingUp,
  Target,
  BookOpen,
  Sparkles,
  ArrowRight,
} from "lucide-react";
import { Card, CardContent } from "../../../components/common/Card";
import { Badge } from "../../../components/common/Badge";
import type { TargetType } from "../../../api/endpoints/learning.api";

interface ReadinessCircularGaugeProps {
  targetName: string;
  targetType?: TargetType;
  readinessAtStart: number;
  currentReadiness: number;
  readinessGrowth: number;
  completionPercentage: number;
  totalSteps: number;
  completedSteps: number;
  createdAt?: string;
}

export const ReadinessCircularGauge: React.FC<ReadinessCircularGaugeProps> = ({
  targetName,
  targetType = "POSITION",
  readinessAtStart,
  currentReadiness,
  readinessGrowth,
  completionPercentage,
  totalSteps,
  completedSteps,
  createdAt,
}) => {
  // SVG circular progress calculation
  const radius = 54;
  const circumference = 2 * Math.PI * radius;
  const clampedReadiness = Math.min(100, Math.max(0, currentReadiness));
  const strokeDashoffset = circumference - (clampedReadiness / 100) * circumference;

  // Status configuration based on readiness level
  const statusConfig = React.useMemo(() => {
    if (clampedReadiness >= 80) {
      return {
        label: "Sẵn sàng cao",
        textColor: "text-emerald-600",
        strokeColor: "#10b981", // emerald-500
        bgColor: "bg-emerald-50",
        borderColor: "border-emerald-200",
        badgeVariant: "verified" as const,
        description: "Hồ sơ năng lực đã tiệm cận và sẵn sàng ứng tuyển vị trí mục tiêu!",
      };
    }
    if (clampedReadiness >= 50) {
      return {
        label: "Khá - Cần bồi dưỡng",
        textColor: "text-amber-600",
        strokeColor: "#f59e0b", // amber-500
        bgColor: "bg-amber-50",
        borderColor: "border-amber-200",
        badgeVariant: "warning" as const,
        description: "Đã có nền tảng vững, chỉ cần bồi dưỡng thêm các kỹ năng thiếu hụt.",
      };
    }
    return {
      label: "Cần bổ sung nhiều",
      textColor: "text-rose-600",
      strokeColor: "#e11d48", // rose-600
        bgColor: "bg-rose-50",
      borderColor: "border-rose-200",
      badgeVariant: "danger" as const,
      description: "Nhiều kỹ năng còn khoảng trống, hãy hoàn thành các bước trong lộ trình học.",
    };
  }, [clampedReadiness]);

  // Formatted creation date
  const formattedCreatedDate = React.useMemo(() => {
    if (!createdAt) return "";
    try {
      const d = new Date(createdAt);
      return d.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return "";
    }
  }, [createdAt]);

  return (
    <Card className="border-border shadow-xs overflow-hidden">
      <CardContent className="p-5 sm:p-6">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-center">
          {/* LEFT: Circular Progress Gauge */}
          <div className="lg:col-span-4 flex flex-col items-center justify-center p-3 rounded-2xl bg-slate-50/70 border border-slate-100">
            <div className="relative w-40 h-40 flex items-center justify-center">
              <svg className="w-full h-full transform -rotate-90" viewBox="0 0 140 140">
                {/* Background track circle */}
                <circle
                  cx="70"
                  cy="70"
                  r={radius}
                  fill="transparent"
                  stroke="#e2e8f0"
                  strokeWidth="11"
                />
                {/* Active progress circle */}
                <circle
                  cx="70"
                  cy="70"
                  r={radius}
                  fill="transparent"
                  stroke={statusConfig.strokeColor}
                  strokeWidth="11"
                  strokeDasharray={circumference}
                  strokeDashoffset={strokeDashoffset}
                  strokeLinecap="round"
                  className="transition-all duration-1000 ease-out"
                />
              </svg>

              {/* Inside circle metrics */}
              <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
                <span className={`text-3xl font-extrabold tracking-tight ${statusConfig.textColor}`}>
                  {Math.round(clampedReadiness)}%
                </span>
                <span className="text-[11px] font-semibold text-muted uppercase tracking-wider mt-0.5">
                  Mức sẵn sàng
                </span>
              </div>
            </div>

            <div className="mt-3 text-center">
              <Badge variant={statusConfig.badgeVariant} size="sm">
                {statusConfig.label}
              </Badge>
            </div>
          </div>

          {/* RIGHT: Detailed Comparison & Growth Metrics */}
          <div className="lg:col-span-8 flex flex-col justify-between h-full space-y-4">
            {/* Header: Target Name & Creation date */}
            <div>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <Target className="h-4 w-4 text-primary shrink-0" />
                  <span className="text-xs font-semibold text-muted uppercase tracking-wide">
                    Mục tiêu lộ trình:
                  </span>
                  <Badge variant={targetType === "JOB" ? "self" : "primary"} size="sm">
                    {targetType === "JOB" ? "JD tuyển dụng" : "Vị trí chuẩn"}
                  </Badge>
                </div>
                {formattedCreatedDate && (
                  <span className="text-xs text-muted">
                    Bắt đầu từ: <strong className="font-medium text-foreground">{formattedCreatedDate}</strong>
                  </span>
                )}
              </div>

              <h3 className="text-xl font-bold text-foreground mt-1 flex items-center gap-2">
                <span>{targetName}</span>
              </h3>
              <p className="text-xs text-muted mt-1 leading-relaxed">
                {statusConfig.description}
              </p>
            </div>

            {/* Metrics Grid: Growth & Step Progress */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
              {/* Box 1: Readiness Growth */}
              <div className="p-3.5 rounded-xl border border-border/80 bg-surface">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted flex items-center gap-1.5">
                    <Sparkles className="h-3.5 w-3.5 text-amber-500" />
                    So sánh mức sẵn sàng
                  </span>
                  {readinessGrowth > 0 ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                      <TrendingUp className="h-3 w-3" />
                      +{readinessGrowth.toFixed(1)}%
                    </span>
                  ) : (
                    <span className="text-xs font-medium text-muted">Chưa tăng</span>
                  )}
                </div>

                <div className="mt-2.5 flex items-center justify-between text-sm">
                  <div>
                    <span className="text-[11px] text-muted block">Lúc tạo:</span>
                    <strong className="text-base font-bold text-slate-700">
                      {Math.round(readinessAtStart)}%
                    </strong>
                  </div>
                  <ArrowRight className="h-4 w-4 text-muted/60" />
                  <div className="text-right">
                    <span className="text-[11px] text-muted block">Hiện tại:</span>
                    <strong className={`text-base font-bold ${statusConfig.textColor}`}>
                      {Math.round(currentReadiness)}%
                    </strong>
                  </div>
                </div>
              </div>

              {/* Box 2: Path Steps Completion */}
              <div className="p-3.5 rounded-xl border border-border/80 bg-surface">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted flex items-center gap-1.5">
                    <BookOpen className="h-3.5 w-3.5 text-blue-500" />
                    Tiến độ hoàn thành lộ trình
                  </span>
                  <span className="text-xs font-bold text-foreground">
                    {Math.round(completionPercentage)}%
                  </span>
                </div>

                <div className="mt-2 flex items-center justify-between">
                  <span className="text-xs text-muted">
                    Số bước đã xong:
                  </span>
                  <span className="text-sm font-bold text-foreground">
                    <span className="text-emerald-600">{completedSteps}</span> / {totalSteps} bước
                  </span>
                </div>

                {/* Mini progress bar */}
                <div className="w-full bg-slate-100 rounded-full h-2 mt-2 overflow-hidden border border-slate-200/60">
                  <div
                    className="bg-emerald-500 h-2 rounded-full transition-all duration-700"
                    style={{ width: `${Math.min(100, Math.max(0, completionPercentage))}%` }}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
