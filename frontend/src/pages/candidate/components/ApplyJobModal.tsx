import React, { useState, useEffect } from "react";
import { Send, Briefcase, Building2, FileText } from "lucide-react";
import { Modal } from "../../../components/common/Modal";
import { Button } from "../../../components/common/Button";
import type { JobPostingItem } from "../../../api/endpoints/jobs.api";

interface ApplyJobModalProps {
  isOpen: boolean;
  job: JobPostingItem | null;
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: (jobId: string, coverLetter: string) => void;
}

export const ApplyJobModal: React.FC<ApplyJobModalProps> = ({
  isOpen,
  job,
  isSubmitting,
  onClose,
  onSubmit,
}) => {
  const [coverLetter, setCoverLetter] = useState("");

  // Reset form khi mở modal mới hoặc đóng modal
  useEffect(() => {
    if (isOpen) {
      setCoverLetter("");
    }
  }, [isOpen, job?.id]);

  if (!isOpen || !job) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(job.id, coverLetter.trim());
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Nộp đơn ứng tuyển"
      description={`${job.title} · ${job.companyName}`}
      size="md"
      footer={
        <div className="flex items-center justify-end gap-2 w-full">
          <Button
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={isSubmitting}
            type="button"
          >
            Hủy
          </Button>
          <Button
            variant="primary"
            size="sm"
            isLoading={isSubmitting}
            onClick={handleSubmit}
            leftIcon={<Send className="h-4 w-4" />}
            type="button"
          >
            Nộp đơn
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Job info summary */}
        <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 text-xs space-y-1">
          <p className="font-semibold text-foreground flex items-center gap-1.5">
            <Briefcase className="h-3.5 w-3.5 text-primary" />
            {job.title}
          </p>
          <p className="text-muted flex items-center gap-1.5">
            <Building2 className="h-3 w-3" />
            {job.companyName} · {job.location}
          </p>
        </div>

        {/* CV automatic snapshot notice */}
        <div className="flex items-start gap-2 p-3 rounded-lg bg-blue-50 border border-blue-200 text-xs text-blue-800">
          <FileText className="h-4 w-4 shrink-0 mt-0.5 text-blue-600" />
          <div>
            <p className="font-semibold mb-0.5">CV của bạn sẽ được đính kèm tự động</p>
            <p className="text-blue-700 opacity-90 leading-relaxed">
              Hệ thống sẽ lưu bản snapshot CV hiện tại của bạn tại thời điểm nộp để HR xem đúng bản đã gửi.
            </p>
          </div>
        </div>

        {/* Cover letter field */}
        <div>
          <label htmlFor="apply-cover-letter" className="block text-xs font-medium text-foreground mb-1.5">
            Thư giới thiệu <span className="text-muted font-normal">(tuỳ chọn)</span>
          </label>
          <textarea
            id="apply-cover-letter"
            className="w-full border border-border rounded-lg px-3 py-2.5 text-sm text-foreground bg-surface resize-none focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all"
            rows={5}
            maxLength={1000}
            placeholder="Giới thiệu bản thân và lý do bạn phù hợp với vị trí này..."
            value={coverLetter}
            onChange={(e) => setCoverLetter(e.target.value)}
          />
          <div className="flex items-center justify-between text-[11px] text-muted mt-1">
            <span>Tối đa 1000 ký tự</span>
            <span className={coverLetter.length >= 950 ? "text-amber-600 font-semibold" : ""}>
              {coverLetter.length}/1000 ký tự
            </span>
          </div>
        </div>
      </form>
    </Modal>
  );
};

