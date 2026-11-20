package com.example.jobservice.job.catalog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "skill.catalog.stub", havingValue = "true")
public class StubSkillCatalogClient implements SkillCatalogClient {

    private static final List<CatalogSkill> SKILLS = List.of(
            createSkill("JAVA_CORE", "Java Core", "Programming"),
            createSkill("SPRING_BOOT", "Spring Boot", "Framework"),
            createSkill("SQL", "SQL", "Database"),
            createSkill("JAVASCRIPT", "JavaScript", "Programming"),
            createSkill("REACT", "React", "Frontend"),
            createSkill("HTML_CSS", "HTML/CSS", "Frontend"),
            createSkill("GIT", "Git", "Tool"),
            createSkill("DOCKER", "Docker", "DevOps"),
            createSkill("KUBERNETES", "Kubernetes", "DevOps"),
            createSkill("CI_CD", "CI/CD", "DevOps"),
            createSkill("TESTING", "Testing", "Quality"),
            createSkill("AGILE", "Agile/Scrum", "Process"),
            createSkill("COMMUNICATION", "Communication", "Soft Skill"),
            createSkill("PROBLEM_SOLVING", "Problem Solving", "Soft Skill"),
            createSkill("TEAMWORK", "Teamwork", "Soft Skill"),
            createSkill("ENGLISH", "English", "Language"),
            createSkill("PYTHON", "Python", "Programming"),
            createSkill("DATA_STRUCTURES", "Data Structures", "CS Fundamental"),
            createSkill("ALGORITHMS", "Algorithms", "CS Fundamental"),
            createSkill("DESIGN_PATTERNS", "Design Patterns", "Architecture")
    );

    private static final java.util.Map<String, String> MOCK_ID_MAP = java.util.Map.ofEntries(
            java.util.Map.entry("skl-01", "JAVA_CORE"),
            java.util.Map.entry("skl-02", "SPRING_BOOT"),
            java.util.Map.entry("skl-03", "SQL"),
            java.util.Map.entry("skl-04", "GIT"),
            java.util.Map.entry("skl-05", "SPRING_BOOT"),
            java.util.Map.entry("skl-06", "HTML_CSS"),
            java.util.Map.entry("skl-07", "JAVASCRIPT"),
            java.util.Map.entry("skl-08", "REACT"),
            java.util.Map.entry("skl-09", "REACT"),
            java.util.Map.entry("skl-10", "DOCKER"),
            java.util.Map.entry("skl-11", "ENGLISH"),
            java.util.Map.entry("skl-12", "PYTHON"),
            java.util.Map.entry("skl-13", "DESIGN_PATTERNS"),
            java.util.Map.entry("skl-14", "SQL"),
            java.util.Map.entry("skl-15", "CI_CD"),
            java.util.Map.entry("skl-16", "TESTING"),
            java.util.Map.entry("skl-17", "CI_CD"),
            java.util.Map.entry("skl-18", "DOCKER"),
            java.util.Map.entry("skl-19", "HTML_CSS"),
            java.util.Map.entry("skl-20", "TEAMWORK")
    );

    private static CatalogSkill createSkill(String code, String name, String group) {
        String id = UUID.nameUUIDFromBytes(("SKILL:" + code).getBytes(StandardCharsets.UTF_8)).toString();
        return new CatalogSkill(id, code, name, group);
    }

    @Override
    public List<CatalogSkill> listSkills() {
        return SKILLS;
    }

    @Override
    public Optional<CatalogSkill> findByIdOrCode(String idOrCode) {
        if (idOrCode == null || idOrCode.isBlank()) {
            return Optional.empty();
        }
        String target = idOrCode.trim();

        // 1. Khớp trực tiếp theo UUID id hoặc code
        Optional<CatalogSkill> direct = SKILLS.stream()
                .filter(s -> s.id().equalsIgnoreCase(target) || s.code().equalsIgnoreCase(target))
                .findFirst();
        if (direct.isPresent()) {
            return direct;
        }

        // 2. Khớp theo mã mock skl-XX
        String mappedCode = MOCK_ID_MAP.get(target.toLowerCase());
        if (mappedCode != null) {
            return SKILLS.stream().filter(s -> s.code().equalsIgnoreCase(mappedCode)).findFirst();
        }

        // 3. Khớp theo tên hiển thị
        return SKILLS.stream()
                .filter(s -> s.name().equalsIgnoreCase(target))
                .findFirst();
    }
}

