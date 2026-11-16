package com.example.learningservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisResponse {
    private TargetInfoResponse target;
    private Double readiness;
    private Integer totalRequiredSkills;
    private Integer passedSkillsCount;
    private Integer missingSkillsCount;
    private Instant computedAt;
    @Builder.Default
    private List<GapItemResponse> items = new ArrayList<>();
}

