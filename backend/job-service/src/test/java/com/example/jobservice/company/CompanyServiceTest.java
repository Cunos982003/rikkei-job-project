
package com.example.jobservice.company;

import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.example.jobservice.company.dto.*;
import com.example.jobservice.company.service.HrAccountCreatedEvent;
import com.example.jobservice.company.service.HrAccountEmailListener;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

        private static final UUID COMPANY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

        private static final UUID OTHER_COMPANY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

        private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

        private static final UUID HR_USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

        @Mock
        private CompanyRepository companyRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private ApplicationEventPublisher eventPublisher;

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
                                eventPublisher,
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
                        Company company = invocation.getArgument(0);
                        company.setId(COMPANY_ID);
                        return company;
                });

                CompanyResponse response = companyService.createCompany(request, "staff1@rikkei.vn");

                assertThat(response).isNotNull();
                assertThat(response.id()).isEqualTo(COMPANY_ID);
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
                        Company company = invocation.getArgument(0);
                        company.setId(COMPANY_ID);
                        return company;
                });

                CompanyResponse response = companyService.createCompany(request, "staff1@rikkei.vn");

                assertThat(response.taxCode()).isEqualTo("0109998888001");
        }

        @Test
        @DisplayName("Mã số thuế sai định dạng → Báo lỗi 400")
        void createCompany_withInvalidTaxCodeFormat_shouldThrowBadRequest() {
                CreateCompanyRequest request9Digits = new CreateCompanyRequest(
                                "Công ty Sai Thuế",
                                "010123456",
                                "CNTT",
                                "1-50",
                                null, null, null, null);

                assertThatThrownBy(() -> companyService.createCompany(request9Digits, "staff1@rikkei.vn"))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Mã số thuế phải gồm 10 hoặc 13 chữ số");

                CreateCompanyRequest requestWithLetters = new CreateCompanyRequest(
                                "Công ty Sai Thuế Chữ",
                                "010123456A",
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
        @DisplayName("Tạo HR thành công và gửi email")
        void createHrAccount_shouldCreateUserAndSendEmail() {
                Company company = new Company();
                company.setId(COMPANY_ID);
                company.setName("Công ty Rikkei A");

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(company));

                when(userRepository.existsByEmail("hr.new@rikkei.vn"))
                                .thenReturn(false);

                when(passwordEncoder.encode(anyString()))
                                .thenReturn("$2a$10$encodedTempPassword");

                when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        user.setId(HR_USER_ID);
                        return user;
                });

                CreateHrAccountRequest request = new CreateHrAccountRequest(
                                "hr.new@rikkei.vn",
                                "Nguyễn Văn Mới");

                HrAccountResponse response = companyService.createHrAccount(COMPANY_ID, request);

                assertThat(response).isNotNull();
                assertThat(response.getId()).isEqualTo(HR_USER_ID);
                assertThat(response.getEmail()).isEqualTo("hr.new@rikkei.vn");
                assertThat(response.getRole()).isEqualTo("ROLE_HR");
                assertThat(response.getCompanyId()).isEqualTo(COMPANY_ID);
                assertThat(response.isMustChangePassword()).isTrue();

                org.mockito.ArgumentCaptor<HrAccountCreatedEvent> eventCaptor =
                                org.mockito.ArgumentCaptor.forClass(HrAccountCreatedEvent.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());
                new HrAccountEmailListener(mockEmailService).sendCredentials(eventCaptor.getValue());

                assertThat(response.isEmailSent()).isTrue();
                assertThat(mockEmailService.getSentEmails()).hasSize(1);

                MockEmailService.SentEmail sentEmail = mockEmailService.getSentEmails().get(0);

                assertThat(sentEmail.to()).isEqualTo("hr.new@rikkei.vn");
                assertThat(sentEmail.companyName()).isEqualTo("Công ty Rikkei A");
        }

        @Test
        @DisplayName("Tạo HR trùng email → 409 Conflict")
        void createHrAccount_duplicateEmail_shouldThrowConflict() {
                Company company = new Company();
                company.setId(COMPANY_ID);

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(company));

                when(userRepository.existsByEmail("hr.existing@rikkei.vn"))
                                .thenReturn(true);

                CreateHrAccountRequest request = new CreateHrAccountRequest(
                                "hr.existing@rikkei.vn",
                                "Nguyễn Văn Đã Có");

                assertThatThrownBy(() -> companyService.createHrAccount(COMPANY_ID, request))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("đã tồn tại");
        }

        @Test
        @DisplayName("HR không được sửa công ty khác")
        void updateCompanyById_hrAttemptsToEditOtherCompany_shouldThrow404() {
                UserPrincipal hr1 = UserPrincipal.of(
                                USER_ID,
                                "hr1@rikkei.vn",
                                "HR 1",
                                "ROLE_HR",
                                COMPANY_ID);

                UpdateCompanyRequest updateReq = new UpdateCompanyRequest(
                                "Tên mới",
                                "CNTT",
                                "51-200",
                                "Địa chỉ mới",
                                "Hà Nội",
                                "https://new.vn",
                                "Mô tả mới");

                assertThatThrownBy(() -> companyService.updateCompanyById(
                                OTHER_COMPANY_ID,
                                hr1,
                                updateReq))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Không tìm thấy doanh nghiệp");
        }

        @Test
        @DisplayName("HR sửa công ty mình thành công và không sửa được tax_code, status")
        void updateCompanyForCurrentHr_cannotModifyTaxCodeOrStatus() {
                UserPrincipal hr1 = UserPrincipal.of(
                                USER_ID,
                                "hr1@rikkei.vn",
                                "HR 1",
                                "ROLE_HR",
                                COMPANY_ID);

                Company companyA = new Company();
                companyA.setId(COMPANY_ID);
                companyA.setName("Công ty A Gốc");
                companyA.setTaxCode("0101234567");
                companyA.setStatus(CompanyStatus.ACTIVE);

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(companyA));

                when(companyRepository.save(any(Company.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

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
                assertThat(response.taxCode()).isEqualTo("0101234567");
                assertThat(response.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Logo lớn hơn 2MB → Bị từ chối")
        void uploadLogo_sizeExceeds2MB_shouldThrowBadRequest() {
                UserPrincipal hr1 = UserPrincipal.of(
                                USER_ID, "hr1@rikkei.vn", "HR 1", "ROLE_HR", COMPANY_ID);

                Company company = new Company();
                company.setId(COMPANY_ID);

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(company));

                byte[] oversizedBytes = new byte[2 * 1024 * 1024 + 1];

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "large-logo.png",
                                "image/png",
                                oversizedBytes);

                assertThatThrownBy(() -> companyService.uploadLogo(hr1, file))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("vượt quá 2MB");
        }

        @Test
        @DisplayName("Logo sai định dạng → Bị từ chối")
        void uploadLogo_invalidFormat_shouldThrowBadRequest() {
                UserPrincipal hr1 = UserPrincipal.of(
                                USER_ID, "hr1@rikkei.vn", "HR 1", "ROLE_HR", COMPANY_ID);

                Company company = new Company();
                company.setId(COMPANY_ID);

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(company));

                MockMultipartFile textFile = new MockMultipartFile(
                                "file", "document.pdf", "application/pdf",
                                new byte[] { 1, 2, 3 });

                assertThatThrownBy(() -> companyService.uploadLogo(hr1, textFile))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG");

                MockMultipartFile svgFile = new MockMultipartFile(
                                "file", "logo.svg", "image/svg+xml",
                                new byte[] { 1, 2, 3 });

                assertThatThrownBy(() -> companyService.uploadLogo(hr1, svgFile))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG");
        }

        @Test
        @DisplayName("Upload logo PNG hợp lệ → Thành công")
        void uploadLogo_validPng_shouldSucceed() {
                UserPrincipal hr1 = UserPrincipal.of(
                                USER_ID, "hr1@rikkei.vn", "HR 1", "ROLE_HR", COMPANY_ID);

                Company company = new Company();
                company.setId(COMPANY_ID);

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(company));

                when(companyRepository.save(any(Company.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                byte[] pngBytes = {
                                (byte) 0x89, 0x50, 0x4E, 0x47,
                                0x0D, 0x0A, 0x1A, 0x0A
                };

                MockMultipartFile validLogo = new MockMultipartFile(
                                "file", "logo.png", "image/png", pngBytes);

                LogoUploadResponse response = companyService.uploadLogo(hr1, validLogo);

                assertThat(response).isNotNull();
                assertThat(response.logoUrl())
                                .startsWith("/uploads/logos/company-" + COMPANY_ID + "-");
                assertThat(response.logoUrl()).endsWith(".png");
        }

        @Test
        @DisplayName("Upload logo mới xóa logo cũ")
        void uploadLogo_replacesPreviousLogo() throws IOException {
                UserPrincipal hr1 = UserPrincipal.of(
                                USER_ID, "hr1@rikkei.vn", "HR 1", "ROLE_HR", COMPANY_ID);

                Company company = new Company();
                company.setId(COMPANY_ID);
                String oldFilename = "company-" + COMPANY_ID + "-old.png";
                Path oldLogo = tempUploadDir.resolve(oldFilename);
                Files.write(oldLogo, new byte[] { 1 });
                company.setLogoUrl("/uploads/logos/" + oldFilename);

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(company));
                when(companyRepository.save(any(Company.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                MockMultipartFile file = new MockMultipartFile(
                                "file", "logo.png", "image/png",
                                new byte[] {
                                                (byte) 0x89, 0x50, 0x4E, 0x47,
                                                0x0D, 0x0A, 0x1A, 0x0A
                                });

                companyService.uploadLogo(hr1, file);

                assertThat(oldLogo).doesNotExist();
        }

        @Test
        @DisplayName("Nội dung file giả ảnh PNG → Bị từ chối")
        void uploadLogo_fakeImageContent_shouldThrowBadRequest() {
                UserPrincipal hr1 = UserPrincipal.of(
                                USER_ID, "hr1@rikkei.vn", "HR 1", "ROLE_HR", COMPANY_ID);

                Company company = new Company();
                company.setId(COMPANY_ID);

                when(companyRepository.findById(COMPANY_ID))
                                .thenReturn(Optional.of(company));

                MockMultipartFile fake = new MockMultipartFile(
                                "file",
                                "virus.png",
                                "image/png",
                                "<html>not an image</html>".getBytes());

                assertThatThrownBy(() -> companyService.uploadLogo(hr1, fake))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Chỉ chấp nhận ảnh định dạng JPG hoặc PNG");
        }
}
