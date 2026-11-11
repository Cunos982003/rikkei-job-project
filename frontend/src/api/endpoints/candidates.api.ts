import { api } from "../client";
import type { ApiResponse, PageParams } from "../types/api";

export interface CandidateSearchItem {
  id: string;
  lmsUserId?: string;
  fullName: string;
  email?: string;
  phone?: string;
  avatarUrl?: string;
  title: string;
  headline?: string;
  program?: string;
  overallScore: number;
  level: string;
  skills: Array<{
    name: string;
    score: number;
    level: string;
    verified: boolean;
  }>;
  isPublic: boolean;
  createdAt: string;
}

export interface CandidateListResponse {
  items: CandidateSearchItem[];
  total: number;
  page: number;
  size: number;
}

/**
 * Endpoints tìm kiếm & xem chi tiết ứng viên (Mock :9000 hoặc profile-service)
 */
export const candidatesApi = {
  /**
   * Lấy danh sách ứng viên công khai có phân trang
   */
  getCandidates(params?: PageParams): Promise<ApiResponse<CandidateListResponse>> {
    return api.get<CandidateListResponse>("/candidates", { params });
  },

  /**
   * Lấy chi tiết ứng viên theo ID
   */
  getCandidateById(id: string): Promise<ApiResponse<CandidateSearchItem>> {
    return api.get<CandidateSearchItem>(`/candidates/${id}`);
  },
};

