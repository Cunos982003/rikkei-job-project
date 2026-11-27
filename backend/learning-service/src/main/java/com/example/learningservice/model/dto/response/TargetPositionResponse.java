package com.example.learningservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetPositionResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    @Builder.Default
    private List<PositionSkillItemResponse> skills = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionSkillItemResponse {
        private String skillId;
        private String skillName;
        private Integer minScore;
        private Boolean isMandatory;
    }
}

