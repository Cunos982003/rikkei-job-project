import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  interviewPracticeApi,
  type CreatePracticePayload,
  type SubmitAnswerPayload,
  type InterviewPracticeItem,
  type PracticeQuestion,
} from "../../../api/endpoints/interviewPractice.api";
import type { ApiResponse } from "../../../api/types/api";

export const INTERVIEW_QUERY_KEYS = {
  all: ["interview-practices"] as const,
  detail: (id: string) => ["interview-practices", id] as const,
};

export function useInterviewPracticeList() {
  return useQuery<InterviewPracticeItem[]>({
    queryKey: INTERVIEW_QUERY_KEYS.all,
    queryFn: async () => {
      const res = await interviewPracticeApi.listPractices();
      return res.data || [];
    },
    staleTime: 60 * 1000,
  });
}

export function useInterviewPractice(id: string | undefined) {
  return useQuery<InterviewPracticeItem>({
    queryKey: INTERVIEW_QUERY_KEYS.detail(id || ""),
    queryFn: async () => {
      if (!id) throw new Error("Practice ID is required");
      const res = await interviewPracticeApi.getPractice(id);
      return res.data;
    },
    enabled: Boolean(id),
    staleTime: 10 * 1000,
  });
}

export function useCreatePractice() {
  const queryClient = useQueryClient();

  return useMutation<ApiResponse<InterviewPracticeItem>, any, CreatePracticePayload>({
    mutationFn: (payload: CreatePracticePayload) => interviewPracticeApi.createPractice(payload),
    onSuccess: (res) => {
      toast.success(res.message || "Bắt đầu buổi luyện phỏng vấn thành công!");
      queryClient.invalidateQueries({ queryKey: INTERVIEW_QUERY_KEYS.all });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || "Không thể khởi tạo buổi phỏng vấn. Vui lòng thử lại.";
      toast.error(msg);
    },
  });
}

export function useSubmitAnswer(practiceId: string) {
  const queryClient = useQueryClient();

  return useMutation<ApiResponse<PracticeQuestion>, any, { questionId: string; payload: SubmitAnswerPayload }>({
    mutationFn: ({ questionId, payload }: { questionId: string; payload: SubmitAnswerPayload }) =>
      interviewPracticeApi.submitAnswer(practiceId, questionId, payload),
    onSuccess: (res) => {
      toast.success(res.message || "AI đã chấm điểm câu trả lời!");
      queryClient.invalidateQueries({ queryKey: INTERVIEW_QUERY_KEYS.detail(practiceId) });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || "Lỗi khi chấm điểm câu trả lời.";
      toast.error(msg);
    },
  });
}

export function useFinishPractice(practiceId: string) {
  const queryClient = useQueryClient();

  return useMutation<ApiResponse<InterviewPracticeItem>, any, void>({
    mutationFn: () => interviewPracticeApi.finishPractice(practiceId),
    onSuccess: (res) => {
      toast.success(res.message || "Hoàn thành buổi luyện tập!");
      queryClient.invalidateQueries({ queryKey: INTERVIEW_QUERY_KEYS.detail(practiceId) });
      queryClient.invalidateQueries({ queryKey: INTERVIEW_QUERY_KEYS.all });
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || "Không thể kết thúc buổi phỏng vấn.";
      toast.error(msg);
    },
  });
}
