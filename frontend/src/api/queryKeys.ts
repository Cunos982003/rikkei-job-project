/**
 * Query Keys chuẩn hóa cho toàn bộ hệ thống frontend Rikkei Jobs.
 * Định dạng phân cấp giúp quản lý cache và vô hiệu hóa (invalidation) đồng bộ giữa các trang.
 */

export const candidateKeys = {
  all: ["candidate"] as const,

  // Hồ sơ ứng viên (Profile)
  profile: {
    all: () => [...candidateKeys.all, "profile"] as const,
    detail: (userId?: string) => [...candidateKeys.profile.all(), "detail", userId ?? "me"] as const,
    educations: (userId?: string) => [...candidateKeys.profile.all(), "educations", userId ?? "me"] as const,
    certificates: (userId?: string) => [...candidateKeys.profile.all(), "certificates", userId ?? "me"] as const,
    portfolio: (userId?: string) => [...candidateKeys.profile.all(), "portfolio", userId ?? "me"] as const,
    employment: (userId?: string) => [...candidateKeys.profile.all(), "employment", userId ?? "me"] as const,
    visibilityHistory: (userId?: string) => [...candidateKeys.profile.all(), "visibilityHistory", userId ?? "me"] as const,
    evaluations: (userId?: string) => [...candidateKeys.profile.all(), "evaluations", userId ?? "me"] as const,
    internships: (userId?: string) => [...candidateKeys.profile.all(), "internships", userId ?? "me"] as const,
    // HS-04 CV
    cvs: {
      all: () => [...candidateKeys.profile.all(), "cvs"] as const,
      list: (userId?: string) => [...candidateKeys.profile.cvs.all(), "list", userId ?? "me"] as const,
      detail: (cvId: string) => [...candidateKeys.profile.cvs.all(), "detail", cvId] as const,
      public: (publicToken: string) => [...candidateKeys.profile.cvs.all(), "public", publicToken] as const,
    },
  },

  // Kỹ năng ứng viên (Skills) - Dùng chung cho Profile, LMS & AI Career
  skills: {
    all: () => [...candidateKeys.all, "skills"] as const,
    byUser: (userId?: string) => [...candidateKeys.skills.all(), userId ?? "me"] as const,
  },

  // Việc làm ứng viên (Jobs, Saved, Detail)
  jobs: {
    all: () => [...candidateKeys.all, "jobs"] as const,
    list: (params?: Record<string, unknown>) => [...candidateKeys.jobs.all(), "list", params ?? {}] as const,
    detail: (jobId?: string | number) => [...candidateKeys.jobs.all(), "detail", String(jobId ?? "")] as const,
    saved: () => [...candidateKeys.jobs.all(), "saved"] as const,
  },

  // Ứng tuyển & Tuyển dụng (Recruitments)
  recruitments: {
    all: () => [...candidateKeys.all, "recruitments"] as const,
    list: (status?: string) => [...candidateKeys.recruitments.all(), "list", status ?? "ALL"] as const,
    detail: (applicationId?: string | number) => [...candidateKeys.recruitments.all(), "detail", String(applicationId ?? "")] as const,
  },

  // Đề nghị tuyển dụng (Offers)
  offers: {
    all: () => [...candidateKeys.all, "offers"] as const,
    list: (userId?: string) => [...candidateKeys.offers.all(), "list", userId ?? "me"] as const,
  },

  // Lịch phỏng vấn (Interviews)
  interviews: {
    all: () => [...candidateKeys.all, "interviews"] as const,
    list: (userId?: string) => [...candidateKeys.interviews.all(), "list", userId ?? "me"] as const,
  },

  // Học tập & Đào tạo bổ sung LMS (Learning)
  learning: {
    all: () => [...candidateKeys.all, "learning"] as const,
    targetPositions: () => [...candidateKeys.learning.all(), "targetPositions"] as const,
    myTarget: () => [...candidateKeys.learning.all(), "myTarget"] as const,
    gapAnalysis: (jobId?: string | number) => [...candidateKeys.learning.all(), "gapAnalysis", jobId ? String(jobId) : "default"] as const,
    courseSuggestions: () => [...candidateKeys.learning.all(), "courseSuggestions"] as const,
    learningPath: () => [...candidateKeys.learning.all(), "learningPath"] as const,
    quickGap: (jobId?: string | number) => [...candidateKeys.learning.all(), "quickGap", String(jobId ?? "")] as const,
  },

  // AI Career
  career: {
    all: () => [...candidateKeys.all, "career"] as const,
    recommendations: (userId?: string, limit?: number) => [...candidateKeys.career.all(), "recommendations", userId ?? "me", limit ?? 10] as const,
  },

  // Tự đánh giá & So sánh năng lực (Self Assessment)
  selfAssessment: {
    all: () => [...candidateKeys.all, "selfAssessment"] as const,
    template: () => [...candidateKeys.selfAssessment.all(), "template"] as const,
    comparison: () => [...candidateKeys.selfAssessment.all(), "comparison"] as const,
  },
};

