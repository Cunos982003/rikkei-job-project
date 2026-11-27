import React from "react";
import {
  BookOpen,
  GraduationCap,
  RefreshCw,
  CheckCircle2,
  Clock,
  Sparkles,
  ExternalLink,
  AlertOctagon,
  AlertTriangle,
  Info,
  Award,
} from "lucide-react";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "../../../components/common/Card";
import { Button } from "../../../components/common/Button";
import { getLmsBaseUrl } from "../../../utils/lms";
import type {
  SkillCourseSuggestion,
  SuggestedCourseItem,
  GapPriority,
} from "../../../api/endpoints/learning.api";

interface CourseSuggestionsSectionProps {
  suggestions: SkillCourseSuggestion[];
  isLoading: boolean;
  onEnroll: (lmsCourseId: string, skillId: string) => Promise<void>;
  onRefresh: () => Promise<void>;
  isRefreshing: boolean;
  enrollingCourseId: string | null;
}

export const CourseSuggestionsSection: React.FC<CourseSuggestionsSectionProps> = ({
  suggestions,
  isLoading,
  onEnroll,
  onRefresh,
  isRefreshing,
  enrollingCourseId,
}) => {
  const lmsBaseUrl = getLmsBaseUrl();

  const renderPriorityBadge = (priority: GapPriority) => {
    switch (priority) {
      case "HIGH":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertOctagon className="h-3 w-3 text-rose-600" />
            Ưu tiên cao
          </span>
        );
      case "MEDIUM":
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <AlertTriangle className="h-3 w-3 text-amber-600" />
            Ưu tiên vừa
          </span>
        );
      case "LOW":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
            <Info className="h-3 w-3 text-slate-500" />
            Ưu tiên thấp
          </span>
        );
    }
  };

  const renderLevelLabel = (course: SuggestedCourseItem) => {
    if (course.level) return course.level;
    if (course.levelTo <= 2) return `Cơ bản (${course.levelFrom}-${course.levelTo})`;
    if (course.levelTo <= 4) return `Trung cấp (${course.levelFrom}-${course.levelTo})`;
    return `Nâng cao (${course.levelFrom}-${course.levelTo})`;
  };

  return (
    <Card className="border-border shadow-xs overflow-hidden">
      {/* Header */}
      <CardHeader className="pb-4 border-b border-border/60 bg-gradient-to-r from-slate-50/80 to-white">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="p-1.5 rounded-lg bg-primary-light text-primary">
                <GraduationCap className="h-5 w-5" />
              </span>
              <CardTitle className="text-lg font-bold text-foreground">
                Gợi ý khóa học bổ sung trên LMS
              </CardTitle>
            </div>
            <CardDescription className="text-xs text-muted">
              Được tự động chọn lọc theo khoảng thiếu hụt năng lực và trình độ hiện tại của bạn. Hoàn thành khóa học trên LMS để nâng cao điểm hồ sơ.
            </CardDescription>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              isLoading={isRefreshing}
              disabled={isRefreshing}
              leftIcon={<RefreshCw className={`h-4 w-4 ${isRefreshing ? "animate-spin text-primary" : ""}`} />}
              className="hover:border-primary/40 hover:text-primary transition-all"
            >
              {isRefreshing ? "Đang đồng bộ..." : "Cập nhật tiến độ LMS"}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-4 sm:p-6 space-y-6">
        {/* Loading state */}
        {isLoading && (
          <div className="space-y-4 animate-pulse">
            <div className="h-28 bg-slate-100 rounded-xl" />
            <div className="h-44 bg-slate-100 rounded-xl" />
          </div>
        )}

        {/* Empty state: No suggestions / 100% ready */}
        {!isLoading && (!suggestions || suggestions.length === 0) && (
          <div className="p-8 text-center space-y-3 rounded-xl border border-dashed border-emerald-200 bg-emerald-50/30">
            <div className="mx-auto w-12 h-12 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600">
              <Award className="h-6 w-6" />
            </div>
            <div>
              <h4 className="text-base font-bold text-slate-900">
                Không có khóa học bắt buộc cần bổ sung!
              </h4>
              <p className="text-xs text-slate-600 max-w-md mx-auto mt-1">
                Tất cả các kỹ năng của bạn đã đạt yêu cầu chuẩn hoặc chưa có môn học phù hợp. Bạn có thể tự do học thêm các khóa nâng cao trên cổng LMS.
              </p>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => window.open(lmsBaseUrl, "_blank")}
              rightIcon={<ExternalLink className="h-3.5 w-3.5" />}
            >
              Truy cập Cổng học tập LMS
            </Button>
          </div>
        )}

        {/* Suggestion list grouped by missing skills */}
        {!isLoading &&
          suggestions &&
          suggestions.map((skill) => (
            <div
              key={skill.skillId}
              className="rounded-xl border border-border/80 bg-white shadow-xs overflow-hidden"
            >
              {/* Skill Group Header */}
              <div className="px-4 py-3.5 bg-slate-50/90 border-b border-border/60 flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <div className="w-2 h-2 rounded-full bg-primary" />
                  <span className="font-bold text-sm text-foreground">
                    {skill.skillName}
                  </span>
                  {renderPriorityBadge(skill.priority)}
                </div>

                <div className="flex items-center gap-3 text-xs">
                  <span className="text-muted">
                    Yêu cầu:{" "}
                    <strong className="text-slate-800 font-semibold">
                      {skill.requiredScore}
                    </strong>
                  </span>
                  <span className="text-muted">
                    Hiện tại:{" "}
                    <strong className="text-blue-700 font-semibold">
                      {skill.currentScore}
                    </strong>
                  </span>
                  <span className="px-2 py-0.5 rounded font-bold bg-rose-50 text-rose-700 border border-rose-200">
                    Thiếu: -{skill.gap} điểm
                  </span>
                </div>
              </div>

              {/* Course items */}
              <div className="p-4">
                {skill.courses && skill.courses.length > 0 ? (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {skill.courses.map((course) => {
                      const isEnrolling = enrollingCourseId === course.lmsCourseId;
                      const isCompleted = course.enrollmentStatus === "COMPLETED";
                      const isInProgress =
                        course.isEnrolled && course.enrollmentStatus !== "COMPLETED";

                      return (
                        <div
                          key={course.lmsCourseId}
                          className={`relative flex flex-col justify-between p-4 rounded-xl border transition-all ${
                            isCompleted
                              ? "border-emerald-200 bg-emerald-50/20 hover:border-emerald-300"
                              : isInProgress
                              ? "border-blue-200 bg-blue-50/20 hover:border-blue-300"
                              : "border-border bg-white hover:border-slate-300 hover:shadow-xs"
                          }`}
                        >
                          <div>
                            {/* Top row: LMS ID badge + Level */}
                            <div className="flex items-center justify-between gap-2 mb-2">
                              <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                                {course.lmsCourseId}
                              </span>
                              <span className="text-[11px] font-medium text-slate-500 bg-slate-50 px-2 py-0.5 rounded-full border border-slate-200">
                                {renderLevelLabel(course)}
                              </span>
                            </div>

                            {/* Course Title */}
                            <h5 className="font-bold text-sm text-foreground leading-snug">
                              {course.courseName}
                            </h5>

                            {/* Lesson hint */}
                            {course.lessonHint && (
                              <div className="mt-2.5 p-2 rounded-lg bg-amber-50/70 border border-amber-200/60 flex items-start gap-2 text-xs text-amber-900">
                                <Sparkles className="h-3.5 w-3.5 text-amber-600 shrink-0 mt-0.5" />
                                <span className="leading-relaxed">
                                  <strong>Bài học trọng tâm:</strong> {course.lessonHint}
                                </span>
                              </div>
                            )}
                          </div>

                          {/* Footer with status & action buttons */}
                          <div className="mt-4 pt-3 border-t border-border/50 flex items-center justify-between gap-2">
                            {/* CASE 1: COMPLETED */}
                            {isCompleted && (
                              <>
                                <div className="flex items-center gap-1.5 text-emerald-700 text-xs font-semibold">
                                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                                  <span>Đã hoàn thành</span>
                                  {course.score !== null && course.score !== undefined && (
                                    <span className="px-1.5 py-0.5 rounded bg-emerald-100 text-emerald-800 text-[11px] font-bold">
                                      {course.score} điểm
                                    </span>
                                  )}
                                </div>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="text-xs text-slate-600 hover:text-slate-900"
                                  onClick={() => window.open(lmsBaseUrl, "_blank")}
                                  rightIcon={<ExternalLink className="h-3 w-3" />}
                                >
                                  Xem trên LMS
                                </Button>
                              </>
                            )}

                            {/* CASE 2: IN PROGRESS / ENROLLED */}
                            {isInProgress && (
                              <>
                                <div className="space-y-1 w-1/2">
                                  <div className="flex items-center justify-between text-xs text-blue-700 font-semibold">
                                    <span className="flex items-center gap-1">
                                      <Clock className="h-3.5 w-3.5 text-blue-600" />
                                      Đang học
                                    </span>
                                    <span>{course.progress || 0}%</span>
                                  </div>
                                  <div className="w-full bg-blue-100 h-1.5 rounded-full overflow-hidden">
                                    <div
                                      className="bg-blue-600 h-full rounded-full transition-all duration-300"
                                      style={{ width: `${Math.min(100, Math.max(0, course.progress || 0))}%` }}
                                    />
                                  </div>
                                </div>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="text-xs text-blue-700 border-blue-200 hover:bg-blue-50"
                                  onClick={() => window.open(lmsBaseUrl, "_blank")}
                                  rightIcon={<ExternalLink className="h-3 w-3" />}
                                >
                                  Học tiếp
                                </Button>
                              </>
                            )}

                            {/* CASE 3: NOT ENROLLED */}
                            {!course.isEnrolled && (
                              <>
                                <span className="text-xs text-muted flex items-center gap-1">
                                  <BookOpen className="h-3.5 w-3.5 text-slate-400" />
                                  Chưa ghi danh
                                </span>
                                <Button
                                  variant="primary"
                                  size="sm"
                                  onClick={() => onEnroll(course.lmsCourseId, skill.skillId)}
                                  isLoading={isEnrolling}
                                  disabled={isEnrolling}
                                  leftIcon={<GraduationCap className="h-3.5 w-3.5" />}
                                  className="text-xs"
                                >
                                  Ghi danh
                                </Button>
                              </>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-xs text-muted italic">
                    Chưa có khóa học LMS nào phù hợp với cấp độ hiện tại của kỹ năng này.
                  </p>
                )}
              </div>
            </div>
          ))}
      </CardContent>
    </Card>
  );
};
