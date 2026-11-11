package com.example.jobservice.company;

import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.example.jobservice.company.dto.*;
import com.example.jobservice.company.service.CompanyService;
import com.example.jobservice.email.MockEmailService;
import com.example.jobservice.security.UserPrincipal;
import com.example.jobservice.user.domain.User;
import com.example.jobservice.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private MockEmailService mockEmailService;
    private CompanyService companyService;
    private Path tempUploadDir;

    @BeforeEach
    void setUp() throws IOException {
        mockEmailService = new MockEmailService();
        tempUploadDir = Files.createTempDirectory("test-logos-");
        companyService = new CompanyService(
                companyRepository,
                userRepository,
                passwordEncoder,
                mockEmailService,
                tempUploadDir.toString());
    }

    @Test
    @DisplayName("Tạo công ty thành công với mã số thuế 10 chữ số")
    void createCompany_with10DigitTaxCode_shouldSucceed() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Công ty Cổ phần Rikkei Test",
                "0109998888",
                "CNTT",
                "51-200",
                "Tầng 12 Keangnam",
                "Hà Nội",
                "https://rikkei.vn",
                "Mô tả công ty");

        when(companyRepository.existsByTaxCode("0109998888")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        CompanyResponse response = companyService.createCompany(request, "staff1@rikkei.vn");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.taxCode()).isEqualTo("0109998888");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.createdBy()).isEqualTo("staff1@rikkei.vn");
    }

    @Test
    @DisplayName("Tạo công ty thành công với mã số thuế 13 chữ số")
    void createCompany_with13DigitTaxCode_shouldSucceed() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Công ty Chi nhánh 13 số",
                "0109998888001",
                "Dịch vụ",
                "1-50",
                "Quận 1",
                "TP. HCM",
                null,
                null);

        when(companyRepository.existsByTaxCode("0109998888001")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setId(11L);
            return c;
        });

        CompanyResponse response = companyService.createCompany(request, "staff1@rikkei.vn");

        assertThat(response.taxCode()).isEqualTo("0109998888001");
    }

    @Test
    @DisplayName("Tiêu chí: Mã số thuế sai định dạng (không phải 10 hoặc 13 số) → Báo lỗi 400")
    void createCompany_withInvalidTaxCodeFormat_shouldThrowBadRequest() {
        CreateCompanyRequest request9Digits = new CreateCompanyRequest(
                "Công ty Sai Thuế",
                "010123456", // 9 digits
                "CNTT",
                "1-50",
                null, null, null, null);

        assertThatThrownBy(() -> companyService.createCompany(request9Digits, "staff1@rikkei.vn"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mã số thuế phải gồm 10 hoặc 13 chữ số");

        CreateCompanyRequest requestWithLetters = new CreateCompanyRequest(
                "Công ty Sai Thuế Chữ",
                "010123456A", // contains letter
                "CNTT",
                "1-50",
                null, null, null, null);

        assertThatThrownBy(() -> companyService.createCompany(requestWithLetters, "staff1@rikkei.vn"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mã số thuế phải gồm 10 hoặc 13 chữ số");
    }

    @Test
    @DisplayName("Tạo công ty trùng mã số thuế → 409 Conflict")
    void createCompany_duplicateTaxCode_shouldThrowConflict() {
        CreateCompanyRequest request = new CreateCompanyRequest(
                "Công ty Trùng Thuế",
                "0101234567",
                "CNTT",
                "1-50",
                null, null, null, null);

        when(companyRepository.existsByTaxCode("0101234567")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(request, "staff1@rikkei.vn"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Tiêu chí: staff1 tạo công ty + HR → Có email chứa mật khẩu tạm, must_change_password = true")
    void createHrAccount_shouldCreateUserAndSendEmail() {
        Long companyId = 1L;
        Company company = new Company();
        company.setId(companyId);
        company.setName("Công ty Rikkei A");

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.existsByEmail("hr.new@rikkei.vn")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedTempPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(55L);
            return u;
        });

        CreateHrAccountRequest request = new CreateHrAccountRequest("hr.new@rikkei.vn", "Nguyễn Văn Mới");
        HrAccountResponse response = companyService.createHrAccount(companyId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.email()).isEqualTo("hr.new@rikkei.vn");
        assertThat(response.role()).isEqualTo("ROLE_HR");
        assertThat(response.companyId()).isEqualTo(companyId);
        assertThat(response.mustChangePassword()).isTrue();

        // Kiểm tra email được gửi
        assertThat(mockEmailService.getSentEmails()).hasSize(1);
        MockEmailService.SentEmail sentEmail = mockEmailService.getSentEmails().get(0);
        assertThat(sentEmail.to()).isEqualTo("hr.new@rikkei.vn");
        assertThat(sentEmail.companyName()).isEqualTo("Công ty Rikkei A");
    }

    @Test
    @DisplayName("Tiêu chí: Tạo HR trùng email → 409 Conflict")
    void createHrAccount_duplicateEmail_shouldThrowConflict() {
        Long companyId = 1L;
        Company company = new Company();
        company.setId(companyId);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.existsByEmail("hr.existing@rikkei.vn")).thenReturn(true);

        CreateHrAccountRequest request = new CreateHrAccountRequest("hr.existing@rikkei.vn", "Nguyễn Văn Đã Có");

        assertThatThrownBy(() -> companyService.createHrAccount(companyId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("Tiêu chí: hr1 sửa được công ty A; gọi API sửa công ty B → 404")
    void updateCompanyById_hrAttemptsToEditOtherCompany_shouldThrow404() {
        UserPrincipal hr1 = UserPrincipal.of(10L, "hr1@rikkei.vn", "HR 1", "ROLE_HR", 1L); // thuộc công ty 1 (A)
        UpdateCompanyRequest updateReq = new UpdateCompanyRequest(
                "Tên mới",
                "CNTT",
                "51-200",
                "Địa chỉ mới",
                "Hà Nội",
                "https://new.vn",
                "Mô tả mới");

        // Gọi sửa công ty 2 (B) -> 404 Not Found
        assertThatThrownBy(() -> companyService.updateCompanyById(2L, hr1, updateReq))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy doanh nghiệp");
    }

    @Test
    @DisplayName("HR sửa công ty mình thành công và KHÔNG sửa được tax_code, status")
    void updateCompanyForCurrentHr_cannotModifyTaxCodeOrStatus() {
        UserPrincipal hr1 = UserPrincipal.of(10L, "hr1@rikkei.vn", "HR 1", "ROLE_HR", 1L);
        Company companyA = new Company();
        companyA.setId(1L);
        companyA.setName("Công ty A Gốc");
        companyA.setTaxCode("0101234567");
        companyA.setStatus(CompanyStatus.ACTIVE);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(companyA));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));

        UpdateCompanyRequest updateReq = new UpdateCompanyRequest(
                "Công ty A Đã Sửa",
                "Fintech",
                "201-1000",
                "Tầng 20 Keangnam",
                "Hà Nội",
                "https://rikkeia.vn",
                "Mô tả cập nhật");

        CompanyResponse response = companyService.updateCompanyForCurrentHr(hr1, updateReq);

        assertThat(response.name()).isEqualTo("Công ty A Đã Sửa");
        assertThat(response.industry()).isEqualTo("Fintech");
        assertThat(response.taxCode()).isEqualTo("0101234567"); // Giữ nguyên tax_code
        assertThat(response.status()).isEqualTo("ACTIVE"); // Giữ nguyên status
    }

    @Test
    @DisplayName("Tiêu chí: Logo > 2MB → Bị từ chối (400 Bad Request)")
    void uploadLogo_sizeExceeds2MB_shouldThrowBadRequest() {
        UserPrincipal hr1 = UserPrincipal.of(10L, "hr1@rikkei.vn", "HR 1", "ROLE_HR", 1L);
        Company company = new Company();
        company.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        // 2MB + 1 byte
        byte[] oversizedBytes = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large-logo.png", "image/png", oversizedBytes);

        assertThatThrownBy(() -> companyService.uploadLogo(hr1, file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("vượt quá 2MB");
    }

    @Test
    @DisplayName("Tiêu chí: Logo sai định dạng (không phải JPG/PNG) → Bị từ chối")
    void uploadLogo_invalidFormat_shouldThrowBadRequest() {
        UserPrincipal hr1 = UserPrincipal.of(10L, "hr1@rikkei.vn", "HR 1", "ROLE_HR", 1L);
        Company company = new Company();
        company.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        MockMultipartFile textFile = new MockMultipartFile("file", "document.pdf", "application/pdf",
                new byte[] { 1, 2, 3 });
        assertThatThrownBy(() -> companyService.uploadLogo(hr1, textFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG");

        MockMultipartFile svgFile = new MockMultipartFile("file", "logo.svg", "image/svg+xml", new byte[] { 1, 2, 3 });
        assertThatThrownBy(() -> companyService.uploadLogo(hr1, svgFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG");
    }

    @Test
    @DisplayName("Upload logo hợp lệ (PNG ≤ 2MB) → Thành công và lưu logo_url")
    void uploadLogo_validPng_shouldSucceed() {
        UserPrincipal hr1 = UserPrincipal.of(10L, "hr1@rikkei.vn", "HR 1", "ROLE_HR", 1L);
        Company company = new Company();
        company.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(i -> i.getArgument(0));

        // 8 byte đầu là chữ ký PNG thật
        byte[] pngBytes = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        MockMultipartFile validLogo = new MockMultipartFile("file", "logo.png", "image/png", pngBytes);
        LogoUploadResponse response = companyService.uploadLogo(hr1, validLogo);

        assertThat(response).isNotNull();
        assertThat(response.logoUrl()).startsWith("/uploads/logos/company-1-");
        assertThat(response.logoUrl()).endsWith(".png");
    }

    @Test
    @DisplayName("Logo khai báo image/png nhưng nội dung không phải ảnh → Bị từ chối")
    void uploadLogo_fakeImageContent_shouldThrowBadRequest() {
        UserPrincipal hr1 = UserPrincipal.of(10L, "hr1@rikkei.vn", "HR 1", "ROLE_HR", 1L);
        Company company = new Company();
        company.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        MockMultipartFile fake = new MockMultipartFile("file", "virus.png", "image/png",
                "<html>not an image</html>".getBytes());

        assertThatThrownBy(() -> companyService.uploadLogo(hr1, fake))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG");
    }
}
