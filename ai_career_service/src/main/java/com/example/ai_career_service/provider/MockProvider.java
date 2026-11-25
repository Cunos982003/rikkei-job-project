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
        String sysLower = systemPrompt != null ? systemPrompt.toLowerCase() : "";
        String userLower = userPrompt != null ? userPrompt.toLowerCase() : "";

        // 0. Career Assistant chat mode (Rikkei Buddy)
        if (sysLower.contains("rikkei buddy") || sysLower.contains("trợ lý nghề nghiệp") || sysLower.contains("nghề nghiệp, học tập, tuyển dụng")) {
            return buildCareerChatResponse(userLower);
        }

        String lower = sysLower + " " + userLower;
        // 1. Kiểm tra ưu tiên chấm điểm câu trả lời
        if (lower.contains("chấm điểm") || lower.contains("grading") || lower.contains("tiêu chí cốt lõi") || (lower.contains("đúng") && lower.contains("đủ"))) {
            return """
                    {
                      "score": 8,
                      "feedback": "Câu trả lời đúng trọng tâm và nêu bật được các khái niệm chính, diễn đạt mạch lạc.",
                      "suggestion": "Để đạt điểm tối đa, bạn nên bổ sung thêm ví dụ cụ thể từ dự án thực tế đã từng tham gia."
                    }
                    """;
        }
        // 2. Tóm tắt kết quả phiên phỏng vấn
        if (lower.contains("tóm tắt") || lower.contains("summary") || lower.contains("finish")) {
            return """
                    {"summary": "Ứng viên thể hiện kiến thức nền tảng vững vàng về kỹ thuật và xử lý tình huống linh hoạt. Cần củng cố thêm ví dụ thực tế khi trả lời câu hỏi chuyên sâu."}
                    """;
        }
        // 3. Sinh 5 câu hỏi phỏng vấn
        if (lower.contains("sinh") || lower.contains("tạo đúng 5 câu hỏi") || lower.contains("tạo 5 câu hỏi") || lower.contains("danh sách câu hỏi")) {
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
        // Mặc định là chấm điểm câu trả lời
        return """
                {
                  "score": 8,
                  "feedback": "Câu trả lời đúng trọng tâm và nêu bật được các khái niệm chính, diễn đạt mạch lạc.",
                  "suggestion": "Để đạt điểm tối đa, bạn nên bổ sung thêm ví dụ cụ thể từ dự án thực tế đã từng tham gia."
                }
                """;
    }

    private String buildCareerChatResponse(String userLower) {
        if (userLower.contains("thời tiết") || userLower.contains("nấu ăn") || userLower.contains("bóng đá") || userLower.contains("xổ số")) {
            return "Mình là **Rikkei Buddy** – người đồng hành về học tập và nghề nghiệp tại Rikkei Jobs & Rikkei Edu, nên mình chỉ hỗ trợ các chủ đề liên quan đến **học tập, kỹ năng, CV, phỏng vấn và tuyển dụng** thôi nhé. Bạn cần mình gợi ý lộ trình học hay chuẩn bị cho vị trí nào không?";
        }
        if (userLower.contains("java backend")) {
            return """
                    ### Lộ trình học Java Backend cùng Rikkei Buddy
                    Để đi vững từ nền tảng đến thực chiến Java Backend, bạn có thể đi theo từng bước sau:

                    1. **Nền tảng Java Core & OOP:** Java 17+, Collections, Stream API, Exception Handling và tư duy hướng đối tượng.
                    2. **Web & Framework:** Spring Boot 3, Spring Web MVC, Spring Security (JWT) và Bean Validation.
                    3. **Cơ sở dữ liệu:** PostgreSQL/MySQL, Spring Data JPA, tối ưu truy vấn SQL & index.
                    4. **Thực chiến dự án:** Xây dựng RESTful API hoàn chỉnh, viết Unit Test (JUnit 5, Mockito) và đóng gói bằng Docker.

                    Bạn đang ở mức mới bắt đầu hay đã có nền tảng Java cơ bản rồi? Nếu muốn, bạn có thể thử tab **Luyện phỏng vấn** để kiểm tra kiến thức ngay nhé!
                    """;
        }
        return """
                ### Mình là Rikkei Buddy – Your Learning & Career Companion
                Mình đã xem qua tóm tắt hồ sơ năng lực của bạn. Để tiến thêm một bước vững chắc, bạn có thể tập trung vào:

                - **Xác định mục tiêu rõ ràng:** Chọn vị trí bạn muốn hướng tới (Backend, Frontend, QA, Data...) và đối chiếu kỹ năng cần có.
                - **Bổ sung kỹ năng thực chiến:** Ưu tiên các kỹ năng còn thiếu qua bài tập và dự án nhỏ để làm nổi bật CV.
                - **Luyện phỏng vấn định kỳ:** Dùng tab **Luyện phỏng vấn** chọn một JD thực tế để mình chấm điểm và góp ý chi tiết cho bạn.

                Bạn muốn mình hỗ trợ cụ thể hơn về học tập, chỉnh sửa CV hay luyện phỏng vấn?
                """;
    }
}
