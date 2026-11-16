package com.example.profileservice.profile.api;

import com.example.profileservice.profile.api.dto.PendingReviewDto;
import com.example.profileservice.profile.api.dto.StaffRejectRequest;
import com.example.profileservice.profile.domain.CandidateProfile.Visibility;
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
    public ResponseEntity<?> listProfileReviews(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(value = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        ResponseEntity<Map<String, Object>> denied = requireStaff(staffId, role);
        if (denied != null) {
            return denied;
        }

        Visibility visibility;
        try {
            visibility = Visibility.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "statusCode", HttpStatus.BAD_REQUEST.value(),
                    "message", "Trạng thái không hợp lệ: " + status));
        }

        Page<PendingReviewDto> result = visibilityService.listByStatus(
                visibility, PageRequest.of(Math.max(page, 0), size <= 0 ? 20 : size));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/profiles/{userId}/approve")
    public ResponseEntity<?> approve(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID userId) {
        ResponseEntity<Map<String, Object>> denied = requireStaff(staffId, role);
        if (denied != null) {
            return denied;
        }

        visibilityService.approve(userId, staffId);
        return ResponseEntity.ok(Map.of(
                "userId", userId.toString(),
                "visibility", Visibility.PUBLIC.name()));
    }

    @PostMapping("/profiles/{userId}/reject")
    public ResponseEntity<?> reject(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID userId,
            @RequestBody(required = false) StaffRejectRequest request) {
        ResponseEntity<Map<String, Object>> denied = requireStaff(staffId, role);
        if (denied != null) {
            return denied;
        }

        visibilityService.reject(userId, staffId, request == null ? null : request.getReason());
        return ResponseEntity.ok(Map.of(
                "userId", userId.toString(),
                "visibility", Visibility.REJECTED.name()));
    }

    /**
     * @return null khi hợp lệ, ngược lại là response 401/403 tương ứng.
     */
    private ResponseEntity<Map<String, Object>> requireStaff(UUID staffId, String role) {
        if (staffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "statusCode", HttpStatus.UNAUTHORIZED.value(),
                    "message", "Thiếu header X-User-Id"));
        }
        String normalized = role == null ? "" : role.trim().toUpperCase().replace("ROLE_", "");
        if (!"STAFF".equals(normalized) && !"ADMIN".equals(normalized)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "statusCode", HttpStatus.FORBIDDEN.value(),
                    "message", "Chức năng chỉ dành cho Cán bộ tuyển dụng (STAFF)"));
        }
        return null;
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
}
