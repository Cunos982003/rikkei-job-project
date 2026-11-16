import { api } from "../client";
import type { ApiResponse, PaginatedData } from "../types/api";
import type { CompanyProfile } from "./companies.api";

export interface StaffCompanyListItem {
  id: string;
  name: string;
  taxCode: string;
  industry: string | null;
  size: string | null;
  address: string | null;
  city: string | null;
  logoUrl: string | null;
  status: "ACTIVE" | "LOCKED";
  totalHr: number;
  totalJd: number;
}

export interface StaffHrAccount {
  id: string;
  fullName: string;
  email: string;
  status: "ACTIVE" | "LOCKED";
  lockedReason: string | null;
  lockedAt: string | null;
  createdAt: string;
}

export interface StaffCompanyDetail {
  id: string;
  name: string;
  taxCode: string;
  industry: string | null;
  size: string | null;
  address: string | null;
  city: string | null;
  website: string | null;
  logoUrl: string | null;
  description: string | null;
  status: "ACTIVE" | "LOCKED";
  createdAt: string;
  updatedAt: string;
  hrAccounts: StaffHrAccount[];
}

export interface StaffCompanyListParams {
  q?: string;
  status?: string;
  page?: number;
  size?: number;
}

export interface CreateHrAccountRequest {
  fullName: string;
  email: string;
}

export interface CreateHrAccountResponse {
  id: string;
  email: string;
  fullName: string;
  role: string;
  companyId: string;
  mustChangePassword: boolean;
  temporaryPassword?: string;
  emailSent?: boolean;
  message?: string;
}

export interface CreateCompanyRequest {
  name: string;
  taxCode: string;
  industry?: string;
  size?: string;
  address?: string;
  city?: string;
  website?: string;
  description?: string;
}

export interface StaffLogoUploadResponse {
  logoUrl: string;
  message: string;
}

export const staffCompaniesApi = {
  list(params?: StaffCompanyListParams): Promise<ApiResponse<PaginatedData<StaffCompanyListItem>>> {
    return api.get<PaginatedData<StaffCompanyListItem>>("/staff/companies", { params });
  },

  getById(id: string): Promise<ApiResponse<StaffCompanyDetail>> {
    return api.get<StaffCompanyDetail>(`/staff/companies/${id}`);
  },

  createCompany(data: CreateCompanyRequest): Promise<ApiResponse<CompanyProfile>> {
    return api.post<CompanyProfile>("/staff/companies", data);
  },

  uploadLogo(companyId: string, file: File, config?: Parameters<typeof api.post>[2]): Promise<ApiResponse<StaffLogoUploadResponse>> {
    const formData = new FormData();
    formData.append("file", file);
    return api.post<StaffLogoUploadResponse>(`/staff/companies/${companyId}/logo`, formData, {
      ...config,
      headers: {
        ...config?.headers,
        "Content-Type": "multipart/form-data",
      },
    });
  },

  createHrAccount(companyId: string, data: CreateHrAccountRequest): Promise<ApiResponse<CreateHrAccountResponse>> {
    return api.post<CreateHrAccountResponse>(`/staff/companies/${companyId}/hr-accounts`, data);
  },

  lockHrAccount(id: string, reason: string): Promise<ApiResponse<StaffHrAccount>> {
    return api.post<StaffHrAccount>(`/staff/hr-accounts/${id}/lock`, { reason });
  },

  unlockHrAccount(id: string): Promise<ApiResponse<StaffHrAccount>> {
    return api.post<StaffHrAccount>(`/staff/hr-accounts/${id}/unlock`);
  },
};

