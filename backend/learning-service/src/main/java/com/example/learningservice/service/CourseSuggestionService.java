package com.example.learningservice.service;

import com.example.learningservice.model.dto.request.EnrollmentRequest;
import com.example.learningservice.model.dto.response.EnrollmentResponse;
import com.example.learningservice.model.dto.response.SkillCourseSuggestionResponse;

import java.util.List;
import java.util.UUID;

public interface CourseSuggestionService {

    /**
     * Gợi ý khóa học bổ sung trên LMS theo từng kỹ năng thiếu hụt (DT-01).
     */
    List<SkillCourseSuggestionResponse> getSuggestionsForCandidate(UUID userId, String authHeader);

    /**
     * Ghi danh khóa học bổ sung trên LMS.
     * Kiểm tra trùng lặp (409) và rollback nếu LMS mock lỗi.
     */
    EnrollmentResponse enrollCourse(UUID userId, EnrollmentRequest request);

    /**
     * Lấy danh sách các khóa học ứng viên đã ghi danh.
     */
    List<EnrollmentResponse> getMyEnrollments(UUID userId);

    /**
     * Đồng bộ làm mới tiến độ học tập và điểm số từ LMS (AC3).
     * Khi khóa học chuyển COMPLETED, tự động tạo bằng chứng LMS và recomputeSkills.
     */
    com.example.learningservice.model.dto.response.RefreshEnrollmentsResponse refreshEnrollments(UUID userId, String authHeader);
}

