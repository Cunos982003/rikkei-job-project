import { type ReactNode } from "react";
import { Inbox } from "lucide-react";
import { cn } from "../../utils/cn";

export interface EmptyStateProps {
  title?: string;
  description?: string;
  icon?: ReactNode;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({
  title = "Không tìm thấy dữ liệu",
  description = "Hiện tại chưa có bản ghi nào phù hợp với yêu cầu của bạn.",
  icon,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center p-8 sm:p-12 text-center rounded-xl border border-dashed border-border bg-slate-50/50",
        className
      )}
    >
      <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center text-muted mb-4 shrink-0">
        {icon || <Inbox className="h-6 w-6" />}
      </div>
      <h4 className="text-base font-semibold text-foreground mb-1">{title}</h4>
      <p className="text-xs text-muted max-w-sm leading-relaxed mb-5">{description}</p>
      {action && <div>{action}</div>}
    </div>
  );
}

