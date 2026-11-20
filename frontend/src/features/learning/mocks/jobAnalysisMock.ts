import type {
  CandidateTarget,
  GapAnalysisResult,
  GapItem,
  TargetInfo,
} from "../../../api/endpoints/learning.api";

// Dữ liệu mẫu chuẩn hóa kỹ năng theo từng mã Job mẫu
const SAMPLE_JOB_SKILLS_MAP: Record<
  string,
  {
    title: string;
    company: string;
    skills: Array<{
      skillId: string;
      skillName: string;
      required: number;
      current: number;
      isMandatory: boolean;
    }>;
  }
> = {
  "job-001": {
    title: "Java Backend Developer (Fresher / Junior)",
    company: "Công ty Cổ phần Rikkeisoft",
    skills: [
      { skillId: "JAVA_CORE", skillName: "Java Core", required: 75, current: 40, isMandatory: true },
      { skillId: "SPRING_BOOT", skillName: "Spring Boot", required: 70, current: 0, isMandatory: true },
      { skillId: "SQL", skillName: "SQL & Cơ sở dữ liệu", required: 70, current: 50, isMandatory: true },
      { skillId: "REST_API", skillName: "Thiết kế REST API", required: 70, current: 65, isMandatory: true },
      { skillId: "GIT", skillName: "Git & Source Control", required: 60, current: 60, isMandatory: false },
    ],
  },
  "job-002": {
    title: "Frontend React Developer (Fresher)",
    company: "Rikkei Digital Solutions",
    skills: [
      { skillId: "REACT", skillName: "ReactJS & Next.js", required: 75, current: 50, isMandatory: true },
      { skillId: "TYPESCRIPT", skillName: "TypeScript", required: 70, current: 0, isMandatory: true },
      { skillId: "HTML_CSS", skillName: "HTML5 / CSS3 / Tailwind", required: 80, current: 80, isMandatory: true },
      { skillId: "REST_API", skillName: "Tích hợp REST API", required: 70, current: 60, isMandatory: true },
      { skillId: "GIT", skillName: "Git Workflow", required: 60, current: 60, isMandatory: false },
    ],
  },
  "job-003": {
    title: "QA / Automation Tester",
    company: "Rikkei QA Center",
    skills: [
      { skillId: "TEST_DESIGN", skillName: "Thiết kế Test Case", required: 80, current: 60, isMandatory: true },
      { skillId: "AUTOMATION_SELENIUM", skillName: "Automation Test (Selenium)", required: 70, current: 0, isMandatory: true },
      { skillId: "API_TESTING", skillName: "Kiểm thử API với Postman", required: 75, current: 45, isMandatory: true },
      { skillId: "SQL", skillName: "Truy vấn CSDL SQL", required: 70, current: 50, isMandatory: true },
      { skillId: "GIT", skillName: "Git cơ bản", required: 60, current: 60, isMandatory: false },
    ],
  },
};

/**
 * Sinh dữ liệu mẫu mục tiêu nghề nghiệp theo Job hoặc Position (Transparent Mock Fallback)
 */
export function getSampleJobTarget(
  jobId?: string,
  positionId?: string,
  customTitle?: string
): CandidateTarget {
  const now = new Date().toISOString();
  if (jobId) {
    const sample = SAMPLE_JOB_SKILLS_MAP[jobId];
    const targetName =
      customTitle || (sample ? sample.title : `Vị trí tuyển dụng (${jobId.length > 10 ? jobId.slice(0, 8) + "..." : jobId})`);
    return {
      userId: "candidate-current",
      targetType: "JOB",
      positionId: null,
      jobId,
      targetName,
      chosenAt: now,
    };
  }

  return {
    userId: "candidate-current",
    targetType: "POSITION",
    positionId: positionId || "POS_JAVA",
    jobId: null,
    targetName: customTitle || "Lập trình viên Backend Java",
    chosenAt: now,
  };
}

/**
 * Sinh dữ liệu phân tích khoảng thiếu hụt năng lực cho một Job ID cụ thể (Transparent Mock Fallback)
 */
export function generateMockJobGapAnalysis(
  jobId: string,
  customTitle?: string
): GapAnalysisResult {
  const sample = SAMPLE_JOB_SKILLS_MAP[jobId] || {
    title: customTitle || `Vị trí tuyển dụng (${jobId.length > 10 ? jobId.slice(0, 8) + "..." : jobId})`,
    company: "Doanh nghiệp đối tác Rikkei",
    skills: [
      { skillId: "CORE_SKILL_1", skillName: "Chuyên môn cốt lõi", required: 75, current: 40, isMandatory: true },
      { skillId: "FRAMEWORK_SKILL", skillName: "Framework chuyên ngành", required: 70, current: 0, isMandatory: true },
      { skillId: "DATABASE_SKILL", skillName: "Cơ sở dữ liệu & Lưu trữ", required: 70, current: 50, isMandatory: true },
      { skillId: "TOOL_SKILL", skillName: "Công cụ & Quy trình dự án", required: 65, current: 65, isMandatory: false },
    ],
  };

  const items: GapItem[] = sample.skills.map((s) => {
    const gap = Math.max(0, s.required - s.current);
    let priority: "HIGH" | "MEDIUM" | "LOW" = "LOW";
    if (s.isMandatory && gap > 30) {
      priority = "HIGH";
    } else if (gap > 15 || s.isMandatory) {
      priority = "MEDIUM";
    }

    return {
      skillId: s.skillId,
      skill: s.skillName,
      skillName: s.skillName,
      required: s.required,
      current: s.current,
      gap,
      priority,
      isMandatory: s.isMandatory,
    };
  });

  const totalRequiredSkills = items.length;
  const passedSkillsCount = items.filter((i) => i.gap === 0).length;
  const missingSkillsCount = totalRequiredSkills - passedSkillsCount;
  const readiness =
    totalRequiredSkills > 0
      ? Math.round((passedSkillsCount / totalRequiredSkills) * 100)
      : 0;

  const target: TargetInfo = {
    type: "JOB",
    id: jobId,
    code: jobId,
    name: sample.title,
    description: `Doanh nghiệp: ${sample.company} · Đang đối chiếu năng lực theo yêu cầu tin tuyển dụng`,
  };

  return {
    target,
    readiness,
    totalRequiredSkills,
    passedSkillsCount,
    missingSkillsCount,
    computedAt: new Date().toISOString(),
    items: items.filter((i) => i.gap > 0).sort((a, b) => b.gap - a.gap),
  };
}

