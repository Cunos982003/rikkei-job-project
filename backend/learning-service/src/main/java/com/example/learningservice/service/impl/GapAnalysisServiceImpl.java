package com.example.learningservice.service.impl;

import com.example.learningservice.client.JobServiceClient;
import com.example.learningservice.client.ProfileServiceClient;
import com.example.learningservice.exception.BusinessException;
import com.example.learningservice.exception.ResourceNotFoundException;
import com.example.learningservice.model.dto.response.GapAnalysisResponse;
import com.example.learningservice.model.dto.response.GapItemResponse;
import com.example.learningservice.model.dto.response.JobDetailResponse;
import com.example.learningservice.model.dto.response.JobSkillResponse;
import com.example.learningservice.model.dto.response.ProfileSkillResponse;
import com.example.learningservice.model.dto.response.TargetInfoResponse;
import com.example.learningservice.model.entity.CandidateTarget;
import com.example.learningservice.model.entity.GapPriority;
import com.example.learningservice.model.entity.GapResult;
import com.example.learningservice.model.entity.PositionSkill;
import com.example.learningservice.model.entity.TargetPosition;
import com.example.learningservice.model.entity.TargetType;
import com.example.learningservice.repository.CandidateTargetRepository;
import com.example.learningservice.repository.GapResultRepository;
import com.example.learningservice.repository.PositionSkillRepository;
import com.example.learningservice.repository.TargetPositionRepository;
import com.example.learningservice.service.GapAnalysisService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GapAnalysisServiceImpl implements GapAnalysisService {

    private final CandidateTargetRepository candidateTargetRepository;
    private final TargetPositionRepository targetPositionRepository;
    private final PositionSkillRepository positionSkillRepository;
    private final GapResultRepository gapResultRepository;
    private final ProfileServiceClient profileServiceClient;
    private final JobServiceClient jobServiceClient;

    @Override
    @Transactional
    public GapAnalysisResponse analyzeGap(UUID userId, String jobIdOverride, String authHeader) {
        log.info("Bắt đầu phân tích khoảng thiếu hụt năng lực cho userId: {}, jobIdOverride: {}", userId, jobIdOverride);

        TargetContext targetContext = resolveTarget(userId, jobIdOverride);

        List<ProfileSkillResponse> candidateSkills = profileServiceClient.getCandidateSkills(userId, authHeader);
        if (candidateSkills == null) {
            candidateSkills = Collections.emptyList();
        }

        List<GapItemResponse> missingItems = new ArrayList<>();
        int totalRequiredSkills = targetContext.getRequiredSkills().size();
        int passedSkillsCount = 0;
        int missingSkillsCount = 0;
        Instant now = Instant.now();

        List<GapResult> gapResultEntities = new ArrayList<>();

        for (SkillRequirement req : targetContext.getRequiredSkills()) {
            Optional<ProfileSkillResponse> matchedSkill = findMatchingCandidateSkill(candidateSkills, req);

            int currentScore = matchedSkill
                    .map(ProfileSkillResponse::getVerifiedScore)
                    .orElse(0);

            if (currentScore < 0) {
                currentScore = 0;
            }

            int requiredScore = req.getMinScore();
            int gap = requiredScore - currentScore;

            if (gap <= 0) {
                passedSkillsCount++;
            } else {
                missingSkillsCount++;
                GapPriority priority = calculatePriority(req.getIsMandatory(), gap);

                GapItemResponse item = GapItemResponse.builder()
                        .skillId(req.getSkillId())
                        .skill(req.getSkillName())
                        .skillName(req.getSkillName())
                        .required(requiredScore)
                        .current(currentScore)
                        .gap(gap)
                        .priority(priority)
                        .isMandatory(req.getIsMandatory())
                        .build();

                missingItems.add(item);

                gapResultEntities.add(GapResult.builder()
                        .userId(userId)
                        .targetType(targetContext.getTargetType())
                        .targetId(targetContext.getTargetId())
                        .skillId(req.getSkillId())
                        .skillName(req.getSkillName())
                        .required(requiredScore)
                        .current(currentScore)
                        .gap(gap)
                        .priority(priority)
                        .computedAt(now)
                        .build());
            }
        }

        missingItems.sort((a, b) -> Integer.compare(b.getGap(), a.getGap()));

        double readiness = 0.0;
        if (totalRequiredSkills > 0) {
            readiness = Math.round(((double) passedSkillsCount / totalRequiredSkills) * 1000.0) / 10.0;
        }

        gapResultRepository.deleteByUserIdAndTargetId(userId, targetContext.getTargetId());
        if (!gapResultEntities.isEmpty()) {
            gapResultRepository.saveAll(gapResultEntities);
        }

        return GapAnalysisResponse.builder()
                .target(targetContext.getTargetInfo())
                .readiness(readiness)
                .totalRequiredSkills(totalRequiredSkills)
                .passedSkillsCount(passedSkillsCount)
                .missingSkillsCount(missingSkillsCount)
                .computedAt(now)
                .items(missingItems)
                .build();
    }

    private TargetContext resolveTarget(UUID userId, String jobIdOverride) {
        if (jobIdOverride != null && !jobIdOverride.trim().isEmpty()) {
            String jobId = jobIdOverride.trim();
            JobDetailResponse job = jobServiceClient.getJobById(jobId);
            if (job == null) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với mã: " + jobId);
            }

            List<SkillRequirement> requirements = new ArrayList<>();
            if (job.getJobSkills() != null && !job.getJobSkills().isEmpty()) {
                requirements = job.getJobSkills().stream()
                        .map(js -> SkillRequirement.builder()
                                .skillId(js.getSkillId())
                                .skillName(js.getSkillName())
                                .minScore(js.getMinScore() != null ? js.getMinScore() : 70)
                                .isMandatory(js.getIsMandatory() != null ? js.getIsMandatory() : true)
                                .build())
                        .collect(Collectors.toList());
            }

            TargetInfoResponse targetInfo = TargetInfoResponse.builder()
                    .type(TargetType.JOB)
                    .id(job.getId())
                    .code(job.getId())
                    .name(job.getTitle())
                    .description(job.getCompanyName() != null ? "Doanh nghiệp: " + job.getCompanyName() : "Tin tuyển dụng")
                    .build();

            return TargetContext.builder()
                    .targetType(TargetType.JOB)
                    .targetId(job.getId())
                    .targetInfo(targetInfo)
                    .requiredSkills(requirements)
                    .build();
        }

        CandidateTarget candidateTarget = candidateTargetRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Bạn chưa chọn mục tiêu nghề nghiệp. Vui lòng chọn vị trí mục tiêu hoặc JD trước khi phân tích khoảng thiếu hụt."));

        if (candidateTarget.getTargetType() == TargetType.POSITION) {
            UUID positionId = candidateTarget.getPositionId();
            TargetPosition position = targetPositionRepository.findWithSkillsById(positionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vị trí mục tiêu với ID: " + positionId));

            List<SkillRequirement> requirements = position.getSkills() != null
                    ? position.getSkills().stream()
                    .map(ps -> SkillRequirement.builder()
                            .skillId(ps.getId() != null ? ps.getId().getSkillId() : ps.getSkillName())
                            .skillName(ps.getSkillName())
                            .minScore(ps.getMinScore())
                            .isMandatory(ps.getIsMandatory())
                            .build())
                    .collect(Collectors.toList())
                    : Collections.emptyList();

            TargetInfoResponse targetInfo = TargetInfoResponse.builder()
                    .type(TargetType.POSITION)
                    .id(position.getId().toString())
                    .code(position.getCode())
                    .name(position.getName())
                    .description(position.getDescription())
                    .build();

            return TargetContext.builder()
                    .targetType(TargetType.POSITION)
                    .targetId(position.getId().toString())
                    .targetInfo(targetInfo)
                    .requiredSkills(requirements)
                    .build();
        } else {
            String jobId = candidateTarget.getJobId();
            JobDetailResponse job = jobServiceClient.getJobById(jobId);
            if (job == null) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng với mã: " + jobId);
            }

            List<SkillRequirement> requirements = job.getJobSkills() != null
                    ? job.getJobSkills().stream()
                    .map(js -> SkillRequirement.builder()
                            .skillId(js.getSkillId())
                            .skillName(js.getSkillName())
                            .minScore(js.getMinScore() != null ? js.getMinScore() : 70)
                            .isMandatory(js.getIsMandatory() != null ? js.getIsMandatory() : true)
                            .build())
                    .collect(Collectors.toList())
                    : Collections.emptyList();

            TargetInfoResponse targetInfo = TargetInfoResponse.builder()
                    .type(TargetType.JOB)
                    .id(job.getId())
                    .code(job.getId())
                    .name(job.getTitle())
                    .description(job.getCompanyName() != null ? "Doanh nghiệp: " + job.getCompanyName() : "Tin tuyển dụng")
                    .build();

            return TargetContext.builder()
                    .targetType(TargetType.JOB)
                    .targetId(job.getId())
                    .targetInfo(targetInfo)
                    .requiredSkills(requirements)
                    .build();
        }
    }

    private Optional<ProfileSkillResponse> findMatchingCandidateSkill(List<ProfileSkillResponse> candidateSkills, SkillRequirement req) {
        return candidateSkills.stream()
                .filter(cs -> isSkillMatch(cs, req))
                .findFirst();
    }

    private boolean isSkillMatch(ProfileSkillResponse cs, SkillRequirement req) {
        if (cs == null) return false;

        String reqId = req.getSkillId();
        String reqName = req.getSkillName();

        if (reqId != null) {
            if (reqId.equalsIgnoreCase(cs.getSkillId()) || reqId.equalsIgnoreCase(cs.getSkillCode())) {
                return true;
            }
        }

        if (reqName != null) {
            if (reqName.equalsIgnoreCase(cs.getSkillName()) || reqName.equalsIgnoreCase(cs.getSkillCode())) {
                return true;
            }

            String normReq = normalize(reqName);
            String normCandidateName = normalize(cs.getSkillName());
            String normCandidateCode = normalize(cs.getSkillCode());

            if (!normReq.isEmpty()) {
                if (normReq.equals(normCandidateName) || normReq.equals(normCandidateCode)) {
                    return true;
                }
                if (normCandidateName.startsWith(normReq) || normReq.startsWith(normCandidateName)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String normalize(String str) {
        if (str == null) return "";
        return str.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private GapPriority calculatePriority(Boolean isMandatory, int gap) {
        if (Boolean.TRUE.equals(isMandatory) && gap > 20) {
            return GapPriority.HIGH;
        }
        if (gap > 10) {
            return GapPriority.MEDIUM;
        }
        return GapPriority.LOW;
    }

    @Getter
    @Builder
    private static class TargetContext {
        private TargetType targetType;
        private String targetId;
        private TargetInfoResponse targetInfo;
        private List<SkillRequirement> requiredSkills;
    }

    @Getter
    @Builder
    private static class SkillRequirement {
        private String skillId;
        private String skillName;
        private Integer minScore;
        private Boolean isMandatory;
    }
}

