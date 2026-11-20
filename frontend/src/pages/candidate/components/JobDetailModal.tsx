import React from "react";
import { Send, Target, CheckCircle2 } from "lucide-react";
import { Modal } from "../../../components/common/Modal";
import { Button } from "../../../components/common/Button";
import type { JobPostingItem } from "../../../api/endpoints/jobs.api";

interface JobDetailModalProps {
  job: JobPostingItem | null;
  isApplied: boolean;
  onClose: () => void;
  onApply: (job: JobPostingItem) => void;
  onAnalyze: (jobId: string) => void;
  formatSalary: (min?: number, max?: number) => string;
}

export const JobDetailModal: React.FC<JobDetailModalProps> = ({
  job,
  isApplied,
  onClose,
  onApply,
  onAnalyze,
  formatSalary,
}) => {
  if (!job) return null;

  return (
    <Modal
      isOpen={Boolean(job)}
      onClose={onClose}
      title={job.title}
      description={job.companyName}
      size="lg"
      footer={
        <div className="flex items-center justify-between w-full">
          <span className="text-xs font-semibold text-emerald-700">
            Lương: {formatSalary(job.salaryMin, job.salaryMax)}
          </span>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={onClose}>
              Đóng
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => onAnalyze(job.id)}
              leftIcon={<Target className="h-4 w-4 text-primary" />}
            >
              Phân tích
            </Button>
            {isApplied ? (
              <div className="flex items-center gap-1.5 text-xs font-medium text-emerald-700 px-3 py-1.5 rounded-lg bg-emerald-50 border border-emerald-200">
                <CheckCircle2 className="h-3.5 w-3.5" />
                Đã nộp đơn
              </div>
            ) : (
              <Button
                variant="primary"
                size="sm"
                onClick={() => onApply(job)}
                leftIcon={<Send className="h-4 w-4" />}
              >
                Ứng tuyển
              </Button>
            )}
          </div>
        </div>
      }
    >
      <div className="space-y-4 text-sm text-foreground">
        <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 grid grid-cols-2 gap-3 text-xs">
          <div>
            <span className="text-muted block">Địa điểm làm việc:</span>
            <span className="font-semibold">{job.location}</span>
          </div>
          <div>
            <span className="text-muted block">Hình thức làm việc:</span>
            <span className="font-semibold">
              {job.jobType === "INTERNSHIP" ? "Thực tập" : "Toàn thời gian"}
            </span>
          </div>
        </div>

        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
            Mô tả công việc:
          </h4>
          <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
            {job.description}
          </p>
        </div>

        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-2">
            Kỹ năng yêu cầu:
          </h4>
          <div className="flex flex-wrap gap-1.5">
            {job.requiredSkills?.map((skill) => (
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
    </Modal>
  );
};

