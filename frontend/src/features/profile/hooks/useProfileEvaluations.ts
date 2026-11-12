import { useQuery } from "@tanstack/react-query";
import { profileApi, type MentorEvaluation } from "@/api";

/**
 * Hook quản lý truy vấn danh sách phiếu đánh giá từ Mentor
 * @param userId ID của ứng viên (hoặc undefined nếu lấy đánh giá của chính mình)
 */
export function useProfileEvaluations(userId?: string) {
  return useQuery<MentorEvaluation[]>({
    queryKey: ["profile-evaluations", userId ?? "me"],
    queryFn: async () => {
      const res = await profileApi.getEvaluations(userId);
      return res.data;
    },
  });
}

