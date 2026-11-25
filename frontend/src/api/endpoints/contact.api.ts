// Contact Access API types and endpoints for UT-03

export type ContactRequestStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ContactAccessVia = "APPLICATION" | "APPROVED_REQUEST";

export interface ContactRequest {
  id: string;
  companyId: string;
  userId: string;
  jobId?: string;
  reason: string;
  status: ContactRequestStatus;
  reviewedBy?: string;
  reviewedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateContactRequestRequest {
  jobId?: string;
  reason: string;
}

export interface ContactResponse {
  email: string;
  phone: string;
}

export interface ContactAccessLog {
  id: string;
  companyId: string;
  companyName?: string;
  hrId: string;
  userId: string;
  via: ContactAccessVia;
  createdAt: string;
}

export interface StaffContactAccessLogsParams {
  companyId?: string;
  userId?: string;
  from?: string;
  to?: string;
}

import { api } from "../client";
import type { ApiResponse } from "../types/api";

export const contactApi = {
  // HR: Create contact request
  createContactRequest(userId: string, data: CreateContactRequestRequest): Promise<ApiResponse<ContactRequest>> {
    return api.post<ContactRequest>(`/candidates/${userId}/contact-requests`, data);
  },

  // HR: Get contact info (returns 403 if not allowed)
  getContact(userId: string): Promise<ApiResponse<ContactResponse>> {
    return api.get<ContactResponse>(`/candidates/${userId}/contact`, { silentError: true });
  },

  // Candidate: Get my access logs
  getMyAccessLogs(): Promise<ApiResponse<ContactAccessLog[]>> {
    return api.get<ContactAccessLog[]>("/me/contact-access-logs", { silentError: true });
  },

  // STAFF: Get contact requests
  getContactRequests(status?: ContactRequestStatus): Promise<ApiResponse<ContactRequest[]>> {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    return api.get<ContactRequest[]>(`/staff/contact-requests?${params.toString()}`);
  },

  // STAFF: Approve request
  approveContactRequest(id: string): Promise<ApiResponse<ContactRequest>> {
    return api.post<ContactRequest>(`/staff/contact-requests/${id}/approve`);
  },

  // STAFF: Reject request
  rejectContactRequest(id: string, reason: string): Promise<ApiResponse<ContactRequest>> {
    return api.post<ContactRequest>(`/staff/contact-requests/${id}/reject`, { reason });
  },

  // STAFF: Get access logs
  getAccessLogs(params: StaffContactAccessLogsParams = {}): Promise<ApiResponse<ContactAccessLog[]>> {
    const searchParams = new URLSearchParams();
    if (params.companyId) searchParams.set("companyId", params.companyId);
    if (params.userId) searchParams.set("userId", params.userId);
    if (params.from) searchParams.set("from", params.from);
    if (params.to) searchParams.set("to", params.to);
    return api.get(`/staff/contact-access-logs?${searchParams.toString()}`);
  },
};