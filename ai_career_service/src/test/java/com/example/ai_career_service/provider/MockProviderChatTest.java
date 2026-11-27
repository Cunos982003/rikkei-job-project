package com.example.ai_career_service.provider;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MockProviderChatTest {

    @Test
    void testChatGeneratesQuestionsAndScores() {
        AIProvider provider = new MockProvider();
        String questionResponse = provider.chat("Bạn là chuyên gia phỏng vấn", "Sinh 5 câu hỏi phỏng vấn");
        assertNotNull(questionResponse);
        assertFalse(questionResponse.isBlank());
        assertTrue(questionResponse.contains("qOrder") || questionResponse.contains("question"));

        String gradingResponse = provider.chat("Chấm câu trả lời", "Chấm điểm score cho câu trả lời này");
        assertNotNull(gradingResponse);
        assertFalse(gradingResponse.isBlank());
        assertTrue(gradingResponse.contains("score") || gradingResponse.contains("feedback"));

        // Test with real prompt structure containing 'Câu hỏi:' and 'Câu trả lời của ứng viên:'
        String realSubmitPrompt = provider.chat(
                "Bạn là một chuyên gia phỏng vấn tuyển dụng CNTT giàu kinh nghiệm. Hãy chấm điểm câu trả lời...",
                "Vị trí tuyển dụng: Backend Java\nLoại câu hỏi: TECH\nCâu hỏi: Spring Boot là gì?\nCâu trả lời của ứng viên: Em dùng Spring Boot để làm API."
        );
        assertNotNull(realSubmitPrompt);
        assertTrue(realSubmitPrompt.contains("score"));
        assertTrue(realSubmitPrompt.contains("feedback"));
        assertFalse(realSubmitPrompt.trim().startsWith("["));
    }
}
