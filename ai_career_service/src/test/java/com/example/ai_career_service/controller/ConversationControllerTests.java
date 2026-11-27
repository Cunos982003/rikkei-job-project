package com.example.ai_career_service.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ai_career_service.model.AiUsage;
import com.example.ai_career_service.repository.AiUsageRepository;
import com.example.ai_career_service.repository.ChatConversationRepository;
import com.example.ai_career_service.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class ConversationControllerTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private AiUsageRepository aiUsageRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        aiUsageRepository.deleteAll();
    }

    @Test
    void careerConversation_createSendListAndQuotaLimit() throws Exception {
        UUID userId = UUID.randomUUID();

        // 1. Create CAREER conversation
        MvcResult createResult = mockMvc.perform(post("/me/conversations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CAREER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mode").value("CAREER"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String conversationId = createdJson.path("data").path("id").asText();

        // 2. Send a career message
        mockMvc.perform(post("/me/conversations/" + conversationId + "/messages")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Mình nên học gì để làm Java Backend?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userMessage.role").value("USER"))
                .andExpect(jsonPath("$.data.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.assistantMessage.content", containsString("Java")))
                .andExpect(jsonPath("$.data.quota.used").value(1))
                .andExpect(jsonPath("$.data.quota.remaining").value(29));

        // 3. List conversations & messages
        mockMvc.perform(get("/me/conversations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/me/conversations/" + conversationId + "/messages")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // 4. Exhaust daily quota -> 429 with resetAt
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        aiUsageRepository.save(new AiUsage(userId, today, 30));

        mockMvc.perform(post("/me/conversations/" + conversationId + "/messages")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Thêm câu hỏi nữa\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.statusCode").value(429))
                .andExpect(jsonPath("$.data.remaining").value(0))
                .andExpect(jsonPath("$.data.resetAt").isNotEmpty());
    }

    @Test
    void interviewConversation_usesAi03FlowAndGradesAnswer() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        MvcResult createResult = mockMvc.perform(post("/me/conversations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"INTERVIEW\",\"jobId\":\"" + jobId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mode").value("INTERVIEW"))
                .andExpect(jsonPath("$.data.practiceId").isNotEmpty())
                .andReturn();

        String conversationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Opening message with Question 1 should already exist
        mockMvc.perform(get("/me/conversations/" + conversationId + "/messages")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].content", containsString("Câu 1/5")));

        // Answer Question 1 -> receives AI-03 grading + Question 2
        mockMvc.perform(post("/me/conversations/" + conversationId + "/messages")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Spring Boot sử dụng IoC container và Dependency Injection để quản lý bean.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assistantMessage.content", containsString("Điểm:")))
                .andExpect(jsonPath("$.data.assistantMessage.content", containsString("Câu 2/5")));
    }

    @Test
    void roleCandidateWithPrefix_isAllowed() throws Exception {
        mockMvc.perform(get("/me/conversations")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ROLE_CANDIDATE"))
                .andExpect(status().isOk());
    }

    @Test
    void nonCandidateRole_isForbidden() throws Exception {
        mockMvc.perform(get("/me/conversations")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "HR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteConversation_success() throws Exception {
        UUID userId = UUID.randomUUID();
        MvcResult createResult = mockMvc.perform(post("/me/conversations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"CAREER\",\"title\":\"Test Conversation\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        String convId = objectMapper.readTree(body).path("data").path("id").asText();

        // Send a message first
        mockMvc.perform(post("/me/conversations/" + convId + "/messages")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello AI\"}"))
                .andExpect(status().isOk());

        // Delete conversation
        mockMvc.perform(delete("/me/conversations/" + convId)
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("thành công")));

        // Verify conversation no longer in list
        mockMvc.perform(get("/me/conversations")
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Role", "CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
