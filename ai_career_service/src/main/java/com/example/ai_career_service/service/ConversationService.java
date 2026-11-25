package com.example.ai_career_service.service;

import com.example.ai_career_service.dto.chat.AiQuotaResponse;
import com.example.ai_career_service.dto.chat.ChatMessageResponse;
import com.example.ai_career_service.dto.chat.ConversationResponse;
import com.example.ai_career_service.dto.chat.CreateConversationRequest;
import com.example.ai_career_service.dto.chat.SendMessageResponse;
import com.example.ai_career_service.dto.interview.InterviewPracticeResponse;
import com.example.ai_career_service.dto.interview.PracticeQuestionResponse;
import com.example.ai_career_service.model.ChatConversation;
import com.example.ai_career_service.model.ChatMessage;
import com.example.ai_career_service.model.ConversationMode;
import com.example.ai_career_service.model.MessageRole;
import com.example.ai_career_service.model.PracticeStatus;
import com.example.ai_career_service.provider.AIProvider;
import com.example.ai_career_service.repository.ChatConversationRepository;
import com.example.ai_career_service.repository.ChatMessageRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private static final int CONTEXT_MESSAGE_LIMIT = 10;
    private static final String DEFAULT_CAREER_TITLE = "Tư vấn nghề nghiệp";

    static final String CAREER_SYSTEM_PROMPT = """
            # Rikkei Buddy – Tính cách & Nguyên tắc hành xử AI

            ## Danh tính
            Bạn là **Rikkei Buddy**, người bạn đồng hành AI thân thiện trong hệ sinh thái **Rikkei Jobs** và **Rikkei Edu**.
            Vai trò của bạn là hỗ trợ người dùng xuyên suốt hành trình học tập, phát triển kỹ năng, định hướng nghề nghiệp, tìm kiếm việc làm và phát triển sự nghiệp bền vững.
            Bạn không chỉ là một chatbot, mà là một người bạn đồng hành tận tâm, một người cố vấn am hiểu và một người hướng dẫn thực tế.
            Khẩu hiệu của bạn: **Your Learning & Career Companion** (Người đồng hành học tập và sự nghiệp của bạn).

            ## Tính cách
            - Thân thiện và gần gũi: Giao tiếp tự nhiên, ấm áp.
            - Khích lệ tích cực: Động viên người dùng đúng mực, không phóng đại hay tâng bốc quá đà.
            - Am hiểu: Giải thích khái niệm chính xác, rõ ràng.
            - Thực tế: Đưa ra các bước hành động cụ thể, ví dụ minh họa và gợi ý hữu ích.
            - Kiên nhẫn: Không bao giờ khiến người dùng ngại ngùng khi hỏi các câu hỏi cơ bản.
            - Trung thực: Không bịa đặt thông tin, bằng cấp, tin tuyển dụng, chi tiết khóa học hay dữ kiện.
            - Linh hoạt: Điều chỉnh cách giải thích phù hợp với trình độ và bối cảnh của người dùng.

            ## Phong cách giao tiếp
            - Mặc định sử dụng tiếng Việt, trừ khi người dùng giao tiếp bằng ngôn ngữ khác.
            - Xưng là "mình" và gọi người dùng là "bạn".
            - Giữ câu trả lời tự nhiên, mạch lạc, dễ hiểu.
            - Tránh ngôn ngữ quá cứng nhắc, máy móc hoặc quá suồng sã.
            - Sử dụng emoji tiết chế và phù hợp ngữ cảnh.
            - Trình bày các câu trả lời phức tạp bằng Markdown (tiêu đề, gạch đầu dòng, ví dụ, code block hoặc hướng dẫn từng bước).
            - Ưu tiên câu trả lời súc tích, trừ khi người dùng yêu cầu giải thích chi tiết.

            ## Hỗ trợ học tập – Rikkei Edu
            - Giải thích kiến thức từng bước kèm ví dụ thực tế, đặc biệt trong lĩnh vực công nghệ phần mềm.
            - Giúp người dùng hiểu bản chất thay vì chỉ đưa đáp án sẵn; khuyến khích tư duy phản biện và tự giải quyết vấn đề.
            - Điều chỉnh nội dung theo mức độ mới bắt đầu, trung cấp hoặc nâng cao; hỗ trợ lập kế hoạch học tập, ôn luyện khái niệm kỹ thuật, bài tập và lộ trình học.

            ## Hỗ trợ nghề nghiệp – Rikkei Jobs
            - Hỗ trợ cải thiện CV, gợi ý công việc phù hợp, lập kế hoạch nghề nghiệp và chuẩn bị phỏng vấn.
            - Đưa ra góp ý mang tính xây dựng và cụ thể; giải thích vì sao một công việc hoặc kỹ năng phù hợp với mục tiêu của người dùng; giúp nhận diện khoảng trống kỹ năng và đề xuất cách cải thiện khả thi.
            - Không bao giờ cam kết hay đảm bảo chắc chắn việc trúng tuyển, đậu phỏng vấn hoặc kết quả tuyển dụng. Không bịa đặt thông tin tin tuyển dụng hay năng lực ứng viên.

            ## Nguyên tắc phản hồi & Giới hạn phạm vi
            1. Hiểu đúng mục đích của người dùng trước khi trả lời và đi thẳng vào câu hỏi chính với hướng dẫn thực tế, áp dụng được ngay.
            2. Chỉ hỏi lại để làm rõ khi thực sự cần thiết; luôn hỗ trợ với thái độ tôn trọng, không kẻ cả.
            3. Tôn trọng quyền riêng tư: không yêu cầu thông tin liên hệ cá nhân không cần thiết và không tiết lộ hay suy đoán thông tin liên hệ (email, số điện thoại, địa chỉ).
            4. Nếu chưa chắc chắn về thông tin nào, hãy nêu rõ sự chưa chắc chắn đó; không bao giờ giả vờ đã thực hiện một hành động mà bạn chưa thực hiện.
            5. GIỚI HẠN PHẠM VI BẮT BUỘC: Chỉ trả lời các chủ đề về nghề nghiệp, học tập, kỹ năng, CV, phỏng vấn và tuyển dụng. Nếu người dùng hỏi chủ đề ngoài phạm vi (giải trí, nấu ăn, chính trị, y tế, thời tiết, v.v.), hãy từ chối lịch sự bằng giọng văn thân thiện của Rikkei Buddy và gợi ý quay lại chủ đề học tập hoặc nghề nghiệp.
            """;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final AiUsageService aiUsageService;
    private final CandidateContextService candidateContextService;
    private final InterviewPracticeService interviewPracticeService;
    private final AIProvider aiProvider;
    private final Executor aiExecutor;

    @Transactional
    public ConversationResponse createConversation(UUID userId, CreateConversationRequest request) {
        if (request.mode() == ConversationMode.INTERVIEW && request.jobId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tin tuyển dụng (JD) để bắt đầu luyện phỏng vấn");
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setMode(request.mode());
        conversation.setJobId(request.jobId());
        conversation.setCreatedAt(Instant.now());

        if (request.mode() == ConversationMode.INTERVIEW) {
            InterviewPracticeResponse practice = interviewPracticeService.createPractice(userId, request.jobId());
            conversation.setPracticeId(practice.id());
            String customTitle = request.title() != null && !request.title().isBlank()
                    ? request.title().trim()
                    : "Phỏng vấn: " + practice.jobTitle();
            conversation.setTitle(truncateTitle(customTitle));
            conversation = conversationRepository.save(conversation);

            String openingContent = buildInterviewOpeningMessage(practice);
            saveMessage(conversation, MessageRole.ASSISTANT, openingContent);
            return ConversationResponse.from(conversation);
        }

        String title = (request.title() != null && !request.title().isBlank())
                ? truncateTitle(request.title().trim())
                : DEFAULT_CAREER_TITLE;
        conversation.setTitle(title);
        conversation = conversationRepository.save(conversation);
        return ConversationResponse.from(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(UUID userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID userId, UUID conversationId) {
        ChatConversation conversation = findOwnedConversation(userId, conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public SendMessageResponse sendMessage(UUID userId, UUID conversationId, String rawContent) {
        String content = validateMessageContent(rawContent);
        ChatConversation conversation = findOwnedConversation(userId, conversationId);
        AiQuotaResponse quota = aiUsageService.consumeQuota(userId);

        ChatMessage userMessage = saveMessage(conversation, MessageRole.USER, content);
        maybeUpdateTitle(conversation, content);

        String assistantReply = generateReply(userId, conversation, content);
        ChatMessage assistantMessage = saveMessage(conversation, MessageRole.ASSISTANT, assistantReply);

        return new SendMessageResponse(
                ChatMessageResponse.from(userMessage),
                ChatMessageResponse.from(assistantMessage),
                quota
        );
    }

    public SseEmitter streamMessage(UUID userId, UUID conversationId, String rawContent) {
        String content = validateMessageContent(rawContent);
        ChatConversation conversation = findOwnedConversation(userId, conversationId);
        AiQuotaResponse quota = aiUsageService.consumeQuota(userId);

        ChatMessage userMessage = saveMessage(conversation, MessageRole.USER, content);
        maybeUpdateTitle(conversation, content);

        SseEmitter emitter = new SseEmitter(120_000L);
        aiExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("user_message")
                        .data(ChatMessageResponse.from(userMessage), MediaType.APPLICATION_JSON));

                String fullReply = generateReply(userId, conversation, content);
                ChatMessage assistantMessage = saveMessage(conversation, MessageRole.ASSISTANT, fullReply);
                List<String> chunks = splitIntoChunks(fullReply, 60);

                for (String chunk : chunks) {
                    emitter.send(SseEmitter.event()
                            .name("delta")
                            .data(Map.of("content", chunk), MediaType.APPLICATION_JSON));
                    Thread.sleep(15L);
                }

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of(
                                "userMessage", ChatMessageResponse.from(userMessage),
                                "assistantMessage", ChatMessageResponse.from(assistantMessage),
                                "quota", quota
                        ), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (IOException ex) {
                log.debug("SSE client disconnected during streaming for conversation {}: {}", conversationId, ex.getMessage());
                emitter.complete();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                emitter.complete();
            } catch (Exception ex) {
                log.error("Error streaming message for conversation {}", conversationId, ex);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", "Không thể tạo phản hồi lúc này. Vui lòng thử lại."), MediaType.APPLICATION_JSON));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    private String generateReply(UUID userId, ChatConversation conversation, String latestUserContent) {
        if (conversation.getMode() == ConversationMode.INTERVIEW) {
            return handleInterviewReply(userId, conversation, latestUserContent);
        }
        return handleCareerReply(userId, conversation.getId(), latestUserContent);
    }

    private String handleCareerReply(UUID userId, UUID conversationId, String latestUserContent) {
        List<ChatMessage> recentDesc = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, CONTEXT_MESSAGE_LIMIT));
        List<ChatMessage> recentAsc = new ArrayList<>(recentDesc);
        Collections.reverse(recentAsc);

        String profileSummary = candidateContextService.buildProfileSummary(userId);
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("### Tóm tắt hồ sơ năng lực ứng viên (không có thông tin liên hệ):\n")
                .append(profileSummary)
                .append("\n\n### Lịch sử hội thoại gần nhất (tối đa 10 tin):\n");

        for (ChatMessage msg : recentAsc) {
            userPrompt.append(msg.getRole() == MessageRole.USER ? "Ứng viên: " : "Trợ lý: ")
                    .append(msg.getContent())
                    .append("\n");
        }
        userPrompt.append("\nHãy trả lời câu hỏi mới nhất của ứng viên: ").append(latestUserContent);

        return aiProvider.chat(CAREER_SYSTEM_PROMPT, userPrompt.toString());
    }

    private String handleInterviewReply(UUID userId, ChatConversation conversation, String answerContent) {
        UUID practiceId = conversation.getPracticeId();
        if (practiceId == null) {
            return "Phiên luyện phỏng vấn chưa được liên kết với bộ câu hỏi JD. Vui lòng tạo phiên luyện phỏng vấn mới.";
        }

        InterviewPracticeResponse practice = interviewPracticeService.getPractice(userId, practiceId);
        if (PracticeStatus.DONE.name().equalsIgnoreCase(practice.status())) {
            return "Buổi luyện phỏng vấn cho vị trí **" + practice.jobTitle() + "** đã hoàn thành với điểm tổng kết **"
                    + practice.overallScore() + "/10**.\n\n"
                    + (practice.summary() != null ? practice.summary() : "")
                    + "\n\nBạn có thể mở tab **Luyện phỏng vấn** và chọn JD khác để bắt đầu buổi mới!";
        }

        PracticeQuestionResponse currentQuestion = practice.questions().stream()
                .filter(q -> q.answer() == null || q.answer().isBlank())
                .findFirst()
                .orElse(null);

        if (currentQuestion == null) {
            InterviewPracticeResponse finished = interviewPracticeService.finishPractice(userId, practiceId);
            return buildInterviewSummaryMessage(finished);
        }

        PracticeQuestionResponse graded = interviewPracticeService.submitAnswer(
                userId, practiceId, currentQuestion.id(), answerContent);

        StringBuilder reply = new StringBuilder();
        reply.append("### Đánh giá Câu ").append(graded.qOrder()).append(" (").append(graded.type()).append(")\n")
                .append("- **Điểm:** ").append(graded.score() != null ? graded.score() : 0).append("/10\n")
                .append("- **Nhận xét:** ").append(graded.feedback() != null ? graded.feedback() : "").append("\n")
                .append("- **Gợi ý cải thiện:** ").append(graded.suggestion() != null ? graded.suggestion() : "").append("\n");

        InterviewPracticeResponse updatedPractice = interviewPracticeService.getPractice(userId, practiceId);
        PracticeQuestionResponse nextQuestion = updatedPractice.questions().stream()
                .filter(q -> q.answer() == null || q.answer().isBlank())
                .findFirst()
                .orElse(null);

        if (nextQuestion != null) {
            reply.append("\n---\n")
                    .append("**Câu ").append(nextQuestion.qOrder()).append("/")
                    .append(updatedPractice.questions().size())
                    .append(" (").append(nextQuestion.type()).append("):**\n")
                    .append(nextQuestion.question());
        } else {
            InterviewPracticeResponse finished = interviewPracticeService.finishPractice(userId, practiceId);
            reply.append("\n---\n").append(buildInterviewSummaryMessage(finished));
        }

        return reply.toString();
    }

    private String buildInterviewOpeningMessage(InterviewPracticeResponse practice) {
        StringBuilder sb = new StringBuilder();
        sb.append("Chào bạn! Chúng ta sẽ bắt đầu buổi luyện phỏng vấn cho vị trí **")
                .append(practice.jobTitle())
                .append("** gồm **")
                .append(practice.questions().size())
                .append(" câu hỏi** (kỹ thuật & tình huống).\n\n");
        if (!practice.questions().isEmpty()) {
            PracticeQuestionResponse first = practice.questions().get(0);
            sb.append("**Câu ").append(first.qOrder()).append("/")
                    .append(practice.questions().size())
                    .append(" (").append(first.type()).append("):**\n")
                    .append(first.question());
        }
        return sb.toString();
    }

    private String buildInterviewSummaryMessage(InterviewPracticeResponse finished) {
        return "### Tổng kết buổi luyện phỏng vấn (" + finished.jobTitle() + ")\n"
                + "- **Điểm trung bình:** **" + finished.overallScore() + "/10**\n"
                + "- **Nhận xét tổng quan:** " + (finished.summary() != null ? finished.summary() : "Hoàn thành tốt!") + "\n\n"
                + "Bạn có thể chọn một JD khác ở tab **Luyện phỏng vấn** để tiếp tục rèn luyện.";
    }

    private ChatConversation findOwnedConversation(UUID userId, UUID conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc hội thoại"));
    }

    private String validateMessageContent(String rawContent) {
        if (rawContent == null || rawContent.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung tin nhắn không được để trống");
        }
        return rawContent.trim();
    }

    private ChatMessage saveMessage(ChatConversation conversation, MessageRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        message.setTokens(estimateTokens(content));
        message.setCreatedAt(Instant.now());
        return messageRepository.save(message);
    }

    private void maybeUpdateTitle(ChatConversation conversation, String firstUserMessage) {
        if (conversation.getMode() == ConversationMode.CAREER
                && DEFAULT_CAREER_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(truncateTitle(firstUserMessage));
            conversationRepository.save(conversation);
        }
    }

    private String truncateTitle(String text) {
        String singleLine = text.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 60 ? singleLine : singleLine.substring(0, 57) + "...";
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    private List<String> splitIntoChunks(String text, int chunkSize) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }
}
