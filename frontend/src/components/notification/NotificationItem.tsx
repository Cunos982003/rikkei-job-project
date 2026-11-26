import React from "react";
import {
  CalendarCheck,
  Gift,
  FileText,
  UserCheck,
  Briefcase,
  GraduationCap,
  Bell,
  Check,
  Trash2,
} from "lucide-react";
import type { NotificationItem as NotificationItemType } from "../../api/endpoints/notification.api";
import { cn } from "../../utils/cn";

interface NotificationItemProps {
  item: NotificationItemType;
  onSelect: (item: NotificationItemType) => void;
  onMarkAsRead: (item: NotificationItemType, e: React.MouseEvent) => void;
  onDelete: (item: NotificationItemType, e: React.MouseEvent) => void;
}

/**
 * Định dạng thời gian tương đối bằng tiếng Việt
 */
function formatRelativeTime(dateString: string): string {
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return "";

    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffSec < 60) return "Vừa xong";
    if (diffMin < 60) return `${diffMin} phút trước`;
    if (diffHour < 24) return `${diffHour} giờ trước`;
    if (diffDay < 7) return `${diffDay} ngày trước`;

    const day = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${day}/${month}/${year} ${hours}:${minutes}`;
  } catch {
    return "";
  }
}

/**
 * Icon và màu nền tương ứng theo nhóm nghiệp vụ của thông báo
 */
function getNotificationVisual(type: string) {
  const upper = (type || "").toUpperCase();

  if (upper.startsWith("INTERVIEW")) {
    return {
      icon: CalendarCheck,
      iconClass: "text-blue-600 bg-blue-50 dark:bg-blue-950/50 dark:text-blue-400 border border-blue-200 dark:border-blue-800/60",
    };
  }
  if (upper.startsWith("OFFER")) {
    return {
      icon: Gift,
      iconClass: "text-emerald-600 bg-emerald-50 dark:bg-emerald-950/50 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/60",
    };
  }
  if (upper.startsWith("APPLICATION")) {
    return {
      icon: FileText,
      iconClass: "text-indigo-600 bg-indigo-50 dark:bg-indigo-950/50 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800/60",
    };
  }
  if (upper.startsWith("PROFILE") || upper.startsWith("CONTACT")) {
    return {
      icon: UserCheck,
      iconClass: "text-teal-600 bg-teal-50 dark:bg-teal-950/50 dark:text-teal-400 border border-teal-200 dark:border-teal-800/60",
    };
  }
  if (upper.startsWith("JOB")) {
    return {
      icon: Briefcase,
      iconClass: "text-amber-600 bg-amber-50 dark:bg-amber-950/50 dark:text-amber-400 border border-amber-200 dark:border-amber-800/60",
    };
  }
  if (upper.startsWith("LMS")) {
    return {
      icon: GraduationCap,
      iconClass: "text-cyan-600 bg-cyan-50 dark:bg-cyan-950/50 dark:text-cyan-400 border border-cyan-200 dark:border-cyan-800/60",
    };
  }

  return {
    icon: Bell,
    iconClass: "text-primary bg-primary-light border border-primary-border",
  };
}

export const NotificationItem: React.FC<NotificationItemProps> = ({
  item,
  onSelect,
  onMarkAsRead,
  onDelete,
}) => {
  const isUnread = !item.readAt && !item.isRead;
  const { icon: VisualIcon, iconClass } = getNotificationVisual(item.type);
  const relativeTime = formatRelativeTime(item.createdAt);

  return (
    <div
      onClick={() => onSelect(item)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onSelect(item);
        }
      }}
      className={cn(
        "group relative flex items-start gap-3 p-3 rounded-xl transition-all duration-150 cursor-pointer text-left select-none",
        isUnread
          ? "bg-primary-50/40 hover:bg-primary-50/70 border border-primary-100/60 dark:bg-primary-950/20 dark:border-primary-900/40"
          : "hover:bg-slate-50 border border-transparent dark:hover:bg-slate-800/40"
      )}
    >
      {/* Icon danh mục */}
      <div
        className={cn(
          "w-9 h-9 rounded-xl flex items-center justify-center shrink-0 shadow-xs transition-transform group-hover:scale-105",
          iconClass
        )}
      >
        <VisualIcon className="h-4 w-4" />
      </div>

      {/* Nội dung thông báo */}
      <div className="flex-1 min-w-0 pr-6">
        <div className="flex items-center gap-1.5">
          <p
            className={cn(
              "text-xs leading-snug truncate",
              isUnread
                ? "font-bold text-foreground"
                : "font-semibold text-foreground/90"
            )}
          >
            {item.title}
          </p>
          {isUnread && (
            <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0 animate-pulse" />
          )}
        </div>

        <p className="text-xs text-muted leading-relaxed line-clamp-2 mt-0.5 break-words">
          {item.body}
        </p>

        {relativeTime && (
          <p className="text-[11px] text-muted-light mt-1.5 font-medium flex items-center gap-1">
            <span>{relativeTime}</span>
          </p>
        )}
      </div>

      {/* Nút hành động nhanh khi hover (Đánh dấu đã đọc / Xóa) */}
      <div
        className="absolute top-2.5 right-2 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity bg-surface/90 backdrop-blur-xs p-0.5 rounded-lg border border-border shadow-xs"
        onClick={(e) => e.stopPropagation()}
      >
        {isUnread && (
          <button
            type="button"
            title="Đánh dấu đã đọc"
            aria-label="Đánh dấu đã đọc"
            onClick={(e) => onMarkAsRead(item, e)}
            className="p-1 rounded-md text-muted hover:text-primary hover:bg-primary-light transition-colors cursor-pointer"
          >
            <Check className="h-3.5 w-3.5" />
          </button>
        )}
        <button
          type="button"
          title="Xóa thông báo"
          aria-label="Xóa thông báo"
          onClick={(e) => onDelete(item, e)}
          className="p-1 rounded-md text-muted hover:text-danger hover:bg-rose-50 transition-colors cursor-pointer"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
};
