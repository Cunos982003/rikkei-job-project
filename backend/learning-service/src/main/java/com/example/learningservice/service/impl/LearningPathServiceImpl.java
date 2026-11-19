package com.example.learningservice.service.impl;

import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.model.dto.response.GapItemResponse;
import com.example.learningservice.model.dto.response.LearningPathResponse;
import com.example.learningservice.model.dto.response.LearningPathStepResponse;
import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.EnrollmentStatus;
import com.example.learningservice.model.entity.LearningPath;
import com.example.learningservice.model.entity.LearningPathStatus;
import com.example.learningservice.model.entity.LearningPathStep;
import com.example.learningservice.model.entity.SkillCourseMapping;
import com.example.learningservice.model.entity.StepStatus;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.EnrollmentRepository;
import com.example.learningservice.repository.LearningPathRepository;
import com.example.learningservice.repository.LearningPathStepRepository;
import com.example.learningservice.repository.SkillCourseMappingRepository;
import com.example.learningservice.service.GapAnalysisService;
import com.example.learningservice.service.LearningPathService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathStepRepository learningPathStepRepository;
    private final SkillCourseMappingRepository skillCourseMappingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CandidateTargetRepository candidateTargetRepository;
    private final GapAnalysisService gapAnalysisService;

    @Override
    @Transactional
    public LearningPathResponse generateLearningPath(UUID userId, String authHeader) {
        log.info("Bắt đầu sinh lộ trình học tập tối ưu cho userId={}", userId);

        // 1. Kiểm tra mục tiêu nghề nghiệp
        var targetOpt = candidateTargetRepository.findByUserId(userId);
        if (targetOpt.isEmpty()) {
            throw new BusinessException("Vui lòng thiết lập mục tiêu nghề nghiệp trước khi sinh lộ trình học tập.");
        }

        // 2. Lấy kết quả phân tích khoảng thiếu hụt năng lực (DT-01)
        GapAnalysisResponse gapAnalysis = gapAnalysisService.analyzeGap(userId, null, authHeader);
        if (gapAnalysis == null || gapAnalysis.getTarget() == null) {
            throw new BusinessException("Không thể phân tích khoảng thiếu hụt năng lực để tạo lộ trình.");
        }

        // 3. Lọc các kỹ năng còn thiếu (gap > 0)
        List<GapItemResponse> missingSkills = gapAnalysis.getItems() != null
                ? gapAnalysis.getItems().stream()
                .filter(item -> item.getGap() != null && item.getGap() > 0)
                .collect(Collectors.toList())
                : new ArrayList<>();

        if (missingSkills.isEmpty()) {
            log.info("Ứng viên userId={} đã đạt chuẩn tất cả kỹ năng cho mục tiêu {}", userId, gapAnalysis.getTarget().getName());
        }

        // 4. QUY TẮC SẮP XẾP DT-03: Kỹ năng bắt buộc trước, thiếu nhiều (gap lớn) trước
        missingSkills.sort((a, b) -> {
            boolean aMandatory = Boolean.TRUE.equals(a.getIsMandatory());
            boolean bMandatory = Boolean.TRUE.equals(b.getIsMandatory());
            if (aMandatory != bMandatory) {
                return aMandatory ? -1 : 1;
            }
            int aGap = a.getGap() != null ? a.getGap() : 0;
            int bGap = b.getGap() != null ? b.getGap() : 0;
            return Integer.compare(bGap, aGap);
        });

        // 5. Lấy danh sách khóa học đã ghi danh của ứng viên để xác định trạng thái bước học
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        Map<String, Enrollment> enrollmentMap = enrollments.stream()
                .collect(Collectors.toMap(
                        e -> e.getSkillId() + "_" + e.getLmsCourseId(),
                        Function.identity(),
                        (first, second) -> first
                ));

        // 6. Lưu trữ (ARCHIVED) các lộ trình ACTIVE cũ của ứng viên
        learningPathRepository.updateStatusByUserIdAndCurrentStatus(
                userId,
                LearningPathStatus.ACTIVE,
                LearningPathStatus.ARCHIVED
        );

        // 7. Tạo lộ trình mới
        BigDecimal readinessAtStart = gapAnalysis.getReadiness() != null
                ? BigDecimal.valueOf(gapAnalysis.getReadiness()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LearningPath learningPath = LearningPath.builder()
                .userId(userId)
                .targetType(gapAnalysis.getTarget().getType())
                .targetId(gapAnalysis.getTarget().getId())
                .readinessAtStart(readinessAtStart)
                .status(LearningPathStatus.ACTIVE)
                .build();

        // 8. Chọn tối đa 1–2 khóa học phù hợp cho từng kỹ năng thiếu
        int stepOrder = 1;
        Set<String> chosenCourseIds = new HashSet<>();

        for (GapItemResponse skillItem : missingSkills) {
            String skillId = skillItem.getSkillId();
            List<SkillCourseMapping> mappings = skillCourseMappingRepository.findBySkillId(skillId);
            if (mappings.isEmpty() && skillItem.getSkill() != null) {
                mappings = skillCourseMappingRepository.findBySkillId(skillItem.getSkill());
            }

            if (mappings.isEmpty()) {
                log.warn("Không tìm thấy khóa học ánh xạ cho kỹ năng: {}", skillId);
                continue;
            }

            // Ưu tiên các khóa chưa từng xuất hiện trong lộ trình; giới hạn 1–2 khóa
            List<SkillCourseMapping> selectedCourses = mappings.stream()
                    .sorted(Comparator.comparing(m -> chosenCourseIds.contains(m.getLmsCourseId()) ? 1 : 0))
                    .limit(2)
                    .toList();

            for (SkillCourseMapping courseMapping : selectedCourses) {
                chosenCourseIds.add(courseMapping.getLmsCourseId());

                String key = skillId + "_" + courseMapping.getLmsCourseId();
                Enrollment enr = enrollmentMap.get(key);

                StepStatus stepStatus = StepStatus.TODO;
                Instant doneAt = null;

                if (enr != null) {
                    if (enr.getStatus() == EnrollmentStatus.COMPLETED) {
                        stepStatus = StepStatus.DONE;
                        doneAt = enr.getCompletedAt() != null ? enr.getCompletedAt() : Instant.now();
                    } else if (enr.getStatus() == EnrollmentStatus.ENROLLED || enr.getStatus() == EnrollmentStatus.IN_PROGRESS) {
                        stepStatus = StepStatus.DOING;
                    }
                }

                LearningPathStep step = LearningPathStep.builder()
                        .stepOrder(stepOrder++)
                        .skillId(skillId)
                        .lmsCourseId(courseMapping.getLmsCourseId())
                        .status(stepStatus)
                        .doneAt(doneAt)
                        .build();

                learningPath.addStep(step);
            }
        }

        LearningPath saved = learningPathRepository.save(learningPath);
        log.info("Sinh lộ trình thành công cho userId={}, pathId={}, tổng số bước={}", userId, saved.getId(), saved.getSteps().size());

        return toLearningPathResponse(saved, gapAnalysis, enrollments);
    }

    @Override
    @Transactional
    public LearningPathResponse getMyLearningPath(UUID userId, String authHeader) {
        log.info("Lấy lộ trình học tập hiện tại cho userId={}", userId);

        Optional<LearningPath> pathOpt = learningPathRepository.findByUserIdAndStatus(userId, LearningPathStatus.ACTIVE);
        if (pathOpt.isEmpty()) {
            return null;
        }

        LearningPath path = pathOpt.get();

        // 1. Tự động đồng bộ trạng thái các bước với các bản ghi Enrollment mới nhất
        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        Map<String, Enrollment> enrollmentMap = enrollments.stream()
                .collect(Collectors.toMap(
                        e -> e.getSkillId() + "_" + e.getLmsCourseId(),
                        Function.identity(),
                        (first, second) -> first
                ));

        boolean updated = false;
        for (LearningPathStep step : path.getSteps()) {
            Enrollment enr = enrollmentMap.get(step.getSkillId() + "_" + step.getLmsCourseId());
            if (enr != null) {
                if (enr.getStatus() == EnrollmentStatus.COMPLETED && step.getStatus() != StepStatus.DONE) {
                    step.setStatus(StepStatus.DONE);
                    step.setDoneAt(enr.getCompletedAt() != null ? enr.getCompletedAt() : Instant.now());
                    updated = true;
                } else if ((enr.getStatus() == EnrollmentStatus.ENROLLED || enr.getStatus() == EnrollmentStatus.IN_PROGRESS)
                        && step.getStatus() == StepStatus.TODO) {
                    step.setStatus(StepStatus.DOING);
                    updated = true;
                }
            }
        }

        if (updated) {
            learningPathRepository.save(path);
        }

        // 2. Lấy Gap analysis mới nhất để cập nhật currentReadiness
        GapAnalysisResponse gapAnalysis = null;
        try {
            gapAnalysis = gapAnalysisService.analyzeGap(userId, null, authHeader);
        } catch (Exception e) {
            log.warn("Không thể lấy GapAnalysis mới nhất cho userId={}: {}", userId, e.getMessage());
        }

        return toLearningPathResponse(path, gapAnalysis, enrollments);
    }

    @Override
    @Transactional
    public void archiveCurrentPath(UUID userId) {
        log.info("Lưu trữ (Archive) lộ trình học tập của userId={}", userId);
        learningPathRepository.updateStatusByUserIdAndCurrentStatus(
                userId,
                LearningPathStatus.ACTIVE,
                LearningPathStatus.ARCHIVED
        );
    }

    @Override
    @Transactional
    public void markStepDoneIfCourseCompleted(UUID userId, String lmsCourseId, String skillId) {
        log.info("Kiểm tra và cập nhật hoàn thành bước học: userId={}, lmsCourseId={}, skillId={}", userId, lmsCourseId, skillId);

        Optional<LearningPath> pathOpt = learningPathRepository.findByUserIdAndStatus(userId, LearningPathStatus.ACTIVE);
        if (pathOpt.isEmpty()) {
            return;
        }

        LearningPath path = pathOpt.get();
        boolean changed = false;

        for (LearningPathStep step : path.getSteps()) {
            boolean matchCourse = lmsCourseId.equalsIgnoreCase(step.getLmsCourseId());
            boolean matchSkill = skillId == null || skillId.equalsIgnoreCase(step.getSkillId());

            if (matchCourse && matchSkill && step.getStatus() != StepStatus.DONE) {
                step.setStatus(StepStatus.DONE);
                step.setDoneAt(Instant.now());
                changed = true;
                log.info("Đã đánh dấu bước order={} thành DONE cho khóa {}", step.getStepOrder(), lmsCourseId);
            }
        }

        if (changed) {
            learningPathRepository.save(path);
        }
    }

    private LearningPathResponse toLearningPathResponse(LearningPath path, GapAnalysisResponse gapAnalysis, List<Enrollment> enrollments) {
        Map<String, Enrollment> enrollmentMap = (enrollments != null ? enrollments : Collections.<Enrollment>emptyList()).stream()
                .collect(Collectors.toMap(
                        e -> e.getSkillId() + "_" + e.getLmsCourseId(),
                        Function.identity(),
                        (first, second) -> first
                ));

        // Bản đồ thông tin kỹ năng từ GapAnalysis
        Map<String, GapItemResponse> gapItemMap = gapAnalysis != null && gapAnalysis.getItems() != null
                ? gapAnalysis.getItems().stream().collect(Collectors.toMap(GapItemResponse::getSkillId, Function.identity(), (a, b) -> a))
                : Collections.emptyMap();

        // Bản đồ thông tin khóa học từ mappings
        List<SkillCourseMapping> allMappings = skillCourseMappingRepository.findAll();
        Map<String, SkillCourseMapping> mappingMap = allMappings.stream()
                .collect(Collectors.toMap(
                        m -> m.getSkillId() + "_" + m.getLmsCourseId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        List<LearningPathStepResponse> stepResponses = new ArrayList<>();
        int doneCount = 0;

        for (LearningPathStep step : path.getSteps()) {
            if (step.getStatus() == StepStatus.DONE) {
                doneCount++;
            }

            String key = step.getSkillId() + "_" + step.getLmsCourseId();
            SkillCourseMapping mapping = mappingMap.get(key);
            GapItemResponse gapItem = gapItemMap.get(step.getSkillId());
            Enrollment enr = enrollmentMap.get(key);

            String courseName = mapping != null ? mapping.getCourseName() : step.getLmsCourseId();
            String lessonHint = mapping != null ? mapping.getLessonHint() : null;
            String skillName = gapItem != null ? gapItem.getSkillName() : step.getSkillId();
            Integer requiredScore = gapItem != null ? gapItem.getRequired() : null;
            Integer currentScore = gapItem != null ? gapItem.getCurrent() : null;
            Integer gap = gapItem != null ? gapItem.getGap() : null;
            Boolean isMandatory = gapItem != null ? gapItem.getIsMandatory() : null;

            BigDecimal progress = enr != null ? enr.getProgress() : null;
            BigDecimal score = enr != null ? enr.getScore() : null;

            stepResponses.add(LearningPathStepResponse.builder()
                    .id(step.getId())
                    .stepOrder(step.getStepOrder())
                    .skillId(step.getSkillId())
                    .skillName(skillName)
                    .requiredScore(requiredScore)
                    .currentScore(currentScore)
                    .gap(gap)
                    .isMandatory(isMandatory)
                    .lmsCourseId(step.getLmsCourseId())
                    .courseName(courseName)
                    .lessonHint(lessonHint)
                    .status(step.getStatus())
                    .doneAt(step.getDoneAt())
                    .progress(progress)
                    .score(score)
                    .build());
        }

        int totalSteps = path.getSteps().size();
        double completionPercentage = totalSteps > 0
                ? Math.round(((double) doneCount / totalSteps) * 1000.0) / 10.0
                : 100.0;

        Double startReadiness = path.getReadinessAtStart() != null ? path.getReadinessAtStart().doubleValue() : 0.0;
        Double currentReadiness = gapAnalysis != null && gapAnalysis.getReadiness() != null
                ? gapAnalysis.getReadiness()
                : startReadiness;
        Double readinessGrowth = Math.round((currentReadiness - startReadiness) * 10.0) / 10.0;

        String targetName = gapAnalysis != null && gapAnalysis.getTarget() != null
                ? gapAnalysis.getTarget().getName()
                : path.getTargetId();

        return LearningPathResponse.builder()
                .id(path.getId())
                .userId(path.getUserId())
                .targetType(path.getTargetType())
                .targetId(path.getTargetId())
                .targetName(targetName)
                .readinessAtStart(startReadiness)
                .currentReadiness(currentReadiness)
                .readinessGrowth(readinessGrowth)
                .completionPercentage(completionPercentage)
                .totalSteps(totalSteps)
                .completedSteps(doneCount)
                .status(path.getStatus())
                .createdAt(path.getCreatedAt())
                .steps(stepResponses)
                .build();
    }
}

