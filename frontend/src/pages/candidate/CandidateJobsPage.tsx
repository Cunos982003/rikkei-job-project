import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Briefcase,
  Building2,
  MapPin,
  DollarSign,
  Target,
  Search,
} from "lucide-react";
import { jobsApi, type JobPostingItem } from "../../api/endpoints/jobs.api";
import { Card, CardContent } from "../../components/common/Card";
import { Button } from "../../components/common/Button";
import { Badge } from "../../components/common/Badge";
import { Modal } from "../../components/common/Modal";

// Fallback sample jobs matching mock/data/jobs.json
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
    description: "Tham gia phát triển các dự án backend quy mô lớn cho khách hàng Nhật Bản và Việt Nam. Yêu cầu nắm vững Java Core, Spring Boot và REST API.",
    status: "PUBLISHED",
    createdAt: "2026-11-10T08:00:00Z",
  },
  {
    id: "job-002",
    title: "Frontend ReactJS Developer",
    companyId: "comp-002",
    companyName: "Rikkei Digital Solutions",
    location: "Đà Nẵng",
    salaryMin: 10000000,
    salaryMax: 16000000,
    jobType: "FULL_TIME",
    requiredSkills: ["ReactJS", "TypeScript", "HTML/CSS", "Git", "REST API"],
    description: "Xây dựng các giao diện web responsive hiện đại với React 19, TypeScript và Tailwind CSS. Phối hợp chặt chẽ với team UI/UX Designer.",
    status: "PUBLISHED",
    createdAt: "2026-11-11T09:30:00Z",
  },
  {
    id: "job-003",
    title: "QA / Automation Tester",
    companyId: "comp-003",
    companyName: "Rikkei QA Center",
    location: "Hồ Chí Minh",
    salaryMin: 11000000,
    salaryMax: 17000000,
    jobType: "FULL_TIME",
    requiredSkills: ["Manual Testing", "SQL", "Selenium", "Postman", "Git"],
    description: "Thiết kế test cases, thực hiện kiểm thử chức năng và phi chức năng, viết automation test scripts với Selenium và Postman.",
    status: "PUBLISHED",
    createdAt: "2026-11-12T10:15:00Z",
  },
];

