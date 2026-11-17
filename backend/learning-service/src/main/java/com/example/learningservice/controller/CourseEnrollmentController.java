package com.example.learningservice.controller;

import com.example.learningservice.model.dto.request.EnrollmentRequest;
import com.example.learningservice.model.dto.response.ApiResponse;
import com.example.learningservice.model.dto.response.EnrollmentResponse;
import com.example.learningservice.model.dto.response.SkillCourseSuggestionResponse;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.CourseSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/learning/me", "/learning/me", "/api/v1/me", "/me"})
@RequiredArgsConstructor
@Tag(name = "LMS Course Suggestions & Enrollments", description = "Gợi ý khóa học LMS theo kỹ năng thiếu, ghi danh và theo dõi tiến độ (DT-02)")
public class CourseEnrollmentController {

    private final CourseSuggestionService courseSuggestionService;
    private final UserContextHelper userContextHelper;

    @GetMapping("/course-suggestions")
    @Operation(summary = "Lấy danh sách khóa học gợi ý LMS theo từng kỹ năng thiếu",
            description = "Dựa trên kết quả phân tích khoảng cách năng lực (DT-01), trả về danh sách các khóa học gợi ý phù hợp theo từng kỹ năng và bậc hiện tại.")
    public ResponseEntity<ApiResponse<List<SkillCourseSuggestionResponse>>> getCourseSuggestions(
            @Parameter(description = "ID người dùng truyền từ Gateway hoặc Mock")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Lấy danh sách khóa học gợi ý cho userId: {}", userId);

        List<SkillCourseSuggestionResponse> suggestions = courseSuggestionService.getSuggestionsForCandidate(userId, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khóa học gợi ý thành công", suggestions));
    }

    @PostMapping("/enrollments")
    @Operation(summary = "Ghi danh khóa học bổ sung trên LMS",
            description = "Ghi danh khóa học được gợi ý. Gọi sang mock LMS POST /lms/enrollments. Nếu ghi danh trùng sẽ trả về 409 Conflict. Nếu mock LMS lỗi sẽ không lưu vào CSDL.")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollCourse(
            @Parameter(description = "ID người dùng truyền từ Gateway hoặc Mock")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody EnrollmentRequest request) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Ghi danh khóa học cho userId: {}, request: {}", userId, request);

        EnrollmentResponse response = courseSuggestionService.enrollCourse(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Ghi danh khóa học thành công", response));
    }

    @GetMapping("/enrollments")
    @Operation(summary = "Lấy danh sách các khóa học ứng viên đã ghi danh",
            description = "Trả về lịch sử và trạng thái các khóa học LMS mà ứng viên hiện tại đã ghi danh.")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> getMyEnrollments(
            @Parameter(description = "ID người dùng truyền từ Gateway hoặc Mock")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Lấy danh sách ghi danh của userId: {}", userId);

        List<EnrollmentResponse> list = courseSuggestionService.getMyEnrollments(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khóa học đã ghi danh thành công", list));
    }

    @PostMapping("/enrollments/refresh")
    @Operation(summary = "Làm mới và đồng bộ tiến độ học tập từ LMS (DT-02 / AC3)",
            description = "Gọi mock LMS GET /lms/enrollments/{lmsUserId} để lấy progress, score mới nhất. Khi có khóa học COMPLETED sẽ tự động tạo skill_evidence và gọi recomputeSkills.")
    public ResponseEntity<ApiResponse<com.example.learningservice.model.dto.response.RefreshEnrollmentsResponse>> refreshEnrollments(
            @Parameter(description = "ID người dùng truyền từ Gateway hoặc Mock")
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerUserRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        UUID userId = userContextHelper.resolveCandidateUserId(headerUserId, headerUserRole, authHeader);
        log.info("API: Yêu cầu làm mới tiến độ học tập cho userId: {}", userId);

        com.example.learningservice.model.dto.response.RefreshEnrollmentsResponse response = 
                courseSuggestionService.refreshEnrollments(userId, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Đồng bộ tiến độ học tập từ LMS thành công", response));
    }
}

