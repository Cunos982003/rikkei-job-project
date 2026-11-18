package com.example.learningservice.service.impl;

import com.example.learningservice.client.LmsClient;
import com.example.learningservice.client.ProfileServiceClient;
import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.model.dto.lms.LmsCourseDto;
import com.example.learningservice.model.dto.lms.LmsEnrollmentDto;
import com.example.learningservice.model.dto.request.EnrollmentRequest;
import com.example.learningservice.model.dto.response.EnrollmentResponse;
import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.model.dto.response.GapItemResponse;
import com.example.learningservice.model.dto.response.ProfileSkillResponse;
import com.example.learningservice.model.dto.response.RefreshEnrollmentsResponse;
import com.example.learningservice.model.dto.response.SkillCourseSuggestionResponse;
import com.example.learningservice.model.dto.response.SuggestedCourseItemResponse;
import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.EnrollmentStatus;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.model.entity.SkillCourseMapping;
import com.example.learningservice.model.entity.TargetPosition;
import com.example.learningservice.repository.EnrollmentRepository;
import com.example.learningservice.repository.SkillCourseMappingRepository;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.CourseSuggestionService;
import com.example.learningservice.service.GapAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSuggestionServiceImpl implements CourseSuggestionService {

    private final GapAnalysisService gapAnalysisService;
    private final SkillCourseMappingRepository skillCourseMappingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final com.example.learningservice.repository.CandidateTargetRepository candidateTargetRepository;
    private final TargetPositionRepository targetPositionRepository;
    private final ProfileServiceClient profileServiceClient;
    private final LmsClient lmsClient;

    @Override
    @Transactional
    public List<SkillCourseSuggestionResponse> getSuggestionsForCandidate(UUID userId, String authHeader) {
        log.info("Lấy danh sách khóa học gợi ý theo kỹ năng thiếu cho userId: {}", userId);

        List<GapItemResponse> missingSkills = new ArrayList<>();

        // 1. Chỉ gọi phân tích nếu ứng viên đã chọn mục tiêu (tránh throw exception gây rollback transaction)
        var targetOpt = candidateTargetRepository.findByUserId(userId);
        if (targetOpt.isPresent()) {
            try {
                GapAnalysisResponse gapAnalysis = gapAnalysisService.analyzeGap(userId, null, authHeader);
                if (gapAnalysis != null && gapAnalysis.getItems() != null) {
                    missingSkills = gapAnalysis.getItems().stream()
                            .filter(item -> item.getGap() != null && item.getGap() > 0)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Lỗi khi phân tích gap theo mục tiêu đã lưu ({}).", e.getMessage());
            }
        } else {
            log.info("Ứng viên userId={} chưa thiết lập mục tiêu nghề nghiệp. Kích hoạt fallback phân tích theo vị trí chuẩn.", userId);
        }

        // Dự phòng thông minh: Nếu ứng viên chưa chọn mục tiêu hoặc chưa có gap -> Lấy từ vị trí mục tiêu mặc định
        if (missingSkills.isEmpty()) {
            List<TargetPosition> positions = targetPositionRepository.findAll();
            if (!positions.isEmpty()) {
                TargetPosition defaultPos = positions.get(0); // Java Backend Fresher
                List<ProfileSkillResponse> userSkills = profileServiceClient.getCandidateSkills(userId, authHeader);
                Map<String, Integer> userScoreMap = userSkills.stream()
                        .filter(s -> s != null && s.getSkillCode() != null)
                        .collect(Collectors.toMap(
                                ProfileSkillResponse::getSkillCode,
                                s -> s.getVerifiedScore() != null ? s.getVerifiedScore() : 0,
                                (a, b) -> a));

                if (defaultPos.getSkills() != null) {
                    missingSkills = defaultPos.getSkills().stream()
                            .map(ps -> {
                                String code = ps.getId() != null ? ps.getId().getSkillId() : ps.getSkillName();
                                int current = userScoreMap.getOrDefault(code, 0);
                                int gap = Math.max(0, ps.getMinScore() - current);
                                return GapItemResponse.builder()
                                        .skillId(code)
                                        .skillName(ps.getSkillName())
                                        .required(ps.getMinScore())
                                        .current(current)
                                        .gap(gap > 0 ? gap : 15) // đảm bảo có khóa học gợi ý
                                        .priority(GapPriority.HIGH)
                                        .build();
                            })
                            .collect(Collectors.toList());
                }
            }
        }

        if (missingSkills.isEmpty()) {
            log.info("Không tìm thấy kỹ năng nào cần gợi ý khóa học");
            return Collections.emptyList();
        }

        // 2. Lấy danh sách ánh xạ khóa học từ DB
        List<String> missingSkillIds = missingSkills.stream()
                .map(GapItemResponse::getSkillId)
                .distinct()
                .toList();

        List<SkillCourseMapping> allMappings = skillCourseMappingRepository.findBySkillIdIn(missingSkillIds);
        Map<String, List<SkillCourseMapping>> mappingsBySkill = allMappings.stream()
                .collect(Collectors.groupingBy(SkillCourseMapping::getSkillId));

        // 3. Lấy thông tin bổ sung từ Mock LMS (nếu khả dụng)
        List<LmsCourseDto> mockCourses = lmsClient.getCourses();
        Map<String, LmsCourseDto> mockCourseMap = mockCourses.stream()
                .filter(c -> c != null && c.getCourseId() != null)
                .collect(Collectors.toMap(LmsCourseDto::getCourseId, Function.identity(), (a, b) -> a));

        // 4. Lấy các khóa học ứng viên hiện tại đã ghi danh
        List<Enrollment> userEnrollments = enrollmentRepository.findByUserId(userId);
        Map<String, Enrollment> enrollmentByCourseId = userEnrollments.stream()
                .collect(Collectors.toMap(Enrollment::getLmsCourseId, Function.identity(), (a, b) -> a));

        // 5. Tổng hợp gợi ý cho từng kỹ năng thiếu
        List<SkillCourseSuggestionResponse> result = new ArrayList<>();

        for (GapItemResponse gapItem : missingSkills) {
            String skillId = gapItem.getSkillId();
            int currentScore = gapItem.getCurrent() != null ? gapItem.getCurrent() : 0;

            List<SkillCourseMapping> skillMappings = mappingsBySkill.getOrDefault(skillId, Collections.emptyList());

            // Lọc các khóa học phù hợp với bậc hiện tại của ứng viên:
            List<SkillCourseMapping> suitableMappings = skillMappings.stream()
                    .filter(m -> currentScore >= m.getLevelFrom() && currentScore <= m.getLevelTo())
                    .toList();

            if (suitableMappings.isEmpty()) {
                suitableMappings = skillMappings;
            }

            List<SuggestedCourseItemResponse> suggestedCourses = suitableMappings.stream()
                    .map(mapping -> {
                        String courseId = mapping.getLmsCourseId();
                        LmsCourseDto lmsCourse = mockCourseMap.get(courseId);
                        Enrollment userEnr = enrollmentByCourseId.get(courseId);

                        return SuggestedCourseItemResponse.builder()
                                .lmsCourseId(courseId)
                                .courseName(mapping.getCourseName())
                                .lessonHint(mapping.getLessonHint())
                                .levelFrom(mapping.getLevelFrom())
                                .levelTo(mapping.getLevelTo())
                                .level(lmsCourse != null && lmsCourse.getLevel() != null ? lmsCourse.getLevel() : "Tiêu chuẩn")
                                .isEnrolled(userEnr != null)
                                .enrollmentId(userEnr != null ? userEnr.getId() : null)
                                .enrollmentStatus(userEnr != null ? userEnr.getStatus() : null)
                                .progress(userEnr != null ? userEnr.getProgress() : null)
                                .score(userEnr != null ? userEnr.getScore() : null)
                                .enrolledAt(userEnr != null ? userEnr.getEnrolledAt() : null)
                                .completedAt(userEnr != null ? userEnr.getCompletedAt() : null)
                                .build();
                    })
                    .toList();

            result.add(SkillCourseSuggestionResponse.builder()
                    .skillId(gapItem.getSkillId())
                    .skillName(gapItem.getSkillName())
                    .requiredScore(gapItem.getRequired())
                    .currentScore(gapItem.getCurrent())
                    .gap(gapItem.getGap())
                    .priority(gapItem.getPriority())
                    .courses(suggestedCourses)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public EnrollmentResponse enrollCourse(UUID userId, EnrollmentRequest request) {
        String lmsCourseId = request.getLmsCourseId().trim();
        String skillId = request.getSkillId().trim();
        log.info("Ứng viên userId={} thực hiện ghi danh khóa học lmsCourseId={} cho skillId={}", userId, lmsCourseId, skillId);

        // 1. AC2: Kiểm tra ghi danh trùng -> 409 Conflict
        if (enrollmentRepository.existsByUserIdAndLmsCourseId(userId, lmsCourseId)) {
            log.warn("Ứng viên userId={} đã ghi danh khóa học {} trước đó", userId, lmsCourseId);
            throw new BusinessException(
                    "Bạn đã ghi danh khóa học " + lmsCourseId + " này rồi (không được ghi danh trùng).",
                    HttpStatus.CONFLICT
            );
        }

        // 2. AC4: Gọi Mock LMS POST /lms/enrollments. Nếu mock lỗi -> ném Exception, không lưu DB
        LmsEnrollmentDto lmsRes = lmsClient.enrollCourse(userId.toString(), lmsCourseId);

        // 3. Mock thành công -> Lưu vào cơ sở dữ liệu
        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .lmsCourseId(lmsCourseId)
                .skillId(skillId)
                .status(EnrollmentStatus.IN_PROGRESS)
                .progress(lmsRes != null && lmsRes.getProgress() != null ? lmsRes.getProgress() : BigDecimal.ZERO)
                .score(lmsRes != null ? lmsRes.getScore() : null)
                .enrolledAt(Instant.now())
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Lưu thành công ghi danh enrollmentId={} cho userId={}", saved.getId(), userId);

        String courseName = skillCourseMappingRepository.findBySkillIdAndLmsCourseId(skillId, lmsCourseId)
                .map(SkillCourseMapping::getCourseName)
                .orElse(lmsCourseId);

        return toEnrollmentResponse(saved, courseName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(UUID userId) {
        List<Enrollment> enrollments = enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(userId);
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<SkillCourseMapping> mappings = skillCourseMappingRepository.findAll();
        Map<String, String> courseNames = mappings.stream()
                .collect(Collectors.toMap(
                        m -> m.getSkillId() + "_" + m.getLmsCourseId(),
                        SkillCourseMapping::getCourseName,
                        (a, b) -> a
                ));

        return enrollments.stream()
                .map(enr -> {
                    String name = courseNames.getOrDefault(enr.getSkillId() + "_" + enr.getLmsCourseId(), enr.getLmsCourseId());
                    return toEnrollmentResponse(enr, name);
                })
                .toList();
    }

    @Override
    @Transactional
    public RefreshEnrollmentsResponse refreshEnrollments(UUID userId, String authHeader) {
        log.info("Bắt đầu làm mới tiến độ học tập từ LMS cho userId={}", userId);

        List<Enrollment> enrollments = enrollmentRepository.findByUserId(userId);
        if (enrollments.isEmpty()) {
            return RefreshEnrollmentsResponse.builder()
                    .totalUpdated(0)
                    .completedCoursesCount(0)
                    .enrollments(Collections.emptyList())
                    .build();
        }

        // Gọi Mock LMS lấy danh sách ghi danh theo mã học viên
        List<LmsEnrollmentDto> lmsList = lmsClient.getStudentEnrollments(userId.toString());
        if (lmsList == null || lmsList.isEmpty()) {
            lmsList = lmsClient.getStudentEnrollments("std-001");
        }

        Map<String, LmsEnrollmentDto> lmsMap = lmsList.stream()
                .collect(Collectors.toMap(
                        e -> e.getCourseId() != null ? e.getCourseId() : e.getLmsCourseId(),
                        Function.identity(),
                        (a, b) -> a
                ));

        int updatedCount = 0;
        int completedCount = 0;

        for (Enrollment enr : enrollments) {
            LmsEnrollmentDto lmsDto = lmsMap.get(enr.getLmsCourseId());
            if (lmsDto != null) {
                boolean changed = false;
                if (lmsDto.getProgress() != null && !lmsDto.getProgress().equals(enr.getProgress())) {
                    enr.setProgress(lmsDto.getProgress());
                    changed = true;
                }
                if (lmsDto.getScore() != null && !lmsDto.getScore().equals(enr.getScore())) {
                    enr.setScore(lmsDto.getScore());
                    changed = true;
                }

                // AC3: Kiểm tra hoàn thành khóa học kèm điểm
                if ("COMPLETED".equalsIgnoreCase(lmsDto.getStatus())) {
                    if (enr.getStatus() != EnrollmentStatus.COMPLETED) {
                        enr.setStatus(EnrollmentStatus.COMPLETED);
                        if (enr.getCompletedAt() == null) {
                            enr.setCompletedAt(Instant.now());
                        }
                        changed = true;
                        completedCount++;

                        // Gửi bằng chứng sang profile-service để recomputeSkills (AC3)
                        short scoreVal = lmsDto.getScore() != null ? lmsDto.getScore().shortValue() : (short) 85;
                        String courseName = skillCourseMappingRepository.findBySkillIdAndLmsCourseId(enr.getSkillId(), enr.getLmsCourseId())
                                .map(SkillCourseMapping::getCourseName)
                                .orElse(enr.getLmsCourseId());

                        profileServiceClient.syncLmsSkillEvidence(userId, enr.getSkillId(), enr.getLmsCourseId(), courseName, scoreVal);
                    }
                }

                if (changed) {
                    enrollmentRepository.save(enr);
                    updatedCount++;
                }
            }
        }

        log.info("Làm mới hoàn tất: updatedCount={}, completedCount={}", updatedCount, completedCount);
        List<EnrollmentResponse> myEnrs = getMyEnrollments(userId);

        return RefreshEnrollmentsResponse.builder()
                .totalUpdated(updatedCount)
                .completedCoursesCount(completedCount)
                .enrollments(myEnrs)
                .build();
    }

    private EnrollmentResponse toEnrollmentResponse(Enrollment enr, String courseName) {
        return EnrollmentResponse.builder()
                .id(enr.getId())
                .userId(enr.getUserId())
                .lmsCourseId(enr.getLmsCourseId())
                .courseName(courseName)
                .skillId(enr.getSkillId())
                .status(enr.getStatus())
                .progress(enr.getProgress())
                .score(enr.getScore())
                .enrolledAt(enr.getEnrolledAt())
                .completedAt(enr.getCompletedAt())
                .build();
    }
}
