package com.example.ai_career_service.service;

import com.example.ai_career_service.dto.prompt.RecommendationPrompt;
import com.example.ai_career_service.dto.prompt.RecommendationPrompt.SkillComparison;
import java.util.List;

public final class RecommendationExplanation {

    private RecommendationExplanation() {
    }

    public static RecommendationPrompt createPrompt(RecommendationSnapshot snapshot, RecommendationSnapshot.EligibleJob job) {
        var verified = snapshot.verifiedSkills().stream()
                .collect(java.util.stream.Collectors.toMap(
                        skill -> normalize(skill.name()),
                        RecommendationSnapshot.VerifiedSkill::index,
                        Math::max));
        var matched = new java.util.ArrayList<SkillComparison>();
        var missing = new java.util.ArrayList<SkillComparison>();

        for (var requirement : job.requirements()) {
            Integer index = verified.get(normalize(requirement.name()));
            var comparison = new SkillComparison(requirement.name(), index, requirement.requiredIndex());
            if (index != null && index >= requirement.requiredIndex()) {
                matched.add(comparison);
            } else {
                missing.add(comparison);
            }
        }
        return new RecommendationPrompt(List.copyOf(matched), List.copyOf(missing));
    }

    public static String fallback(RecommendationPrompt prompt) {
        String matched = names(prompt.matchedSkills());
        String missing = names(prompt.missingSkills());
        if (prompt.matchedSkills().isEmpty() && prompt.missingSkills().isEmpty()) {
            return "Chưa có dữ liệu kỹ năng đã xác thực để giải thích mức độ phù hợp.";
        }
        return "Phù hợp ở các kỹ năng: %s. Cần bổ sung hoặc nâng mức: %s."
                .formatted(
                        matched.isBlank() ? "chưa có" : matched,
                        missing.isBlank() ? "không có" : missing);
    }

    public static String limitWords(String explanation) {
        String normalized = explanation == null ? "" : explanation.trim().replaceAll("\\s+", " ");
        String[] words = normalized.isEmpty() ? new String[0] : normalized.split(" ");
        if (words.length <= 60) {
            return normalized;
        }
        return String.join(" ", java.util.Arrays.copyOf(words, 60));
    }

    private static String names(List<SkillComparison> skills) {
        return skills.stream().map(SkillComparison::name).distinct().reduce((a, b) -> a + ", " + b).orElse("");
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
