export interface SkillEvidence {
  source: string;
  sourceRef: string;
  title: string;
  score: number;
  recordedAt: string;
  verified: boolean;
}

export interface SkillProfileResponse {
  skillId: string;
  skillCode: string;
  skillName: string;
  group: string;
  verifiedScore: number | null;
  selfScore: number | null;
  level: string;
  evidences: SkillEvidence[];
}