import { api } from "../client";
import type { ApiResponse } from "../types/api";
import type { InterviewScheduleItem, InterviewSlotItem } from "./interviews.api";
import type { EmploymentRecordItem, OfferItem } from "./offers.api";

export type RecruitmentStatus =
  | "SUBMITTED"
  | "SCREENING"
  | "INTERVIEW"
  | "OFFER"
  | "HIRED"
  | "REJECTED"
  | "WITHDRAWN";

// ===== CANDIDATE: Ứng tuyển =====

export interface ApplyRequest {
  cvId?: string;
  coverLetter?: string;
}

export interface WithdrawRequest {
  reason?: string;
}

export interface CandidateRecruitmentSummary {
  id: string;
  jobId: string;
  jobTitle?: string;
  companyName?: string;
  cvId: string | null;
  cvSnapshotUrl: string | null;
  coverLetter: string | null;
  status: RecruitmentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CandidateRecruitmentDetails extends Partial<CandidateRecruitmentSummary> {
  recruitment?: CandidateRecruitmentSummary;
  history: RecruitmentHistoryItem[];
}

export type InterviewRoundResult = "PENDING" | "PASS" | "FAIL";

export interface InterviewRoundSummary {
  id: string;
  roundNo: number;
  name: string;
  interviewerEmail: string | null;
  scheduledAt: string | null;
  result: InterviewRoundResult;
  score: number | null;
  note: string | null;
  slots?: InterviewSlotItem[];
  schedule?: InterviewScheduleItem | null;
}

export interface RecruitmentHistoryItem {
  id: string;
  fromStatus: RecruitmentStatus;
  toStatus: RecruitmentStatus;
  actorId: string;
  note: string | null;
  createdAt: string;
}

export interface RecruitmentSummary {
  id: string;
  jobId: string;
  cvId: string | null;
  cvSnapshotUrl: string | null;
  coverLetter: string | null;
  status: RecruitmentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface RecruitmentReviewDetails {
  recruitment: RecruitmentSummary;
  candidateId: string;
  candidateEmail: string;
  coverLetter: string | null;
  cvSnapshotUrl: string | null;
  score: number;
  rounds: InterviewRoundSummary[];
  history: RecruitmentHistoryItem[];
  offer?: OfferItem | null;
  employmentRecord?: EmploymentRecordItem | null;
}

export interface RecruitmentStaffSummary {
  id: string;
  jobId: string;
  candidateId: string;
  candidateEmail: string;
  status: RecruitmentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface TransitionRequest {
  toStatus: RecruitmentStatus;
  note?: string;
}

export interface CreateInterviewRoundRequest {
  name: string;
  interviewerEmail?: string;
  scheduledAt?: string;
}

export interface UpdateInterviewRoundRequest {
  result: InterviewRoundResult;
  score?: number | null;
  note?: string;
  scheduledAt?: string;
}

export interface StaffRecruitmentFilterParams {
  companyId?: string;
  jobId?: string;
  status?: RecruitmentStatus;
  page?: number;
}

export const recruitmentApi = {
  // =============================================
  // CANDIDATE endpoints
  // =============================================

  /**
   * Nộp đơn ứng tuyển vào một JD (gọi tới recruitment-service :8086 qua Gateway)
   */
  applyJob: (jobId: string, data: ApplyRequest): Promise<ApiResponse<CandidateRecruitmentSummary>> => {
    return api.post<CandidateRecruitmentSummary>(`/jobs/${jobId}/recruit`, data);
  },

  /**
   * Danh sách đơn ứng tuyển của ứng viên hiện tại
   */
  getMyRecruitments: (status?: RecruitmentStatus): Promise<ApiResponse<CandidateRecruitmentSummary[]>> => {
    return api.get<CandidateRecruitmentSummary[]>("/me/recruitments", { params: status ? { status } : {} });
  },

  /**
   * Chi tiết đơn ứng tuyển của ứng viên hiện tại (kèm lịch sử)
   */
  getMyRecruitmentDetail: (id: string): Promise<ApiResponse<CandidateRecruitmentDetails>> => {
    return api.get<CandidateRecruitmentDetails>(`/me/recruitments/${id}`);
  },

  /**
   * Rút đơn ứng tuyển (chỉ trước OFFER)
   */
  withdrawRecruitment: (id: string, data: WithdrawRequest): Promise<ApiResponse<CandidateRecruitmentSummary>> => {
    return api.post<CandidateRecruitmentSummary>(`/me/recruitments/${id}/withdraw`, data);
  },

  // =============================================
  // HR / STAFF endpoints
  // =============================================

  /**
   * Lấy chi tiết đơn ứng tuyển cho HR hoặc STAFF
   */
  getReviewDetails: (id: string): Promise<ApiResponse<RecruitmentReviewDetails>> => {
    return api.get<RecruitmentReviewDetails>(`/recruitments/${id}`);
  },

  /**
   * Chuyển trạng thái đơn ứng tuyển (chỉ HR)
   */
  transitionStatus: (id: string, data: TransitionRequest): Promise<ApiResponse<RecruitmentSummary>> => {
    return api.post<RecruitmentSummary>(`/recruitments/${id}/transition`, data);
  },

  /**
   * Tạo vòng phỏng vấn mới (chỉ HR khi đang INTERVIEW)
   */
  createRound: (id: string, data: CreateInterviewRoundRequest): Promise<ApiResponse<InterviewRoundSummary>> => {
    return api.post<InterviewRoundSummary>(`/recruitments/${id}/rounds`, data);
  },

  /**
   * Cập nhật kết quả vòng phỏng vấn (chỉ HR)
   */
  updateRound: (
    id: string,
    roundId: string,
    data: UpdateInterviewRoundRequest
  ): Promise<ApiResponse<InterviewRoundSummary>> => {
    return api.put<InterviewRoundSummary>(`/recruitments/${id}/rounds/${roundId}`, data);
  },

  /**
   * Xóa vòng phỏng vấn (chỉ HR)
   */
  deleteRound: (id: string, roundId: string): Promise<ApiResponse<void>> => {
    return api.delete<void>(`/recruitments/${id}/rounds/${roundId}`);
  },

  /**
   * Danh sách đơn ứng tuyển dành cho HR của doanh nghiệp
   */
  listForHr: (params?: {
    jobId?: string;
    status?: RecruitmentStatus;
    page?: number;
  }): Promise<ApiResponse<SpringPage<RecruitmentStaffSummary>>> => {
    return api.get<SpringPage<RecruitmentStaffSummary>>("/hr/applications", { params });
  },

  /**
   * Danh sách đơn ứng tuyển dành cho STAFF
   */
  listForStaff: (params: StaffRecruitmentFilterParams): Promise<ApiResponse<SpringPage<RecruitmentStaffSummary>>> => {
    return api.get<SpringPage<RecruitmentStaffSummary>>("/staff/recruitments", { params });
  },

  /**
   * Tải URL xem trước CV snapshot
   */
  getCvSnapshotUrl: (id: string): string => {
    return `/api/v1/recruitments/${id}/cv-snapshot`;
  },
};
