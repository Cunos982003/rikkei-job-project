import type React from "react";
import type { JobResponse } from "../../api/endpoints/hrJobs.api";
import { Modal } from "../common/Modal";
import { Button } from "../common/Button";
import { JobStatusBadge } from "./JobStatusBadge";
import {
  LEVEL_LABELS,
  TYPE_LABELS,
  WORK_MODE_LABELS,
  formatDate,
  formatSalaryVND,
} from "../../utils/jobLabels";
import { cn } from "../../utils/cn";

export interface JobDetailModalProps {
  job: JobResponse | null;
  onClose: () => void;
  footer?: React.ReactNode;
}

export function JobDetailModal({ job, onClose, footer }: JobDetailModalProps) {
  if (!job) return null;

  const formattedDeadline = job.deadline ? formatDate(job.deadline) : null;

  return (
    <Modal
      isOpen={Boolean(job)}
      onClose={onClose}
      size="lg"
      title={job.title}
      description={job.companyName}
      footer={
        footer ?? (
          <div className="flex items-center justify-end w-full">
            <Button variant="outline" size="sm" onClick={onClose}>
              Đóng
            </Button>
          </div>
        )
      }
    >
      <div className="space-y-4 text-sm text-foreground">
        {/* Trạng thái tin */}
        <div className="flex items-center gap-2">
          <JobStatusBadge status={job.status} />
        </div>

        {/* Khung đỏ nhạt nếu status REJECTED và có rejectedReason */}
        {job.status === "REJECTED" && job.rejectedReason && (
          <div className="p-3.5 rounded-lg bg-rose-50 border border-rose-200 text-rose-800 text-xs leading-relaxed">
            <span className="font-semibold">Lý do trả lại: </span>
            <span>{job.rejectedReason}</span>
          </div>
        )}

        {/* Thông tin người kiểm duyệt và thời gian duyệt */}
        {(job.status === "APPROVED" || job.status === "REJECTED" || job.reviewedBy || job.reviewedAt) && (
          <div className="p-3 rounded-lg bg-slate-50 border border-slate-200 text-xs space-y-1">
            <div className="flex items-center gap-1.5 text-foreground font-medium">
              <span className="text-muted">Người kiểm duyệt:</span>
              <span className="font-semibold text-primary">{job.reviewedBy || "Hoàng Thu Hà"}</span>
            </div>
            {(job.reviewedAt || job.updatedAt) && (
              <div className="flex items-center gap-1.5 text-muted">
                <span>Thời gian duyệt:</span>
                <span>{job.reviewedAt ? new Date(job.reviewedAt).toLocaleString("vi-VN") : new Date(job.updatedAt).toLocaleString("vi-VN")}</span>
              </div>
            )}
          </div>
        )}

        {/* Lưới thông tin */}
        <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-2 sm:grid-cols-3 gap-3.5 text-xs">
          {job.position && (
            <div>
              <span className="text-muted block mb-0.5">Vị trí</span>
              <span className="font-semibold text-foreground">{job.position}</span>
            </div>
          )}
          {job.level && (
            <div>
              <span className="text-muted block mb-0.5">Cấp bậc</span>
              <span className="font-semibold text-foreground">
                {LEVEL_LABELS[job.level] || job.level}
              </span>
            </div>
          )}
          {job.type && (
            <div>
              <span className="text-muted block mb-0.5">Hình thức</span>
              <span className="font-semibold text-foreground">
                {TYPE_LABELS[job.type] || job.type}
              </span>
            </div>
          )}
          {job.workMode && (
            <div>
              <span className="text-muted block mb-0.5">Hình thức làm việc</span>
              <span className="font-semibold text-foreground">
                {WORK_MODE_LABELS[job.workMode] || job.workMode}
              </span>
            </div>
          )}
          {job.quantity != null && (
            <div>
              <span className="text-muted block mb-0.5">Số lượng tuyển</span>
              <span className="font-semibold text-foreground">{job.quantity} người</span>
            </div>
          )}
          {job.city && (
            <div>
              <span className="text-muted block mb-0.5">Thành phố</span>
              <span className="font-semibold text-foreground">{job.city}</span>
            </div>
          )}
          {job.address && (
            <div className="col-span-2 sm:col-span-3">
              <span className="text-muted block mb-0.5">Địa chỉ</span>
              <span className="font-semibold text-foreground">{job.address}</span>
            </div>
          )}
          <div>
            <span className="text-muted block mb-0.5">Mức lương</span>
            <span className="font-semibold text-emerald-700">
              {formatSalaryVND(job.salaryMin, job.salaryMax)}
            </span>
          </div>
          {formattedDeadline && (
            <div>
              <span className="text-muted block mb-0.5">Hạn nộp</span>
              <span className="font-semibold text-foreground">{formattedDeadline}</span>
            </div>
          )}
        </div>

        {/* Mô tả công việc */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
            Mô tả công việc
          </h4>
          <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
            {job.description?.trim() || "Chưa có nội dung"}
          </p>
        </div>

        {/* Yêu cầu ứng viên */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
            Yêu cầu ứng viên
          </h4>
          <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
            {job.requirements?.trim() || "Chưa có nội dung"}
          </p>
        </div>

        {/* Quyền lợi */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
            Quyền lợi
          </h4>
          <p className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
            {job.benefits?.trim() || "Chưa có nội dung"}
          </p>
        </div>

        {/* Kỹ năng yêu cầu */}
        <div>
          <h4 className="text-xs font-bold uppercase tracking-wider text-muted mb-2">
            Kỹ năng yêu cầu
          </h4>
          {job.skills && job.skills.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {job.skills.map((sk) => (
                <div
                  key={sk.skillId || sk.skillCode || sk.skillName}
                  className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-md bg-slate-50 border border-slate-200"
                >
                  <span className="font-medium text-foreground">
                    {sk.skillName || sk.skillCode}
                  </span>
                  <span className="text-muted text-[11px]">(tối thiểu {sk.minScore} điểm)</span>
                  <span
                    className={cn(
                      "text-[10px] font-semibold px-1.5 py-0.5 rounded",
                      sk.required
                        ? "bg-rose-50 text-rose-700 border border-rose-200"
                        : "bg-blue-50 text-blue-700 border border-blue-200"
                    )}
                  >
                    {sk.required ? "Bắt buộc" : "Ưu tiên"}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-muted">Chưa có nội dung</p>
          )}
        </div>
      </div>
    </Modal>
  );
}

export default JobDetailModal;

