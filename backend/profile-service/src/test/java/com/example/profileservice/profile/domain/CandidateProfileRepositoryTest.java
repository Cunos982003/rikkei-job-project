package com.example.profileservice.profile.domain;

import com.example.profileservice.profile.infrastructure.CandidateProfileRepository;
import com.example.profileservice.profile.infrastructure.EducationRepository;
import com.example.profileservice.profile.infrastructure.CertificateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.profileservice.profile.domain.CandidateProfile.Visibility;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class CandidateProfileRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.4")
            .withDatabaseName("profile_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    CandidateProfileRepository candidateProfileRepo;

    @Autowired
    EducationRepository educationRepo;

    @Autowired
    CertificateRepository certificateRepo;

    private static final UUID TEST_USER_ID = UUID.randomUUID();

    @Test
    void candidate_profile_crud() {
        CandidateProfile p = CandidateProfile.builder()
                .userId(TEST_USER_ID)
                .fullName("Test User")
                .build();
        p = candidateProfileRepo.save(p);
        assertThat(p.getUserId()).isNotNull();
        assertThat(p.getVisibility()).isEqualTo(Visibility.PRIVATE);
    }

    @Test
    void education_crud() {
        Education e = Education.builder()
                .userId(TEST_USER_ID)
                .school("Test University")
                .major("Computer Science")
                .build();
        e = educationRepo.save(e);
        assertThat(e.getId()).isNotNull();
        assertThat(e.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    void certificate_crud() {
        Certificate c = Certificate.builder()
                .userId(TEST_USER_ID)
                .name("Test Certificate")
                .issuer("Test Issuer")
                .build();
        c = certificateRepo.save(c);
        assertThat(c.getId()).isNotNull();
        assertThat(c.getUserId()).isEqualTo(TEST_USER_ID);
    }

    @Test
    void candidate_profile_findByUserId() {
        CandidateProfile p = CandidateProfile.builder()
                .userId(TEST_USER_ID)
                .fullName("Test User")
                .build();
        candidateProfileRepo.save(p);

        CandidateProfile found = candidateProfileRepo.findByUserId(TEST_USER_ID).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getFullName()).isEqualTo("Test User");
    }

    @Test
    void education_findByUserId() {
        Education e1 = Education.builder()
                .userId(TEST_USER_ID)
                .school("University 1")
                .major("Major 1")
                .build();
        educationRepo.save(e1);

        Education e2 = Education.builder()
                .userId(TEST_USER_ID)
                .school("University 2")
                .major("Major 2")
                .build();
        educationRepo.save(e2);

        var found = educationRepo.findByUserId(TEST_USER_ID);
        assertThat(found).hasSize(2);
    }

    @Test
    void certificate_findByUserId() {
        Certificate c1 = Certificate.builder()
                .userId(TEST_USER_ID)
                .name("Cert 1")
                .issuer("Issuer 1")
                .build();
        certificateRepo.save(c1);

        Certificate c2 = Certificate.builder()
                .userId(TEST_USER_ID)
                .name("Cert 2")
                .issuer("Issuer 2")
                .build();
        certificateRepo.save(c2);

        var found = certificateRepo.findByUserId(TEST_USER_ID);
        assertThat(found).hasSize(2);
    }

    @Test
    void candidate_profile_full_fields() {
        CandidateProfile p = CandidateProfile.builder()
                .userId(TEST_USER_ID)
                .fullName("Full Name")
                .dob(LocalDate.of(1990, 1, 1))
                .gender("Male")
                .phone("123456789")
                .email("test@example.com")
                .address("123 Test St")
                .avatarUrl("https://example.com/avatar.jpg")
                .headline("Senior Developer")
                .summary("Summary text")
                .program("CS Program")
                .classCode("CS101")
                .courseName("Computer Science")
                .visibility(Visibility.PUBLIC)
                .build();
        p = candidateProfileRepo.save(p);

        CandidateProfile found = candidateProfileRepo.findByUserId(TEST_USER_ID).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getFullName()).isEqualTo("Full Name");
        assertThat(found.getDob()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(found.getGender()).isEqualTo("Male");
        assertThat(found.getPhone()).isEqualTo("123456789");
        assertThat(found.getEmail()).isEqualTo("test@example.com");
        assertThat(found.getAddress()).isEqualTo("123 Test St");
        assertThat(found.getAvatarUrl()).isEqualTo("https://example.com/avatar.jpg");
        assertThat(found.getHeadline()).isEqualTo("Senior Developer");
        assertThat(found.getSummary()).isEqualTo("Summary text");
        assertThat(found.getProgram()).isEqualTo("CS Program");
        assertThat(found.getClassCode()).isEqualTo("CS101");
        assertThat(found.getCourseName()).isEqualTo("Computer Science");
        assertThat(found.getVisibility()).isEqualTo(Visibility.PUBLIC);
    }
}