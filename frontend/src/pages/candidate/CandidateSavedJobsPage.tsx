import { useState, useMemo, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Heart,
  Search,
  Building2,
  MapPin,
  Clock,
  Sparkles,
  Send,
  CheckCircle2,
  ArrowLeft,
  Briefcase,
  Trash2,
  SlidersHorizontal,
  FolderHeart,
  ChevronRight,
} from "lucide-react";
import { toast } from "sonner";
import { candidateJobsApi, type SavedJobItem } from "../../api/endpoints/candidateJobs.api";
import { recruitmentApi } from "../../api/endpoints/recruitment.api";
import type { JobPostingItem } from "../../api/endpoints/jobs.api";
import { Button } from "../../components/common/Button";
import { Card, CardContent } from "../../components/common/Card";
import { QuickCompetenceDrawer } from "./components/QuickCompetenceDrawer";
import { ApplyJobModal } from "./components/ApplyJobModal";
import { cn } from "../../utils/cn";

// Bảng màu gradient nhẹ nhàng cho avatar công ty
const GRADIENT_PALETTES = [
  "from-blue-600 to-indigo-600",
  "from-emerald-600 to-teal-600",
  "from-violet-600 to-purple-600",
  "from-rose-600 to-pink-600",
  "from-amber-600 to-orange-600",
];

function getCompanyInitials(name?: string): string {
  if (!name) return "RJ";
  const words = name.trim().split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[words.length - 1][0]).toUpperCase();
}

function getPaletteByString(str: string): string {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return GRADIENT_PALETTES[Math.abs(hash) % GRADIENT_PALETTES.length];
}

function formatDate(isoString?: string): string {
  if (!isoString) return "";
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return "";
    return d.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit", year: "numeric" });
  } catch {
    return "";
  }
}

type SortOption = "NEWEST" | "SALARY_DESC" | "EXPIRING_SOON";

