package com.example.learningservice.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDetailResponse {
    private String id;
    private String title;
    private String companyId;
    private String companyName;
    private String status;
    @Builder.Default
    private List<JobSkillResponse> jobSkills = new ArrayList<>();
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();
}

