package com.example.profileservice.internship;


import com.example.profileservice.security.UserPrincipal;
import com.example.profileservice.skill.application.SkillRecomputeService;
import com.example.profileservice.skill.application.dto.ExternalRecordDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import com.example.profileservice.skill.api.ProfileVisibilityService;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/profiles")
public class InternshipController {
    private final JdbcTemplate jdbc;
    private final SkillRecomputeService recompute;
    private final ProfileVisibilityService visibility;
    private final RestClient pmClient;
    private final String studentId;

    public InternshipController(JdbcTemplate jdbc, SkillRecomputeService recompute, ProfileVisibilityService visibility,
            @Value("${integrations.pm.base-url:http://localhost:9000}") String pmBaseUrl,
            @Value("${integrations.pm.student-id:std-001}") String studentId) {
        this.jdbc = jdbc;
        this.recompute = recompute;
        this.visibility = visibility;
        this.pmClient = RestClient.builder().baseUrl(pmBaseUrl).build();
        this.studentId = studentId;
    }

    @GetMapping("/me/internships")
    public List<Map<String, Object>> syncAndGet(@AuthenticationPrincipal UserPrincipal principal,
                                                 @RequestHeader(value = "X-PM-Student-Id", required = false) String pmStudentId) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        UUID userId = principal.getUserId();
        String externalId = pmStudentId == null || pmStudentId.isBlank() ? studentId : pmStudentId;
        JsonNode records;
        try {
            records = pmClient.get().uri("/pm/students/{id}/internships", externalId).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể lấy dữ liệu thực tập từ lms-pm", e);
        }
        if (records == null || !records.isArray()) return List.of();
        List<ExternalRecordDto> evidence = new ArrayList<>();
        for (JsonNode item : records) {
            String batchId = item.path("batchId").asText();
            UUID internshipId = upsertInternship(userId, item, batchId);
            upsertTasks(internshipId, item.path("tasks"));
            String topic = item.path("topicName").asText();
            String group = item.path("groupName").asText();
            jdbc.update("INSERT INTO portfolio_projects(user_id,source,source_ref,name,role) VALUES (?, 'IOC', ?, ?, ?) " +
                    "ON CONFLICT(user_id,source,source_ref) DO UPDATE SET name=EXCLUDED.name, role=EXCLUDED.role",
                    userId, batchId, topic, "Thực tập sinh – " + group);
            if (item.hasNonNull("finalScore")) {
                short score = (short) Math.min(100, Math.max(0, Math.round(item.path("finalScore").floatValue() * 10)));
                String normalizedTopic = topic.toLowerCase(Locale.ROOT);
                String code = normalizedTopic.contains("java") || normalizedTopic.contains("backend") || normalizedTopic.contains("thanh toán")
                        ? "JAVA_BACKEND" : "IOC_JAVA";
                evidence.add(new ExternalRecordDto("PM", code, topic, score, OffsetDateTime.now()));
            }
        }
        if (!evidence.isEmpty()) recompute.recomputeSkills(userId, evidence);
        return jdbc.queryForList("SELECT i.*, COALESCE((SELECT json_agg(t ORDER BY t.accepted_at) FROM internship_tasks t WHERE t.internship_id=i.id), '[]'::json) AS tasks " +
                "FROM internships i WHERE i.user_id=? ORDER BY i.start_date DESC NULLS LAST, i.batch_id", userId);
    }

    @GetMapping("/{userId}/internships")
    public List<Map<String, Object>> getUserInternships(@PathVariable UUID userId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        UUID requesterId = principal.getUserId();
        if (!visibility.canViewProfile(requesterId, userId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return jdbc.queryForList("SELECT i.*, COALESCE((SELECT json_agg(t ORDER BY t.accepted_at) FROM internship_tasks t WHERE t.internship_id=i.id), '[]'::json) AS tasks " +
                "FROM internships i WHERE i.user_id=? ORDER BY i.start_date DESC NULLS LAST, i.batch_id", userId);
    }

    private UUID upsertInternship(UUID userId, JsonNode row, String batchId) {
        return jdbc.queryForObject("INSERT INTO internships(user_id,batch_id,batch_name,group_name,topic_name,mentor_email,start_date,end_date,final_score,synced_at) " +
                "VALUES (?,?,?,?,?,?,NULL,NULL,?,now()) ON CONFLICT(user_id,batch_id) DO UPDATE SET batch_name=EXCLUDED.batch_name, group_name=EXCLUDED.group_name, " +
                "topic_name=EXCLUDED.topic_name, mentor_email=EXCLUDED.mentor_email, final_score=EXCLUDED.final_score, synced_at=now() RETURNING id",
                UUID.class, userId, batchId, text(row,"batchName"), text(row,"groupName"), text(row,"topicName"), text(row,"mentorEmail"), decimal(row,"finalScore"));
    }

    private void upsertTasks(UUID internshipId, JsonNode tasks) {
        if (!tasks.isArray()) return;
        Set<String> current = new HashSet<>();
        for (JsonNode task : tasks) {
            String title = text(task,"title"); current.add(title);
            jdbc.update("INSERT INTO internship_tasks(internship_id,title,accepted_score,accepted_at) VALUES (?,?,?,?) " +
                    "ON CONFLICT(internship_id,title) DO UPDATE SET accepted_score=EXCLUDED.accepted_score, accepted_at=EXCLUDED.accepted_at",
                    internshipId, title, decimal(task,"acceptedScore"), instant(task,"acceptedAt"));
        }
        if (current.isEmpty()) jdbc.update("DELETE FROM internship_tasks WHERE internship_id=?", internshipId);
        else jdbc.update("DELETE FROM internship_tasks WHERE internship_id=? AND title <> ALL (?)", internshipId, current.toArray(String[]::new));
    }
    private static String text(JsonNode node, String field) { return node.path(field).isMissingNode() || node.path(field).isNull() ? null : node.path(field).asText(); }
    private static BigDecimal decimal(JsonNode node, String field) { return node.path(field).isNumber() ? node.path(field).decimalValue() : null; }
    private static OffsetDateTime instant(JsonNode node, String field) { String value=text(node,field); return value == null ? null : OffsetDateTime.parse(value); }
}
