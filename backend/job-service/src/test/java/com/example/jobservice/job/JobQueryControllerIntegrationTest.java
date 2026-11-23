package com.example.jobservice.job;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.job.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class JobQueryControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    private MockMvc mockMvc;
    private UUID testJobId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        Company company = companyRepository.findByTaxCode("0101234567")
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setName("Công ty Cổ phần Rikkeisoft");
                    c.setTaxCode("0101234567");
                    return companyRepository.save(c);
                });

        Job job = new Job();
        job.setCompanyId(company.getId());
        job.setTitle("Senior Java Developer Query Test");
        job.setPosition("Senior Java Developer");
        job.setLevel(JobLevel.SENIOR);
        job.setType(JobType.FULLTIME);
        job.setStatus(JobStatus.APPROVED);
        job.setSalaryMin(BigDecimal.valueOf(20000000));
        job.setSalaryMax(BigDecimal.valueOf(40000000));
        job.setCity("Hà Nội");
        job.setAddress("Cầu Giấy");
        job.setWorkMode(WorkMode.HYBRID);
        job.setQuantity(2);
        job.setDescription("Mô tả công việc chi tiết");
        job.setRequirements("Kinh nghiệm Spring Boot từ 3 năm");
        job.setBenefits("Lương tháng 13");
        job.setDeadline(LocalDate.now().plusDays(30));
        job = jobRepository.save(job);
        testJobId = job.getId();

        JobSkill skill = new JobSkill(job.getId(), "JAVA_CORE", "JAVA_CORE", "Java Core", true, 80);
        jobSkillRepository.save(skill);
    }

    @Test
    @DisplayName("GET /jobs chuẩn Envelope 5 trường và phân trang trong data")
    void listJobs_shouldReturnStandardEnvelope() throws Exception {
        mockMvc.perform(get("/jobs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.message", is("Lấy danh sách việc làm thành công")))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.pagination.page", is(0)))
                .andExpect(jsonPath("$.data.pagination.size", is(10)))
                .andExpect(jsonPath("$.data.pagination.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.pagination.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.pagination.isFirst", is(true)))
                .andExpect(jsonPath("$.data.pagination.hasNext").isBoolean())
                .andExpect(jsonPath("$.data.pagination.hasPrevious", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/jobs cũng trả về chuẩn Envelope 5 trường")
    void listJobsApiV1_shouldReturnStandardEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .param("keyword", "Senior Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.message", is("Lấy danh sách việc làm thành công")))
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.items[0].title", containsString("Senior Java")));
    }

    @Test
    @DisplayName("GET /api/v1/jobs/{id} trả về chi tiết JD bọc trong Envelope chuẩn")
    void getJobByIdApiV1_shouldReturnStandardEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/" + testJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.message", is("Lấy chi tiết tin tuyển dụng thành công")))
                .andExpect(jsonPath("$.data.id", is(testJobId.toString())))
                .andExpect(jsonPath("$.data.title", is("Senior Java Developer Query Test")));
    }

    @Test
    @DisplayName("GET /jobs trả về payload có benefits, quantity, workMode, position, level và required trong jobSkills")
    void listJobs_shouldContainNewFieldsInPayload() throws Exception {
        mockMvc.perform(get("/jobs")
                        .param("keyword", "Senior Java Developer Query Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items", not(empty())))
                .andExpect(jsonPath("$.data.items[0].benefits", is("Lương tháng 13")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(2)))
                .andExpect(jsonPath("$.data.items[0].workMode", is("HYBRID")))
                .andExpect(jsonPath("$.data.items[0].position", is("Senior Java Developer")))
                .andExpect(jsonPath("$.data.items[0].level", is("SENIOR")))
                .andExpect(jsonPath("$.data.items[0].experienceLevel", is("SENIOR")))
                .andExpect(jsonPath("$.data.items[0].jobSkills[0].required", is(true)))
                .andExpect(jsonPath("$.data.items[0].jobSkills[0].isMandatory", is(true)));
    }
}
