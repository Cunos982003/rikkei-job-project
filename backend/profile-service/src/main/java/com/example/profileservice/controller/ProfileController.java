package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.dto.PublishRequest;
import com.example.profileservice.dto.ReviewHistoryItemDto;
import com.example.profileservice.dto.SelfSkillDto;
import com.example.profileservice.dto.UpdateProfileRequest;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.service.ProfileCommandService;
import com.example.profileservice.service.SkillProfileService;
import com.example.profileservice.service.VisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileCommandService profileCommandService;
    private final SkillProfileService skillProfileService;
    private final VisibilityService visibilityService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CandidateProfile>> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        CandidateProfile profile = profileCommandService.getOrCreateProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CandidateProfile>> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody UpdateProfileRequest update) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        CandidateProfile updated = profileCommandService.updateProfile(userId, update);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updated));
    }

    @PutMapping("/me/self-skills")
    public ResponseEntity<ApiResponse<Void>> upsertSelfSkills(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody List<SelfSkillDto> skills) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        skillProfileService.upsertSelfSkills(userId, skills);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kỹ năng tự khai thành công", null));
    }

    @PostMapping("/me/publish")
    public ResponseEntity<ApiResponse<CandidateProfile>> publish(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody(required = false) PublishRequest request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        boolean consent = request != null && Boolean.TRUE.equals(request.getConsent());
        visibilityService.submitForReview(userId, consent);
        CandidateProfile profile = profileCommandService.getOrCreateProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Gửi hồ sơ duyệt thành công", profile));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<CandidateProfile>> withdraw(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        visibilityService.withdraw(userId);
        CandidateProfile profile = profileCommandService.getOrCreateProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Rút hồ sơ thành công", profile));
    }

    @GetMapping("/me/review-history")
    public ResponseEntity<ApiResponse<List<ReviewHistoryItemDto>>> reviewHistory(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        List<ReviewHistoryItemDto> history = visibilityService.getReviewHistory(userId).stream()
                .map(ReviewHistoryItemDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử duyệt thành công", history));
    }
}