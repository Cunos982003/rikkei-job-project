
import { api } from "../client";
import type { ApiResponse } from "../types/api";
import type {
  MentorEvaluation,
  Internship,
} from "../types/evaluation";

// ===== HS-03 Portfolio Types =====

export type PortfolioSource = "PM" | "SELF" | "IOC" | "MANUAL";

export interface PortfolioMedia {
  id: string;
  url: string;
  mediaType: "IMAGE";
  sortOrder: number;
}

export interface PortfolioProject {
  id: string;
  name: string;
  role?: string;
  description?: string;
  techStack?: string;
  repoUrl?: string;
  demoUrl?: string;
  startDate?: string;
  endDate?: string;
  visible: boolean;
  sortOrder: number;
  internshipId?: string;
  source: PortfolioSource;
  sourceRef: string;
  media: PortfolioMedia[];
}

export interface CreatePortfolioProjectRequest {
  name: string;
  role?: string;
  description?: string;
  techStack?: string;
  repoUrl?: string;
  demoUrl?: string;
  startDate?: string;
  endDate?: string;
  visible?: boolean;
  sortOrder?: number;
}

export type UpdatePortfolioProjectRequest = CreatePortfolioProjectRequest;

export interface ReorderPortfolioProjectsRequest {
  projectIds: string[];
}

export interface ReorderPortfolioMediaRequest {
  mediaIds: string[];
}

export interface UploadPortfolioMediaResponse {
  id: string;
  url: string;
  mediaType: "IMAGE";
  sortOrder: number;
}

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

export type Visibility =
  | "PRIVATE"
  | "PENDING"
  | "PUBLIC"
  | "REJECTED"
  | "RECRUITER";

export type ReviewAction = "SUBMIT" | "APPROVE" | "REJECT" | "WITHDRAW";

/**
 * Khớp ReviewHistoryItemDto của profile-service (HS-05).
 */
export interface ReviewHistoryItem {
  id: string;
  action: ReviewAction;
  reason?: string | null;
  actorId: string;
  createdAt: string;
}

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

// ===== HS-06 Staff Profile Review Types =====

/**
 * Staff preview profile - full profile with masked contact info
 */
export interface StaffPreviewProfile {
  userId: string;
  fullName?: string;
  email?: string;
  phone?: string;
  maskedEmail?: string;
  maskedPhone?: string;
  dob?: string;
  gender?: string;
  address?: string;
  avatarUrl?: string;
  headline?: string;
  summary?: string;
  program?: string;
  classCode?: string;
  courseName?: string;
  visibility?: Visibility;
  consentAt?: string;
  consentWithdrawnAt?: string;
  updatedAt?: string;
  skills: StaffSkillProfileItem[];
  educations: StaffEducationItem[];
  certificates: StaffCertificateItem[];
  reviewHistory: StaffReviewHistoryItem[];
  rejectionCount: number;
}

export interface StaffSkillProfileItem {
  skillId: string;
  skillCode: string;
  skillName: string;
  group: string;
  verifiedScore: number | null;
  selfScore: number | null;
  level: string;
  evidences: StaffSkillEvidenceItem[];
}

export interface StaffSkillEvidenceItem {
  source: string;
  sourceRef?: string;
  title: string;
  score: number;
  recordedAt: string;
  verified: boolean;
}

export interface StaffEducationItem {
  id?: string;
  school: string;
  major: string;
  fromDate?: string;
  toDate?: string;
  gpa?: number;
  note?: string;
}

export interface StaffCertificateItem {
  id?: string;
  name: string;
  issuer: string;
  issuedAt?: string;
  expiresAt?: string;
  fileUrl?: string;
  fileType?: string;
}

export interface StaffReviewHistoryItem {
  id: string;
  action: ReviewAction;
  reason?: string | null;
  actorId: string;
  createdAt: string;
}

/**
 * Enhanced PendingReviewDto for staff queue
 */
export interface StaffPendingReview {
  userId: string;
  fullName?: string;
  email?: string;
  headline?: string;
  avatarUrl?: string;
  classCode?: string;
  submittedAt?: string;
  rejectionCount: number;
}

/**
 * Request for editing self-declared fields
 */
export interface StaffEditSelfDeclaredRequest {
  field: "HEADLINE" | "SUMMARY" | "SELF_SKILLS";
  value: string;
  reason: string;
}

export interface StaffEditSelfDeclaredResponse {
  userId: string;
  field: "HEADLINE" | "SUMMARY" | "SELF_SKILLS";
  oldValue: string;
  newValue: string;
}

/**
 * Query params for staff profile reviews
 */
