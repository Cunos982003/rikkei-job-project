import React from "react";
import {
  CheckCircle2,
  Clock,
  BookOpen,
  Sparkles,
  ExternalLink,
  Award,
  RefreshCw,
} from "lucide-react";
import { Card, CardHeader, CardTitle, CardContent } from "../../../components/common/Card";
import { Badge } from "../../../components/common/Badge";
import { Button } from "../../../components/common/Button";
import { getLmsBaseUrl } from "../../../utils/lms";
import type { LearningPathStep } from "../../../api/endpoints/learning.api";

interface LearningPathTimelineProps {
  steps: LearningPathStep[];
  onEnroll: (lmsCourseId: string, skillId: string) => Promise<void>;
  isEnrolling?: boolean;
  enrollingCourseId?: string | null;
  onRefresh?: () => Promise<void>;
  isRefreshing?: boolean;
}

export const LearningPathTimeline: React.FC<LearningPathTimelineProps> = ({
  steps,
  onEnroll,
  isEnrolling = false,
  enrollingCourseId = null,
  onRefresh,
  isRefreshing = false,
}) => {
  const lmsBaseUrl = getLmsBaseUrl();

  // Format date helper
  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return "";
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString("vi-VN", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <Card className="border-border shadow-xs">
      <CardHeader className="pb-3 border-b border-border/60">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <CardTitle className="text-base font-bold text-foreground flex items-center gap-2">
              <BookOpen className="h-5 w-5 text-primary shrink-0" />
              <span>Dòng thời gian các bước học tập (Learning Timeline)</span>
            </CardTitle>
            <p className="text-xs text-muted mt-0.5">
              Thứ tự học tập tối ưu: Kỹ năng bắt buộc học trước, kỹ năng thiếu nhiều học trước.
            </p>
          </div>

          {onRefresh && (
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={isRefreshing}
              isLoading={isRefreshing}
              leftIcon={<RefreshCw className={`h-3.5 w-3.5 ${isRefreshing ? "animate-spin" : ""}`} />}
              className="text-xs shrink-0"
            >
              Cập nhật tiến độ từ LMS
            </Button>
          )}
        </div>
      </CardHeader>

      <CardContent className="p-5 sm:p-6">
        {steps.length === 0 ? (
          <div className="py-12 text-center text-muted text-sm">
            Chưa có bước học tập nào trong lộ trình.
          </div>
        ) : (
          <div className="relative pl-4 sm:pl-6 space-y-8 before:absolute before:left-[19px] sm:before:left-[27px] before:top-3 before:bottom-3 before:w-[2px] before:bg-slate-200">
            {steps.map((step) => {
              const isCurrentlyEnrolling = isEnrolling && enrollingCourseId === step.lmsCourseId;
              const isDone = step.status === "DONE";
              const isDoing = step.status === "DOING";
              const isTodo = step.status === "TODO";

              return (
                <div key={step.id || step.stepOrder} className="relative flex items-start gap-4">
                  {/* Step Node Marker on vertical line */}
                  <div className="relative z-10 shrink-0">
                    {isDone ? (
                      <div className="w-8 h-8 rounded-full bg-emerald-100 border-2 border-emerald-500 flex items-center justify-center text-emerald-700 shadow-xs">
                        <CheckCircle2 className="h-4 w-4" />
                      </div>
                    ) : isDoing ? (
                      <div className="relative w-8 h-8 rounded-full bg-sky-100 border-2 border-sky-500 flex items-center justify-center text-sky-700 shadow-xs">
                        <Clock className="h-4 w-4" />
                        <span className="absolute -top-0.5 -right-0.5 w-2.5 h-2.5 rounded-full bg-sky-500 animate-ping" />
                      </div>
                    ) : (
                      <div className="w-8 h-8 rounded-full bg-slate-100 border-2 border-slate-300 flex items-center justify-center text-slate-700 text-xs font-bold shadow-xs">
                        {step.stepOrder}
                      </div>
                    )}
                  </div>

                  {/* Step Card Content */}
                  <div
                    className={`flex-1 rounded-xl border p-4 sm:p-5 transition-all ${
                      isDone
                        ? "border-emerald-200 bg-emerald-50/20"
                        : isDoing
                        ? "border-sky-300 bg-sky-50/20 shadow-xs"
                        : "border-border bg-surface hover:border-slate-300"
                    }`}
                  >
                    {/* Header: Step order, Skill name & Status badge */}
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="px-2 py-0.5 rounded text-[11px] font-bold uppercase tracking-wider bg-slate-100 text-slate-700">
                          Bước {step.stepOrder}
                        </span>
                        <h4 className="font-bold text-base text-foreground">
                          {step.skillName}
                        </h4>
                        {step.isMandatory ? (
                          <Badge variant="danger" size="sm">
                            Bắt buộc
                          </Badge>
                        ) : (
                          <Badge variant="default" size="sm">
                            Khuyến nghị
                          </Badge>
                        )}
                      </div>

                      {/* Status badge */}
                      {isDone && (
                        <Badge variant="verified" size="sm" showIcon>
                          Đã hoàn thành
                        </Badge>
                      )}
                      {isDoing && (
                        <Badge variant="self" size="sm">
                          Đang học
                        </Badge>
                      )}
                      {isTodo && (
                        <Badge variant="outline" size="sm">
                          Chưa học
                        </Badge>
                      )}
                    </div>

                    {/* Skill Score Requirements */}
                    <div className="mt-2 flex flex-wrap items-center gap-4 text-xs text-muted">
                      <span>
                        Yêu cầu: <strong className="text-foreground">{step.requiredScore} điểm</strong>
                      </span>
                      <span>
                        Hiện tại: <strong className="text-sky-700">{step.currentScore} điểm</strong>
                      </span>
                      <span className="text-rose-700 font-semibold">
                        Thiếu hụt: -{step.gap} điểm
                      </span>
                    </div>

                    {/* Course Suggestion Box */}
                    <div className="mt-3.5 p-3.5 rounded-lg border border-border/80 bg-white/80 space-y-2">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200 font-semibold">
                          Mã: {step.lmsCourseId}
                        </span>
                        {isDone && step.score !== null && step.score !== undefined && (
                          <span className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                            <Award className="h-3.5 w-3.5 text-emerald-600" />
                            Đạt: {step.score} điểm
                          </span>
                        )}
                      </div>

                      <h5 className="font-bold text-sm text-foreground">
                        {step.courseName}
                      </h5>

                      {/* Lesson Hint */}
                      {step.lessonHint && (
                        <div className="p-2 rounded bg-amber-50/70 border border-amber-200/60 text-xs text-amber-900 flex items-start gap-2">
                          <Sparkles className="h-3.5 w-3.5 text-amber-600 shrink-0 mt-0.5" />
                          <span>
                            <strong>Trọng tâm:</strong> {step.lessonHint}
                          </span>
                        </div>
                      )}

                      {/* Progress bar if DOING */}
                      {isDoing && step.progress !== null && step.progress !== undefined && (
                        <div className="pt-1">
                          <div className="flex items-center justify-between text-xs text-muted mb-1">
                            <span>Tiến độ học tập:</span>
                            <strong className="text-sky-700 font-bold">{step.progress}%</strong>
                          </div>
                          <div className="w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
                            <div
                              className="bg-sky-500 h-1.5 rounded-full transition-all duration-500"
                              style={{ width: `${step.progress}%` }}
                            />
                          </div>
                        </div>
                      )}

                      {/* Done Date if DONE */}
                      {isDone && step.doneAt && (
                        <div className="text-[11px] text-muted flex items-center gap-1 pt-1">
                          <Clock className="h-3 w-3" />
                          <span>Hoàn thành lúc: {formatDate(step.doneAt)}</span>
                        </div>
                      )}
                    </div>

                    {/* Action Buttons */}
                    <div className="mt-4 flex flex-wrap items-center justify-end gap-2 pt-2 border-t border-border/50">
                      {isTodo && (
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => onEnroll(step.lmsCourseId, step.skillId)}
                          disabled={isEnrolling}
                          isLoading={isCurrentlyEnrolling}
                          leftIcon={<BookOpen className="h-3.5 w-3.5" />}
                          className="text-xs"
                        >
                          Ghi danh khóa học
                        </Button>
                      )}

                      {isDoing && (
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => window.open(lmsBaseUrl, "_blank")}
                          rightIcon={<ExternalLink className="h-3 w-3" />}
                          className="text-xs"
                        >
                          Tiếp tục học trên LMS
                        </Button>
                      )}

                      {isDone && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => window.open(lmsBaseUrl, "_blank")}
                          rightIcon={<ExternalLink className="h-3 w-3" />}
                          className="text-xs text-slate-600 hover:text-slate-900"
                        >
                          Xem lại trên LMS
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
