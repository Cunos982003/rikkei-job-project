import { useState, useMemo, useCallback } from "react";
import { useParams, useNavigate, useSearchParams, Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Building2,
  MapPin,
  DollarSign,
  Clock,
  ArrowLeft,
  Heart,
  Send,
  Sparkles,
  CheckCircle2,
  Users,
  Target,
  FileCheck,
} from "lucide-react";
import { toast } from "sonner";
import { jobsApi, type JobPostingItem } from "../../api/endpoints/jobs.api";
import { candidateJobsApi } from "../../api/endpoints/candidateJobs.api";
import { recruitmentApi } from "../../api/endpoints/recruitment.api";
import { learningApi } from "../../api/endpoints/learning.api";
import { candidateKeys } from "../../api/queryKeys";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";
import { QuickCompetenceDrawer } from "./components/QuickCompetenceDrawer";
import { ApplyJobModal } from "./components/ApplyJobModal";
import { cn } from "../../utils/cn";

// Fallback jobs nếu chưa kết nối server
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
    workMode: "HYBRID",
    experienceLevel: "Fresher / Junior",
    expiresAt: "2026-12-31T23:59:59Z",
    requiredSkills: ["Java", "Spring Boot", "SQL", "Git", "REST API"],
    jobSkills: [
      { skillId: "JAVA_CORE", skillName: "Java Core", minScore: 75, isMandatory: true },
      { skillId: "SPRING_BOOT", skillName: "Spring Boot", minScore: 70, isMandatory: true },
      { skillId: "SQL", skillName: "SQL", minScore: 70, isMandatory: true },
      { skillId: "REST_API", skillName: "REST API", minScore: 70, isMandatory: true },
      { skillId: "GIT", skillName: "Git", minScore: 60, isMandatory: false },
    ],
    description:
      "Tham gia phát triển các dự án tài chính, ngân hàng và thương mại điện tử quy mô lớn. Làm việc với kiến trúc Microservices, Spring Boot, PostgreSQL, Apache Kafka và Docker.",
    requirements:
      "• Tốt nghiệp hoặc sắp tốt nghiệp Đại học/Học viện chuyên ngành CNTT, Phần mềm hoặc liên quan.\n• Nắm vững kiến thức nền tảng Java Core, OOP, Collection, Concurrency cơ bản.\n• Đã từng làm việc với Spring Boot, Spring Data JPA, Hibernate và RESTful API.\n• Hiểu biết về cơ sở dữ liệu quan hệ (PostgreSQL, MySQL), có khả năng viết truy vấn SQL tối ưu.\n• Kỹ năng sử dụng Git cơ bản, tinh thần trách nhiệm cao, ham học hỏi và sẵn sàng nghiên cứu công nghệ mới.",
    benefits:
      "• Mức lương cạnh tranh từ 12 - 18 triệu theo năng lực thực tế, xét tăng lương định kỳ 2 lần/năm.\n• Thưởng dự án hấp dẫn, thưởng tháng lương thứ 13, thưởng các ngày lễ Tết.\n• Được Mentor Senior dày dặn kinh nghiệm kèm cặp 1-1 trong suốt thời gian làm việc.\n• Công ty tài trợ 100% học phí và lệ phí thi các chứng chỉ quốc tế (AWS, Oracle Java...).\n• Gói bảo hiểm sức khỏe Rikkei Care cao cấp, khám sức khỏe định kỳ hàng năm.\n• Du lịch nghỉ mát 1-2 lần/năm, teambuilding hàng quý, môi trường trẻ trung, cởi mở.",
    status: "PUBLISHED",
    createdAt: "2026-10-15T08:00:00Z",
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
    workMode: "ONSITE",
    experienceLevel: "Fresher",
    expiresAt: "2026-12-25T23:59:59Z",
    requiredSkills: ["ReactJS", "TypeScript", "HTML/CSS", "JavaScript", "Tailwind CSS"],
    jobSkills: [
      { skillId: "REACT", skillName: "ReactJS", minScore: 70, isMandatory: true },
      { skillId: "JAVASCRIPT", skillName: "JavaScript", minScore: 75, isMandatory: true },
      { skillId: "HTML_CSS", skillName: "HTML/CSS", minScore: 75, isMandatory: true },
      { skillId: "TYPESCRIPT", skillName: "TypeScript", minScore: 65, isMandatory: false },
      { skillId: "TAILWIND_CSS", skillName: "Tailwind CSS", minScore: 60, isMandatory: false },
    ],
    description:
      "Phát triển giao diện ứng dụng web responsive, tích hợp REST API, tối ưu hiệu năng trang web cho thị trường Nhật Bản và Việt Nam. Phối hợp chặt chẽ với UI/UX Designer và Backend team.",
    requirements:
      "• Nắm vững kiến thức HTML5, CSS3, JavaScript (ES6+), ReactJS (Hooks, State Management).\n• Có kinh nghiệm làm việc với TypeScript và Tailwind CSS là một lợi thế lớn.\n• Hiểu cách tương tác với REST API thông qua Axios hoặc Fetch.\n• Tư duy thẩm mỹ UI/UX tốt, chú trọng chi tiết pixel-perfect và responsiveness.",
    benefits:
      "• Thu nhập từ 10 - 16 triệu/tháng, đánh giá năng lực xét thưởng rõ ràng.\n• Thưởng tháng lương thứ 13, thưởng hiệu quả công việc.\n• Tham gia vào các dự án chuyển đổi số công nghệ cao tại Đà Nẵng.\n• Văn phòng hiện đại view sông Hàn, trang thiết bị làm việc tân tiến (Macbook/Màn hình kép).",
    status: "PUBLISHED",
    createdAt: "2026-10-20T08:00:00Z",
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
    workMode: "HYBRID",
    experienceLevel: "Internship",
    expiresAt: "2026-12-15T23:59:59Z",
    requiredSkills: ["Java", "Spring Boot", "ReactJS", "SQL", "Làm việc nhóm"],
    jobSkills: [
      { skillId: "JAVA_CORE", skillName: "Java Core", minScore: 70, isMandatory: true },
      { skillId: "SPRING_BOOT", skillName: "Spring Boot", minScore: 65, isMandatory: true },
      { skillId: "REACT", skillName: "ReactJS", minScore: 65, isMandatory: true },
      { skillId: "SQL", skillName: "SQL", minScore: 65, isMandatory: true },
      { skillId: "GIT", skillName: "Git", minScore: 60, isMandatory: false },
    ],
    description:
      "Tham gia đợt thực tập dự án thực chiến IOC, được Mentor hướng dẫn 1-1, cơ hội trở thành nhân viên chính thức sau khi tốt nghiệp. Thực chiến phát triển các module Fullstack Java & React.",
    requirements:
      "• Sinh viên năm cuối hoặc mới tốt nghiệp chuyên ngành CNTT, có thể làm việc tối thiểu 4 ngày/tuần.\n• Nắm cơ bản Java và ReactJS.\n• Thái độ tích cực, chủ động, ham học hỏi và có khả năng làm việc nhóm.",
    benefits:
      "• Phụ cấp thực tập từ 5 - 8 triệu/tháng.\n• Cơ hội đặc cách lên nhân viên chính thức (Fresher) ngay sau kỳ thực tập mà không cần thi tuyển lại.\n• Xác nhận con dấu thực tập và hỗ trợ đồ án tốt nghiệp từ các chuyên gia Rikkei.",
    status: "PUBLISHED",
    createdAt: "2026-10-25T08:00:00Z",
  },
];

