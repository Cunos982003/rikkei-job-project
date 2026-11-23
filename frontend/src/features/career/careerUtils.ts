import { useOutletContext } from 'react-router-dom';
import type { JobRecommendation, SkillGap } from '../../api/endpoints/career.api';
import type { CareerOutletContext } from './CareerLayout';

export const formatScore = (score: number) => `${Math.round(score)}%`;

export function useCareerData() {
  return useOutletContext<CareerOutletContext>();
}

export function getSkillGaps(recommendations: JobRecommendation[]) {
  return recommendations.flatMap((recommendation) =>
    recommendation.skillGaps.map((gap: SkillGap) => ({ ...gap, jobId: recommendation.jobId }))
  );
}
