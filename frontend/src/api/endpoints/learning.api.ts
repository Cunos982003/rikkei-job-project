import { api } from "../client";
import type { ApiResponse } from "../types/api";

export interface PositionSkill {
  skillId: string;
  skillName: string;
  minScore: number;
  isMandatory: boolean;
}

export interface TargetPosition {
  id: string;
  code: string;
  name: string;
  description: string;
  skills: PositionSkill[];
}

export type TargetType = "POSITION" | "JOB";

export interface CandidateTarget {
  userId: string;
  targetType: TargetType;
  positionId?: string | null;
  jobId?: string | null;
  targetName: string;
  chosenAt: string;
}

export interface SetTargetPayload {
  positionId?: string;
  jobId?: string;
}

export interface TargetInfo {
  type: TargetType;
  id: string;
  code: string;
  name: string;
  description: string;
}

export type GapPriority = "HIGH" | "MEDIUM" | "LOW";

export interface GapItem {
  skillId: string;
  skill: string;
  skillName: string;
  required: number;
  current: number;
  gap: number;
  priority: GapPriority;
  isMandatory: boolean;
}

export interface GapAnalysisResult {
  target: TargetInfo;
  readiness: number;
  totalRequiredSkills: number;
  passedSkillsCount: number;
  missingSkillsCount: number;
  computedAt: string;
  items: GapItem[];
}

/**
 * Endpoints phân tích khoảng thiếu hụt năng lực và mục tiêu nghề nghiệp (learning-service qua API Gateway :8080)
 */
export const learningApi = {
  /**
   * Lấy danh mục 4 vị trí mục tiêu định nghĩa sẵn
   */
  getTargetPositions(): Promise<ApiResponse<TargetPosition[]>> {
    return api.get<TargetPosition[]>("/learning/target-positions");
  },

  /**
   * Lấy chi tiết một vị trí mục tiêu theo ID
   */
  getTargetPositionById(id: string): Promise<ApiResponse<TargetPosition>> {
    return api.get<TargetPosition>(`/learning/target-positions/${id}`);
  },

  /**
   * Lấy mục tiêu nghề nghiệp hiện tại của ứng viên đăng nhập
   */
  getMyTarget(): Promise<ApiResponse<CandidateTarget | null>> {
    return api.get<CandidateTarget | null>("/learning/me/target");
  },

  /**
   * Thiết lập hoặc thay đổi mục tiêu nghề nghiệp (chọn Position hoặc Job ID)
   */
  setMyTarget(payload: SetTargetPayload): Promise<ApiResponse<CandidateTarget>> {
    return api.put<CandidateTarget>("/learning/me/target", payload);
  },

  /**
   * Phân tích khoảng thiếu hụt năng lực theo mục tiêu đã lưu hoặc theo Job ID ghi đè
   */
  getGapAnalysis(jobId?: string): Promise<ApiResponse<GapAnalysisResult>> {
    return api.get<GapAnalysisResult>("/learning/me/gap-analysis", {
      params: jobId ? { jobId } : undefined,
    });
  },
};

