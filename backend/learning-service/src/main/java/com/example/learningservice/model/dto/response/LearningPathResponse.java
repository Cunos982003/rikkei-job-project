package com.example.learningservice.model.dto.response;

import com.example.learningservice.model.entity.LearningPathStatus;
import com.example.learningservice.model.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathResponse {
    private UUID id;
    private UUID userId;
    private TargetType targetType;
    private String targetId;
    private String targetName;
    private Double readinessAtStart;
    private Double currentReadiness;
    private Double readinessGrowth;
    private Double completionPercentage;
    private Integer totalSteps;
    private Integer completedSteps;
    private LearningPathStatus status;
    private Instant createdAt;
    @Builder.Default
    private List<LearningPathStepResponse> steps = new ArrayList<>();
    @Builder.Default
    private List<GapItemResponse> radarSkills = new ArrayList<>();
}

