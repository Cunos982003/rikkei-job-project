package com.example.profileservice.skill.api;

import com.example.profileservice.dto.SkillCatalogResponse;
import com.example.profileservice.skill.application.SkillCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller cung cấp danh mục kỹ năng chuẩn của hệ thống.
 */
@RestController
@RequestMapping({"/api/v1/skill-catalog", "/api/v1/skills"})
@RequiredArgsConstructor
public class SkillCatalogController {

    private final SkillCatalogService skillCatalogService;

    @GetMapping
    public ResponseEntity<?> getSkills(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "q", required = false) String q) {

        List<SkillCatalogResponse> skills = skillCatalogService.getSkills(group, q);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("success", true);
        responseBody.put("statusCode", HttpStatus.OK.value());
        responseBody.put("message", "Lấy danh mục kỹ năng thành công");
        responseBody.put("data", skills);
        responseBody.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(responseBody);
    }
}

