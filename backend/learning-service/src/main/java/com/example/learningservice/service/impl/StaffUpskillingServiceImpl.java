package com.example.learningservice.service.impl;

import com.example.learningservice.model.dto.response.staff.CandidateInfoDto;
import com.example.learningservice.model.dto.response.staff.EnrollmentProgressDto;
import com.example.learningservice.model.dto.response.staff.MissingSkillDto;
import com.example.learningservice.model.dto.response.staff.TopMissingSkillDto;
import com.example.learningservice.model.dto.response.staff.UpskillingCandidateDto;
import com.example.learningservice.model.dto.response.staff.UpskillingSummaryResponse;
import com.example.learningservice.model.entity.CandidateProfile;
import com.example.learningservice.model.entity.CandidateTarget;
import com.example.learningservice.model.entity.Enrollment;
import com.example.learningservice.model.entity.GapResult;
import com.example.learningservice.model.entity.SkillCourseMapping;
import com.example.learningservice.model.entity.TargetPosition;
import com.example.learningservice.repository.CandidateProfileRepository;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.EnrollmentRepository;
import com.example.learningservice.repository.GapResultRepository;
import com.example.learningservice.repository.SkillCourseMappingRepository;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.StaffUpskillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffUpskillingServiceImpl implements StaffUpskillingService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateTargetRepository candidateTargetRepository;
    private final TargetPositionRepository targetPositionRepository;
    private final GapResultRepository gapResultRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SkillCourseMappingRepository skillCourseMappingRepository;

    private static final Map<String, String> STATIC_COURSE_NAMES = Map.ofEntries(
            Map.entry("CRS-01", "Lập trình Spring Boot nâng cao & Microservices"),
            Map.entry("CRS-02", "React 19 & Next.js Fullstack Architecture"),
            Map.entry("CRS-03", "Tối ưu hóa CSDL quan hệ PostgreSQL"),
            Map.entry("CRS-04", "DevOps cơ bản: Docker, CI/CD và Linux"),
            Map.entry("CRS-05", "Kiểm thử tự động & Chất lượng phần mềm"),
            Map.entry("CRS-06", "Phân tích dữ liệu với Python"),
            Map.entry("CRS-07", "Java Core & Cấu trúc dữ liệu giải thuật"),
            Map.entry("CRS-08", "Node.js & Express RESTful API"),
            Map.entry("CRS-09", "Lập trình Go cơ bản & Microservices"),
            Map.entry("CRS-10", "Hệ thống phân tán với Apache Kafka"),
            Map.entry("CRS-11", "HTML5/CSS3 & Web Responsive cơ bản"),
            Map.entry("CRS-12", "JavaScript ES6+ & TypeScript nền tảng"),
            Map.entry("CRS-13", "TypeScript nâng cao & Design Patterns"),
            Map.entry("CRS-14", "Lập trình Frontend với Vue.js 3"),
            Map.entry("CRS-15", "Kubernetes & Điều phối Container"),
            Map.entry("CRS-16", "Điện toán đám mây với AWS Solutions Architect"),
            Map.entry("CRS-17", "Quản trị CSDL MySQL nâng cao"),
            Map.entry("CRS-18", "CSDL NoSQL MongoDB & Redis Caching"),
            Map.entry("CRS-19", "Kỹ năng mềm & Giao tiếp trong dự án CNTT")
    );

    @Override
    public List<UpskillingCandidateDto> getUpskillingList(String courseName, String classCode, String status, String skillId, Integer page, Integer size) {
        List<UpskillingCandidateDto> allFiltered = getFilteredCandidates(courseName, classCode, status, skillId);

        if (page == null || size == null || size <= 0) {
            return allFiltered;
        }

        int total = allFiltered.size();
        int safePage = Math.max(0, page);
        int fromIndex = Math.min(safePage * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        return allFiltered.subList(fromIndex, toIndex);
    }

    @Override
    public long countUpskillingList(String courseName, String classCode, String status, String skillId) {
        return getFilteredCandidates(courseName, classCode, status, skillId).size();
    }

    private List<UpskillingCandidateDto> getFilteredCandidates(String courseName, String classCode, String status, String skillId) {
        boolean hasClassCode = StringUtils.hasText(classCode);
        boolean hasCourseName = StringUtils.hasText(courseName);

        List<CandidateProfile> profiles;
        if (hasClassCode && hasCourseName) {
            profiles = candidateProfileRepository.findByClassCodeAndCourseNameContainingIgnoreCase(classCode.trim(), courseName.trim());
        } else if (hasClassCode) {
            profiles = candidateProfileRepository.findByClassCode(classCode.trim());
        } else if (hasCourseName) {
            profiles = candidateProfileRepository.findByCourseNameContainingIgnoreCase(courseName.trim());
        } else {
            profiles = candidateProfileRepository.findAll();
        }

        if (profiles.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = profiles.stream().map(CandidateProfile::getUserId).toList();

        Map<UUID, CandidateTarget> targetByUser = candidateTargetRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(CandidateTarget::getUserId, Function.identity(), (a, b) -> a));

        Map<UUID, String> positionNameById = targetPositionRepository.findAll().stream()
                .collect(Collectors.toMap(TargetPosition::getId, TargetPosition::getName, (a, b) -> a));

        Map<UUID, List<GapResult>> gapsByUser = gapResultRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(GapResult::getUserId));

        Map<UUID, List<Enrollment>> enrollmentsByUser = enrollmentRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(Enrollment::getUserId));

        Map<String, String> courseNames = new HashMap<>(STATIC_COURSE_NAMES);
        skillCourseMappingRepository.findAll().forEach(m -> {
            if (StringUtils.hasText(m.getLmsCourseId()) && StringUtils.hasText(m.getCourseName())) {
                courseNames.putIfAbsent(m.getLmsCourseId(), m.getCourseName());
            }
        });

        List<UpskillingCandidateDto> result = new ArrayList<>();

        for (CandidateProfile profile : profiles) {
            UUID uid = profile.getUserId();

            // 1. Missing skills
            List<GapResult> userGaps = gapsByUser.getOrDefault(uid, List.of());
            List<MissingSkillDto> missingSkills = userGaps.stream()
                    .filter(g -> g.getGap() != null && g.getGap() > 0)
                    .map(g -> MissingSkillDto.builder()
                            .skillId(g.getSkillId())
                            .skillName(g.getSkillName())
                            .required(g.getRequired())
                            .current(g.getCurrent())
                            .gap(g.getGap())
                            .priority(g.getPriority() != null ? g.getPriority().name() : "MEDIUM")
                            .build())
                    .toList();

            // Lọc theo skillId nếu có
            if (StringUtils.hasText(skillId)) {
                String targetSkill = skillId.trim().toUpperCase();
                boolean matchesSkill = missingSkills.stream()
                        .anyMatch(s -> s.getSkillId() != null && s.getSkillId().equalsIgnoreCase(targetSkill));
                if (!matchesSkill) {
                    continue;
                }
            }

            // 2. Enrollments
            List<Enrollment> userEnrollments = enrollmentsByUser.getOrDefault(uid, List.of());
            List<EnrollmentProgressDto> enrollmentDtos = userEnrollments.stream()
                    .map(e -> EnrollmentProgressDto.builder()
                            .course(courseNames.getOrDefault(e.getLmsCourseId(), "Khóa học " + e.getLmsCourseId()))
                            .progress(e.getProgress() != null ? e.getProgress() : BigDecimal.ZERO)
                            .score(e.getScore())
                            .status(e.getStatus() != null ? e.getStatus().name() : "ENROLLED")
                            .build())
                    .toList();

            // 3. Trạng thái học tập của ứng viên
            String candidateStatus = determineCandidateStatus(userEnrollments, missingSkills);

            // Lọc theo status nếu có
            if (StringUtils.hasText(status)) {
                String filterStatus = status.trim().toUpperCase();
                boolean matchesStatus = candidateStatus.equalsIgnoreCase(filterStatus) ||
                        userEnrollments.stream().anyMatch(e -> e.getStatus() != null && e.getStatus().name().equalsIgnoreCase(filterStatus));
                if (!matchesStatus) {
                    continue;
                }
            }

            // 4. Target title
            String targetTitle = "Chưa thiết lập";
            CandidateTarget target = targetByUser.get(uid);
            if (target != null) {
                if (target.getPositionId() != null && positionNameById.containsKey(target.getPositionId())) {
                    targetTitle = positionNameById.get(target.getPositionId());
                } else if (StringUtils.hasText(target.getJobId())) {
                    targetTitle = "Vị trí tuyển dụng #" + target.getJobId();
                }
            } else if (StringUtils.hasText(profile.getCourseName())) {
                targetTitle = profile.getCourseName();
            }

            // 5. Tỷ lệ sẵn sàng readiness (%)
            Double readiness = calculateReadiness(userGaps, userEnrollments);

            // 6. Build DTO
            UpskillingCandidateDto dto = UpskillingCandidateDto.builder()
                    .candidate(CandidateInfoDto.builder()
                            .userId(profile.getUserId())
                            .fullName(profile.getFullName())
                            .email(profile.getEmail())
                            .phone(profile.getPhone())
                            .build())
                    .classCode(profile.getClassCode())
                    .courseName(profile.getCourseName())
                    .target(targetTitle)
                    .missingSkills(missingSkills)
                    .enrollments(enrollmentDtos)
                    .readiness(readiness)
                    .status(candidateStatus)
                    .build();

            result.add(dto);
        }

        result.sort(Comparator.comparing(dto -> dto.getCandidate().getFullName()));
        return result;
    }

    private String determineCandidateStatus(List<Enrollment> enrollments, List<MissingSkillDto> missingSkills) {
        if (enrollments != null && !enrollments.isEmpty()) {
            boolean allCompleted = enrollments.stream().allMatch(e -> "COMPLETED".equals(e.getStatus() != null ? e.getStatus().name() : ""));
            if (allCompleted) {
                return "COMPLETED";
            }
            boolean anyInProgress = enrollments.stream().anyMatch(e -> "IN_PROGRESS".equals(e.getStatus() != null ? e.getStatus().name() : ""));
            if (anyInProgress) {
                return "IN_PROGRESS";
            }
            boolean anyEnrolled = enrollments.stream().anyMatch(e -> "ENROLLED".equals(e.getStatus() != null ? e.getStatus().name() : ""));
            if (anyEnrolled) {
                return "ENROLLED";
            }
            boolean anyFailed = enrollments.stream().anyMatch(e -> "FAILED".equals(e.getStatus() != null ? e.getStatus().name() : ""));
            if (anyFailed) {
                return "FAILED";
            }
            return "IN_PROGRESS";
        }

        if (missingSkills == null || missingSkills.isEmpty()) {
            return "COMPLETED";
        }
        return "NOT_STARTED";
    }

    private Double calculateReadiness(List<GapResult> gaps, List<Enrollment> enrollments) {
        double baseScore = 0.0;
        int totalSkills = gaps.size();
        if (totalSkills > 0) {
            long passed = gaps.stream().filter(g -> g.getGap() == null || g.getGap() <= 0).count();
            baseScore = ((double) passed / totalSkills) * 100.0;
        } else {
            baseScore = 100.0;
        }

        if (enrollments != null && !enrollments.isEmpty()) {
            double avgProgress = enrollments.stream()
                    .mapToDouble(e -> e.getProgress() != null ? e.getProgress().doubleValue() : 0.0)
                    .average()
                    .orElse(0.0);
            double weighted = (baseScore * 0.5) + (avgProgress * 0.5);
            return Math.min(100.0, Math.max(0.0, Math.round(weighted * 10.0) / 10.0));
        }

        return Math.min(100.0, Math.max(0.0, Math.round(baseScore * 10.0) / 10.0));
    }

    @Override
    public UpskillingSummaryResponse getUpskillingSummary() {
        List<CandidateProfile> allProfiles = candidateProfileRepository.findAll();
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();

        Map<UUID, List<Enrollment>> enrollmentsByUser = allEnrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getUserId));

        long inProgressCount = 0;
        long completedCount = 0;

        for (CandidateProfile profile : allProfiles) {
            List<Enrollment> userEnrolls = enrollmentsByUser.getOrDefault(profile.getUserId(), List.of());
            if (userEnrolls.isEmpty()) {
                continue;
            }

            boolean allDone = userEnrolls.stream().allMatch(e -> "COMPLETED".equals(e.getStatus() != null ? e.getStatus().name() : ""));
            if (allDone) {
                completedCount++;
            } else {
                boolean hasInProgress = userEnrolls.stream().anyMatch(e -> "IN_PROGRESS".equals(e.getStatus() != null ? e.getStatus().name() : ""));
                if (hasInProgress) {
                    inProgressCount++;
                }
            }
        }

        List<Object[]> rawTopSkills = gapResultRepository.findTopMissingSkills();
        long totalProfiles = allProfiles.isEmpty() ? 1 : allProfiles.size();

        List<TopMissingSkillDto> topMissingSkills = rawTopSkills.stream()
                .limit(5)
                .map(row -> {
                    String skillId = (String) row[0];
                    String skillName = (String) row[1];
                    Long missingCount = (Long) row[2];
                    double percentage = Math.round(((double) missingCount / totalProfiles) * 1000.0) / 10.0;
                    return TopMissingSkillDto.builder()
                            .skillId(skillId)
                            .skillName(skillName)
                            .missingCount(missingCount)
                            .percentage(percentage)
                            .build();
                })
                .toList();

        return UpskillingSummaryResponse.builder()
                .inProgressCount(inProgressCount)
                .completedCount(completedCount)
                .topMissingSkills(topMissingSkills)
                .build();
    }
}
