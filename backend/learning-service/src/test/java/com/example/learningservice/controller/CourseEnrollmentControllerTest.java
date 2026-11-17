package com.example.learningservice.controller;

import com.example.learningservice.advice.GlobalHandleException;
import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.model.dto.request.EnrollmentRequest;
import com.example.learningservice.model.dto.response.EnrollmentResponse;
import com.example.learningservice.model.dto.response.SkillCourseSuggestionResponse;
import com.example.learningservice.model.dto.response.SuggestedCourseItemResponse;
import com.example.learningservice.model.entity.EnrollmentStatus;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.CourseSuggestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CourseSuggestionService courseSuggestionService;

    @Spy
    private UserContextHelper userContextHelper = new UserContextHelper();

    @InjectMocks
    private CourseEnrollmentController controller;

    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandleException())
                .build();
    }

    @Test
    @DisplayName("AC1: GET /me/course-suggestions trả về danh sách gợi ý theo kỹ năng thiếu")
    void getCourseSuggestions_success() throws Exception {
        SuggestedCourseItemResponse course = SuggestedCourseItemResponse.builder()
                .lmsCourseId("CRS-01")
                .courseName("Lập trình Spring Boot nâng cao")
                .lessonHint("Bài 1-3: OOP nâng cao")
                .levelFrom(0)
                .levelTo(75)
                .level("Tốt")
                .isEnrolled(false)
                .build();

        SkillCourseSuggestionResponse suggestion = SkillCourseSuggestionResponse.builder()
                .skillId("JAVA_CORE")
                .skillName("Java Core")
                .requiredScore(75)
                .currentScore(50)
                .gap(25)
                .priority(GapPriority.HIGH)
                .courses(List.of(course))
                .build();

        when(courseSuggestionService.getSuggestionsForCandidate(eq(CANDIDATE_ID), any()))
                .thenReturn(List.of(suggestion));

        mockMvc.perform(get("/api/v1/learning/me/course-suggestions")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].skillId", is("JAVA_CORE")))
                .andExpect(jsonPath("$.data[0].gap", is(25)))
                .andExpect(jsonPath("$.data[0].courses[0].lmsCourseId", is("CRS-01")));
    }

    @Test
    @DisplayName("Ghi danh khóa học thành công trả về 201 Created")
    void enrollCourse_success() throws Exception {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .lmsCourseId("CRS-01")
                .skillId("JAVA_CORE")
                .build();

        EnrollmentResponse response = EnrollmentResponse.builder()
                .id(UUID.randomUUID())
                .userId(CANDIDATE_ID)
                .lmsCourseId("CRS-01")
                .courseName("Lập trình Spring Boot nâng cao")
                .skillId("JAVA_CORE")
                .status(EnrollmentStatus.IN_PROGRESS)
                .progress(BigDecimal.ZERO)
                .enrolledAt(Instant.now())
                .build();

        when(courseSuggestionService.enrollCourse(eq(CANDIDATE_ID), any(EnrollmentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/learning/me/enrollments")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(201)))
                .andExpect(jsonPath("$.data.lmsCourseId", is("CRS-01")))
                .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("AC2: Ghi danh cùng khóa học lần 2 trả về HTTP 409 Conflict")
    void enrollCourse_duplicate_returns409Conflict() throws Exception {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .lmsCourseId("CRS-01")
                .skillId("JAVA_CORE")
                .build();

        when(courseSuggestionService.enrollCourse(eq(CANDIDATE_ID), any(EnrollmentRequest.class)))
                .thenThrow(new BusinessException("Bạn đã ghi danh khóa học này rồi.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/learning/me/enrollments")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(409)))
                .andExpect(jsonPath("$.message", is("Bạn đã ghi danh khóa học này rồi.")));
    }

    @Test
    @DisplayName("AC4: Khi Mock LMS lỗi -> trả về lỗi (502), không lưu vào DB")
    void enrollCourse_lmsError_returnsError() throws Exception {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .lmsCourseId("CRS-ERROR")
                .skillId("JAVA_CORE")
                .build();

        when(courseSuggestionService.enrollCourse(eq(CANDIDATE_ID), any(EnrollmentRequest.class)))
                .thenThrow(new BusinessException("Hệ thống LMS từ chối ghi danh hoặc đang bận", HttpStatus.BAD_GATEWAY));

        mockMvc.perform(post("/api/v1/learning/me/enrollments")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(502)));
    }

    @Test
    @DisplayName("GET /me/enrollments trả về danh sách khóa học ứng viên đã ghi danh")
    void getMyEnrollments_success() throws Exception {
        EnrollmentResponse enr = EnrollmentResponse.builder()
                .id(UUID.randomUUID())
                .userId(CANDIDATE_ID)
                .lmsCourseId("CRS-01")
                .courseName("Lập trình Spring Boot nâng cao")
                .skillId("JAVA_CORE")
                .status(EnrollmentStatus.IN_PROGRESS)
                .progress(BigDecimal.valueOf(45.0))
                .enrolledAt(Instant.now())
                .build();

        when(courseSuggestionService.getMyEnrollments(eq(CANDIDATE_ID)))
                .thenReturn(List.of(enr));

        mockMvc.perform(get("/api/v1/learning/me/enrollments")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].lmsCourseId", is("CRS-01")))
                .andExpect(jsonPath("$.data[0].progress", is(45.0)));
    }

    @Test
    @DisplayName("AC3: POST /me/enrollments/refresh làm mới tiến độ học tập từ LMS")
    void refreshEnrollments_success() throws Exception {
        var refreshResp = com.example.learningservice.model.dto.response.RefreshEnrollmentsResponse.builder()
                .totalUpdated(1)
                .completedCoursesCount(1)
                .enrollments(List.of())
                .build();

        when(courseSuggestionService.refreshEnrollments(eq(CANDIDATE_ID), any()))
                .thenReturn(refreshResp);

        mockMvc.perform(post("/api/v1/learning/me/enrollments/refresh")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data.totalUpdated", is(1)))
                .andExpect(jsonPath("$.data.completedCoursesCount", is(1)));
    }
}

