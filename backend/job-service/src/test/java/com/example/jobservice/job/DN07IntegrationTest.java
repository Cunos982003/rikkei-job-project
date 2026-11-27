package com.example.jobservice.job;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.AuthHrAccount;
import com.example.jobservice.auth.client.StubAuthClient;
import com.example.jobservice.company.domain.*;
import com.example.jobservice.company.dto.LockCompanyRequest;
import com.example.jobservice.job.domain.*;
import com.example.jobservice.job.dto.CreateJobRequest;
import com.example.jobservice.job.dto.JobSkillItem;
import com.example.jobservice.job.service.JobService;
import com.example.jobservice.security.JwtTokenProvider;
import com.example.jobservice.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class DN07IntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyActionRepository companyActionRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private Clock clock;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID staffId;
    private String staffToken;
    private String candidateToken;
    private String hrToken;
    private UUID companyAId;
    private UUID companyBId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        staffId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        staffToken = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(staffId, "staff@rikkeijobs.test", "Staff Officer", "ROLE_STAFF", null));

        candidateToken = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(UUID.randomUUID(), "candidate@test.com", "Candidate One", "ROLE_CANDIDATE", null));

        Company companyA = companyRepository.findByTaxCode("0101234567")
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setName("Công ty Cổ phần Rikkeisoft");
                    c.setTaxCode("0101234567");
                    c.setStatus(CompanyStatus.ACTIVE);
                    return companyRepository.save(c);
                });
        companyA.setStatus(CompanyStatus.ACTIVE);
        companyA = companyRepository.save(companyA);
        companyAId = companyA.getId();

        Company companyB = companyRepository.findByTaxCode("0107654321")
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setName("Rikkei Digital Solutions");
                    c.setTaxCode("0107654321");
                    c.setStatus(CompanyStatus.ACTIVE);
                    return companyRepository.save(c);
                });
        companyB.setStatus(CompanyStatus.ACTIVE);
        companyB = companyRepository.save(companyB);
        companyBId = companyB.getId();

        hrToken = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(UUID.randomUUID(), "hr1@rikkeijobs.test", "HR One", "ROLE_HR", companyAId));
    }

    private Job createJobWithStatus(UUID companyId, String title, JobStatus status, LocalDate deadline) {
        Job job = new Job();
        job.setCompanyId(companyId);
        job.setTitle(title);
        job.setPosition("Developer");
        job.setLevel(JobLevel.MIDDLE);
        job.setType(JobType.FULLTIME);
        job.setQuantity(2);
        job.setCity("Hà Nội");
        job.setAddress("Keangnam");
        job.setWorkMode(WorkMode.ONSITE);
        job.setDeadline(deadline != null ? deadline : LocalDate.now(clock).plusDays(20));
        job.setStatus(status);
        job.setCreatedBy("hr@test.com");
        return jobRepository.save(job);
    }

    @Test
    @DisplayName("a) Duyệt JD PENDING -> 200, status=APPROVED, reviewedBy/reviewedAt được lưu, có APPROVE_JD trong company_actions")
    void a_approvePendingJob_success() throws Exception {
        Job job = createJobWithStatus(companyAId, "Java Backend Engineer for Approval", JobStatus.PENDING, null);

        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/approve")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedBy").value(staffId.toString()))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(updated.getReviewedBy()).isEqualTo(staffId.toString());
        assertThat(updated.getReviewedAt()).isNotNull();

        List<CompanyAction> actions = companyActionRepository.findByCompanyId(companyAId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        boolean hasApproveAction = actions.stream()
                .anyMatch(a -> a.getAction() == CompanyActionType.APPROVE_JD
                        && job.getId().equals(a.getTargetId())
                        && staffId.toString().equals(a.getStaffId()));
        assertThat(hasApproveAction).isTrue();
    }

    @Test
    @DisplayName("b) Duyệt JD DRAFT -> ném ConflictException 409")
    void b_approveDraftJob_throwsConflict() throws Exception {
        Job job = createJobWithStatus(companyAId, "Draft JD Approval Test", JobStatus.DRAFT, null);

        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/approve")
                        .header("Authorization", staffToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Chỉ có thể phê duyệt tin đang chờ duyệt")));
    }

    @Test
    @DisplayName("c) Trả lại JD không kèm lý do -> 400; kèm lý do -> 200 REJECTED, có REJECT_JD, HR resubmit -> PENDING")
    void c_rejectJobAndResubmitFlow() throws Exception {
        Job job = createJobWithStatus(companyAId, "JD To Reject", JobStatus.PENDING, null);

        // Trả lại không kèm lý do -> 400
        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/reject")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng nhập lý do trả lại tin tuyển dụng"));

        // Trả lại kèm lý do hợp lệ -> 200
        String reason = "Yêu cầu kỹ năng chưa rõ ràng và dải lương không phù hợp";
        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/reject")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", reason))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectedReason").value(reason))
                .andExpect(jsonPath("$.data.reviewedBy").value(staffId.toString()))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.REJECTED);
        assertThat(updated.getRejectedReason()).isEqualTo(reason);

        // HR gửi duyệt lại: thêm kỹ năng bắt buộc và gọi submit
        jobSkillRepository.save(new JobSkill(job.getId(), "JAVA_CORE", "JAVA_CORE", "Java Core", true, 70));

        mockMvc.perform(post("/jobs/" + job.getId() + "/submit")
                        .header("Authorization", hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.rejectedReason").doesNotExist());
    }

    @Test
    @DisplayName("d) Lọc GET /staff/jobs?status=PENDING&companyId=... trả đúng dữ liệu")
    void d_listStaffJobs_filterStatusAndCompanyId() throws Exception {
        Job jobA1 = createJobWithStatus(companyAId, "Pending Job CompA", JobStatus.PENDING, null);
        Job jobA2 = createJobWithStatus(companyAId, "Approved Job CompA", JobStatus.APPROVED, null);
        Job jobB1 = createJobWithStatus(companyBId, "Pending Job CompB", JobStatus.PENDING, null);

        mockMvc.perform(get("/staff/jobs")
                        .header("Authorization", staffToken)
                        .param("status", "PENDING")
                        .param("companyId", companyAId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.items[*].companyId", everyItem(equalTo(companyAId.toString()))))
                .andExpect(jsonPath("$.data.items[*].status", everyItem(equalTo("PENDING"))));
    }

    @Test
    @DisplayName("e) Khoá công ty không lý do -> 400; khoá có lý do -> 200, LOCKED, HR LOCKED; khoá lại -> 409; mở khoá -> ACTIVE, chỉ HR [COMPANY_LOCK] được mở")
    void e_lockAndUnlockCompany_cascadeHrFlow() throws Exception {
        if (authClient instanceof StubAuthClient stub) {
            stub.clear();
            // Seed 2 HR cho companyA
            UUID hr1Id = UUID.randomUUID();
            UUID hr2Id = UUID.randomUUID();

            stub.seedAccount(new AuthHrAccount(
                    hr1Id, "hr1@test.com", "HR Active", companyAId, "ACTIVE", null, null, false, OffsetDateTime.now(ZoneOffset.UTC), null, true));
            stub.seedAccount(new AuthHrAccount(
                    hr2Id, "hr2@test.com", "HR Individually Locked", companyAId, "LOCKED", "Vi phạm bảo mật riêng", OffsetDateTime.now(ZoneOffset.UTC), false, OffsetDateTime.now(ZoneOffset.UTC), null, true));

            // 1. Khoá không lý do -> 400
            mockMvc.perform(post("/staff/companies/" + companyAId + "/lock")
                            .header("Authorization", staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "   "))))
                    .andExpect(status().isBadRequest());

            // 2. Khoá có lý do -> 200, status=LOCKED
            String lockReason = "Công ty vi phạm chính sách đăng tin";
            mockMvc.perform(post("/staff/companies/" + companyAId + "/lock")
                            .header("Authorization", staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LockCompanyRequest(lockReason))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("LOCKED"));

            // Kiểm tra HR trong stub
            AuthHrAccount hr1AfterLock = stub.findById(hr1Id).orElseThrow();
            assertThat(hr1AfterLock.status()).isEqualTo("LOCKED");
            assertThat(hr1AfterLock.lockedReason()).isEqualTo("[COMPANY_LOCK] " + lockReason);

            AuthHrAccount hr2AfterLock = stub.findById(hr2Id).orElseThrow();
            assertThat(hr2AfterLock.status()).isEqualTo("LOCKED");
            assertThat(hr2AfterLock.lockedReason()).isEqualTo("Vi phạm bảo mật riêng");

            // 3. Khoá lại công ty đã LOCKED -> 409
            mockMvc.perform(post("/staff/companies/" + companyAId + "/lock")
                            .header("Authorization", staffToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LockCompanyRequest(lockReason))))
                    .andExpect(status().isConflict());

            // 4. Mở khoá công ty -> 200, status=ACTIVE
            mockMvc.perform(post("/staff/companies/" + companyAId + "/unlock")
                            .header("Authorization", staffToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

            // HR1 được mở khóa
            AuthHrAccount hr1AfterUnlock = stub.findById(hr1Id).orElseThrow();
            assertThat(hr1AfterUnlock.status()).isEqualTo("ACTIVE");

            // HR2 vẫn giữ LOCKED vì bị khóa riêng
            AuthHrAccount hr2AfterUnlock = stub.findById(hr2Id).orElseThrow();
            assertThat(hr2AfterUnlock.status()).isEqualTo("LOCKED");
            assertThat(hr2AfterUnlock.lockedReason()).isEqualTo("Vi phạm bảo mật riêng");
        }
    }

    @Test
    @DisplayName("f) Công ty bị khoá: JD APPROVED ẩn khỏi GET /jobs; GET /jobs/{id} bởi candidate -> 404; HR POST /jobs -> 403; mở khoá -> JD hiện lại")
    void f_lockedCompany_hidingAndBlockingFlow() throws Exception {
        Job job = createJobWithStatus(companyAId, "Public Job Hide Test", JobStatus.APPROVED, null);

        // Khi công ty ACTIVE:
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + job.getId() + "')]").isNotEmpty());

        mockMvc.perform(get("/jobs/" + job.getId())
                        .header("Authorization", candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(job.getId().toString()));

        // Khoá công ty A
        mockMvc.perform(post("/staff/companies/" + companyAId + "/lock")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockCompanyRequest("Khoá kiểm tra ẩn tin"))))
                .andExpect(status().isOk());

        // 1. GET /jobs không còn hiển thị JD của công ty bị khoá
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + job.getId() + "')]").isEmpty());

        // 2. Ứng viên gọi GET /jobs/{id} nhận 404
        mockMvc.perform(get("/jobs/" + job.getId())
                        .header("Authorization", candidateToken))
                .andExpect(status().isNotFound());

        // 3. HR của công ty bị khoá tạo JD -> 403 Forbidden
        CreateJobRequest createReq = new CreateJobRequest(
                "New Job When Locked", "Pos", JobLevel.JUNIOR, JobType.FULLTIME, 1,
                "HN", "Address", WorkMode.ONSITE, null, null, null, null, null,
                LocalDate.now(clock).plusDays(10));

        mockMvc.perform(post("/jobs")
                        .header("Authorization", hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Công ty đang bị khoá, không thể thực hiện thao tác"));

        // 4. Mở khoá công ty A
        mockMvc.perform(post("/staff/companies/" + companyAId + "/unlock")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk());

        // JD xuất hiện trở lại trên GET /jobs
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.id == '" + job.getId() + "')]").isNotEmpty());

        // Ứng viên xem lại được JD
        mockMvc.perform(get("/jobs/" + job.getId())
                        .header("Authorization", candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(job.getId().toString()));
    }

    @Test
    @DisplayName("g) GET /staff/companies/{id}/history trả đúng danh sách theo thứ tự mới nhất trước")
    void g_getCompanyHistory_returnsNewestFirst() throws Exception {
        // Thực hiện lock rồi unlock
        mockMvc.perform(post("/staff/companies/" + companyAId + "/lock")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockCompanyRequest("Lý do lịch sử 1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/staff/companies/" + companyAId + "/unlock")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/staff/companies/" + companyAId + "/history")
                        .header("Authorization", staffToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.items[0].action").value("UNLOCK"))
                .andExpect(jsonPath("$.data.items[1].action").value("LOCK"));
    }

    @Test
    @DisplayName("h) Tự động đóng tin hết hạn: APPROVED hạn hôm qua -> CLOSED; hôm nay & ngày mai -> APPROVED; DRAFT hết hạn -> DRAFT")
    void h_closeExpiredJobs_scenario() {
        LocalDate today = LocalDate.now(clock);

        Job expiredApproved = createJobWithStatus(companyAId, "Expired Approved Job", JobStatus.APPROVED, today.minusDays(1));
        Job todayApproved = createJobWithStatus(companyAId, "Today Approved Job", JobStatus.APPROVED, today);
        Job tomorrowApproved = createJobWithStatus(companyAId, "Tomorrow Approved Job", JobStatus.APPROVED, today.plusDays(1));
        Job expiredDraft = createJobWithStatus(companyAId, "Expired Draft Job", JobStatus.DRAFT, today.minusDays(1));

        int closedCount = jobService.closeExpiredJobs();
        assertThat(closedCount).isGreaterThanOrEqualTo(1);

        Job updatedExpiredApproved = jobRepository.findById(expiredApproved.getId()).orElseThrow();
        assertThat(updatedExpiredApproved.getStatus()).isEqualTo(JobStatus.CLOSED);

        Job updatedTodayApproved = jobRepository.findById(todayApproved.getId()).orElseThrow();
        assertThat(updatedTodayApproved.getStatus()).isEqualTo(JobStatus.APPROVED);

        Job updatedTomorrowApproved = jobRepository.findById(tomorrowApproved.getId()).orElseThrow();
        assertThat(updatedTomorrowApproved.getStatus()).isEqualTo(JobStatus.APPROVED);

        Job updatedExpiredDraft = jobRepository.findById(expiredDraft.getId()).orElseThrow();
        assertThat(updatedExpiredDraft.getStatus()).isEqualTo(JobStatus.DRAFT);
    }
}
