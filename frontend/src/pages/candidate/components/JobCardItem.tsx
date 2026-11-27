import React from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Building2,
  MapPin,
  DollarSign,
  Heart,
  Send,
  Target,
  ChevronRight,
  CheckCircle2,
  Calendar,
} from "lucide-react";
import { Card, CardContent } from "../../../components/common/Card";
import { Button } from "../../../components/common/Button";
import { Badge } from "../../../components/common/Badge";
import type { JobPostingItem } from "../../../api/endpoints/jobs.api";
import { cn } from "../../../utils/cn";

interface JobCardItemProps {
  job: JobPostingItem;
  isApplied: boolean;
  isSaved: boolean;
  onApply: (job: JobPostingItem) => void;
  onAnalyze: (jobId: string) => void;
  onToggleSave: (jobId: string) => void;
  formatSalary: (min?: number, max?: number) => string;
}

// Hàm lấy chữ cái viết tắt của tên công ty
function getCompanyInitials(name: string): string {
  if (!name) return "RJ";
  const words = name.trim().split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[words.length - 1][0]).toUpperCase();
}

// Bảng màu gradient nhẹ nhàng cho avatar công ty
const GRADIENT_PALETTES = [
  "from-blue-600 to-indigo-600",
  "from-emerald-600 to-teal-600",
  "from-violet-600 to-purple-600",
  "from-rose-600 to-pink-600",
  "from-amber-600 to-orange-600",
];

function getPaletteByString(str: string): string {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return GRADIENT_PALETTES[Math.abs(hash) % GRADIENT_PALETTES.length];
}

