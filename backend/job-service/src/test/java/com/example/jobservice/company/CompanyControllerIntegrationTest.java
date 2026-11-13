
package com.example.jobservice.company;

import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
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

import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class CompanyControllerIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private CompanyRepository companyRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JwtTokenProvider tokenProvider;

        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private String staffToken;
        private String hr1Token;
        private String candidateToken;
        private String mentorToken;

        private UUID companyAId;
        private UUID companyBId;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build();

                // Lấy HR1 được nạp từ mock/data/users.json
                User hr1 = userRepository.findAll().stream()
                                .filter(user -> "hr1@rikkeijobs.test".equals(user.getEmail()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Không tìm thấy HR1 trong dữ liệu test"));

                companyAId = hr1.getCompanyId();

                if (companyAId == null) {
                        throw new IllegalStateException("HR1 chưa được gán companyId");
                }

                // Lấy công ty B khác công ty A
                companyBId = companyRepository.findAll().stream()
                                .map(Company::getId)
                                .filter(id -> !id.equals(companyAId))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                                "Không tìm thấy công ty B trong dữ liệu test"));

                staffToken = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                UUID.randomUUID(),
                                                "staff1@rikkeijobs.test",
                                                "Staff 1",
                                                "ROLE_STAFF",
                                                null));

                hr1Token = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                hr1.getId(),
                                                hr1.getEmail(),
                                                hr1.getFullName(),
                                                hr1.getRole(),
                                                hr1.getCompanyId()));

                candidateToken = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                UUID.randomUUID(),
                                                "candidate@rikkei.vn",
                                                "Candidate One",
                                                "ROLE_CANDIDATE",
                                                null));

                mentorToken = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                UUID.randomUUID(),
                                                "mentor@rikkei.vn",
                                                "Mentor One",
                                                "ROLE_MENTOR",
                                                null));
        }

        @Test
        @DisplayName("STAFF tạo công ty thành công với mã số thuế 10 số")
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
                                .andExpect(jsonPath("$.id").isString())
                                .andExpect(jsonPath("$.name", is("Công ty TNHH Phần mềm ABC")))
                                .andExpect(jsonPath("$.taxCode", is("0108889999")))
                                .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Mã số thuế sai định dạng → 400 Bad Request")
        void staffCreateCompany_invalidTaxCode_returns400() throws Exception {
                CreateCompanyRequest request = new CreateCompanyRequest(
                                "Công ty Thuế Sai",
                                "123456789",
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
        @DisplayName("Mã số thuế đã tồn tại → 409 Conflict")
        void staffCreateCompany_duplicateTaxCode_returns409() throws Exception {
                CreateCompanyRequest request = new CreateCompanyRequest(
                                "Công ty Trùng MST",
                                "0101234567",
                                "CNTT",
                                "1-50",
                                null, null, null, null);

                mockMvc.perform(post("/staff/companies")
                                .header("Authorization", staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.detail", containsString("đã tồn tại")));
        }

        @Test
        @DisplayName("STAFF tạo HR cho doanh nghiệp thành công")
        void staffCreateHrAccount_shouldReturn201WithTemporaryPassword() throws Exception {
                CreateHrAccountRequest request = new CreateHrAccountRequest(
                                "hr.test.create@rikkei.vn",
                                "Trần Văn Tuyển Dụng");

                mockMvc.perform(post("/staff/companies/" + companyAId + "/hr-accounts")
                                .header("Authorization", staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email", is("hr.test.create@rikkei.vn")))
                                .andExpect(jsonPath("$.role", is("ROLE_HR")))
                                .andExpect(jsonPath("$.companyId", is(companyAId.toString())))
                                .andExpect(jsonPath("$.mustChangePassword", is(true)))
                                .andExpect(jsonPath("$.emailSent", is(true)));
        }

        @Test
        @DisplayName("Tạo HR trùng email → 409 Conflict")
        void staffCreateHrAccount_duplicateEmail_returns409() throws Exception {
                CreateHrAccountRequest request = new CreateHrAccountRequest(
                                "hr1@rikkeijobs.test",
                                "Trùng Tên");

                mockMvc.perform(post("/staff/companies/" + companyAId + "/hr-accounts")
                                .header("Authorization", staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.detail", containsString("đã tồn tại")));
        }

        @Test
        @DisplayName("HR lấy thông tin công ty mình → 200 OK")
        void hrGetMyCompany_returns200() throws Exception {
                mockMvc.perform(get("/companies/me")
                                .header("Authorization", hr1Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(companyAId.toString())))
                                .andExpect(jsonPath("$.taxCode", is("0101234567")));
        }

        @Test
        @DisplayName("HR tự sửa hồ sơ công ty mình → 200 OK")
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
                                .andExpect(jsonPath("$.name",
                                                is("Công ty Cổ phần Công nghệ Rikkei A Mới")))
                                .andExpect(jsonPath("$.industry", is("Fintech")))
                                .andExpect(jsonPath("$.taxCode", is("0101234567")))
                                .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("HR A gửi companyId của B khi cập nhật công ty → 404")
        void hrSpoofingCompanyId_isRejectedWith404() throws Exception {
                String spoofedBody = """
                                {
                                  "name": "Cố tình sửa công ty B",
                                  "industry": "Fintech",
                                  "size": "51-200",
                                  "address": "Quận 1",
                                  "city": "TP. HCM",
                                  "website": null,
                                  "description": null
                                }
                                """;

                mockMvc.perform(put("/companies/" + companyBId)
                                .header("Authorization", hr1Token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(spoofedBody))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.detail", containsString("Không tìm thấy doanh nghiệp")));

                mockMvc.perform(put("/companies/" + companyBId)
                                .header("Authorization", hr1Token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(spoofedBody))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.detail", containsString("Không tìm thấy doanh nghiệp")));
        }

        @Test
        @DisplayName("HR không được sửa công ty khác → 404")
        void hr1UpdateCompanyB_returns404() throws Exception {
                UpdateCompanyRequest request = new UpdateCompanyRequest(
                                "Cố tình sửa công ty B",
                                "Fintech",
                                "51-200",
                                "Quận 1",
                                "TP. HCM",
                                null,
                                null);

                mockMvc.perform(put("/companies/" + companyBId)
                                .header("Authorization", hr1Token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.detail",
                                                containsString("Không tìm thấy doanh nghiệp")));
        }

        @Test
        @DisplayName("Anonymous gọi HR company API → 401")
        void anonymousCompanyApi_returns401() throws Exception {
                mockMvc.perform(get("/companies/me"))
                                .andExpect(status().isUnauthorized());
                mockMvc.perform(put("/companies/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Candidate/Mentor sai vai trò truy cập HR company API → 403")
        void wrongRoleAccessToHrCompanyApi_returns403() throws Exception {
                mockMvc.perform(get("/companies/me").header("Authorization", candidateToken))
                                .andExpect(status().isForbidden());
                mockMvc.perform(get("/companies/me").header("Authorization", mentorToken))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("HR chưa đổi mật khẩu bị chặn tới khi đổi mật khẩu")
        void mustChangePassword_blocksCompanyApiAndChangePasswordReturnsNewToken() throws Exception {
                User hr1 = userRepository.findByEmail("hr1@rikkeijobs.test").orElseThrow();
                String originalPasswordHash = hr1.getPasswordHash();
                boolean originalMustChangePassword = hr1.isMustChangePassword();

                try {
                        hr1.setMustChangePassword(true);
                        userRepository.saveAndFlush(hr1);

                        String passwordChangeToken = "Bearer " + objectMapper.readTree(
                                        mockMvc.perform(post("/api/v1/auth/login")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content("""
                                                                        {"email":"hr1@rikkeijobs.test","password":"Test@123"}
                                                                        """))
                                                        .andExpect(status().isOk())
                                                        .andReturn()
                                                        .getResponse()
                                                        .getContentAsString())
                                        .get("token").asText();

                        mockMvc.perform(get("/companies/me")
                                        .header("Authorization", passwordChangeToken))
                                        .andExpect(status().isForbidden())
                                        .andExpect(jsonPath("$.detail",
                                                        containsString("phải đổi mật khẩu")));

                        mockMvc.perform(post("/api/v1/auth/change-password")
                                        .header("Authorization", passwordChangeToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"oldPassword":"Test@123","newPassword":"NewPassword@123"}
                                                        """))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.token").isString())
                                        .andExpect(jsonPath("$.message", is("Đổi mật khẩu thành công")))
                                        .andDo(result -> {
                                                String newToken = objectMapper.readTree(
                                                                result.getResponse().getContentAsString())
                                                                .get("token").asText();
                                                mockMvc.perform(get("/companies/me")
                                                                .header("Authorization", "Bearer " + newToken))
                                                                .andExpect(status().isOk());
                                        });
                } finally {
                        hr1.setPasswordHash(originalPasswordHash);
                        hr1.setMustChangePassword(originalMustChangePassword);
                        userRepository.saveAndFlush(hr1);
                }
        }

        @Test
        @DisplayName("Đổi mật khẩu không có token → 401")
        void changePassword_withoutToken_returns401() throws Exception {
                mockMvc.perform(post("/api/v1/auth/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"oldPassword":"Test@123","newPassword":"NewPassword@123"}
                                                """))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Logo lớn hơn 2MB → 400 Bad Request")
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
                                .andExpect(jsonPath("$.detail",
                                                containsString("không được vượt quá 2MB")));
        }

        @Test
        @DisplayName("Upload logo PNG hợp lệ → 200 OK")
        void uploadLogo_validPng_returns200() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "logo.png",
                                "image/png",
                                new byte[] {
                                                (byte) 0x89, 0x50, 0x4E, 0x47,
                                                0x0D, 0x0A, 0x1A, 0x0A
                                });

                String oldLogoUrl = mockMvc.perform(multipart("/companies/me/logo")
                                .file(file)
                                .header("Authorization", hr1Token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.logoUrl",
                                                startsWith("/uploads/logos/company-" + companyAId + "-")))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();
                String oldLogo = objectMapper.readTree(oldLogoUrl).get("logoUrl").asText();

                mockMvc.perform(multipart("/companies/me/logo")
                                .file(file)
                                .header("Authorization", hr1Token))
                                .andExpect(status().isOk());

                Path oldLogoFile = Path.of("build/test-uploads/logos")
                                .resolve(Path.of(oldLogo).getFileName());
                org.assertj.core.api.Assertions.assertThat(Files.exists(oldLogoFile)).isFalse();
        }

        @Test
        @DisplayName("OpenAPI liệt kê API DN-01 và Swagger UI tồn tại")
        void openApiDocumentsCompanyEndpoints() throws Exception {
                mockMvc.perform(get("/v3/api-docs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.paths['/staff/companies'].post").exists())
                                .andExpect(jsonPath("$.paths['/staff/companies/{id}/hr-accounts'].post").exists())
                                .andExpect(jsonPath("$.paths['/companies/me'].get").exists())
                                .andExpect(jsonPath("$.paths['/companies/me'].put").exists())
                                .andExpect(jsonPath("$.paths['/companies/me/logo'].post").exists())
                                .andExpect(jsonPath("$.paths['/companies/{id}'].put").exists())
                                .andExpect(jsonPath("$.paths['/staff/companies/{id}'].get").exists());

                mockMvc.perform(get("/swagger-ui.html"))
                                .andExpect(status().is3xxRedirection());
        }
}
