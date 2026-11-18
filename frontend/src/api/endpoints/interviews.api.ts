import { api } from "../client";
import type { ApiResponse } from "../types/api";
import type { InterviewRoundResult, RecruitmentStatus } from "./recruitment.api";

export type InterviewSlotStatus = "OPEN" | "PICKED" | "CANCELLED";
export type InterviewScheduleStatus = "SCHEDULED" | "DONE" | "CANCELLED";

export interface InterviewSlotInput {
  startAt: string;
  endAt: string;
}

export interface InterviewSlotItem {
  id: string;
  roundId: string;
  startAt: string;
  endAt: string;
  status: InterviewSlotStatus;
}

export interface InterviewScheduleItem {
  id: string;
  roundId: string;
  slotId: string;
  startAt: string;
  endAt: string;
  meetUrl: string;
  reminded24hAt: string | null;
  reminded1hAt: string | null;
  status: InterviewScheduleStatus;
}

export interface CandidateInterviewItem {
  roundId: string;
  recruitmentId: string;
  jobId: string;
  companyId: string | null;
  roundNo: number;
  roundName: string;
  interviewerEmail: string | null;
  result: InterviewRoundResult;
  score: number | null;
  note: string | null;
  applicationStatus?: RecruitmentStatus;
  slots: InterviewSlotItem[];
  schedule: InterviewScheduleItem | null;
}

export interface CompanyInterviewItem {
  scheduleId: string;
  roundId: string;
  recruitmentId: string;
  jobId: string;
  candidateId: string;
  candidateEmail: string | null;
  roundNo: number;
  roundName: string;
  interviewerEmail: string | null;
  startAt: string;
  endAt: string;
  meetUrl: string;
  status: InterviewScheduleStatus;
  reminded24hAt: string | null;
  reminded1hAt: string | null;
}

export interface RescheduleResponse {
  roundId: string;
  cancelledSchedulesCount: number;
  cancelledSlotsCount: number;
  slots: InterviewSlotItem[];
}

export const interviewsApi = {
  proposeSlots: (
    roundId: string,
    slots: InterviewSlotInput[]
  ): Promise<ApiResponse<InterviewSlotItem[]>> => {
    return api.post<InterviewSlotItem[]>(`/rounds/${roundId}/slots`, slots);
  },

  listRoundSlots: (roundId: string): Promise<ApiResponse<InterviewSlotItem[]>> => {
    return api.get<InterviewSlotItem[]>(`/rounds/${roundId}/slots`);
  },

  reschedule: (
    roundId: string,
    slots: InterviewSlotInput[]
  ): Promise<ApiResponse<RescheduleResponse>> => {
    return api.post<RescheduleResponse>(`/rounds/${roundId}/reschedule`, slots);
  },

  listMyInterviews: (): Promise<ApiResponse<CandidateInterviewItem[]>> => {
    return api.get<CandidateInterviewItem[]>("/me/interviews");
  },

  pickSlot: (
    roundId: string,
    slotId: string
  ): Promise<ApiResponse<InterviewScheduleItem>> => {
    return api.post<InterviewScheduleItem>(`/me/interviews/${roundId}/pick`, { slotId });
  },

  listCompanyInterviews: (
    from: string,
    to: string
  ): Promise<ApiResponse<CompanyInterviewItem[]>> => {
    return api.get<CompanyInterviewItem[]>("/companies/me/interviews", {
      params: { from, to },
    });
  },
};
