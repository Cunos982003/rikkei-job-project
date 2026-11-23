import React, { useState, useMemo, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Briefcase,
  Sparkles,
  Building2,
  TrendingUp,
  RotateCcw,
} from "lucide-react";
import { toast } from "sonner";
import { jobsApi, type JobPostingItem } from "../../api/endpoints/jobs.api";
import { candidateJobsApi } from "../../api/endpoints/candidateJobs.api";
import { recruitmentApi } from "../../api/endpoints/recruitment.api";
import { Button } from "../../components/common/Button";
import { JobCardItem } from "./components/JobCardItem";
import { JobFilterBar, type JobFilters } from "./components/JobFilterBar";
import { ApplyJobModal } from "./components/ApplyJobModal";

// Fallback sample jobs nếu mock server chưa kết nối
const FALLBACK_JOBS: JobPostingItem[] = [
  {
    id: "job-001",
    title: "Java Backend Developer (Fresher / Junior)",
    companyId: "comp-001",
    companyName: "Công ty Cổ phần Rikkeisoft",
    location: "Hà Nội",
    salaryMin: 12000000,
    salaryMax: 18000000,
    jobType: "FULL_TIME",
    requiredSkills: ["Java Core", "Spring Boot", "SQL", "REST API", "Git"],
    description:
      "Tham gia phát triển các dự án backend quy mô lớn cho khách hàng Nhật Bản và Việt Nam. Yêu cầu nắm vững Java Core, Spring Boot và REST API.",
    status: "PUBLISHED",
    createdAt: "2026-11-10T08:00:00Z",
  },
  {
    id: "job-002",
    title: "Frontend React Developer (Fresher)",
    companyId: "comp-002",
    companyName: "Rikkei Digital Solutions",
    location: "Đà Nẵng",
    salaryMin: 10000000,
    salaryMax: 16000000,
    jobType: "FULL_TIME",
    requiredSkills: ["ReactJS", "TypeScript", "HTML/CSS", "JavaScript", "Tailwind CSS"],
    description:
      "Xây dựng các giao diện web responsive hiện đại với React, TypeScript và Tailwind CSS. Phối hợp chặt chẽ với team UI/UX Designer.",
    status: "PUBLISHED",
    createdAt: "2026-11-11T09:30:00Z",
  },
  {
    id: "job-003",
    title: "Thực tập sinh Fullstack (Dự án IOC)",
    companyId: "comp-001",
    companyName: "Công ty Cổ phần Rikkeisoft",
    location: "Hà Nội / Hồ Chí Minh",
    salaryMin: 5000000,
    salaryMax: 8000000,
    jobType: "INTERNSHIP",
    requiredSkills: ["Java", "Spring Boot", "ReactJS", "SQL", "Git"],
    description:
      "Tham gia đợt thực tập dự án thực chiến IOC, được Mentor hướng dẫn 1-1, cơ hội trở thành nhân viên chính thức sau khi tốt nghiệp.",
    status: "PUBLISHED",
    createdAt: "2026-11-12T10:15:00Z",
  },
];

const INITIAL_FILTERS: JobFilters = {
  keyword: "",
  location: "",
  salaryRange: "",
  jobType: "",
  level: "",
};

