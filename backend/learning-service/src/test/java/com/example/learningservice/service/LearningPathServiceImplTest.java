package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.model.dto.response.GapItemResponse;
import com.example.learningservice.model.dto.response.LearningPathResponse;
import com.example.learningservice.model.dto.response.TargetInfoResponse;
import com.example.learningservice.model.entity.CandidateTarget;
import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.EnrollmentStatus;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.model.entity.LearningPath;
import com.example.learningservice.model.entity.LearningPathStatus;
import com.example.learningservice.model.entity.LearningPathStep;
import com.example.learningservice.model.entity.SkillCourseMapping;
import com.example.learningservice.model.entity.StepStatus;
import com.example.learningservice.model.entity.TargetType;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.EnrollmentRepository;
import com.example.learningservice.repository.LearningPathRepository;
import com.example.learningservice.repository.LearningPathStepRepository;
import com.example.learningservice.repository.SkillCourseMappingRepository;
import com.example.learningservice.service.impl.LearningPathServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningPathServiceImplTest {

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private LearningPathStepRepository learningPathStepRepository;

    @Mock
    private SkillCourseMappingRepository skillCourseMappingRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CandidateTargetRepository candidateTargetRepository;

    @Mock
    private GapAnalysisService gapAnalysisService;

    @InjectMocks
    private LearningPathServiceImpl learningPathService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("DT-03 Rule: Sinh lộ trình ưu tiên kỹ năng bắt buộc trước, kỹ năng thiếu nhiều trước")
    void testGenerateLearningPath_prioritizesMandatoryAndHigherGap() {
        // Given
        CandidateTarget target = CandidateTarget.builder()
                .userId(userId)
                .targetType(TargetType.POSITION)
                .positionId(UUID.randomUUID())
                .build();
        when(candidateTargetRepository.findByUserId(userId)).thenReturn(Optional.of(target));

        // 3 kỹ năng thiếu:
        // 1. SQL: Không bắt buộc (mandatory=false), gap=30 (lớn nhất)
        // 2. JAVA_CORE: Bắt buộc (mandatory=true), gap=15
        // 3. SPRING_BOOT: Bắt buộc (mandatory=true), gap=25
        List<GapItemResponse> gapItems = new ArrayList<>();
        gapItems.add(GapItemResponse.builder()
                .skillId("SQL").skillName("SQL").required(70).current(40).gap(30).isMandatory(false).priority(GapPriority.HIGH).build());
        gapItems.add(GapItemResponse.builder()
                .skillId("JAVA_CORE").skillName("Java Core").required(80).current(65).gap(15).isMandatory(true).priority(GapPriority.MEDIUM).build());
        gapItems.add(GapItemResponse.builder()
                .skillId("SPRING_BOOT").skillName("Spring Boot").required(75).current(50).gap(25).isMandatory(true).priority(GapPriority.HIGH).build());

        TargetInfoResponse targetInfo = TargetInfoResponse.builder()
                .type(TargetType.POSITION)
                .id("pos-java")
                .name("Java Backend Fresher")
                .build();

        GapAnalysisResponse gapResponse = GapAnalysisResponse.builder()
                .target(targetInfo)
                .readiness(55.0)
                .items(gapItems)
                .build();

        when(gapAnalysisService.analyzeGap(eq(userId), any(), any())).thenReturn(gapResponse);

        // Mappings cho từng kỹ năng
        when(skillCourseMappingRepository.findBySkillId("SPRING_BOOT")).thenReturn(List.of(
                SkillCourseMapping.builder().skillId("SPRING_BOOT").lmsCourseId("CRS-SB-01").courseName("Spring Boot Chuyên Sâu").build()
        ));
        when(skillCourseMappingRepository.findBySkillId("JAVA_CORE")).thenReturn(List.of(
                SkillCourseMapping.builder().skillId("JAVA_CORE").lmsCourseId("CRS-JV-01").courseName("Java Core Nâng Cao").build()
        ));
        when(skillCourseMappingRepository.findBySkillId("SQL")).thenReturn(List.of(
                SkillCourseMapping.builder().skillId("SQL").lmsCourseId("CRS-SQL-01").courseName("Tối ưu SQL").build()
        ));

        when(enrollmentRepository.findByUserId(userId)).thenReturn(List.of());
        when(learningPathRepository.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        LearningPathResponse response = learningPathService.generateLearningPath(userId, "Bearer token");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getReadinessAtStart()).isEqualTo(55.0);
        assertThat(response.getSteps()).hasSize(3);

        // Kiểm tra thứ tự đúng quy tắc:
        // Bước 1: SPRING_BOOT (bắt buộc, gap=25)
        assertThat(response.getSteps().get(0).getSkillId()).isEqualTo("SPRING_BOOT");
        assertThat(response.getSteps().get(0).getStepOrder()).isEqualTo(1);
        assertThat(response.getSteps().get(0).getIsMandatory()).isTrue();

        // Bước 2: JAVA_CORE (bắt buộc, gap=15)
        assertThat(response.getSteps().get(1).getSkillId()).isEqualTo("JAVA_CORE");
        assertThat(response.getSteps().get(1).getStepOrder()).isEqualTo(2);
        assertThat(response.getSteps().get(1).getIsMandatory()).isTrue();

        // Bước 3: SQL (không bắt buộc dù gap=30)
        assertThat(response.getSteps().get(2).getSkillId()).isEqualTo("SQL");
        assertThat(response.getSteps().get(2).getStepOrder()).isEqualTo(3);
        assertThat(response.getSteps().get(2).getIsMandatory()).isFalse();

        // Kiểm tra đã archive lộ trình cũ
        verify(learningPathRepository).updateStatusByUserIdAndCurrentStatus(userId, LearningPathStatus.ACTIVE, LearningPathStatus.ARCHIVED);
    }

    @Test
    @DisplayName("DT-03 Rule: Giới hạn tối đa 1-2 khóa học cho mỗi kỹ năng thiếu")
    void testGenerateLearningPath_limitsToOneOrTwoCoursesPerSkill() {
        // Given
        CandidateTarget target = CandidateTarget.builder().userId(userId).targetType(TargetType.POSITION).build();
        when(candidateTargetRepository.findByUserId(userId)).thenReturn(Optional.of(target));

        GapItemResponse skill = GapItemResponse.builder()
                .skillId("JAVA_CORE").skillName("Java Core").required(80).current(50).gap(30).isMandatory(true).build();

        GapAnalysisResponse gapResponse = GapAnalysisResponse.builder()
                .target(TargetInfoResponse.builder().id("pos-1").name("Pos 1").type(TargetType.POSITION).build())
                .readiness(60.0)
                .items(List.of(skill))
                .build();
        when(gapAnalysisService.analyzeGap(eq(userId), any(), any())).thenReturn(gapResponse);

        // Kỹ năng có 4 khóa học ánh xạ
        when(skillCourseMappingRepository.findBySkillId("JAVA_CORE")).thenReturn(List.of(
                SkillCourseMapping.builder().skillId("JAVA_CORE").lmsCourseId("CRS-01").courseName("Khóa 1").build(),
                SkillCourseMapping.builder().skillId("JAVA_CORE").lmsCourseId("CRS-02").courseName("Khóa 2").build(),
                SkillCourseMapping.builder().skillId("JAVA_CORE").lmsCourseId("CRS-03").courseName("Khóa 3").build(),
                SkillCourseMapping.builder().skillId("JAVA_CORE").lmsCourseId("CRS-04").courseName("Khóa 4").build()
        ));

        when(enrollmentRepository.findByUserId(userId)).thenReturn(List.of());
        when(learningPathRepository.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        LearningPathResponse response = learningPathService.generateLearningPath(userId, null);

        // Then: Chỉ lấy tối đa 2 khóa học
        assertThat(response.getSteps()).hasSize(2);
        assertThat(response.getSteps().get(0).getLmsCourseId()).isEqualTo("CRS-01");
        assertThat(response.getSteps().get(1).getLmsCourseId()).isEqualTo("CRS-02");
    }

    @Test
    @DisplayName("DT-03 Rule: Tự động khởi tạo trạng thái bước DONE / DOING từ lịch sử ghi danh")
    void testGenerateLearningPath_autoInitializesStepStatusesFromEnrollments() {
        // Given
        CandidateTarget target = CandidateTarget.builder().userId(userId).targetType(TargetType.POSITION).build();
        when(candidateTargetRepository.findByUserId(userId)).thenReturn(Optional.of(target));

        GapItemResponse skill1 = GapItemResponse.builder()
                .skillId("JAVA_CORE").skillName("Java").required(80).current(60).gap(20).isMandatory(true).build();
        GapItemResponse skill2 = GapItemResponse.builder()
                .skillId("SQL").skillName("SQL").required(70).current(50).gap(20).isMandatory(true).build();

        GapAnalysisResponse gapResponse = GapAnalysisResponse.builder()
                .target(TargetInfoResponse.builder().id("pos-1").name("Pos 1").type(TargetType.POSITION).build())
                .readiness(65.0)
                .items(List.of(skill1, skill2))
                .build();
        when(gapAnalysisService.analyzeGap(eq(userId), any(), any())).thenReturn(gapResponse);

        when(skillCourseMappingRepository.findBySkillId("JAVA_CORE")).thenReturn(List.of(
                SkillCourseMapping.builder().skillId("JAVA_CORE").lmsCourseId("CRS-01").courseName("Java Core").build()
        ));
        when(skillCourseMappingRepository.findBySkillId("SQL")).thenReturn(List.of(
                SkillCourseMapping.builder().skillId("SQL").lmsCourseId("CRS-03").courseName("SQL Core").build()
        ));

        // User đã hoàn thành CRS-01 (COMPLETED), đang học CRS-03 (ENROLLED)
        Enrollment enr1 = Enrollment.builder()
                .userId(userId).skillId("JAVA_CORE").lmsCourseId("CRS-01")
                .status(EnrollmentStatus.COMPLETED).completedAt(Instant.now()).build();
        Enrollment enr2 = Enrollment.builder()
                .userId(userId).skillId("SQL").lmsCourseId("CRS-03")
                .status(EnrollmentStatus.ENROLLED).build();

        when(enrollmentRepository.findByUserId(userId)).thenReturn(List.of(enr1, enr2));
        when(learningPathRepository.save(any(LearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        LearningPathResponse response = learningPathService.generateLearningPath(userId, null);

        // Then
        assertThat(response.getSteps()).hasSize(2);
        assertThat(response.getSteps().get(0).getStatus()).isEqualTo(StepStatus.DONE);
        assertThat(response.getSteps().get(0).getDoneAt()).isNotNull();

        assertThat(response.getSteps().get(1).getStatus()).isEqualTo(StepStatus.DOING);
        assertThat(response.getCompletedSteps()).isEqualTo(1);
        assertThat(response.getCompletionPercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("DT-03 Rule: getMyLearningPath tự động đồng bộ trạng thái khi enrollment COMPLETED")
    void testGetMyLearningPath_syncsWithLatestEnrollments() {
        // Given
        LearningPath path = LearningPath.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .targetType(TargetType.POSITION)
                .targetId("pos-1")
                .readinessAtStart(BigDecimal.valueOf(50.0))
                .status(LearningPathStatus.ACTIVE)
                .build();

        LearningPathStep step = LearningPathStep.builder()
                .id(UUID.randomUUID())
                .stepOrder(1)
                .skillId("JAVA_CORE")
                .lmsCourseId("CRS-01")
                .status(StepStatus.DOING) // Trước đó đang DOING
                .build();
        path.addStep(step);

        when(learningPathRepository.findByUserIdAndStatus(userId, LearningPathStatus.ACTIVE)).thenReturn(Optional.of(path));

        // Hiện tại enrollment đã chuyển sang COMPLETED
        Enrollment enr = Enrollment.builder()
                .userId(userId).skillId("JAVA_CORE").lmsCourseId("CRS-01")
                .status(EnrollmentStatus.COMPLETED).completedAt(Instant.now()).score(BigDecimal.valueOf(90.0)).build();
        when(enrollmentRepository.findByUserId(userId)).thenReturn(List.of(enr));

        GapAnalysisResponse gapResponse = GapAnalysisResponse.builder()
                .target(TargetInfoResponse.builder().id("pos-1").name("Pos 1").type(TargetType.POSITION).build())
                .readiness(75.0) // Điểm sẵn sàng hiện tại tăng lên 75
                .items(List.of())
                .build();
        when(gapAnalysisService.analyzeGap(eq(userId), any(), any())).thenReturn(gapResponse);

        // When
        LearningPathResponse response = learningPathService.getMyLearningPath(userId, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSteps().get(0).getStatus()).isEqualTo(StepStatus.DONE);
        assertThat(response.getCompletionPercentage()).isEqualTo(100.0);
        assertThat(response.getReadinessAtStart()).isEqualTo(50.0);
        assertThat(response.getCurrentReadiness()).isEqualTo(75.0);
        assertThat(response.getReadinessGrowth()).isEqualTo(25.0); // Tăng trưởng +25%
        verify(learningPathRepository).save(path);
    }
}

