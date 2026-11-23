import React from "react";
import {
  Building2,
  MapPin,
  DollarSign,
  Send,
  Target,
  Eye,
  CheckCircle2,
} from "lucide-react";
import { Card, CardContent } from "../../../components/common/Card";
import { Button } from "../../../components/common/Button";
import { Badge } from "../../../components/common/Badge";
import type { JobPostingItem } from "../../../api/endpoints/jobs.api";

interface JobCardItemProps {
  job: JobPostingItem;
  isApplied: boolean;
  onApply: (job: JobPostingItem) => void;
  onAnalyze: (jobId: string) => void;
  onViewDetail: (job: JobPostingItem) => void;
  formatSalary: (min?: number, max?: number) => string;
}

export const JobCardItem = React.memo<JobCardItemProps>(
  ({ job, isApplied, onApply, onAnalyze, onViewDetail, formatSalary }) => {
    return (
      <Card hoverable className="border-border">
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

              <h3
                className="text-lg font-bold text-foreground leading-tight hover:text-primary transition-colors cursor-pointer"
                onClick={() => onViewDetail(job)}
              >
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
            <div className="flex flex-col sm:flex-row lg:flex-col items-stretch sm:items-center lg:items-end justify-center gap-2 pt-3 lg:pt-0 border-t lg:border-t-0 border-border/60 shrink-0 w-full sm:w-auto">
              {isApplied ? (
                <div className="w-full sm:w-32 lg:w-32 h-8 inline-flex items-center justify-center gap-1.5 text-xs font-medium text-emerald-600 bg-emerald-50 border border-emerald-200 rounded-lg select-none">
                  <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
                  Đã nộp đơn
                </div>
              ) : (
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => onApply(job)}
                  leftIcon={<Send className="h-3.5 w-3.5" />}
                  className="w-full sm:w-32 lg:w-32 justify-center shadow-xs hover:shadow-sm active:scale-[0.98] transition-all"
                >
                  Ứng tuyển
                </Button>
              )}
              <Button
                variant="outline"
                size="sm"
                onClick={() => onAnalyze(job.id)}
                leftIcon={<Target className="h-3.5 w-3.5 text-primary" />}
                className="w-full sm:w-32 lg:w-32 justify-center hover:border-primary/50 hover:text-primary active:scale-[0.98] transition-all"
              >
                Phân tích
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onViewDetail(job)}
                leftIcon={<Eye className="h-3.5 w-3.5" />}
                className="w-full sm:w-32 lg:w-32 justify-center hover:border-primary/50 hover:text-primary active:scale-[0.98] transition-all"
              >
                Xem chi tiết
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }
);

JobCardItem.displayName = "JobCardItem";

