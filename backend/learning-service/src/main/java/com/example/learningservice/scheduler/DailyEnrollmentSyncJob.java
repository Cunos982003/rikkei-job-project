package com.example.learningservice.scheduler;

import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.EnrollmentStatus;
import com.example.learningservice.repository.EnrollmentRepository;
import com.example.learningservice.service.CourseSuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyEnrollmentSyncJob {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseSuggestionService courseSuggestionService;

    /**
     * Job tự động chạy hằng ngày vào lúc 02:00 sáng (hoặc định kỳ mỗi 6 tiếng)
     * để đồng bộ tiến độ học tập và điểm số các khóa học đang học (IN_PROGRESS, ENROLLED).
     */
    @Scheduled(cron = "${app.scheduler.daily-enrollment-sync:0 0 2 * * ?}")
    public void syncDailyEnrollments() {
        log.info("⏰ Bắt đầu tác vụ định kỳ hằng ngày: Đồng bộ tiến độ khóa học LMS...");

        try {
            // Lấy danh sách các học viên có khóa học chưa hoàn thành
            List<Enrollment> activeEnrollments = enrollmentRepository.findByStatusIn(
                    List.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.IN_PROGRESS)
            );

            if (activeEnrollments.isEmpty()) {
                log.info("Không có khóa học nào đang trong trạng thái cần đồng bộ tiến độ.");
                return;
            }

            Set<UUID> userIds = activeEnrollments.stream()
                    .map(Enrollment::getUserId)
                    .collect(Collectors.toSet());

            log.info("Tìm thấy {} ứng viên có khóa học đang diễn ra cần làm mới tiến độ", userIds.size());

            for (UUID userId : userIds) {
                try {
                    courseSuggestionService.refreshEnrollments(userId, null);
                } catch (Exception e) {
                    log.error("Lỗi khi đồng bộ tiến độ định kỳ cho userId {}: {}", userId, e.getMessage());
                }
            }

            log.info("Hoàn tất tác vụ đồng bộ tiến độ LMS hằng ngày thành công.");
        } catch (Exception e) {
            log.error("Lỗi tác vụ Scheduled đồng bộ tiến độ LMS: {}", e.getMessage(), e);
        }
    }
}

