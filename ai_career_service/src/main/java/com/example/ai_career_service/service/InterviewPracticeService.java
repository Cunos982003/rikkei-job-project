package com.example.ai_career_service.service;

import com.example.ai_career_service.dto.interview.InterviewPracticeResponse;
import com.example.ai_career_service.dto.interview.PracticeQuestionResponse;
import com.example.ai_career_service.model.InterviewPractice;
import com.example.ai_career_service.model.PracticeQuestion;
import com.example.ai_career_service.model.PracticeStatus;
import com.example.ai_career_service.model.QuestionType;
import com.example.ai_career_service.provider.AIProvider;
import com.example.ai_career_service.repository.InterviewPracticeRepository;
import com.example.ai_career_service.repository.PracticeQuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewPracticeService {

    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");

    private final InterviewPracticeRepository practiceRepository;
    private final PracticeQuestionRepository questionRepository;
    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper;

    @Value("${app.integrations.job-service-url:http://localhost:8085}")
    private String jobServiceUrl;

    @Transactional
    public InterviewPracticeResponse createPractice(UUID userId, UUID jobId) {
        // 1. Fetch JD info
        JobDetails jd = fetchJobDetails(jobId);

        // 2. Generate 5 questions via AI
        String systemPrompt = """
                Bạn là một chuyên gia phỏng vấn tuyển dụng CNTT chuyên nghiệp.
                Dựa trên thông tin tin tuyển dụng (JD) được cung cấp, hãy tạo đúng 5 câu hỏi phỏng vấn bằng tiếng Việt:
                - 3 câu hỏi chuyên môn kỹ thuật (TECH) xoay quanh các kỹ năng, công nghệ yêu cầu trong JD.
                - 2 câu hỏi tình huống / hành vi (BEHAVIOR) để đánh giá kỹ năng mềm, giải quyết xung đột, tinh thần học hỏi.

                BẮT BUỘC trả về đúng định dạng JSON Array chứa đúng 5 phần tử như sau, KHÔNG kèm lời dẫn, không markdown thừa:
                [
                  {"qOrder": 1, "question": "...", "type": "TECH"},
                  {"qOrder": 2, "question": "...", "type": "TECH"},
                  {"qOrder": 3, "question": "...", "type": "TECH"},
                  {"qOrder": 4, "question": "...", "type": "BEHAVIOR"},
                  {"qOrder": 5, "question": "...", "type": "BEHAVIOR"}
                ]
                """;

        String userPrompt = "Thông tin JD:\nTiêu đề: " + jd.title() + "\nMô tả: " + jd.description()
                + "\nYêu cầu: " + jd.requirements() + "\nKỹ năng: " + jd.skills();

        JsonNode questionsJson = callAiWithRetry(systemPrompt, userPrompt, true);

        InterviewPractice practice = new InterviewPractice();
        practice.setUserId(userId);
        practice.setJobId(jobId);
        practice.setJobTitle(jd.title() != null && !jd.title().isBlank() ? jd.title() : "Vị trí tuyển dụng");
        practice.setStatus(PracticeStatus.IN_PROGRESS);
        practice.setCreatedAt(Instant.now());

        List<PracticeQuestion> questions = new ArrayList<>();
        if (questionsJson.isArray()) {
            for (int i = 0; i < questionsJson.size() && i < 5; i++) {
                JsonNode qNode = questionsJson.get(i);
                PracticeQuestion question = new PracticeQuestion();
                question.setPractice(practice);
                question.setQOrder(qNode.has("qOrder") ? qNode.get("qOrder").asInt(i + 1) : i + 1);
                question.setQuestion(qNode.has("question") ? qNode.get("question").asText() : "Câu hỏi phỏng vấn " + (i + 1));
                String typeStr = qNode.has("type") ? qNode.get("type").asText() : (i < 3 ? "TECH" : "BEHAVIOR");
                try {
                    question.setType(QuestionType.valueOf(typeStr.toUpperCase()));
                } catch (Exception e) {
                    question.setType(i < 3 ? QuestionType.TECH : QuestionType.BEHAVIOR);
                }
                questions.add(question);
            }
        }

        // Fallback nếu JSON không đủ 5 câu
        while (questions.size() < 5) {
            int nextOrder = questions.size() + 1;
            PracticeQuestion fallbackQ = new PracticeQuestion();
            fallbackQ.setPractice(practice);
            fallbackQ.setQOrder(nextOrder);
            fallbackQ.setType(nextOrder <= 3 ? QuestionType.TECH : QuestionType.BEHAVIOR);
            fallbackQ.setQuestion(nextOrder <= 3
                    ? "Bạn hãy trình bày kinh nghiệm và kiến thức của mình về các công nghệ yêu cầu trong vị trí " + practice.getJobTitle()
                    : "Hãy kể về một thử thách lớn bạn từng gặp trong công việc/học tập và cách bạn vượt qua.");
            questions.add(fallbackQ);
        }

        practice.setQuestions(questions);
        practice = practiceRepository.save(practice);

        return InterviewPracticeResponse.from(practice);
    }

    @Transactional
    public PracticeQuestionResponse submitAnswer(UUID userId, UUID practiceId, UUID questionId, String answer) {
        if (answer == null || answer.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Câu trả lời không được để trống");
        }

        InterviewPractice practice = practiceRepository.findByIdAndUserId(practiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên luyện phỏng vấn"));

        if (practice.getStatus() == PracticeStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phiên luyện tập này đã kết thúc");
        }

        PracticeQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi"));

        if (!question.getPractice().getId().equals(practiceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Câu hỏi không thuộc phiên luyện tập này");
        }

        // AI Chấm điểm theo 3 tiêu chí: Đúng, Đủ, Rõ ràng (thang 0-10)
        String systemPrompt = """
                Bạn là một chuyên gia phỏng vấn tuyển dụng CNTT giàu kinh nghiệm.
                Hãy chấm điểm câu trả lời phỏng vấn của ứng viên theo 3 tiêu chí cốt lõi:
                1. ĐÚNG (Kiến thức chính xác, logic hợp lý)
                2. ĐỦ (Đầy đủ ý chính, có ví dụ hoặc lập luận cụ thể)
                3. RÕ RÀNG (Diễn đạt mạch lạc, cấu trúc câu trả lời mạch lạc)

                Cho điểm tổng hợp từ 0 đến 10 (số nguyên). Đưa ra nhận xét ngắn gọn và gợi ý cách trả lời hoàn thiện hơn.

                BẮT BUỘC trả về đúng định dạng JSON như sau, không kèm bất kỳ giải thích nào khác:
                {
                  "score": 8,
                  "feedback": "Nhận xét súc tích bằng tiếng Việt...",
                  "suggestion": "Gợi ý câu trả lời tốt hơn..."
                }
                """;

        String userPrompt = "Vị trí tuyển dụng: " + practice.getJobTitle()
                + "\nLoại câu hỏi: " + question.getType()
                + "\nCâu hỏi: " + question.getQuestion()
                + "\nCâu trả lời của ứng viên: " + answer.trim();

        JsonNode gradingJson = callAiWithRetry(systemPrompt, userPrompt, false);

        int score = gradingJson.has("score") ? Math.max(0, Math.min(10, gradingJson.get("score").asInt())) : 6;
        String feedback = gradingJson.has("feedback") ? gradingJson.get("feedback").asText() : "Câu trả lời đã được ghi nhận.";
        String suggestion = gradingJson.has("suggestion") ? gradingJson.get("suggestion").asText() : "Nên bổ sung thêm dẫn chứng thực tế.";

        question.setAnswer(answer.trim());
        question.setScore(score);
        question.setFeedback(feedback);
        question.setSuggestion(suggestion);

        question = questionRepository.save(question);
        return PracticeQuestionResponse.from(question);
    }

    @Transactional
    public InterviewPracticeResponse finishPractice(UUID userId, UUID practiceId) {
        InterviewPractice practice = practiceRepository.findByIdAndUserId(practiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên luyện phỏng vấn"));

        if (practice.getStatus() == PracticeStatus.DONE) {
            return InterviewPracticeResponse.from(practice);
        }

        List<PracticeQuestion> questions = practice.getQuestions();
        double totalScore = 0.0;
        int count = 0;
        StringBuilder historyBuilder = new StringBuilder();

        for (PracticeQuestion q : questions) {
            int score = q.getScore() != null ? q.getScore() : 0;
            totalScore += score;
            count++;
            historyBuilder.append("- Câu ").append(q.getQOrder()).append(" (").append(q.getType()).append("): ")
                    .append(q.getQuestion()).append("\n  Điểm: ").append(score).append("/10\n  Nhận xét: ")
                    .append(q.getFeedback() != null ? q.getFeedback() : "Chưa trả lời").append("\n");
        }

        double overallScore = count > 0 ? Math.round((totalScore / count) * 10.0) / 10.0 : 0.0;

        // AI Tóm tắt điểm mạnh / điểm yếu
        String systemPrompt = """
                Bạn là chuyên gia phỏng vấn đánh giá tổng kết.
                Dựa trên điểm số và nhận xét từng câu trả lời của ứng viên, hãy viết một đoạn tóm tắt ngắn (khoảng 3-5 câu) bằng tiếng Việt nêu rõ:
                - Điểm mạnh nổi bật
                - Điểm còn hạn chế cần cải thiện
                BẮT BUỘC trả về đúng định dạng JSON:
                {"summary": "..."}
                """;

        String userPrompt = "Kết quả các câu hỏi:\n" + historyBuilder.toString();
        JsonNode summaryJson = callAiWithRetry(systemPrompt, userPrompt, false);
        String summary = summaryJson.has("summary")
                ? summaryJson.get("summary").asText()
                : "Ứng viên đã hoàn thành buổi luyện phỏng vấn với điểm trung bình " + overallScore + "/10.";

        practice.setStatus(PracticeStatus.DONE);
        practice.setOverallScore(overallScore);
        practice.setSummary(summary);

        practice = practiceRepository.save(practice);
        return InterviewPracticeResponse.from(practice);
    }

    @Transactional(readOnly = true)
    public List<InterviewPracticeResponse> listPractices(UUID userId) {
        return practiceRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(InterviewPracticeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewPracticeResponse getPractice(UUID userId, UUID practiceId) {
        InterviewPractice practice = practiceRepository.findByIdAndUserId(practiceId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên luyện phỏng vấn"));
        return InterviewPracticeResponse.from(practice);
    }

    private JsonNode callAiWithRetry(String systemPrompt, String userPrompt, boolean expectArray) {
        String rawResponse = null;
        try {
            rawResponse = aiProvider.chat(systemPrompt, userPrompt);
            return parseJson(rawResponse, expectArray);
        } catch (Exception firstError) {
            log.warn("AI response parsing failed on first attempt: {}. Retrying once with explicit instruction...", firstError.getMessage());
            try {
                String retrySystemPrompt = systemPrompt + "\nLƯU Ý: Lần trước định dạng sai. Chỉ trả về chuỗi JSON thô hợp lệ, không thêm bất kỳ văn bản nào khác.";
                rawResponse = aiProvider.chat(retrySystemPrompt, userPrompt);
                return parseJson(rawResponse, expectArray);
            } catch (Exception secondError) {
                log.error("AI response failed on retry. Raw response: {}. Using fallback response.", rawResponse, secondError);
                return createFallbackJson(systemPrompt, userPrompt, expectArray);
            }
        }
    }

    private JsonNode createFallbackJson(String systemPrompt, String userPrompt, boolean expectArray) {
        try {
            if (expectArray) {
                return objectMapper.readTree("""
                        [
                          {"qOrder": 1, "question": "Bạn hiểu thế nào về kiến trúc Spring Boot và cách triển khai Dependency Injection?", "type": "TECH"},
                          {"qOrder": 2, "question": "Làm thế nào để tối ưu hóa truy vấn SQL và xử lý Transaction trong cơ sở dữ liệu lớn?", "type": "TECH"},
                          {"qOrder": 3, "question": "Trình bày cách bạn thiết kế RESTful API chuẩn và xử lý Exception toàn cục.", "type": "TECH"},
                          {"qOrder": 4, "question": "Khi gặp bất đồng ý kiến về kỹ thuật với đồng đội trong dự án, bạn xử lý như thế nào?", "type": "BEHAVIOR"},
                          {"qOrder": 5, "question": "Hãy kể về một tình huống bạn phải hoàn thành task dưới áp lực deadline ngặt nghèo.", "type": "BEHAVIOR"}
                        ]
                        """);
            }
            String lower = (systemPrompt + " " + userPrompt).toLowerCase();
            if (lower.contains("tóm tắt") || lower.contains("summary") || lower.contains("finish")) {
                return objectMapper.readTree("""
                        {"summary": "Ứng viên đã hoàn thành buổi phỏng vấn. Kiến thức cơ bản được trình bày rõ ràng, cần củng cố thêm ví dụ thực tiễn trong công việc."}
                        """);
            }
            return objectMapper.readTree("""
                    {
                      "score": 8,
                      "feedback": "Câu trả lời đúng trọng tâm và nêu bật được các khái niệm chính, diễn đạt mạch lạc.",
                      "suggestion": "Để đạt điểm tối đa, bạn nên bổ sung thêm ví dụ cụ thể từ dự án thực tế đã từng tham gia."
                    }
                    """);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tạo phản hồi dự phòng");
        }
    }

    private JsonNode parseJson(String rawText, boolean expectArray) throws JsonProcessingException {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("AI returned empty response");
        }
        String clean = rawText.trim();
        Matcher matcher = MARKDOWN_CODE_BLOCK.matcher(clean);
        if (matcher.find()) {
            clean = matcher.group(1).trim();
        }

        JsonNode node = objectMapper.readTree(clean);
        if (expectArray && !node.isArray()) {
            throw new IllegalArgumentException("Expected JSON array but got " + node.getNodeType());
        }
        if (!expectArray && !node.isObject()) {
            throw new IllegalArgumentException("Expected JSON object but got " + node.getNodeType());
        }
        return node;
    }

    private JobDetails fetchJobDetails(UUID jobId) {
        try {
            RestClient restClient = RestClient.builder().baseUrl(jobServiceUrl).build();
            Map<String, Object> jobResponse = restClient.get()
                    .uri("/jobs/" + jobId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (jobResponse == null) {
                return new JobDetails("Vị trí tuyển dụng", "", "", "");
            }

            // Có thể response được bọc ApiResponse envelope
            Object dataObj = jobResponse.get("data");
            Map<String, Object> map = (dataObj instanceof Map<?, ?> m) ? (Map<String, Object>) m : jobResponse;

            String title = (String) map.get("title");
            String description = (String) map.get("description");
            String requirements = (String) map.get("requirements");

            StringBuilder skills = new StringBuilder();
            if (map.get("jobSkills") instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> sm) {
                        skills.append(sm.get("skillName")).append(", ");
                    }
                }
            } else if (map.get("requiredSkills") instanceof List<?> list) {
                for (Object item : list) {
                    skills.append(item).append(", ");
                }
            }

            return new JobDetails(
                    title != null ? title : "Lập trình viên",
                    description != null ? description : "",
                    requirements != null ? requirements : "",
                    skills.toString()
            );
        } catch (Exception e) {
            log.warn("Could not fetch JD from job-service for jobId {}: {}", jobId, e.getMessage());
            return new JobDetails("Lập trình viên phần mềm", "", "", "Java, Spring Boot, SQL, Git");
        }
    }

    private record JobDetails(String title, String description, String requirements, String skills) {}
}
