package com.example.profileservice.profile.api;

import com.example.profileservice.profile.api.dto.PublishRequest;
import com.example.profileservice.profile.api.dto.ReviewHistoryItemDto;
import com.example.profileservice.profile.api.dto.SelfSkillDto;
import com.example.profileservice.profile.api.dto.UpdateProfileRequest;
import com.example.profileservice.profile.domain.CandidateProfile;
import com.example.profileservice.profile.infrastructure.LmsClient;
import com.example.profileservice.profile.service.InvalidTransitionException;
import com.example.profileservice.profile.service.ProfileService;
import com.example.profileservice.profile.service.VisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final VisibilityService visibilityService;

    @GetMapping("/me")
    public ResponseEntity<CandidateProfile> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        CandidateProfile profile = profileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<CandidateProfile> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody UpdateProfileRequest update) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        CandidateProfile updated = profileService.updateProfile(userId, update);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/me/self-skills")
    public ResponseEntity<Void> upsertSelfSkills(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody List<SelfSkillDto> skills) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        profileService.upsertSelfSkills(userId, skills);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/me/publish")
    public ResponseEntity<CandidateProfile> publish(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody(required = false) PublishRequest request) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        boolean consent = request != null && Boolean.TRUE.equals(request.getConsent());
        visibilityService.submitForReview(userId, consent);
        return ResponseEntity.ok(profileService.getOrCreateProfile(userId));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<CandidateProfile> withdraw(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        visibilityService.withdraw(userId);
        return ResponseEntity.ok(profileService.getOrCreateProfile(userId));
    }

    @GetMapping("/me/review-history")
    public ResponseEntity<List<ReviewHistoryItemDto>> reviewHistory(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<ReviewHistoryItemDto> history = visibilityService.getReviewHistory(userId).stream()
                .map(ReviewHistoryItemDto::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "statusCode", HttpStatus.CONFLICT.value(),
                "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "statusCode", HttpStatus.BAD_REQUEST.value(),
                "message", ex.getMessage()));
    }

    @ExceptionHandler(LmsClient.LmsClientException.class)
    public ResponseEntity<Map<String, Object>> handleLmsClientException(LmsClient.LmsClientException ex) {
        HttpStatus status = ex.getStatusCode() != null ? ex.getStatusCode() : HttpStatus.SERVICE_UNAVAILABLE;
        // For 404 (student not in LMS), return 404 to indicate resource not found
        // For other errors (5xx, other 4xx), return 503 Service Unavailable
        HttpStatus responseStatus = (status == HttpStatus.NOT_FOUND) ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(responseStatus).body(Map.of(
                "statusCode", responseStatus.value(),
                "message", ex.getMessage()));
    }
}