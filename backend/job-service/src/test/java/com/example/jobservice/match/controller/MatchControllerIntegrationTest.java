package com.example.jobservice.match.controller;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.job.domain.*;
import com.example.jobservice.job.dto.JobSkillItem;
import com.example.jobservice.match.client.CandidateSkillScore;
import com.example.jobservice.match.client.StubCandidateSkillClient;
import com.example.jobservice.match.domain.MatchScore;
import com.example.jobservice.match.domain.MatchScoreId;
import com.example.jobservice.match.domain.MatchScoreRepository;
import com.example.jobservice.security.JwtTokenProvider;
import com.example.jobservice.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class MatchControllerIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private CompanyRepository companyRepository;

        @Autowired
        private JobRepository jobRepository;

        @Autowired
        private JobSkillRepository jobSkillRepository;

        @Autowired
        private MatchScoreRepository matchScoreRepository;

        @Autowired
        private StubCandidateSkillClient candidateSkillClient;

        @Autowired
        private JwtTokenProvider tokenProvider;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private String hr1Token;
        private String hr2Token;
        private String candidateToken;

        private UUID companyAId;
        private UUID companyBId;
        private UUID candidateUserId;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build();

                candidateSkillClient.clear();

                Company companyA = companyRepository.findByTaxCode("0101234567")
                                .orElseGet(() -> {
                                        Company c = new Company();
                                        c.setName("Công ty Cổ phần Rikkeisoft");
                                        c.setTaxCode("0101234567");
                                        return companyRepository.save(c);
                                });
                companyAId = companyA.getId();

                Company companyB = companyRepository.findByTaxCode("0107654321")
                                .orElseGet(() -> {
                                        Company c = new Company();
                                        c.setName("Rikkei Digital Solutions");
                                        c.setTaxCode("0107654321");
                                        return companyRepository.save(c);
                                });
                companyBId = companyB.getId();

                hr1Token = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                UUID.randomUUID(),
                                                "hr1@rikkeijobs.test",
                                                "HR Account One",
                                                "ROLE_HR",
                                                companyAId));

                hr2Token = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                UUID.randomUUID(),
                                                "hr2@rikkeijobs.test",
                                                "HR Account Two",
                                                "ROLE_HR",
                                                companyBId));

                candidateUserId = UUID.randomUUID();
                candidateToken = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                candidateUserId,
                                                "candidate@test.com",
                                                "Nguyen Van Ung Vien",
                                                "ROLE_CANDIDATE",
                                                null));
        }

        private Job createJob(UUID companyId, JobStatus status, String title) {
                Job job = new Job();
                job.setCompanyId(companyId);
                job.setTitle(title);
                job.setPosition(title);
                job.setLevel(JobLevel.JUNIOR);
                job.setType(JobType.FULLTIME);
                job.setQuantity(2);
                job.setCity("Hà Nội");
                job.setWorkMode(WorkMode.ONSITE);
                job.setDeadline(LocalDate.now().plusDays(15));
                job.setStatus(status);
                job.setCreatedBy("hr1@rikkeijobs.test");
                return jobRepository.save(job);
        }

        private void addJobSkill(UUID jobId, String skillId, String skillCode, String skillName, boolean required,
                        int minScore) {
                JobSkill skill = new JobSkill(jobId, skillId, skillCode, skillName, required, minScore);
                jobSkillRepository.save(skill);
        }

        @Test
        @DisplayName("1. Quyền truy cập: HR xem JD công ty mình -> 200, JD công ty khác -> 404; CANDIDATE gọi /match/{userId} -> 403; HR gọi /match/me -> 403")
        void testAccessControl_matrix() throws Exception {
                Job jobA = createJob(companyAId, JobStatus.APPROVED, "Backend Dev Công ty A");
                addJobSkill(jobA.getId(), "skl-01", "JAVA_CORE", "Java Core", true, 60);

                Job jobB = createJob(companyBId, JobStatus.APPROVED, "Backend Dev Công ty B");
                addJobSkill(jobB.getId(), "skl-01", "JAVA_CORE", "Java Core", true, 60);

                candidateSkillClient.setSkills(candidateUserId, List.of(
                                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", 70, null)));

                // HR công ty A xem JD công ty A -> 200
                mockMvc.perform(get("/jobs/" + jobA.getId() + "/match/" + candidateUserId)
                                .header("Authorization", hr1Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.jobId").value(jobA.getId().toString()))
                                .andExpect(jsonPath("$.data.userId").value(candidateUserId.toString()));

                // HR công ty A xem JD công ty B -> 404 (bảo mật, không tìm thấy)
                mockMvc.perform(get("/jobs/" + jobB.getId() + "/match/" + candidateUserId)
                                .header("Authorization", hr1Token))
                                .andExpect(status().isNotFound());

                // CANDIDATE gọi /jobs/{id}/match/{userId} -> 403
                mockMvc.perform(get("/jobs/" + jobA.getId() + "/match/" + candidateUserId)
                                .header("Authorization", candidateToken))
                                .andExpect(status().isForbidden());

                // HR gọi /jobs/{id}/match/me -> 403
                mockMvc.perform(get("/jobs/" + jobA.getId() + "/match/me")
                                .header("Authorization", hr1Token))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("2. CANDIDATE gọi /match/me trên JD APPROVED -> 200; trên JD DRAFT -> 404")
        void testCandidate_matchMe_approvedVsDraft() throws Exception {
                Job approvedJob = createJob(companyAId, JobStatus.APPROVED, "Approved Java Job");
                addJobSkill(approvedJob.getId(), "skl-01", "JAVA_CORE", "Java Core", true, 60);

                Job draftJob = createJob(companyAId, JobStatus.DRAFT, "Draft Java Job");
                addJobSkill(draftJob.getId(), "skl-01", "JAVA_CORE", "Java Core", true, 60);

                candidateSkillClient.setSkills(candidateUserId, List.of(
                                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", 80, null)));

                // Trên JD APPROVED -> 200
                mockMvc.perform(get("/jobs/" + approvedJob.getId() + "/match/me")
                                .header("Authorization", candidateToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.score").value(100.0))
                                .andExpect(jsonPath("$.data.belowMinimum").value(false));

                // Trên JD DRAFT -> 404
                mockMvc.perform(get("/jobs/" + draftJob.getId() + "/match/me")
                                .header("Authorization", candidateToken))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("3. Đổi minScore JD (PUT /jobs/{id}/skills) -> điểm MỚI khác điểm cũ, stale chuyển true sau khi đổi và false sau khi tính lại")
        void testStaleFlagAndRecalculation_onSkillChange() throws Exception {
                Job job = createJob(companyAId, JobStatus.DRAFT, "JD đổi kỹ năng");
                addJobSkill(job.getId(), "skl-01", "JAVA_CORE", "Java Core", true, 50);

                // Ứng viên verifiedScore 50
                candidateSkillClient.setSkills(candidateUserId, List.of(
                                new CandidateSkillScore("skl-01", "JAVA_CORE", "Java Core", 50, null)));

                // Lần 1: Tính match -> 50/50 = 100.0
                mockMvc.perform(get("/jobs/" + job.getId() + "/match/" + candidateUserId)
                                .header("Authorization", hr1Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.score").value(100.0))
                                .andExpect(jsonPath("$.data.stale").value(false));

                MatchScoreId scoreId = new MatchScoreId(job.getId(), candidateUserId);
                MatchScore recordBefore = matchScoreRepository.findById(scoreId).orElseThrow();
                assertFalse(recordBefore.isStale(), "Bản ghi phải có stale = false sau khi tính");
                assertEquals(100.0, recordBefore.getScore().doubleValue());

                // Đổi minScore lên 100 qua PUT /jobs/{id}/skills
                List<JobSkillItem> updatedSkills = List.of(
                                new JobSkillItem("JAVA_CORE", true, 100));

                mockMvc.perform(put("/jobs/" + job.getId() + "/skills")
                                .header("Authorization", hr1Token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedSkills)))
                                .andExpect(status().isOk());

                // Kiểm tra stale=true trong database ngay sau khi cập nhật kỹ năng
                MatchScore recordAfterUpdate = matchScoreRepository.findById(scoreId).orElseThrow();
                assertTrue(recordAfterUpdate.isStale(),
                                "Bản ghi phải được đánh stale = true ngay sau khi đổi kỹ năng JD");

                // Lần 2: Gọi lại match -> LUÔN tính lại, trả điểm mới 50.0 (50 / 100)
                mockMvc.perform(get("/jobs/" + job.getId() + "/match/" + candidateUserId)
                                .header("Authorization", hr1Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.score").value(50.0))
                                .andExpect(jsonPath("$.data.stale").value(false));

                // Kiểm tra stale đã được reset về false sau khi tính lại
                MatchScore recordAfterRecalculation = matchScoreRepository.findById(scoreId).orElseThrow();
                assertFalse(recordAfterRecalculation.isStale(),
                                "Bản ghi phải được reset stale = false sau khi tính lại");
                assertEquals(50.0, recordAfterRecalculation.getScore().doubleValue());
        }

        @Test
        @DisplayName("4. Ứng viên không có kỹ năng nào -> 200, score 0.0, belowMinimum=true, missing liệt kê đủ")
        void testCandidate_noSkills_returnsZeroScoreAndFullMissing() throws Exception {
                Job job = createJob(companyAId, JobStatus.APPROVED, "JD nhiều kỹ năng");
                addJobSkill(job.getId(), "skl-01", "JAVA_CORE", "Java Core", true, 70);
                addJobSkill(job.getId(), "skl-02", "SPRING_BOOT", "Spring Boot", true, 60);
                addJobSkill(job.getId(), "skl-04", "GIT", "Git", false, 50);

                // Không cài đặt kỹ năng cho ứng viên (mặc định mảng rỗng)
                candidateSkillClient.clear();

                mockMvc.perform(get("/jobs/" + job.getId() + "/match/" + candidateUserId)
                                .header("Authorization", hr1Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.score").value(0.0))
                                .andExpect(jsonPath("$.data.belowMinimum").value(true))
                                .andExpect(jsonPath("$.data.met", hasSize(0)))
                                .andExpect(jsonPath("$.data.missing", hasSize(3)))
                                .andExpect(jsonPath("$.data.missing[0].skillCode").value("JAVA_CORE"))
                                .andExpect(jsonPath("$.data.missing[0].gap").value(70))
                                .andExpect(jsonPath("$.data.missing[1].skillCode").value("SPRING_BOOT"))
                                .andExpect(jsonPath("$.data.missing[1].gap").value(60))
                                .andExpect(jsonPath("$.data.missing[2].skillCode").value("GIT"))
                                .andExpect(jsonPath("$.data.missing[2].gap").value(50));
        }
}
