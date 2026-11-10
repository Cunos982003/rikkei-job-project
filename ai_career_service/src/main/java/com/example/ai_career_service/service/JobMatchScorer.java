package com.example.ai_career_service.service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class JobMatchScorer {

    private JobMatchScorer() {
    }

    public static MatchResult score(
            List<RecommendationSnapshot.VerifiedSkill> verifiedSkills,
            List<RecommendationSnapshot.SkillRequirement> requirements) {
        Map<String, Integer> verifiedIndexes = verifiedIndexes(verifiedSkills);

        List<SkillGap> gaps = requirements.stream()
                .map(requirement -> {
                    Integer verifiedIndex = verifiedIndexes.get(normalize(requirement.name()));
                    int gap = Math.max(0, requirement.requiredIndex() - (verifiedIndex == null ? 0 : verifiedIndex));
                    return new SkillGap(
                            requirement.name(),
                            verifiedIndex,
                            requirement.requiredIndex(),
                            gap,
                            requirement.type());
                })
                .toList();

        double score = requirements.isEmpty()
                ? 0
                : requirements.stream()
                        .mapToDouble(requirement -> {
                            Integer verifiedIndex = verifiedIndexes.get(normalize(requirement.name()));
                            if (verifiedIndex == null) {
                                return 0;
                            }
                            if (requirement.requiredIndex() == 0) {
                                return 100;
                            }
                            return Math.min(100, verifiedIndex * 100.0 / requirement.requiredIndex());
                        })
                        .average()
                        .orElse(0);

        List<String> missingRequiredSkills = gaps.stream()
                .filter(gap -> gap.type() == RecommendationSnapshot.RequirementType.REQUIRED)
                .filter(gap -> gap.verifiedIndex() == null || gap.verifiedIndex() < gap.requiredIndex())
                .map(SkillGap::name)
                .toList();

        return new MatchResult(score, missingRequiredSkills, gaps);
    }

    static Map<String, Integer> verifiedIndexes(List<RecommendationSnapshot.VerifiedSkill> verifiedSkills) {
        Map<String, Integer> indexes = new HashMap<>();
        for (RecommendationSnapshot.VerifiedSkill skill : verifiedSkills) {
            String name = normalize(skill.name());
            if (indexes.putIfAbsent(name, skill.index()) != null) {
                throw new IllegalStateException("Duplicate verified skill: " + name);
            }
        }
        return indexes;
    }

    static String normalize(String name) {
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record MatchResult(double score, List<String> missingRequiredSkills, List<SkillGap> skillGaps) {
    }

    public record SkillGap(
            String name,
            Integer verifiedIndex,
            int requiredIndex,
            int gap,
            RecommendationSnapshot.RequirementType type) {
    }
}
