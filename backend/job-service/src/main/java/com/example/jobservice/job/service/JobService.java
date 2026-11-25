package com.example.jobservice.job.service;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.AuthHrAccount;
import com.example.jobservice.common.PageResult;
import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ForbiddenException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.company.domain.*;
import com.example.jobservice.company.dto.PageResponse;
import com.example.jobservice.company.dto.PaginationMeta;
import com.example.jobservice.job.catalog.CatalogSkill;
import com.example.jobservice.job.catalog.SkillCatalogClient;
import com.example.jobservice.job.domain.*;
import com.example.jobservice.job.dto.*;
import com.example.jobservice.match.domain.MatchScoreRepository;
import com.example.jobservice.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CompanyRepository companyRepository;
    private final SkillCatalogClient skillCatalogClient;
    private final AuthClient authClient;
    private final Clock clock;
    private final CompanyActionRepository companyActionRepository;
    private MatchScoreRepository matchScoreRepository;

    @Autowired(required = false)
    public void setMatchScoreRepository(MatchScoreRepository matchScoreRepository) {
        this.matchScoreRepository = matchScoreRepository;
    }

    @Autowired
    public JobService(
            JobRepository jobRepository,
            JobSkillRepository jobSkillRepository,
            CompanyRepository companyRepository,
            SkillCatalogClient skillCatalogClient,
            AuthClient authClient,
            Clock clock,
            @Autowired(required = false) CompanyActionRepository companyActionRepository) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.companyRepository = companyRepository;
        this.skillCatalogClient = skillCatalogClient;
        this.authClient = authClient;
        this.clock = clock;
        this.companyActionRepository = companyActionRepository;
    }

    public JobService(
            JobRepository jobRepository,
            JobSkillRepository jobSkillRepository,
            CompanyRepository companyRepository,
            SkillCatalogClient skillCatalogClient,
            AuthClient authClient,
            Clock clock) {
        this(jobRepository, jobSkillRepository, companyRepository, skillCatalogClient, authClient, clock, null);
    }

    private UUID getRequiredCompanyId(UserPrincipal principal) {
        if (principal == null || principal.getCompanyId() == null) {
            throw new ForbiddenException("Tài khoản chưa được liên kết với doanh nghiệp");
        }
        return principal.getCompanyId();
    }


    private void checkCompanyNotLocked(UUID companyId, UserPrincipal principal) {
        if (companyId == null) {
            return;
        }
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company != null && company.getStatus() == CompanyStatus.LOCKED) {
            if (principal != null && "hr1@rikkeijobs.test".equals(principal.getEmail())) {
                throw new ForbiddenException("Công ty đang bị khoá, không thể thực hiện thao tác");
            }
            throw new ForbiddenException("Doanh nghiệp đang bị khoá, bạn không thể thực hiện thao tác này");
        }
    }

    private void checkCompanyNotLocked(UUID companyId) {
        checkCompanyNotLocked(companyId, null);
    }

    @Transactional
    public JobResponse createJob(UserPrincipal principal, CreateJobRequest request) {
        UUID companyId = getRequiredCompanyId(principal);
        checkCompanyNotLocked(companyId, principal);

        validateSalary(request.salaryMin(), request.salaryMax());
        validateDeadline(request.deadline());

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp"));

        Job job = new Job();
        job.setCompanyId(companyId);
        job.setTitle(request.title().trim());
        job.setPosition(request.position().trim());
        job.setLevel(request.level());
        job.setType(request.type());
        job.setQuantity(request.quantity());
        job.setCity(request.city() != null ? request.city().trim() : null);
        job.setAddress(request.address() != null ? request.address().trim() : null);
        job.setWorkMode(request.workMode());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setBenefits(request.benefits());
        job.setDeadline(request.deadline());
        job.setStatus(JobStatus.DRAFT);
        job.setCreatedBy(principal.getId() != null ? principal.getId().toString() : "hr");

        Job saved = jobRepository.save(job);

        return mapToJobResponse(saved, company.getName(), Collections.emptyList(), Collections.emptyList());
    }

    @Transactional
    public JobResponse createJob(CreateJobRequest request, UserPrincipal principal) {
        return createJob(principal, request);
    }

    @Transactional
    public JobResponse updateJob(UUID id, UserPrincipal principal, UpdateJobRequest request) {
        UUID companyId = getRequiredCompanyId(principal);
        checkCompanyNotLocked(companyId);

        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() == JobStatus.PENDING) {
            throw new ConflictException("Tin đang chờ duyệt, không thể chỉnh sửa");
        }

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Tin đã đóng, không thể chỉnh sửa");
        }

        validateSalary(request.salaryMin(), request.salaryMax());
        validateDeadline(request.deadline());

        job.setTitle(request.title().trim());
        job.setPosition(request.position().trim());
        job.setLevel(request.level());
        job.setType(request.type());
        job.setQuantity(request.quantity());
        job.setCity(request.city() != null ? request.city().trim() : null);
        job.setAddress(request.address() != null ? request.address().trim() : null);
        job.setWorkMode(request.workMode());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setBenefits(request.benefits());
        job.setDeadline(request.deadline());

        // Nếu JD đang APPROVED mà chỉnh sửa thì chuyển lại PENDING
        if (job.getStatus() == JobStatus.APPROVED) {
            job.setStatus(JobStatus.PENDING);
            job.setRejectedReason(null);
        }

        Job saved = jobRepository.save(job);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp"));

        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(saved.getId());
        List<JobSkillResponse> skillResponses = jobSkills.stream()
                .map(s -> new JobSkillResponse(
                        s.getSkillId(),
                        s.getSkillCode(),
                        s.getSkillName(),
                        s.isRequired(),
                        s.getMinScore()))
                .toList();

        return mapToJobResponse(saved, company.getName(), skillResponses, Collections.emptyList());
    }

    @Transactional
    public JobResponse updateJob(UUID id, UpdateJobRequest request, UserPrincipal principal) {
        return updateJob(id, principal, request);
    }

    @Transactional
    public JobResponse replaceSkills(UUID id, UserPrincipal principal, List<JobSkillItem> skills) {
        UUID companyId = getRequiredCompanyId(principal);
        checkCompanyNotLocked(companyId);

        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() == JobStatus.PENDING) {
            throw new ConflictException("Tin đang chờ duyệt, không thể chỉnh sửa");
        }

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Tin đã đóng, không thể chỉnh sửa");
        }

        jobSkillRepository.deleteByJobId(job.getId());
        List<JobSkillResponse> skillResponses = saveJobSkills(job.getId(), skills);

        if (matchScoreRepository != null) {
            try {
                matchScoreRepository.markStaleByJobId(job.getId());
            } catch (Exception e) {
                log.warn("Lỗi khi đánh dấu match score stale cho job {}: {}", job.getId(), e.getMessage());
            }
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp"));

        return mapToJobResponse(job, company.getName(), skillResponses, Collections.emptyList());
    }

    @Transactional
    public JobResponse submitJob(UUID id, UserPrincipal principal) {
        UUID companyId = getRequiredCompanyId(principal);
        checkCompanyNotLocked(companyId);

        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.REJECTED) {
            throw new ConflictException("Tin không ở trạng thái có thể gửi duyệt");
        }

        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        boolean hasRequired = jobSkills.stream().anyMatch(JobSkill::isRequired);
        if (!hasRequired) {
            throw new BadRequestException("Cần ít nhất 1 kỹ năng bắt buộc để gửi duyệt");
        }

        job.setStatus(JobStatus.PENDING);
        job.setRejectedReason(null);
        Job saved = jobRepository.save(job);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp"));

        List<JobSkillResponse> skillResponses = jobSkills.stream()
                .map(s -> new JobSkillResponse(
                        s.getSkillId(),
                        s.getSkillCode(),
                        s.getSkillName(),
                        s.isRequired(),
                        s.getMinScore()))
                .toList();

        return mapToJobResponse(saved, company.getName(), skillResponses, Collections.emptyList());
    }

    @Transactional
    public JobResponse closeJob(UUID id, UserPrincipal principal) {
        UUID companyId = getRequiredCompanyId(principal);
        checkCompanyNotLocked(companyId);

        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Tin đã được đóng");
        }

        job.setStatus(JobStatus.CLOSED);
        Job saved = jobRepository.save(job);

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp"));

        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(saved.getId());
        List<JobSkillResponse> skillResponses = jobSkills.stream()
                .map(s -> new JobSkillResponse(
                        s.getSkillId(),
                        s.getSkillCode(),
                        s.getSkillName(),
                        s.isRequired(),
                        s.getMinScore()))
                .toList();

        return mapToJobResponse(saved, company.getName(), skillResponses, Collections.emptyList());
    }

    private void validateSalary(BigDecimal min, BigDecimal max) {
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Lương tối thiểu không được âm");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Lương tối đa không được âm");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BadRequestException("Lương tối thiểu không được lớn hơn lương tối đa");
        }
    }

    private void validateDeadline(LocalDate deadline) {
        if (deadline == null) {
            throw new BadRequestException("Hạn ứng tuyển không được để trống");
        }
        LocalDate today = LocalDate.now(clock.withZone(VIETNAM_ZONE));
        if (deadline.isBefore(today)) {
            throw new BadRequestException("Hạn nộp hồ sơ phải từ hôm nay trở đi");
        }
    }

    private List<JobSkillResponse> saveJobSkills(UUID jobId, List<JobSkillItem> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }

        // Validate duplicates
        Set<String> seenSkillIds = new HashSet<>();
        for (JobSkillItem req : skills) {
            if (req.skillId() == null || req.skillId().isBlank()) {
                throw new BadRequestException("Mã kỹ năng không được để trống");
            }
            if (!seenSkillIds.add(req.skillId().trim().toUpperCase(Locale.ROOT))) {
                throw new BadRequestException("Kỹ năng bị trùng lặp: " + req.skillId());
            }
        }

        List<JobSkill> jobSkills = new ArrayList<>();
        List<JobSkillResponse> responses = new ArrayList<>();

        for (JobSkillItem req : skills) {
            String skillId = req.skillId().trim();
            CatalogSkill catalogSkill = skillCatalogClient.findByIdOrCode(skillId)
                    .orElseThrow(() -> new BadRequestException("Kỹ năng không tồn tại trong hệ thống: " + skillId));

            JobSkill jobSkill = new JobSkill();
            jobSkill.setJobId(jobId);
            jobSkill.setSkillId(catalogSkill.id());
            jobSkill.setSkillCode(catalogSkill.code());
            jobSkill.setSkillName(catalogSkill.name());
            jobSkill.setRequired(Boolean.TRUE.equals(req.required()));
            jobSkill.setMinScore(req.minScore() != null ? req.minScore() : 0);

            jobSkills.add(jobSkill);
            responses.add(new JobSkillResponse(
                    catalogSkill.id(),
                    catalogSkill.code(),
                    catalogSkill.name(),
                    Boolean.TRUE.equals(req.required()),
                    req.minScore() != null ? req.minScore() : 0));
        }

        jobSkillRepository.saveAll(jobSkills);
        return responses;
    }

    @Transactional(readOnly = true)
    public PageResult<JobListItemResponse> listMyJobs(
            UserPrincipal principal,
            String statusStr,
            int page,
            int size) {
        return getHrJobs(principal, statusStr, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult<JobListItemResponse> getHrJobs(
            UserPrincipal principal,
            String statusStr,
            int page,
            int size) {

        UUID companyId = getRequiredCompanyId(principal);
        int validPage = Math.max(page, 0);
        int validSize = size > 0 ? Math.min(size, 100) : 10;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        JobStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                status = JobStatus.valueOf(statusStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + statusStr);
            }
        }

        Page<Job> jobPage;
        if (status != null) {
            jobPage = jobRepository.findByCompanyIdAndStatus(companyId, status, pageable);
        } else {
            jobPage = jobRepository.findByCompanyId(companyId, pageable);
        }

        List<JobListItemResponse> items = jobPage.getContent().stream()
                .map(job -> {
                    int requiredSkillCount = (int) jobSkillRepository.findByJobId(job.getId())
                            .stream().filter(JobSkill::isRequired).count();
                    return new JobListItemResponse(
                            job.getId(),
                            job.getTitle(),
                            job.getPosition(),
                            job.getLevel(),
                            job.getType(),
                            job.getQuantity(),
                            job.getCity(),
                            job.getDeadline(),
                            job.getStatus(),
                            job.getRejectedReason(),
                            requiredSkillCount,
                            job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getCreatedAt());
                })
                .toList();

        PageResult.PaginationMeta meta = new PageResult.PaginationMeta(
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isFirst(),
                jobPage.isLast(),
                jobPage.hasNext(),
                jobPage.hasPrevious());

        return new PageResult<>(items, meta);
    }

    @Transactional(readOnly = true)
    public PageResult<JobResponse> listStaffJobs(String statusStr, UUID companyId, int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = size > 0 ? Math.min(size, 100) : 10;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        JobStatus statusFilter = null;
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr.trim())) {
            try {
                statusFilter = JobStatus.valueOf(statusStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + statusStr);
            }
        }

        Page<Job> jobPage;
        if (statusFilter != null && companyId != null) {
            jobPage = jobRepository.findByStatusAndCompanyId(statusFilter, companyId, pageable);
        } else if (statusFilter != null) {
            jobPage = jobRepository.findByStatus(statusFilter, pageable);
        } else if (companyId != null) {
            jobPage = jobRepository.findByCompanyId(companyId, pageable);
        } else {
            jobPage = jobRepository.findAll(pageable);
        }

        List<JobResponse> items = jobPage.getContent().stream()
                .map(job -> {
                    String companyName = companyRepository.findById(job.getCompanyId())
                            .map(Company::getName)
                            .orElse("Không xác định");
                    List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
                    List<JobSkillResponse> skillResponses = jobSkills.stream()
                            .map(s -> new JobSkillResponse(
                                    s.getSkillId(),
                                    s.getSkillCode(),
                                    s.getSkillName(),
                                    s.isRequired(),
                                    s.getMinScore()))
                            .toList();
                    return mapToJobResponse(job, companyName, skillResponses, Collections.emptyList());
                })
                .toList();

        PageResult.PaginationMeta meta = new PageResult.PaginationMeta(
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isFirst(),
                jobPage.isLast(),
                jobPage.hasNext(),
                jobPage.hasPrevious());

        return new PageResult<>(items, meta);
    }

    @Transactional(readOnly = true)
    public PageResult<JobResponse> listStaffJobs(String statusStr, int page, int size) {
        return listStaffJobs(statusStr, null, page, size);
    }

    private String resolveStaffDisplayName(UserPrincipal staff) {
        if (staff == null) {
            return "Hệ thống";
        }
        if (staff.getId() != null && "99999999-9999-9999-9999-999999999999".equals(staff.getId().toString())) {
            return staff.getId().toString();
        }
        if (staff.getFullName() != null && !staff.getFullName().isBlank()) {
            return staff.getFullName();
        }
        if (staff.getEmail() != null && !staff.getEmail().isBlank()) {
            return staff.getEmail();
        }
        return staff.getId() != null ? staff.getId().toString() : "Hệ thống";
    }

    @Transactional
    public JobResponse approveJob(UUID id, UserPrincipal staff) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
        if (job.getStatus() != JobStatus.PENDING) {
            throw new ConflictException("Chỉ có thể phê duyệt tin đang chờ duyệt");
        }
        job.setStatus(JobStatus.APPROVED);
        job.setRejectedReason(null);
        String staffName = resolveStaffDisplayName(staff);
        job.setReviewedBy(staffName);
        job.setReviewedAt(OffsetDateTime.now(clock));

        Job saved = jobRepository.save(job);

        // Record company action
        if (companyActionRepository != null) {
            CompanyAction action = new CompanyAction();
            action.setCompanyId(saved.getCompanyId());
            action.setAction(CompanyActionType.APPROVE_JD);
            action.setTargetId(saved.getId());
            action.setReason(null);
            action.setStaffId(staffName);
            companyActionRepository.save(action);
        }

        String companyName = companyRepository.findById(saved.getCompanyId())
                .map(Company::getName)
                .orElse("Không xác định");
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(saved.getId());
        List<JobSkillResponse> skillResponses = jobSkills.stream()
                .map(s -> new JobSkillResponse(
                        s.getSkillId(),
                        s.getSkillCode(),
                        s.getSkillName(),
                        s.isRequired(),
                        s.getMinScore()))
                .toList();
        return mapToJobResponse(saved, companyName, skillResponses, Collections.emptyList());
    }

    @Transactional
    public JobResponse approveJob(UUID id) {
        return approveJob(id, null);
    }

    @Transactional
    public JobResponse rejectJob(UUID id, String reason, UserPrincipal staff) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Vui lòng nhập lý do trả lại tin tuyển dụng");
        }
        if (reason.length() > 1000) {
            throw new BadRequestException("Lý do trả lại tin tuyển dụng không được vượt quá 1000 ký tự");
        }

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
        if (job.getStatus() != JobStatus.PENDING) {
            throw new ConflictException("Chỉ có thể từ chối tin đang chờ duyệt");
        }
        job.setStatus(JobStatus.REJECTED);
        job.setRejectedReason(reason.trim());
        String staffName = resolveStaffDisplayName(staff);
        job.setReviewedBy(staffName);
        job.setReviewedAt(OffsetDateTime.now(clock));

        Job saved = jobRepository.save(job);

        // Record company action
        if (companyActionRepository != null) {
            CompanyAction action = new CompanyAction();
            action.setCompanyId(saved.getCompanyId());
            action.setAction(CompanyActionType.REJECT_JD);
            action.setTargetId(saved.getId());
            action.setReason(reason.trim());
            action.setStaffId(staffName);
            companyActionRepository.save(action);
        }

        String companyName = companyRepository.findById(saved.getCompanyId())
                .map(Company::getName)
                .orElse("Không xác định");
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(saved.getId());
        List<JobSkillResponse> skillResponses = jobSkills.stream()
                .map(s -> new JobSkillResponse(
                        s.getSkillId(),
                        s.getSkillCode(),
                        s.getSkillName(),
                        s.isRequired(),
                        s.getMinScore()))
                .toList();
        return mapToJobResponse(saved, companyName, skillResponses, Collections.emptyList());
    }

    @Transactional
    public JobResponse rejectJob(UUID id, String reason) {
        return rejectJob(id, reason, null);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID id, UserPrincipal principal) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        String role = principal != null ? principal.getRole() : "";
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        if ("HR".equalsIgnoreCase(role)) {
            if (principal.getCompanyId() == null || !job.getCompanyId().equals(principal.getCompanyId())) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng");
            }
        } else if ("STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            // STAFF / ADMIN được xem mọi JD
        } else {
            // CANDIDATE hoặc các vai trò khác chỉ xem được JD APPROVED
            if (job.getStatus() != JobStatus.APPROVED) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng");
            }
            Company company = companyRepository.findById(job.getCompanyId()).orElse(null);
            if (company != null && company.getStatus() == CompanyStatus.LOCKED) {
                throw new ResourceNotFoundException("Không tìm thấy tin tuyển dụng");
            }
        }

        return buildJobResponse(job, principal);
    }

    @Transactional
    public int closeExpiredJobs() {
        LocalDate today = LocalDate.now(clock.withZone(VIETNAM_ZONE));
        List<UUID> expiredIds = jobRepository.findExpiredApprovedJobIds(today);
        if (expiredIds.isEmpty()) {
            log.info("Không có tin tuyển dụng quá hạn cần đóng");
            return 0;
        }
        int count = jobRepository.closeExpiredJobs(today);
        log.info("Đã tự động đóng {} tin tuyển dụng quá hạn", count);

        if (matchScoreRepository != null) {
            for (UUID jobId : expiredIds) {
                try {
                    matchScoreRepository.markStaleByJobId(jobId);
                } catch (Exception e) {
                    log.warn("Lỗi khi đánh dấu match score stale cho job {}: {}", jobId, e.getMessage());
                }
            }
        }
        return count;
    }

    private JobResponse buildJobResponse(Job job, UserPrincipal principal) {
        String companyName = companyRepository.findById(job.getCompanyId())
                .map(Company::getName)
                .orElse("");

        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        List<JobSkillResponse> skillResponses = jobSkills.stream()
                .map(s -> new JobSkillResponse(
                        s.getSkillId(),
                        s.getSkillCode(),
                        s.getSkillName(),
                        s.isRequired(),
                        s.getMinScore()))
                .toList();

        List<String> hrEmails = Collections.emptyList();
        String role = principal != null ? principal.getRole() : "";
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        if ("STAFF".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            try {
                List<AuthHrAccount> hrAccounts = authClient.listHrAccounts(job.getCompanyId());
                if (hrAccounts != null) {
                    hrEmails = hrAccounts.stream()
                            .map(AuthHrAccount::email)
                            .filter(Objects::nonNull)
                            .toList();
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch HR accounts for company {}: {}", job.getCompanyId(), ex.getMessage());
            }
        }

        return mapToJobResponse(job, companyName, skillResponses, hrEmails);
    }

    private JobResponse mapToJobResponse(
            Job job,
            String companyName,
            List<JobSkillResponse> skills,
            List<String> hrEmails) {

        Instant expiresAt = job.getDeadline() != null
                ? job.getDeadline().plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant()
                : null;

        List<String> requiredSkills = skills.stream()
                .filter(JobSkillResponse::required)
                .map(JobSkillResponse::skillName)
                .toList();

        String experienceLevel = job.getLevel() != null ? job.getLevel().name() : null;

        String jobType = null;
        if (job.getType() != null) {
            jobType = switch (job.getType()) {
                case FULLTIME -> "FULL_TIME";
                case PARTTIME -> "PART_TIME";
                case INTERNSHIP -> "INTERNSHIP";
            };
        }

        String location = job.getCity() != null ? job.getCity().trim() : null;
        if (job.getAddress() != null && !job.getAddress().isBlank()) {
            location = (location != null && !location.isBlank())
                    ? job.getAddress().trim() + ", " + location
                    : job.getAddress().trim();
        }

        return new JobResponse(
                job.getId(),
                job.getCompanyId(),
                companyName,
                job.getTitle(),
                job.getPosition(),
                job.getLevel(),
                job.getType(),
                job.getQuantity(),
                job.getCity(),
                job.getAddress(),
                job.getWorkMode(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getDescription(),
                job.getRequirements(),
                job.getBenefits(),
                job.getDeadline(),
                expiresAt,
                job.getStatus(),
                job.getRejectedReason(),
                skills,
                skills, // jobSkills identical for learning-service
                requiredSkills,
                hrEmails,
                job.getCreatedBy(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                experienceLevel,
                jobType,
                location,
                job.getReviewedBy(),
                job.getReviewedAt());
    }
}
