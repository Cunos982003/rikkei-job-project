
package com.example.profileservice.badge;

import com.example.profileservice.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/badges")
@RequiredArgsConstructor
public class AdminBadgeController {

    private final AdminBadgeService badgeService;

    public record GrantBadgeRequest(
            @NotNull UUID userId,
            @NotNull UUID badgeRuleId,
            @NotBlank String reason
    ) {}

    public record RevokeBadgeRequest(
            @NotBlank String reason
    ) {}

    @GetMapping("/candidates")
    public ResponseEntity<ApiResponse<List<AdminBadgeService.CandidateItem>>>
    searchCandidates(
            @RequestHeader(value = "X-User-Id", required = false) UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam(required = false) String q
    ) {
        requireAdmin(adminId, role);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Candidates retrieved successfully",
                        badgeService.searchCandidates(q)
                )
        );
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<AdminBadgeService.BadgeRuleItem>>>
    getBadgeRules(
            @RequestHeader(value = "X-User-Id", required = false) UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        requireAdmin(adminId, role);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Badge rules retrieved successfully",
                        badgeService.getBadgeRules()
                )
        );
    }

    @GetMapping("/candidates/{userId}/history")
    public ResponseEntity<ApiResponse<List<AdminBadgeService.BadgeHistoryItem>>>
    getHistory(
            @RequestHeader(value = "X-User-Id", required = false) UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID userId
    ) {
        requireAdmin(adminId, role);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Badge history retrieved successfully",
                        badgeService.getCandidateBadgeHistory(userId)
                )
        );
    }

    @PostMapping("/grant")
    public ResponseEntity<ApiResponse<AdminBadgeService.BadgeActionResult>>
    grantBadge(
            @RequestHeader(value = "X-User-Id", required = false) UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @Valid @RequestBody GrantBadgeRequest request
    ) {
        requireAdmin(adminId, role);

        AdminBadgeService.BadgeActionResult result =
                badgeService.grantBadge(
                        request.userId(),
                        request.badgeRuleId(),
                        request.reason(),
                        adminId
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Badge granted successfully", result)
        );
    }

    @PostMapping("/{profileBadgeId}/revoke")
    public ResponseEntity<ApiResponse<AdminBadgeService.BadgeActionResult>>
    revokeBadge(
            @RequestHeader(value = "X-User-Id", required = false) UUID adminId,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable UUID profileBadgeId,
            @Valid @RequestBody RevokeBadgeRequest request
    ) {
        requireAdmin(adminId, role);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Badge revoked successfully",
                        badgeService.revokeBadge(
                                profileBadgeId,
                                request.reason(),
                                adminId
                        )
                )
        );
    }

    private void requireAdmin(UUID adminId, String role) {
        if (adminId == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Thiếu header X-User-Id"
            );
        }

        String normalized = role == null
                ? ""
                : role.trim().toUpperCase().replace("ROLE_", "");

        if (!"ADMIN".equals(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chỉ ADMIN mới được quản lý huy hiệu"
            );
        }
    }
}
