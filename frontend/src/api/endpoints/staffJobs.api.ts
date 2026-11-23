import { api } from "../client";
import type { ApiResponse, PaginatedData } from "../types/api";
import type { JobResponse } from "./hrJobs.api";

export interface ListStaffJobsParams {
  status?: string;
  companyId?: string;
  page?: number;
  size?: number;
}

export const staffJobsApi = {
  /**
   * Lấy danh sách tin tuyển dụng cho Cán bộ (STAFF/ADMIN)
   * GET /api/v1/staff/jobs
   */
  getStaffJobs(params?: ListStaffJobsParams): Promise<ApiResponse<PaginatedData<JobResponse>>> {
    return api.get<PaginatedData<JobResponse>>("/staff/jobs", { params });
  },

  /**
   * Phê duyệt tin tuyển dụng
   * POST /api/v1/staff/jobs/{id}/approve
   */
  approveJob(id: string): Promise<ApiResponse<JobResponse>> {
    return api.post<JobResponse>(`/staff/jobs/${id}/approve`);
  },

  /**
   * Từ chối tin tuyển dụng kèm lý do
   * POST /api/v1/staff/jobs/{id}/reject
   */
  rejectJob(id: string, reason: string): Promise<ApiResponse<JobResponse>> {
    return api.post<JobResponse>(`/staff/jobs/${id}/reject`, { reason });
  },
};
