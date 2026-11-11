import { api } from "../client";
import type { ApiResponse } from "../types/api";

export interface SkillItem {
  id: string | number;
  code: string;
  name: string;
  group?: string;
  description?: string;
}

/**
 * Endpoints danh mục kỹ năng chuẩn (assessment-service :8084 hoặc Mock :9000)
 */
export const skillsApi = {
  /**
   * Lấy danh mục 20 kỹ năng chuẩn
   */
  getSkills(): Promise<ApiResponse<SkillItem[]>> {
    return api.get<SkillItem[]>("/skills");
  },
};

