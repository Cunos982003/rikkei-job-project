import { useQuery } from '@tanstack/react-query';
import type { MentorEvaluation } from '@/api/types/evaluation';
import { candidateKeys } from '@/api/queryKeys';
import { profileApi } from '@/api/endpoints/profile.api';

export function useProfileEvaluations(userId: string | undefined) {
  return useQuery<MentorEvaluation[]>({
    queryKey: candidateKeys.profile.evaluations(userId),
    queryFn: async (): Promise<MentorEvaluation[]> => {
      try {
        const response = await profileApi.getEvaluations(userId);
        return response.data || [];
      } catch (err) {
        console.warn("Không thể tải đánh giá từ mentor (có thể backend chưa triển khai endpoint):", err);
        return [];
      }
    },
    enabled: !!userId,
    staleTime: 60 * 1000,
  });
}
