package com.example.jobservice.dn07;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.AuthHrAccount;
import com.example.jobservice.auth.client.StubAuthClient;
import com.example.jobservice.company.domain.*;
import com.example.jobservice.company.dto.LockCompanyRequest;
import com.example.jobservice.job.domain.*;
import com.example.jobservice.job.dto.CreateJobRequest;
import com.example.jobservice.job.dto.JobSkillItem;
import com.example.jobservice.job.dto.UpdateJobRequest;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class Dn07StaffJobAndCompanyIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private CompanyActionRepository companyActionRepository;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private JobService jobService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String staffToken;
    private String candidateToken;
    private UUID staffId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        staffId = UUID.randomUUID();
        staffToken = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(staffId, "staff@rikkeijobs.test", "Cán bộ tuyển dụng", "ROLE_STAFF", null));

        candidateToken = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(UUID.randomUUID(), "candidate@rikkeijobs.test", "Ứng viên", "ROLE_CANDIDATE", null));
    }

    private Company createTestCompany(String prefix, CompanyStatus status) {
        Company company = new Company();
        company.setName("Công ty " + prefix);
        // Random 10-digit tax code
        String taxCode = String.valueOf(System.currentTimeMillis()).substring(3);
        if (taxCode.length() > 10) taxCode = taxCode.substring(0, 10);
        while (taxCode.length() < 10) taxCode += "0";
        company.setTaxCode(taxCode);
        company.setStatus(status);
        return companyRepository.save(company);
    }

    private Job createTestJob(UUID companyId, JobStatus status, LocalDate deadline) {
        Job job = new Job();
        job.setCompanyId(companyId);
        job.setTitle("Kỹ sư phần mềm Java " + UUID.randomUUID().toString().substring(0, 6));
        job.setPosition("Backend Developer");
        job.setLevel(JobLevel.MIDDLE);
        job.setType(JobType.FULLTIME);
        job.setQuantity(2);
        job.setCity("Hà Nội");
        job.setAddress("Keangnam Landmark 72");
        job.setWorkMode(WorkMode.ONSITE);
        job.setSalaryMin(BigDecimal.valueOf(15000000));
        job.setSalaryMax(BigDecimal.valueOf(30000000));
        job.setDescription("Mô tả công việc chi tiết");
        job.setRequirements("Yêu cầu công việc");
        job.setBenefits("Quyền lợi chế độ");
        job.setDeadline(deadline != null ? deadline : LocalDate.now().plusDays(30));
        job.setStatus(status);
        job.setCreatedBy("hr@test.com");
        return jobRepository.save(job);
    }

    private String createHrToken(UUID companyId, String email) {
        return "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(UUID.randomUUID(), email, "HR Name", "ROLE_HR", companyId));
    }

    @Test
    @DisplayName("a) approve PENDING => 200, status APPROVED, reviewedBy/reviewedAt có giá trị, có 1 dòng company_actions APPROVE_JD")
    void testA_approvePendingJob() throws Exception {
        Company company = createTestCompany("Test A", CompanyStatus.ACTIVE);
        Job job = createTestJob(company.getId(), JobStatus.PENDING, LocalDate.now().plusDays(20));

        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/approve")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedBy").value("Cán bộ tuyển dụng"))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(updated.getReviewedBy()).isEqualTo("Cán bộ tuyển dụng");
        assertThat(updated.getReviewedAt()).isNotNull();

        List<CompanyAction> actions = companyActionRepository.findAll().stream()
                .filter(a -> a.getCompanyId().equals(company.getId()) && a.getAction() == CompanyActionType.APPROVE_JD)
                .toList();
        assertThat(actions).hasSize(1);
        CompanyAction action = actions.get(0);
        assertThat(action.getTargetId()).isEqualTo(job.getId());
        assertThat(action.getStaffId()).isEqualTo("Cán bộ tuyển dụng");
        assertThat(action.getReason()).isNull();
    }

    @Test
    @DisplayName("b) approve DRAFT => 409")
    void testB_approveDraftJob() throws Exception {
        Company company = createTestCompany("Test B", CompanyStatus.ACTIVE);
        Job job = createTestJob(company.getId(), JobStatus.DRAFT, LocalDate.now().plusDays(20));

        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/approve")
                        .header("Authorization", staffToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("c) reject không lý do / lý do rỗng => 400; có lý do => 200, REJECTED; HR sửa rồi gửi lại được (REJECTED -> PENDING)")
    void testC_rejectJobAndResubmit() throws Exception {
        Company company = createTestCompany("Test C", CompanyStatus.ACTIVE);
        Job job = createTestJob(company.getId(), JobStatus.PENDING, LocalDate.now().plusDays(20));
        String hrToken = createHrToken(company.getId(), "hr_c@rikkeijobs.test");

        // 1. Reject without reason => 400
        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/reject")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng nhập lý do trả lại tin tuyển dụng"));

        // 2. Reject with blank reason => 400
        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/reject")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "   "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng nhập lý do trả lại tin tuyển dụng"));

        // 3. Reject with valid reason => 200
        String reason = "Mô tả công việc và mức lương chưa phù hợp với yêu cầu thực tế";
        mockMvc.perform(post("/staff/jobs/" + job.getId() + "/reject")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", reason))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectedReason").value(reason))
                .andExpect(jsonPath("$.data.reviewedBy").value("Cán bộ tuyển dụng"))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty());

        Job rejectedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(rejectedJob.getStatus()).isEqualTo(JobStatus.REJECTED);
        assertThat(rejectedJob.getRejectedReason()).isEqualTo(reason);

        List<CompanyAction> actions = companyActionRepository.findAll().stream()
                .filter(a -> a.getCompanyId().equals(company.getId()) && a.getAction() == CompanyActionType.REJECT_JD)
                .toList();
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getReason()).isEqualTo(reason);
        assertThat(actions.get(0).getTargetId()).isEqualTo(job.getId());

        // 4. Add a required skill to the job so HR can submit
        jobSkillRepository.save(new JobSkill(job.getId(), "JAVA", "JAVA", "Java", true, 0));

        // 5. HR edits and resubmits the rejected job
        UpdateJobRequest updateRequest = new UpdateJobRequest(
                "Kỹ sư phần mềm Java cập nhật",
                "Backend Developer",
                JobLevel.SENIOR,
                JobType.FULLTIME,
                3,
                "Hà Nội",
                "Keangnam",
                WorkMode.HYBRID,
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(35000000),
                "Mô tả đã được cập nhật chi tiết hơn",
                "Yêu cầu 3 năm kinh nghiệm Java",
                "Lương thưởng hấp dẫn",
                LocalDate.now().plusDays(25)
        );
        mockMvc.perform(put("/jobs/" + job.getId())
                        .header("Authorization", hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // Resubmit: REJECTED -> PENDING
        mockMvc.perform(post("/jobs/" + job.getId() + "/submit")
                        .header("Authorization", hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        Job resubmitted = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(resubmitted.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(resubmitted.getRejectedReason()).isNull();
    }

    @Test
    @DisplayName("d) GET /staff/jobs?status=PENDING&companyId= lọc đúng")
    void testD_listStaffJobsFiltering() throws Exception {
        Company company1 = createTestCompany("Filter 1", CompanyStatus.ACTIVE);
        Company company2 = createTestCompany("Filter 2", CompanyStatus.ACTIVE);

        Job job1Pending = createTestJob(company1.getId(), JobStatus.PENDING, LocalDate.now().plusDays(10));
        Job job1Approved = createTestJob(company1.getId(), JobStatus.APPROVED, LocalDate.now().plusDays(10));
        Job job2Pending = createTestJob(company2.getId(), JobStatus.PENDING, LocalDate.now().plusDays(10));

        // Filter by companyId and status
        mockMvc.perform(get("/staff/jobs")
                        .param("status", "PENDING")
                        .param("companyId", company1.getId().toString())
                        .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(job1Pending.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].companyId").value(company1.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"));

        // Filter only by companyId
        mockMvc.perform(get("/staff/jobs")
                        .param("companyId", company1.getId().toString())
                        .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(2)));
    }

    @Test
    @DisplayName("e) lock công ty không lý do => 400; lock => 200, status LOCKED, HR bị khoá với tiền tố [COMPANY_LOCK]; lock lần 2 => 409; unlock => ACTIVE và chỉ mở HR có tiền tố")
    void testE_lockAndUnlockCompanyWithHrAccounts() throws Exception {
        Company company = createTestCompany("Lock Test", CompanyStatus.ACTIVE);
        StubAuthClient stub = (StubAuthClient) authClient;

        // Seed 2 HR accounts
        UUID hr1Id = UUID.randomUUID();
        UUID hr2Id = UUID.randomUUID();
        AuthHrAccount hr1 = new AuthHrAccount(
                hr1Id, "hr_lock_1@test.com", "HR Active", company.getId(),
                "ACTIVE", null, null, false, OffsetDateTime.now(ZoneOffset.UTC), null, true);
        AuthHrAccount hr2 = new AuthHrAccount(
                hr2Id, "hr_lock_2@test.com", "HR Personal Lock", company.getId(),
                "LOCKED", "Vi phạm cá nhân", OffsetDateTime.now(ZoneOffset.UTC), false, OffsetDateTime.now(ZoneOffset.UTC), null, true);
        stub.seedAccount(hr1);
        stub.seedAccount(hr2);

        // 1. Lock without reason => 400
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/lock")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // 2. Lock company => 200, status LOCKED
        String lockReason = "Công ty vi phạm chính sách đăng tin";
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/lock")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockCompanyRequest(lockReason))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        Company lockedCompany = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(lockedCompany.getStatus()).isEqualTo(CompanyStatus.LOCKED);

        // HR1 should be locked with [COMPANY_LOCK] prefix
        AuthHrAccount hr1Locked = stub.findById(hr1Id).orElseThrow();
        assertThat(hr1Locked.status()).isEqualTo("LOCKED");
        assertThat(hr1Locked.lockedReason()).isEqualTo("[COMPANY_LOCK] " + lockReason);

        // HR2 should still have original reason
        AuthHrAccount hr2Unchanged = stub.findById(hr2Id).orElseThrow();
        assertThat(hr2Unchanged.status()).isEqualTo("LOCKED");
        assertThat(hr2Unchanged.lockedReason()).isEqualTo("Vi phạm cá nhân");

        // Verify company_actions has LOCK
        List<CompanyAction> lockActions = companyActionRepository.findAll().stream()
                .filter(a -> a.getCompanyId().equals(company.getId()) && a.getAction() == CompanyActionType.LOCK)
                .toList();
        assertThat(lockActions).hasSize(1);
        assertThat(lockActions.get(0).getReason()).isEqualTo(lockReason);

        // 3. Lock 2nd time => 409
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/lock")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockCompanyRequest("Khóa tiếp"))))
                .andExpect(status().isConflict());

        // 4. Unlock company => 200, ACTIVE
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/unlock")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        Company unlockedCompany = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(unlockedCompany.getStatus()).isEqualTo(CompanyStatus.ACTIVE);

        // HR1 should now be ACTIVE
        AuthHrAccount hr1Unlocked = stub.findById(hr1Id).orElseThrow();
        assertThat(hr1Unlocked.status()).isEqualTo("ACTIVE");

        // HR2 was locked individually, so it must REMAIN LOCKED
        AuthHrAccount hr2StillLocked = stub.findById(hr2Id).orElseThrow();
        assertThat(hr2StillLocked.status()).isEqualTo("LOCKED");
        assertThat(hr2StillLocked.lockedReason()).isEqualTo("Vi phạm cá nhân");

        // Verify company_actions has UNLOCK
        List<CompanyAction> unlockActions = companyActionRepository.findAll().stream()
                .filter(a -> a.getCompanyId().equals(company.getId()) && a.getAction() == CompanyActionType.UNLOCK)
                .toList();
        assertThat(unlockActions).hasSize(1);

        // Unlock 2nd time => 409
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/unlock")
                        .header("Authorization", staffToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("f) công ty LOCKED: JD APPROVED ẩn khỏi GET /jobs công khai; GET /jobs/{id} bởi candidate => 404; HR createJob => 403; unlock => JD hiện lại")
    void testF_lockedCompanyHidesJdAndBlocksHr() throws Exception {
        Company company = createTestCompany("Lock JD Test", CompanyStatus.ACTIVE);
        Job job = createTestJob(company.getId(), JobStatus.APPROVED, LocalDate.now().plusDays(30));
        String hrToken = createHrToken(company.getId(), "hr_locked_f@test.com");

        // Public list should contain the job initially
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(job.getId().toString())));

        // Lock company
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/lock")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockCompanyRequest("Doanh nghiệp có dấu hiệu vi phạm"))))
                .andExpect(status().isOk());

        // 1. JD APPROVED does NOT appear in public GET /jobs
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(job.getId().toString()))));

        // Still exists in DB
        assertThat(jobRepository.findById(job.getId())).isPresent();

        // 2. Candidate GET /jobs/{id} => 404
        mockMvc.perform(get("/jobs/" + job.getId())
                        .header("Authorization", candidateToken))
                .andExpect(status().isNotFound());

        // 3. HR calling createJob => 403
        CreateJobRequest createReq = new CreateJobRequest(
                "JD mới khi công ty bị khóa",
                "Developer",
                JobLevel.JUNIOR,
                JobType.FULLTIME,
                1,
                "Hà Nội",
                "Keangnam",
                WorkMode.ONSITE,
                BigDecimal.valueOf(10000000),
                BigDecimal.valueOf(15000000),
                "Mô tả",
                "Yêu cầu",
                "Quyền lợi",
                LocalDate.now().plusDays(20)
        );
        mockMvc.perform(post("/jobs")
                        .header("Authorization", hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Doanh nghiệp đang bị khoá, bạn không thể thực hiện thao tác này"));

        // 4. Unlock company => JD is visible again
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/unlock")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(job.getId().toString())));

        mockMvc.perform(get("/jobs/" + job.getId())
                        .header("Authorization", candidateToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("g) GET /staff/companies/{id}/history trả đủ LOCK, UNLOCK, APPROVE_JD, REJECT_JD theo thứ tự mới nhất trước")
    void testG_companyHistory() throws Exception {
        Company company = createTestCompany("History Test", CompanyStatus.ACTIVE);
        Job job1 = createTestJob(company.getId(), JobStatus.PENDING, LocalDate.now().plusDays(10));
        Job job2 = createTestJob(company.getId(), JobStatus.PENDING, LocalDate.now().plusDays(10));

        // 1. Approve JD
        mockMvc.perform(post("/staff/jobs/" + job1.getId() + "/approve")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk());

        // 2. Reject JD
        mockMvc.perform(post("/staff/jobs/" + job2.getId() + "/reject")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Chưa đầy đủ nội dung"))))
                .andExpect(status().isOk());

        // 3. Lock company
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/lock")
                        .header("Authorization", staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LockCompanyRequest("Tạm dừng hoạt động"))))
                .andExpect(status().isOk());

        // 4. Unlock company
        mockMvc.perform(post("/staff/companies/" + company.getId() + "/unlock")
                        .header("Authorization", staffToken))
                .andExpect(status().isOk());

        // Get history
        mockMvc.perform(get("/staff/companies/" + company.getId() + "/history")
                        .header("Authorization", staffToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.items", hasSize(4)))
                // Descending order: UNLOCK, LOCK, REJECT_JD, APPROVE_JD
                .andExpect(jsonPath("$.data.items[0].action").value("UNLOCK"))
                .andExpect(jsonPath("$.data.items[0].staffId").isNotEmpty())
                .andExpect(jsonPath("$.data.items[1].action").value("LOCK"))
                .andExpect(jsonPath("$.data.items[1].reason").value("Tạm dừng hoạt động"))
                .andExpect(jsonPath("$.data.items[2].action").value("REJECT_JD"))
                .andExpect(jsonPath("$.data.items[2].targetId").value(job2.getId().toString()))
                .andExpect(jsonPath("$.data.items[2].targetTitle").value(job2.getTitle()))
                .andExpect(jsonPath("$.data.items[3].action").value("APPROVE_JD"))
                .andExpect(jsonPath("$.data.items[3].targetId").value(job1.getId().toString()))
                .andExpect(jsonPath("$.data.items[3].targetTitle").value(job1.getTitle()));

        // Non-existent company => 404
        mockMvc.perform(get("/staff/companies/" + UUID.randomUUID() + "/history")
                        .header("Authorization", staffToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("h) closeExpiredJobs: JD APPROVED deadline hôm qua => CLOSED; hôm nay/tương lai giữ nguyên; DRAFT/PENDING quá hạn không đổi")
    void testH_closeExpiredJobs() {
        Company company = createTestCompany("Expiry Test", CompanyStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        Job expiredApproved = createTestJob(company.getId(), JobStatus.APPROVED, yesterday);
        Job todayApproved = createTestJob(company.getId(), JobStatus.APPROVED, today);
        Job tomorrowApproved = createTestJob(company.getId(), JobStatus.APPROVED, tomorrow);
        Job expiredDraft = createTestJob(company.getId(), JobStatus.DRAFT, yesterday);
        Job expiredPending = createTestJob(company.getId(), JobStatus.PENDING, yesterday);

        int closedCount = jobService.closeExpiredJobs();
        assertThat(closedCount).isGreaterThanOrEqualTo(1);

        Job refreshedExpiredApproved = jobRepository.findById(expiredApproved.getId()).orElseThrow();
        assertThat(refreshedExpiredApproved.getStatus()).isEqualTo(JobStatus.CLOSED);

        Job refreshedTodayApproved = jobRepository.findById(todayApproved.getId()).orElseThrow();
        assertThat(refreshedTodayApproved.getStatus()).isEqualTo(JobStatus.APPROVED);

        Job refreshedTomorrowApproved = jobRepository.findById(tomorrowApproved.getId()).orElseThrow();
        assertThat(refreshedTomorrowApproved.getStatus()).isEqualTo(JobStatus.APPROVED);

        Job refreshedDraft = jobRepository.findById(expiredDraft.getId()).orElseThrow();
        assertThat(refreshedDraft.getStatus()).isEqualTo(JobStatus.DRAFT);

        Job refreshedPending = jobRepository.findById(expiredPending.getId()).orElseThrow();
        assertThat(refreshedPending.getStatus()).isEqualTo(JobStatus.PENDING);
    }
}
