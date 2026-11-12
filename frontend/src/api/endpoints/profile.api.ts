import { api } from "../client";
import type { ApiResponse } from "../types/api";
import type { MentorEvaluation } from "../types/evaluation";

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

/**
 * Khớp EducationDto của profile-service.
 */
export interface EducationItem {
  id?: string;
  userId?: string;
  school: string;
  major: string;
  fromDate: string;
  toDate?: string;
  gpa?: number;
  note?: string;
}

/**
 * Khớp CertificateDto của profile-service.
 */
export interface CertificateItem {
  id?: string;
  userId?: string;
  name: string;
  issuer: string;
  issuedAt: string;
  expiresAt?: string;
  fileUrl?: string;
  fileType?: string;
}

export type Visibility = "PRIVATE" | "PUBLIC" | "RECRUITER";

/**
 * Khớp CandidateProfile entity của profile-service.
 * Các trường đồng bộ từ LMS (fullName, email, program, classCode, courseName)
 * do backend quản lý, không gửi lên khi cập nhật.
 */
export interface CandidateProfile {
  userId: string;
  fullName?: string;
  dob?: string;
  gender?: string;
  phone?: string;
  email?: string;
  address?: string;
  avatarUrl?: string;
  headline?: string;
  summary?: string;
  program?: string;
  classCode?: string;
  courseName?: string;
  visibility?: Visibility;
  updatedAt?: string;
}

/**
 * Khớp SelfSkillDto của profile-service.
 */
export interface SelfSkillItem {
  skillId: string;
  selfScore: number;
}

/**
 * Endpoints hồ sơ năng lực ứng viên (profile-service :8082 hoặc Mock :9001)
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
   * Lấy danh sách đánh giá của Mentor dành cho ứng viên
   * @param userId ID của ứng viên (hoặc để trống/undefined nếu lấy của chính mình)
   */
  getEvaluations(userId?: string): Promise<ApiResponse<MentorEvaluation[]>> {
    const url = userId ? `/profiles/${userId}/evaluations` : "/profiles/me/evaluations";
    return api.get<MentorEvaluation[]>(url);
  },

  // ===== Hồ sơ cá nhân =====

  /**
   * Lấy chi tiết hồ sơ cá nhân (tự tạo từ LMS nếu chưa có)
   */
  getMyProfile(): Promise<ApiResponse<CandidateProfile>> {
    return api.get<CandidateProfile>("/profiles/me");
  },

  /**
   * Cập nhật hồ sơ cá nhân. Backend bỏ qua các trường LMS.
   */
  updateMyProfile(data: Partial<CandidateProfile>): Promise<ApiResponse<CandidateProfile>> {
    return api.put<CandidateProfile>("/profiles/me", data);
  },

  // ===== Kỹ năng tự đánh giá (HS-01 skill_evidences, source=SELF) =====

  /**
   * Ghi đè danh sách kỹ năng tự đánh giá trong một lần gọi.
   */
  upsertSelfSkills(skills: SelfSkillItem[]): Promise<ApiResponse<null>> {
    return api.put<null>("/profiles/me/self-skills", skills);
  },

  // ===== Học vấn =====

  getEducations(): Promise<ApiResponse<EducationItem[]>> {
    return api.get<EducationItem[]>("/profiles/me/educations");
  },

  createEducation(data: EducationItem): Promise<ApiResponse<EducationItem>> {
    return api.post<EducationItem>("/profiles/me/educations", data);
  },

  updateEducation(id: string, data: EducationItem): Promise<ApiResponse<EducationItem>> {
    return api.put<EducationItem>(`/profiles/me/educations/${id}`, data);
  },

  deleteEducation(id: string): Promise<ApiResponse<null>> {
    return api.delete<null>(`/profiles/me/educations/${id}`);
  },

  // ===== Chứng chỉ =====

  getCertificates(): Promise<ApiResponse<CertificateItem[]>> {
    return api.get<CertificateItem[]>("/profiles/me/certificates");
  },

  createCertificate(data: CertificateItem): Promise<ApiResponse<CertificateItem>> {
    return api.post<CertificateItem>("/profiles/me/certificates", data);
  },

  updateCertificate(id: string, data: CertificateItem): Promise<ApiResponse<CertificateItem>> {
    return api.put<CertificateItem>(`/profiles/me/certificates/${id}`, data);
  },

  deleteCertificate(id: string): Promise<ApiResponse<null>> {
    return api.delete<null>(`/profiles/me/certificates/${id}`);
  },

  // ===== Tệp đính kèm =====

  /**
   * Tải tệp lên (PDF, JPG, PNG — tối đa 5MB, kiểm tra bằng magic bytes).
   * @returns URL tương đối của tệp đã lưu
   */
  uploadFile(file: File): Promise<ApiResponse<{ url: string }>> {
    const formData = new FormData();
    formData.append("file", file);
    return api.post<{ url: string }>("/files", formData);
  },
};
