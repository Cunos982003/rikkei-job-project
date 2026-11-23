
import { useQuery } from "@tanstack/react-query";
import { profileApi } from "@/api/endpoints/profile.api";
import type { Internship } from "@/api/types/evaluation";

export function useInternships(userId?: string) {
    return useQuery({
        queryKey: ["internships", userId ?? "me"],

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
