package com.example.notificationservice;

import com.example.notificationservice.common.PageResponse;
import com.example.notificationservice.dto.CreateNotificationRequest;
import com.example.notificationservice.dto.NotificationDto;
import com.example.notificationservice.enums.NotificationType;
import com.example.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    private static final String TEST_USER_ID = "test-cand-001";

    @BeforeEach
    void setUp() {
        // Tạo một thông báo mẫu cho mỗi test
        CreateNotificationRequest req = CreateNotificationRequest.builder()
                .userId(TEST_USER_ID)
                .recipientRole("CANDIDATE")
                .type(NotificationType.OFFER_RECEIVED)
                .title("Thư mời nhận việc kiểm thử")
                .body("Chúc mừng bạn đã nhận được offer làm việc.")
                .actionUrl("/candidate/offers")
                .build();
        notificationService.createNotification(req);
    }

    @Test
    @DisplayName("Lấy danh sách thông báo của người dùng phân trang thành công")
    void testGetMyNotifications() {
        PageResponse<NotificationDto> page = notificationService.getMyNotifications(TEST_USER_ID, false, 0, 10);
        assertNotNull(page);
        assertFalse(page.items().isEmpty());
        assertTrue(page.pagination().totalElements() >= 1);
    }

    @Test
    @DisplayName("Lấy số lượng thông báo chưa đọc chính xác")
    void testGetUnreadCount() {
        Map<String, Object> countData = notificationService.getUnreadCount(TEST_USER_ID);
        assertNotNull(countData);
        long unreadCount = (long) countData.get("unreadCount");
        assertTrue(unreadCount >= 1);
    }

    @Test
    @DisplayName("Đánh dấu 1 thông báo là đã đọc thành công")
    void testMarkAsRead() {
        PageResponse<NotificationDto> page = notificationService.getMyNotifications(TEST_USER_ID, true, 0, 1);
        assertFalse(page.items().isEmpty());
        NotificationDto item = page.items().get(0);

        NotificationDto readItem = notificationService.markAsRead(TEST_USER_ID, item.id());
        assertTrue(readItem.isRead());
        assertNotNull(readItem.readAt());
    }

    @Test
    @DisplayName("Đánh dấu tất cả thông báo của người dùng là đã đọc")
    void testMarkAllAsRead() {
        Map<String, Object> result = notificationService.markAllAsRead(TEST_USER_ID);
        assertNotNull(result);

        Map<String, Object> unreadData = notificationService.getUnreadCount(TEST_USER_ID);
        assertEquals(0L, unreadData.get("unreadCount"));
    }

    @Test
    @DisplayName("Xóa thông báo thành công")
    void testDeleteNotification() {
        PageResponse<NotificationDto> page = notificationService.getMyNotifications(TEST_USER_ID, false, 0, 10);
        assertFalse(page.items().isEmpty());
        NotificationDto item = page.items().get(0);

        assertDoesNotThrow(() -> notificationService.deleteNotification(TEST_USER_ID, item.id()));
    }
}
