import { api } from "../client";
import type { ApiResponse } from "../types/api";

export interface SkillEvidenceItem {
  source: string;
  sourceRef?: string;
  title: string;
  score: number;
  recordedAt: string;
  verified: boolean;
}

export interface SkillProfileItem {
  skillId: string;
  skillCode: string;
  skillName: string;
  group: string;
  verifiedScore: number | null;
  selfScore: number | null;
  level: string;
  evidences: SkillEvidenceItem[];
}

export interface EducationItem {
  id?: string | number;
  school: string;
  major: string;
  startDate: string;
  endDate?: string;
  gpa?: number;
}

export interface CertificateItem {
  id?: string | number;
  name: string;
  organization: string;
  issuedDate: string;
  credentialUrl?: string;
}

export interface CandidateDetailProfile {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  avatarUrl?: string;
  title?: string;
  headline?: string;
  program?: string;
  overallScore?: number;
  level?: string;
  isPublic?: boolean;
  skills?: SkillProfileItem[];
  educations?: EducationItem[];
  certificates?: CertificateItem[];
}

/**
 * Endpoints hồ sơ năng lực ứng viên (profile-service :8082 hoặc Mock :9000)
 */
export const profileApi = {
  /**
   * Lấy danh sách kỹ năng của chính mình (Candidate)
   */
  getMySkills(): Promise<ApiResponse<SkillProfileItem[]>> {
    return api.get<SkillProfileItem[]>("/profiles/me/skills");
  },

  /**
   * Lấy danh sách kỹ năng của ứng viên khác (nếu đã công khai)
   */
  getUserSkills(userId: string): Promise<ApiResponse<SkillProfileItem[]>> {
    return api.get<SkillProfileItem[]>(`/profiles/${userId}/skills`);
  },

  /**
   * Lấy chi tiết hồ sơ cá nhân
   */
  getMyProfile(): Promise<ApiResponse<CandidateDetailProfile>> {
    return api.get<CandidateDetailProfile>("/profiles/me");
  },

  /**
   * Cập nhật kỹ năng tự khai (Self-reported)
   */
  updateSelfSkill(skillId: string, score: number): Promise<ApiResponse<null>> {
    return api.post<null>(`/profiles/me/skills/${skillId}/self`, { score });
  },
};

