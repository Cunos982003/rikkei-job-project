import axios from "axios";
import { getAccessToken, getCurrentUserId } from "../client";

export type AIDraftSection = "SUMMARY" | "PROJECT";
export type AIDraftStatus = "DRAFT" | "ACCEPTED" | "DISCARDED";

export interface AIDraftRequest {
  section: AIDraftSection;
  projectId?: string | null;
  jobId?: string | null;
}

export interface AIDraftResponse {
  id: string;
  cvId: string;
  section: AIDraftSection;
  projectId?: string | null;
  targetJobId?: string | null;
  output: string;
  warnings: string[];
  status: AIDraftStatus;
  createdAt: string;
}

const AI_CAREER_BASE = import.meta.env.VITE_AI_CAREER_BASE_URL || "/ai-career";

function buildHeaders() {
  const token = getAccessToken();
  const userId = getCurrentUserId();
  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (userId) {
    headers["X-User-Id"] = userId;
  }
  return headers;
}

export async function createAIDraft(cvId: string, payload: AIDraftRequest): Promise<AIDraftResponse> {
  const res = await axios.post<AIDraftResponse>(`/cvs/${cvId}/ai-write`, payload, {
    baseURL: AI_CAREER_BASE,
    headers: buildHeaders(),
    timeout: 30000,
  });
  return res.data;
}

export async function acceptAIDraft(draftId: string): Promise<AIDraftResponse> {
  const res = await axios.post<AIDraftResponse>(
    `/ai-drafts/${draftId}/accept`,
    {},
    {
      baseURL: AI_CAREER_BASE,
      headers: buildHeaders(),
      timeout: 15000,
    },
  );
  return res.data;
}

export async function discardAIDraft(draftId: string): Promise<AIDraftResponse> {
  const res = await axios.post<AIDraftResponse>(
    `/ai-drafts/${draftId}/discard`,
    {},
    {
      baseURL: AI_CAREER_BASE,
      headers: buildHeaders(),
      timeout: 15000,
    },
  );
  return res.data;
}

export async function getAIDraftHistory(cvId: string): Promise<AIDraftResponse[]> {
  const res = await axios.get<AIDraftResponse[]>(`/cvs/${cvId}/ai-drafts`, {
    baseURL: AI_CAREER_BASE,
    headers: buildHeaders(),
    timeout: 15000,
  });
  return Array.isArray(res.data) ? res.data : [];
}
