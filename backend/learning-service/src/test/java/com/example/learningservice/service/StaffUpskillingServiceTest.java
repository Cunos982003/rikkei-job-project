package com.example.learningservice.service;

import com.example.learningservice.model.dto.response.staff.UpskillingCandidateDto;
import com.example.learningservice.model.dto.response.staff.UpskillingSummaryResponse;
import com.example.learningservice.model.entity.CandidateProfile;
import com.example.learningservice.model.entity.CandidateTarget;
import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.EnrollmentStatus;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.model.entity.GapResult;
import com.example.learningservice.model.entity.SkillCourseMapping;
import com.example.learningservice.model.entity.TargetPosition;
import com.example.learningservice.model.entity.TargetType;
import com.example.learningservice.repository.CandidateProfileRepository;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.EnrollmentRepository;
import com.example.learningservice.repository.GapResultRepository;
import com.example.learningservice.repository.SkillCourseMappingRepository;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.impl.StaffUpskillingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffUpskillingServiceTest {

    @Mock
    private CandidateProfileRepository candidateProfileRepository;

    @Mock
    private CandidateTargetRepository candidateTargetRepository;

    @Mock
    private TargetPositionRepository targetPositionRepository;

    @Mock
    private GapResultRepository gapResultRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SkillCourseMappingRepository skillCourseMappingRepository;

    @InjectMocks
    private StaffUpskillingServiceImpl service;

    private CandidateProfile profile1;
    private CandidateProfile profile2;
    private UUID user1;
    private UUID user2;
    private UUID positionId;

    @BeforeEach
    void setUp() {
        user1 = UUID.fromString("ed854c07-f3eb-48f1-a186-4e7135f54645");
        user2 = UUID.fromString("797c5663-386b-4e96-9112-6d3e414b4449");
        positionId = UUID.fromString("11111111-1111-1111-1111-111111111101");

        profile1 = CandidateProfile.builder()
                .userId(user1)
                .fullName("Nguyễn Văn An")
                .email("candidate1@rikkeijobs.test")
                .phone("0912345600")
                .classCode("RA-JAVA-2026")
                .courseName("Khóa đào tạo Kỹ sư Phần mềm Java")
                .build();

        profile2 = CandidateProfile.builder()
                .userId(user2)
                .fullName("Trần Thị Bình")
                .email("candidate2@rikkeijobs.test")
                .phone("0912345737")
                .classCode("RA-FE-2026")
                .courseName("Khóa đào tạo Chuyên viên Frontend React")
                .build();
    }

    @Test
    @DisplayName("Lọc theo lớp ra đúng ứng viên của lớp đó (AC-1)")
    void getUpskillingList_filterByClassCode_returnsCorrectCandidates() {
        when(candidateProfileRepository.findByClassCode("RA-JAVA-2026"))
                .thenReturn(List.of(profile1));

        CandidateTarget target = CandidateTarget.builder()
                .userId(user1)
                .positionId(positionId)
                .targetType(TargetType.POSITION)
                .chosenAt(Instant.now())
                .build();
        when(candidateTargetRepository.findByUserIdIn(List.of(user1)))
                .thenReturn(List.of(target));

        TargetPosition position = TargetPosition.builder()
                .id(positionId)
                .code("JAVA_BACKEND")
                .name("Java Backend Fresher")
                .build();
        when(targetPositionRepository.findAll()).thenReturn(List.of(position));

        GapResult gap = GapResult.builder()
                .id(UUID.randomUUID())
                .userId(user1)
                .skillId("JAVA_CORE")
                .skillName("Java Core")
                .required(80)
                .current(55)
                .gap(25)
                .priority(GapPriority.HIGH)
                .targetType(TargetType.POSITION)
                .targetId(positionId.toString())
                .computedAt(Instant.now())
                .build();
        when(gapResultRepository.findByUserIdIn(List.of(user1))).thenReturn(List.of(gap));

        Enrollment enrollment = Enrollment.builder()
                .id(UUID.randomUUID())
                .userId(user1)
                .lmsCourseId("CRS-01")
                .skillId("SPRING_BOOT")
                .status(EnrollmentStatus.IN_PROGRESS)
                .progress(new BigDecimal("60.00"))
                .build();
        when(enrollmentRepository.findByUserIdIn(List.of(user1))).thenReturn(List.of(enrollment));
        when(skillCourseMappingRepository.findAll()).thenReturn(List.of());

        List<UpskillingCandidateDto> result = service.getUpskillingList(null, "RA-JAVA-2026", null, null, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCandidate().getFullName()).isEqualTo("Nguyễn Văn An");
        assertThat(result.get(0).getClassCode()).isEqualTo("RA-JAVA-2026");
        assertThat(result.get(0).getTarget()).isEqualTo("Java Backend Fresher");
        assertThat(result.get(0).getMissingSkills()).hasSize(1);
        assertThat(result.get(0).getEnrollments()).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Thống kê summary tính đúng Top 5 kỹ năng thiếu khớp seed (AC-2)")
    void getUpskillingSummary_returnsAccurateTopMissingSkills() {
        when(candidateProfileRepository.findAll()).thenReturn(List.of(profile1, profile2));

        Enrollment e1 = Enrollment.builder().userId(user1).status(EnrollmentStatus.IN_PROGRESS).build();
        Enrollment e2 = Enrollment.builder().userId(user2).status(EnrollmentStatus.COMPLETED).build();
        when(enrollmentRepository.findAll()).thenReturn(List.of(e1, e2));

        List<Object[]> mockTop = List.of(
                new Object[]{"TYPESCRIPT", "TypeScript", 10L},
                new Object[]{"JAVA_CORE", "Java Core", 8L},
                new Object[]{"REACT", "React", 7L},
                new Object[]{"SPRING_BOOT", "Spring Boot", 7L},
                new Object[]{"SQL", "SQL", 5L}
        );
        when(gapResultRepository.findTopMissingSkills()).thenReturn(mockTop);

        UpskillingSummaryResponse summary = service.getUpskillingSummary();

        assertThat(summary.getInProgressCount()).isEqualTo(1L);
        assertThat(summary.getCompletedCount()).isEqualTo(1L);
        assertThat(summary.getTopMissingSkills()).hasSize(5);
        assertThat(summary.getTopMissingSkills().get(0).getSkillName()).isEqualTo("TypeScript");
        assertThat(summary.getTopMissingSkills().get(0).getMissingCount()).isEqualTo(10L);
        assertThat(summary.getTopMissingSkills().get(1).getSkillName()).isEqualTo("Java Core");
    }
}