function getCompanyInitials(name: string): string {
  if (!name) return "RJ";
  const words = name.trim().split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[words.length - 1][0]).toUpperCase();
}

export function CandidateJobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isDrawerOpen, setIsDrawerOpen] = useState(() => searchParams.get("analyze") === "true");
  const [isApplyModalOpen, setIsApplyModalOpen] = useState(false);

  // 1. Lấy chi tiết công việc
  const { data: jobRes, isLoading } = useQuery({
    queryKey: candidateKeys.jobs.detail(id),
    queryFn: async () => {
      if (!id) return null;
      try {
        const res = await jobsApi.getJobById(id);
        return res;
      } catch {
        return null;
      }
    },
    staleTime: 5 * 60 * 1000,
  });

  const job: JobPostingItem | undefined = useMemo(() => {
    if (jobRes?.data) return jobRes.data;
    return FALLBACK_JOBS.find((j) => j.id === id) || FALLBACK_JOBS[0];
  }, [jobRes, id]);

  // 2. Lấy danh sách việc làm đã lưu
  const { data: savedJobsRes } = useQuery({
    queryKey: candidateKeys.jobs.saved(),
    queryFn: () =>
      candidateJobsApi.getSavedJobs().catch(() => ({ success: true, statusCode: 200, message: "", data: [] })),
    staleTime: 60 * 1000,
    retry: false,
  });

  const isSaved = useMemo(() => {
    if (!job || !Array.isArray(savedJobsRes?.data)) return false;
    return savedJobsRes.data.some((j: { id: string }) => j.id === job.id);
  }, [savedJobsRes, job]);

  // 3. Lấy danh sách ứng tuyển
  const { data: recruitmentsRes } = useQuery({
    queryKey: candidateKeys.recruitments.all(),
    queryFn: () => recruitmentApi.getMyRecruitments().catch(() => ({ data: [] } as any)),
    staleTime: 5 * 60 * 1000,
  });

  const isApplied = useMemo(() => {
    if (!job) return false;
    const list = (recruitmentsRes as any)?.data ?? recruitmentsRes;
    if (Array.isArray(list)) {
      return list.some((r: any) => r?.jobId === job.id && r?.status !== "WITHDRAWN");
    }
    return false;
  }, [recruitmentsRes, job]);

  // 4. Lấy nhanh điểm sẵn sàng cho thẻ widget
  const { data: quickGapRes } = useQuery({
    queryKey: candidateKeys.learning.gapAnalysis(job?.id),
    queryFn: () => learningApi.getGapAnalysis(job?.id).catch(() => null),
    enabled: Boolean(job?.id),
    staleTime: 60 * 1000,
    retry: false,
  });

  const readinessScore = quickGapRes?.data?.readiness ?? 80;

  // 5. Thao tác Lưu / Bỏ lưu
  const toggleSaveMutation = useMutation({
    mutationFn: async () => {
      if (!job) return;
      if (isSaved) {
        return candidateJobsApi.unsaveJob(job.id);
      } else {
        return candidateJobsApi.saveJob(job.id);
      }
    },
    onMutate: async () => {
      if (!job) return;
      await queryClient.cancelQueries({ queryKey: candidateKeys.jobs.saved() });
      const previous = queryClient.getQueryData(candidateKeys.jobs.saved());

      queryClient.setQueryData(candidateKeys.jobs.saved(), (old: any) => {
        const currentData = Array.isArray(old?.data) ? old.data : [];
        if (isSaved) {
          return { ...old, data: currentData.filter((j: any) => j.id !== job.id) };
        } else {
          return { ...old, data: [...currentData, job] };
        }
      });

      return { previous };
    },
    onError: (_err, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(candidateKeys.jobs.saved(), context.previous);
      }
      toast.error("Không thể cập nhật danh sách việc làm đã lưu!");
    },
    onSuccess: () => {
      if (isSaved) {
        toast.info("Đã bỏ lưu việc làm.");
      } else {
        toast.success("Đã lưu việc làm vào danh sách theo dõi!");
      }
      queryClient.invalidateQueries({ queryKey: candidateKeys.jobs.saved() });
    },
  });

  // 6. Nộp đơn ứng tuyển
  const applyMutation = useMutation({
    mutationFn: ({ coverLetter }: { coverLetter: string }) => {
      if (!job) throw new Error("Chưa chọn việc làm");
      return recruitmentApi.applyJob(job.id, { coverLetter: coverLetter || undefined });
    },
    onSuccess: () => {
      toast.success("Nộp đơn ứng tuyển thành công! HR doanh nghiệp sẽ liên hệ với bạn.");
      setIsApplyModalOpen(false);
      queryClient.invalidateQueries({ queryKey: candidateKeys.recruitments.all() });
    },
  });

  const formatSalary = useCallback((min?: number, max?: number) => {
    if (!min && !max) return "Thoả thuận";
    if (min && max) {
      return `${(min / 1000000).toFixed(0)} - ${(max / 1000000).toFixed(0)} triệu đ/tháng`;
    }
    return `${((min || max)! / 1000000).toFixed(0)} triệu đ/tháng`;
  }, []);

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4 space-y-6 animate-pulse">
        <div className="h-6 w-32 bg-slate-200 rounded-lg" />
        <div className="h-48 bg-slate-200 rounded-3xl" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 h-96 bg-slate-200 rounded-3xl" />
          <div className="h-96 bg-slate-200 rounded-3xl" />
        </div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="max-w-2xl mx-auto py-16 px-4 text-center space-y-4">
        <h2 className="text-xl font-bold text-foreground">Không tìm thấy tin tuyển dụng này</h2>
        <p className="text-sm text-muted">Tin tuyển dụng có thể đã hết hạn hoặc bị gỡ bỏ.</p>
        <Button variant="primary" onClick={() => navigate("/candidate/jobs")}>
          Quay lại danh sách việc làm
        </Button>
      </div>
    );
  }

  const initials = getCompanyInitials(job.companyName);

  return (
    <div className="max-w-7xl mx-auto py-4 sm:py-6 space-y-6 pb-16">
      {/* 1. Breadcrumbs & Back Navigation */}
      <div className="flex items-center justify-between text-xs text-muted">
        <button
          type="button"
          onClick={() => navigate("/candidate/jobs")}
          className="inline-flex items-center gap-1.5 font-medium hover:text-primary transition-colors cursor-pointer"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Quay lại tìm việc làm</span>
        </button>

        <div className="flex items-center gap-2 hidden sm:flex">
          <Link to="/candidate/jobs" className="hover:text-primary">
            Cơ hội việc làm
          </Link>
          <span>/</span>
          <span className="text-foreground font-semibold truncate max-w-[200px]">
            {job.title}
          </span>
        </div>
      </div>

      {/* 2. Top Banner Card (TopCV Hero Header) */}
      <div className="bg-surface border border-border rounded-3xl p-6 sm:p-8 shadow-xs space-y-6">
        <div className="flex flex-col md:flex-row items-start justify-between gap-6">
          <div className="flex items-start gap-4 flex-1">
            <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-br from-primary to-indigo-700 text-white font-extrabold text-xl sm:text-2xl flex items-center justify-center shrink-0 shadow-sm">
              {initials}
            </div>

            <div className="space-y-2 flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h1 className="text-xl sm:text-2xl font-extrabold text-foreground tracking-tight">
                  {job.title}
                </h1>
                {isApplied && (
                  <span className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    Đã nộp đơn
                  </span>
                )}
              </div>

              <div className="flex items-center gap-4 text-xs text-muted flex-wrap">
                <span className="flex items-center gap-1.5 font-semibold text-slate-800">
                  <Building2 className="h-4 w-4 text-muted" />
                  {job.companyName}
                </span>
                <span className="flex items-center gap-1.5">
                  <MapPin className="h-4 w-4 text-muted" />
                  {job.location}
                </span>
                {job.expiresAt && (
                  <span className="flex items-center gap-1.5 text-slate-500">
                    <Clock className="h-4 w-4 text-muted" />
                    Hạn nộp: {new Date(job.expiresAt).toLocaleDateString("vi-VN")}
                  </span>
                )}
              </div>

              {/* Salary highlight */}
              <div className="pt-1 flex items-center gap-3 flex-wrap">
                <span className="inline-flex items-center gap-1.5 text-sm sm:text-base font-extrabold px-3 py-1 rounded-xl bg-emerald-50 text-emerald-700 border border-emerald-200">
                  <DollarSign className="h-4 w-4" />
                  {formatSalary(job.salaryMin, job.salaryMax)}
                </span>

                <Badge variant="verified">
                  {job.jobType === "INTERNSHIP" ? "Thực tập" : "Toàn thời gian"}
                </Badge>

                {job.workMode && (
                  <span className="text-xs font-medium px-2.5 py-1 rounded-lg bg-slate-100 text-slate-700">
                    {job.workMode}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Action buttons stack */}
          <div className="flex items-center md:flex-col gap-3 w-full md:w-auto shrink-0 pt-4 md:pt-0 border-t md:border-t-0 border-border">
            {isApplied ? (
              <div className="flex-1 md:w-48 h-10 inline-flex items-center justify-center gap-2 text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-300 rounded-xl">
                <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                Đã nộp đơn ứng tuyển
              </div>
            ) : (
              <Button
                variant="primary"
                size="md"
                onClick={() => setIsApplyModalOpen(true)}
                leftIcon={<Send className="h-4 w-4" />}
                className="flex-1 md:w-48 justify-center shadow-md font-bold text-sm"
              >
                Nộp đơn ứng tuyển
              </Button>
            )}

            <Button
              variant="outline"
              size="md"
              onClick={() => setIsDrawerOpen(true)}
              leftIcon={<Sparkles className="h-4 w-4 text-primary" />}
              className="flex-1 md:w-48 justify-center text-xs font-semibold hover:border-primary/60 hover:text-primary"
            >
              Phân tích năng lực
            </Button>

            <button
              type="button"
              onClick={() => toggleSaveMutation.mutate()}
              className={cn(
                "p-2.5 rounded-xl border transition-all cursor-pointer flex items-center justify-center md:w-48 gap-2 text-xs font-semibold",
                isSaved
                  ? "bg-rose-50 text-rose-600 border-rose-200 hover:bg-rose-100"
                  : "bg-surface text-slate-600 border-border hover:bg-slate-50 hover:text-rose-600"
              )}
            >
              <Heart className={cn("h-4 w-4", isSaved ? "fill-rose-600" : "")} />
              <span>{isSaved ? "Đã lưu việc làm" : "Lưu việc làm"}</span>
            </button>
          </div>
        </div>
      </div>

      {/* 3. Main Grid (70% Content Left - 30% Widgets Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left Column: Chi tiết JD */}
        <div className="lg:col-span-2 space-y-6">
          {/* Card: Chi tiết công việc */}
          <div className="bg-surface border border-border rounded-3xl p-6 sm:p-8 shadow-xs space-y-8">
            {/* Mô tả công việc */}
            <div className="space-y-3">
              <h2 className="text-base font-bold text-foreground flex items-center gap-2 border-l-4 border-primary pl-3">
                Mô tả công việc
              </h2>
              <div className="text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line pl-4">
                {job.description}
              </div>
            </div>

            {/* Yêu cầu ứng viên */}
            {job.requirements && (
              <div className="space-y-3">
                <h2 className="text-base font-bold text-foreground flex items-center gap-2 border-l-4 border-primary pl-3">
                  Yêu cầu ứng viên
                </h2>
                <div className="text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line pl-4">
                  {job.requirements}
                </div>
              </div>
            )}

            {/* Quyền lợi được hưởng */}
            {job.benefits && (
              <div className="space-y-3">
                <h2 className="text-base font-bold text-foreground flex items-center gap-2 border-l-4 border-emerald-500 pl-3">
                  Quyền lợi được hưởng
                </h2>
                <div className="text-xs sm:text-sm text-slate-700 leading-relaxed whitespace-pre-line pl-4">
                  {job.benefits}
                </div>
              </div>
            )}

            {/* Kỹ năng yêu cầu & Điểm chuẩn */}
            <div className="space-y-3">
              <h2 className="text-base font-bold text-foreground flex items-center gap-2 border-l-4 border-primary pl-3">
                Kỹ năng chuyên môn & Điểm chuẩn tối thiểu
              </h2>

              <p className="text-xs text-muted pl-4">
                Mức điểm đánh giá dựa trên tiêu chuẩn sát hạch và bài thi thực tế từ học viện Rikkei LMS:
              </p>

              <div className="pl-4 grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
                {(job.jobSkills && job.jobSkills.length > 0
                  ? job.jobSkills
                  : (job.requiredSkills || []).map((s) => ({
                      skillId: s,
                      skillName: s,
                      minScore: 70,
                      isMandatory: true,
                    }))
                ).map((sk) => (
                  <div
                    key={sk.skillId}
                    className="p-3 rounded-2xl border border-border bg-slate-50/70 flex items-center justify-between text-xs"
                  >
                    <div>
                      <p className="font-bold text-foreground">{sk.skillName}</p>
                      <span className="text-[11px] text-muted">
                        {sk.isMandatory ? (
                          <span className="text-danger font-medium">* Bắt buộc</span>
                        ) : (
                          "Kỹ năng bổ trợ"
                        )}
                      </span>
                    </div>

                    <div className="text-right">
                      <span className="text-xs font-bold px-2 py-0.5 rounded-lg bg-primary-light text-primary">
                        ≥ {sk.minScore} điểm
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Bottom Apply CTA bar */}
            <div className="pt-6 border-t border-border flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-bold text-foreground">Bạn đã sẵn sàng cho cơ hội này?</p>
                <p className="text-[11px] text-muted">Hồ sơ sẽ được gửi trực tiếp tới phòng nhân sự Rikkei.</p>
              </div>

              {!isApplied && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => setIsApplyModalOpen(true)}
                  leftIcon={<Send className="h-3.5 w-3.5" />}
                  className="shadow-xs font-bold"
                >
                  Nộp đơn ngay
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* Right Column: Widgets */}
        <div className="space-y-6">
          {/* Widget 1: Thẻ phân tích năng lực nhanh tại chỗ */}
          <div className="bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 rounded-3xl p-6 text-white shadow-md space-y-4">
            <div className="flex items-center gap-2 text-emerald-400 text-xs font-bold uppercase tracking-wider">
              <Sparkles className="h-4 w-4" />
              <span>Đối chiếu năng lực tại chỗ</span>
            </div>

            <div>
              <p className="text-xs text-slate-300">Độ tương thích hồ sơ LMS của bạn:</p>
              <div className="flex items-baseline gap-2 mt-1">
                <span className="text-3xl font-extrabold text-white">{readinessScore}%</span>
                <span className="text-xs text-emerald-400 font-semibold">Phù hợp</span>
              </div>
            </div>

            <p className="text-[11px] text-slate-300 leading-relaxed">
              Nhấn để xem chi tiết các kỹ năng đã đạt, kỹ năng cần bổ sung và các khóa học LMS gợi ý trực tiếp tại trang này.
            </p>

            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsDrawerOpen(true)}
              leftIcon={<Target className="h-3.5 w-3.5 text-primary" />}
              className="w-full justify-center bg-white text-slate-900 hover:bg-slate-100 border-none font-bold text-xs"
            >
              Mở bảng phân tích năng lực
            </Button>
          </div>

          {/* Widget 2: Thông tin công ty */}
          <div className="bg-surface border border-border rounded-3xl p-6 shadow-xs space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center text-primary font-bold text-base border border-border">
                {initials}
              </div>
              <div className="min-w-0">
                <h3 className="text-sm font-bold text-foreground truncate">{job.companyName}</h3>
                <p className="text-xs text-muted">Doanh nghiệp đối tác tuyển dụng</p>
              </div>
            </div>

            <div className="space-y-2.5 text-xs text-slate-700 pt-2 border-t border-border-subtle">
              <div className="flex items-start gap-2">
                <MapPin className="h-4 w-4 text-muted shrink-0 mt-0.5" />
                <span>{job.location}</span>
              </div>
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4 text-muted shrink-0" />
                <span>Quy mô: 500 - 1000+ nhân viên</span>
              </div>
              <div className="flex items-center gap-2">
                <FileCheck className="h-4 w-4 text-muted shrink-0" />
                <span>Xác thực doanh nghiệp chính thức</span>
              </div>
            </div>
          </div>

          {/* Widget 3: Tóm tắt thông tin việc làm */}
          <div className="bg-surface border border-border rounded-3xl p-6 shadow-xs space-y-3">
            <h3 className="text-xs font-bold text-foreground uppercase tracking-wider">
              Tổng quan tuyển dụng
            </h3>

            <div className="space-y-2.5 text-xs text-slate-700">
              <div className="flex items-center justify-between">
                <span className="text-muted">Cấp bậc:</span>
                <span className="font-semibold text-foreground">{job.experienceLevel || "Fresher / Junior"}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted">Hình thức:</span>
                <span className="font-semibold text-foreground">{job.workMode || "Toàn thời gian"}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-muted">Ngày đăng tin:</span>
                <span className="font-semibold text-foreground">
                  {new Date(job.createdAt).toLocaleDateString("vi-VN")}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 4. Sliding Drawer Phân tích năng lực tại chỗ */}
      <QuickCompetenceDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        jobId={job.id}
        jobTitle={job.title}
        companyName={job.companyName}
        jobSkills={job.jobSkills}
        requiredSkills={job.requiredSkills}
        isApplied={isApplied}
        onOpenApply={() => setIsApplyModalOpen(true)}
      />

      {/* 5. Modal Nộp đơn ứng tuyển độc lập */}
      <ApplyJobModal
        isOpen={isApplyModalOpen}
        job={job}
        isSubmitting={applyMutation.isPending}
        onClose={() => setIsApplyModalOpen(false)}
        onSubmit={(_jobId, coverLetter) => applyMutation.mutate({ coverLetter })}
      />
    </div>
  );
}

export default CandidateJobDetailPage;
