package com.example.profileservice.profile.api;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.common.domain.Visibility;
import com.example.profileservice.profile.api.dto.PendingReviewDto;
import com.example.profileservice.profile.api.dto.StaffEditSelfDeclaredRequest;
import com.example.profileservice.profile.api.dto.StaffEditSelfDeclaredResponse;
import com.example.profileservice.profile.api.dto.StaffPreviewProfileDto;
import com.example.profileservice.profile.api.dto.StaffRejectRequest;
import com.example.profileservice.profile.service.InvalidTransitionException;
import com.example.profileservice.profile.service.VisibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Màn cán bộ tuyển dụng: duyệt hoặc trả lại hồ sơ ứng viên.
 *
 * <p>Vai trò lấy từ header {@code X-User-Role} do gateway truyền xuống; chỉ STAFF và ADMIN
 * được phép gọi. Thiếu header cũng bị từ chối để service không mở công khai khi bị gọi trực tiếp.
 */
@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffProfileController {

    private final VisibilityService visibilityService;

    @GetMapping("/profile-reviews")
    public ResponseEntity<ApiResponse<Page<PendingReviewDto>>> listProfileReviews(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "classCode", required = false) String classCode,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<Page<PendingReviewDto>>>) denied;
        }

        Visibility visibility;
        try {
            visibility = Visibility.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status));
        }

        Page<PendingReviewDto> result = visibilityService.listByStatus(
                visibility, q, classCode, PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : size));
        return ResponseEntity.ok(ApiResponse.success("Profile reviews retrieved successfully", result));
    }

    @GetMapping("/profiles/{userId}/preview")
    public ResponseEntity<ApiResponse<StaffPreviewProfileDto>> previewProfile(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID userId) {
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<StaffPreviewProfileDto>>) denied;
        }

        StaffPreviewProfileDto preview = visibilityService.getPreviewProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile preview retrieved successfully", preview));
    }

    @PutMapping("/profiles/{userId}/self-declared")
    public ResponseEntity<ApiResponse<StaffEditSelfDeclaredResponse>> editSelfDeclared(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID userId,
            @RequestBody StaffEditSelfDeclaredRequest request) {
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<StaffEditSelfDeclaredResponse>>) denied;
        }

        StaffEditSelfDeclaredResponse response = visibilityService.editSelfDeclared(userId, staffId, request);
        return ResponseEntity.ok(ApiResponse.success("Self-declared field updated successfully", response));
    }

    @PostMapping("/profiles/{userId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID userId) {
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<Map<String, Object>>>) denied;
        }

        visibilityService.approve(userId, staffId);
        Map<String, Object> data = Map.of(
                "userId", userId.toString(),
                "visibility", Visibility.PUBLIC.name());
        return ResponseEntity.ok(ApiResponse.success("Profile approved successfully", data));
    }

    @PostMapping("/profiles/{userId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID userId,
            @RequestBody(required = false) StaffRejectRequest request) {
        ResponseEntity<?> denied = requireStaff(staffId, role);
        if (denied != null) {
            return (ResponseEntity<ApiResponse<Map<String, Object>>>) denied;
        }

        visibilityService.reject(userId, staffId, request == null ? null : request.getReason());
        Map<String, Object> data = Map.of(
                "userId", userId.toString(),
                "visibility", Visibility.REJECTED.name());
        return ResponseEntity.ok(ApiResponse.success("Profile rejected successfully", data));
    }

    /**
     * @return null khi hợp lệ, ngược lại là response 401/403 tương ứng.
     */
    private ResponseEntity<?> requireStaff(UUID staffId, String role) {
        if (staffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Thiếu header X-User-Id"));
        }
        String normalized = role == null ? "" : role.trim().toUpperCase().replace("ROLE_", "");
        if (!"STAFF".equals(normalized) && !"ADMIN".equals(normalized)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(HttpStatus.FORBIDDEN, "Chức năng chỉ dành cho Cán bộ tuyển dụng (STAFF)"));
        }
        return null;
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTransition(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }
}
