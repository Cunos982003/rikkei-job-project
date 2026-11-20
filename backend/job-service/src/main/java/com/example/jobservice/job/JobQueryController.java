package com.example.jobservice.job;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobQueryController {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

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
            where.append(" AND UPPER(j.status) = 'APPROVED' AND j.deadline >= ? ");
            params.add(java.sql.Date.valueOf(LocalDate.now(VIETNAM_ZONE)));
        }
        if (StringUtils.hasText(jobType)) {
            where.append(" AND REPLACE(UPPER(j.type), '_', '') = ? ");
            params.add(jobType.trim().replace("_", "").toUpperCase());
        }
        if (StringUtils.hasText(location)) {
            where.append(" AND (LOWER(COALESCE(j.city, '')) LIKE LOWER(?) OR LOWER(COALESCE(j.address, '')) LIKE LOWER(?)) ");
            String loc = "%" + location.trim() + "%";
            params.add(loc);
            params.add(loc);
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
                       j.title, j.description, j.requirements, j.level, j.type,
                       j.status, j.salary_min, j.salary_max, j.city, j.address, j.deadline, j.created_at
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

    private Map<String, Object> toJobPayload(Map<String, Object> row) {
        UUID id = (UUID) getVal(row, "id");
        UUID companyId = (UUID) getVal(row, "company_id");

        List<Map<String, Object>> skillRows = jdbcTemplate.queryForList(
                "SELECT js.skill_id, js.skill_code, js.skill_name, js.min_score, js.required FROM job_skills js WHERE js.job_id = ? ORDER BY js.required DESC, js.skill_name ASC",
                id);

        List<String> requiredSkills = new ArrayList<>();
        List<Map<String, Object>> jobSkills = new ArrayList<>(skillRows.size());
        for (Map<String, Object> skillRow : skillRows) {
            String skillName = (String) getVal(skillRow, "skill_name");
            String skillCode = (String) getVal(skillRow, "skill_code");
            String skillIdCol = (String) getVal(skillRow, "skill_id");
            String effectiveSkillId = StringUtils.hasText(skillCode) ? skillCode : skillIdCol;
            Boolean isMandatory = toBoolean(getVal(skillRow, "required"));
            Integer minScore = toInteger(getVal(skillRow, "min_score"));

            if (Boolean.TRUE.equals(isMandatory)) {
                requiredSkills.add(skillName != null ? skillName : effectiveSkillId);
            }

            Map<String, Object> js = new LinkedHashMap<>();
            js.put("skillId", effectiveSkillId);
            js.put("skillName", skillName);
            js.put("minScore", minScore != null ? minScore : 0);
            js.put("isMandatory", isMandatory != null ? isMandatory : true);
            jobSkills.add(js);
        }

        if (requiredSkills.isEmpty() && !jobSkills.isEmpty()) {
            for (Map<String, Object> js : jobSkills) {
                Object name = js.get("skillName");
                requiredSkills.add(name != null ? name.toString() : (String) js.get("skillId"));
            }
        }

        String city = (String) getVal(row, "city");
        String address = (String) getVal(row, "address");
        String locationStr = city != null ? city.trim() : null;
        if (StringUtils.hasText(address)) {
            locationStr = (locationStr != null && !locationStr.isBlank())
                    ? address.trim() + ", " + locationStr
                    : address.trim();
        }

        Object levelVal = getVal(row, "level");
        Object typeVal = getVal(row, "type");
        Object statusVal = getVal(row, "status");

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("companyId", companyId);
        item.put("companyName", getVal(row, "company_name"));
        item.put("title", getVal(row, "title"));
        item.put("description", getVal(row, "description"));
        item.put("requirements", getVal(row, "requirements"));
        item.put("experienceLevel", levelVal != null ? levelVal.toString() : null);
        item.put("jobType", formatJobType(typeVal));
        item.put("status", statusVal != null ? statusVal.toString() : null);
        item.put("salaryMin", toDouble(getVal(row, "salary_min")));
        item.put("salaryMax", toDouble(getVal(row, "salary_max")));
        item.put("location", locationStr);
        item.put("workLocation", locationStr);
        item.put("expiresAt", deadlineToExpiresAt(getVal(row, "deadline")));
        item.put("createdAt", toInstant(getVal(row, "created_at")));
        item.put("requiredSkills", requiredSkills);
        item.put("jobSkills", jobSkills);
        item.put("hrEmails", Collections.emptyList());
        return item;
    }

    private String formatJobType(Object typeObj) {
        if (typeObj == null) {
            return null;
        }
        String t = typeObj.toString().trim().toUpperCase().replace("_", "");
        return switch (t) {
            case "FULLTIME" -> "FULL_TIME";
            case "PARTTIME" -> "PART_TIME";
            case "INTERNSHIP" -> "INTERNSHIP";
            default -> typeObj.toString();
        };
    }

    private Instant deadlineToExpiresAt(Object deadlineObj) {
        if (deadlineObj == null) return null;
        LocalDate date = null;
        if (deadlineObj instanceof LocalDate ld) {
            date = ld;
        } else if (deadlineObj instanceof java.sql.Date sd) {
            date = sd.toLocalDate();
        } else if (deadlineObj instanceof java.util.Date ud) {
            date = ud.toInstant().atZone(VIETNAM_ZONE).toLocalDate();
        } else {
            try {
                date = LocalDate.parse(deadlineObj.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return date.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();
    }

    private Object getVal(Map<String, Object> map, String key) {
        if (map == null) return null;
        if (map.containsKey(key)) return map.get(key);
        if (map.containsKey(key.toUpperCase())) return map.get(key.toUpperCase());
        if (map.containsKey(key.toLowerCase())) return map.get(key.toLowerCase());
        return null;
    }

    private Boolean toBoolean(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() != 0;
        return Boolean.valueOf(val.toString());
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        if (value instanceof java.time.ZonedDateTime zdt) return zdt.toInstant();
        if (value instanceof java.time.LocalDateTime ldt) return ldt.atZone(VIETNAM_ZONE).toInstant();
        if (value instanceof java.util.Date d) return d.toInstant();
        return null;
    }
}
