package com.example.ai_career_service.mapper;

import com.example.ai_career_service.dto.JobRecommendationResponse;
import com.example.ai_career_service.model.JobRecommendation;
import com.example.ai_career_service.service.JobMatchScorer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobRecommendationMapper {

    private final SkillGapMapper skillGapMapper;

    public JobRecommendationResponse toResponse(
            JobRecommendation recommendation,
            JobMatchScorer.MatchResult match) {
        return new JobRecommendationResponse(
                recommendation.getJobId(),
                recommendation.getScore(),
                recommendation.getExplanation(),
                recommendation.getCreatedAt(),
                match.missingRequiredSkills(),
                match.skillGaps().stream()
                        .map(skillGapMapper::toResponse)
                        .toList());
    }
}
