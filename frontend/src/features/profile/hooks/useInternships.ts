import { useQuery } from '@tanstack/react-query';
import type { Internship } from '@/api/types/evaluation';
import { MOCK_INTERNSHIPS } from '../mocks/internshipMock';

export function useInternships(userId: string | undefined) {
    return useQuery({
        queryKey: ['internships', userId],
        queryFn: async (): Promise<Internship[]> => {
            // KHI BE XONG: return client.get(`/api/v1/profiles/${userId}/internships`).then(res => res.data);

            // TẠM THỜI DÙNG MOCK:
            await new Promise((resolve) => setTimeout(resolve, 500));
            return MOCK_INTERNSHIPS;
        },
        enabled: !!userId,
    });
}