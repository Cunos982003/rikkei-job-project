import type React from "react";
import { Send, Target, CheckCircle2 } from "lucide-react";
import type { JobPostingItem } from "../../../api/endpoints/jobs.api";
import { Modal } from "../../../components/common/Modal";
import { Button } from "../../../components/common/Button";
import {
  LEVEL_LABELS,
  TYPE_LABELS,
  WORK_MODE_LABELS,
  formatDate,
  formatSalary,
} from "../../../utils/jobLabels";
import { cn } from "../../../utils/cn";
import { useAuthStore } from "../../../store/useAuthStore";
import { MatchScorePanel } from "../../../components/match/MatchScorePanel";

export interface JobDetailModalProps {
  job?: JobPostingItem | null;
  selectedJob?: JobPostingItem | null;
  isOpen?: boolean;
  onClose: () => void;
  onApply?: (job: JobPostingItem) => void;
  onAnalyze?: (jobId: string) => void;
  isApplied?: boolean;
  appliedJobIds?: Set<string>;
}

export const JobDetailModal: React.FC<JobDetailModalProps> = ({
  job,
  selectedJob,
  isOpen,
  onClose,
  onApply,
  onAnalyze,
  isApplied,
  appliedJobIds,
}) => {
  const { user } = useAuthStore();
  const isCandidate = user?.role === "CANDIDATE";

  const currentJob = job || selectedJob;
  const isModalOpen = isOpen !== undefined ? isOpen : !!currentJob;

  if (!currentJob) return null;

  const isJobApplied =
    isApplied !== undefined
      ? isApplied
      : appliedJobIds
        ? appliedJobIds.has(currentJob.id)
        : false;

  const levelVal = currentJob.level || currentJob.experienceLevel;
  const levelLabel = levelVal ? LEVEL_LABELS[levelVal] || levelVal : null;
  const typeLabel = currentJob.jobType ? TYPE_LABELS[currentJob.jobType] || currentJob.jobType : null;
  const workModeLabel = currentJob.workMode
    ? WORK_MODE_LABELS[currentJob.workMode] || currentJob.workMode
    : null;
  const formattedExpiresAt = currentJob.expiresAt ? formatDate(currentJob.expiresAt) : null;

  return (
    <Modal
      isOpen={isModalOpen}
      onClose={onClose}
      title={currentJob.title}
      description={currentJob.companyName}
      size="lg"
      footer={
        <div className="flex items-center justify-between w-full">
          <span className="text-xs font-semibold text-emerald-700">
            Lương: {formatSalary(currentJob.salaryMin, currentJob.salaryMax)}
          </span>
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={onClose}>
              Đóng
            </Button>
            {onAnalyze && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onAnalyze(currentJob.id)}
                leftIcon={<Target className="h-4 w-4" />}
              >
                Phân tích
              </Button>
            )}
            {isJobApplied ? (
              <div className="flex items-center gap-1.5 text-xs font-medium text-emerald-700 px-3 py-1.5 rounded-lg bg-emerald-50 border border-emerald-200">
                <CheckCircle2 className="h-3.5 w-3.5" />
                Đã nộp đơn
              </div>
            ) : onApply ? (
              <Button
                variant="primary"
                size="sm"
                onClick={() => onApply(currentJob)}
                leftIcon={<Send className="h-4 w-4" />}
              >
                Ứng tuyển
              </Button>
            ) : null}
          </div>
        </div>
      }
    >
      <div className="space-y-4 text-sm text-foreground">
        {/* Lưới thông tin chung */}
        <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
          {currentJob.position && (
            <div>
              <span className="text-muted block">Vị trí:</span>
              <span className="font-semibold">{currentJob.position}</span>
            </div>
          )}
          {levelLabel && (
            <div>
              <span className="text-muted block">Cấp bậc:</span>
              <span className="font-semibold">{levelLabel}</span>
            </div>
          )}
          {typeLabel && (
            <div>
              <span className="text-muted block">Hình thức:</span>
              <span className="font-semibold">{typeLabel}</span>
            </div>
          )}
          {workModeLabel && (
            <div>
              <span className="text-muted block">Hình thức làm việc:</span>
              <span className="font-semibold">{workModeLabel}</span>
            </div>
          )}
          {currentJob.quantity != null && (
            <div>
              <span className="text-muted block">Số lượng tuyển:</span>
              <span className="font-semibold">{currentJob.quantity} người</span>
            </div>
          )}
          {formattedExpiresAt && (
            <div>
              <span className="text-muted block">Hạn nộp:</span>
              <span className="font-semibold">{formattedExpiresAt}</span>
            </div>
          )}
          {currentJob.location && (
            <div className="col-span-2 sm:col-span-3">
              <span className="text-muted block">Địa điểm:</span>
              <span className="font-semibold">{currentJob.location}</span>
            </div>
          )}
        </div>

        {/* Mô tả công việc */}
        {currentJob.description && (
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              Mô tả công việc:
            </h4>
            <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
              {currentJob.description}
            </p>
          </div>
        )}

        {/* Yêu cầu ứng viên */}
        {currentJob.requirements && currentJob.requirements.trim() ? (
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              Yêu cầu ứng viên:
            </h4>
            <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
              {currentJob.requirements}
            </p>
          </div>
        ) : null}

        {/* Quyền lợi */}
        {currentJob.benefits && currentJob.benefits.trim() ? (
          <div>
            <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              Quyền lợi:
            </h4>
            <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
              {currentJob.benefits}
            </p>
          </div>
        ) : null}

        {/* Kỹ năng yêu cầu */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-2">
            Kỹ năng yêu cầu:
          </h4>
          {currentJob.jobSkills && currentJob.jobSkills.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {currentJob.jobSkills.map((sk) => {
                const isMandatory = sk.required ?? sk.isMandatory ?? true;
                return (
                  <div
                    key={sk.skillId || sk.skillName}
                    className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-md bg-slate-50 border border-slate-200"
                  >
                    <span className="font-medium text-foreground">{sk.skillName}</span>
                    <span className="text-muted text-[11px]">(tối thiểu {sk.minScore} điểm)</span>
                    <span
                      className={cn(
                        "text-[10px] font-semibold px-1.5 py-0.5 rounded",
                        isMandatory
                          ? "bg-rose-50 text-rose-700 border border-rose-200"
                          : "bg-blue-50 text-blue-700 border border-blue-200"
                      )}
                    >
                      {isMandatory ? "Bắt buộc" : "Ưu tiên"}
                    </span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {currentJob.requiredSkills?.map((skill) => (
                <span
                  key={skill}
                  className="text-xs font-medium px-2.5 py-1 rounded-md bg-primary-light text-primary border border-primary-border"
                >
                  {skill}
                </span>
              ))}
            </div>
          )}
        </div>

        {/* DN-06 Match Score Panel - Candidate Only */}
        {isCandidate && <MatchScorePanel jobId={currentJob.id} mode="me" enabled={isModalOpen} />}
      </div>
    </Modal>
  );
};

export default JobDetailModal;