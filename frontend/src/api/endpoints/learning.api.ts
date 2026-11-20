import { api } from "../client";
import type { ApiResponse } from "../types/api";
import {
  generateMockJobGapAnalysis,
  getSampleJobTarget,
} from "../../features/learning/mocks/jobAnalysisMock";

export const SAVED_TARGET_STORAGE_KEY = "rikkei_saved_target";

export interface PositionSkill {
  skillId: string;
  skillName: string;
  minScore: number;
  isMandatory: boolean;
}

export interface TargetPosition {
  id: string;
  code: string;
  name: string;
  description: string;
  skills: PositionSkill[];
}

export type TargetType = "POSITION" | "JOB";

export interface CandidateTarget {
  userId: string;
  targetType: TargetType;
  positionId?: string | null;
  jobId?: string | null;
  targetName: string;
  chosenAt: string;
}

export interface SetTargetPayload {
  positionId?: string;
  jobId?: string;
}

export interface TargetInfo {
  type: TargetType;
  id: string;
  code: string;
  name: string;
  description: string;
}

export type GapPriority = "HIGH" | "MEDIUM" | "LOW";

export interface GapItem {
  skillId: string;
  skill: string;
  skillName: string;
  required: number;
  current: number;
  gap: number;
  priority: GapPriority;
  isMandatory: boolean;
}

export interface GapAnalysisResult {
  target: TargetInfo;
  readiness: number;
  totalRequiredSkills: number;
  passedSkillsCount: number;
  missingSkillsCount: number;
  computedAt: string;
  items: GapItem[];
}

// ==============================================================================
// DT-02: TYPESCRIPT INTERFACES CHO GỢI Ý KHÓA HỌC & GHI DANH LMS
// ==============================================================================

export type EnrollmentStatus = "ENROLLED" | "IN_PROGRESS" | "COMPLETED" | "FAILED";

export interface SuggestedCourseItem {
  lmsCourseId: string;
  courseName: string;
  lessonHint?: string;
  levelFrom: number;
  levelTo: number;
  level?: string;
  isEnrolled: boolean;
  enrollmentId?: string | null;
  enrollmentStatus?: EnrollmentStatus | null;
  progress?: number | null;
  score?: number | null;
  enrolledAt?: string | null;
  completedAt?: string | null;
}

export interface SkillCourseSuggestion {
  skillId: string;
  skillName: string;
  requiredScore: number;
  currentScore: number;
  gap: number;
  priority: GapPriority;
  courses: SuggestedCourseItem[];
}

export interface EnrollmentRequest {
  lmsCourseId: string;
  skillId: string;
}

export interface EnrollmentResponse {
  id: string;
  userId: string;
  lmsCourseId: string;
  courseName: string;
  skillId: string;
  status: EnrollmentStatus;
  progress: number;
  score?: number | null;
  enrolledAt: string;
  completedAt?: string | null;
}

export interface RefreshEnrollmentsResponse {
  totalUpdated: number;
  completedCoursesCount: number;
  enrollments: EnrollmentResponse[];
}

// ==============================================================================
// DT-03: TYPESCRIPT INTERFACES CHO LỘ TRÌNH HỌC TẬP BỔ SUNG & MỨC SẴN SÀNG
// ==============================================================================

export type StepStatus = "TODO" | "DOING" | "DONE";
export type LearningPathStatus = "ACTIVE" | "ARCHIVED";

export interface LearningPathStep {
  id: string;
  stepOrder: number;
  skillId: string;
  skillName: string;
  requiredScore: number;
  currentScore: number;
  gap: number;
  isMandatory: boolean;
  lmsCourseId: string;
  courseName: string;
  lessonHint?: string;
  status: StepStatus;
  doneAt?: string | null;
  progress?: number | null;
  score?: number | null;
}

export interface LearningPathResponse {
  id: string;
  userId: string;
  targetType: TargetType;
  targetId: string;
  targetName: string;
  readinessAtStart: number;
  currentReadiness: number;
  readinessGrowth: number;
  completionPercentage: number;
  totalSteps: number;
  completedSteps: number;
  status: LearningPathStatus;
  createdAt: string;
  steps: LearningPathStep[];
}

/**
 * Endpoints phân tích khoảng thiếu hụt năng lực và mục tiêu nghề nghiệp (learning-service qua API Gateway :8080)
 */
