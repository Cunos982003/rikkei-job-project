package vn.rikkei.jobs.assessment.form.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MentorEvaluationApiIT {

    private static final String MENTOR1_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MENTOR1_EMAIL = "mentor1@rikkei.com";
    private static final String MENTOR2_ID = "22222222-2222-2222-2222-222222222222";
    private static final String MENTOR2_EMAIL = "mentor2@rikkei.com";
    private static final String INTERN_AN =
            "b1111111-1111-1111-1111-111111111101";
    private static final String INTERN_MENTOR2 =
            "b2222222-2222-2222-2222-222222222201";
    private static final String CRITERION_TECH =
            "c1111111-1111-1111-1111-111111111101";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void mentor1OnlySeesOwnInternshipAndInterviewTasks() throws Exception {
        MvcResult internshipResult = mockMvc.perform(get("/api/v1/mentor/evaluation-tasks")
                        .param("context", "INTERNSHIP")
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.remainingCount").value(3))
                .andReturn();

        JsonNode internshipItems = objectMapper.readTree(internshipResult.getResponse().getContentAsString())
                .get("items");
        assertThat(internshipItems.toString()).doesNotContain(INTERN_MENTOR2);
        assertThat(internshipItems.toString()).contains("Nguyễn Văn An");

        mockMvc.perform(get("/api/v1/mentor/evaluation-tasks")
                        .param("context", "INTERVIEW")
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.items[0].candidateName").exists());

        mockMvc.perform(get("/api/v1/mentor/evaluation-tasks")
                        .param("context", "INTERNSHIP")
                        .header("X-User-Id", MENTOR2_ID)
                        .header("X-User-Email", MENTOR2_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].candidateName").value("Phạm Quốc Dũng"));
    }

    @Test
    void draftPersistsAndSubmitLocksEditing() throws Exception {
        MvcResult opened = mockMvc.perform(post("/api/v1/mentor/evaluation-tasks/{subjectId}/open", INTERN_AN)
                        .param("context", "INTERNSHIP")
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.editable").value(true))
                .andReturn();

        UUID evaluationId = UUID.fromString(
                objectMapper.readTree(opened.getResponse().getContentAsString()).get("id").asText());

        String draftBody =
                """
                {
                  "overallComment": "Tiến độ tốt, cần cải thiện test",
                  "items": [
                    {
                      "criterionId": "%s",
                      "score": 80,
                      "comment": "Nắm Spring Boot vững"
                    }
                  ]
                }
                """
                        .formatted(CRITERION_TECH);

        mockMvc.perform(put("/api/v1/evaluations/{id}", evaluationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody)
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallComment").value("Tiến độ tốt, cần cải thiện test"));

        mockMvc.perform(get("/api/v1/evaluations/{id}", evaluationId)
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallComment").value("Tiến độ tốt, cần cải thiện test"))
                .andExpect(jsonPath("$.items[?(@.criterionId=='%s')].score".formatted(CRITERION_TECH))
                        .value(org.hamcrest.Matchers.contains(80)));

        mockMvc.perform(post("/api/v1/evaluations/{id}/submit", evaluationId)
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.editable").value(false));

        mockMvc.perform(put("/api/v1/evaluations/{id}", evaluationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody)
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/mentor/evaluation-tasks")
                        .param("context", "INTERNSHIP")
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCount").value(2));
    }

    @Test
    void mentorCannotOpenSomeoneElsesSubject() throws Exception {
        mockMvc.perform(post("/api/v1/mentor/evaluation-tasks/{subjectId}/open", INTERN_MENTOR2)
                        .param("context", "INTERNSHIP")
                        .header("X-User-Id", MENTOR1_ID)
                        .header("X-User-Email", MENTOR1_EMAIL)
                        .header("X-User-Role", "MENTOR"))
                .andExpect(status().isForbidden());
    }
}
