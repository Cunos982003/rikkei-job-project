import React, { useState, useMemo } from "react";
import { Check, Compass, Briefcase, Code, Laptop, ShieldCheck, Layers, Loader2, AlertCircle } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../../components/common/Card";
import { Button } from "../../../components/common/Button";
import type { TargetPosition } from "../../../api/endpoints/learning.api";
import { jobsApi, type JobPostingItem } from "../../../api/endpoints/jobs.api";
import { candidateKeys } from "../../../api/queryKeys";

interface TargetSelectorCardProps {
  positions: TargetPosition[];
  currentPositionId?: string | null;
  isSettingTarget: boolean;
  onSelectPosition: (positionId: string) => void;
  onSelectJob?: (jobId: string) => void;
}

export const TargetSelectorCard: React.FC<TargetSelectorCardProps> = ({
  positions,
  currentPositionId,
  isSettingTarget,
  onSelectPosition,
  onSelectJob,
}) => {
  const [activeTab, setActiveTab] = useState<"positions" | "jobs">("positions");

  const getPositionIcon = (code: string) => {
    switch (code) {
      case "JAVA_BACKEND_FRESHER":
        return <Code className="h-5 w-5 text-red-600" />;
      case "FRONTEND_REACT_FRESHER":
        return <Laptop className="h-5 w-5 text-blue-600" />;
      case "TESTER":
        return <ShieldCheck className="h-5 w-5 text-emerald-600" />;
      case "FULLSTACK":
        return <Layers className="h-5 w-5 text-purple-600" />;
      default:
        return <Compass className="h-5 w-5 text-primary" />;
    }
  };

  // Fetch real jobs from backend
  const { data: realJobsRes, isLoading: isJobsLoading } = useQuery({
    queryKey: candidateKeys.jobs.list({ status: "PUBLISHED", size: 6 }),
    queryFn: async () => {
      try {
        const res = await jobsApi.getJobs({ status: "PUBLISHED", size: 6 });
        return res;
      } catch {
        return null;
      }
    },
    enabled: activeTab === "jobs",
    staleTime: 5 * 60 * 1000,
  });

  const realJobs: JobPostingItem[] = useMemo(() => {
    if (!realJobsRes?.data) return [];
    if (Array.isArray(realJobsRes.data)) {
      return realJobsRes.data;
    }
    if ("items" in realJobsRes.data && Array.isArray((realJobsRes.data as any).items)) {
      return (realJobsRes.data as any).items;
    }
    return [];
  }, [realJobsRes]);

  return (
    <Card className="border-border shadow-xs">
      <CardHeader className="pb-3 border-b border-border/60">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <CardTitle className="text-base font-bold text-foreground flex items-center gap-2">
              <Compass className="h-5 w-5 text-primary" />
              <span>Thiết lập Mục tiêu Nghề nghiệp</span>
            </CardTitle>
            <CardDescription className="text-xs text-muted mt-0.5">
              Chọn một vị trí hoặc tin tuyển dụng để hệ thống tự động phân tích khoảng thiếu hụt năng lực và gợi ý lộ trình bồi dưỡng
            </CardDescription>
          </div>

          {/* Switch tabs */}
          <div className="flex rounded-lg bg-slate-100 p-0.5 self-start sm:self-auto">
            <button
              type="button"
              onClick={() => setActiveTab("positions")}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${
                activeTab === "positions"
                  ? "bg-white text-foreground shadow-xs"
                  : "text-muted hover:text-foreground"
              }`}
            >
              Vị trí chuẩn hóa (4 lộ trình)
            </button>
            <button
              type="button"
              onClick={() => setActiveTab("jobs")}
              className={`px-3 py-1 text-xs font-semibold rounded-md transition-all ${
                activeTab === "jobs"
                  ? "bg-white text-foreground shadow-xs"
                  : "text-muted hover:text-foreground"
              }`}
            >
              Tin tuyển dụng (JD)
            </button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="p-5 pt-4">
        {activeTab === "positions" ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
            {positions.map((pos) => {
              const isSelected = currentPositionId === pos.id;

              return (
                <div
                  key={pos.id}
                  className={`p-4 rounded-xl border transition-all flex flex-col justify-between ${
                    isSelected
                      ? "border-primary bg-primary-light/40 ring-1 ring-primary/30"
                      : "border-border hover:border-slate-300 hover:shadow-xs bg-surface"
                  }`}
                >
                  <div className="space-y-2">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex items-center gap-2.5">
                        <div className="p-2 rounded-lg bg-slate-100 shrink-0">
                          {getPositionIcon(pos.code)}
                        </div>
                        <div>
                          <h4 className="text-sm font-bold text-foreground leading-tight">
                            {pos.name}
                          </h4>
                          <span className="text-[11px] font-mono text-muted">
                            {pos.code}
                          </span>
                        </div>
                      </div>

                      {isSelected && (
                        <span className="inline-flex items-center gap-1 text-[11px] font-bold text-primary bg-primary-100 px-2 py-0.5 rounded-full shrink-0">
                          <Check className="h-3 w-3" />
                          Đang chọn
                        </span>
                      )}
                    </div>

                    <p className="text-xs text-muted leading-relaxed line-clamp-2">
                      {pos.description}
                    </p>

                    {/* Skill preview tags */}
                    <div className="pt-2 flex flex-wrap gap-1.5">
                      {pos.skills?.map((sk) => (
                        <span
                          key={sk.skillId}
                          className={`text-[10px] font-medium px-2 py-0.5 rounded-md ${
                            sk.isMandatory
                              ? "bg-slate-100 text-slate-800 border border-slate-200"
                              : "bg-slate-50 text-slate-500"
                          }`}
                        >
                          {sk.skillName} (≥{sk.minScore})
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="pt-3 mt-3 border-t border-border/50 flex justify-end">
                    <Button
                      variant={isSelected ? "outline" : "primary"}
                      size="sm"
                      disabled={isSelected || isSettingTarget}
                      isLoading={isSettingTarget && isSelected}
                      onClick={() => onSelectPosition(pos.id)}
                      className="w-full sm:w-auto text-xs"
                      leftIcon={isSelected ? <Check className="h-3.5 w-3.5" /> : undefined}
                    >
                      {isSelected ? "Đang là mục tiêu hiện tại" : "Chọn làm mục tiêu"}
                    </Button>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-xs text-muted mb-2">
              Chọn một tin tuyển dụng đang mở từ Rikkei Jobs để hệ thống tự động phân tích khoảng thiếu hụt năng lực theo JD:
            </p>
            {isJobsLoading ? (
              <div className="flex items-center justify-center p-8 gap-2 text-xs text-muted">
                <Loader2 className="h-4 w-4 animate-spin text-primary" />
                <span>Đang tải danh sách tin tuyển dụng...</span>
              </div>
            ) : realJobs.length === 0 ? (
              <div className="flex flex-col items-center justify-center p-8 border border-dashed border-border rounded-xl text-center">
                <AlertCircle className="h-8 w-8 text-muted mb-2" />
                <p className="text-sm font-semibold text-foreground">Chưa có tin tuyển dụng nào</p>
                <p className="text-xs text-muted mt-1">Vui lòng quay lại tab Vị trí chuẩn hóa để chọn lộ trình nghề nghiệp.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                {realJobs.map((job) => {
                  const skills = (job.jobSkills && job.jobSkills.length > 0)
                    ? job.jobSkills.map((s) => s.skillName)
                    : (job.requiredSkills || []);

                  return (
                    <div
                      key={job.id}
                      className="p-3.5 rounded-xl border border-border bg-surface hover:border-slate-300 hover:shadow-xs transition-all flex flex-col justify-between"
                    >
                      <div className="space-y-1.5">
                        <div className="flex items-center gap-1.5 text-primary text-xs font-semibold">
                          <Briefcase className="h-3.5 w-3.5" />
                          <span>{job.id}</span>
                        </div>
                        <h4 className="text-sm font-bold text-foreground leading-tight line-clamp-1">
                          {job.title}
                        </h4>
                        <p className="text-xs text-muted line-clamp-1">{job.companyName || "Doanh nghiệp tuyển dụng"}</p>
                        <div className="flex flex-wrap gap-1 pt-1">
                          {skills.slice(0, 4).map((s) => (
                            <span key={s} className="text-[10px] px-1.5 py-0.5 bg-slate-100 text-slate-700 rounded">
                              {s}
                            </span>
                          ))}
                          {skills.length > 4 && (
                            <span className="text-[10px] px-1.5 py-0.5 bg-slate-50 text-slate-500 rounded">
                              +{skills.length - 4}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="pt-3 mt-2 border-t border-border/50 flex justify-end">
                        <Button
                          variant="outline"
                          size="sm"
                          className="w-full text-xs"
                          onClick={() => onSelectJob?.(job.id)}
                        >
                          Phân tích theo JD này
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