export interface StaffProfileReviewsParams {
  status?: Visibility;
  q?: string;
  classCode?: string;
  page?: number;
  size?: number;
}

/**
 * Endpoints hồ sơ năng lực ứng viên (profile-service :8082 hoặc Mock :9000)
 */
export const profileApi = {
  /**
   * Lấy danh sách kỹ năng của chính mình (Candidate)
   */
  getMySkills(): Promise<ApiResponse<SkillProfileItem[]>> {
    return api.get<SkillProfileItem[]>("/profiles/me/skills", {
      silentError: true,
    });
  },

  /**
   * Lấy danh sách kỹ năng của ứng viên khác (nếu đã công khai)
   */
  getUserSkills(userId: string): Promise<ApiResponse<SkillProfileItem[]>> {
    return api.get<SkillProfileItem[]>(`/profiles/${userId}/skills`);
  },

  // ===== Thực tập IOC (TT-01) =====

  /**
   * Lấy danh sách thực tập của chính ứng viên đang đăng nhập.
   */
  getMyInternships(): Promise<ApiResponse<Internship[]>> {
    return api.get<Internship[]>("/profiles/me/internships");
  },

  /**
   * Lấy danh sách thực tập của ứng viên khác.
   */
  getUserInternships(userId: string): Promise<ApiResponse<Internship[]>> {
    return api.get<Internship[]>(`/profiles/${userId}/internships`);
  },

  /**
   * Lấy danh sách đánh giá của Mentor dành cho ứng viên
   * @param userId ID của ứng viên (hoặc để trống/undefined nếu lấy của chính mình)
   */
  getEvaluations(userId?: string): Promise<ApiResponse<MentorEvaluation[]>> {
    const url = userId
      ? `/profiles/${userId}/evaluations`
      : "/profiles/me/evaluations";

    return api.get<MentorEvaluation[]>(url, { silentError: true });
  },

  // ===== Hồ sơ cá nhân =====

  /**
   * Lấy chi tiết hồ sơ cá nhân (tự tạo từ LMS nếu chưa có)
   */
  getMyProfile(): Promise<ApiResponse<CandidateProfile>> {
    return api.get<CandidateProfile>("/profiles/me", {
      silentError: true,
    });
  },

  /**
   * Cập nhật hồ sơ cá nhân. Backend bỏ qua các trường LMS.
   */
  updateMyProfile(
    data: Partial<CandidateProfile>
  ): Promise<ApiResponse<CandidateProfile>> {
    return api.put<CandidateProfile>("/profiles/me", data);
  },

  // ===== Công khai hồ sơ (HS-05) =====

  /**
   * Gửi hồ sơ cho cán bộ duyệt. Bắt buộc tick đồng ý điều khoản, nếu thiếu backend trả 400.
   * Chỉ gọi được khi hồ sơ đang PRIVATE hoặc REJECTED.
   */
  publishProfile(consent: boolean): Promise<ApiResponse<CandidateProfile>> {
    return api.post<CandidateProfile>("/profiles/me/publish", { consent });
  },

  /**
   * Rút hồ sơ khỏi trạng thái công khai. Có hiệu lực ngay.
   * Chỉ gọi được khi hồ sơ đang PENDING hoặc PUBLIC.
   */
  withdrawProfile(): Promise<ApiResponse<CandidateProfile>> {
    return api.post<CandidateProfile>("/profiles/me/withdraw");
  },

  /**
   * Lịch sử duyệt hồ sơ của chính mình, mới nhất trước.
   */
  getReviewHistory(): Promise<ApiResponse<ReviewHistoryItem[]>> {
    return api.get<ReviewHistoryItem[]>("/profiles/me/review-history");
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
    return api.get<EducationItem[]>("/profiles/me/educations", {
      silentError: true,
    });
  },

  createEducation(
    data: EducationItem
  ): Promise<ApiResponse<EducationItem>> {
    return api.post<EducationItem>("/profiles/me/educations", data);
  },

  updateEducation(
    id: string,
    data: EducationItem
  ): Promise<ApiResponse<EducationItem>> {
    return api.put<EducationItem>(`/profiles/me/educations/${id}`, data);
  },

  deleteEducation(id: string): Promise<ApiResponse<null>> {
    return api.delete<null>(`/profiles/me/educations/${id}`);
  },

  // ===== Chứng chỉ =====

  getCertificates(): Promise<ApiResponse<CertificateItem[]>> {
    return api.get<CertificateItem[]>("/profiles/me/certificates", {
      silentError: true,
    });
  },

  createCertificate(
    data: CertificateItem
  ): Promise<ApiResponse<CertificateItem>> {
    return api.post<CertificateItem>("/profiles/me/certificates", data);
  },

  updateCertificate(
    id: string,
    data: CertificateItem
  ): Promise<ApiResponse<CertificateItem>> {
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

  // ===== HS-06 Staff Profile Review =====

  /**
   * Lấy danh sách hồ sơ chờ duyệt cho staff (có filter q, classCode, rejectionCount)
   */
  getStaffProfileReviews(
      params: StaffProfileReviewsParams = {}
  ): Promise<
      ApiResponse<{
        items?: StaffPendingReview[];
        content?: StaffPendingReview[];
        pagination?: {
          page: number;
          size: number;
          totalElements: number;
          totalPages: number;
        };
        totalElements?: number;
        totalPages?: number;
        number?: number;
        size?: number;
      }>
  > {
    const searchParams = new URLSearchParams();

    if (params.status) searchParams.set("status", params.status);
    if (params.q) searchParams.set("q", params.q);
    if (params.classCode) searchParams.set("classCode", params.classCode);
    if (params.page !== undefined) {
      searchParams.set("page", String(params.page));
    }
    if (params.size !== undefined) {
      searchParams.set("size", String(params.size));
    }

    return api.get<{
      items?: StaffPendingReview[];
      content?: StaffPendingReview[];
      pagination?: {
        page: number;
        size: number;
        totalElements: number;
        totalPages: number;
      };
      totalElements?: number;
      totalPages?: number;
      number?: number;
      size?: number;
    }>(`/staff/profile-reviews?${searchParams.toString()}`);
  },

  /**
   * Lấy preview hồ sơ ứng viên cho staff (che email/phone)
   */
  getStaffPreviewProfile(
    userId: string
  ): Promise<ApiResponse<StaffPreviewProfile>> {
    return api.get<StaffPreviewProfile>(`/staff/profiles/${userId}/preview`);
  },

  /**
   * Sửa phần tự khai (headline, summary, self_skills) bởi staff
   */
  editStaffSelfDeclared(
    userId: string,
    data: StaffEditSelfDeclaredRequest
  ): Promise<ApiResponse<StaffEditSelfDeclaredResponse>> {
    return api.put<StaffEditSelfDeclaredResponse>(
      `/staff/profiles/${userId}/self-declared`,
      data
    );
  },

  /**
   * Duyệt hồ sơ ứng viên (PENDING → PUBLIC) - Staff only
   */
  approveProfile(
    userId: string
  ): Promise<ApiResponse<{ userId: string; visibility: string }>> {
    return api.post<{ userId: string; visibility: string }>(
      `/staff/profiles/${userId}/approve`
    );
  },

  /**
   * Trả lại hồ sơ ứng viên (PENDING → REJECTED) - Staff only
   */
  rejectProfile(
    userId: string,
    reason: string
  ): Promise<ApiResponse<{ userId: string; visibility: string }>> {
    return api.post<{ userId: string; visibility: string }>(
      `/staff/profiles/${userId}/reject`,
      { reason }
    );
  },

  // ===== HS-03 Portfolio =====

  /**
   * Lấy danh sách dự án portfolio của chính mình
   */
  getMyPortfolio(): Promise<ApiResponse<PortfolioProject[]>> {
    return api.get<PortfolioProject[]>("/profiles/me/portfolio", { silentError: true });
  },

  /**
   * Lấy danh sách dự án portfolio công khai của người khác
   */
  getUserPortfolio(userId: string): Promise<ApiResponse<PortfolioProject[]>> {
    return api.get<PortfolioProject[]>(`/profiles/${userId}/portfolio`);
  },

  /**
   * Tạo dự án portfolio mới
   */
  createPortfolioProject(
    data: CreatePortfolioProjectRequest
  ): Promise<ApiResponse<PortfolioProject>> {
    return api.post<PortfolioProject>("/profiles/me/portfolio", data);
  },

  /**
   * Cập nhật dự án portfolio
   */
  updatePortfolioProject(
    projectId: string,
    data: UpdatePortfolioProjectRequest
  ): Promise<ApiResponse<PortfolioProject>> {
    return api.put<PortfolioProject>(`/profiles/me/portfolio/${projectId}`, data);
  },

  /**
   * Xóa dự án portfolio (không thể xóa dự án IOC)
   */
  deletePortfolioProject(projectId: string): Promise<ApiResponse<null>> {
    return api.delete<null>(`/profiles/me/portfolio/${projectId}`);
  },

  /**
   * Sắp xếp lại thứ tự dự án portfolio
   */
  reorderPortfolioProjects(
    data: ReorderPortfolioProjectsRequest
  ): Promise<ApiResponse<PortfolioProject[]>> {
    return api.put<PortfolioProject[]>("/profiles/me/portfolio/order", data);
  },

  /**
   * Tải lên ảnh cho dự án portfolio (JPG/PNG, tối đa 5MB, tối đa 6 ảnh/dự án)
   */
  uploadPortfolioMedia(
    projectId: string,
    file: File
  ): Promise<ApiResponse<UploadPortfolioMediaResponse>> {
    const formData = new FormData();
    formData.append("file", file);
    return api.post<UploadPortfolioMediaResponse>(
      `/profiles/me/portfolio/${projectId}/media`,
      formData
    );
  },

  /**
   * Sắp xếp lại thứ tự ảnh trong dự án portfolio
   */
  reorderPortfolioMedia(
    projectId: string,
    data: ReorderPortfolioMediaRequest
  ): Promise<ApiResponse<null>> {
    return api.put<null>(
      `/profiles/me/portfolio/${projectId}/media/order`,
      data
    );
  },

  /**
   * Xóa ảnh khỏi dự án portfolio
   */
  deletePortfolioMedia(
    projectId: string,
    mediaId: string
  ): Promise<ApiResponse<null>> {
    return api.delete<null>(
      `/profiles/me/portfolio/${projectId}/media/${mediaId}`
    );
  },
};

// ===== HS-04 CV Types =====

export type CVSectionType =
  | 'personal_info'
  | 'education'
  | 'experience'
  | 'skills'
  | 'projects'
  | 'certificates'
  | 'summary';

export interface CVSection {
  id: string;
  sectionType: CVSectionType;
  contentJson: string;
  sortOrder: number;
}

export interface CV {
  id: string;
  title: string;
  publicToken: string;
  isPublic: boolean;
  pdfUrl?: string;
  createdAt: string;
  updatedAt: string;
  sections: CVSection[];
}

export interface CreateCVRequest {
  title: string;
}

export interface UpdateCVRequest {
  title?: string;
  isPublic?: boolean;
}

export interface UpdateCVSectionRequest {
  sectionType: CVSectionType;
  contentJson: string;
  sortOrder?: number;
}

export interface ReorderCVSectionsRequest {
  sectionIds: string[];
}

export interface GeneratePDFResponse {
  pdfUrl: string;
  publicUrl: string;
}

// ===== HS-04 CV Endpoints =====

/**
 * Lấy danh sách CV của chính mình
 */
export const getMyCVs = (): Promise<ApiResponse<CV[]>> => {
  return api.get<CV[]>('/profiles/me/cvs', { silentError: true });
};

/**
 * Lấy chi tiết một CV
 */
export const getMyCV = (cvId: string): Promise<ApiResponse<CV>> => {
  return api.get<CV>(`/profiles/me/cvs/${cvId}`);
};

/**
 * Tạo CV mới
 */
export const createCV = (data: CreateCVRequest): Promise<ApiResponse<CV>> => {
  return api.post<CV>('/profiles/me/cvs', data);
};

/**
 * Cập nhật CV
 */
export const updateCV = (cvId: string, data: UpdateCVRequest): Promise<ApiResponse<CV>> => {
  return api.put<CV>(`/profiles/me/cvs/${cvId}`, data);
};

/**
 * Xóa CV
 */
export const deleteCV = (cvId: string): Promise<ApiResponse<null>> => {
  return api.delete<null>(`/profiles/me/cvs/${cvId}`);
};

/**
 * Cập nhật section của CV
 */
export const upsertCVSection = (cvId: string, data: UpdateCVSectionRequest): Promise<ApiResponse<CVSection>> => {
  return api.put<CVSection>(`/profiles/me/cvs/${cvId}/sections`, data);
};

/**
 * Xóa section của CV
 */
export const deleteCVSection = (cvId: string, sectionType: CVSectionType): Promise<ApiResponse<null>> => {
  return api.delete<null>(`/profiles/me/cvs/${cvId}/sections/${sectionType}`);
};

/**
 * Sắp xếp lại thứ tự sections
 */
export const reorderCVSections = (cvId: string, data: ReorderCVSectionsRequest): Promise<ApiResponse<null>> => {
  return api.put<null>(`/profiles/me/cvs/${cvId}/sections/order`, data);
};

/**
 * Tạo PDF cho CV
 */
export const generateCVPDF = (cvId: string): Promise<ApiResponse<GeneratePDFResponse>> => {
  return api.post<GeneratePDFResponse>(`/profiles/me/cvs/${cvId}/generate-pdf`, {});
};

/**
 * Lấy CV công khai (không cần auth)
 */
export const getPublicCV = (publicToken: string): Promise<ApiResponse<CV>> => {
  return api.get<CV>(`/profiles/public/${publicToken}`);
};
