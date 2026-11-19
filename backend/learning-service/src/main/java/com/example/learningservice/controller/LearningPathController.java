package com.example.learningservice.controller;

import com.example.learningservice.model.dto.response.ApiResponse;
import com.example.learningservice.model.dto.response.LearningPathResponse;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.LearningPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/learning/me", "/learning/me", "/api/v1/me", "/me"})
@RequiredArgsConstructor
@Tag(name = "Learning Path & Upskilling Progress", description = "Sinh lộ trình học bổ sung từ kết quả phân tích khoảng thiếu hụt và theo dõi tiến độ (DT-03)")
public class LearningPathController {

    private final LearningPathService learningPathService;
    private final UserContextHelper userContextHelper;

    @PostMapping("/learning-path")
    @Operation(summary = "Sinh lộ trình học tập tối ưu theo mục tiêu (DT-03)",
            description = "Sinh lộ trình học tập từ kết quả phân tích Gap (DT-01) và khóa học gợi ý (DT-02). " +
                    "Quy tắc ưu tiên: Kỹ năng bắt buộc trước, kỹ năng thiếu nhiều trước. " +
                    "Mỗi kỹ năng chọn 1-2 khóa học. Lộ trình cũ đang ACTIVE sẽ tự động chuyển sang ARCHIVED.")
    public ResponseEntity<ApiResponse<LearningPathResponse>> generateLearningPath(
            @Parameter(description = "ID người dùng truyền từ Gateway hoặc Mock")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Kích hoạt sinh lộ trình học tập tối ưu cho userId: {}", userId);

        LearningPathResponse response = learningPathService.generateLearningPath(userId, authHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Sinh lộ trình học tập tối ưu thành công", response));
    }

    @GetMapping("/learning-path")
    @Operation(summary = "Lấy lộ trình học tập và tiến độ hiện tại của ứng viên (DT-03)",
            description = "Trả về thông tin lộ trình ACTIVE hiện tại, danh sách các bước học, % hoàn thành tổng thể, " +
                    "và mức độ tăng trưởng sẵn sàng so với lúc tạo lộ trình. " +
                    "Các bước tự động chuyển trạng thái DONE khi môn học tương ứng COMPLETED.")
    public ResponseEntity<ApiResponse<LearningPathResponse>> getMyLearningPath(
            @Parameter(description = "ID người dùng truyền từ Gateway hoặc Mock")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Lấy thông tin lộ trình học tập cho userId: {}", userId);

        LearningPathResponse response = learningPathService.getMyLearningPath(userId, authHeader);
        if (response != null) {
            return ResponseEntity.ok(ApiResponse.success("Lấy lộ trình học tập thành công", response));
        } else {
            return ResponseEntity.ok(ApiResponse.success("Chưa có lộ trình học tập nào được tạo", null));
        }
    }
}

