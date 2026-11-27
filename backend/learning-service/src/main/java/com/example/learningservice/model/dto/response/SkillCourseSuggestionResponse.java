package com.example.learningservice.model.dto.response;

import com.example.learningservice.model.entity.GapPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillCourseSuggestionResponse {
    private String skillId;
    private String skillName;
    private Integer requiredScore;
    private Integer currentScore;
    private Integer gap;
    private GapPriority priority;
    private List<SuggestedCourseItemResponse> courses;
}

