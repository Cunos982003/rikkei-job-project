package com.example.jobservice.company.service;

import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.example.jobservice.company.dto.*;
import com.example.jobservice.email.EmailService;
import com.example.jobservice.security.UserPrincipal;
import com.example.jobservice.user.domain.User;
import com.example.jobservice.user.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CompanyService {

    private static final Pattern TAX_CODE_PATTERN = Pattern.compile("^(\\d{10}|\\d{13})$");
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/jpg");
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2MB

    // Chữ ký (magic bytes) đầu file để xác định đúng là ảnh PNG / JPEG
    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
    private static final byte[] JPEG_SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    private static final String INVALID_IMAGE_MESSAGE = "Chỉ chấp nhận ảnh định dạng JPG hoặc PNG";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String uploadDir;
    private final SecureRandom secureRandom = new SecureRandom();

    public CompanyService(
            CompanyRepository companyRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.upload.dir:uploads/logos}") String uploadDir) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request, String createdBy) {
        String taxCode = request.taxCode() != null ? request.taxCode().trim() : "";

        // Validate tax code: 10 or 13 digits
        if (!TAX_CODE_PATTERN.matcher(taxCode).matches()) {
            throw new BadRequestException("Mã số thuế phải gồm 10 hoặc 13 chữ số");
        }

        // Check uniqueness of tax code
        if (companyRepository.existsByTaxCode(taxCode)) {
            throw new ConflictException("Mã số thuế " + taxCode + " đã tồn tại trên hệ thống");
        }

        Company company = new Company();
        company.setName(request.name().trim());
        company.setTaxCode(taxCode);
        company.setIndustry(request.industry());
        company.setSize(request.size());
        company.setAddress(request.address());
        company.setCity(request.city());
        company.setWebsite(request.website());
        company.setDescription(request.description());
        company.setStatus(CompanyStatus.ACTIVE);
        company.setCreatedBy(createdBy != null ? createdBy : "staff");

        Company saved = companyRepository.save(company);
        return CompanyResponse.fromEntity(saved);
    }

    @Transactional
    public HrAccountResponse createHrAccount(Long companyId, CreateHrAccountRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp có id: " + companyId));

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Email trùng với bất kỳ user nào → 409
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email " + email + " đã tồn tại trên hệ thống");
        }

        String temporaryPassword = generateTemporaryPassword();

        User user = new User();
        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setRole("ROLE_HR");
        user.setCompanyId(companyId);
        user.setMustChangePassword(true);

        User savedUser = userRepository.save(user);

        // Mật khẩu tạm chỉ được gửi qua email, KHÔNG trả về trong response
        emailService.sendHrAccountCredentials(
                savedUser.getEmail(), savedUser.getFullName(), company.getName(), temporaryPassword);

        return new HrAccountResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                savedUser.getCompanyId(),
                savedUser.isMustChangePassword(),
                "Tạo tài khoản HR thành công. Thông tin đăng nhập đã được gửi tới email " + email);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyForCurrentHr(UserPrincipal principal) {
        Company company = findCompanyOfHr(principal);
        return CompanyResponse.fromEntity(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp có id: " + companyId));
        return CompanyResponse.fromEntity(company);
    }

    @Transactional
    public CompanyResponse updateCompanyForCurrentHr(UserPrincipal principal, UpdateCompanyRequest request) {
        Company company = findCompanyOfHr(principal);

        applyCompanyUpdates(company, request);
        Company updated = companyRepository.save(company);
        return CompanyResponse.fromEntity(updated);
    }

    @Transactional
    public CompanyResponse updateCompanyById(Long targetCompanyId, UserPrincipal principal,
            UpdateCompanyRequest request) {
        // Tiêu chí: hr1 sửa được công ty A; gọi API sửa công ty B → 404
        if ("ROLE_HR".equals(principal.getRole()) || "HR".equals(principal.getRole())) {
            if (principal.getCompanyId() == null || !principal.getCompanyId().equals(targetCompanyId)) {
                throw new ResourceNotFoundException("Không tìm thấy doanh nghiệp có id: " + targetCompanyId);
            }
        }

        Company company = companyRepository.findById(targetCompanyId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp có id: " + targetCompanyId));

        applyCompanyUpdates(company, request);
        Company updated = companyRepository.save(company);
        return CompanyResponse.fromEntity(updated);
    }

    @Transactional
    public LogoUploadResponse uploadLogo(UserPrincipal principal, MultipartFile file) {
        Company company = findCompanyOfHr(principal);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tệp tải lên không được để trống");
        }

        // 1. Kích thước ≤ 2MB
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BadRequestException("Kích thước tệp logo không được vượt quá 2MB");
        }

        // 2. Content-Type khai báo phải là JPG/PNG
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException(INVALID_IMAGE_MESSAGE);
        }

        // 3. Nội dung thật của file phải đúng là PNG/JPEG (không tin tên file hay
        // Content-Type do client gửi)
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc tệp tải lên");
        }

        String extension = detectImageExtension(content);
        if (extension == null) {
            throw new BadRequestException(INVALID_IMAGE_MESSAGE);
        }

        // 4. Lưu file với tên do server sinh (UUID), không dùng tên client gửi → tránh
        // path traversal
        try {
            Path targetDir = Paths.get(uploadDir);
            Files.createDirectories(targetDir);

            String filename = "company-" + company.getId() + "-" + UUID.randomUUID() + "." + extension;
            Files.write(targetDir.resolve(filename), content);

            String logoUrl = "/uploads/logos/" + filename;
            company.setLogoUrl(logoUrl);
            companyRepository.save(company);

            return new LogoUploadResponse(logoUrl, "Tải lên logo thành công");
        } catch (IOException e) {
            // Không đưa e.getMessage() ra ngoài vì có thể lộ đường dẫn thư mục trên server
            throw new BadRequestException("Không thể lưu tệp tải lên");
        }
    }

    private Company findCompanyOfHr(UserPrincipal principal) {
        Long companyId = principal.getCompanyId();
        if (companyId == null) {
            throw new ResourceNotFoundException("Tài khoản HR chưa được gán cho doanh nghiệp nào");
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin doanh nghiệp"));
    }

    private void applyCompanyUpdates(Company company, UpdateCompanyRequest request) {
        if (request.name() != null && !request.name().isBlank()) {
            company.setName(request.name().trim());
        }
        if (request.industry() != null) {
            company.setIndustry(request.industry().trim());
        }
        if (request.size() != null) {
            company.setSize(request.size().trim());
        }
        if (request.address() != null) {
            company.setAddress(request.address().trim());
        }
        if (request.city() != null) {
            company.setCity(request.city().trim());
        }
        if (request.website() != null) {
            company.setWebsite(request.website().trim());
        }
        if (request.description() != null) {
            company.setDescription(request.description().trim());
        }
        // tax_code và status cố ý KHÔNG được sửa ở đây
    }

    /**
     * Trả về "png" hoặc "jpg" theo nội dung thật của file, hoặc null nếu không phải
     * ảnh hợp lệ.
     */
    private String detectImageExtension(byte[] content) {
        if (startsWith(content, PNG_SIGNATURE)) {
            return "png";
        }
        if (startsWith(content, JPEG_SIGNATURE)) {
            return "jpg";
        }
        return null;
    }

    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }
}