export const CandidateJobsPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<JobFilters>(INITIAL_FILTERS);
  const [applyJob, setApplyJob] = useState<JobPostingItem | null>(null);
  const [localAppliedIds, setLocalAppliedIds] = useState<Set<string>>(new Set());

  // 1. Lấy danh sách việc làm đã lưu
  const { data: savedJobsRes } = useQuery({
    queryKey: ["savedJobs"],
    queryFn: () =>
      candidateJobsApi.getSavedJobs().catch(() => ({ success: true, statusCode: 200, message: "", data: [] })),
    staleTime: 60 * 1000,
    retry: false,
  });

  const savedJobIds = useMemo(() => {
    const list = savedJobsRes?.data;
    if (Array.isArray(list)) {
      return new Set(list.map((j: { id: string }) => j.id));
    }
    return new Set<string>();
  }, [savedJobsRes]);

  // 2. Lấy danh sách đơn ứng tuyển đã nộp
  const { data: myRecruitmentsRes } = useQuery({
    queryKey: ["my-recruitments"],
    queryFn: () => recruitmentApi.getMyRecruitments().catch(() => ({ data: [] } as any)),
    staleTime: 5 * 60 * 1000,
  });

  const appliedJobIds = useMemo(() => {
    const set = new Set(localAppliedIds);
    const list = (myRecruitmentsRes as any)?.data ?? myRecruitmentsRes;
    if (Array.isArray(list)) {
      list.forEach((r: any) => {
        if (r?.jobId) set.add(r.jobId);
      });
    }
    return set;
  }, [myRecruitmentsRes, localAppliedIds]);

  // 3. Lấy danh sách việc làm từ Backend / Mock API
  const { data: jobsRes, isLoading } = useQuery({
    queryKey: ["candidateJobs"],
    queryFn: async () => {
      try {
        const res = await jobsApi.getJobs();
        return res;
      } catch {
        return null;
      }
    },
    staleTime: 5 * 60 * 1000,
  });

  const jobs: JobPostingItem[] = useMemo(() => {
    if (jobsRes?.data) {
      if (Array.isArray(jobsRes.data)) return jobsRes.data;
      if ("items" in jobsRes.data && Array.isArray(jobsRes.data.items)) return jobsRes.data.items;
    }
    if (Array.isArray(jobsRes)) return jobsRes;
    return FALLBACK_JOBS;
  }, [jobsRes]);

  // 4. Lọc việc làm đa tiêu chí
  const filteredJobs = useMemo(() => {
    return jobs.filter((job) => {
      // a. Từ khóa (chức danh, công ty, kỹ năng, mô tả)
      if (filters.keyword) {
        const q = filters.keyword.toLowerCase().trim();
        const matchTitle = job.title?.toLowerCase().includes(q);
        const matchCompany = job.companyName?.toLowerCase().includes(q);
        const matchSkills = job.requiredSkills?.some((s) => s.toLowerCase().includes(q));
        const matchDesc = job.description?.toLowerCase().includes(q);
        if (!matchTitle && !matchCompany && !matchSkills && !matchDesc) {
          return false;
        }
      }

      // b. Địa điểm
      if (filters.location) {
        const loc = filters.location.toLowerCase();
        if (loc === "remote") {
          const isRemote =
            job.location?.toLowerCase().includes("remote") ||
            job.title?.toLowerCase().includes("remote");
          if (!isRemote) return false;
        } else {
          if (!job.location?.toLowerCase().includes(loc)) {
            return false;
          }
        }
      }

      // c. Mức lương
      if (filters.salaryRange) {
        const min = job.salaryMin || 0;
        const max = job.salaryMax || 0;
        switch (filters.salaryRange) {
          case "under-10":
            if (max > 10000000 || (min > 10000000 && max === 0)) return false;
            break;
          case "10-15":
            if (max < 10000000 || min > 15000000) return false;
            break;
          case "15-20":
            if (max < 15000000 || min > 20000000) return false;
            break;
          case "20-30":
            if (max < 20000000 || min > 30000000) return false;
            break;
          case "over-30":
            if (max < 30000000 && min < 30000000) return false;
            break;
          case "negotiable":
            if (min > 0 || max > 0) return false;
            break;
        }
      }

      // d. Hình thức làm việc
      if (filters.jobType) {
        if (job.jobType !== filters.jobType) return false;
      }

      // e. Cấp bậc
      if (filters.level) {
        const text = `${job.title} ${job.description || ""}`.toLowerCase();
        switch (filters.level) {
          case "INTERN":
            if (!text.includes("intern") && !text.includes("thực tập") && job.jobType !== "INTERNSHIP") {
              return false;
            }
            break;
          case "FRESHER":
            if (!text.includes("fresher") && !text.includes("mới tốt nghiệp")) {
              return false;
            }
            break;
          case "JUNIOR":
            if (!text.includes("junior")) return false;
            break;
          case "MIDDLE_SENIOR":
            if (!text.includes("middle") && !text.includes("senior") && !text.includes("lead")) {
              return false;
            }
            break;
        }
      }

      return true;
    });
  }, [jobs, filters]);

  // 5. Thao tác Lưu / Bỏ lưu việc làm với Optimistic UI
  const toggleSaveMutation = useMutation({
    mutationFn: async ({ jobId, isCurrentlySaved }: { jobId: string; isCurrentlySaved: boolean }) => {
      if (isCurrentlySaved) {
        return candidateJobsApi.unsaveJob(jobId);
      } else {
        return candidateJobsApi.saveJob(jobId);
      }
    },
    onMutate: async ({ jobId, isCurrentlySaved }) => {
      await queryClient.cancelQueries({ queryKey: ["savedJobs"] });
      const previousSaved = queryClient.getQueryData(["savedJobs"]);

      queryClient.setQueryData(["savedJobs"], (old: any) => {
        const currentData = Array.isArray(old?.data) ? old.data : [];
        if (isCurrentlySaved) {
          return { ...old, data: currentData.filter((j: any) => j.id !== jobId) };
        } else {
          const targetJob = jobs.find((j) => j.id === jobId);
          return { ...old, data: targetJob ? [...currentData, targetJob] : currentData };
        }
      });

      return { previousSaved };
    },
    onError: (_err, _variables, context) => {
      if (context?.previousSaved) {
        queryClient.setQueryData(["savedJobs"], context.previousSaved);
      }
      toast.error("Không thể cập nhật danh sách việc làm đã lưu!");
    },
    onSuccess: (_data, { isCurrentlySaved }) => {
      if (isCurrentlySaved) {
        toast.info("Đã bỏ lưu việc làm.");
      } else {
        toast.success("Đã lưu việc làm vào danh sách theo dõi!");
      }
      queryClient.invalidateQueries({ queryKey: ["savedJobs"] });
    },
  });

  const handleToggleSave = useCallback(
    (jobId: string) => {
      const isCurrentlySaved = savedJobIds.has(jobId);
      toggleSaveMutation.mutate({ jobId, isCurrentlySaved });
    },
    [savedJobIds, toggleSaveMutation]
  );

  // 6. Nộp đơn ứng tuyển
  const applyMutation = useMutation({
    mutationFn: ({ jobId, coverLetter }: { jobId: string; coverLetter: string }) =>
      recruitmentApi.applyJob(jobId, { coverLetter: coverLetter || undefined }),
    onSuccess: (_res, variables) => {
      toast.success("Nộp đơn ứng tuyển thành công! Doanh nghiệp sẽ liên hệ với bạn.");
      setLocalAppliedIds((prev) => new Set(prev).add(variables.jobId));
      setApplyJob(null);
      queryClient.invalidateQueries({ queryKey: ["my-recruitments"] });
    },
    onError: () => {
      // Toast lỗi đã có axios interceptor xử lý
    },
  });

  const formatSalary = useCallback((min?: number, max?: number) => {
    if (!min && !max) return "Thoả thuận";
    if (min && max) {
      return `${(min / 1000000).toFixed(0)} - ${(max / 1000000).toFixed(0)} triệu`;
    }
    return `${((min || max)! / 1000000).toFixed(0)} triệu`;
  }, []);

  const handleAnalyzeJob = useCallback(
    (jobId: string) => {
      navigate(`/candidate/jobs/${jobId}?analyze=true`);
    },
    [navigate]
  );

  const handleOpenApplyModal = useCallback((job: JobPostingItem) => {
    setApplyJob(job);
  }, []);

  const handleCloseApplyModal = useCallback(() => {
    setApplyJob(null);
  }, []);

  const handleSubmitApply = useCallback(
    (jobId: string, coverLetter: string) => {
      applyMutation.mutate({ jobId, coverLetter });
    },
    [applyMutation]
  );

  return (
    <div className="space-y-6 pb-12">
      {/* 1. Hero Banner TopCV Style */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-slate-900 via-slate-800 to-indigo-950 text-white p-6 sm:p-8 shadow-lg">
        <div className="relative z-10 max-w-2xl space-y-2">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md text-xs font-semibold text-emerald-400">
            <Sparkles className="h-3.5 w-3.5" />
            <span>Cổng tuyển dụng đồng bộ hồ sơ Rikkei LMS</span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Khám phá công việc IT phù hợp năng lực của bạn
          </h1>

          <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
            Hàng trăm việc làm từ Fresher đến Senior tại các doanh nghiệp hàng đầu.
            Phân tích mức độ đáp ứng kỹ năng tại chỗ và nộp đơn nhanh chóng.
          </p>

          <div className="flex items-center gap-4 pt-2 text-xs text-slate-300">
            <span className="flex items-center gap-1.5">
              <Building2 className="h-4 w-4 text-emerald-400" />
              <span>Đối tác tuyển dụng uy tín</span>
            </span>
            <span className="flex items-center gap-1.5">
              <TrendingUp className="h-4 w-4 text-primary-light" />
              <span>Cập nhật liên tục mỗi ngày</span>
            </span>
          </div>
        </div>

        {/* Trang trí background */}
        <div className="absolute right-0 top-0 bottom-0 w-1/3 bg-radial from-primary/20 to-transparent pointer-events-none" />
      </div>

      {/* 2. Hero Search & Filter Bar */}
      <JobFilterBar
        filters={filters}
        onChange={setFilters}
        onReset={() => setFilters(INITIAL_FILTERS)}
        totalResults={filteredJobs.length}
      />

      {/* 3. Job Cards List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="space-y-4 animate-pulse">
            <div className="h-44 bg-slate-200/80 rounded-2xl" />
            <div className="h-44 bg-slate-200/80 rounded-2xl" />
            <div className="h-44 bg-slate-200/80 rounded-2xl" />
          </div>
        ) : filteredJobs.length === 0 ? (
          <div className="text-center py-16 px-4 rounded-2xl border border-dashed border-border bg-surface space-y-4">
            <div className="w-14 h-14 rounded-2xl bg-slate-100 flex items-center justify-center mx-auto text-muted">
              <Briefcase className="h-7 w-7" />
            </div>
            <div className="space-y-1">
              <h3 className="text-sm font-bold text-foreground">
                Không tìm thấy việc làm nào phù hợp
              </h3>
              <p className="text-xs text-muted max-w-md mx-auto">
                Hãy thử xóa bớt các tiêu chí lọc hoặc tìm kiếm bằng từ khóa chung hơn
                (ví dụ: Java, React, SQL...).
              </p>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setFilters(INITIAL_FILTERS)}
              leftIcon={<RotateCcw className="h-3.5 w-3.5" />}
            >
              Đặt lại toàn bộ bộ lọc
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-3.5">
            {filteredJobs.map((job) => (
              <JobCardItem
                key={job.id}
                job={job}
                isApplied={appliedJobIds.has(job.id)}
                isSaved={savedJobIds.has(job.id)}
                onApply={handleOpenApplyModal}
                onAnalyze={handleAnalyzeJob}
                onToggleSave={handleToggleSave}
                formatSalary={formatSalary}
              />
            ))}
          </div>
        )}
      </div>

      {/* 4. Modal Nộp đơn ứng tuyển độc lập */}
      <ApplyJobModal
        isOpen={Boolean(applyJob)}
        job={applyJob}
        isSubmitting={applyMutation.isPending}
        onClose={handleCloseApplyModal}
        onSubmit={handleSubmitApply}
      />
    </div>
  );
};

export default CandidateJobsPage;
