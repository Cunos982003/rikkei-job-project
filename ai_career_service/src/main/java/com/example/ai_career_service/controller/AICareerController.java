package com.example.ai_career_service.controller;

import com.example.ai_career_service.dto.JobRecommendationResponse;
import com.example.ai_career_service.service.CareerService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/me/job-recommendations")
@RequiredArgsConstructor
@Validated
public class AICareerController {

    private final CareerService careerService;

    @GetMapping
    public List<JobRecommendationResponse> getRecommendations(
            Principal principal,
            @RequestParam(name = "limit", defaultValue = "10") @Min(1) @Max(50) int limit) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated candidate is required");
        }
        UUID userId;
        try {
            userId = UUID.fromString(principal.getName());
            if (!userId.toString().equalsIgnoreCase(principal.getName())) {
                throw new IllegalArgumentException("Invalid UUID format");
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated candidate ID must be a UUID", exception);
        }
        return careerService.getRecommendations(userId, limit);
    }
}
