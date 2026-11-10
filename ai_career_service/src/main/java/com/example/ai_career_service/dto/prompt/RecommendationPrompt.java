package com.example.ai_career_service.dto.prompt;

import java.util.List;
import java.util.stream.Collectors;

public record RecommendationPrompt(
        List<SkillComparison> matchedSkills,
        List<SkillComparison> missingSkills) {

    public String toUserPrompt() {
        return """
                Hãy giải thích bằng tiếng Việt vì sao ứng viên phù hợp và còn thiếu gì.
                Chỉ dùng dữ liệu kỹ năng dưới đây, không suy đoán; tối đa 60 từ.
                Kỹ năng khớp: %s
                Kỹ năng còn thiếu: %s
                """.formatted(format(matchedSkills), format(missingSkills));
    }

    private String format(List<SkillComparison> skills) {
        if (skills.isEmpty()) {
            return "không có";
        }
        return skills.stream()
                .map(skill -> "%s (chỉ số đã xác thực: %s; mức JD yêu cầu: %d)"
                        .formatted(
                                skill.name(),
                                skill.verifiedIndex() == null ? "chưa có" : skill.verifiedIndex(),
                                skill.requiredIndex()))
                .collect(Collectors.joining(", "));
    }

    public record SkillComparison(String name, Integer verifiedIndex, int requiredIndex) {
    }
}
