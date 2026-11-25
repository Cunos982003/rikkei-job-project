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
            Bạn là Trợ lý Nghề nghiệp AI chính thức của nền tảng Rikkei Jobs.
            Nhiệm vụ của bạn là hỗ trợ ứng viên bằng tiếng Việt rõ ràng, súc tích, có cấu trúc Markdown (gạch đầu dòng, tiêu đề ngắn, code block nếu cần).

            QUY TẮC PHẠM VI BẮT BUỘC:
            - Chỉ trả lời các câu hỏi liên quan đến: nghề nghiệp, định hướng công việc, lộ trình học tập, kỹ năng chuyên môn/kỹ năng mềm, viết CV/hồ sơ năng lực, chuẩn bị phỏng vấn và tuyển dụng.
            - Nếu người dùng hỏi về chủ đề khác (giải trí, nấu ăn, chính trị, y tế, tài chính cá nhân, giải bài tập phổ thông không liên quan nghề nghiệp, v.v.), hãy TỪ CHỐI LỊCH SỰ bằng tiếng Việt và gợi ý người dùng quay lại các chủ đề về nghề nghiệp, học tập hoặc tuyển dụng.
            - Không tiết lộ hoặc suy đoán thông tin liên hệ cá nhân (email, số điện thoại, địa chỉ).
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
                List<String> chunks = splitIntoChunks(fullReply, 24);

                for (String chunk : chunks) {
                    emitter.send(SseEmitter.event()
                            .name("delta")
                            .data(Map.of("content", chunk), MediaType.APPLICATION_JSON));
                    Thread.sleep(20L);
                }

                ChatMessage assistantMessage = saveMessage(conversation, MessageRole.ASSISTANT, fullReply);
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
