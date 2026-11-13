import React, { useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  GraduationCap,
  RefreshCw,
  Compass,
  ArrowRight,
} from "lucide-react";
import { useAuthStore } from "../../store/useAuthStore";
import { learningApi } from "../../api/endpoints/learning.api";
import { Button } from "../../components/common/Button";
import { Card, CardContent } from "../../components/common/Card";
import { ReadinessGaugeCard } from "../../features/learning/components/ReadinessGaugeCard";
import { SkillGapTable } from "../../features/learning/components/SkillGapTable";
import { TargetSelectorCard } from "../../features/learning/components/TargetSelectorCard";
import { JobAnalysisBanner } from "../../features/learning/components/JobAnalysisBanner";

export const CandidateLearningPage: React.FC = () => {
  const { isCandidate } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  const jobIdOverride = searchParams.get("jobId") || undefined;

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

  // 4. Mutation to set / change target
  const setTargetMutation = useMutation({
    mutationFn: (payload: { positionId?: string; jobId?: string }) =>
      learningApi.setMyTarget(payload),
    onSuccess: (res) => {
      toast.success(res.message || "Thiết lập mục tiêu nghề nghiệp thành công!");
      queryClient.invalidateQueries({ queryKey: ["myTarget"] });
      queryClient.invalidateQueries({ queryKey: ["gapAnalysis"] });
      if (jobIdOverride) {
        setSearchParams({});
      }
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || "Không thể cập nhật mục tiêu. Vui lòng thử lại sau";
      toast.error(msg);
    },
  });

  const positions = positionsRes?.data || [];
  const myTarget = myTargetRes?.data;
  const gapAnalysis = gapAnalysisRes?.data;
  const isInitialLoading = (positionsLoading || myTargetLoading || gapLoading) && !gapAnalysis;

  const handleSelectPosition = (positionId: string) => {
    setTargetMutation.mutate({ positionId });
  };

  const handleSelectJob = (jobId: string) => {
    setSearchParams({ jobId });
  };

  const handleClearOverride = () => {
    setSearchParams({});
  };

  const handleSaveOverrideAsMainTarget = () => {
    if (jobIdOverride) {
      setTargetMutation.mutate({ jobId: jobIdOverride });
    }
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header Bar */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-lg bg-primary-light text-primary">
              <GraduationCap className="h-6 w-6" />
            </span>
            <div>
              <h1 className="text-2xl font-bold text-foreground">
                Đào tạo Bổ sung & Phân tích Năng lực
              </h1>
              <p className="text-xs text-muted mt-0.5">
                Hệ thống so khớp khoảng thiếu hụt năng lực (Skill Gap) dựa trên hồ sơ đã kiểm chứng và mục tiêu nghề nghiệp
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2 self-start sm:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetchGap()}
            isLoading={gapRefetching}
            leftIcon={<RefreshCw className={`h-4 w-4 ${gapRefetching ? "animate-spin" : ""}`} />}
          >
            Làm mới phân tích
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
          {/* 1. Job Override Banner if active */}
          {jobIdOverride && gapAnalysis?.target && (
            <JobAnalysisBanner
              target={gapAnalysis.target}
              isSavingTarget={setTargetMutation.isPending}
              onSaveAsMyTarget={handleSaveOverrideAsMainTarget}
              onClearOverride={handleClearOverride}
            />
          )}

          {/* 2. Readiness Gauge & Summary Card */}
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
                    Hãy chọn một trong 4 lộ trình đào tạo chuẩn hóa bên dưới để hệ thống đối chiếu hồ sơ và tính toán khoảng thiếu hụt năng lực của bạn!
                  </p>
                </div>
              </CardContent>
            </Card>
          )}

          {/* 3. Skill Gap Table */}
          {gapAnalysis && (
            <SkillGapTable items={gapAnalysis.items} />
          )}

          {/* 4. Target Selector Card */}
          <TargetSelectorCard
            positions={positions}
            currentPositionId={myTarget?.positionId}
            isSettingTarget={setTargetMutation.isPending}
            onSelectPosition={handleSelectPosition}
            onSelectJob={handleSelectJob}
          />
        </>
      )}
    </div>
  );
};

export default CandidateLearningPage;
