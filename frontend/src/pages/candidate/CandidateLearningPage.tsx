import React, { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  GraduationCap,
  RefreshCw,
  Compass,
  ArrowRight,
  Route,
  Sparkles,
  Target,
  PlusCircle,
} from "lucide-react";
import { useAuthStore } from "../../store/useAuthStore";
import { learningApi } from "../../api/endpoints/learning.api";
import { Button } from "../../components/common/Button";
import { Card, CardContent } from "../../components/common/Card";
import { ReadinessCircularGauge } from "../../features/learning/components/ReadinessCircularGauge";
import { SkillRadarChart } from "../../features/learning/components/SkillRadarChart";
import { LearningPathTimeline } from "../../features/learning/components/LearningPathTimeline";
import { LearningPathSuccessBanner } from "../../features/learning/components/LearningPathSuccessBanner";
import { ReadinessGaugeCard } from "../../features/learning/components/ReadinessGaugeCard";
import { SkillGapTable } from "../../features/learning/components/SkillGapTable";
import { CourseSuggestionsSection } from "../../features/learning/components/CourseSuggestionsSection";
import { TargetSelectorCard } from "../../features/learning/components/TargetSelectorCard";
import { JobAnalysisBanner } from "../../features/learning/components/JobAnalysisBanner";

export const CandidateLearningPage: React.FC = () => {
  const { isCandidate } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [enrollingCourseId, setEnrollingCourseId] = useState<string | null>(null);

  const jobIdOverride = searchParams.get("jobId") || undefined;
  const initialTab = jobIdOverride ? "gap" : (searchParams.get("tab") as "path" | "gap") || "path";
  const [activeTab, setActiveTab] = useState<"path" | "gap">(initialTab);

  // Sync active tab with url if changed
  const handleTabChange = (tab: "path" | "gap") => {
    setActiveTab(tab);
    const newParams = new URLSearchParams(searchParams);
    newParams.set("tab", tab);
    setSearchParams(newParams);
  };

  // Verify role
  useEffect(() => {
    if (!isCandidate()) {
      navigate("/unauthorized");
    }
  }, [isCandidate, navigate]);

  // 1. Fetch available target positions
  const {
    data: positionsRes,
    isLoading: positionsLoading,
  } = useQuery({
    queryKey: ["targetPositions"],
    queryFn: () => learningApi.getTargetPositions(),
    staleTime: 10 * 60 * 1000,
  });

  // 2. Fetch candidate's current set target
  const {
    data: myTargetRes,
    isLoading: myTargetLoading,
  } = useQuery({
    queryKey: ["myTarget"],
    queryFn: () => learningApi.getMyTarget(),
    staleTime: 5 * 60 * 1000,
  });

  // 3. Fetch gap analysis result (either by saved target or by jobIdOverride)
  const {
    data: gapAnalysisRes,
    isLoading: gapLoading,
    isRefetching: gapRefetching,
    refetch: refetchGap,
  } = useQuery({
    queryKey: ["gapAnalysis", jobIdOverride || "default"],
    queryFn: () => learningApi.getGapAnalysis(jobIdOverride),
    retry: 1,
  });

  // 4. Fetch course suggestions based on missing skills (DT-02)
  const {
    data: suggestionsRes,
    isLoading: suggestionsLoading,
    refetch: refetchSuggestions,
  } = useQuery({
    queryKey: ["courseSuggestions"],
    queryFn: () => learningApi.getCourseSuggestions(),
    staleTime: 5 * 60 * 1000,
  });

  // 5. Fetch candidate's active learning path (DT-03)
  const {
    data: learningPathRes,
    isLoading: pathLoading,
    isRefetching: pathRefetching,
    refetch: refetchPath,
  } = useQuery({
    queryKey: ["myLearningPath"],
    queryFn: () => learningApi.getMyLearningPath(),
    staleTime: 5 * 60 * 1000,
  });

  // 6. Mutation to generate learning path (DT-03)
  const generatePathMutation = useMutation({
    mutationFn: () => learningApi.generateLearningPath(),
    onSuccess: (res) => {
      toast.success(res.message || "Đã sinh lộ trình học tập tối ưu thành công!");
      queryClient.invalidateQueries({ queryKey: ["myLearningPath"] });
      queryClient.invalidateQueries({ queryKey: ["gapAnalysis"] });
      queryClient.invalidateQueries({ queryKey: ["courseSuggestions"] });
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        "Không thể sinh lộ trình học tập. Vui lòng thiết lập mục tiêu trước.";
      toast.error(msg);
    },
  });

  // 7. Mutation to set / change target
  const setTargetMutation = useMutation({
    mutationFn: (payload: { positionId?: string; jobId?: string }) =>
      learningApi.setMyTarget(payload),
    onSuccess: (res) => {
      toast.success(res.message || "Thiết lập mục tiêu nghề nghiệp thành công!");
      queryClient.invalidateQueries({ queryKey: ["myTarget"] });
      queryClient.invalidateQueries({ queryKey: ["gapAnalysis"] });
      queryClient.invalidateQueries({ queryKey: ["courseSuggestions"] });
      queryClient.invalidateQueries({ queryKey: ["myLearningPath"] });
      if (jobIdOverride) {
        setSearchParams({});
      }
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || "Không thể cập nhật mục tiêu. Vui lòng thử lại sau";
      toast.error(msg);
    },
  });

  // 8. Mutation to enroll course (DT-02 / DT-03)
  const enrollMutation = useMutation({
    mutationFn: (payload: { lmsCourseId: string; skillId: string }) =>
      learningApi.enrollCourse(payload),
    onMutate: (payload) => {
      setEnrollingCourseId(payload.lmsCourseId);
    },
    onSuccess: (res) => {
      toast.success(res.message || "Ghi danh khóa học thành công!");
      queryClient.invalidateQueries({ queryKey: ["courseSuggestions"] });
      queryClient.invalidateQueries({ queryKey: ["gapAnalysis"] });
      queryClient.invalidateQueries({ queryKey: ["myLearningPath"] });
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        "Không thể ghi danh khóa học. Vui lòng thử lại sau";
      toast.error(msg);
    },
    onSettled: () => {
      setEnrollingCourseId(null);
    },
  });

  // 9. Mutation to refresh LMS enrollments & progress (DT-02 / DT-03)
  const refreshMutation = useMutation({
    mutationFn: () => learningApi.refreshEnrollments(),
    onSuccess: (res) => {
      const total = res.data?.totalUpdated ?? 0;
      const completed = res.data?.completedCoursesCount ?? 0;
      toast.success(
        res.message ||
          `Đã đồng bộ ${total} khóa học từ LMS! (${completed} môn đã hoàn thành)`
      );
      queryClient.invalidateQueries({ queryKey: ["courseSuggestions"] });
      queryClient.invalidateQueries({ queryKey: ["gapAnalysis"] });
      queryClient.invalidateQueries({ queryKey: ["myProfileSkills"] });
      queryClient.invalidateQueries({ queryKey: ["myLearningPath"] });
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        "Không thể đồng bộ tiến độ LMS. Vui lòng thử lại sau";
      toast.error(msg);
    },
  });

  const positions = positionsRes?.data || [];
  const myTarget = myTargetRes?.data;
  const gapAnalysis = gapAnalysisRes?.data;
  const learningPath = learningPathRes?.data;

  const isInitialLoading =
    (positionsLoading || myTargetLoading || gapLoading || pathLoading) &&
    !gapAnalysis &&
    !learningPath;

  const handleSelectPosition = (positionId: string) => {
    setTargetMutation.mutate({ positionId });
  };

  const handleSelectJob = (jobId: string) => {
    setSearchParams({ jobId, tab: "gap" });
    setActiveTab("gap");
  };

  const handleClearOverride = () => {
    const newParams = new URLSearchParams(searchParams);
    newParams.delete("jobId");
    setSearchParams(newParams);
  };

  const handleSaveOverrideAsMainTarget = () => {
    if (jobIdOverride) {
      setTargetMutation.mutate({ jobId: jobIdOverride });
    }
  };

  const handleEnroll = async (lmsCourseId: string, skillId: string) => {
    await enrollMutation.mutateAsync({ lmsCourseId, skillId });
  };

  const handleRefreshEnrollments = async () => {
    await refreshMutation.mutateAsync();
  };

  const handleGeneratePath = () => {
    generatePathMutation.mutate();
  };

  const isPathCompleted =
    learningPath &&
    (learningPath.completionPercentage >= 100 ||
      (learningPath.totalSteps > 0 &&
        learningPath.completedSteps >= learningPath.totalSteps));

  return (
    <div className="space-y-6 pb-12">
      {/* Header Bar */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="p-2.5 rounded-xl bg-primary-light text-primary shadow-2xs">
              <GraduationCap className="h-6 w-6" />
            </span>
            <div>
              <h1 className="text-2xl font-bold text-foreground">
                Lộ trình Đào tạo & Bổ sung Năng lực
              </h1>
              <p className="text-xs text-muted mt-0.5">
                Theo dõi tiến độ học tập cá nhân hóa, đo lường mức độ sẵn sàng và xóa bỏ khoảng thiếu hụt kỹ năng
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              refetchGap();
              refetchSuggestions();
              refetchPath();
            }}
            isLoading={gapRefetching || suggestionsLoading || pathRefetching}
            leftIcon={
              <RefreshCw
                className={`h-4 w-4 ${
                  gapRefetching || suggestionsLoading || pathRefetching ? "animate-spin" : ""
                }`}
              />
            }
          >
            Làm mới toàn bộ
          </Button>

          <Button
            variant="primary"
            size="sm"
            onClick={() => navigate("/candidate/jobs")}
            rightIcon={<ArrowRight className="h-4 w-4" />}
          >
            Tìm việc làm phù hợp
          </Button>
        </div>
      </div>

      {/* Tabs Switcher */}
      <div className="flex border-b border-border gap-2 sm:gap-6">
        <button
          type="button"
          onClick={() => handleTabChange("path")}
          className={`pb-3 px-2 sm:px-4 text-sm font-bold border-b-2 transition-all flex items-center gap-2 cursor-pointer ${
            activeTab === "path"
              ? "border-primary text-primary"
              : "border-transparent text-muted hover:text-foreground"
          }`}
        >
          <Route className="h-4 w-4" />
          <span>Lộ trình của tôi</span>
          {learningPath && (
            <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-100 text-emerald-800">
              {Math.round(learningPath.completionPercentage)}%
            </span>
          )}
        </button>

        <button
          type="button"
          onClick={() => handleTabChange("gap")}
          className={`pb-3 px-2 sm:px-4 text-sm font-bold border-b-2 transition-all flex items-center gap-2 cursor-pointer ${
            activeTab === "gap"
              ? "border-primary text-primary"
              : "border-transparent text-muted hover:text-foreground"
          }`}
        >
          <Target className="h-4 w-4" />
          <span>Khoảng thiếu hụt & Gợi ý môn học</span>
          {gapAnalysis && (
            <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-slate-100 text-slate-700">
              {gapAnalysis.items.length} kỹ năng
            </span>
          )}
        </button>
      </div>

      {/* Loading Skeleton */}
      {isInitialLoading && (
        <div className="space-y-4 animate-pulse">
          <div className="h-44 bg-slate-200/80 rounded-xl" />
          <div className="h-56 bg-slate-200/80 rounded-xl" />
          <div className="h-64 bg-slate-200/80 rounded-xl" />
        </div>
      )}

      {!isInitialLoading && (
        <>
          {/* ========================================================================= */}
          {/* TAB 1: LỘ TRÌNH CỦA TÔI (DT-03) */}
          {/* ========================================================================= */}
          {activeTab === "path" && (
            <div className="space-y-6">
              {/* CASE 1: 100% SUCCESS BANNER */}
              {isPathCompleted && (
                <LearningPathSuccessBanner
                  targetName={learningPath.targetName}
                  onExploreJobs={() => navigate("/candidate/jobs")}
                />
              )}

              {/* CASE 2: LEARNING PATH EXISTS */}
              {learningPath ? (
                <>
                  {/* Action Bar for regenerating path */}
                  <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 border border-slate-200/80 text-xs">
                    <span className="text-muted">
                      Lộ trình hiện tại được tối ưu hóa dựa trên phân tích năng lực và khóa học LMS.
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleGeneratePath}
                      disabled={generatePathMutation.isPending}
                      isLoading={generatePathMutation.isPending}
                      leftIcon={<Sparkles className="h-3.5 w-3.5 text-amber-500" />}
                      className="text-xs"
                    >
                      Sinh lại lộ trình mới
                    </Button>
                  </div>

                  {/* 1. Circular Gauge & Growth Comparison */}
                  <ReadinessCircularGauge
                    targetName={learningPath.targetName}
                    targetType={learningPath.targetType}
                    readinessAtStart={learningPath.readinessAtStart}
                    currentReadiness={learningPath.currentReadiness}
                    readinessGrowth={learningPath.readinessGrowth}
                    completionPercentage={learningPath.completionPercentage}
                    totalSteps={learningPath.totalSteps}
                    completedSteps={learningPath.completedSteps}
                    createdAt={learningPath.createdAt}
                  />

                  {/* 2. Pure SVG Radar Chart */}
                  <SkillRadarChart steps={learningPath.steps} />

                  {/* 3. Learning Path Timeline Stepper */}
                  <LearningPathTimeline
                    steps={learningPath.steps}
                    onEnroll={handleEnroll}
                    isEnrolling={enrollMutation.isPending}
                    enrollingCourseId={enrollingCourseId}
                    onRefresh={handleRefreshEnrollments}
                    isRefreshing={refreshMutation.isPending}
                  />
                </>
              ) : (
                /* CASE 3: NO LEARNING PATH CREATED YET (EMPTY STATE) */
                <Card className="border-dashed border-primary/30 bg-primary-50/20 shadow-xs">
                  <CardContent className="p-8 sm:p-12 text-center space-y-4">
                    <div className="mx-auto w-16 h-16 rounded-2xl bg-primary-light text-primary flex items-center justify-center shadow-inner">
                      <Sparkles className="h-8 w-8 text-primary" />
                    </div>

                    <div className="max-w-lg mx-auto">
                      <h3 className="text-lg font-bold text-foreground">
                        Bạn chưa có lộ trình học tập bổ sung
                      </h3>
                      <p className="text-xs text-muted mt-2 leading-relaxed">
                        Hệ thống sẽ tổng hợp khoảng thiếu hụt kỹ năng (DT-01) và khóa học gợi ý LMS (DT-02), sắp xếp các bước học theo thứ tự tối ưu (kỹ năng bắt buộc trước, thiếu nhiều trước) để bạn từng bước chinh phục mục tiêu!
                      </p>
                    </div>

                    <div className="pt-2">
                      <Button
                        variant="primary"
                        size="md"
                        onClick={handleGeneratePath}
                        disabled={generatePathMutation.isPending}
                        isLoading={generatePathMutation.isPending}
                        leftIcon={<PlusCircle className="h-4 w-4" />}
                      >
                        Tạo lộ trình học tập tối ưu ngay
                      </Button>
                    </div>

                    {!myTarget && (
                      <p className="text-[11px] text-amber-700 bg-amber-50 p-2.5 rounded-lg border border-amber-200/80 max-w-md mx-auto">
                        <strong>Lưu ý:</strong> Bạn cần thiết lập mục tiêu nghề nghiệp trước. Hãy chuyển sang tab <strong>"Khoảng thiếu hụt & Gợi ý môn học"</strong> để chọn mục tiêu.
                      </p>
                    )}
                  </CardContent>
                </Card>
              )}
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 2: KHOẢNG THIẾU HỤT & GỢI Ý MÔN HỌC (DT-01 & DT-02) */}
          {/* ========================================================================= */}
          {activeTab === "gap" && (
            <div className="space-y-6">
              {/* Job Override Banner if active */}
              {jobIdOverride && gapAnalysis?.target && (
                <JobAnalysisBanner
                  target={gapAnalysis.target}
                  isSavingTarget={setTargetMutation.isPending}
                  onSaveAsMyTarget={handleSaveOverrideAsMainTarget}
                  onClearOverride={handleClearOverride}
                />
              )}

              {/* 1. Readiness Gauge & Summary Card */}
              {gapAnalysis ? (
                <ReadinessGaugeCard
                  target={gapAnalysis.target}
                  readiness={gapAnalysis.readiness}
                  totalRequiredSkills={gapAnalysis.totalRequiredSkills}
                  passedSkillsCount={gapAnalysis.passedSkillsCount}
                  missingSkillsCount={gapAnalysis.missingSkillsCount}
                  computedAt={gapAnalysis.computedAt}
                />
              ) : (
                <Card className="border-dashed border-amber-300 bg-amber-50/40 shadow-xs">
                  <CardContent className="p-6 text-center space-y-3">
                    <div className="mx-auto w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center text-amber-700">
                      <Compass className="h-6 w-6" />
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-slate-900">
                        Bạn chưa thiết lập mục tiêu nghề nghiệp
                      </h3>
                      <p className="text-xs text-slate-600 max-w-md mx-auto mt-1">
                        Hãy chọn một trong các vị trí mục tiêu bên dưới để hệ thống đối chiếu hồ sơ và tính toán khoảng thiếu hụt năng lực của bạn!
                      </p>
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* 2. Skill Gap Table */}
              {gapAnalysis && <SkillGapTable items={gapAnalysis.items} />}

              {/* 3. Course Suggestions Section (DT-02) */}
              <CourseSuggestionsSection
                suggestions={suggestionsRes?.data || []}
                isLoading={suggestionsLoading}
                onEnroll={handleEnroll}
                onRefresh={handleRefreshEnrollments}
                isRefreshing={refreshMutation.isPending}
                enrollingCourseId={enrollingCourseId}
              />

              {/* 4. Target Selector Card */}
              <TargetSelectorCard
                positions={positions}
                currentPositionId={myTarget?.positionId}
                isSettingTarget={setTargetMutation.isPending}
                onSelectPosition={handleSelectPosition}
                onSelectJob={handleSelectJob}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default CandidateLearningPage;
