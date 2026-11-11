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