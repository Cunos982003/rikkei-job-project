import { useQuery } from "@tanstack/react-query";
import { api } from "../client";
import type { SkillProfileResponse } from "../types/skill";

export function useSkillProfile(userId?: string) {
  const endpoint = userId ? `/profiles/${userId}/skills` : "/profiles/me/skills";

  return useQuery({
    queryKey: ["skillProfile", userId || "me"],
    queryFn: async (): Promise<SkillProfileResponse[]> => {
      const response = await api.get<SkillProfileResponse[]>(endpoint);
      return response.data;
    },
    enabled: !!userId || true, // always enabled for "me", only when userId provided for others
    staleTime: 1000 * 60 * 5, // 5 minutes
    retry: 1,
    refetchOnWindowFocus: false,
  });
}