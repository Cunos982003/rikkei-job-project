package com.example.ai_career_service.controller;

import com.example.ai_career_service.dto.RecruitmentMatchScoreResponse;
import com.example.ai_career_service.service.CareerService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/recruitments")
public class RecruitmentMatchScoreController {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    private final CareerService careerService;
    private final String internalServiceToken;

    public RecruitmentMatchScoreController(
            CareerService careerService,
            @Value("${internal.service.token:}") String internalServiceToken) {
        this.careerService = careerService;
        this.internalServiceToken = internalServiceToken;
    }

    @GetMapping("/{userId}/jobs/{jobId}/match-score")
    public RecruitmentMatchScoreResponse getMatchScore(
            @PathVariable("userId") UUID userId,
            @PathVariable("jobId") UUID jobId,
            @RequestHeader(name = INTERNAL_TOKEN_HEADER, required = false) String requestToken) {
        if (internalServiceToken == null
                || internalServiceToken.isBlank()
                || requestToken == null
                || !MessageDigest.isEqual(
                        internalServiceToken.getBytes(StandardCharsets.UTF_8),
                        requestToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service token");
        }
        return new RecruitmentMatchScoreResponse(userId, jobId, careerService.getMatchScore(userId, jobId));
    }
}