export function CandidateSavedJobsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState<SortOption>("NEWEST");
  const [analyzingJob, setAnalyzingJob] = useState<SavedJobItem | null>(null);
  const [applyingJob, setApplyingJob] = useState<SavedJobItem | null>(null);

  // 1. Lấy danh sách việc làm đã lưu từ mock/backend
  const { data: savedJobsRes, isLoading } = useQuery({
    queryKey: ["savedJobs"],
    queryFn: () =>
      candidateJobsApi.getSavedJobs().catch(() => ({
        success: true,
        statusCode: 200,
        message: "",
        data: [] as SavedJobItem[],
      })),
    staleTime: 5 * 60 * 1000,
  });

  const rawSavedJobs: SavedJobItem[] = useMemo(() => {
    if (Array.isArray(savedJobsRes?.data)) return savedJobsRes.data;
    return [];
  }, [savedJobsRes]);

  // 2. Lấy danh sách các đơn đã nộp để hiển thị trạng thái "Đã nộp đơn"
  const { data: recruitmentsRes } = useQuery({
    queryKey: ["my-recruitments"],
    queryFn: () => recruitmentApi.getMyRecruitments().catch(() => ({ data: [] } as any)),
    staleTime: 5 * 60 * 1000,
  });

  const appliedJobIds = useMemo(() => {
    const set = new Set<string>();
    const list = (recruitmentsRes as any)?.data ?? recruitmentsRes;
    if (Array.isArray(list)) {
      list.forEach((r: any) => {
        if (r?.jobId) set.add(r.jobId);
      });
    }
    return set;
  }, [recruitmentsRes]);

  // 3. Mutation Bỏ lưu việc làm (Optimistic UI)
  const unsaveMutation = useMutation({
    mutationFn: (jobId: string) => candidateJobsApi.unsaveJob(jobId),
    onMutate: async (jobId: string) => {
      await queryClient.cancelQueries({ queryKey: ["savedJobs"] });
      const previous = queryClient.getQueryData(["savedJobs"]);

      queryClient.setQueryData(["savedJobs"], (old: any) => {
        const currentData = Array.isArray(old?.data) ? old.data : [];
        return {
          ...old,
          data: currentData.filter((j: any) => j.id !== jobId),
        };
      });

      return { previous };
    },
    onError: (_err, _jobId, context: any) => {
      if (context?.previous) {
        queryClient.setQueryData(["savedJobs"], context.previous);
      }
      toast.error("Không thể bỏ lưu việc làm. Vui lòng thử lại!");
    },
    onSuccess: () => {
      toast.info("Đã bỏ lưu việc làm khỏi danh sách theo dõi.");
      queryClient.invalidateQueries({ queryKey: ["savedJobs"] });
    },
  });

  // 4. Mutation Nộp đơn ứng tuyển nhanh từ danh sách đã lưu
  const applyMutation = useMutation({
    mutationFn: ({ coverLetter }: { coverLetter: string }) => {
      if (!applyingJob) throw new Error("Chưa chọn việc làm");
      return recruitmentApi.applyJob(applyingJob.id, { coverLetter: coverLetter || undefined });
    },
    onSuccess: () => {
      toast.success("Nộp đơn ứng tuyển thành công! Nhà tuyển dụng sẽ xem xét hồ sơ của bạn.");
      setApplyingJob(null);
      queryClient.invalidateQueries({ queryKey: ["my-recruitments"] });
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "Nộp đơn ứng tuyển thất bại. Vui lòng thử lại!");
    },
  });

  // 5. Định dạng mức lương
  const formatSalary = useCallback((min?: number, max?: number) => {
    if (!min && !max) return "Thoả thuận";
    if (min && max) {
      return `${(min / 1000000).toFixed(0)} - ${(max / 1000000).toFixed(0)} triệu`;
    }
    return `${((min || max)! / 1000000).toFixed(0)} triệu`;
  }, []);

  // 6. Lọc và sắp xếp danh sách
  const filteredAndSortedJobs = useMemo(() => {
    let result = [...rawSavedJobs];

    // Lọc theo từ khóa tìm kiếm
    if (keyword.trim()) {
      const q = keyword.toLowerCase().trim();
      result = result.filter(
        (j) =>
          j.title?.toLowerCase().includes(q) ||
          j.companyName?.toLowerCase().includes(q) ||
          j.location?.toLowerCase().includes(q) ||
          (Array.isArray(j.requiredSkills) && j.requiredSkills.some((s) => s.toLowerCase().includes(q)))
      );
    }

    // Sắp xếp
    result.sort((a, b) => {
      if (sortBy === "NEWEST") {
        const timeA = a.savedAt ? new Date(a.savedAt).getTime() : 0;
        const timeB = b.savedAt ? new Date(b.savedAt).getTime() : 0;
        return timeB - timeA;
      }
      if (sortBy === "SALARY_DESC") {
        const salaryA = a.salaryMax || a.salaryMin || 0;
        const salaryB = b.salaryMax || b.salaryMin || 0;
        return salaryB - salaryA;
      }
      if (sortBy === "EXPIRING_SOON") {
        const expA = a.expiresAt ? new Date(a.expiresAt).getTime() : Infinity;
        const expB = b.expiresAt ? new Date(b.expiresAt).getTime() : Infinity;
        return expA - expB;
      }
      return 0;
    });

    return result;
  }, [rawSavedJobs, keyword, sortBy]);

  return (
    <div className="max-w-7xl mx-auto py-4 sm:py-6 space-y-6 pb-16">
      {/* 1. Breadcrumbs & Header Section */}
      <div className="space-y-2">
        <div className="flex items-center gap-2 text-xs text-muted">
          <Link to="/candidate/jobs" className="hover:text-primary transition-colors flex items-center gap-1">
            <ArrowLeft className="h-3.5 w-3.5" />
            <span>Tìm việc làm</span>
          </Link>
          <span>/</span>
          <span className="text-foreground font-medium">Việc làm đã lưu</span>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pt-1">
          <div>
            <h1 className="text-xl sm:text-2xl font-black text-foreground flex items-center gap-2.5">
              <span className="p-2 rounded-2xl bg-rose-50 text-rose-600 border border-rose-200 shadow-xs inline-flex items-center justify-center">
                <Heart className="h-5 w-5 fill-rose-600" />
              </span>
              <span>Việc làm đã lưu</span>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-rose-100 text-rose-800 font-bold">
                {rawSavedJobs.length} việc làm
              </span>
            </h1>
            <p className="text-xs sm:text-sm text-muted mt-1">
              Quản lý các cơ hội nghề nghiệp bạn đã thả tim theo dõi. Bạn có thể phân tích năng lực hoặc nộp đơn ứng tuyển nhanh bất cứ lúc nào.
            </p>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate("/candidate/jobs")}
            leftIcon={<Briefcase className="h-4 w-4 text-primary" />}
            className="self-start sm:self-auto shrink-0"
          >
            Tìm thêm việc làm
          </Button>
        </div>
      </div>

      {/* 2. Search & Sort Control Toolbar */}
      {rawSavedJobs.length > 0 && (
        <div className="p-3 sm:p-4 rounded-2xl bg-surface border border-border shadow-xs flex flex-col sm:flex-row gap-3 items-stretch sm:items-center justify-between">
          <div className="relative flex-1 max-w-md">
            <Search className="h-4 w-4 text-muted absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm nhanh theo vị trí, công ty hoặc kỹ năng..."
              className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all placeholder:text-muted"
            />
            {keyword && (
              <button
                type="button"
                onClick={() => setKeyword("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-muted hover:text-foreground cursor-pointer px-1.5 py-0.5"
              >
                Xóa
              </button>
            )}
          </div>

          <div className="flex items-center gap-2 self-end sm:self-auto shrink-0">
            <span className="text-xs text-muted font-medium flex items-center gap-1.5 hidden sm:flex">
              <SlidersHorizontal className="h-3.5 w-3.5" />
              Sắp xếp:
            </span>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as SortOption)}
              className="text-xs px-3 py-2 rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all font-medium text-foreground cursor-pointer"
            >
              <option value="NEWEST">Mới lưu gần đây</option>
              <option value="SALARY_DESC">Lương cao nhất</option>
              <option value="EXPIRING_SOON">Hạn nộp gần nhất</option>
            </select>
          </div>
        </div>
      )}

      {/* 3. Main Content: List or Empty State */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 animate-pulse">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="h-48 rounded-2xl bg-slate-200" />
          ))}
        </div>
      ) : filteredAndSortedJobs.length === 0 ? (
        <Card className="border-dashed border-2 p-8 sm:p-14 text-center">
          <CardContent className="flex flex-col items-center justify-center space-y-4 max-w-md mx-auto">
            <div className="w-16 h-16 rounded-3xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-500 shadow-xs">
              <FolderHeart className="h-8 w-8" />
            </div>

            <div className="space-y-1.5">
              <h3 className="text-base sm:text-lg font-bold text-foreground">
                {keyword ? "Không tìm thấy công việc phù hợp" : "Bạn chưa lưu cơ hội việc làm nào"}
              </h3>
              <p className="text-xs sm:text-sm text-muted leading-relaxed">
                {keyword
                  ? `Không có tin tuyển dụng đã lưu nào khớp với từ khóa "${keyword}". Thử tìm với từ khóa khác.`
                  : "Khám phá hàng trăm tin tuyển dụng IT hấp dẫn từ các doanh nghiệp hàng đầu và bấm vào biểu tượng Trái tim để lưu lại theo dõi."}
              </p>
            </div>

            <div className="flex items-center gap-2 pt-2">
              {keyword ? (
                <Button variant="outline" size="sm" onClick={() => setKeyword("")}>
                  Xóa bộ lọc tìm kiếm
                </Button>
              ) : (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => navigate("/candidate/jobs")}
                  leftIcon={<Search className="h-4 w-4" />}
                >
                  Khám phá việc làm ngay
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {filteredAndSortedJobs.map((job) => {
            const isApplied = appliedJobIds.has(job.id);
            const initials = getCompanyInitials(job.companyName);
            const palette = getPaletteByString(job.companyName || job.id);
            const skills = Array.isArray(job.requiredSkills) ? job.requiredSkills.slice(0, 4) : [];

            return (
              <Card
                key={job.id}
                className="group hover:border-primary/40 hover:shadow-md transition-all duration-200 flex flex-col justify-between overflow-hidden bg-surface"
              >
                <CardContent className="p-4 sm:p-5 space-y-4">
                  {/* Top info row */}
                  <div className="flex items-start gap-3.5">
                    {/* Avatar công ty */}
                    <div
                      className={cn(
                        "w-12 h-12 rounded-2xl bg-gradient-to-br flex items-center justify-center text-white font-extrabold text-sm shadow-xs shrink-0 select-none",
                        palette
                      )}
                    >
                      {initials}
                    </div>

                    {/* Tiêu đề & Công ty */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <Link
                          to={`/candidate/jobs/${job.id}`}
                          className="font-bold text-sm sm:text-base text-foreground hover:text-primary transition-colors line-clamp-1 group-hover:underline"
                        >
                          {job.title}
                        </Link>

                        <button
                          type="button"
                          onClick={() => unsaveMutation.mutate(job.id)}
                          aria-label="Bỏ lưu việc làm"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-danger hover:bg-danger/10 transition-colors cursor-pointer shrink-0"
                          title="Bỏ lưu việc làm này"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>

                      <p className="text-xs text-muted font-medium line-clamp-1 mt-0.5 flex items-center gap-1.5">
                        <Building2 className="h-3 w-3 text-slate-400 shrink-0" />
                        <span>{job.companyName}</span>
                      </p>
                    </div>
                  </div>

                  {/* Highlights (Salary, Location, Date saved) */}
                  <div className="flex flex-wrap items-center gap-2 text-xs pt-1">
                    <span className="font-extrabold text-emerald-700 bg-emerald-50 border border-emerald-200/80 px-2.5 py-1 rounded-lg">
                      {formatSalary(job.salaryMin, job.salaryMax)}
                    </span>

                    <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700 font-medium">
                      <MapPin className="h-3 w-3 text-slate-500" />
                      <span className="truncate max-w-[150px]">{job.location || "Toàn quốc"}</span>
                    </span>

                    {job.savedAt && (
                      <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-rose-50/70 border border-rose-100 text-rose-700 font-medium text-[11px] ml-auto">
                        <Clock className="h-3 w-3 text-rose-500" />
                        <span>Đã lưu: {formatDate(job.savedAt)}</span>
                      </span>
                    )}
                  </div>

                  {/* Skills tags */}
                  {skills.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 pt-0.5">
                      {skills.map((skill) => (
                        <span
                          key={skill}
                          className="px-2 py-0.5 rounded-md text-[11px] font-medium bg-slate-100 text-slate-700"
                        >
                          {skill}
                        </span>
                      ))}
                      {job.requiredSkills && job.requiredSkills.length > 4 && (
                        <span className="px-1.5 py-0.5 rounded-md text-[10px] text-muted font-medium bg-slate-100">
                          +{job.requiredSkills.length - 4}
                        </span>
                      )}
                    </div>
                  )}

                  {/* Action buttons footer */}
                  <div className="pt-3 border-t border-border flex flex-wrap items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      {/* Nút phân tích năng lực nhanh tại chỗ */}
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setAnalyzingJob(job)}
                        leftIcon={<Sparkles className="h-3.5 w-3.5 text-primary" />}
                        className="text-xs"
                      >
                        Phân tích năng lực
                      </Button>

                      {/* Link chi tiết */}
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => navigate(`/candidate/jobs/${job.id}`)}
                        rightIcon={<ChevronRight className="h-3.5 w-3.5" />}
                        className="text-xs hidden sm:inline-flex"
                      >
                        Xem chi tiết
                      </Button>
                    </div>

                    {/* Nút ứng tuyển hoặc đã nộp */}
                    {isApplied ? (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold text-emerald-800 bg-emerald-100 border border-emerald-200">
                        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                        Đã nộp đơn
                      </span>
                    ) : (
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={() => setApplyingJob(job)}
                        leftIcon={<Send className="h-3.5 w-3.5" />}
                        className="text-xs"
                      >
                        Ứng tuyển ngay
                      </Button>
                    )}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* 4. Sliding Drawer Phân tích Năng lực Nhanh tại chỗ */}
      {analyzingJob && (
        <QuickCompetenceDrawer
          isOpen={Boolean(analyzingJob)}
          onClose={() => setAnalyzingJob(null)}
          jobId={analyzingJob.id}
          jobTitle={analyzingJob.title}
          companyName={analyzingJob.companyName}
          jobSkills={analyzingJob.jobSkills}
          requiredSkills={analyzingJob.requiredSkills}
          isApplied={appliedJobIds.has(analyzingJob.id)}
          onOpenApply={() => {
            const current = analyzingJob;
            setAnalyzingJob(null);
            setApplyingJob(current);
          }}
        />
      )}

      {/* 5. Modal Nộp đơn Ứng tuyển nhanh */}
      {applyingJob && (
        <ApplyJobModal
          isOpen={Boolean(applyingJob)}
          job={applyingJob as JobPostingItem}
          isSubmitting={applyMutation.isPending}
          onClose={() => setApplyingJob(null)}
          onSubmit={(_jobId, coverLetter) => applyMutation.mutate({ coverLetter })}
        />
      )}
    </div>
  );
}

export default CandidateSavedJobsPage;