export const JobCardItem = React.memo<JobCardItemProps>(
  ({
    job,
    isApplied,
    isSaved,
    onApply,
    onAnalyze,
    onToggleSave,
    formatSalary,
  }) => {
    const navigate = useNavigate();
    const initials = getCompanyInitials(job.companyName);
    const gradient = getPaletteByString(job.companyName || job.id);

    const handleCardClick = (e: React.MouseEvent) => {
      // Tránh chuyển trang khi click vào nút hành động con
      const target = e.target as HTMLElement;
      if (target.closest("button") || target.closest("a")) {
        return;
      }
      navigate(`/candidate/jobs/${job.id}`);
    };

    return (
      <Card
        hoverable
        onClick={handleCardClick}
        className="group relative border-border hover:border-primary/40 transition-all duration-200 cursor-pointer overflow-hidden bg-surface shadow-xs hover:shadow-md"
      >
        <CardContent className="p-5">
          <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
            {/* 1. Left: Company Avatar + Job Information */}
            <div className="flex items-start gap-4 flex-1 min-w-0">
              {/* Company Logo / Initials */}
              <div
                className={cn(
                  "w-12 h-12 rounded-xl flex items-center justify-center text-white font-extrabold text-sm shrink-0 shadow-xs bg-gradient-to-br",
                  gradient
                )}
              >
                {initials}
              </div>

              {/* Title & Metadata */}
              <div className="space-y-1.5 flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <Link
                    to={`/candidate/jobs/${job.id}`}
                    className="text-base font-bold text-foreground group-hover:text-primary transition-colors line-clamp-1"
                    title={job.title}
                  >
                    {job.title}
                  </Link>
                  {isApplied && (
                    <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                      <CheckCircle2 className="h-3 w-3" />
                      Đã nộp đơn
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-3 text-xs text-muted flex-wrap">
                  <span className="flex items-center gap-1 font-medium text-slate-700">
                    <Building2 className="h-3.5 w-3.5 text-muted shrink-0" />
                    <span className="truncate max-w-[200px]">{job.companyName}</span>
                  </span>
                  <span className="flex items-center gap-1">
                    <MapPin className="h-3.5 w-3.5 text-muted shrink-0" />
                    <span>{job.location}</span>
                  </span>
                  {job.createdAt && (
                    <span className="flex items-center gap-1 text-slate-400 hidden sm:flex">
                      <Calendar className="h-3.5 w-3.5 shrink-0" />
                      <span>
                        {new Date(job.createdAt).toLocaleDateString("vi-VN")}
                      </span>
                    </span>
                  )}
                </div>

                {/* Tags Highlight: Mức lương & Hình thức & Cấp bậc */}
                <div className="flex items-center gap-2 flex-wrap pt-1">
                  <span className="inline-flex items-center gap-1 text-xs font-bold px-2.5 py-0.5 rounded-lg bg-emerald-50 text-emerald-700 border border-emerald-200/80">
                    <DollarSign className="h-3.5 w-3.5" />
                    {formatSalary(job.salaryMin, job.salaryMax)}
                  </span>

                  <Badge variant="verified" size="sm">
                    {job.jobType === "INTERNSHIP" ? "Thực tập" : "Toàn thời gian"}
                  </Badge>

                  {job.id && (
                    <span className="text-[11px] font-mono text-muted bg-slate-100 px-1.5 py-0.5 rounded">
                      {job.id}
                    </span>
                  )}
                </div>

                {/* Description snippet */}
                {job.description && (
                  <p className="text-xs text-muted line-clamp-2 leading-relaxed pt-1">
                    {job.description}
                  </p>
                )}

                {/* Skills tags */}
                {job.requiredSkills && job.requiredSkills.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1.5">
                    {job.requiredSkills.slice(0, 6).map((skill) => (
                      <span
                        key={skill}
                        className="text-[11px] font-medium px-2 py-0.5 rounded-md bg-slate-100/90 text-slate-700 border border-slate-200 hover:bg-slate-200/80 transition-colors"
                      >
                        {skill}
                      </span>
                    ))}
                    {job.requiredSkills.length > 6 && (
                      <span className="text-[10px] font-semibold text-muted px-1.5 py-0.5 self-center">
                        +{job.requiredSkills.length - 6}
                      </span>
                    )}
                  </div>
                )}
              </div>
            </div>

            {/* 2. Right: Heart Toggle & Action Buttons */}
            <div className="flex md:flex-col items-center md:items-end justify-between md:justify-start gap-3 shrink-0 pt-3 md:pt-0 border-t md:border-t-0 border-border/60">
              {/* Heart icon button (Lưu việc làm) */}
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onToggleSave(job.id);
                }}
                aria-label={isSaved ? "Bỏ lưu việc làm" : "Lưu việc làm"}
                title={isSaved ? "Bỏ lưu việc làm" : "Lưu việc làm"}
                className={cn(
                  "p-2 rounded-xl transition-all cursor-pointer flex items-center justify-center",
                  isSaved
                    ? "bg-rose-50 text-rose-600 hover:bg-rose-100 shadow-2xs"
                    : "text-slate-400 hover:text-rose-600 hover:bg-rose-50/70"
                )}
              >
                <Heart
                  className={cn(
                    "h-5 w-5 transition-transform active:scale-125",
                    isSaved ? "fill-rose-600 text-rose-600" : ""
                  )}
                />
              </button>

              {/* Action buttons stack */}
              <div className="flex items-center md:flex-col gap-2 w-full sm:w-auto">
                {isApplied ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/candidate/jobs/${job.id}`);
                    }}
                    className="w-full md:w-32 justify-center text-xs"
                    rightIcon={<ChevronRight className="h-3.5 w-3.5" />}
                  >
                    Xem chi tiết
                  </Button>
                ) : (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation();
                      onApply(job);
                    }}
                    leftIcon={<Send className="h-3.5 w-3.5" />}
                    className="w-full md:w-32 justify-center text-xs shadow-xs hover:shadow-sm"
                  >
                    Ứng tuyển
                  </Button>
                )}

                <Button
                  variant="outline"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    onAnalyze(job.id);
                  }}
                  leftIcon={<Target className="h-3.5 w-3.5 text-primary" />}
                  className="w-full md:w-32 justify-center text-xs hover:border-primary/50 hover:text-primary"
                >
                  Phân tích
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }
);

JobCardItem.displayName = "JobCardItem";
