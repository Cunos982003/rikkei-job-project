package com.example.learningservice.controller;

import com.example.learningservice.advice.GlobalHandleException;
import com.example.learningservice.model.dto.response.staff.CandidateInfoDto;
import com.example.learningservice.model.dto.response.staff.EnrollmentProgressDto;
import com.example.learningservice.model.dto.response.staff.MissingSkillDto;
import com.example.learningservice.model.dto.response.staff.TopMissingSkillDto;
import com.example.learningservice.model.dto.response.staff.UpskillingCandidateDto;
import com.example.learningservice.model.dto.response.staff.UpskillingSummaryResponse;
import com.example.learningservice.security.UserContextHelper;
import com.example.learningservice.service.ExcelExportService;
import com.example.learningservice.service.StaffUpskillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StaffUpskillingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StaffUpskillingService staffUpskillingService;

    @Spy
    private ExcelExportService excelExportService = new ExcelExportService();

    @Spy
    private UserContextHelper userContextHelper = new UserContextHelper();

    @InjectMocks
    private StaffUpskillingController controller;

    private static final UUID STAFF_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CANDIDATE_ID = UUID.fromString("ed854c07-f3eb-48f1-a186-4e7135f54645");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalHandleException())
                .build();
    }

    private UpskillingCandidateDto createSampleCandidate() {
        return UpskillingCandidateDto.builder()
                .candidate(CandidateInfoDto.builder()
                        .userId(CANDIDATE_ID)
                        .fullName("Nguyễn Văn An")
                        .email("candidate1@rikkeijobs.test")
                        .phone("0912345600")
                        .build())
                .classCode("RA-JAVA-2026")
                .courseName("Khóa đào tạo Kỹ sư Phần mềm Java")
                .target("Java Backend Fresher")
                .missingSkills(List.of(
                        MissingSkillDto.builder()
                                .skillId("JAVA_CORE")
                                .skillName("Java Core")
                                .required(80)
                                .current(55)
                                .gap(25)
                                .priority("HIGH")
                                .build()
                ))
                .enrollments(List.of(
                        EnrollmentProgressDto.builder()
                                .course("Lập trình Spring Boot nâng cao & Microservices")
                                .progress(new BigDecimal("100.00"))
                                .score(new BigDecimal("80.00"))
                                .status("COMPLETED")
                                .build()
                ))
                .readiness(65.0)
                .status("IN_PROGRESS")
                .build();
    }

    @Test
    @DisplayName("STAFF gọi GET /staff/upskilling trả về 200 và danh sách ứng viên")
    void getUpskillingList_asStaff_returns200() throws Exception {
        when(staffUpskillingService.getUpskillingList(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createSampleCandidate()));
        when(staffUpskillingService.countUpskillingList(any(), any(), any(), any()))
                .thenReturn(1L);

        mockMvc.perform(get("/api/v1/staff/upskilling")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF")
                        .param("classCode", "RA-JAVA-2026")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].candidate.fullName", is("Nguyễn Văn An")))
                .andExpect(jsonPath("$.data[0].classCode", is("RA-JAVA-2026")))
                .andExpect(jsonPath("$.data[0].target", is("Java Backend Fresher")))
                .andExpect(jsonPath("$.data[0].missingSkills[0].skillId", is("JAVA_CORE")))
                .andExpect(jsonPath("$.data[0].enrollments[0].status", is("COMPLETED")))
                .andExpect(header().string("X-Total-Count", "1"));
    }

    @Test
    @DisplayName("ADMIN gọi GET /staff/upskilling trả về 200")
    void getUpskillingList_asAdmin_returns200() throws Exception {
        when(staffUpskillingService.getUpskillingList(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(createSampleCandidate()));
        when(staffUpskillingService.countUpskillingList(any(), any(), any(), any()))
                .thenReturn(1L);

        mockMvc.perform(get("/api/v1/staff/upskilling")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("HR gọi GET /staff/upskilling trả về 403 Forbidden (AC-4)")
    void getUpskillingList_asHR_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/staff/upskilling")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_HR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(403)));
    }

    @Test
    @DisplayName("CANDIDATE gọi GET /staff/upskilling trả về 403 Forbidden (AC-4)")
    void getUpskillingList_asCandidate_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/staff/upskilling")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "ROLE_CANDIDATE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(403)));
    }

    @Test
    @DisplayName("Chưa đăng nhập gọi GET /staff/upskilling trả về 401 Unauthorized")
    void getUpskillingList_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/staff/upskilling"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(401)));
    }

    @Test
    @DisplayName("STAFF gọi GET /staff/upskilling/summary trả về 200 và thống kê tổng hợp")
    void getUpskillingSummary_asStaff_returns200() throws Exception {
        UpskillingSummaryResponse summary = UpskillingSummaryResponse.builder()
                .inProgressCount(15L)
                .completedCount(10L)
                .topMissingSkills(List.of(
                        TopMissingSkillDto.builder().skillId("TYPESCRIPT").skillName("TypeScript").missingCount(10L).percentage(25.0).build(),
                        TopMissingSkillDto.builder().skillId("JAVA_CORE").skillName("Java Core").missingCount(8L).percentage(20.0).build(),
                        TopMissingSkillDto.builder().skillId("REACT").skillName("React").missingCount(7L).percentage(17.5).build(),
                        TopMissingSkillDto.builder().skillId("SPRING_BOOT").skillName("Spring Boot").missingCount(7L).percentage(17.5).build(),
                        TopMissingSkillDto.builder().skillId("SQL").skillName("SQL").missingCount(5L).percentage(12.5).build()
                ))
                .build();

        when(staffUpskillingService.getUpskillingSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/staff/upskilling/summary")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.inProgressCount", is(15)))
                .andExpect(jsonPath("$.data.completedCount", is(10)))
                .andExpect(jsonPath("$.data.topMissingSkills", hasSize(5)))
                .andExpect(jsonPath("$.data.topMissingSkills[0].skillName", is("TypeScript")))
                .andExpect(jsonPath("$.data.topMissingSkills[1].skillName", is("Java Core")));
    }

    @Test
    @DisplayName("HR gọi GET /staff/upskilling/summary trả về 403 Forbidden (AC-4)")
    void getUpskillingSummary_asHR_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/staff/upskilling/summary")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_HR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(403)));
    }

    @Test
    @DisplayName("CANDIDATE gọi GET /staff/upskilling/summary trả về 403 Forbidden (AC-4)")
    void getUpskillingSummary_asCandidate_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/staff/upskilling/summary")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "ROLE_CANDIDATE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(403)));
    }

    @Test
    @DisplayName("STAFF gọi GET /staff/upskilling/export trả về 200 OK và file .xlsx khớp bộ lọc (AC-3)")
    void exportUpskillingExcel_asStaff_returns200AndXlsxFile() throws Exception {
        when(staffUpskillingService.getUpskillingList(isNull(), eq("RA-JAVA-2026"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(createSampleCandidate()));

        mockMvc.perform(get("/api/v1/staff/upskilling/export")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_STAFF")
                        .param("classCode", "RA-JAVA-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"bao-cao-dao-tao-bo-sung-")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx\"")));
    }

    @Test
    @DisplayName("HR gọi GET /staff/upskilling/export trả về 403 Forbidden (AC-4)")
    void exportUpskillingExcel_asHR_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/staff/upskilling/export")
                        .header("X-User-Id", STAFF_ID.toString())
                        .header("X-User-Role", "ROLE_HR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(403)));
    }

    @Test
    @DisplayName("CANDIDATE gọi GET /staff/upskilling/export trả về 403 Forbidden (AC-4)")
    void exportUpskillingExcel_asCandidate_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/staff/upskilling/export")
                        .header("X-User-Id", CANDIDATE_ID.toString())
                        .header("X-User-Role", "ROLE_CANDIDATE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(403)));
    }
}
