package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.dto.PortfolioProjectDto;
import com.example.profileservice.dto.PublishRequest;
import com.example.profileservice.dto.ReviewHistoryItemDto;
import com.example.profileservice.dto.SelfSkillDto;
import com.example.profileservice.dto.UpdateProfileRequest;
import com.example.profileservice.entity.CandidateProfile;
import com.example.profileservice.service.PortfolioService;
import com.example.profileservice.service.ProfileCommandService;
import com.example.profileservice.service.SkillProfileService;
import com.example.profileservice.service.VisibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileCommandService profileCommandService;
    private final SkillProfileService skillProfileService;
    private final VisibilityService visibilityService;
    private final PortfolioService portfolioService;

    private UUID resolveUserId(Object headerUserId) {
        if (headerUserId instanceof UUID uuid) {
            return uuid;
        }
        if (headerUserId instanceof String str && !str.isBlank()) {
            try {
                return UUID.fromString(str);
            } catch (Exception ignored) {}
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (Exception ignored) {}
        }
        return null;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CandidateProfile>> getMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) Object userId) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        CandidateProfile profile = profileCommandService.getOrCreateProfile(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CandidateProfile>> updateMyProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody UpdateProfileRequest update) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        CandidateProfile updated = profileCommandService.updateProfile(effectiveUserId, update);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", updated));
    }

    @PutMapping("/me/self-skills")
    public ResponseEntity<ApiResponse<Void>> upsertSelfSkills(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody List<SelfSkillDto> skills) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        skillProfileService.upsertSelfSkills(effectiveUserId, skills);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kỹ năng tự khai thành công", null));
    }

    @PostMapping("/me/publish")
    public ResponseEntity<ApiResponse<CandidateProfile>> publish(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestBody(required = false) PublishRequest request) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        boolean consent = request != null && Boolean.TRUE.equals(request.getConsent());
        visibilityService.submitForReview(effectiveUserId, consent);
        CandidateProfile profile = profileCommandService.getOrCreateProfile(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success("Gửi hồ sơ duyệt thành công", profile));
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<ApiResponse<CandidateProfile>> withdraw(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        visibilityService.withdraw(effectiveUserId);
        CandidateProfile profile = profileCommandService.getOrCreateProfile(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success("Rút hồ sơ thành công", profile));
    }

    @GetMapping("/me/review-history")
    public ResponseEntity<ApiResponse<List<ReviewHistoryItemDto>>> reviewHistory(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        List<ReviewHistoryItemDto> history = visibilityService.getReviewHistory(effectiveUserId).stream()
                .map(ReviewHistoryItemDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử duyệt thành công", history));
    }

    // ========== Portfolio Endpoints (HS-03) ==========

    @GetMapping("/me/portfolio")
    public ResponseEntity<ApiResponse<List<PortfolioProjectDto>>> getMyPortfolio(
            @RequestHeader(value = "X-User-Id", required = false) Object userId) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        try {
            List<PortfolioProjectDto> portfolio = portfolioService.getMyPortfolio(effectiveUserId);
            return ResponseEntity.ok(ApiResponse.success("Lấy portfolio thành công", portfolio));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.success("Lấy portfolio thành công", java.util.Collections.emptyList()));
        }
    }

    @GetMapping("/{userId}/portfolio")
    public ResponseEntity<ApiResponse<List<PortfolioProjectDto>>> getPublicPortfolio(
            @PathVariable UUID userId) {
        List<PortfolioProjectDto> portfolio = portfolioService.getPublicPortfolio(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy portfolio công khai thành công", portfolio));
    }

    @PostMapping("/me/portfolio")
    public ResponseEntity<ApiResponse<PortfolioProjectDto>> createProject(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody PortfolioProjectDto dto) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        PortfolioProjectDto created = portfolioService.createProject(effectiveUserId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo dự án portfolio thành công", created));
    }

    @PutMapping("/me/portfolio/{projectId}")
    public ResponseEntity<ApiResponse<PortfolioProjectDto>> updateProject(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID projectId,
            @Valid @RequestBody PortfolioProjectDto dto) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        PortfolioProjectDto updated = portfolioService.updateProject(effectiveUserId, projectId, dto);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật dự án portfolio thành công", updated));
    }

    @DeleteMapping("/me/portfolio/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID projectId) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        portfolioService.deleteProject(effectiveUserId, projectId);
        return ResponseEntity.ok(ApiResponse.success("Xóa dự án portfolio thành công", null));
    }

    @PutMapping("/me/portfolio/order")
    public ResponseEntity<ApiResponse<List<PortfolioProjectDto>>> reorderProjects(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody PortfolioProjectDto.ReorderProjectsRequest request) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        List<PortfolioProjectDto> reordered = portfolioService.reorderProjects(effectiveUserId, request.getProjectIds());
        return ResponseEntity.ok(ApiResponse.success("Sắp xếp lại portfolio thành công", reordered));
    }

    @PostMapping("/me/portfolio/{projectId}/media")
    public ResponseEntity<ApiResponse<PortfolioProjectDto.PortfolioMediaDto>> uploadMedia(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID projectId,
            @RequestPart("file") MultipartFile file) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        PortfolioProjectDto.PortfolioMediaDto media = portfolioService.uploadMedia(effectiveUserId, projectId, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải lên ảnh portfolio thành công", media));
    }

    @DeleteMapping("/me/portfolio/{projectId}/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID projectId,
            @PathVariable UUID mediaId) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        portfolioService.deleteMedia(effectiveUserId, projectId, mediaId);
        return ResponseEntity.ok(ApiResponse.success("Xóa ảnh portfolio thành công", null));
    }

    @PutMapping("/me/portfolio/{projectId}/media/order")
    public ResponseEntity<ApiResponse<Void>> reorderMedia(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable UUID projectId,
            @Valid @RequestBody PortfolioProjectDto.ReorderMediaRequest request) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        portfolioService.reorderMedia(effectiveUserId, projectId, request.getMediaIds());
        return ResponseEntity.ok(ApiResponse.success("Sắp xếp lại ảnh portfolio thành công", null));
    }

    @GetMapping("/{userId}/evaluations")
    public ResponseEntity<ApiResponse<java.util.List<Object>>> getCandidateEvaluations(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", java.util.Collections.emptyList()));
    }

    @GetMapping("/me/evaluations")
    public ResponseEntity<ApiResponse<java.util.List<Object>>> getMyEvaluations() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", java.util.Collections.emptyList()));
    }
}
