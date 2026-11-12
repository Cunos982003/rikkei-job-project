import axios from "axios";

export interface SkillGap {
  skillName: string;
  verifiedIndex: number | null;
  requiredIndex: number;
  gap: number;
  required: boolean;
}

export interface JobRecommendation {
  jobId: string;
  score: number;
  explanation: string;
  createdAt: string;
  missingRequiredSkills: string[];
  skillGaps: SkillGap[];
}

export async function fetchRecommendations(limit: number, token: string, signal?: AbortSignal) {
  const response = await axios.get<JobRecommendation[]>("/me/job-recommendations", {
    baseURL: import.meta.env.VITE_AI_CAREER_BASE_URL || "/ai-career",
    params: { limit },
    timeout: 15000,
    headers: { Accept: "application/json", Authorization: `Bearer ${token}` },
    signal,
  });
  return response.data;
}

export function getCareerApiError(error: unknown) {
  if (!axios.isAxiosError<{ detail?: string; message?: string; title?: string }>(error)) {
    return { status: undefined, message: error instanceof Error ? error.message : "Đã xảy ra lỗi không xác định." };
  }
  const status = error.response?.status;
  const data = error.response?.data;
  const message = data?.detail || data?.message || data?.title;
  if (status === 401) return { status, message: message || "Phiên đăng nhập chưa được xác thực." };
  if (status === 503) return { status, message: message || "Nguồn dữ liệu hồ sơ hoặc việc làm chưa sẵn sàng." };
  if (!error.response) return { status, message: "Không thể kết nối tới AI Career Service. Vui lòng thử lại." };
  return { status, message: message || `Yêu cầu thất bại (HTTP ${status}).` };
}
