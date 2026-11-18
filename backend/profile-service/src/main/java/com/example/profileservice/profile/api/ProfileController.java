package com.example.profileservice.profile.api;

import com.example.profileservice.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<CandidateProfile>> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        CandidateProfile profile = profileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CandidateProfile>> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody UpdateProfileRequest update) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        CandidateProfile updated = profileService.updateProfile(userId, update);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    @PutMapping("/me/self-skills")
    public ResponseEntity<ApiResponse<Void>> upsertSelfSkills(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody List<SelfSkillDto> skills) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        profileService.upsertSelfSkills(userId, skills);
        return ResponseEntity.ok(ApiResponse.success("Self skills updated successfully", null));
    }

    @PostMapping("/me/publish")
    public ResponseEntity<ApiResponse<CandidateProfile>> publish(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody(required = false) PublishRequest request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        boolean consent = request != null && Boolean.TRUE.equals(request.getConsent());
        visibilityService.submitForReview(userId, consent);
        CandidateProfile profile = profileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile submitted for review", profile));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<CandidateProfile>> withdraw(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        visibilityService.withdraw(userId);
        CandidateProfile profile = profileService.getOrCreateProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile withdrawn successfully", profile));
    }

    @GetMapping("/me/review-history")
    public ResponseEntity<ApiResponse<List<ReviewHistoryItemDto>>> reviewHistory(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        }
        List<ReviewHistoryItemDto> history = visibilityService.getReviewHistory(userId).stream()
                .map(ReviewHistoryItemDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Review history retrieved successfully", history));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(LmsClient.LmsClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleLmsClientException(LmsClient.LmsClientException ex) {
        HttpStatus status = ex.getStatusCode() != null ? ex.getStatusCode() : HttpStatus.SERVICE_UNAVAILABLE;
        // For 404 (student not in LMS), return 404 to indicate resource not found
        // For other errors (5xx, other 4xx), return 503 Service Unavailable
        HttpStatus responseStatus = (status == HttpStatus.NOT_FOUND) ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(responseStatus)
                .body(ApiResponse.error(responseStatus, ex.getMessage()));
    }
}