import { useQuery } from "@tanstack/react-query";
import { fetchRecommendations } from "../../../api/endpoints/career.api";
import { useAuthStore } from "../../../store/useAuthStore";

export function useRecommendations(limit: number) {
  const userId = useAuthStore((state) => state.user?.id);
  const token = useAuthStore((state) => state.accessToken);
  return useQuery({
    queryKey: ["career", "recommendations", userId, limit],
    queryFn: ({ signal }) => {
      if (!userId || !token) throw new Error("Bạn cần đăng nhập để xem đề xuất.");
      return fetchRecommendations(limit, token, signal);
    },
    enabled: Boolean(userId && token),
  });
}
