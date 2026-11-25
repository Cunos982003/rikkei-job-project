import { api } from "../client";
import type { ApiResponse } from "../types/api";
import type { JobPostingItem } from "./jobs.api";

export interface SavedJobItem extends JobPostingItem {
  savedAt?: string;
}

export interface ToggleSaveJobResponse {
  jobId: string;
  saved: boolean;
}

/**
 * Endpoints dành riêng cho các tính năng tìm kiếm và lưu trữ việc làm của Candidate
 */
export const candidateJobsApi = {
  /**
   * Lấy danh sách việc làm mà ứng viên đã bấm lưu / thả tim
   */
  getSavedJobs(): Promise<ApiResponse<SavedJobItem[]>> {
    return api.get<SavedJobItem[]>("/candidate/saved-jobs", { silentError: true });
  },

  /**
   * Lưu việc làm (thả tim)
   */
  saveJob(jobId: string): Promise<ApiResponse<ToggleSaveJobResponse>> {
    return api.post<ToggleSaveJobResponse>("/candidate/saved-jobs", { jobId });
  },

  /**
   * Bỏ lưu việc làm
   */
  unsaveJob(jobId: string): Promise<ApiResponse<ToggleSaveJobResponse>> {
    return api.delete<ToggleSaveJobResponse>(`/candidate/saved-jobs/${jobId}`);
  },
};

