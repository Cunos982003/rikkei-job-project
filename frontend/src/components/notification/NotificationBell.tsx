import React, { useState, useRef, useEffect } from "react";
import { Bell } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { notificationApi } from "../../api/endpoints/notification.api";
import { notificationKeys } from "../../api/queryKeys";
import { useAuthStore } from "../../store/useAuthStore";
import { NotificationDropdown } from "./NotificationDropdown";
import { cn } from "../../utils/cn";

interface NotificationBellProps {
  className?: string;
}

export const NotificationBell: React.FC<NotificationBellProps> = ({ className }) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const { isAuthenticated } = useAuthStore();

  // Lấy số lượng thông báo chưa đọc theo chu kỳ định kỳ (60s)
  const { data: unreadRes } = useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: () =>
      notificationApi
        .getUnreadCount()
        .catch(() => ({
          success: true,
          statusCode: 200,
          message: "",
          data: { unreadCount: 0 },
          timestamp: "",
        })),
    refetchInterval: 60 * 1000,
    staleTime: 30 * 1000,
    enabled: isAuthenticated,
    retry: false,
  });

  const unreadCount = unreadRes?.data?.unreadCount ?? 0;

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(e.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [isOpen]);

  return (
    <div className={cn("relative", className)} ref={containerRef}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-label="Thông báo hệ thống"
        aria-expanded={isOpen}
        className={cn(
          "relative p-2 text-muted hover:text-foreground hover:bg-slate-100 rounded-lg transition-colors cursor-pointer",
          isOpen && "bg-slate-100 text-foreground"
        )}
      >
        <Bell className="h-5 w-5" />
        {/* Chỉ hiển thị chấm đỏ khi có thông báo chưa đọc (unreadCount > 0) */}
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-primary" />
        )}
      </button>

      {/* Popover menu hiển thị danh sách thông báo */}
      {isOpen && (
        <NotificationDropdown
          onClose={() => setIsOpen(false)}
          unreadCount={unreadCount}
        />
      )}
    </div>
  );
};
