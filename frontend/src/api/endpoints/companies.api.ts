import { api } from "../client";
import type { ApiResponse } from "../types/api";

export interface CompanyProfile {
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
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateCompanyProfileRequest {
  name: string;
  industry: string;
  size: string | null;
  address: string;
  city: string;
  website: string;
  description: string;
}

export interface CompanyLogoResponse {
  logoUrl: string;
  message: string;
}

export const companiesApi = {
  getMyCompany(): Promise<ApiResponse<CompanyProfile>> {
    return api.get<CompanyProfile>("/companies/me");
  },

  updateMyCompany(data: UpdateCompanyProfileRequest): Promise<ApiResponse<CompanyProfile>> {
    return api.put<CompanyProfile>("/companies/me", data);
  },

  uploadMyCompanyLogo(file: File): Promise<ApiResponse<CompanyLogoResponse>> {
    const formData = new FormData();
    formData.append("file", file);
    return api.post<CompanyLogoResponse>("/companies/me/logo", formData);
  },
};
