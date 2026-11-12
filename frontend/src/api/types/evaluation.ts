export interface EvaluationCriterionScore {
  criterionId: string;
  skillName: string;
  description: string;
  weight: number;
  score: number;
  comment?: string | null;
}

export interface MentorEvaluation {
  id: string;
  templateName: string;
  mentorName: string;
  context: "INTERNSHIP" | "INTERVIEW" | string;
  batchId?: string | null;
  interviewRoundId?: string | null;
  overallComment?: string | null;
  submittedAt: string;
  scores: EvaluationCriterionScore[];
}

export type EvaluationContext = 'INTERNSHIP' | 'INTERVIEW'

export interface EvaluationScore {
  criterionId: string
  skillId: string
  description: string
  score: number
  comment: string | null
}

export interface Evaluation {
  id: string
  templateId: string
  templateName: string
  userId: string
  mentorId: string
  context: EvaluationContext
  batchId: string | null
  interviewRoundId: string | null
  status: 'DRAFT' | 'SUBMITTED'
  overallComment: string | null
  submittedAt: string | null
  scores: EvaluationScore[]
}

// Kiểu dữ liệu Task nghiệm thu
export interface InternshipTask {
  id: string;
  title: string;
  acceptedScore: number;
  acceptedAt: string | null;
}

// Kiểu dữ liệu Đợt thực tập
export interface Internship {
  id: string;
  batchName: string;
  groupName: string;
  topicName: string;
  mentorEmail: string;
  startDate: string | null;
  endDate: string | null;
  finalScore: number;
  tasks: InternshipTask[];
}

export interface EvaluationTask {
  id: string;
  candidateName: string;
  context: 'INTERNSHIP' | 'INTERVIEW';
  batchOrRoundName: string; // Tên đợt (nếu Thực tập) hoặc Vòng (nếu Phỏng vấn)
  deadline: string;
  status: 'PENDING' | 'DRAFT' | 'SUBMITTED';
}

export interface EvaluationFormData {
  id: string;
  candidateName: string;
  skillsSummary: string;
  internshipTasks?: { title: string; score: number }[]; // Chỉ có khi là Thực tập
  criteria: {
    id: string;
    name: string;
    description: string;
    weight: number;
  }[];
}