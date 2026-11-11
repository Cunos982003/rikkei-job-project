package com.example.profileservice.skill.application;

import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.domain.SkillEvidence;
import com.example.profileservice.skill.domain.SkillMapping;
import com.example.profileservice.skill.domain.SkillScore;
import com.example.profileservice.skill.domain.SkillScoreId;
import com.example.profileservice.skill.infrastructure.SkillEvidenceRepository;
import com.example.profileservice.skill.infrastructure.SkillMappingRepository;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import com.example.profileservice.skill.infrastructure.SkillScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillRecomputeService {

    private static final int DECAY_THRESHOLD_DAYS = 365;
    private static final BigDecimal DECAY_FACTOR = BigDecimal.valueOf(0.5);

    private final SkillMappingRepository mappingRepo;
    private final SkillEvidenceRepository evidenceRepo;
    private final SkillScoreRepository scoreRepo;
    private final SkillRepository skillRepo;

    public SkillRecomputeService(
            SkillMappingRepository mappingRepo,
            SkillEvidenceRepository evidenceRepo,
            SkillScoreRepository scoreRepo,
            SkillRepository skillRepo) {
        this.mappingRepo = mappingRepo;
        this.evidenceRepo = evidenceRepo;
        this.scoreRepo = scoreRepo;
        this.skillRepo = skillRepo;
    }

    public void recomputeSkills(UUID userId, List<ExternalRecordDto> externalRecords) {
        // For each external record, find mappings and create evidences
        for (ExternalRecordDto record : externalRecords) {
            List<SkillMapping> mappings = mappingRepo.findBySourceAndSourceCode(record.source(), record.sourceCode());
            for (SkillMapping mapping : mappings) {
                createOrUpdateEvidence(userId, mapping, record);
            }
        }

        // Recalculate scores for all skills that have ANY evidences for this user
        List<SkillEvidence> allEvidences = evidenceRepo.findByUserId(userId);
        Map<UUID, List<SkillEvidence>> evidencesBySkill = allEvidences.stream()
                .collect(Collectors.groupingBy(e -> e.getSkill().getId()));

        for (Map.Entry<UUID, List<SkillEvidence>> entry : evidencesBySkill.entrySet()) {
            UUID skillId = entry.getKey();
            List<SkillEvidence> evidences = entry.getValue();
            recalculateScore(userId, skillId, evidences);
        }
    }

    private void createOrUpdateEvidence(UUID userId, SkillMapping mapping, ExternalRecordDto record) {
        Skill skill = mapping.getSkill();

        // Use the original external score (not multiplied by mapping weight)
        // The mapping weight will be used in future DG-03 feature
        short evidenceScore = record.score();

        // Upsert: delete existing then save new
        evidenceRepo.deleteByUserSkillSourceRef(userId, skill.getId(), record.source(), record.sourceCode());

        SkillEvidence evidence = SkillEvidence.builder()
                .userId(userId)
                .skill(skill)
                .source(record.source())
                .sourceRef(record.sourceCode())
                .title(record.title())
                .score(evidenceScore)
                .recordedAt(record.recordedAt())
                .verified(true)
                .build();
        evidenceRepo.save(evidence);
    }

    private void recalculateScore(UUID userId, UUID skillId, List<SkillEvidence> evidences) {
        // Separate verified and self-reported evidences
        List<SkillEvidence> verified = evidences.stream().filter(SkillEvidence::isVerified).toList();
        List<SkillEvidence> self = evidences.stream().filter(e -> !e.isVerified()).toList();

        // Calculate verified_score: average of (score * time_decay) for each verified evidence
        // Time decay: 1.0 within 1 year (365 days), 0.5 for older
        Short verifiedScore = null;
        if (!verified.isEmpty()) {
            BigDecimal totalDecayedScore = BigDecimal.ZERO;
            for (SkillEvidence evidence : verified) {
                long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(evidence.getRecordedAt().toLocalDate(), OffsetDateTime.now().toLocalDate());
                BigDecimal decay = daysDiff <= DECAY_THRESHOLD_DAYS ? BigDecimal.ONE : DECAY_FACTOR;
                totalDecayedScore = totalDecayedScore.add(BigDecimal.valueOf(evidence.getScore()).multiply(decay));
            }
            BigDecimal avg = totalDecayedScore.divide(BigDecimal.valueOf(verified.size()), 0, RoundingMode.HALF_UP);
            verifiedScore = avg.shortValueExact();
        }

        // Calculate self_score (simple average of self-reported)
        Short selfScore = null;
        if (!self.isEmpty()) {
            double avg = self.stream().mapToInt(SkillEvidence::getScore).average().orElse(0);
            selfScore = (short) Math.round(avg);
        }

        // Find or create SkillScore
        SkillScoreId scoreId = new SkillScoreId(userId, skillId);
        SkillScore score = scoreRepo.findById(scoreId).orElse(SkillScore.builder()
                .userId(userId)
                .skillId(skillId)
                .build());

        score.setVerifiedScore(verifiedScore);
        score.setSelfScore(selfScore);
        score.setUpdatedAt(OffsetDateTime.now());

        // Determine level based on verifiedScore (or selfScore if no verified)
        Short effectiveScore = verifiedScore != null ? verifiedScore : selfScore;
        String level = calculateLevel(effectiveScore);
        score.setLevel(level);

        scoreRepo.save(score);
    }

    private String calculateLevel(Short score) {
        if (score == null) {
            return "Cơ bản";
        }
        if (score >= 85) {
            return "Nâng cao";
        } else if (score >= 70) {
            return "Vững";
        } else if (score >= 50) {
            return "Khá";
        } else {
            return "Cơ bản";
        }
    }
}