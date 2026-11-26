package com.example.recruitment_service.controller;

import com.example.recruitment_service.dto.CreateEvaluationLinkRequest;
import com.example.recruitment_service.dto.EvaluationFormResponse;
import com.example.recruitment_service.dto.EvaluationLinkResponse;
import com.example.recruitment_service.exception.UnauthorizedException;
import com.example.recruitment_service.service.EvaluationLinkService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class EvaluationLinkController {

    private final EvaluationLinkService linkService;

    @Value("${app.pm.service-key}")
    private String expectedServiceKey;

    public EvaluationLinkController(EvaluationLinkService linkService) {
        this.linkService = linkService;
    }

    /**
     * POST /integrations/pm/evaluation-links
     */
    @PostMapping("/integrations/pm/evaluation-links")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationLinkResponse createEvaluationLink(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @Valid @RequestBody CreateEvaluationLinkRequest request) {

        if (serviceKey == null || !serviceKey.equals(expectedServiceKey)) {
            throw new UnauthorizedException("X-Service-Key không hợp lệ");
        }

        String url = linkService.createLink(
                request.mentorEmail(),
                request.lmsUserId(),
                request.batchId()
        );
        return new EvaluationLinkResponse(url);
    }

    /**
     * GET /evaluate/{token}
     */
    @GetMapping("/evaluate/{token}")
    public EvaluationFormResponse resolveEvaluationLink(@PathVariable String token) {
        return linkService.resolveAndBuildForm(token);
    }
}