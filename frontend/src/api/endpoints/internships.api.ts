import api from "../client";
import type { ApiResponse } from "../types/api";
import type { InternshipDemand, CompanyIntern } from "../types/internship";

export interface AssignPlacementRequest {
  userId: string;
  jobId: string;
  batchName: string;
}

export interface AssignPlacementResponse {
  success: boolean;
  message: string;
  userId?: string;
  batchName?: string;
  assignedAt?: string;
}

export const internshipsApi = {
  /**
   * Lấy danh sách thực tập sinh thuộc công ty của HR
   */
  getCompanyInterns(): Promise<ApiResponse<CompanyIntern[]>> {
    return api.get<CompanyIntern[]>("/hr/interns");
  },

  /**
   * Lấy danh sách nhu cầu thực tập và ứng viên chờ ghép đợt (dành cho Cán bộ vận hành Staff)
   */
  getPlacementDemands(): Promise<ApiResponse<InternshipDemand[]>> {
    return api.get<InternshipDemand[]>("/staff/placements");
  },

  /**
   * Ghép ứng viên trúng tuyển vào một đợt thực tập IOC cụ thể
   */
  assignPlacement(
    data: AssignPlacementRequest
  ): Promise<ApiResponse<AssignPlacementResponse>> {
    return api.post<AssignPlacementResponse>("/staff/placements/assign", data);
  },
};

