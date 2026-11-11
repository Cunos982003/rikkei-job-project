package com.example.ai_career_service.controller;

import com.example.ai_career_service.dto.JobRecommendationResponse;
import com.example.ai_career_service.service.CareerService;
import com.example.ai_career_service.utils.AuthenticatedCandidate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me/job-recommendations")
@RequiredArgsConstructor
@Validated
public class AICareerController {

    private final CareerService careerService;

    @GetMapping
    public List<JobRecommendationResponse> getRecommendations(
            Principal principal,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return careerService.getRecommendations(AuthenticatedCandidate.requireUserId(principal), limit);
    }
}
