package com.example.profileservice.profile.integration;

import com.example.profileservice.profile.api.dto.LmsStudentDto;
import com.example.profileservice.profile.infrastructure.LmsClient;
import com.example.profileservice.skill.domain.Skill;
import com.example.profileservice.skill.infrastructure.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Luồng HS-02 đầu-cuối: hồ sơ tự tạo từ LMS → học vấn → chứng chỉ → tệp đính kèm →
 * kỹ năng tự đánh giá (source=SELF) → hiển thị trong hồ sơ kỹ năng HS-01.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
class ProfileIntegrationTest {

    static final Path UPLOAD_DIR;

    static {
        try {
            UPLOAD_DIR = Files.createTempDirectory("hs02-uploads");
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được thư mục upload tạm", e);
        }
    }

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
        registry.add("app.upload.dir", UPLOAD_DIR::toString);
    }

    @MockitoBean
    LmsClient lmsClient;

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    SkillRepository skillRepo;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private static byte[] pdfBytes() {
        byte[] content = new byte[256];
        System.arraycopy(new byte[]{0x25, 0x50, 0x44, 0x46}, 0, content, 0, 4);
        return content;
    }

    @Test
    void profile_is_auto_created_from_lms_on_first_access() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "Nguyễn Văn An", "an@rikkeijobs.test",
                "Java Fullstack", "RA-JAVA-2026", "Lập trình Java Fullstack")));

        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.email").value("an@rikkeijobs.test"))
                .andExpect(jsonPath("$.program").value("Java Fullstack"))
                .andExpect(jsonPath("$.classCode").value("RA-JAVA-2026"))
                .andExpect(jsonPath("$.courseName").value("Lập trình Java Fullstack"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test
    void profile_endpoints_require_identity_header() throws Exception {
        mvc.perform(get("/api/v1/profiles/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/profiles/me/educations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/profiles/me/certificates")).andExpect(status().isUnauthorized());
    }

    @Test
    void updating_profile_keeps_lms_fields_and_applies_editable_ones() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "Nguyễn Văn An", "an@rikkeijobs.test", "Java Fullstack", "RA-JAVA-2026", "Java Course")));
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk());

        // Client tries to overwrite LMS-owned identity fields
        String body = """
                {
                  "phone": "0987654321",
                  "address": "Hà Nội",
                  "headline": "Java Backend Developer",
                  "visibility": "PUBLIC",
                  "fullName": "HACKED",
                  "email": "attacker@evil.test",
                  "program": "HACKED PROGRAM",
                  "classCode": "HACKED-CLASS",
                  "courseName": "HACKED COURSE"
                }
                """;

        mvc.perform(put("/api/v1/profiles/me")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("0987654321"))
                .andExpect(jsonPath("$.address").value("Hà Nội"))
                .andExpect(jsonPath("$.headline").value("Java Backend Developer"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                // LMS-owned fields must survive untouched
                .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.email").value("an@rikkeijobs.test"))
                .andExpect(jsonPath("$.program").value("Java Fullstack"))
                .andExpect(jsonPath("$.classCode").value("RA-JAVA-2026"))
                .andExpect(jsonPath("$.courseName").value("Java Course"));
    }

    @Test
    void partial_update_does_not_wipe_other_fields() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "An", "an@rikkeijobs.test", "Java", "RA-1", "Java Course")));
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()));

        mvc.perform(put("/api/v1/profiles/me")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0900000000\",\"headline\":\"Dev\"}"))
                .andExpect(status().isOk());

        // A visibility-only update must leave phone and headline alone
        mvc.perform(put("/api/v1/profiles/me")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("0900000000"))
                .andExpect(jsonPath("$.headline").value("Dev"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @Test
    void education_crud_round_trip() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "An", "an@rikkeijobs.test", "Java", "RA-1", "Java Course")));
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()));

        String created = mvc.perform(post("/api/v1/profiles/me/educations")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"school":"Đại học Bách Khoa Hà Nội","major":"Kỹ thuật phần mềm",
                                 "fromDate":"2021-09-01","toDate":"2025-06-30","gpa":3.45,"note":"Học bổng"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.school").value("Đại học Bách Khoa Hà Nội"))
                .andExpect(jsonPath("$.fromDate").value("2021-09-01"))
                .andExpect(jsonPath("$.gpa").value(3.45))
                .andReturn().getResponse().getContentAsString();

        String id = created.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/v1/profiles/me/educations").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(put("/api/v1/profiles/me/educations/" + id)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"school":"Đại học Quốc gia","major":"CNTT",
                                 "fromDate":"2021-09-01","toDate":"2025-06-30","gpa":3.9,"note":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.school").value("Đại học Quốc gia"))
                .andExpect(jsonPath("$.gpa").value(3.9));

        mvc.perform(delete("/api/v1/profiles/me/educations/" + id).header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/profiles/me/educations").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void certificate_crud_round_trip() throws Exception {
        UUID userId = UUID.randomUUID();
        when(lmsClient.getStudent(any())).thenReturn(Mono.just(new LmsStudentDto(
                "An", "an@rikkeijobs.test", "Java", "RA-1", "Java Course")));
        mvc.perform(get("/api/v1/profiles/me").header("X-User-Id", userId.toString()));

        String created = mvc.perform(post("/api/v1/profiles/me/certificates")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"OCP Java SE 11","issuer":"Oracle","issuedAt":"2026-05-20",
                                 "expiresAt":null,"fileUrl":"/files/x.pdf","fileType":"PDF"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issuer").value("Oracle"))
                .andExpect(jsonPath("$.issuedAt").value("2026-05-20"))
                .andReturn().getResponse().getContentAsString();

        String id = created.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(put("/api/v1/profiles/me/certificates/" + id)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"OCP Java SE 17","issuer":"Oracle","issuedAt":"2026-05-20"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("OCP Java SE 17"));

        mvc.perform(delete("/api/v1/profiles/me/certificates/" + id).header("X-User-Id", userId.toString()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/profiles/me/certificates").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void uploaded_file_can_be_downloaded_back_unchanged() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "certificate.pdf", "application/pdf", pdfBytes());

        String body = mvc.perform(multipart("/api/v1/files").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", startsWith("/files/")))
                .andReturn().getResponse().getContentAsString();

        String storedName = body.replaceAll(".*\"/files/([^\"]+)\".*", "$1");

        mvc.perform(get("/api/v1/files/" + storedName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfBytes()));
    }

    @Test
    void self_assessed_skill_shows_up_in_hs01_skill_profile() throws Exception {
        UUID userId = UUID.randomUUID();
        Skill javaCore = skillRepo.findByCode("JAVA_CORE").orElseThrow();

        mvc.perform(put("/api/v1/profiles/me/self-skills")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"skillId\":\"" + javaCore.getId() + "\",\"selfScore\":88}]"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/profiles/me/skills").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].selfScore", hasItem(88)))
                // self-declared only — never a verified score
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].verifiedScore", hasItem(nullValue())))
                .andExpect(jsonPath("$[?(@.skillCode=='JAVA_CORE')].evidences[?(@.source=='SELF')].score", hasItem(88)));
    }

}
