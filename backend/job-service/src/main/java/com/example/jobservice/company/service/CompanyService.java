package com.example.jobservice.company.service;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.AuthHrAccount;
import com.example.jobservice.common.PageResult;
import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.domain.CompanyStatus;
import com.example.jobservice.company.dto.*;
import com.example.jobservice.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private static final Pattern TAX_CODE_PATTERN = Pattern.compile("^(\\d{10}|\\d{13})$");

    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg");

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final byte[] JPEG_SIGNATURE = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    private static final String INVALID_IMAGE_MESSAGE = "Chỉ chấp nhận ảnh định dạng JPG hoặc PNG";

    private final CompanyRepository companyRepository;
    private final AuthClient authClient;
    private final String uploadDir;

    public CompanyService(
            CompanyRepository companyRepository,
            AuthClient authClient,
            @Value("${app.upload.dir:uploads/logos}") String uploadDir) {

        this.companyRepository = companyRepository;
        this.authClient = authClient;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public CompanyResponse createCompany(
            CreateCompanyRequest request,
            String createdBy) {

        String taxCode = request.taxCode() != null
                ? request.taxCode().trim()
                : "";

        if (!TAX_CODE_PATTERN.matcher(taxCode).matches()) {
            throw new BadRequestException(
                    "Mã số thuế phải gồm 10 hoặc 13 chữ số");
        }

        if (companyRepository.existsByTaxCode(taxCode)) {
            throw new ConflictException(
                    "Mã số thuế " + taxCode + " đã tồn tại trên hệ thống");
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
        company.setCreatedBy(
                createdBy != null ? createdBy : "staff");

        Company saved = companyRepository.save(company);

        return CompanyResponse.fromEntity(saved);
    }

    public HrAccountResponse createHrAccount(
            UUID companyId,
            CreateHrAccountRequest request) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy doanh nghiệp có id: " + companyId));

        String email = request.email() != null
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : "";
        String fullName = request.fullName() != null
                ? request.fullName().trim()
                : "";

        AuthHrAccount authAccount = authClient.createHrAccount(email, fullName, companyId);

        String message = authAccount.emailSent()
                ? "Tạo tài khoản HR thành công. Thông tin đăng nhập đã được gửi tới email " + authAccount.email()
                : "Đã tạo tài khoản nhưng gửi email thất bại, hãy gửi mật khẩu tạm thủ công";

        return new HrAccountResponse(
                authAccount.id(),
                authAccount.email(),
                authAccount.fullName(),
                "ROLE_HR",
                authAccount.companyId(),
                authAccount.mustChangePassword(),
                authAccount.temporaryPassword(),
                authAccount.emailSent(),
                message);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyForCurrentHr(
            UserPrincipal principal) {

        Company company = findCompanyOfHr(principal);

        return CompanyResponse.fromEntity(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy doanh nghiệp có id: " + companyId));

        return CompanyResponse.fromEntity(company);
    }

    @Transactional(readOnly = true)
    public PageResult<CompanyListItemResponse> getStaffCompanies(
            String q,
            String statusStr,
            int page,
            int size) {

        int validPage = Math.max(page, 0);
        int validSize = size > 0 ? Math.min(size, 100) : 10;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        CompanyStatus filterStatus = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                filterStatus = CompanyStatus.valueOf(statusStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + statusStr);
            }
        }

        final CompanyStatus finalFilterStatus = filterStatus;
        Specification<Company> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.trim().isEmpty()) {
                String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate taxLike = cb.like(root.get("taxCode"), pattern);
                predicates.add(cb.or(nameLike, taxLike));
            }

            if (finalFilterStatus != null) {
                predicates.add(cb.equal(root.get("status"), finalFilterStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Company> companyPage = companyRepository.findAll(spec, pageable);
        List<UUID> companyIds = companyPage.getContent().stream().map(Company::getId).toList();

        Map<UUID, Long> hrCounts = new HashMap<>();
        if (!companyIds.isEmpty()) {
            try {
                hrCounts = authClient.countHrAccounts(companyIds);
            } catch (Exception e) {
                log.warn("Failed to get HR counts from auth service: {}", e.getMessage());
            }
        }

        Map<UUID, Long> finalHrCounts = hrCounts != null ? hrCounts : Map.of();
        List<CompanyListItemResponse> items = companyPage.getContent().stream()
                .map(comp -> CompanyListItemResponse.fromEntity(
                        comp,
                        finalHrCounts.getOrDefault(comp.getId(), 0L),
                        0L))
                .toList();

        PageResult.PaginationMeta meta = new PageResult.PaginationMeta(
                companyPage.getNumber(),
                companyPage.getSize(),
                companyPage.getTotalElements(),
                companyPage.getTotalPages(),
                companyPage.isFirst(),
                companyPage.isLast(),
                companyPage.hasNext(),
                companyPage.hasPrevious());

        return new PageResult<>(items, meta);
    }

    @Transactional(readOnly = true)
    public StaffCompanyDetailResponse getStaffCompanyDetail(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy doanh nghiệp có id: " + companyId));

        List<AuthHrAccount> hrUsers = authClient.listHrAccounts(companyId);
        List<StaffHrAccountResponse> hrAccounts = hrUsers.stream()
                .map(StaffHrAccountResponse::fromAuthHrAccount)
                .toList();

        return StaffCompanyDetailResponse.fromEntity(company, hrAccounts);
    }

    public StaffHrAccountResponse lockHrAccount(UUID hrId, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException("Lý do khóa tài khoản không được để trống");
        }

        AuthHrAccount account = authClient.lockHrAccount(hrId, reason.trim());
        return StaffHrAccountResponse.fromAuthHrAccount(account);
    }

    public StaffHrAccountResponse unlockHrAccount(UUID hrId) {
        AuthHrAccount account = authClient.unlockHrAccount(hrId);
        return StaffHrAccountResponse.fromAuthHrAccount(account);
    }

    @Transactional
    public CompanyResponse updateCompanyForCurrentHr(
            UserPrincipal principal,
            UpdateCompanyRequest request) {

        Company company = findCompanyOfHr(principal);

        applyCompanyUpdates(company, request);

        Company updated = companyRepository.save(company);

        return CompanyResponse.fromEntity(updated);
    }

    @Transactional
    public CompanyResponse updateCompanyById(
            UUID targetCompanyId,
            UserPrincipal principal,
            UpdateCompanyRequest request) {

        if ("ROLE_HR".equals(principal.getRole())
                || "HR".equals(principal.getRole())) {
            UUID hrCompanyId = principal.getCompanyId();

            if (hrCompanyId == null
                    || !hrCompanyId.equals(targetCompanyId)) {
                throw new ResourceNotFoundException(
                        "Không tìm thấy doanh nghiệp có id: "
                                + targetCompanyId);
            }
        }

        Company company = companyRepository.findById(targetCompanyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy doanh nghiệp có id: "
                                + targetCompanyId));

        applyCompanyUpdates(company, request);

        Company updated = companyRepository.save(company);

        return CompanyResponse.fromEntity(updated);
    }

    @Transactional
    public LogoUploadResponse uploadLogo(
            UserPrincipal principal,
            MultipartFile file) {

        Company company = findCompanyOfHr(principal);
        return uploadLogoForCompany(company, file);
    }

    @Transactional
    public LogoUploadResponse uploadCompanyLogoById(
            UUID companyId,
            MultipartFile file) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy doanh nghiệp có id: " + companyId));

        return uploadLogoForCompany(company, file);
    }

    private LogoUploadResponse uploadLogoForCompany(
            Company company,
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException(
                    "Tệp tải lên không được để trống");
        }

        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new BadRequestException(
                    "Kích thước tệp logo không được vượt quá 2MB");
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException(INVALID_IMAGE_MESSAGE);
        }

        byte[] content;

        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException(
                    "Không thể đọc tệp tải lên");
        }

        String extension = detectImageExtension(content);

        if (extension == null) {
            throw new BadRequestException(INVALID_IMAGE_MESSAGE);
        }

        try {
            Path targetDir = Paths.get(uploadDir);

            Files.createDirectories(targetDir);

            String filename = "company-"
                    + company.getId()
                    + "-"
                    + UUID.randomUUID()
                    + "."
                    + extension;

            Files.write(targetDir.resolve(filename), content);

            String logoUrl = "/uploads/logos/" + filename;

            company.setLogoUrl(logoUrl);

            companyRepository.save(company);

            return new LogoUploadResponse(
                    logoUrl,
                    "Tải lên logo thành công");

        } catch (IOException e) {
            throw new BadRequestException(
                    "Không thể lưu tệp tải lên");
        }
    }

    private Company findCompanyOfHr(UserPrincipal principal) {

        UUID companyId = principal.getCompanyId();

        if (companyId == null) {
            throw new ResourceNotFoundException(
                    "Tài khoản HR chưa được gán cho doanh nghiệp nào");
        }

        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thông tin doanh nghiệp"));
    }

    private void applyCompanyUpdates(
            Company company,
            UpdateCompanyRequest request) {

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
    }

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
}
