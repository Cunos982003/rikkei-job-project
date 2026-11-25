package com.example.profileservice.controller;

import com.example.profileservice.common.ApiResponse;
import com.example.profileservice.dto.ContactRequestDto;
import com.example.profileservice.dto.ContactAccessLogDto;
import com.example.profileservice.entity.ContactRequest;
import com.example.profileservice.service.ContactAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContactController {

    private final ContactAccessService contactAccessService;

    private UUID resolveUserId(UUID headerUserId) {
        if (headerUserId != null) return headerUserId;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try { return UUID.fromString(auth.getName()); } catch (Exception ignored) {}
        }
        return null;
    }

    // ========== HR Endpoints ==========

    @PostMapping("/candidates/{userId}/contact-requests")
    public ResponseEntity<ApiResponse<ContactRequestDto>> createContactRequest(
            @RequestHeader(value = "X-User-Id", required = false) UUID hrId,
            @PathVariable UUID userId,
            @Valid @RequestBody ContactRequestDto.CreateRequest request) {
        UUID effectiveHrId = resolveUserId(hrId);
        if (effectiveHrId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        UUID companyId = getCompanyIdForHr(effectiveHrId);

        ContactRequestDto dto = ContactRequestDto.from(
                contactAccessService.requestContactAccess(companyId, effectiveHrId, userId, request.getJobId(), request.getReason())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi yêu cầu mở liên hệ thành công", dto));
    }

    @GetMapping("/candidates/{userId}/contact")
    public ResponseEntity<ApiResponse<ContactAccessService.ContactResponse>> getContact(
            @RequestHeader(value = "X-User-Id", required = false) UUID hrId,
            @PathVariable UUID userId) {
        UUID effectiveHrId = resolveUserId(hrId);
        if (effectiveHrId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        UUID companyId = getCompanyIdForHr(effectiveHrId);

        try {
            ContactAccessService.ContactResponse contact = contactAccessService.getContact(companyId, effectiveHrId, userId);
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin liên hệ thành công", contact));
        } catch (ContactAccessService.ContactAccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(HttpStatus.FORBIDDEN, "CONTACT_LOCKED"));
        }
    }

    @GetMapping("/me/contact-access-logs")
    public ResponseEntity<ApiResponse<List<ContactAccessLogDto>>> getMyAccessLogs(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID effectiveUserId = resolveUserId(userId);
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        List<ContactAccessLogDto> logs = contactAccessService.getMyAccessLogs(effectiveUserId).stream()
                .map(ContactAccessLogDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy nhật ký xem liên hệ thành công", logs));
    }

    // ========== STAFF Endpoints ==========

    @GetMapping("/staff/contact-requests")
    public ResponseEntity<ApiResponse<List<ContactRequestDto>>> getContactRequests(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestParam(required = false) ContactRequest.ContactRequestStatus status) {
        UUID effectiveStaffId = resolveUserId(staffId);
        if (effectiveStaffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        List<ContactRequestDto> requests = (status != null
                ? contactAccessService.getRequestsByStatus(status)
                : contactAccessService.getAllRequests()).stream()
                .map(ContactRequestDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu thành công", requests));
    }

    @PostMapping("/staff/contact-requests/{id}/approve")
    public ResponseEntity<ApiResponse<ContactRequestDto>> approveRequest(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @PathVariable UUID id) {
        UUID effectiveStaffId = resolveUserId(staffId);
        if (effectiveStaffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        ContactRequestDto dto = ContactRequestDto.from(
                contactAccessService.approveRequest(id, effectiveStaffId)
        );
        return ResponseEntity.ok(ApiResponse.success("Duyệt yêu cầu thành công", dto));
    }

    @PostMapping("/staff/contact-requests/{id}/reject")
    public ResponseEntity<ApiResponse<ContactRequestDto>> rejectRequest(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @PathVariable UUID id,
            @Valid @RequestBody ContactRequestDto.RejectRequest request) {
        UUID effectiveStaffId = resolveUserId(staffId);
        if (effectiveStaffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        ContactRequestDto dto = ContactRequestDto.from(
                contactAccessService.rejectRequest(id, effectiveStaffId, request.getReason())
        );
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu thành công", dto));
    }

    @GetMapping("/staff/contact-access-logs")
    public ResponseEntity<ApiResponse<List<ContactAccessLogDto>>> getStaffAccessLogs(
            @RequestHeader(value = "X-User-Id", required = false) UUID staffId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        UUID effectiveStaffId = resolveUserId(staffId);
        if (effectiveStaffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        }
        List<ContactAccessLogDto> logs = contactAccessService.getAccessLogs(companyId, userId, from, to).stream()
                .map(ContactAccessLogDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Lấy nhật ký xem liên hệ thành công", logs));
    }

    // Helper - TODO: implement properly via company-service or JWT claims
    private UUID getCompanyIdForHr(UUID hrId) {
        // For now, return a placeholder - should be resolved from HR's company association
        // This needs integration with company-service or auth-service
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}