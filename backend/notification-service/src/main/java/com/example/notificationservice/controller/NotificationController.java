package com.example.notificationservice.controller;

import com.example.notificationservice.common.ApiResponse;
import com.example.notificationservice.common.PageResponse;
import com.example.notificationservice.dto.CreateNotificationRequest;
import com.example.notificationservice.dto.NotificationDto;
import com.example.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private String resolveUserId(String headerUserId) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            return headerUserId.trim();
        }
        return "cand-001"; // Fallback default user for local testing if header is missing
    }

    @GetMapping({"", "/me"})
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> getMyNotifications(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String effectiveUserId = resolveUserId(userId);
        PageResponse<NotificationDto> result = notificationService.getMyNotifications(effectiveUserId, unreadOnly, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String effectiveUserId = resolveUserId(userId);
        Map<String, Object> countData = notificationService.getUnreadCount(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công", countData));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id) {

        String effectiveUserId = resolveUserId(userId);
        NotificationDto updated = notificationService.markAsRead(effectiveUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu thông báo đã đọc thành công", updated));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String effectiveUserId = resolveUserId(userId);
        Map<String, Object> result = notificationService.markAllAsRead(effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success("Đánh dấu tất cả thông báo đã đọc thành công", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id) {

        String effectiveUserId = resolveUserId(userId);
        notificationService.deleteNotification(effectiveUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thông báo thành công", null));
    }

    @PostMapping("/internal/send")
    public ResponseEntity<ApiResponse<NotificationDto>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {

        NotificationDto created = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo thông báo thành công", created));
    }
}
