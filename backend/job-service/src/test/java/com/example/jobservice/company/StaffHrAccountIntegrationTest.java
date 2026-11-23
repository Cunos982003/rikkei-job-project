package com.example.jobservice.company;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.AuthHrAccount;
import com.example.jobservice.auth.client.StubAuthClient;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.dto.LockHrAccountRequest;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class StaffHrAccountIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private CompanyRepository companyRepository;

        @Autowired
        private AuthClient authClient;

        @Autowired
        private JwtTokenProvider tokenProvider;

        private MockMvc mockMvc;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private String staffToken;
        private AuthHrAccount hr2Account;
        private Company companyB;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build();

                staffToken = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(
                                                UUID.randomUUID(),
                                                "staff1@rikkeijobs.test",
                                                "Hoàng Thu Hà",
                                                "ROLE_STAFF",
                                                null));

                companyB = companyRepository.findByTaxCode("0107654321")
                                .orElseGet(() -> {
                                        Company c = new Company();
                                        c.setName("Rikkei Digital Solutions");
                                        c.setTaxCode("0107654321");
                                        return companyRepository.save(c);
                                });

                StubAuthClient stub = (StubAuthClient) authClient;
                hr2Account = stub.findByEmail("hr2@rikkeijobs.test")
                                .orElseGet(() -> {
                                        AuthHrAccount acc = new AuthHrAccount(
                                                        UUID.randomUUID(),
                                                        "hr2@rikkeijobs.test",
                                                        "HR Account Two",
                                                        companyB.getId(),
                                                        "ACTIVE",
                                                        null,
                                                        null,
                                                        false,
                                                        OffsetDateTime.now(ZoneOffset.UTC),
                                                        null,
                                                        true);
                                        stub.seedAccount(acc);
                                        return acc;
                                });

                // Đảm bảo ban đầu HR2 ở trạng thái ACTIVE
                stub.seedAccount(new AuthHrAccount(
                                hr2Account.id(),
                                hr2Account.email(),
                                hr2Account.fullName(),
                                companyB.getId(),
                                "ACTIVE",
                                null,
                                null,
                                false,
                                OffsetDateTime.now(ZoneOffset.UTC),
                                null,
                                true));
        }

        @Test
        @DisplayName("TEST 1 - STAFF khóa tài khoản HR2 với lý do hợp lệ: cập nhật status = LOCKED")
        void staffLockHrAccountSuccess() throws Exception {
                LockHrAccountRequest lockRequest = new LockHrAccountRequest("Vi phạm quy chế tuyển dụng của nền tảng");

                mockMvc.perform(post("/staff/hr-accounts/{id}/lock", hr2Account.id())
                                .header("Authorization", staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(lockRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.id", is(hr2Account.id().toString())))
                                .andExpect(jsonPath("$.data.status", is("LOCKED")))
                                .andExpect(jsonPath("$.data.lockedReason",
                                                is("Vi phạm quy chế tuyển dụng của nền tảng")))
                                .andExpect(jsonPath("$.data.lockedAt", notNullValue()));

                StubAuthClient stub = (StubAuthClient) authClient;
                AuthHrAccount updated = stub.findById(hr2Account.id()).orElseThrow();
                assertThat(updated.status()).isEqualTo("LOCKED");
                assertThat(updated.lockedReason()).isEqualTo("Vi phạm quy chế tuyển dụng của nền tảng");
                assertThat(updated.lockedAt()).isNotNull();
        }

        @Test
        @DisplayName("TEST 2 - STAFF mở khóa HR2: status = ACTIVE")
        void staffUnlockHrAccountSuccess() throws Exception {
                StubAuthClient stub = (StubAuthClient) authClient;
                stub.lockHrAccount(hr2Account.id(), "Lý do cũ");

                mockMvc.perform(post("/staff/hr-accounts/{id}/unlock", hr2Account.id())
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                                .andExpect(jsonPath("$.data.lockedReason").value(nullValue()))
                                .andExpect(jsonPath("$.data.lockedAt").value(nullValue()));

                AuthHrAccount unlocked = stub.findById(hr2Account.id()).orElseThrow();
                assertThat(unlocked.status()).isEqualTo("ACTIVE");
                assertThat(unlocked.lockedReason()).isNull();
                assertThat(unlocked.lockedAt()).isNull();
        }

        @Test
        @DisplayName("TEST 3 - Validation: Lý do rỗng, khóa lại user đã khóa, mở khóa user đang ACTIVE")
        void staffLockValidationErrors() throws Exception {
                // 3a. Lý do rỗng / khoảng trắng
                LockHrAccountRequest emptyReason = new LockHrAccountRequest("   ");
                mockMvc.perform(post("/staff/hr-accounts/{id}/lock", hr2Account.id())
                                .header("Authorization", staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(emptyReason)))
                                .andExpect(status().isBadRequest());

                // 3b. Khóa user không tồn tại -> 404
                mockMvc.perform(post("/staff/hr-accounts/{id}/lock", UUID.randomUUID())
                                .header("Authorization", staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LockHrAccountRequest("Lý do"))))
                                .andExpect(status().isNotFound());

                // 3c. Khóa lại user đã bị khóa -> 400
                StubAuthClient stub = (StubAuthClient) authClient;
                stub.lockHrAccount(hr2Account.id(), "Lý do ban đầu");

                mockMvc.perform(post("/staff/hr-accounts/{id}/lock", hr2Account.id())
                                .header("Authorization", staffToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LockHrAccountRequest("Lý do khác"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", containsString("Tài khoản HR đã bị khóa")));

                // 3d. Mở khóa user chưa bị khóa (sau khi đã mở khóa hoặc đang ACTIVE)
                stub.unlockHrAccount(hr2Account.id());
                mockMvc.perform(post("/staff/hr-accounts/{id}/unlock", hr2Account.id())
                                .header("Authorization", staffToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message", containsString("Tài khoản HR chưa bị khóa")));
        }

        @Test
        @DisplayName("TEST 4 - Quyền truy cập: Chưa đăng nhập hoặc HR gọi API lock/unlock bị từ chối")
        void unauthorizedAccessRejected() throws Exception {
                String hrToken = "Bearer " + tokenProvider.generateToken(
                                UserPrincipal.of(hr2Account.id(), "hr2@rikkeijobs.test", "HR 2", "ROLE_HR",
                                                companyB.getId()));

                // Chưa đăng nhập -> 401
                mockMvc.perform(post("/staff/hr-accounts/{id}/lock", hr2Account.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LockHrAccountRequest("Lý do"))))
                                .andExpect(status().isUnauthorized());

                // HR gọi API của STAFF -> 403
                mockMvc.perform(post("/staff/hr-accounts/{id}/lock", hr2Account.id())
                                .header("Authorization", hrToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new LockHrAccountRequest("Lý do"))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("TEST 5 - STAFF Danh sách doanh nghiệp: tìm kiếm, lọc trạng thái, phân trang, totalHr")
        void staffListCompanies() throws Exception {
                // 5a. Lấy danh sách mặc định
                mockMvc.perform(get("/staff/companies")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(2))))
                                .andExpect(jsonPath("$.data.pagination.totalElements", greaterThanOrEqualTo(2)))
                                .andExpect(jsonPath("$.data.items[0].totalHr", notNullValue()))
                                .andExpect(jsonPath("$.data.items[0].totalJd", notNullValue()));

                // 5b. Tìm kiếm theo tên
                mockMvc.perform(get("/staff/companies")
                                .param("q", "Digital")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.items", hasSize(1)))
                                .andExpect(jsonPath("$.data.items[0].name", containsString("Digital")));

                // 5c. Tìm kiếm theo mã số thuế
                mockMvc.perform(get("/staff/companies")
                                .param("q", companyB.getTaxCode())
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.items", hasSize(1)))
                                .andExpect(jsonPath("$.data.items[0].taxCode", is(companyB.getTaxCode())));

                // 5d. Lọc theo trạng thái
                mockMvc.perform(get("/staff/companies")
                                .param("status", "ACTIVE")
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))));
        }

        @Test
        @DisplayName("TEST 6 - STAFF Chi tiết doanh nghiệp: xem thông tin và danh sách HR trực thuộc")
        void staffGetCompanyDetail() throws Exception {
                mockMvc.perform(get("/staff/companies/{id}", companyB.getId())
                                .header("Authorization", staffToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data.id", is(companyB.getId().toString())))
                                .andExpect(jsonPath("$.data.name", is(companyB.getName())))
                                .andExpect(jsonPath("$.data.taxCode", is(companyB.getTaxCode())))
                                .andExpect(jsonPath("$.data.hrAccounts", hasSize(greaterThanOrEqualTo(1))))
                                .andExpect(jsonPath("$.data.hrAccounts[0].email", is("hr2@rikkeijobs.test")))
                                .andExpect(jsonPath("$.data.hrAccounts[0].status", is("ACTIVE")));

                // Khi id không tồn tại -> 404
                mockMvc.perform(get("/staff/companies/{id}", UUID.randomUUID())
                                .header("Authorization", staffToken))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TEST 7 - STAFF lọc danh sách công ty với status không hợp lệ → 400 Bad Request")
        void staffGetCompaniesInvalidStatusReturns400() throws Exception {
                mockMvc.perform(get("/staff/companies")
                                .param("status", "BANNED")
                                .header("Authorization", staffToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success", is(false)))
                                .andExpect(jsonPath("$.message", containsString("Trạng thái không hợp lệ")));
        }

        @Test
        @DisplayName("TEST 8 - countHrAccounts đếm chính xác số lượng HR theo công ty")
        void countHrAccountsReturnsAccurateCount() {
                var counts = authClient.countHrAccounts(List.of(companyB.getId()));
                assertThat(counts).containsKey(companyB.getId());
                assertThat(counts.get(companyB.getId())).isGreaterThanOrEqualTo(1L);
        }
}