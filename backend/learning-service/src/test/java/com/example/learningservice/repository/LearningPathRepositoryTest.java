package com.example.learningservice.repository;

import com.example.learningservice.model.entity.LearningPath;
import com.example.learningservice.model.entity.LearningPathStatus;
import com.example.learningservice.model.entity.LearningPathStep;
import com.example.learningservice.model.entity.StepStatus;
import com.example.learningservice.model.entity.TargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:learning_repo_test_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Transactional
class LearningPathRepositoryTest {

    @Autowired
    private LearningPathRepository learningPathRepository;

    @Autowired
    private LearningPathStepRepository learningPathStepRepository;

    @Test
    @DisplayName("Lưu và truy vấn thành công LearningPath kèm các bước với CASCADE")
    void testSaveAndFindActivePath() {
        UUID userId = UUID.randomUUID();

        LearningPath path = LearningPath.builder()
                .userId(userId)
                .targetType(TargetType.POSITION)
                .targetId("11111111-1111-1111-1111-111111111101")
                .readinessAtStart(BigDecimal.valueOf(45.50))
                .status(LearningPathStatus.ACTIVE)
                .build();

        LearningPathStep step1 = LearningPathStep.builder()
                .stepOrder(1)
                .skillId("JAVA_CORE")
                .lmsCourseId("CRS-01")
                .status(StepStatus.DOING)
                .build();

        LearningPathStep step2 = LearningPathStep.builder()
                .stepOrder(2)
                .skillId("SPRING_BOOT")
                .lmsCourseId("CRS-01")
                .status(StepStatus.TODO)
                .build();

        path.addStep(step1);
        path.addStep(step2);

        LearningPath saved = learningPathRepository.save(path);
        assertThat(saved.getId()).isNotNull();

        Optional<LearningPath> foundOpt = learningPathRepository.findByUserIdAndStatus(userId, LearningPathStatus.ACTIVE);
        assertThat(foundOpt).isPresent();
        LearningPath found = foundOpt.get();

        assertThat(found.getReadinessAtStart()).isEqualByComparingTo("45.50");
        assertThat(found.getSteps()).hasSize(2);
        assertThat(found.getSteps().get(0).getStepOrder()).isEqualTo(1);
        assertThat(found.getSteps().get(0).getStatus()).isEqualTo(StepStatus.DOING);
        assertThat(found.getSteps().get(1).getStepOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cập nhật lưu trữ (Archive) lộ trình cũ khi có mục tiêu mới")
    void testArchivePreviousPaths() {
        UUID userId = UUID.randomUUID();

        LearningPath oldPath = LearningPath.builder()
                .userId(userId)
                .targetType(TargetType.POSITION)
                .targetId("pos-old")
                .readinessAtStart(BigDecimal.valueOf(30.00))
                .status(LearningPathStatus.ACTIVE)
                .build();
        learningPathRepository.saveAndFlush(oldPath);

        int updatedCount = learningPathRepository.updateStatusByUserIdAndCurrentStatus(
                userId,
                LearningPathStatus.ACTIVE,
                LearningPathStatus.ARCHIVED
        );
        assertThat(updatedCount).isEqualTo(1);

        Optional<LearningPath> activeOpt = learningPathRepository.findByUserIdAndStatus(userId, LearningPathStatus.ACTIVE);
        assertThat(activeOpt).isEmpty();

        List<LearningPath> archivedList = learningPathRepository.findAllByUserIdAndStatus(userId, LearningPathStatus.ARCHIVED);
        assertThat(archivedList).hasSize(1);
        assertThat(archivedList.get(0).getTargetId()).isEqualTo("pos-old");
    }

    @Test
    @DisplayName("Truy vấn các bước học tập theo pathId và cập nhật trạng thái DONE")
    void testFindStepsByPathIdAndMarkDone() {
        UUID userId = UUID.randomUUID();

        LearningPath path = LearningPath.builder()
                .userId(userId)
                .targetType(TargetType.JOB)
                .targetId("job-001")
                .readinessAtStart(BigDecimal.valueOf(60.00))
                .status(LearningPathStatus.ACTIVE)
                .build();

        LearningPathStep step = LearningPathStep.builder()
                .stepOrder(1)
                .skillId("SQL")
                .lmsCourseId("CRS-03")
                .status(StepStatus.TODO)
                .build();
        path.addStep(step);

        LearningPath savedPath = learningPathRepository.saveAndFlush(path);
        UUID pathId = savedPath.getId();

        List<LearningPathStep> steps = learningPathStepRepository.findByLearningPathIdOrderByStepOrderAsc(pathId);
        assertThat(steps).hasSize(1);

        LearningPathStep stepToUpdate = steps.get(0);
        stepToUpdate.setStatus(StepStatus.DONE);
        stepToUpdate.setDoneAt(Instant.now());
        learningPathStepRepository.saveAndFlush(stepToUpdate);

        List<LearningPathStep> doneSteps = learningPathStepRepository.findByLearningPathIdAndStatus(pathId, StepStatus.DONE);
        assertThat(doneSteps).hasSize(1);
        assertThat(doneSteps.get(0).getDoneAt()).isNotNull();
    }
}
