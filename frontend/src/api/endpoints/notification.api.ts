import { api } from "../client";
import type { ApiResponse, PaginatedData } from "../types/api";

export type NotificationType =
  | "APPLICATION_STATUS_CHANGED"
  | "INTERVIEW_INVITATION"
  | "INTERVIEW_SCHEDULED"
  | "INTERVIEW_REMINDER"
  | "OFFER_RECEIVED"
  | "OFFER_STATUS_CHANGED"
  | "PROFILE_REVIEWED"
  | "LMS_SYNC"
  | "JOB_REVIEWED"
  | "CONTACT_UNLOCKED"
  | "SYSTEM_ANNOUNCEMENT"
  | string;

export interface NotificationItem {
  id: string;
  userId: string;
  recipientRole?: string;
  type: NotificationType;
  title: string;
  body: string;
  actionUrl?: string;
  metadata?: string;
  readAt?: string | null;
  isRead: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface MarkAllReadResponse {
  updatedCount: number;
}

export interface NotificationQueryParams {
  unreadOnly?: boolean;
  page?: number;
  size?: number;
}

export const notificationApi = {
  /**
   * Lấy danh sách thông báo in-app của người dùng hiện tại
   */
  getMyNotifications(params?: NotificationQueryParams): Promise<ApiResponse<PaginatedData<NotificationItem>>> {
    return api.get<PaginatedData<NotificationItem>>("/notifications/me", {
      params,
      silentError: true,
    });
  },

  /**
   * Lấy số lượng thông báo chưa đọc cho biểu tượng cái chuông (Header/Navbar)
   */
  getUnreadCount(): Promise<ApiResponse<UnreadCountResponse>> {
    return api.get<UnreadCountResponse>("/notifications/unread-count", {
      silentError: true,
    });
  },

  /**
   * Đánh dấu 1 thông báo cụ thể là đã đọc
   */
  markAsRead(id: string): Promise<ApiResponse<NotificationItem>> {
    return api.put<NotificationItem>(`/notifications/${id}/read`);
  },

  /**
   * Đánh dấu tất cả thông báo hiện có là đã đọc
   */
  markAllAsRead(): Promise<ApiResponse<MarkAllReadResponse>> {
    return api.put<MarkAllReadResponse>("/notifications/read-all");
  },

  /**
   * Xóa một thông báo khỏi danh sách
   */
  deleteNotification(id: string): Promise<ApiResponse<void>> {
    return api.delete<void>(`/notifications/${id}`);
  },
};
