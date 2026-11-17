package com.example.learningservice.service;

import com.example.learningservice.client.LmsClient;
import com.example.learningservice.client.ProfileServiceClient;
import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.model.dto.lms.LmsCourseDto;
import com.example.learningservice.model.dto.lms.LmsEnrollmentDto;
import com.example.learningservice.model.dto.request.EnrollmentRequest;
import com.example.learningservice.model.dto.response.EnrollmentResponse;
import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.model.dto.response.GapItemResponse;
import com.example.learningservice.model.dto.response.SkillCourseSuggestionResponse;
import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.EnrollmentStatus;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.model.entity.SkillCourseMapping;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.EnrollmentRepository;
import com.example.learningservice.repository.SkillCourseMappingRepository;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.impl.CourseSuggestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSuggestionServiceImplTest {

    @Mock
    private GapAnalysisService gapAnalysisService;

    @Mock
    private SkillCourseMappingRepository skillCourseMappingRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CandidateTargetRepository candidateTargetRepository;

    @Mock
    private TargetPositionRepository targetPositionRepository;

    @Mock
    private ProfileServiceClient profileServiceClient;

    @Mock
    private LmsClient lmsClient;

    @InjectMocks
    private CourseSuggestionServiceImpl courseSuggestionService;

    private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private SkillCourseMapping mappingJava;

    @BeforeEach
    void setUp() {
        mappingJava = SkillCourseMapping.builder()
                .id(UUID.randomUUID())
                .skillId("JAVA_CORE")
                .lmsCourseId("CRS-01")
                .courseName("Lập trình Spring Boot nâng cao")
                .lessonHint("Bài 1-3: OOP nâng cao")
                .levelFrom(0)
                .levelTo(75)
                .build();
    }

    @Test
    @DisplayName("AC1: Gợi ý khóa học đúng theo bảng ánh xạ cho kỹ năng bị thiếu")
    void getSuggestionsForCandidate_returnsMatchingCourses() {
        GapItemResponse missingSkill = GapItemResponse.builder()
                .skillId("JAVA_CORE")
                .skillName("Java Core")
                .required(75)
                .current(50)
                .gap(25)
                .priority(GapPriority.HIGH)
                .build();

        GapAnalysisResponse gapResponse = GapAnalysisResponse.builder()
                .readiness(60.0)
                .items(List.of(missingSkill))
                .build();

        when(candidateTargetRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(com.example.learningservice.model.entity.CandidateTarget.builder()
                        .userId(USER_ID)
                        .targetType(com.example.learningservice.model.entity.TargetType.POSITION)
                        .build()));
        when(gapAnalysisService.analyzeGap(USER_ID, null, "Bearer token")).thenReturn(gapResponse);
        when(skillCourseMappingRepository.findBySkillIdIn(List.of("JAVA_CORE"))).thenReturn(List.of(mappingJava));
        when(lmsClient.getCourses()).thenReturn(List.of(
                LmsCourseDto.builder().courseId("CRS-01").name("Spring Boot").level("Tốt").build()
        ));
        when(enrollmentRepository.findByUserId(USER_ID)).thenReturn(List.of());

        List<SkillCourseSuggestionResponse> suggestions = courseSuggestionService.getSuggestionsForCandidate(USER_ID, "Bearer token");

        assertThat(suggestions).hasSize(1);
        SkillCourseSuggestionResponse item = suggestions.get(0);
        assertThat(item.getSkillId()).isEqualTo("JAVA_CORE");
        assertThat(item.getGap()).isEqualTo(25);
        assertThat(item.getCourses()).hasSize(1);
        assertThat(item.getCourses().get(0).getLmsCourseId()).isEqualTo("CRS-01");
        assertThat(item.getCourses().get(0).getIsEnrolled()).isFalse();
    }

    @Test
    @DisplayName("AC2: Ghi danh cùng khóa 2 lần ném BusinessException với HTTP 409 Conflict")
    void enrollCourse_duplicateEnrollment_throws409Conflict() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .lmsCourseId("CRS-01")
                .skillId("JAVA_CORE")
                .build();

        when(enrollmentRepository.existsByUserIdAndLmsCourseId(USER_ID, "CRS-01")).thenReturn(true);

        assertThatThrownBy(() -> courseSuggestionService.enrollCourse(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessage()).contains("không được ghi danh trùng");
                });

        verify(lmsClient, never()).enrollCourse(any(), any());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC4: Khi Mock LMS gặp sự cố -> ném lỗi và KHÔNG lưu bản ghi vào DB")
    void enrollCourse_mockError_doesNotSaveToDatabase() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .lmsCourseId("CRS-ERROR")
                .skillId("JAVA_CORE")
                .build();

        when(enrollmentRepository.existsByUserIdAndLmsCourseId(USER_ID, "CRS-ERROR")).thenReturn(false);
        when(lmsClient.enrollCourse(USER_ID.toString(), "CRS-ERROR"))
                .thenThrow(new BusinessException("LMS Server Error", HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> courseSuggestionService.enrollCourse(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });

        // Đảm bảo tuyệt đối không gọi enrollmentRepository.save(...)
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ghi danh hợp lệ thành công và lưu vào database")
    void enrollCourse_success() {
        EnrollmentRequest request = EnrollmentRequest.builder()
                .lmsCourseId("CRS-01")
                .skillId("JAVA_CORE")
                .build();

        when(enrollmentRepository.existsByUserIdAndLmsCourseId(USER_ID, "CRS-01")).thenReturn(false);
        when(lmsClient.enrollCourse(USER_ID.toString(), "CRS-01"))
                .thenReturn(LmsEnrollmentDto.builder().id("enr-123").progress(BigDecimal.ZERO).status("IN_PROGRESS").build());

        Enrollment savedEnr = Enrollment.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .lmsCourseId("CRS-01")
                .skillId("JAVA_CORE")
                .status(EnrollmentStatus.IN_PROGRESS)
                .progress(BigDecimal.ZERO)
                .enrolledAt(Instant.now())
                .build();

        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(savedEnr);
        when(skillCourseMappingRepository.findBySkillIdAndLmsCourseId("JAVA_CORE", "CRS-01"))
                .thenReturn(Optional.of(mappingJava));

        EnrollmentResponse res = courseSuggestionService.enrollCourse(USER_ID, request);

        assertThat(res).isNotNull();
        assertThat(res.getLmsCourseId()).isEqualTo("CRS-01");
        assertThat(res.getCourseName()).isEqualTo("Lập trình Spring Boot nâng cao");
        assertThat(res.getStatus()).isEqualTo(EnrollmentStatus.IN_PROGRESS);
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("AC3: Khi khóa học chuyển COMPLETED -> đồng bộ bằng chứng sang profile-service")
    void refreshEnrollments_courseCompleted_triggersProfileEvidenceSync() {
        Enrollment inProgressEnr = Enrollment.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .lmsCourseId("CRS-01")
                .skillId("JAVA_CORE")
                .status(EnrollmentStatus.IN_PROGRESS)
                .progress(BigDecimal.valueOf(50.0))
                .enrolledAt(Instant.now().minusSeconds(86400))
                .build();

        when(enrollmentRepository.findByUserId(USER_ID)).thenReturn(List.of(inProgressEnr));

        LmsEnrollmentDto completedLmsDto = LmsEnrollmentDto.builder()
                .courseId("CRS-01")
                .status("COMPLETED")
                .progress(BigDecimal.valueOf(100.0))
                .score(BigDecimal.valueOf(88.0))
                .build();

        when(lmsClient.getStudentEnrollments(USER_ID.toString())).thenReturn(List.of(completedLmsDto));
        when(skillCourseMappingRepository.findBySkillIdAndLmsCourseId("JAVA_CORE", "CRS-01"))
                .thenReturn(Optional.of(mappingJava));

        var response = courseSuggestionService.refreshEnrollments(USER_ID, "Bearer token");

        assertThat(response).isNotNull();
        assertThat(response.getCompletedCoursesCount()).isEqualTo(1);
        assertThat(response.getTotalUpdated()).isEqualTo(1);
        assertThat(inProgressEnr.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);

        // Xác nhận đã gọi syncLmsSkillEvidence sang profile-service để recomputeSkills (AC3)
        verify(profileServiceClient).syncLmsSkillEvidence(
                eq(USER_ID),
                eq("JAVA_CORE"),
                eq("CRS-01"),
                eq("Lập trình Spring Boot nâng cao"),
                eq((short) 88)
        );
    }
}