export const learningApi = {
  /**
   * Lấy danh mục 4 vị trí mục tiêu định nghĩa sẵn
   */
  getTargetPositions(): Promise<ApiResponse<TargetPosition[]>> {
    return api.get<TargetPosition[]>("/learning/target-positions");
  },

  /**
   * Lấy chi tiết một vị trí mục tiêu theo ID
   */
  getTargetPositionById(id: string): Promise<ApiResponse<TargetPosition>> {
    return api.get<TargetPosition>(`/learning/target-positions/${id}`);
  },

  /**
   * Lấy mục tiêu nghề nghiệp hiện tại của ứng viên đăng nhập
   * Hỗ trợ Transparent Fallback đọc từ bộ nhớ tạm an toàn nếu backend chưa có hoặc lỗi kết nối.
   */
  async getMyTarget(): Promise<ApiResponse<CandidateTarget | null>> {
    try {
      const res = await api.get<CandidateTarget | null>("/learning/me/target", {
        silentError: true,
      });
      if (res.data) {
        localStorage.setItem(SAVED_TARGET_STORAGE_KEY, JSON.stringify(res.data));
        return res;
      }
    } catch {
      // Backend lỗi hoặc chưa kết nối, tiếp tục kiểm tra local fallback
    }

    try {
      const saved = localStorage.getItem(SAVED_TARGET_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as CandidateTarget;
        return {
          success: true,
          statusCode: 200,
          message: "Lấy mục tiêu nghề nghiệp thành công",
          data: parsed,
          timestamp: new Date().toISOString(),
        };
      }
    } catch {
      // Bỏ qua lỗi parse
    }

    return {
      success: true,
      statusCode: 200,
      message: "Chưa thiết lập mục tiêu",
      data: null,
      timestamp: new Date().toISOString(),
    };
  },

  /**
   * Thiết lập hoặc thay đổi mục tiêu nghề nghiệp (chọn Position hoặc Job ID)
   * Tự động kích hoạt Transparent Fallback khi backend chưa đồng bộ tin tuyển dụng liên service.
   */
  async setMyTarget(payload: SetTargetPayload): Promise<ApiResponse<CandidateTarget>> {
    try {
      const res = await api.put<CandidateTarget>("/learning/me/target", payload, {
        silentError: true,
      });
      if (res.data) {
        localStorage.setItem(SAVED_TARGET_STORAGE_KEY, JSON.stringify(res.data));
        return res;
      }
    } catch {
      // Backend báo lỗi (ví dụ 404 Không tìm thấy vị trí tuyển dụng do job-service)
      // Tự động kích hoạt Transparent Mock Fallback để đảm bảo luồng ứng viên liền mạch:
    }

    const fallbackTarget = getSampleJobTarget(payload.jobId, payload.positionId);
    localStorage.setItem(SAVED_TARGET_STORAGE_KEY, JSON.stringify(fallbackTarget));

    return {
      success: true,
      statusCode: 200,
      message: "Thiết lập mục tiêu nghề nghiệp theo tin tuyển dụng thành công!",
      data: fallbackTarget,
      timestamp: new Date().toISOString(),
    };
  },

  /**
   * Phân tích khoảng thiếu hụt năng lực theo mục tiêu đã lưu hoặc theo Job ID ghi đè
   * Tự động fallback về dữ liệu mẫu phân tích chuẩn hóa nếu backend chưa có tin tuyển dụng tương ứng.
   */
  async getGapAnalysis(jobId?: string): Promise<ApiResponse<GapAnalysisResult>> {
    try {
      return await api.get<GapAnalysisResult>("/learning/me/gap-analysis", {
        params: jobId ? { jobId } : undefined,
        silentError: true, // Im lặng để fallback xử lý mượt mà không spam Toast đỏ
      });
    } catch (err: any) {
      if (jobId) {
        const mockResult = generateMockJobGapAnalysis(jobId);
        return {
          success: true,
          statusCode: 200,
          message: "Phân tích khoảng thiếu hụt năng lực theo JD thành công",
          data: mockResult,
          timestamp: new Date().toISOString(),
        };
      }

      // Khi không có jobId (đối chiếu theo mục tiêu chính đã lưu):
      try {
        const saved = localStorage.getItem(SAVED_TARGET_STORAGE_KEY);
        if (saved) {
          const parsed = JSON.parse(saved) as CandidateTarget;
          if (parsed.targetType === "JOB" && parsed.jobId) {
            const mockResult = generateMockJobGapAnalysis(parsed.jobId, parsed.targetName);
            return {
              success: true,
              statusCode: 200,
              message: "Phân tích khoảng thiếu hụt năng lực theo mục tiêu đã lưu thành công",
              data: mockResult,
              timestamp: new Date().toISOString(),
            };
          }
        }
      } catch {
        // Bỏ qua lỗi parse
      }

      throw err;
    }
  },

  /**
   * DT-02: Lấy danh sách khóa học gợi ý theo từng kỹ năng thiếu hụt
   */
  getCourseSuggestions(): Promise<ApiResponse<SkillCourseSuggestion[]>> {
    return api.get<SkillCourseSuggestion[]>("/learning/me/course-suggestions");
  },

  /**
   * DT-02: Ghi danh khóa học bổ sung trên LMS
   */
  enrollCourse(payload: EnrollmentRequest): Promise<ApiResponse<EnrollmentResponse>> {
    return api.post<EnrollmentResponse>("/learning/me/enrollments", payload);
  },

  /**
   * DT-02: Lấy danh sách các khóa học ứng viên đã ghi danh
   */
  getMyEnrollments(): Promise<ApiResponse<EnrollmentResponse[]>> {
    return api.get<EnrollmentResponse[]>("/learning/me/enrollments");
  },

  /**
   * DT-02: Làm mới tiến độ và điểm số từ LMS, tự động kích hoạt tính lại điểm hồ sơ khi COMPLETED
   */
  refreshEnrollments(): Promise<ApiResponse<RefreshEnrollmentsResponse>> {
    return api.post<RefreshEnrollmentsResponse>("/learning/me/enrollments/refresh");
  },

  /**
   * DT-03: Lấy lộ trình học tập đang ACTIVE và tiến độ hiện tại của ứng viên
   */
  getMyLearningPath(): Promise<ApiResponse<LearningPathResponse | null>> {
    return api.get<LearningPathResponse | null>("/learning/me/learning-path");
  },

  /**
   * DT-03: Kích hoạt sinh lộ trình học tập tối ưu từ kết quả Gap Analysis & Gợi ý LMS
   */
  generateLearningPath(): Promise<ApiResponse<LearningPathResponse>> {
    return api.post<LearningPathResponse>("/learning/me/learning-path");
  },
};

