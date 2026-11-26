package com.example.learningservice.model.dto.response.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpskillingSummaryResponse {
    private Long inProgressCount;
    private Long completedCount;
    @Builder.Default
    private List<TopMissingSkillDto> topMissingSkills = new ArrayList<>();
}
