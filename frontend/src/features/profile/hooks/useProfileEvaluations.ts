import { useQuery } from '@tanstack/react-query';
import type { MentorEvaluation } from '@/api/types/evaluation';
import { MOCK_MENTOR_EVALUATIONS } from '../mocks/evaluationMock';

export function useProfileEvaluations(userId: string | undefined) {
  return useQuery({
    queryKey: ['profile-evaluations', userId],
    queryFn: async (): Promise<MentorEvaluation[]> => {
      await new Promise((resolve) => setTimeout(resolve, 500));
      return MOCK_MENTOR_EVALUATIONS as any;
    },
    enabled: !!userId,
  });
}