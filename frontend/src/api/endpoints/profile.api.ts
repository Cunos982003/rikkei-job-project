
import { api } from "../client";
import type { ApiResponse } from "../types/api";
import type {
  MentorEvaluation,
  Internship,
} from "../types/evaluation";

// ======================================================
// SKILLS
// ======================================================

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

export interface SelfSkillItem {
  skillId: string;
  selfScore: number;
}

// ======================================================
// EDUCATION
// ======================================================

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

// ======================================================
// CERTIFICATES
// ======================================================

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

// ======================================================
// PROFILE
// ======================================================

export type Visibility =
    | "PRIVATE"
    | "PENDING"
    | "PUBLIC"
    | "REJECTED"
    | "RECRUITER";

export type ReviewAction =
    | "SUBMIT"
    | "APPROVE"
    | "REJECT"
    | "WITHDRAW";

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
 * Các trường đồng bộ từ LMS do backend quản lý,
 * không gửi lên khi cập nhật.
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

// ======================================================
// PORTFOLIO
// ======================================================

/**
 * Thông tin ảnh/tệp đính kèm của một dự án Portfolio.
 */
export interface PortfolioMedia {
  id: string;
  projectId?: string;
  fileName?: string;
  fileUrl?: string;
  url?: string;
  contentType?: string;
  fileType?: string;
  sortOrder?: number;
  createdAt?: string;
}

/**
 * Thông tin một dự án trong Portfolio của ứng viên.
 */

export interface PortfolioProject {
  id: string;
  userId?: string;

  name: string;
  role?: string;
  description?: string;
  techStack?: string;

  repoUrl?: string;
  demoUrl?: string;

  startDate?: string;
  endDate?: string;

  visible: boolean;
  sortOrder?: number;

  // Nguồn gốc dự án và mã tham chiếu
  source?: string;
  sourceRef?: string;

  media?: PortfolioMedia[];

  createdAt?: string;
  updatedAt?: string;
}

/**
 * Request tạo dự án Portfolio.
 */
export interface CreatePortfolioProjectRequest {
  name: string;
  role?: string;
  description?: string;
  techStack?: string;
  repoUrl?: string;
  demoUrl?: string;
  startDate?: string;
  endDate?: string;
  visible: boolean;
  sortOrder?: number;
}

/**
 * Request cập nhật dự án Portfolio.
 */
export interface UpdatePortfolioProjectRequest
    extends Partial<CreatePortfolioProjectRequest> {}

// ======================================================
// HS-06 STAFF PROFILE REVIEW TYPES
// ======================================================

/**
 * Staff preview profile - full profile with masked contact info.
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
 * Enhanced PendingReviewDto for staff queue.
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
 * Request for editing self-declared fields.
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
 * Query params for staff profile reviews.
 */
export interface StaffProfileReviewsParams {
  status?: Visibility;
  q?: string;
  classCode?: string;
  page?: number;
  size?: number;
}

// ======================================================
// PROFILE API
// ======================================================

/**
 * Endpoints hồ sơ năng lực ứng viên.
 * profile-service :8082 hoặc Mock :9000.
 */
