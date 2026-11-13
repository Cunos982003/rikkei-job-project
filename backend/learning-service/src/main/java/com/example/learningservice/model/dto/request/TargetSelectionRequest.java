package com.example.learningservice.model.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetSelectionRequest {
    private UUID positionId;
    private String jobId;

    @AssertTrue(message = "Phải chọn đúng một trong hai: positionId hoặc jobId")
    public boolean isValid() {
        boolean hasPosition = positionId != null;
        boolean hasJob = jobId != null && !jobId.isBlank();
        return (hasPosition && !hasJob) || (!hasPosition && hasJob);
    }
}

