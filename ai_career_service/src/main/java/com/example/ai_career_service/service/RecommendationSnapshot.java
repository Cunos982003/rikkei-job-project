package com.example.ai_career_service.service;

import java.util.List;
import java.util.Objects;

public record RecommendationSnapshot(
        String profileRevision,
        List<VerifiedSkill> verifiedSkills,
        List<EligibleJob> eligibleJobs) {

    public RecommendationSnapshot {
        profileRevision = Objects.requireNonNull(profileRevision, "profileRevision");
        verifiedSkills = List.copyOf(verifiedSkills);
        eligibleJobs = List.copyOf(eligibleJobs);
    }

    public record VerifiedSkill(String name, int index) {
        public VerifiedSkill {
            requireSkillName(name);
            requireIndex(index);
        }
    }

    public record EligibleJob(
            String jobId,
            String jobRevision,
            double dn06Score,
            List<SkillRequirement> requirements) {
        public EligibleJob {
            if (jobId == null || jobId.isBlank()) {
                throw new IllegalArgumentException("jobId must not be blank");
            }
            jobRevision = Objects.requireNonNull(jobRevision, "jobRevision");
            if (!Double.isFinite(dn06Score) || dn06Score < 0 || dn06Score > 100) {
                throw new IllegalArgumentException("DN-06 score must be between 0 and 100");
            }
            requirements = List.copyOf(requirements);
        }
    }

    public record SkillRequirement(String name, int requiredIndex) {
        public SkillRequirement {
            requireSkillName(name);
            requireIndex(requiredIndex);
        }
    }

    private static void requireSkillName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill name must not be blank");
        }
    }

    private static void requireIndex(int index) {
        if (index < 0 || index > 100) {
            throw new IllegalArgumentException("Skill index must be between 0 and 100");
        }
    }
}
