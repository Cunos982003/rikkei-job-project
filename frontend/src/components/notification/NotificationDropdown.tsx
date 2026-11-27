import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck, Loader2, Sparkles, Inbox } from "lucide-react";
import { notificationApi, type NotificationItem as NotificationItemType } from "../../api/endpoints/notification.api";
import { notificationKeys } from "../../api/queryKeys";
import { NotificationItem } from "./NotificationItem";
import { cn } from "../../utils/cn";

interface NotificationDropdownProps {
  onClose: () => void;
  unreadCount?: number;
}

export const NotificationDropdown: React.FC<NotificationDropdownProps> = ({
  onClose,
  unreadCount = 0,
}) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<"ALL" | "UNREAD">("ALL");

  // Lấy danh sách thông báo theo tab
  const { data: notificationsRes, isLoading } = useQuery({
    queryKey: notificationKeys.list({ unreadOnly: activeTab === "UNREAD" }),
    queryFn: () =>
      notificationApi.getMyNotifications({
        unreadOnly: activeTab === "UNREAD",
        page: 0,
        size: 30,
      }),
    staleTime: 15 * 1000,
  });

  const notifications = notificationsRes?.data?.items || [];

  // Mutation: Đánh dấu 1 thông báo đã đọc
  const markAsReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all() });
    },
  });

  // Mutation: Đánh dấu tất cả thông báo đã đọc
  const markAllAsReadMutation = useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all() });
    },
  });

  // Mutation: Xóa thông báo
  const deleteMutation = useMutation({
    mutationFn: (id: string) => notificationApi.deleteNotification(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.all() });
    },
  });

  const handleSelect = (item: NotificationItemType) => {
    if (!item.readAt && !item.isRead) {
      markAsReadMutation.mutate(item.id);
    }
    onClose();
    if (item.actionUrl) {
      navigate(item.actionUrl);
    }
  };

  const handleMarkAsRead = (item: NotificationItemType, e: React.MouseEvent) => {
    e.stopPropagation();
    markAsReadMutation.mutate(item.id);
  };

  const handleDelete = (item: NotificationItemType, e: React.MouseEvent) => {
    e.stopPropagation();
    deleteMutation.mutate(item.id);
  };

  return (
    <div
      className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl bg-surface border border-border shadow-2xl py-2 z-50 animate-in fade-in zoom-in-95 duration-150 overflow-hidden flex flex-col"
      style={{ maxHeight: "min(32rem, calc(100vh - 5rem))" }}
    >
      {/* Header */}
      <div className="px-4 py-3 border-b border-border-subtle flex items-center justify-between gap-2 shrink-0">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-bold text-foreground">Thông báo</h3>
          {unreadCount > 0 && (
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-primary-light text-primary border border-primary-border">
              {unreadCount} mới
            </span>
          )}
        </div>

        {unreadCount > 0 && (
          <button
            type="button"
            onClick={() => markAllAsReadMutation.mutate()}
            disabled={markAllAsReadMutation.isPending}
            className="text-xs font-medium text-primary hover:text-primary-hover flex items-center gap-1 transition-colors cursor-pointer disabled:opacity-50"
            title="Đánh dấu tất cả thông báo đã đọc"
          >
            {markAllAsReadMutation.isPending ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <CheckCheck className="h-3.5 w-3.5" />
            )}
            <span>Đã đọc tất cả</span>
          </button>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="px-3 pt-2 pb-1 border-b border-border-subtle flex items-center gap-2 shrink-0 bg-slate-50/50 dark:bg-slate-900/30">
        <button
          type="button"
          onClick={() => setActiveTab("ALL")}
          className={cn(
            "px-3 py-1 text-xs font-semibold rounded-lg transition-colors cursor-pointer",
            activeTab === "ALL"
              ? "bg-surface text-foreground shadow-xs border border-border"
              : "text-muted hover:text-foreground"
          )}
        >
          Tất cả
        </button>
        <button
          type="button"
          onClick={() => setActiveTab("UNREAD")}
          className={cn(
            "px-3 py-1 text-xs font-semibold rounded-lg transition-colors cursor-pointer flex items-center gap-1.5",
            activeTab === "UNREAD"
              ? "bg-surface text-foreground shadow-xs border border-border"
              : "text-muted hover:text-foreground"
          )}
        >
          <span>Chưa đọc</span>
          {unreadCount > 0 && (
            <span className="w-1.5 h-1.5 rounded-full bg-primary" />
          )}
        </button>
      </div>

      {/* Notifications List */}
      <div className="flex-1 overflow-y-auto p-2 space-y-1 divide-y divide-border-subtle/50 min-h-[140px]">
        {isLoading ? (
          <div className="py-12 flex flex-col items-center justify-center text-muted gap-2">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
            <p className="text-xs">Đang tải thông báo...</p>
          </div>
        ) : notifications.length === 0 ? (
          <div className="py-10 px-4 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-slate-800 text-muted flex items-center justify-center mb-3 shadow-inner">
              {activeTab === "UNREAD" ? (
                <Inbox className="h-6 w-6 text-muted-light" />
              ) : (
                <Bell className="h-6 w-6 text-muted-light" />
              )}
            </div>
            <p className="text-xs font-semibold text-foreground">
              {activeTab === "UNREAD"
                ? "Không có thông báo chưa đọc"
                : "Chưa có thông báo nào"}
            </p>
            <p className="text-[11px] text-muted max-w-[220px] mt-1 leading-relaxed">
              {activeTab === "UNREAD"
                ? "Tất cả thông báo tuyển dụng và hệ thống đã được xem."
                : "Các cập nhật về ứng tuyển, lịch phỏng vấn và đãi ngộ sẽ xuất hiện tại đây."}
            </p>
          </div>
        ) : (
          notifications.map((item) => (
            <div key={item.id} className="pt-1 first:pt-0">
              <NotificationItem
                item={item}
                onSelect={handleSelect}
                onMarkAsRead={handleMarkAsRead}
                onDelete={handleDelete}
              />
            </div>
          ))
        )}
      </div>

      {/* Footer info */}
      <div className="px-4 py-2 border-t border-border-subtle bg-slate-50/50 dark:bg-slate-900/30 flex items-center justify-between text-[11px] text-muted shrink-0">
        <span className="flex items-center gap-1">
          <Sparkles className="h-3 w-3 text-primary" />
          <span>Hệ thống thông báo Rikkei Jobs</span>
        </span>
        <button
          type="button"
          onClick={onClose}
          className="text-muted hover:text-foreground transition-colors cursor-pointer font-medium"
        >
          Đóng
        </button>
      </div>
    </div>
  );
};
