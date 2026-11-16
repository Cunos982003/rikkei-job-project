package com.example.learningservice.model.dto.response;

import com.example.learningservice.model.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateTargetResponse {
    private UUID userId;
    private TargetType targetType;
    private UUID positionId;
    private String jobId;
    private String targetName;
    private Instant chosenAt;
}

