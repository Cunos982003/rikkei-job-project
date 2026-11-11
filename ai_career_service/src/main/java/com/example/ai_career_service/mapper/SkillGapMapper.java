package com.example.ai_career_service.mapper;

import com.example.ai_career_service.dto.SkillGapResponse;
import com.example.ai_career_service.service.JobMatchScorer;
import com.example.ai_career_service.service.RecommendationSnapshot;
import org.springframework.stereotype.Component;

@Component
public class SkillGapMapper {

    public SkillGapResponse toResponse(JobMatchScorer.SkillGap skillGap) {
        return new SkillGapResponse(
                skillGap.name(),
                skillGap.verifiedIndex(),
                skillGap.requiredIndex(),
                skillGap.gap(),
                skillGap.type() == RecommendationSnapshot.RequirementType.REQUIRED);
    }
}
