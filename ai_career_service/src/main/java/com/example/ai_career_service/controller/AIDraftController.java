package com.example.ai_career_service.controller;

import com.example.ai_career_service.dto.AIDraftRequest;
import com.example.ai_career_service.dto.AIDraftResponse;
import com.example.ai_career_service.service.AIDraftService;
import com.example.ai_career_service.utils.AuthenticatedCandidate;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AIDraftController {

    private final AIDraftService service;

    @PostMapping("/cvs/{id}/ai-write")
    @ResponseStatus(HttpStatus.CREATED)
    public AIDraftResponse createDraft(
            Principal principal,
            @PathVariable("id") UUID cvId,
            @Valid @RequestBody AIDraftRequest request) {
        return service.createDraft(AuthenticatedCandidate.requireUserId(principal), cvId, request);
    }

    @PostMapping("/ai-drafts/{id}/accept")
    public AIDraftResponse accept(Principal principal, @PathVariable("id") UUID draftId) {
        return service.accept(AuthenticatedCandidate.requireUserId(principal), draftId);
    }

    @PostMapping("/ai-drafts/{id}/discard")
    public AIDraftResponse discard(Principal principal, @PathVariable("id") UUID draftId) {
        return service.discard(AuthenticatedCandidate.requireUserId(principal), draftId);
    }

    @GetMapping("/cvs/{id}/ai-drafts")
    public List<AIDraftResponse> getHistory(Principal principal, @PathVariable("id") UUID cvId) {
        return service.getHistory(AuthenticatedCandidate.requireUserId(principal), cvId);
    }
}
