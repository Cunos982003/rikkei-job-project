package com.example.learningservice.repository;

import com.example.learningservice.model.entity.CandidateProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:candidate_profile_repo_test_db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@ActiveProfiles("test")
@Transactional
class CandidateProfileRepositoryTest {

    @Autowired
    private CandidateProfileRepository candidateProfileRepository;

    @Test
    @DisplayName("Lưu và truy vấn chính xác danh sách hồ sơ học viên theo lớp (classCode)")
    void testFindByClassCode() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        CandidateProfile p1 = CandidateProfile.builder()
                .userId(u1)
                .fullName("Nguyễn Văn An")
                .email("an@rikkeijobs.test")
                .phone("0912345600")
                .classCode("RA-JAVA-2026")
                .courseName("Khóa đào tạo Kỹ sư Phần mềm Java")
                .createdAt(Instant.now())
                .build();

        CandidateProfile p2 = CandidateProfile.builder()
                .userId(u2)
                .fullName("Trần Thị Bình")
                .email("binh@rikkeijobs.test")
                .phone("0912345700")
                .classCode("RA-FE-2026")
                .courseName("Khóa đào tạo Chuyên viên Frontend React")
                .createdAt(Instant.now())
                .build();

        CandidateProfile p3 = CandidateProfile.builder()
                .userId(u3)
                .fullName("Lương Văn Sơn")
                .email("son@rikkeijobs.test")
                .phone("0912345800")
                .classCode("RA-JAVA-2026")
                .courseName("Khóa đào tạo Kỹ sư Phần mềm Java")
                .createdAt(Instant.now())
                .build();

        candidateProfileRepository.saveAll(List.of(p1, p2, p3));

        List<CandidateProfile> javaClass = candidateProfileRepository.findByClassCode("RA-JAVA-2026");
        assertThat(javaClass).hasSize(2);
        assertThat(javaClass).extracting(CandidateProfile::getFullName)
                .containsExactlyInAnyOrder("Nguyễn Văn An", "Lương Văn Sơn");

        List<CandidateProfile> feClass = candidateProfileRepository.findByClassCode("RA-FE-2026");
        assertThat(feClass).hasSize(1);
        assertThat(feClass.get(0).getFullName()).isEqualTo("Trần Thị Bình");

        List<CandidateProfile> nonExistent = candidateProfileRepository.findByClassCode("RA-UNKNOWN-9999");
        assertThat(nonExistent).isEmpty();
    }

    @Test
    @DisplayName("Lọc theo tên khoá đào tạo không phân biệt hoa thường (courseName)")
    void testFindByCourseNameContaining() {
        CandidateProfile p = CandidateProfile.builder()
                .userId(UUID.randomUUID())
                .fullName("Lê Hoàng Cường")
                .email("cuong@rikkeijobs.test")
                .classCode("RA-FS-2026")
                .courseName("Khóa đào tạo Kỹ sư Phần mềm Fullstack")
                .createdAt(Instant.now())
                .build();
        candidateProfileRepository.save(p);

        List<CandidateProfile> results = candidateProfileRepository.findByCourseNameContainingIgnoreCase("fullstack");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFullName()).isEqualTo("Lê Hoàng Cường");
    }
}
