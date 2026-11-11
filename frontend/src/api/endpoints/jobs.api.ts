import { api } from "../client";
import type { ApiResponse, PaginatedData, PageParams } from "../types/api";

export interface JobPostingItem {
  id: string;
  title: string;
  companyId: string;
  companyName: string;
  location: string;
  salaryMin?: number;
  salaryMax?: number;
  jobType: "FULL_TIME" | "PART_TIME" | "INTERNSHIP" | "CONTRACT";
  requiredSkills: string[];
  description: string;
  status: "DRAFT" | "PENDING" | "PUBLISHED" | "CLOSED";
  createdAt: string;
}

export interface JobFilterParams extends PageParams {
  keyword?: string;
  location?: string;
  jobType?: string;
  status?: string;
}

/**
 * Endpoints tin tuyển dụng (job-service :8085 hoặc Mock :9000)
 */
export const jobsApi = {
  /**
   * Lấy danh sách việc làm có phân trang
   */
  getJobs(params?: JobFilterParams): Promise<ApiResponse<PaginatedData<JobPostingItem> | { items: JobPostingItem[]; total: number; page: number; size: number }>> {
    return api.get("/jobs", { params });
  },

  /**
   * Lấy chi tiết tin tuyển dụng theo ID
   */
  getJobById(id: string): Promise<ApiResponse<JobPostingItem>> {
    return api.get<JobPostingItem>(`/jobs/${id}`);
  },
};

