package com.example.jobservice.job;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class JobQueryController {

    private final JdbcTemplate jdbcTemplate;

    public JobQueryController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping({ "/jobs", "/api/v1/jobs" })
    public Map<String, Object> listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (StringUtils.hasText(status)) {
            where.append(" AND UPPER(j.status) = UPPER(?) ");
            params.add(status.trim());
        } else {
            where.append(" AND UPPER(j.status) = 'APPROVED' ");
        }
        if (StringUtils.hasText(jobType)) {
            where.append(" AND UPPER(j.job_type) = UPPER(?) ");
            params.add(jobType.trim());
        }
        if (StringUtils.hasText(location)) {
            where.append(" AND LOWER(COALESCE(j.work_location, '')) LIKE LOWER(?) ");
            params.add("%" + location.trim() + "%");
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (LOWER(j.title) LIKE LOWER(?) OR LOWER(COALESCE(c.name, '')) LIKE LOWER(?) OR LOWER(COALESCE(j.description, '')) LIKE LOWER(?)) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        String countSql = "SELECT COUNT(*) FROM jobs j LEFT JOIN companies c ON c.id = j.company_id " + where;
        Long totalObj = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long total = totalObj != null ? totalObj : 0L;

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(safeSize);
        pageParams.add((long) safePage * safeSize);

        String listSql = """
                SELECT j.id, j.company_id, COALESCE(c.name, 'Doanh nghiệp đối tác') AS company_name,
                       j.title, j.description, j.requirements, j.experience_level, j.job_type,
                       j.status, j.salary_min, j.salary_max, j.work_location, j.expires_at, j.created_at
                FROM jobs j
                LEFT JOIN companies c ON c.id = j.company_id
                """ + where + " ORDER BY j.created_at DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(listSql, pageParams.toArray());
        List<Map<String, Object>> items = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            items.add(toJobPayload(row));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", items);
        response.put("total", total);
        response.put("page", safePage);
        response.put("size", safeSize);
        return response;
    }

    @GetMapping({ "/jobs/{jobId}", "/api/v1/jobs/{jobId}" })
    public Map<String, Object> getJobById(@PathVariable UUID jobId) {
        String sql = """
                SELECT j.id, j.company_id, COALESCE(c.name, 'Doanh nghiệp đối tác') AS company_name,
                       j.title, j.description, j.requirements, j.experience_level, j.job_type,
                       j.status, j.salary_min, j.salary_max, j.work_location, j.expires_at, j.created_at
                FROM jobs j
                LEFT JOIN companies c ON c.id = j.company_id
                WHERE j.id = ?
                """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, jobId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        return toJobPayload(rows.get(0));
    }

    private Map<String, Object> toJobPayload(Map<String, Object> row) {
        UUID id = (UUID) row.get("id");
        UUID companyId = (UUID) row.get("company_id");

        List<Map<String, Object>> skillRows = jdbcTemplate.queryForList(
                "SELECT id, skill_code, skill_name, min_score, is_mandatory FROM job_skills WHERE job_id = ? ORDER BY is_mandatory DESC, skill_name ASC",
                id);

        List<String> requiredSkills = new ArrayList<>(skillRows.size());
        List<Map<String, Object>> jobSkills = new ArrayList<>(skillRows.size());
        for (Map<String, Object> skillRow : skillRows) {
            String skillName = (String) skillRow.get("skill_name");
            String skillCode = (String) skillRow.get("skill_code");
            requiredSkills.add(skillName != null ? skillName : skillCode);

            Map<String, Object> js = new LinkedHashMap<>();
            js.put("skillId", skillCode);
            js.put("skillName", skillName);
            js.put("minScore", skillRow.get("min_score"));
            js.put("isMandatory", skillRow.get("is_mandatory"));
            jobSkills.add(js);
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("companyId", companyId);
        item.put("companyName", row.get("company_name"));
        item.put("title", row.get("title"));
        item.put("description", row.get("description"));
        item.put("requirements", row.get("requirements"));
        item.put("experienceLevel", row.get("experience_level"));
        item.put("jobType", row.get("job_type"));
        item.put("status", row.get("status"));
        item.put("salaryMin", toDouble(row.get("salary_min")));
        item.put("salaryMax", toDouble(row.get("salary_max")));
        item.put("location", row.get("work_location"));
        item.put("workLocation", row.get("work_location"));
        item.put("expiresAt", toInstant(row.get("expires_at")));
        item.put("createdAt", toInstant(row.get("created_at")));
        item.put("requiredSkills", requiredSkills);
        item.put("jobSkills", jobSkills);
        item.put("hrEmails", List.of());
        return item;
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        return null;
    }

    private Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return null;
    }
}
