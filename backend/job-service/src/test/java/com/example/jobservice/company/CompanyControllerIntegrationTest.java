package com.example.jobservice.company;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.example.jobservice.company.dto.CreateCompanyRequest;
import com.example.jobservice.company.dto.CreateHrAccountRequest;
import com.example.jobservice.company.dto.UpdateCompanyRequest;
import com.example.jobservice.security.JwtTokenProvider;
import com.example.jobservice.security.UserPrincipal;
import com.example.jobservice.user.domain.User;
import com.example.jobservice.user.domain.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class CompanyControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String staffToken;
    private String hr1Token; // belongs to company A (id: 1)
    private String hr2Token; // belongs to company B (id: 2)

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        staffToken = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(1L, "staff1@rikkei.vn", "Staff 1", "ROLE_STAFF", null));

        hr1Token = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(2L, "hr1@rikkei.vn", "HR 1", "ROLE_HR", 1L));

        hr2Token = "Bearer " + tokenProvider.generateToken(
                UserPrincipal.of(3L, "hr2@rikkei.vn", "HR 2", "ROLE_HR", 2L));
    }

    @Test
    @DisplayName("STAFF tạo công ty thành công với mã số thuế hợp lệ 10 số → 201 Created")
    void staffCreateCompany_valid10Digits_returns201() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Công ty TNHH Phần mềm ABC",
                "0108889999",
                "Phần mềm",
                "51-200",
                "Cầu Giấy",
                "Hà Nội",
                "https://abc.vn",
                "Công ty phần mềm ABC");

        mockMvc.perform(post("/staff/companies")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Công ty TNHH Phần mềm ABC")))
                .andExpect(jsonPath("$.taxCode", is("0108889999")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("Tiêu chí: Mã số thuế sai định dạng (9 số hoặc có chữ) → 400 Bad Request")
    void staffCreateCompany_invalidTaxCode_returns400() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Công ty Thuế Sai",
                "123456789", // 9 digits only
                "CNTT",
                "1-50",
                null, null, null, null);

        mockMvc.perform(post("/staff/companies")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.taxCode",
                        containsString("Mã số thuế phải gồm 10 hoặc 13 chữ số")));
    }

    @Test
    @DisplayName("STAFF tạo HR cho doanh nghiệp thành công → 201 Created")
    void staffCreateHrAccount_shouldReturn201WithTemporaryPassword() throws Exception {
        CreateHrAccountRequest request = new CreateHrAccountRequest(
                "hr.test.create@rikkei.vn",
                "Trần Văn Tuyển Dụng");

        mockMvc.perform(post("/staff/companies/1/hr-accounts")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("hr.test.create@rikkei.vn")))
                .andExpect(jsonPath("$.role", is("ROLE_HR")))
                .andExpect(jsonPath("$.companyId", is(1)))
                .andExpect(jsonPath("$.mustChangePassword", is(true)));
    }

    @Test
    @DisplayName("Tiêu chí: Tạo HR trùng email → 409 Conflict")
    void staffCreateHrAccount_duplicateEmail_returns409() throws Exception {
        // hr1@rikkei.vn already seeded in V1 flyway
        CreateHrAccountRequest request = new CreateHrAccountRequest(
                "hr1@rikkei.vn",
                "Trùng Tên");

        mockMvc.perform(post("/staff/companies/1/hr-accounts")
                .header("Authorization", staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("đã tồn tại")));
    }

    @Test
    @DisplayName("HR lấy thông tin công ty mình qua /companies/me → 200 OK")
    void hrGetMyCompany_returns200() throws Exception {
        mockMvc.perform(get("/companies/me")
                .header("Authorization", hr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.taxCode", is("0101234567")));
    }

    @Test
    @DisplayName("HR tự sửa hồ sơ công ty mình qua /companies/me → 200 OK")
    void hrUpdateMyCompany_returns200() throws Exception {
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Công ty Cổ phần Công nghệ Rikkei A Mới",
                "Fintech",
                "201-1000",
                "Tầng 25 Keangnam",
                "Hà Nội",
                "https://rikkeia-new.com",
                "Mô tả đã được cập nhật bởi HR");

        mockMvc.perform(put("/companies/me")
                .header("Authorization", hr1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Công ty Cổ phần Công nghệ Rikkei A Mới")))
                .andExpect(jsonPath("$.industry", is("Fintech")))
                .andExpect(jsonPath("$.taxCode", is("0101234567"))) // không đổi tax_code
                .andExpect(jsonPath("$.status", is("ACTIVE"))); // không đổi status
    }

    @Test
    @DisplayName("Tiêu chí: hr1 sửa được công ty A; gọi API sửa công ty B → 404")
    void hr1UpdateCompanyB_returns404() throws Exception {
        UpdateCompanyRequest request = new UpdateCompanyRequest(
                "Cố tình sửa công ty B",
                "Fintech",
                "51-200",
                "Quận 1",
                "TP. HCM",
                null,
                null);

        // hr1 belongs to company 1 (A). Calling PUT /companies/2 (B) -> 404
        mockMvc.perform(put("/companies/2")
                .header("Authorization", hr1Token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("Không tìm thấy doanh nghiệp")));
    }

    @Test
    @DisplayName("Tiêu chí: Logo > 2MB → Bị từ chối (400 Bad Request)")
    void uploadLogo_exceeds2MB_returns400() throws Exception {
        byte[] largeBytes = new byte[2 * 1024 * 1024 + 10];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "big-logo.jpg",
                "image/jpeg",
                largeBytes);

        mockMvc.perform(multipart("/companies/me/logo")
                .file(file)
                .header("Authorization", hr1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("không được vượt quá 2MB")));
    }

    @Test
    @DisplayName("Upload logo hợp lệ (PNG ≤ 2MB) → 200 OK")
    void uploadLogo_validPng_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });

        mockMvc.perform(multipart("/companies/me/logo")
                .file(file)
                .header("Authorization", hr1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl", startsWith("/uploads/logos/company-1-")));
    }
}
