
import { api } from "../client";
import type { ApiResponse } from "../types/api";

/**
 * Một kỹ năng trong khung đánh giá DG-01.
 */
export interface AssessmentSkill {
    skillId: string;
    skillName: string;
    description?: string;
}

/**
 * Khung tiêu chí tự đánh giá.
 */
export interface SelfAssessmentTemplate {
    templateId: string;
    name: string;
    skills: AssessmentSkill[];
}

/**
 * Một dòng điểm tự đánh giá.
 */
export interface SelfAssessmentScoreRequest {
    skillId: string;
    score: number;
    note?: string;
}

/**
 * Request gửi bản tự đánh giá.
 */
export interface SelfAssessmentRequest {
    templateId: string;
    scores: SelfAssessmentScoreRequest[];
}

/**
 * Trạng thái so sánh.
 */
export type ComparisonFlag =
    | "OVER"
    | "UNDER"
    | "MATCH"
    | "NO_DATA";

/**
 * Một kỹ năng trong kết quả so sánh.
 */
export interface SkillComparisonItem {
    skillId: string;
    skillName: string;
    measured: number | null;
    self: number | null;
    gap: number | null;
    flag: ComparisonFlag;
}

/**
 * DG-02 API
 *
 * Các endpoint theo đặc tả task.
 */
export const selfAssessmentApi = {
    /**
     * Lấy khung tiêu chí DG-01.
     */
    getTemplate(): Promise<ApiResponse<SelfAssessmentTemplate>> {
        return api.get<SelfAssessmentTemplate>(
            "/me/self-assessment/template",
            { silentError: true }
        );
    },

    /**
     * Gửi bản tự đánh giá.
     */
    submit(
        data: SelfAssessmentRequest
    ): Promise<ApiResponse<unknown>> {
        return api.post<unknown>(
            "/me/self-assessments",
            data
        );
    },

    /**
     * Lấy dữ liệu so sánh giữa điểm tự đánh giá
     * và điểm đo thực tế.
     */
    getComparison(): Promise<ApiResponse<SkillComparisonItem[]>> {
        return api.get<SkillComparisonItem[]>(
            "/me/skill-comparison"
        );
    },
};