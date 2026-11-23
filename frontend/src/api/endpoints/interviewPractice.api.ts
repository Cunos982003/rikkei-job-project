import axios from "axios";
import { getAccessToken, getCurrentUserId } from "../client";
import type { ApiResponse } from "../types/api";

export interface PracticeQuestion {
  id: string;
  qOrder: number;
  question: string;
  type: "TECH" | "BEHAVIOR";
  answer?: string | null;
  score?: number | null;
  feedback?: string | null;
  suggestion?: string | null;
}

export interface InterviewPracticeItem {
  id: string;
  jobId: string;
  jobTitle: string;
  status: "IN_PROGRESS" | "DONE";
  overallScore?: number | null;
  summary?: string | null;
  createdAt: string;
  questions: PracticeQuestion[];
}

export interface CreatePracticePayload {
  jobId: string;
}

export interface SubmitAnswerPayload {
  answer: string;
}

const interviewAxios = axios.create({
  baseURL: "/me/interview-practices",
  timeout: 45000,
  headers: {
    "Content-Type": "application/json",
  },
});

interviewAxios.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  const userId = getCurrentUserId();
  if (userId) {
    config.headers["X-User-Id"] = userId;
  }
  return config;
});

interviewAxios.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
);

export const interviewPracticeApi = {
  listPractices: async (): Promise<ApiResponse<InterviewPracticeItem[]>> => {
    return interviewAxios.get("");
  },

  getPractice: async (id: string): Promise<ApiResponse<InterviewPracticeItem>> => {
    return interviewAxios.get(`/${id}`);
  },

  createPractice: async (payload: CreatePracticePayload): Promise<ApiResponse<InterviewPracticeItem>> => {
    return interviewAxios.post("", payload);
  },

  submitAnswer: async (practiceId: string, questionId: string, payload: SubmitAnswerPayload): Promise<ApiResponse<PracticeQuestion>> => {
    return interviewAxios.post(`/${practiceId}/questions/${questionId}/answer`, payload);
  },

  finishPractice: async (practiceId: string): Promise<ApiResponse<InterviewPracticeItem>> => {
    return interviewAxios.post(`/${practiceId}/finish`);
  },
};
