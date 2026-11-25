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

        // 0. Career Assistant chat mode
        if (sysLower.contains("trợ lý nghề nghiệp") || sysLower.contains("nghề nghiệp, học tập, tuyển dụng")) {
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
            return "Xin lỗi bạn, mình là **Trợ lý Nghề nghiệp Rikkei Jobs** nên chỉ hỗ trợ các chủ đề về **nghề nghiệp, học tập, kỹ năng, CV và tuyển dụng**. Bạn có muốn mình tư vấn lộ trình học tập hoặc chuẩn bị phỏng vấn không?";
        }
        if (userLower.contains("java backend")) {
            return """
                    ### Lộ trình trở thành Java Backend Developer
                    Dựa trên hồ sơ năng lực hiện tại của bạn, bạn nên tập trung theo thứ tự sau:

                    1. **Nền tảng Core & OOP:** Java 17+, Collections, Stream API, Exception Handling và Đa luồng.
                    2. **Framework & Web:** Spring Boot 3, Spring Web MVC, Spring Security (JWT), Validation.
                    3. **Cơ sở dữ liệu:** PostgreSQL/MySQL, Spring Data JPA, tối ưu truy vấn & index, Flyway/Liquibase.
                    4. **Kiến trúc & Vận hành:** RESTful API, Docker, Redis caching, Unit Test (JUnit 5, Mockito).

                    **Gợi ý bước tiếp theo:** Bạn có thể chọn một JD Java Backend trên Rikkei Jobs và thử tab **Luyện phỏng vấn** để kiểm tra kiến thức ngay!
                    """;
        }
        return """
                ### Gợi ý từ Trợ lý Nghề nghiệp Rikkei Jobs
                Mình đã xem qua tóm tắt hồ sơ năng lực của bạn. Để phát triển sự nghiệp hiệu quả:

                - **Xác định mục tiêu rõ ràng:** Chọn vị trí phù hợp (Backend, Frontend, QA, Data...) và đối chiếu với các JD đang mở.
                - **Bổ sung kỹ năng còn thiếu:** Tập trung vào các kỹ năng lõi có yêu cầu cao trong JD và làm dự án thực tế để chứng minh năng lực.
                - **Luyện phỏng vấn định kỳ:** Sử dụng chế độ **Luyện phỏng vấn** theo từng JD để nhận điểm và phản hồi chi tiết.

                Bạn muốn mình tư vấn sâu hơn về kỹ năng hay vị trí nào?
                """;
    }
}
