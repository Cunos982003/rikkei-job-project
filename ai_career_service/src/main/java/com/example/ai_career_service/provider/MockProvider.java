package com.example.ai_career_service.provider;

import com.example.ai_career_service.dto.prompt.RecommendationPrompt;

public class MockProvider implements AIProvider {
    @Override
    public String generateExplanation(
            RecommendationPrompt prompt) {

        return com.example.ai_career_service.service.RecommendationExplanation.fallback(prompt);
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        String lower = (systemPrompt + " " + userPrompt).toLowerCase();
        if (lower.contains("sinh") || lower.contains("câu hỏi") || lower.contains("question")) {
            return """
                    [
                      {"qOrder": 1, "question": "Bạn hiểu thế nào về kiến trúc Spring Boot và cách triển khai Dependency Injection?", "type": "TECH"},
                      {"qOrder": 2, "question": "Làm thế nào để tối ưu hóa truy vấn SQL và xử lý Transaction trong cơ sở dữ liệu lớn?", "type": "TECH"},
                      {"qOrder": 3, "question": "Trình bày cách bạn thiết kế RESTful API chuẩn và xử lý Exception toàn cục.", "type": "TECH"},
                      {"qOrder": 4, "question": "Khi gặp bất đồng ý kiến về kỹ thuật với đồng đội trong dự án, bạn xử lý như thế nào?", "type": "BEHAVIOR"},
                      {"qOrder": 5, "question": "Hãy kể về một tình huống bạn phải hoàn thành task dưới áp lực deadline ngặt nghèo.", "type": "BEHAVIOR"}
                    ]
                    """;
        }
        if (lower.contains("tóm tắt") || lower.contains("summary") || lower.contains("finish")) {
            return """
                    {"summary": "Ứng viên thể hiện kiến thức nền tảng vững vàng về kỹ thuật và xử lý tình huống linh hoạt. Cần củng cố thêm ví dụ thực tế khi trả lời câu hỏi chuyên sâu."}
                    """;
        }
        // Mặc định là chấm điểm câu trả lời
        return """
                {
                  "score": 8,
                  "feedback": "Câu trả lời đúng trọng tâm và nêu bật được các khái niệm chính, diễn đạt mạch lạc.",
                  "suggestion": "Để đạt điểm tối đa, bạn nên bổ sung thêm ví dụ cụ thể từ dự án thực tế đã từng tham gia."
                }
                """;
    }
}
