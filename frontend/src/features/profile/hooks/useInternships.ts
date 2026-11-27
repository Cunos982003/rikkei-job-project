
import { useQuery } from "@tanstack/react-query";
import { profileApi } from "@/api/endpoints/profile.api";
import type { Internship } from "@/api/types/evaluation";
import { candidateKeys } from "@/api/queryKeys";

export function useInternships(userId?: string) {
    return useQuery({
        queryKey: candidateKeys.profile.internships(userId),

        queryFn: async (): Promise<Internship[]> => {
            const response = userId
                ? await profileApi.getUserInternships(userId)
                : await profileApi.getMyInternships();

            if (!response.success) {
                throw new Error(
                    response.message || "Không thể tải dữ liệu thực tập"
                );
            }

            return response.data ?? [];
        },

        staleTime: 60_000,
        retry: 1,
    });
}
