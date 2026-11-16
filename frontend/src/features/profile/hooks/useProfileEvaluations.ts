import { useQuery } from '@tanstack/react-query';
import type { MentorEvaluation } from '@/api/types/evaluation';
import { MOCK_MENTOR_EVALUATIONS } from '../mocks/evaluationMock'; // Trỏ đúng đường dẫn tới file mock bên trên

export function useProfileEvaluations(userId: string | undefined) {
  return useQuery({
    queryKey: ['profile-evaluations', userId],
    queryFn: async (): Promise<MentorEvaluation[]> => {
      // Giả lập độ trễ mạng 500ms
      await new Promise((resolve) => setTimeout(resolve, 500));

      // Trả về dữ liệu Mock
      return MOCK_MENTOR_EVALUATIONS;

      // KHI BE XONG:
      // return client.get(`/api/v1/profiles/${userId}/evaluations`).then(res => res.data);
    },
    enabled: !!userId,
  });
}