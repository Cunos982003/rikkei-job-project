package com.example.learningservice.service;

import com.example.learningservice.client.JobServiceClient;
import com.example.learningservice.client.ProfileServiceClient;
import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.model.dto.response.JobDetailResponse;
import com.example.learningservice.model.dto.response.JobSkillResponse;
import com.example.learningservice.model.dto.response.ProfileSkillResponse;
import com.example.learningservice.model.entity.CandidateTarget;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.model.entity.PositionSkill;
import com.example.learningservice.model.entity.PositionSkillId;
import com.example.learningservice.model.entity.TargetPosition;
import com.example.learningservice.model.entity.TargetType;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.GapResultRepository;
import com.example.learningservice.repository.PositionSkillRepository;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.impl.GapAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GapAnalysisServiceTest {

    @Mock
    private CandidateTargetRepository candidateTargetRepository;

    @Mock
    private TargetPositionRepository targetPositionRepository;

    @Mock
    private PositionSkillRepository positionSkillRepository;

    @Mock
    private GapResultRepository gapResultRepository;

    @Mock
    private ProfileServiceClient profileServiceClient;

    @Mock
    private JobServiceClient jobServiceClient;

    @InjectMocks
    private GapAnalysisServiceImpl gapAnalysisService;

    private UUID userId;
    private UUID positionId;
    private TargetPosition targetPosition;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        positionId = UUID.randomUUID();

        targetPosition = TargetPosition.builder()
                .id(positionId)
                .code("JAVA_BACKEND_FRESHER")
                .name("Java Backend Fresher")
                .description("Mô tả vị trí Java Fresher")
                .skills(new ArrayList<>())
                .build();

        List<PositionSkill> skills = List.of(
                PositionSkill.builder()
                        .id(new PositionSkillId(positionId, "JAVA_CORE"))
                        .targetPosition(targetPosition)
                        .skillName("Java Core")
                        .minScore(75)
                        .isMandatory(true)
                        .build(),
                PositionSkill.builder()
                        .id(new PositionSkillId(positionId, "SPRING_BOOT"))
                        .targetPosition(targetPosition)
                        .skillName("Spring Boot")
                        .minScore(70)
                        .isMandatory(true)
                        .build(),
                PositionSkill.builder()
                        .id(new PositionSkillId(positionId, "SQL"))
                        .targetPosition(targetPosition)
                        .skillName("SQL")
                        .minScore(70)
                        .isMandatory(true)
                        .build(),
                PositionSkill.builder()
                        .id(new PositionSkillId(positionId, "REST_API"))
                        .targetPosition(targetPosition)
                        .skillName("REST API")
                        .minScore(70)
                        .isMandatory(true)
                        .build(),
                PositionSkill.builder()
                        .id(new PositionSkillId(positionId, "GIT"))
                        .targetPosition(targetPosition)
                        .skillName("Git")
                        .minScore(60)
                        .isMandatory(false)
                        .build()
        );

        targetPosition.setSkills(skills);
    }

    @Test
    @DisplayName("Nghiệm thu: Ứng viên có điểm biết trước -> kết quả thiếu đúng từng kỹ năng và loại trừ kỹ năng đã đạt")
    void testAnalyzeGap_WithKnownCandidateScores() {
        CandidateTarget candidateTarget = CandidateTarget.builder()
                .userId(userId)
                .targetType(TargetType.POSITION)
                .positionId(positionId)
                .chosenAt(Instant.now())
                .build();

        when(candidateTargetRepository.findByUserId(userId)).thenReturn(Optional.of(candidateTarget));
        when(targetPositionRepository.findWithSkillsById(positionId)).thenReturn(Optional.of(targetPosition));

        List<ProfileSkillResponse> candidateSkills = List.of(
                ProfileSkillResponse.builder().skillId("1").skillCode("JAVA_CORE").skillName("Java Core").verifiedScore(85).build(),
                ProfileSkillResponse.builder().skillId("2").skillCode("SPRING_BOOT").skillName("Spring Boot").verifiedScore(78).build(),
                ProfileSkillResponse.builder().skillId("3").skillCode("SQL").skillName("SQL").verifiedScore(72).build(),
                ProfileSkillResponse.builder().skillId("4").skillCode("GIT").skillName("Git").verifiedScore(70).build()
        );
        when(profileServiceClient.getCandidateSkills(eq(userId), any())).thenReturn(candidateSkills);

        GapAnalysisResponse response = gapAnalysisService.analyzeGap(userId, null, null);

        assertNotNull(response);
        assertEquals(5, response.getTotalRequiredSkills());
        assertEquals(4, response.getPassedSkillsCount());
        assertEquals(1, response.getMissingSkillsCount());
        assertEquals(80.0, response.getReadiness());

        assertEquals(1, response.getItems().size());
        var missingSkill = response.getItems().get(0);
        assertEquals("REST_API", missingSkill.getSkillId());
        assertEquals("REST API", missingSkill.getSkillName());
        assertEquals(70, missingSkill.getRequired());
        assertEquals(0, missingSkill.getCurrent());
        assertEquals(70, missingSkill.getGap());
        assertEquals(GapPriority.HIGH, missingSkill.getPriority());

        assertTrue(response.getItems().stream().noneMatch(i -> i.getSkillName().equals("Java Core")));
        assertTrue(response.getItems().stream().noneMatch(i -> i.getSkillName().equals("Spring Boot")));
        assertTrue(response.getItems().stream().noneMatch(i -> i.getSkillName().equals("SQL")));
        assertTrue(response.getItems().stream().noneMatch(i -> i.getSkillName().equals("Git")));

        verify(gapResultRepository, times(1)).deleteByUserIdAndTargetId(userId, positionId.toString());
        verify(gapResultRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Nghiệm thu: Phân tích theo JD dùng đúng min_score và danh sách job_skills của JD")
    void testAnalyzeGap_WithJobOverride() {
        String jobId = "job-001";
        JobDetailResponse mockJob = JobDetailResponse.builder()
                .id(jobId)
                .title("Kỹ sư Java Cao cấp")
                .companyName("Rikkei Corp")
                .jobSkills(List.of(
                        JobSkillResponse.builder().skillId("JAVA_CORE").skillName("Java Core").minScore(90).isMandatory(true).build(),
                        JobSkillResponse.builder().skillId("DOCKER").skillName("Docker").minScore(75).isMandatory(false).build()
                ))
                .build();

        when(jobServiceClient.getJobById(jobId)).thenReturn(mockJob);

        List<ProfileSkillResponse> candidateSkills = List.of(
                ProfileSkillResponse.builder().skillId("1").skillCode("JAVA_CORE").skillName("Java Core").verifiedScore(80).build(),
                ProfileSkillResponse.builder().skillId("2").skillCode("DOCKER").skillName("Docker").verifiedScore(60).build()
        );
        when(profileServiceClient.getCandidateSkills(eq(userId), any())).thenReturn(candidateSkills);

        GapAnalysisResponse response = gapAnalysisService.analyzeGap(userId, jobId, null);

        assertNotNull(response);
        assertEquals(TargetType.JOB, response.getTarget().getType());
        assertEquals(jobId, response.getTarget().getId());
        assertEquals(2, response.getTotalRequiredSkills());
        assertEquals(0, response.getPassedSkillsCount());
        assertEquals(2, response.getMissingSkillsCount());
        assertEquals(0.0, response.getReadiness());

        assertEquals(2, response.getItems().size());
        assertEquals("Docker", response.getItems().get(0).getSkillName());
        assertEquals(15, response.getItems().get(0).getGap());
        assertEquals(GapPriority.MEDIUM, response.getItems().get(0).getPriority());

        assertEquals("Java Core", response.getItems().get(1).getSkillName());
        assertEquals(10, response.getItems().get(1).getGap());
        assertEquals(GapPriority.LOW, response.getItems().get(1).getPriority());
    }

    @Test
    @DisplayName("Quy tắc xác định độ ưu tiên: HIGH (>20 & mandatory), MEDIUM (>10), LOW (<=10)")
    void testPriorityLogic() {
        String jobId = "job-priority-test";
        JobDetailResponse mockJob = JobDetailResponse.builder()
                .id(jobId)
                .title("Priority Test Job")
                .jobSkills(List.of(
                        JobSkillResponse.builder().skillId("SKILL_HIGH").skillName("Skill High").minScore(80).isMandatory(true).build(),
                        JobSkillResponse.builder().skillId("SKILL_MED").skillName("Skill Medium").minScore(70).isMandatory(false).build(),
                        JobSkillResponse.builder().skillId("SKILL_LOW").skillName("Skill Low").minScore(50).isMandatory(true).build()
                ))
                .build();

        when(jobServiceClient.getJobById(jobId)).thenReturn(mockJob);

        List<ProfileSkillResponse> candidateSkills = List.of(
                ProfileSkillResponse.builder().skillId("1").skillCode("SKILL_HIGH").skillName("Skill High").verifiedScore(50).build(),
                ProfileSkillResponse.builder().skillId("2").skillCode("SKILL_MED").skillName("Skill Medium").verifiedScore(55).build(),
                ProfileSkillResponse.builder().skillId("3").skillCode("SKILL_LOW").skillName("Skill Low").verifiedScore(45).build()
        );
        when(profileServiceClient.getCandidateSkills(eq(userId), any())).thenReturn(candidateSkills);

        GapAnalysisResponse response = gapAnalysisService.analyzeGap(userId, jobId, null);

        assertEquals(3, response.getItems().size());
        assertEquals(GapPriority.HIGH, response.getItems().stream().filter(i -> i.getSkillId().equals("SKILL_HIGH")).findFirst().get().getPriority());
        assertEquals(GapPriority.MEDIUM, response.getItems().stream().filter(i -> i.getSkillId().equals("SKILL_MED")).findFirst().get().getPriority());
        assertEquals(GapPriority.LOW, response.getItems().stream().filter(i -> i.getSkillId().equals("SKILL_LOW")).findFirst().get().getPriority());
    }

    @Test
    @DisplayName("Ngoại lệ: Khi người dùng chưa thiết lập mục tiêu và không truyền jobId override")
    void testAnalyzeGap_NoTargetThrowsException() {
        when(candidateTargetRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> gapAnalysisService.analyzeGap(userId, null, null));
    }
}

