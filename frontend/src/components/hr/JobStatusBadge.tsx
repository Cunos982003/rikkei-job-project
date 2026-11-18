import type { JobStatus } from "../../api/endpoints/hrJobs.api";
import { cn } from "../../utils/cn";

export interface JobStatusBadgeProps {
  status: JobStatus;
  className?: string;
}

export function JobStatusBadge({ status, className }: JobStatusBadgeProps) {
  const configs: Record<
    JobStatus,
    { label: string; bg: string; text: string; border: string }
  > = {
    DRAFT: {
      label: "Nháp",
      bg: "bg-slate-100",
      text: "text-slate-700",
      border: "border-slate-200",
    },
    PENDING: {
      label: "Chờ duyệt",
      bg: "bg-amber-50",
      text: "text-amber-800",
      border: "border-amber-200",
    },
    APPROVED: {
      label: "Đã duyệt",
      bg: "bg-emerald-50",
      text: "text-emerald-800",
      border: "border-emerald-200",
    },
    REJECTED: {
      label: "Bị trả lại",
      bg: "bg-rose-50",
      text: "text-rose-800",
      border: "border-rose-200",
    },
    CLOSED: {
      label: "Đã đóng",
      bg: "bg-slate-200",
      text: "text-slate-800",
      border: "border-slate-300",
    },
  };

  const config = configs[status] || configs.DRAFT;

  return (
    <span
      className={cn(
        "inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold border select-none transition-colors",
        config.bg,
        config.text,
        config.border,
        className
      )}
    >
      {config.label}
    </span>
  );
}
