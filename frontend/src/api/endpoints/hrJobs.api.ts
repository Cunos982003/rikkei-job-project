import { api } from "../client";
import type { ApiResponse, PaginatedData } from "../types/api";

export type JobStatus = "DRAFT" | "PENDING" | "APPROVED" | "REJECTED" | "CLOSED";
export type JobLevel = "INTERN" | "FRESHER" | "JUNIOR" | "MIDDLE" | "SENIOR";
export type JobType = "FULLTIME" | "PARTTIME" | "INTERNSHIP";
export type WorkMode = "ONSITE" | "REMOTE" | "HYBRID";

export interface JobSkillResponse {
  skillId: string;
  skillCode?: string;
  skillName: string;
  required: boolean;
  minScore: number;
}

export interface JobResponse {
  id: string;
  companyId?: string;
  companyName?: string;
  title: string;
  position: string;
  level: JobLevel;
  type: JobType;
  quantity: number;
  city?: string;
  address?: string;
  workMode: WorkMode;
  salaryMin?: number;
  salaryMax?: number;
  description?: string;
  requirements?: string;
  benefits?: string;
  deadline: string; // yyyy-MM-dd
  expiresAt?: string;
  status: JobStatus;
  rejectedReason?: string | null;
  reviewedBy?: string;
  reviewedAt?: string;
  skills: JobSkillResponse[];
  jobSkills?: JobSkillResponse[];
  requiredSkills?: string[];
  hrEmails?: string[];
  createdBy?: string;
  createdAt?: string;
  updatedAt: string;
}

export interface JobListItem {
  id: string;
  title: string;
  position: string;
  level: JobLevel;
  type: JobType;
  quantity: number;
  city?: string;
  deadline: string;
  status: JobStatus;
  rejectedReason?: string | null;
  requiredSkillCount: number;
  updatedAt: string;
}

export interface CreateJobDto {
  title: string;
  position: string;
  level: JobLevel;
  type: JobType;
  quantity: number;
  city?: string;
  address?: string;
  workMode: WorkMode;
  salaryMin?: number;
  salaryMax?: number;
  description?: string;
  requirements?: string;
  benefits?: string;
  deadline: string;
}

export interface UpdateJobDto extends CreateJobDto {}

export interface JobSkillItemDto {
  skillId: string;
  required: boolean;
  minScore: number;
}

export interface JobFilterParams {
  status?: string;
  page?: number;
  size?: number;
}

export const hrJobsApi = {
  createJob(body: CreateJobDto): Promise<ApiResponse<JobResponse>> {
    return api.post<JobResponse>("/jobs", body);
  },

  updateJob(id: string, body: UpdateJobDto): Promise<ApiResponse<JobResponse>> {
    return api.put<JobResponse>(`/jobs/${id}`, body);
  },

  replaceJobSkills(id: string, items: JobSkillItemDto[]): Promise<ApiResponse<JobResponse>> {
    return api.put<JobResponse>(`/jobs/${id}/skills`, items);
  },

  submitJob(id: string): Promise<ApiResponse<JobResponse>> {
    return api.post<JobResponse>(`/jobs/${id}/submit`);
  },

  closeJob(id: string): Promise<ApiResponse<JobResponse>> {
    return api.post<JobResponse>(`/jobs/${id}/close`);
  },

  getJob(id: string): Promise<ApiResponse<JobResponse>> {
    return api.get<JobResponse>(`/jobs/${id}`);
  },

  getMyJobs(params?: JobFilterParams): Promise<ApiResponse<PaginatedData<JobListItem>>> {
    return api.get<PaginatedData<JobListItem>>("/companies/me/jobs", { params });
  },
};

