package com.example.ai_career_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JobMatchScorer {

    private static final BigDecimal REQUIRED_WEIGHT = BigDecimal.valueOf(70);
    private static final BigDecimal PREFERRED_WEIGHT = BigDecimal.valueOf(30);
    private static final BigDecimal FULL_WEIGHT = BigDecimal.valueOf(100);
    private static final int DIVISION_SCALE = 10;

    private JobMatchScorer() {
    }

    public static MatchResult score(
            List<RecommendationSnapshot.VerifiedSkill> verifiedSkills,
            List<RecommendationSnapshot.SkillRequirement> requirements) {
        Map<String, Integer> verifiedIndexes = verifiedIndexes(verifiedSkills);

        List<SkillGap> gaps = new ArrayList<>(requirements.size());
        List<BigDecimal> requiredRatios = new ArrayList<>();
        List<BigDecimal> preferredRatios = new ArrayList<>();
        boolean belowMinimum = false;

        for (RecommendationSnapshot.SkillRequirement requirement : requirements) {
            Integer verifiedIndex = verifiedIndexes.get(normalize(requirement.name()));
            int candidateScore = verifiedIndex == null ? 0 : verifiedIndex;
            int minScore = requirement.requiredIndex();
            int gap = Math.max(0, minScore - candidateScore);

            gaps.add(new SkillGap(
                    requirement.name(),
                    verifiedIndex,
                    minScore,
                    gap,
                    requirement.type()));

            BigDecimal ratio = computeSkillRatio(candidateScore, minScore);
            if (requirement.type() == RecommendationSnapshot.RequirementType.REQUIRED) {
                requiredRatios.add(ratio);
                if (minScore > 0 && candidateScore * 2 < minScore) {
                    belowMinimum = true;
                }
            } else {
                preferredRatios.add(ratio);
            }
        }

        double score = computeWeightedScore(requiredRatios, preferredRatios);

        List<String> missingRequiredSkills = gaps.stream()
                .filter(gap -> gap.type() == RecommendationSnapshot.RequirementType.REQUIRED)
                .filter(gap -> gap.verifiedIndex() == null || gap.verifiedIndex() < gap.requiredIndex())
                .map(SkillGap::name)
                .toList();

        return new MatchResult(score, belowMinimum, missingRequiredSkills, List.copyOf(gaps));
    }

    private static BigDecimal computeSkillRatio(int candidateScore, int minScore) {
        if (minScore <= 0) {
            return BigDecimal.ONE;
        }
        BigDecimal ratio = BigDecimal.valueOf(candidateScore)
                .divide(BigDecimal.valueOf(minScore), DIVISION_SCALE, RoundingMode.HALF_UP);
        return ratio.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : ratio;
    }

    private static double computeWeightedScore(
            List<BigDecimal> requiredRatios,
            List<BigDecimal> preferredRatios) {
        BigDecimal totalScore;
        if (!requiredRatios.isEmpty() && !preferredRatios.isEmpty()) {
            totalScore = average(requiredRatios).multiply(REQUIRED_WEIGHT)
                    .add(average(preferredRatios).multiply(PREFERRED_WEIGHT));
        } else if (!requiredRatios.isEmpty()) {
            totalScore = average(requiredRatios).multiply(FULL_WEIGHT);
        } else if (!preferredRatios.isEmpty()) {
            totalScore = average(preferredRatios).multiply(FULL_WEIGHT);
        } else {
            totalScore = BigDecimal.ZERO;
        }
        return totalScore.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal average(List<BigDecimal> ratios) {
        BigDecimal sum = ratios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(ratios.size()), DIVISION_SCALE, RoundingMode.HALF_UP);
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

    public record MatchResult(
            double score,
            boolean belowMinimum,
            List<String> missingRequiredSkills,
            List<SkillGap> skillGaps) {

        public MatchResult(double score, List<String> missingRequiredSkills, List<SkillGap> skillGaps) {
            this(score, false, missingRequiredSkills, skillGaps);
        }
    }

    public record SkillGap(
            String name,
            Integer verifiedIndex,
            int requiredIndex,
            int gap,
            RecommendationSnapshot.RequirementType type) {
    }
}
