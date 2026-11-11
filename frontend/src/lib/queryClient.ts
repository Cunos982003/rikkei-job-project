import { QueryClient } from "@tanstack/react-query";

/**
 * Cấu hình toàn cục cho TanStack Query v5 theo chuẩn dự án Rikkei Jobs
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 phút (tránh gọi lại API liên tục)
      gcTime: 1000 * 60 * 15, // 15 phút (giữ trong cache trước khi garbage collect)
      retry: 1, // Thử lại tối đa 1 lần nếu gặp lỗi mạng
      refetchOnWindowFocus: false, // Không tự refetch khi người dùng đổi tab trình duyệt
      refetchOnReconnect: true, // Tự refetch khi có mạng trở lại
    },
    mutations: {
      retry: 0, // Không tự động retry với các hành động tạo/sửa/xóa để tránh gửi đúp
    },
  },
});

