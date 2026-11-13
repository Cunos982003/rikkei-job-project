import React, { useState } from "react";
import { Check, Compass, Briefcase, Code, Laptop, ShieldCheck, Layers } from "lucide-react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "../../../components/common/Card";
import { Button } from "../../../components/common/Button";
import type { TargetPosition } from "../../../api/endpoints/learning.api";

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

  // Sample jobs from mock server for quick switching
  const sampleJobs = [
    {
      id: "job-001",
      title: "Java Backend Developer (Fresher / Junior)",
      company: "Công ty Cổ phần Rikkeisoft",
      skills: ["Java Core", "Spring Boot", "SQL", "REST API", "Git"],
    },
    {
      id: "job-002",
      title: "Frontend ReactJS Developer",
      company: "Rikkei Digital Solutions",
      skills: ["ReactJS", "TypeScript", "HTML/CSS", "Git"],
    },
    {
      id: "job-003",
      title: "QA / Automation Tester",
      company: "Rikkei QA Center",
      skills: ["Manual Testing", "SQL", "Selenium", "Postman"],
    },
  ];

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
              Chọn nhanh một tin tuyển dụng mẫu từ hệ sinh thái Rikkei Jobs để phân tích hoặc làm mục tiêu:
            </p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {sampleJobs.map((job) => (
                <div
                  key={job.id}
                  className="p-3.5 rounded-xl border border-border bg-surface hover:border-slate-300 hover:shadow-xs transition-all flex flex-col justify-between"
                >
                  <div className="space-y-1.5">
                    <div className="flex items-center gap-1.5 text-primary text-xs font-semibold">
                      <Briefcase className="h-3.5 w-3.5" />
                      <span>{job.id}</span>
                    </div>
                    <h4 className="text-sm font-bold text-foreground leading-tight">
                      {job.title}
                    </h4>
                    <p className="text-xs text-muted">{job.company}</p>
                    <div className="flex flex-wrap gap-1 pt-1">
                      {job.skills.map((s) => (
                        <span key={s} className="text-[10px] px-1.5 py-0.5 bg-slate-100 text-slate-700 rounded">
                          {s}
                        </span>
                      ))}
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
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