export const profileApi = {
  // ====================================================
  // SKILLS
  // ====================================================

  /**
   * Lấy danh sách kỹ năng của chính mình (Candidate).
   */
  getMySkills(): Promise<ApiResponse<SkillProfileItem[]>> {
    return api.get<SkillProfileItem[]>("/profiles/me/skills", {
      silentError: true,
    });
  },

  /**
   * Lấy danh sách kỹ năng của ứng viên khác.
   */
  getUserSkills(
      userId: string
  ): Promise<ApiResponse<SkillProfileItem[]>> {
    return api.get<SkillProfileItem[]>(`/profiles/${userId}/skills`);
  },

  /**
   * Ghi đè danh sách kỹ năng tự đánh giá.
   */
  upsertSelfSkills(
      skills: SelfSkillItem[]
  ): Promise<ApiResponse<null>> {
    return api.put<null>("/profiles/me/self-skills", skills);
  },

  // ====================================================
  // INTERNSHIPS - TT-01
  // ====================================================

  /**
   * Lấy danh sách thực tập của ứng viên đang đăng nhập.
   */
  getMyInternships(): Promise<ApiResponse<Internship[]>> {
    return api.get<Internship[]>("/profiles/me/internships");
  },

  /**
   * Lấy danh sách thực tập của ứng viên khác.
   */
  getUserInternships(
      userId: string
  ): Promise<ApiResponse<Internship[]>> {
    return api.get<Internship[]>(`/profiles/${userId}/internships`);
  },

  /**
   * Lấy danh sách đánh giá của Mentor dành cho ứng viên.
   */
  getEvaluations(
      userId?: string
  ): Promise<ApiResponse<MentorEvaluation[]>> {
    const url = userId
        ? `/profiles/${userId}/evaluations`
        : "/profiles/me/evaluations";

    return api.get<MentorEvaluation[]>(url);
  },

  // ====================================================
  // PERSONAL PROFILE
  // ====================================================

  /**
   * Lấy chi tiết hồ sơ cá nhân.
   */
  getMyProfile(): Promise<ApiResponse<CandidateProfile>> {
    return api.get<CandidateProfile>("/profiles/me", {
      silentError: true,
    });
  },

  /**
   * Cập nhật hồ sơ cá nhân.
   * Backend bỏ qua các trường đồng bộ từ LMS.
   */
  updateMyProfile(
      data: Partial<CandidateProfile>
  ): Promise<ApiResponse<CandidateProfile>> {
    return api.put<CandidateProfile>("/profiles/me", data);
  },

  // ====================================================
  // PROFILE VISIBILITY - HS-05
  // ====================================================

  /**
   * Gửi hồ sơ cho cán bộ duyệt.
   */
  publishProfile(
      consent: boolean
  ): Promise<ApiResponse<CandidateProfile>> {
    return api.post<CandidateProfile>("/profiles/me/publish", {
      consent,
    });
  },

  /**
   * Rút hồ sơ khỏi trạng thái công khai.
   */
  withdrawProfile(): Promise<ApiResponse<CandidateProfile>> {
    return api.post<CandidateProfile>("/profiles/me/withdraw");
  },

  /**
   * Lịch sử duyệt hồ sơ của chính mình.
   */
  getReviewHistory(): Promise<ApiResponse<ReviewHistoryItem[]>> {
    return api.get<ReviewHistoryItem[]>("/profiles/me/review-history");
  },

  // ====================================================
  // EDUCATION
  // ====================================================

  /**
   * Lấy danh sách học vấn.
   */
  getEducations(): Promise<ApiResponse<EducationItem[]>> {
    return api.get<EducationItem[]>("/profiles/me/educations", {
      silentError: true,
    });
  },

  /**
   * Thêm học vấn.
   */
  createEducation(
      data: EducationItem
  ): Promise<ApiResponse<EducationItem>> {
    return api.post<EducationItem>("/profiles/me/educations", data);
  },

  /**
   * Cập nhật học vấn.
   */
  updateEducation(
      id: string,
      data: EducationItem
  ): Promise<ApiResponse<EducationItem>> {
    return api.put<EducationItem>(
        `/profiles/me/educations/${id}`,
        data
    );
  },

  /**
   * Xóa học vấn.
   */
  deleteEducation(id: string): Promise<ApiResponse<null>> {
    return api.delete<null>(`/profiles/me/educations/${id}`);
  },

  // ====================================================
  // CERTIFICATES
  // ====================================================

  /**
   * Lấy danh sách chứng chỉ.
   */
  getCertificates(): Promise<ApiResponse<CertificateItem[]>> {
    return api.get<CertificateItem[]>("/profiles/me/certificates", {
      silentError: true,
    });
  },

  /**
   * Thêm chứng chỉ.
   */
  createCertificate(
      data: CertificateItem
  ): Promise<ApiResponse<CertificateItem>> {
    return api.post<CertificateItem>(
        "/profiles/me/certificates",
        data
    );
  },

  /**
   * Cập nhật chứng chỉ.
   */
  updateCertificate(
      id: string,
      data: CertificateItem
  ): Promise<ApiResponse<CertificateItem>> {
    return api.put<CertificateItem>(
        `/profiles/me/certificates/${id}`,
        data
    );
  },

  /**
   * Xóa chứng chỉ.
   */
  deleteCertificate(id: string): Promise<ApiResponse<null>> {
    return api.delete<null>(`/profiles/me/certificates/${id}`);
  },

  // ====================================================
  // FILE UPLOAD
  // ====================================================

  /**
   * Tải tệp lên (PDF, JPG, PNG — tối đa 5MB).
   */
  uploadFile(
      file: File
  ): Promise<ApiResponse<{ url: string }>> {
    const formData = new FormData();
    formData.append("file", file);

    return api.post<{ url: string }>("/files", formData);
  },

  // ====================================================
  // PORTFOLIO
  // ====================================================

  /**
   * Lấy danh sách dự án Portfolio của ứng viên hiện tại.
   */
  getMyPortfolio(): Promise<ApiResponse<PortfolioProject[]>> {
    return api.get<PortfolioProject[]>("/profiles/me/portfolio");
  },

  /**
   * Tạo dự án Portfolio.
   */
  createPortfolioProject(
      data: CreatePortfolioProjectRequest
  ): Promise<ApiResponse<PortfolioProject>> {
    return api.post<PortfolioProject>(
        "/profiles/me/portfolio/projects",
        data
    );
  },

  /**
   * Cập nhật dự án Portfolio.
   */
  updatePortfolioProject(
      projectId: string,
      data: UpdatePortfolioProjectRequest
  ): Promise<ApiResponse<PortfolioProject>> {
    return api.put<PortfolioProject>(
        `/profiles/me/portfolio/projects/${projectId}`,
        data
    );
  },

  /**
   * Xóa dự án Portfolio.
   */
  deletePortfolioProject(
      projectId: string
  ): Promise<ApiResponse<null>> {
    return api.delete<null>(
        `/profiles/me/portfolio/projects/${projectId}`
    );
  },

  /**
   * Tải ảnh minh chứng cho dự án Portfolio.
   */
  uploadPortfolioMedia(
      projectId: string,
      file: File
  ): Promise<ApiResponse<PortfolioMedia>> {
    const formData = new FormData();
    formData.append("file", file);

    return api.post<PortfolioMedia>(
        `/profiles/me/portfolio/projects/${projectId}/media`,
        formData
    );
  },

  /**
   * Xóa ảnh minh chứng khỏi dự án Portfolio.
   */
  deletePortfolioMedia(
      projectId: string,
      mediaId: string
  ): Promise<ApiResponse<null>> {
    return api.delete<null>(
        `/profiles/me/portfolio/projects/${projectId}/media/${mediaId}`
    );
  },

  // ====================================================
  // HS-06 STAFF PROFILE REVIEW
  // ====================================================

  /**
   * Lấy danh sách hồ sơ chờ duyệt cho Staff.
   */
  getStaffProfileReviews(
      params: StaffProfileReviewsParams = {}
  ): Promise<ApiResponse<{
    items?: StaffPendingReview[];
    content?: StaffPendingReview[];
    totalElements?: number;
    totalPages?: number;
    pagination?: {
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    };
    number?: number;
    size?: number;
  }>> {
    const searchParams = new URLSearchParams();

    if (params.status) {
      searchParams.set("status", params.status);
    }

    if (params.q) {
      searchParams.set("q", params.q);
    }

    if (params.classCode) {
      searchParams.set("classCode", params.classCode);
    }

    if (params.page !== undefined) {
      searchParams.set("page", String(params.page));
    }

    if (params.size !== undefined) {
      searchParams.set("size", String(params.size));
    }

    const queryString = searchParams.toString();
    const url = queryString
        ? `/staff/profile-reviews?${queryString}`
        : "/staff/profile-reviews";

    return api.get(url);
  },

  /**
   * Lấy preview hồ sơ ứng viên cho Staff.
   * Email và số điện thoại được che theo backend.
   */
  getStaffPreviewProfile(
      userId: string
  ): Promise<ApiResponse<StaffPreviewProfile>> {
    return api.get<StaffPreviewProfile>(
        `/staff/profiles/${userId}/preview`
    );
  },

  /**
   * Sửa phần tự khai bởi Staff.
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
   * Duyệt hồ sơ ứng viên (PENDING → PUBLIC).
   */
  approveProfile(
      userId: string
  ): Promise<ApiResponse<{
    userId: string;
    visibility: string;
  }>> {
    return api.post<{
      userId: string;
      visibility: string;
    }>(`/staff/profiles/${userId}/approve`);
  },

  /**
   * Trả lại hồ sơ ứng viên (PENDING → REJECTED).
   */
  rejectProfile(
      userId: string,
      reason: string
  ): Promise<ApiResponse<{
    userId: string;
    visibility: string;
  }>> {
    return api.post<{
      userId: string;
      visibility: string;
    }>(`/staff/profiles/${userId}/reject`, {
      reason,
    });
  },
};
