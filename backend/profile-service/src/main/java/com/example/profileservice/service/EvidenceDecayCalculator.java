package com.example.profileservice.service;

import com.example.profileservice.common.domain.Score;
import com.example.profileservice.entity.SkillEvidence;
import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service for calculating evidence decay averages.
 * Encapsulates the time-decay calculation logic for verified skill evidences.
 * Extracted from SkillRecomputeService as part of Phase 5 refactor (Task 5.1).
 */
public class EvidenceDecayCalculator {

    /**
     * Calculate the decayed average score from a list of verified evidences.
     * Uses SkillEvidence.getDecayedScore() which applies time decay:
     * - Within 1 year (365 days): full score (decay factor 1.0)
     * - Older than 1 year: half score (decay factor 0.5)
     *
     * @param verifiedEvidences list of verified SkillEvidence (non-verified are ignored)
     * @return Score representing the decayed average, or Score.ZERO if no verified evidences
     */
    public Score calculateDecayedAverage(List<SkillEvidence> verifiedEvidences) {
        if (verifiedEvidences == null || verifiedEvidences.isEmpty()) {
            return Score.ZERO;
        }

        BigDecimal totalDecayedScore = BigDecimal.ZERO;
        for (SkillEvidence evidence : verifiedEvidences) {
            totalDecayedScore = totalDecayedScore.add(evidence.getDecayedScore());
        }

        BigDecimal avg = totalDecayedScore.divide(
                BigDecimal.valueOf(verifiedEvidences.size()),
                0,
                BigDecimal.ROUND_HALF_UP
        );

        return Score.of(avg.shortValueExact());
    }
}