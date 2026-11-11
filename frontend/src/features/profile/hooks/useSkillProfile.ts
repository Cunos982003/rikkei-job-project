import { useQuery } from "@tanstack/react-query";
import { profileApi, type SkillProfileItem } from "@/api";

/**
 * Hook quản lý truy vấn hồ sơ kỹ năng ứng viên
 * @param userId ID của ứng viên (hoặc undefined nếu lấy hồ sơ của chính mình)
 */
export function useSkillProfile(userId?: string) {
  return useQuery<SkillProfileItem[]>({
    queryKey: ["skill-profile", userId ?? "me"],
    queryFn: async () => {
      const res = userId
        ? await profileApi.getUserSkills(userId)
        : await profileApi.getMySkills();
      return res.data;
    },
  });
}

