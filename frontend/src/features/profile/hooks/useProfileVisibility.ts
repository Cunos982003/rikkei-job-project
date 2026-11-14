import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { profileApi } from "../../../api/endpoints/profile.api";

/**
 * Lịch sử duyệt hồ sơ (HS-05). Chỉ tải khi người dùng thực sự cần xem.
 */
export function useReviewHistory(enabled = true) {
  return useQuery({
    queryKey: ["reviewHistory"],
    queryFn: () => profileApi.getReviewHistory(),
    staleTime: 30 * 1000,
    enabled,
  });
}

/**
 * Gửi duyệt / rút công khai. Sau khi thành công, làm mới cả hồ sơ lẫn lịch sử
 * để trạng thái và bảng lịch sử luôn khớp nhau.
 */
export function usePublishProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (consent: boolean) => profileApi.publishProfile(consent),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myProfile"] });
      queryClient.invalidateQueries({ queryKey: ["reviewHistory"] });
    },
  });
}

export function useWithdrawProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => profileApi.withdrawProfile(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["myProfile"] });
      queryClient.invalidateQueries({ queryKey: ["reviewHistory"] });
    },
  });
}
