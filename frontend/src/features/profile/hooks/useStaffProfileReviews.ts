import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  profileApi,
  type StaffEditSelfDeclaredRequest,
  type StaffProfileReviewsParams,
} from "@/api/endpoints/profile.api";

// Query keys
export const staffProfileReviewKeys = {
  all: ["staffProfileReviews"] as const,
  list: (params: StaffProfileReviewsParams) => [...staffProfileReviewKeys.all, "list", params] as const,
  preview: (userId: string) => [...staffProfileReviewKeys.all, "preview", userId] as const,
};

/**
 * Hook to fetch staff profile reviews with filters and pagination
 */
export function useStaffProfileReviews(params: StaffProfileReviewsParams = {}) {
  return useQuery({
    queryKey: staffProfileReviewKeys.list(params),
    queryFn: () => profileApi.getStaffProfileReviews(params),
    staleTime: 30 * 1000, // 30 seconds
  });
}

/**
 * Hook to fetch a single candidate preview profile for staff
 */
export function useStaffPreviewProfile(userId: string, enabled = true) {
  return useQuery({
    queryKey: staffProfileReviewKeys.preview(userId),
    queryFn: () => profileApi.getStaffPreviewProfile(userId),
    enabled: enabled && !!userId,
    staleTime: 30 * 1000,
  });
}

/**
 * Hook to approve a candidate profile
 */
export function useApproveProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (userId: string) => profileApi.approveProfile(userId),
    onSuccess: (_, userId) => {
      // Invalidate staff profile reviews list
      queryClient.invalidateQueries({ queryKey: staffProfileReviewKeys.all });
      // Invalidate the specific preview
      queryClient.invalidateQueries({ queryKey: staffProfileReviewKeys.preview(userId) });
    },
  });
}

/**
 * Hook to reject a candidate profile
 */
export function useRejectProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, reason }: { userId: string; reason: string }) =>
      profileApi.rejectProfile(userId, reason),
    onSuccess: (_, { userId }) => {
      // Invalidate staff profile reviews list
      queryClient.invalidateQueries({ queryKey: staffProfileReviewKeys.all });
      // Invalidate the specific preview
      queryClient.invalidateQueries({ queryKey: staffProfileReviewKeys.preview(userId) });
    },
  });
}

/**
 * Hook to edit self-declared fields (headline, summary, self_skills) by staff
 */
export function useEditSelfDeclared() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ userId, data }: { userId: string; data: StaffEditSelfDeclaredRequest }) =>
      profileApi.editStaffSelfDeclared(userId, data),
    onSuccess: (_, { userId }) => {
      // Invalidate the specific preview
      queryClient.invalidateQueries({ queryKey: staffProfileReviewKeys.preview(userId) });
    },
  });
}