import { api } from "../client";
import type { ApiResponse } from "../types/api";
import type { RecruitmentStatus, RecruitmentStaffSummary, SpringPage } from "./recruitment.api";

export type OfferStatus = "SENT" | "ACCEPTED" | "DECLINED" | "EXPIRED";

export interface OfferItem {
  id: string;
  applicationId: string;
  jobId: string;
  companyId: string | null;
  companyName: string | null;
  candidateId: string;
  candidateEmail: string | null;
  position: string;
  salary: number;
  startDate: string;
  workLocation: string;
  note: string | null;
  responseDeadline: string;
  status: OfferStatus;
  applicationStatus: RecruitmentStatus | null;
  respondedAt: string | null;
  declineReason: string | null;
  confirmedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SendOfferPayload {
  position: string;
  salary: number;
  startDate: string;
  workLocation: string;
  responseDeadline: string;
  note?: string;
  companyName?: string;
}

export interface EmploymentRecordItem {
  id: string;
  userId: string;
  companyId: string;
  companyName: string | null;
  jobId: string;
  applicationId: string | null;
  position: string;
  startDate: string;
  confirmedAt: string | null;
  createdAt: string;
}

export interface HrApplicationsFilter {
  jobId?: string;
  status?: RecruitmentStatus;
  page?: number;
}

export const offersApi = {
  listHrApplications(params?: HrApplicationsFilter): Promise<ApiResponse<SpringPage<RecruitmentStaffSummary>>> {
    return api.get<SpringPage<RecruitmentStaffSummary>>("/hr/applications", { params });
  },

  sendOffer(applicationId: string, payload: SendOfferPayload): Promise<ApiResponse<OfferItem>> {
    return api.post<OfferItem>(`/applications/${applicationId}/offer`, payload);
  },

  confirmOnboard(applicationId: string): Promise<ApiResponse<EmploymentRecordItem>> {
    return api.post<EmploymentRecordItem>(`/applications/${applicationId}/offer/confirm-onboard`);
  },

  listMyOffers(): Promise<ApiResponse<OfferItem[]>> {
    return api.get<OfferItem[]>("/me/offers");
  },

  getMyOffer(offerId: string): Promise<ApiResponse<OfferItem>> {
    return api.get<OfferItem>(`/me/offers/${offerId}`);
  },

  acceptOffer(offerId: string): Promise<ApiResponse<OfferItem>> {
    return api.post<OfferItem>(`/me/offers/${offerId}/accept`);
  },

  declineOffer(offerId: string, reason: string): Promise<ApiResponse<OfferItem>> {
    return api.post<OfferItem>(`/me/offers/${offerId}/decline`, { reason });
  },

  listMyEmploymentRecords(): Promise<ApiResponse<EmploymentRecordItem[]>> {
    return api.get<EmploymentRecordItem[]>("/me/employment-records");
  },
};
