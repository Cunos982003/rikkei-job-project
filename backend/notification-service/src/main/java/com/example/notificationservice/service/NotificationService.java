package com.example.notificationservice.service;

import com.example.notificationservice.common.PageResponse;
import com.example.notificationservice.dto.CreateNotificationRequest;
import com.example.notificationservice.dto.NotificationDto;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> getMyNotifications(String userId, boolean unreadOnly, int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(validPage, validSize);

        Page<Notification> notificationPage;
        if (unreadOnly) {
            notificationPage = notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable);
        } else {
            notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return PageResponse.from(notificationPage, NotificationDto::from);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCount(String userId) {
        long unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        return Map.of("unreadCount", unreadCount);
    }

    @Transactional
    public NotificationDto markAsRead(String userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo hoặc bạn không có quyền truy cập"));

        notification.markAsRead();
        notification = notificationRepository.save(notification);
        return NotificationDto.from(notification);
    }

    @Transactional
    public Map<String, Object> markAllAsRead(String userId) {
        int updated = notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
        log.info("Đã đánh dấu đã đọc {} thông báo cho user {}", updated, userId);
        return Map.of("updatedCount", updated);
    }

    @Transactional
    public void deleteNotification(String userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo hoặc bạn không có quyền xóa"));

        notificationRepository.delete(notification);
    }

    @Transactional
    public NotificationDto createNotification(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.userId())
                .recipientRole(request.recipientRole())
                .type(request.type())
                .title(request.title())
                .body(request.body())
                .actionUrl(request.actionUrl())
                .metadata(request.metadata())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Đã tạo thông báo mới [type={}, user={}]", request.type(), request.userId());
        return NotificationDto.from(notification);
    }
}
