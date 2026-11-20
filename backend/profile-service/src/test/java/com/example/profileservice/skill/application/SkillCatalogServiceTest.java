package com.example.profileservice.skill.application;

import com.example.profileservice.skill.api.dto.SkillCatalogResponse;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillCatalogServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillCatalogService skillCatalogService;

    private List<Skill> mockSkills;

    @BeforeEach
    void setUp() {
        mockSkills = List.of(
                Skill.builder().id(UUID.randomUUID()).code("SQL").name("SQL Database").group("DATABASE").build(),
                Skill.builder().id(UUID.randomUUID()).code("SPRING_BOOT").name("Spring Boot").group("BACKEND").build(),
                Skill.builder().id(UUID.randomUUID()).code("JAVA_CORE").name("Java Core").group("BACKEND").build(),
                Skill.builder().id(UUID.randomUUID()).code("REACT").name("ReactJS").group("FRONTEND").build()
        );
    }

    @Test
    @DisplayName("getSkills sắp xếp theo group ASC rồi name ASC")
    void getSkills_sortedByGroupThenName() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        List<SkillCatalogResponse> result = skillCatalogService.getSkills(null, null);

        assertThat(result).hasSize(4);
        // Group BACKEND comes first, then Java Core before Spring Boot
        assertThat(result.get(0).code()).isEqualTo("JAVA_CORE");
        assertThat(result.get(1).code()).isEqualTo("SPRING_BOOT");
        assertThat(result.get(2).code()).isEqualTo("SQL");
        assertThat(result.get(3).code()).isEqualTo("REACT");
    }

    @Test
    @DisplayName("getSkills lọc theo group không phân biệt hoa thường")
    void getSkills_filterByGroup() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        List<SkillCatalogResponse> result = skillCatalogService.getSkills("backend", null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SkillCatalogResponse::code).containsExactly("JAVA_CORE", "SPRING_BOOT");
    }

    @Test
    @DisplayName("getSkills tìm kiếm theo tên hoặc code không phân biệt hoa thường")
    void getSkills_searchByQuery() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        List<SkillCatalogResponse> result = skillCatalogService.getSkills(null, "java");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("JAVA_CORE");

        List<SkillCatalogResponse> result2 = skillCatalogService.getSkills(null, "sql");
        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).code()).isEqualTo("SQL");
    }
}

