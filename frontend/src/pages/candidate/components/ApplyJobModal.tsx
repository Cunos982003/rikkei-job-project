import React, { useState } from "react";
import { Send, Briefcase, Building2, FileText, CheckCircle2, User } from "lucide-react";
import { Modal } from "../../../components/common/Modal";
import { Button } from "../../../components/common/Button";
import { useAuthStore } from "../../../store/useAuthStore";
import type { JobPostingItem } from "../../../api/endpoints/jobs.api";

interface ApplyJobModalProps {
  isOpen: boolean;
  job: JobPostingItem | null;
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: (jobId: string, coverLetter: string) => void;
}

interface ApplyJobModalContentProps {
  isOpen: boolean;
  job: JobPostingItem;
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: (jobId: string, coverLetter: string) => void;
}

const ApplyJobModalContent: React.FC<ApplyJobModalContentProps> = ({
  isOpen,
  job,
  isSubmitting,
  onClose,
  onSubmit,
}) => {
  const { user } = useAuthStore();
  const [coverLetter, setCoverLetter] = useState("");

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
            Xác nhận nộp đơn
          </Button>
        </div>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* 1. Job info summary */}
        <div className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-1">
          <p className="font-bold text-foreground flex items-center gap-1.5">
            <Briefcase className="h-3.5 w-3.5 text-primary" />
            {job.title}
          </p>
          <p className="text-muted flex items-center gap-1.5">
            <Building2 className="h-3 w-3" />
            {job.companyName} · {job.location}
          </p>
        </div>

        {/* 2. Synced LMS Profile & CV snapshot info */}
        <div className="p-3 rounded-xl bg-blue-50/70 border border-blue-200 text-xs space-y-2">
          <div className="flex items-center justify-between">
            <span className="font-bold text-blue-900 flex items-center gap-1.5">
              <User className="h-3.5 w-3.5 text-blue-700" />
              <span>Hồ sơ ứng viên từ Rikkei LMS</span>
            </span>
            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-800 flex items-center gap-1">
              <CheckCircle2 className="h-3 w-3 text-blue-600" />
              Đã đồng bộ
            </span>
          </div>

          <div className="grid grid-cols-2 gap-2 text-[11px] text-blue-900 bg-white/70 p-2 rounded-lg border border-blue-100">
            <div>
              <span className="text-muted block">Họ và tên:</span>
              <strong className="text-foreground">{user?.fullName || "Ứng viên Rikkei"}</strong>
            </div>
            <div>
              <span className="text-muted block">Email liên hệ:</span>
              <strong className="text-foreground truncate block">{user?.email || "candidate@email.com"}</strong>
            </div>
          </div>

          <div className="flex items-start gap-2 pt-1 text-blue-800 text-[11px]">
            <FileText className="h-3.5 w-3.5 shrink-0 mt-0.5 text-blue-600" />
            <p className="leading-relaxed">
              Hệ thống sẽ tự động đính kèm bản snapshot hồ sơ năng lực và chứng chỉ kỹ năng LMS của bạn vào hồ sơ ứng tuyển để chuyển trực tiếp đến nhà tuyển dụng.
            </p>
          </div>
        </div>

        {/* 3. Optional Cover letter */}
        <div>
          <label htmlFor="apply-cover-letter" className="block text-xs font-semibold text-foreground mb-1.5">
            Thư giới thiệu / Lời nhắn cho nhà tuyển dụng <span className="text-muted font-normal">(tuỳ chọn)</span>
          </label>
          <textarea
            id="apply-cover-letter"
            className="w-full border border-border rounded-xl px-3 py-2.5 text-xs text-foreground bg-surface resize-none focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all placeholder:text-muted"
            rows={4}
            maxLength={1000}
            placeholder="Chia sẻ ngắn gọn về kinh nghiệm, thế mạnh kỹ năng và mong muốn của bạn với vị trí này..."
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

export const ApplyJobModal: React.FC<ApplyJobModalProps> = ({
  isOpen,
  job,
  isSubmitting,
  onClose,
  onSubmit,
}) => {
  if (!isOpen || !job) return null;

  return (
    <ApplyJobModalContent
      key={job.id}
      isOpen={isOpen}
      job={job}
      isSubmitting={isSubmitting}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
};