export const CandidateJobsPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedJob, setSelectedJob] = useState<JobPostingItem | null>(null);

  const { data: jobsRes, isLoading } = useQuery({
    queryKey: ["candidateJobs"],
    queryFn: async () => {
      try {
        const res = await jobsApi.getJobs();
        return res;
      } catch (err) {
        // Fallback gracefully
        return null;
      }
    },
    staleTime: 5 * 60 * 1000,
  });

  // Extract jobs list
  const jobs: JobPostingItem[] = React.useMemo(() => {
    if (jobsRes?.data) {
      if (Array.isArray(jobsRes.data)) return jobsRes.data;
      if ("items" in jobsRes.data && Array.isArray(jobsRes.data.items)) return jobsRes.data.items;
    }
    return FALLBACK_JOBS;
  }, [jobsRes]);

  const filteredJobs = jobs.filter(
    (j) =>
      j.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      j.companyName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      j.requiredSkills.some((s) => s.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const formatSalary = (min?: number, max?: number) => {
    if (!min && !max) return "Thoả thuận";
    if (min && max) {
      return `${(min / 1000000).toFixed(0)} - ${(max / 1000000).toFixed(0)} triệu đ`;
    }
    return `${((min || max)! / 1000000).toFixed(0)} triệu đ`;
  };

  const handleAnalyzeJob = (jobId: string) => {
    navigate(`/candidate/learning?jobId=${jobId}`);
  };

  return (
    <div className="space-y-6 pb-12">
      {/* Header Bar */}
      <div className="pb-4 border-b border-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-lg bg-primary-light text-primary">
              <Briefcase className="h-6 w-6" />
            </span>
            <div>
              <h1 className="text-2xl font-bold text-foreground">
                Cơ hội Việc làm & Thực tập
              </h1>
              <p className="text-xs text-muted mt-0.5">
                Khám phá các tin tuyển dụng và phân tích độ phù hợp năng lực tức thì
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate("/candidate/learning")}
            leftIcon={<Target className="h-4 w-4 text-primary" />}
          >
            Xem phân tích năng lực cá nhân
          </Button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative">
        <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted" />
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Tìm theo chức danh, công ty hoặc kỹ năng (Java, ReactJS, SQL...)..."
          className="w-full pl-10 pr-4 py-2.5 text-sm rounded-xl border border-border bg-surface focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all shadow-xs"
        />
      </div>

      {/* Job List */}
      {isLoading ? (
        <div className="space-y-4 animate-pulse">
          <div className="h-40 bg-slate-200/80 rounded-xl" />
          <div className="h-40 bg-slate-200/80 rounded-xl" />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {filteredJobs.map((job) => (
          <Card key={job.id} hoverable className="border-border">
            <CardContent className="p-5">
              <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
                <div className="space-y-2 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-mono font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                      {job.id}
                    </span>
                    <Badge variant="verified" size="sm">
                      {job.jobType === "INTERNSHIP" ? "Thực tập" : "Toàn thời gian"}
                    </Badge>
                  </div>

                  <h3 className="text-lg font-bold text-foreground leading-tight hover:text-primary transition-colors cursor-pointer" onClick={() => setSelectedJob(job)}>
                    {job.title}
                  </h3>

                  <div className="flex items-center gap-4 text-xs text-muted flex-wrap">
                    <span className="flex items-center gap-1">
                      <Building2 className="h-3.5 w-3.5" />
                      {job.companyName}
                    </span>
                    <span className="flex items-center gap-1">
                      <MapPin className="h-3.5 w-3.5" />
                      {job.location}
                    </span>
                    <span className="flex items-center gap-1 font-semibold text-emerald-700">
                      <DollarSign className="h-3.5 w-3.5" />
                      {formatSalary(job.salaryMin, job.salaryMax)}
                    </span>
                  </div>

                  <p className="text-xs text-muted line-clamp-2 leading-relaxed">
                    {job.description}
                  </p>

                  {/* Skills */}
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {job.requiredSkills?.map((skill) => (
                      <span
                        key={skill}
                        className="text-[11px] font-medium px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 border border-slate-200"
                      >
                        {skill}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex lg:flex-col items-center sm:items-end justify-between lg:justify-center gap-2 pt-3 lg:pt-0 border-t lg:border-t-0 border-border/60 shrink-0">
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => handleAnalyzeJob(job.id)}
                    leftIcon={<Target className="h-4 w-4" />}
                    className="w-full sm:w-auto"
                  >
                    Phân tích so với JD này
                  </Button>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setSelectedJob(job)}
                    className="w-full sm:w-auto text-xs"
                  >
                    Xem chi tiết
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
          ))}
        </div>
      )}

      {/* Modal Job Details */}
      <Modal
        isOpen={!!selectedJob}
        onClose={() => setSelectedJob(null)}
        title={selectedJob?.title}
        description={selectedJob?.companyName}
        size="lg"
        footer={
          <div className="flex items-center justify-between w-full">
            <span className="text-xs font-semibold text-emerald-700">
              Lương: {selectedJob && formatSalary(selectedJob.salaryMin, selectedJob.salaryMax)}
            </span>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={() => setSelectedJob(null)}>
                Đóng
              </Button>
              {selectedJob && (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => {
                    handleAnalyzeJob(selectedJob.id);
                  }}
                  leftIcon={<Target className="h-4 w-4" />}
                >
                  Phân tích so với JD này
                </Button>
              )}
            </div>
          </div>
        }
      >
        {selectedJob && (
          <div className="space-y-4 text-sm text-foreground">
            <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 grid grid-cols-2 gap-3 text-xs">
              <div>
                <span className="text-muted block">Địa điểm làm việc:</span>
                <span className="font-semibold">{selectedJob.location}</span>
              </div>
              <div>
                <span className="text-muted block">Hình thức làm việc:</span>
                <span className="font-semibold">{selectedJob.jobType}</span>
              </div>
            </div>

            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                Mô tả công việc:
              </h4>
              <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
                {selectedJob.description}
              </p>
            </div>

            <div>
              <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-2">
                Kỹ năng yêu cầu:
              </h4>
              <div className="flex flex-wrap gap-1.5">
                {selectedJob.requiredSkills?.map((skill) => (
                  <span
                    key={skill}
                    className="text-xs font-medium px-2.5 py-1 rounded-md bg-primary-light text-primary border border-primary-border"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default CandidateJobsPage;
