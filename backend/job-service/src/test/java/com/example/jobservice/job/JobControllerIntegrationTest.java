package com.example.jobservice.job;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.job.domain.*;
import com.example.jobservice.job.dto.CreateJobRequest;
import com.example.jobservice.job.dto.JobSkillItem;
import com.example.jobservice.job.dto.UpdateJobRequest;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class JobControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private String hr1Token;
    private String hr2Token;
    private String candidateToken;

    private UUID companyAId;
    private UUID companyBId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

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

        candidateToken = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(
                        UUID.randomUUID(),
                        "candidate@test.com",
                        "Nguyen Van Ung Vien",
                        "ROLE_CANDIDATE",
                        null));
    }

    private Job createDraftJob(UUID companyId, String title) {
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
        job.setStatus(JobStatus.DRAFT);
        job.setCreatedBy("hr1@rikkeijobs.test");
        return jobRepository.save(job);
    }

    @Test
    @DisplayName("1. JD không có kỹ năng bắt buộc -> submit 400")
    void submitJob_noSkills_returns400() throws Exception {
        Job job = createDraftJob(companyAId, "Java Backend Dev");

        mockMvc.perform(post("/jobs/" + job.getId() + "/submit")
                        .header("Authorization", hr1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Cần ít nhất 1 kỹ năng bắt buộc")));
    }

    @Test
    @DisplayName("2. Có kỹ năng chỉ ưu tiên (required=false) -> submit vẫn 400")
    void submitJob_onlyOptionalSkills_returns400() throws Exception {
        Job job = createDraftJob(companyAId, "Java Backend Dev");

        List<JobSkillItem> skills = List.of(
                new JobSkillItem("GIT", false, 60)
        );

        mockMvc.perform(put("/jobs/" + job.getId() + "/skills")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skills)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/jobs/" + job.getId() + "/submit")
                        .header("Authorization", hr1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Cần ít nhất 1 kỹ năng bắt buộc")));
    }

    @Test
    @DisplayName("3. Deadline trong quá khứ -> create 400; salaryMin > salaryMax -> 400")
    void createJob_invalidContent_returns400() throws Exception {
        // Deadline in past
        CreateJobRequest pastDeadlineReq = new CreateJobRequest(
                "Lập trình viên Java",
                "Backend Developer",
                JobLevel.JUNIOR,
                JobType.FULLTIME,
                1,
                "Hà Nội",
                "Duy Tân",
                WorkMode.ONSITE,
                BigDecimal.valueOf(10000000),
                BigDecimal.valueOf(20000000),
                "Mô tả công việc",
                "Yêu cầu",
                "Quyền lợi",
                LocalDate.now().minusDays(1)
        );

        mockMvc.perform(post("/jobs")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pastDeadlineReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Hạn nộp hồ sơ phải từ hôm nay trở đi")));

        // salaryMin > salaryMax
        CreateJobRequest invalidSalaryReq = new CreateJobRequest(
                "Lập trình viên Java",
                "Backend Developer",
                JobLevel.JUNIOR,
                JobType.FULLTIME,
                1,
                "Hà Nội",
                "Duy Tân",
                WorkMode.ONSITE,
                BigDecimal.valueOf(25000000),
                BigDecimal.valueOf(15000000),
                "Mô tả công việc",
                "Yêu cầu",
                "Quyền lợi",
                LocalDate.now().plusDays(20)
        );

        mockMvc.perform(post("/jobs")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSalaryReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Lương tối thiểu không được lớn hơn lương tối đa")));
    }

    @Test
    @DisplayName("4. hr1 (công ty A) sửa/xem/submit/close JD công ty B -> 404")
    void hr1_accessJobCompanyB_returns404() throws Exception {
        Job jobB = createDraftJob(companyBId, "React Frontend Dev");

        UpdateJobRequest updateReq = new UpdateJobRequest(
                "Tiêu đề mới",
                "Vị trí mới",
                JobLevel.MIDDLE,
                JobType.FULLTIME,
                2,
                "Đà Nẵng",
                "Nguyễn Văn Linh",
                WorkMode.HYBRID,
                BigDecimal.valueOf(15000000),
                BigDecimal.valueOf(25000000),
                "Mô tả mới",
                "Yêu cầu mới",
                "Quyền lợi mới",
                LocalDate.now().plusDays(20)
        );

        // Xem -> 404
        mockMvc.perform(get("/jobs/" + jobB.getId())
                        .header("Authorization", hr1Token))
                .andExpect(status().isNotFound());

        // Sửa -> 404
        mockMvc.perform(put("/jobs/" + jobB.getId())
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Submit -> 404
        mockMvc.perform(post("/jobs/" + jobB.getId() + "/submit")
                        .header("Authorization", hr1Token))
                .andExpect(status().isNotFound());

        // Close -> 404
        mockMvc.perform(post("/jobs/" + jobB.getId() + "/close")
                        .header("Authorization", hr1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("5. Sửa JD APPROVED -> status PENDING; sửa JD PENDING -> 409; sửa JD CLOSED -> 409")
    void updateJob_statusTransitions_andConflicts() throws Exception {
        Job job = createDraftJob(companyAId, "Fullstack Dev");
        job.setStatus(JobStatus.APPROVED);
        job.setRejectedReason("Lý do cũ");
        job = jobRepository.save(job);

        UpdateJobRequest updateReq = new UpdateJobRequest(
                "Fullstack Developer (Updated)",
                "Fullstack Developer",
                JobLevel.SENIOR,
                JobType.FULLTIME,
                3,
                "Hồ Chí Minh",
                "Quận 1",
                WorkMode.REMOTE,
                BigDecimal.valueOf(20000000),
                BigDecimal.valueOf(35000000),
                "Mô tả cập nhật",
                "Yêu cầu cập nhật",
                "Chế độ cập nhật",
                LocalDate.now().plusDays(25)
        );

        // 1) Sửa APPROVED -> PENDING, xoá rejectedReason
        mockMvc.perform(put("/jobs/" + job.getId())
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.rejectedReason").value(nullValue()));

        // 2) Sửa khi đang PENDING -> 409
        mockMvc.perform(put("/jobs/" + job.getId())
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Tin đang chờ duyệt, không thể chỉnh sửa")));

        // 3) Sửa khi đã CLOSED -> 409
        job.setStatus(JobStatus.CLOSED);
        job = jobRepository.save(job);

        mockMvc.perform(put("/jobs/" + job.getId())
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("Tin đã đóng, không thể chỉnh sửa")));
    }

    @Test
    @DisplayName("6. PUT skills với skillId không tồn tại -> 400; trùng skillId -> 400")
    void replaceSkills_validationErrors_returns400() throws Exception {
        Job job = createDraftJob(companyAId, "DevOps Engineer");

        // SkillId không tồn tại
        List<JobSkillItem> nonExistent = List.of(
                new JobSkillItem("NON_EXISTENT_SKILL_CODE_XYZ", true, 70)
        );
        mockMvc.perform(put("/jobs/" + job.getId() + "/skills")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Kỹ năng không tồn tại")));

        // Trùng skillId
        List<JobSkillItem> duplicateSkills = List.of(
                new JobSkillItem("JAVA_CORE", true, 70),
                new JobSkillItem("JAVA_CORE", false, 60)
        );
        mockMvc.perform(put("/jobs/" + job.getId() + "/skills")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateSkills)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Kỹ năng bị trùng lặp")));
    }

    @Test
    @DisplayName("7. Tạo JD loại INTERNSHIP thành công")
    void createJob_internship_success() throws Exception {
        CreateJobRequest request = new CreateJobRequest(
                "Thực tập sinh Java Backend (Spring Boot)",
                "Java Intern",
                JobLevel.INTERN,
                JobType.INTERNSHIP,
                5,
                "Hà Nội",
                "HPC Landmark 105 Tố Hữu",
                WorkMode.HYBRID,
                BigDecimal.valueOf(4000000),
                BigDecimal.valueOf(7000000),
                "Tham gia thực tập dự án thực chiến IOC",
                "Nắm vững kiến thức Java Core và OOP cơ bản",
                "Trợ cấp thực tập, có cơ hội lên chính thức",
                LocalDate.now().plusDays(40)
        );

        mockMvc.perform(post("/jobs")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.type").value("INTERNSHIP"))
                .andExpect(jsonPath("$.level").value("INTERN"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.companyId").value(companyAId.toString()));
    }

    @Test
    @DisplayName("8. Luồng đầy đủ: create -> PUT skills -> submit -> PENDING; list /companies/me/jobs?status=PENDING có phân trang đúng")
    void fullFlow_createSkillsSubmitAndList() throws Exception {
        CreateJobRequest request = new CreateJobRequest(
                "Lập trình viên Java Full Flow",
                "Java Dev",
                JobLevel.FRESHER,
                JobType.FULLTIME,
                2,
                "Hà Nội",
                "Keangnam",
                WorkMode.ONSITE,
                BigDecimal.valueOf(11000000),
                BigDecimal.valueOf(16000000),
                "Phát triển backend",
                "Java, Spring Boot",
                "Bảo hiểm đầy đủ",
                LocalDate.now().plusDays(20)
        );

        // 1) Create -> 201 DRAFT
        String createRes = mockMvc.perform(post("/jobs")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        String jobIdStr = objectMapper.readTree(createRes).get("id").asText();
        UUID jobId = UUID.fromString(jobIdStr);

        // 2) PUT skills
        List<JobSkillItem> skills = List.of(
                new JobSkillItem("JAVA_CORE", true, 75),
                new JobSkillItem("SPRING_BOOT", true, 70),
                new JobSkillItem("GIT", false, 60)
        );
        mockMvc.perform(put("/jobs/" + jobId + "/skills")
                        .header("Authorization", hr1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skills)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills", hasSize(3)))
                .andExpect(jsonPath("$.requiredSkills", hasSize(2)))
                .andExpect(jsonPath("$.requiredSkills", containsInAnyOrder("Java Core", "Spring Boot")));

        // 3) Submit -> 200 PENDING
        mockMvc.perform(post("/jobs/" + jobId + "/submit")
                        .header("Authorization", hr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 4) List /companies/me/jobs?status=PENDING
        mockMvc.perform(get("/companies/me/jobs")
                        .header("Authorization", hr1Token)
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", not(empty())))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.size").value(10))
                .andExpect(jsonPath("$.items[0].requiredSkillCount").isNumber());
    }

    @Test
    @DisplayName("9. GET /jobs/{id}: CANDIDATE xem JD APPROVED được, JD DRAFT -> 404")
    void candidateViewJob_onlyApproved() throws Exception {
        Job job = createDraftJob(companyAId, "Golang Microservice Engineer");

        // Khi JD là DRAFT -> CANDIDATE nhận 404
        mockMvc.perform(get("/jobs/" + job.getId())
                        .header("Authorization", candidateToken))
                .andExpect(status().isNotFound());

        // Khi JD là APPROVED -> CANDIDATE xem được (200 OK)
        job.setStatus(JobStatus.APPROVED);
        job = jobRepository.save(job);

        mockMvc.perform(get("/jobs/" + job.getId())
                        .header("Authorization", candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId().toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}

