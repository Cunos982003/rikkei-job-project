import api from "../client";
import type { ApiResponse } from "../types/api";

export interface MatchSkillItem {
  skill: string;
  skillCode: string;
  required: boolean;
  minScore: number;
  candidateScore: number;
  gap: number;
}

export interface MatchResult {
  jobId: string;
  userId: string;
  score: number;
  belowMinimum: boolean;
  met: MatchSkillItem[];
  missing: MatchSkillItem[];
  computedAt: string;
  stale: boolean;
}

export const matchApi = {
  getMyMatch(jobId: string): Promise<ApiResponse<MatchResult>> {
    return api.get<MatchResult>(`/jobs/${jobId}/match/me`, { silentError: true });
  },

  getCandidateMatch(jobId: string, userId: string): Promise<ApiResponse<MatchResult>> {
    return api.get<MatchResult>(`/jobs/${jobId}/match/${userId}`, { silentError: true });
  },
};
