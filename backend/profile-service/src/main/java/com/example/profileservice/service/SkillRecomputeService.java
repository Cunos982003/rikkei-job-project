package com.example.profileservice.service;

import com.example.profileservice.common.domain.UserId;
import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import com.example.profileservice.entity.Skill;
import com.example.profileservice.entity.SkillEvidence;
import com.example.profileservice.entity.SkillMapping;
import com.example.profileservice.entity.SkillScore;
import com.example.profileservice.entity.SkillScoreId;
import com.example.profileservice.repository.SkillEvidenceRepository;
import com.example.profileservice.repository.SkillMappingRepository;
import com.example.profileservice.repository.SkillRepository;
import com.example.profileservice.repository.SkillScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkillRecomputeService {

    private final SkillMappingRepository mappingRepo;
    private final SkillEvidenceRepository evidenceRepo;
    private final SkillScoreRepository scoreRepo;
    private final SkillRepository skillRepo;
    private final EvidenceDecayCalculator decayCalculator;

    public SkillRecomputeService(
            SkillMappingRepository mappingRepo,
            SkillEvidenceRepository evidenceRepo,
            SkillScoreRepository scoreRepo,
            SkillRepository skillRepo,
            EvidenceDecayCalculator decayCalculator) {
        this.mappingRepo = mappingRepo;
        this.evidenceRepo = evidenceRepo;
        this.scoreRepo = scoreRepo;
        this.skillRepo = skillRepo;
        this.decayCalculator = decayCalculator;
    }

    public void recomputeSkills(UUID userId, List<ExternalRecordDto> externalRecords) {
        recomputeSkills(UserId.of(userId), externalRecords);
    }

    public void recomputeSkills(UserId userId, List<ExternalRecordDto> externalRecords) {
        // For each external record, find mappings and create evidences
        for (ExternalRecordDto record : externalRecords) {
            List<SkillMapping> mappings = mappingRepo.findBySourceAndSourceCode(record.source(), record.sourceCode());
            for (SkillMapping mapping : mappings) {
                createOrUpdateEvidence(userId, mapping, record);
            }
        }

        // Recalculate scores for all skills that have ANY evidences for this user
        List<SkillEvidence> allEvidences = evidenceRepo.findByUserId(userId.value());
        Map<UUID, List<SkillEvidence>> evidencesBySkill = allEvidences.stream()
                .collect(Collectors.groupingBy(e -> e.getSkill().getId()));

        for (Map.Entry<UUID, List<SkillEvidence>> entry : evidencesBySkill.entrySet()) {
            UUID skillId = entry.getKey();
            List<SkillEvidence> evidences = entry.getValue();
            recalculateScore(userId, skillId, evidences);
        }
    }

    private void createOrUpdateEvidence(UserId userId, SkillMapping mapping, ExternalRecordDto record) {
        Skill skill = mapping.getSkill();

        // Use the original external score (not multiplied by mapping weight)
        // The mapping weight will be used in future DG-03 feature
        short evidenceScore = record.score();

        // Upsert: delete existing then save new
        evidenceRepo.deleteByUserSkillSourceRef(userId.value(), skill.getId(), record.source(), record.sourceCode());

        SkillEvidence evidence = SkillEvidence.builder()
                .userId(userId.value())
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

    private void recalculateScore(UserId userId, UUID skillId, List<SkillEvidence> evidences) {
        // Delegate to domain: SkillScore.recalculate() handles verified/self separation,
        // decay calculation (via SkillEvidence.getDecayedScore()), and level determination (via Level.fromScore())
        SkillScoreId scoreId = new SkillScoreId(userId.value(), skillId);
        SkillScore score = scoreRepo.findById(scoreId).orElse(SkillScore.builder()
                .userId(userId.value())
                .skillId(skillId)
                .build());

        // Use domain method to recalculate everything from evidences
        score.recalculate(evidences);
        scoreRepo.save(score);
    }
}