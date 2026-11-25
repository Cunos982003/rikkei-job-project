import React, { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  X,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  GraduationCap,
  ExternalLink,
  Send,
  Loader2,
  TrendingUp,
  Award,
} from "lucide-react";
import { learningApi, type GapAnalysisResult } from "../../../api/endpoints/learning.api";
import { profileApi, type SkillProfileItem } from "../../../api/endpoints/profile.api";
import { candidateKeys } from "../../../api/queryKeys";
import { useAuthStore } from "../../../store/useAuthStore";
import { Button } from "../../../components/common/Button";
import { Badge } from "../../../components/common/Badge";
import { cn } from "../../../utils/cn";

interface QuickCompetenceDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  jobId: string;
  jobTitle: string;
  companyName: string;
  jobSkills?: { skillId?: string; skillName: string; minScore?: number; isMandatory?: boolean }[];
  requiredSkills?: string[];
  onOpenApply?: () => void;
  isApplied?: boolean;
}

export const QuickCompetenceDrawer: React.FC<QuickCompetenceDrawerProps> = ({
  isOpen,
  onClose,
  jobId,
  jobTitle,
  companyName,
  jobSkills,
  requiredSkills,
  onOpenApply,
  isApplied,
}) => {
  const { user } = useAuthStore();

  // Fetch gap analysis data dynamically from learning-service
  const { data: gapRes, isLoading } = useQuery({
    queryKey: candidateKeys.learning.quickGap(jobId),
    queryFn: () => learningApi.getGapAnalysis(jobId).catch(() => null),
    enabled: isOpen && Boolean(jobId),
    staleTime: 60 * 1000,
    retry: false,
  });

  // Lấy danh sách kỹ năng thực tế của ứng viên từ profile-service
  const { data: mySkillsRes } = useQuery({
    queryKey: candidateKeys.skills.byUser(user?.id),
    queryFn: async () => {
      try {
        const res = await profileApi.getMySkills();
        return res;
      } catch {
        return null;
      }
    },
    enabled: isOpen && Boolean(user?.id),
    staleTime: 5 * 60 * 1000,
  });

  const candidateSkillsMap = useMemo(() => {
    const map = new Map<string, number>();
    const skills: SkillProfileItem[] = mySkillsRes?.data || [];
    for (const s of skills) {
      const score = s.verifiedScore ?? s.selfScore ?? 0;
      if (s.skillName) {
        map.set(s.skillName.toLowerCase().trim(), score);
      }
      if (s.skillCode) {
        map.set(s.skillCode.toLowerCase().trim(), score);
      }
      if (s.skillId) {
        map.set(s.skillId.toLowerCase().trim(), score);
      }
    }
    return map;
  }, [mySkillsRes]);

  // Phân tích năng lực đối chiếu điểm thực tế của ứng viên với yêu cầu của tin tuyển dụng
  const fallbackAnalysis: GapAnalysisResult = useMemo(() => {
    const rawSkills =
      jobSkills && jobSkills.length > 0
        ? jobSkills
        : (requiredSkills && requiredSkills.length > 0
            ? requiredSkills
            : ["Java Core", "Spring Boot", "SQL"]
          ).map((s, idx) => ({
            skillId: `SKL_${idx + 1}`,
            skillName: s,
            minScore: 70,
            isMandatory: idx < 2,
          }));

    const items = rawSkills.map((s, idx) => {
      const required = s.minScore || 70;
      const sName = (s.skillName || "").toLowerCase().trim();
      const sId = (s.skillId || "").toLowerCase().trim();

      // Tra cứu điểm kỹ năng thật của ứng viên
      let current = 0;
      if (candidateSkillsMap.has(sName)) {
        current = candidateSkillsMap.get(sName)!;
      } else if (candidateSkillsMap.has(sId)) {
        current = candidateSkillsMap.get(sId)!;
      } else {
        for (const [k, v] of candidateSkillsMap.entries()) {
          if (sName.includes(k) || k.includes(sName)) {
            current = Math.max(current, v);
          }
        }
      }

      const gap = Math.max(0, required - current);
      return {
        skillId: s.skillId || `SKL_${idx + 1}`,
        skill: s.skillName,
        skillName: s.skillName,
        required,
        current,
        gap,
        priority: (gap > 15 ? "HIGH" : gap > 0 ? "MEDIUM" : "LOW") as "HIGH" | "MEDIUM" | "LOW",
        isMandatory: Boolean(s.isMandatory),
      };
    });

    const passedCount = items.filter((i) => i.gap <= 0).length;
    const readiness = items.length > 0 ? Math.round((passedCount / items.length) * 100) : 100;

    return {
      target: {
        type: "JOB",
        id: jobId,
        code: "JOB",
        name: jobTitle,
        description: "",
      },
      readiness,
      totalRequiredSkills: items.length,
      passedSkillsCount: passedCount,
      missingSkillsCount: items.length - passedCount,
      computedAt: new Date().toISOString(),
      items,
    };
  }, [jobSkills, requiredSkills, jobId, jobTitle, candidateSkillsMap]);

  const analysis: GapAnalysisResult = gapRes?.data || fallbackAnalysis;
  const readiness = analysis?.readiness ?? 0;

  if (!isOpen) return null;

  // Đánh giá mức độ phù hợp
  const getReadinessLevel = (score: number) => {
    if (score >= 80) return { label: "Rất phù hợp", color: "text-emerald-600 bg-emerald-50 border-emerald-200" };
    if (score >= 60) return { label: "Khá phù hợp", color: "text-blue-600 bg-blue-50 border-blue-200" };
    return { label: "Cần bổ sung kỹ năng", color: "text-amber-600 bg-amber-50 border-amber-200" };
  };

  const readinessInfo = getReadinessLevel(readiness);

  // Phân chia kỹ năng đạt vs còn thiếu
  const passedItems = analysis?.items?.filter((item) => item.gap <= 0) ?? [];
  const missingItems = analysis?.items?.filter((item) => item.gap > 0) ?? [];

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* 1. Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs transition-opacity duration-300"
        onClick={onClose}
      />

      {/* 2. Slide Drawer Panel */}
      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md sm:max-w-lg bg-surface border-l border-border shadow-2xl flex flex-col z-10 animate-in slide-in-from-right duration-300">
          {/* Header */}
          <div className="p-4 sm:p-5 border-b border-border flex items-center justify-between bg-slate-50/70">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-primary text-white flex items-center justify-center shadow-xs">
                <Sparkles className="h-4 w-4" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-foreground">
                  Phân tích Năng lực Nhanh
                </h2>
                <p className="text-[11px] text-muted line-clamp-1 max-w-[280px]">
                  {jobTitle} · {companyName}
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={onClose}
              aria-label="Đóng bảng phân tích"
              className="p-1.5 text-muted hover:text-foreground rounded-lg transition-colors cursor-pointer"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Body Content */}
          <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-6">
            {isLoading ? (
              <div className="py-20 flex flex-col items-center justify-center space-y-3 text-muted">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
                <p className="text-xs">Đang đối chiếu năng lực với yêu cầu JD...</p>
              </div>
            ) : !analysis ? (
              <div className="py-12 text-center space-y-3">
                <AlertTriangle className="h-8 w-8 text-amber-500 mx-auto" />
                <p className="text-xs text-muted">
                  Không thể tải dữ liệu phân tích năng lực cho công việc này.
                </p>
              </div>
            ) : (
              <>
                {/* Score Banner */}
                <div className="bg-gradient-to-br from-slate-900 to-indigo-950 rounded-2xl p-5 text-white shadow-md relative overflow-hidden">
                  <div className="relative z-10 flex items-center justify-between gap-4">
                    <div>
                      <span className="text-[11px] font-semibold text-emerald-400 uppercase tracking-wider">
                        Độ tương thích năng lực
                      </span>
                      <div className="flex items-baseline gap-2 mt-1">
                        <span className="text-4xl font-extrabold tracking-tight">
                          {readiness}%
                        </span>
                        <span className="text-xs text-slate-300">sẵn sàng</span>
                      </div>
                      <div className="mt-2">
                        <span
                          className={cn(
                            "inline-block px-2.5 py-0.5 rounded-full text-[11px] font-semibold border",
                            readinessInfo.color
                          )}
                        >
                          {readinessInfo.label}
                        </span>
                      </div>
                    </div>

                    <div className="w-16 h-16 rounded-full border-4 border-emerald-400/30 flex items-center justify-center bg-white/5 backdrop-blur-xs">
                      <TrendingUp className="h-7 w-7 text-emerald-400" />
                    </div>
                  </div>

                  <p className="text-[11px] text-slate-300/90 mt-3 pt-3 border-t border-white/10 leading-relaxed">
                    Dữ liệu được đối chiếu tự động từ kết quả bài thi và chứng chỉ kỹ năng đã xác thực trên hệ thống Rikkei LMS.
                  </p>
                </div>

                {/* Stat row */}
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 rounded-xl bg-emerald-50/70 border border-emerald-200/80 space-y-1">
                    <div className="flex items-center gap-1.5 text-emerald-800 text-xs font-semibold">
                      <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                      <span>Kỹ năng đã đạt</span>
                    </div>
                    <p className="text-xl font-extrabold text-emerald-900">
                      {passedItems.length}
                      <span className="text-xs font-normal text-emerald-700 ml-1">
                        / {analysis.items.length}
                      </span>
                    </p>
                  </div>

                  <div className="p-3 rounded-xl bg-amber-50/70 border border-amber-200/80 space-y-1">
                    <div className="flex items-center gap-1.5 text-amber-800 text-xs font-semibold">
                      <AlertTriangle className="h-3.5 w-3.5 text-amber-600" />
                      <span>Cần bổ sung</span>
                    </div>
                    <p className="text-xl font-extrabold text-amber-900">
                      {missingItems.length}
                      <span className="text-xs font-normal text-amber-700 ml-1">kỹ năng</span>
                    </p>
                  </div>
                </div>

                {/* 1. Kỹ năng đạt yêu cầu */}
                <div className="space-y-2.5">
                  <h3 className="text-xs font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                    <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                    <span>Kỹ năng đạt yêu cầu ({passedItems.length})</span>
                  </h3>

                  {passedItems.length === 0 ? (
                    <p className="text-xs text-muted italic">Chưa có kỹ năng nào đạt yêu cầu tối thiểu của JD.</p>
                  ) : (
                    <div className="space-y-2">
                      {passedItems.map((item) => (
                        <div
                          key={item.skillId}
                          className="p-3 rounded-xl border border-emerald-200/70 bg-emerald-50/30 flex items-center justify-between text-xs"
                        >
                          <div>
                            <span className="font-semibold text-foreground">
                              {item.skillName || item.skill}
                            </span>
                            {item.isMandatory && (
                              <span className="ml-1.5 text-[10px] text-danger font-medium">
                                *Bắt buộc
                              </span>
                            )}
                          </div>
                          <div className="flex items-center gap-2">
                            <span className="text-slate-500">
                              Điểm: <strong className="text-emerald-700">{item.current}</strong> / {item.required}
                            </span>
                            <Badge variant="verified" size="sm">
                              Đạt chuẩn
                            </Badge>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* 2. Khoảng thiếu hụt cần cải thiện */}
                {missingItems.length > 0 && (
                  <div className="space-y-2.5">
                    <h3 className="text-xs font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                      <AlertTriangle className="h-4 w-4 text-amber-600" />
                      <span>Khoảng thiếu hụt cần bù đắp ({missingItems.length})</span>
                    </h3>

                    <div className="space-y-2">
                      {missingItems.map((item) => (
                        <div
                          key={item.skillId}
                          className="p-3 rounded-xl border border-amber-200/80 bg-amber-50/30 space-y-1.5 text-xs"
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-semibold text-foreground">
                              {item.skillName || item.skill}
                            </span>
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                              Thiếu {item.gap} điểm
                            </span>
                          </div>

                          <div className="flex items-center justify-between text-muted text-[11px]">
                            <span>Hiện tại: {item.current} điểm</span>
                            <span>Yêu cầu JD: {item.required} điểm</span>
                          </div>

                          {/* Progress bar so sánh */}
                          <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden">
                            <div
                              className="bg-amber-500 h-1.5 rounded-full"
                              style={{ width: `${Math.min(100, Math.round((item.current / (item.required || 1)) * 100))}%` }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 3. Khóa học LMS ngắn hạn gợi ý */}
                <div className="space-y-2.5">
                  <h3 className="text-xs font-bold text-foreground uppercase tracking-wider flex items-center gap-1.5">
                    <GraduationCap className="h-4 w-4 text-primary" />
                    <span>Khóa học ngắn hạn LMS gợi ý</span>
                  </h3>

                  <div className="p-3.5 rounded-xl border border-border bg-slate-50 space-y-2 text-xs">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="font-bold text-foreground">
                          Lập trình Java Web & Spring Boot Chuyên Sâu
                        </p>
                        <p className="text-[11px] text-muted mt-0.5">
                          Học viện Rikkei Academy · Thời lượng 12 buổi thực hành
                        </p>
                      </div>
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800 shrink-0">
                        LMS Online
                      </span>
                    </div>

                    <p className="text-[11px] text-slate-600 leading-relaxed">
                      Bổ sung kiến thức Spring Data JPA, Microservices và REST API theo đúng chuẩn dự án doanh nghiệp.
                    </p>

                    <div className="pt-1 flex items-center justify-between">
                      <span className="text-[11px] text-emerald-700 font-semibold flex items-center gap-1">
                        <Award className="h-3.5 w-3.5" />
                        Tự động cấp chứng nhận sau khi hoàn thành
                      </span>
                      <a
                        href="/candidate/learning"
                        className="inline-flex items-center gap-1 text-[11px] font-semibold text-primary hover:underline"
                      >
                        <span>Vào khóa học</span>
                        <ExternalLink className="h-3 w-3" />
                      </a>
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Footer Action */}
          <div className="p-4 border-t border-border bg-surface flex items-center justify-between gap-3">
            <Button variant="outline" size="sm" onClick={onClose}>
              Đóng
            </Button>

            {onOpenApply && (
              isApplied ? (
                <div className="inline-flex items-center gap-1 text-xs font-medium text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-lg border border-emerald-200">
                  <CheckCircle2 className="h-4 w-4" />
                  Đã nộp đơn cho vị trí này
                </div>
              ) : (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    onClose();
                    onOpenApply();
                  }}
                  leftIcon={<Send className="h-3.5 w-3.5" />}
                  className="shadow-xs"
                >
                  Nộp đơn ứng tuyển ngay
                </Button>
              )
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

