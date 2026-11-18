package com.example.jobservice.job.service;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.auth.client.AuthHrAccount;
import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ForbiddenException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.company.dto.PageResponse;
import com.example.jobservice.company.dto.PaginationMeta;
import com.example.jobservice.job.catalog.CatalogSkill;
import com.example.jobservice.job.catalog.SkillCatalogClient;
import com.example.jobservice.job.domain.*;
import com.example.jobservice.job.dto.*;
import com.example.jobservice.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public JobService(
            JobRepository jobRepository,
            JobSkillRepository jobSkillRepository,
            CompanyRepository companyRepository,
            SkillCatalogClient skillCatalogClient,
            AuthClient authClient,
            Clock clock) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.companyRepository = companyRepository;
        this.skillCatalogClient = skillCatalogClient;
        this.authClient = authClient;
        this.clock = clock;
    }

    private UUID getRequiredCompanyId(UserPrincipal principal) {
        if (principal == null || principal.getCompanyId() == null) {
            throw new ForbiddenException("Tài khoản chưa được liên kết với doanh nghiệp");
        }
        return principal.getCompanyId();
    }

    private void validateJobContent(LocalDate deadline, BigDecimal salaryMin, BigDecimal salaryMax, Integer quantity) {
        LocalDate today = LocalDate.now(clock);
        if (deadline == null || deadline.isBefore(today)) {
            throw new BadRequestException("Hạn nộp hồ sơ phải từ hôm nay trở đi");
        }
        if (quantity != null && quantity < 1) {
            throw new BadRequestException("Số lượng tuyển phải lớn hơn hoặc bằng 1");
        }
        if (salaryMin != null && salaryMin.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Lương không được âm");
        }
        if (salaryMax != null && salaryMax.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Lương không được âm");
        }
        if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
            throw new BadRequestException("Lương tối thiểu không được lớn hơn lương tối đa");
        }
    }

    public JobResponse createJob(UserPrincipal principal, CreateJobRequest request) {
        UUID companyId = getRequiredCompanyId(principal);
        validateJobContent(request.deadline(), request.salaryMin(), request.salaryMax(), request.quantity());

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy doanh nghiệp có id: " + companyId));

        Job job = new Job();
        job.setCompanyId(companyId);
        job.setTitle(request.title());
        job.setPosition(request.position());
        job.setLevel(request.level());
        job.setType(request.type());
        job.setQuantity(request.quantity());
        job.setCity(request.city());
        job.setAddress(request.address());
        job.setWorkMode(request.workMode());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setBenefits(request.benefits());
        job.setDeadline(request.deadline());
        job.setStatus(JobStatus.DRAFT);
        job.setCreatedBy(principal.getEmail());

        Job saved = jobRepository.save(job);
        return mapToJobResponse(saved, company.getName(), Collections.emptyList(), Collections.emptyList());
    }

    public JobResponse updateJob(UUID id, UserPrincipal principal, UpdateJobRequest request) {
        UUID companyId = getRequiredCompanyId(principal);
        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() == JobStatus.PENDING) {
            throw new ConflictException("Tin đang chờ duyệt, không thể chỉnh sửa");
        }
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Tin đã đóng, không thể chỉnh sửa");
        }

        validateJobContent(request.deadline(), request.salaryMin(), request.salaryMax(), request.quantity());

        if (job.getStatus() == JobStatus.APPROVED) {
            job.setStatus(JobStatus.PENDING);
            job.setRejectedReason(null);
        }

        job.setTitle(request.title());
        job.setPosition(request.position());
        job.setLevel(request.level());
        job.setType(request.type());
        job.setQuantity(request.quantity());
        job.setCity(request.city());
        job.setAddress(request.address());
        job.setWorkMode(request.workMode());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setBenefits(request.benefits());
        job.setDeadline(request.deadline());

        Job updated = jobRepository.save(job);
        return buildJobResponse(updated, principal);
    }

    public JobResponse replaceSkills(UUID id, UserPrincipal principal, List<JobSkillItem> skillItems) {
        UUID companyId = getRequiredCompanyId(principal);
        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() == JobStatus.PENDING) {
            throw new ConflictException("Tin đang chờ duyệt, không thể chỉnh sửa");
        }
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Tin đã đóng, không thể chỉnh sửa");
        }

        if (job.getStatus() == JobStatus.APPROVED) {
            job.setStatus(JobStatus.PENDING);
            job.setRejectedReason(null);
        }

        List<JobSkillItem> items = skillItems != null ? skillItems : Collections.emptyList();
        if (items.size() > 30) {
            throw new BadRequestException("Số lượng kỹ năng tối đa là 30");
        }

        Set<String> seenInputIds = new HashSet<>();
        for (JobSkillItem item : items) {
            if (item.skillId() == null || item.skillId().isBlank()) {
                throw new BadRequestException("Mã kỹ năng không được để trống");
            }
            if (!seenInputIds.add(item.skillId().trim().toLowerCase())) {
                throw new BadRequestException("Kỹ năng bị trùng lặp: " + item.skillId());
            }
        }

        Set<String> seenResolvedIds = new HashSet<>();
        List<JobSkill> newSkills = new ArrayList<>();

        for (JobSkillItem item : items) {
            String trimmedId = item.skillId().trim();
            CatalogSkill catalogSkill = skillCatalogClient.findByIdOrCode(trimmedId)
                    .orElseThrow(() -> new BadRequestException("Kỹ năng không tồn tại: " + item.skillId()));

            if (!seenResolvedIds.add(catalogSkill.id().toLowerCase())) {
                throw new BadRequestException("Kỹ năng bị trùng lặp: " + item.skillId());
            }

            boolean required = Boolean.TRUE.equals(item.required());
            int minScore = item.minScore() != null ? item.minScore() : 0;

            newSkills.add(new JobSkill(
                    job.getId(),
                    catalogSkill.id(),
                    catalogSkill.code(),
                    catalogSkill.name(),
                    required,
                    minScore));
        }

        jobSkillRepository.deleteByJobId(job.getId());
        jobSkillRepository.flush();

        if (!newSkills.isEmpty()) {
            jobSkillRepository.saveAll(newSkills);
        }

        Job updatedJob = jobRepository.save(job);
        return buildJobResponse(updatedJob, principal);
    }

    public JobResponse submitJob(UUID id, UserPrincipal principal) {
        UUID companyId = getRequiredCompanyId(principal);
        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() != JobStatus.DRAFT && job.getStatus() != JobStatus.REJECTED) {
            throw new ConflictException("Tin không ở trạng thái có thể gửi duyệt");
        }

        List<JobSkill> skills = jobSkillRepository.findByJobId(job.getId());
        boolean hasRequiredSkill = skills.stream().anyMatch(JobSkill::isRequired);
        if (!hasRequiredSkill) {
            throw new BadRequestException("Cần ít nhất 1 kỹ năng bắt buộc để gửi duyệt");
        }

        LocalDate today = LocalDate.now(clock);
        if (job.getDeadline().isBefore(today)) {
            throw new BadRequestException("Hạn nộp hồ sơ phải từ hôm nay trở đi");
        }

        job.setStatus(JobStatus.PENDING);
        job.setRejectedReason(null);

        Job updated = jobRepository.save(job);
        return buildJobResponse(updated, principal);
    }

    public JobResponse closeJob(UUID id, UserPrincipal principal) {
        UUID companyId = getRequiredCompanyId(principal);
        Job job = jobRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Tin đã được đóng");
        }

        job.setStatus(JobStatus.CLOSED);
        Job updated = jobRepository.save(job);
        return buildJobResponse(updated, principal);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobListItemResponse> listMyJobs(UserPrincipal principal, String statusStr, int page, int size) {
        UUID companyId = getRequiredCompanyId(principal);

        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size <= 0 ? 10 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        JobStatus statusFilter = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                statusFilter = JobStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + statusStr);
            }
        }

        Page<Job> jobPage;
        if (statusFilter != null) {
            jobPage = jobRepository.findByCompanyIdAndStatus(companyId, statusFilter, pageable);
        } else {
            jobPage = jobRepository.findByCompanyId(companyId, pageable);
        }

        List<JobListItemResponse> items = jobPage.getContent().stream()
                .map(job -> {
                    int requiredSkillCount = (int) jobSkillRepository.findByJobId(job.getId()).stream()
                            .filter(JobSkill::isRequired)
                            .count();
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
                            job.getUpdatedAt());
                })
                .toList();

        PaginationMeta meta = new PaginationMeta(
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isFirst(),
                jobPage.isLast(),
                jobPage.hasNext(),
                jobPage.hasPrevious());

        return new PageResponse<>(
                items,
                meta,
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.getNumber(),
                jobPage.getSize());
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> listStaffJobs(String statusStr, int page, int size) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size <= 0 ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "updatedAt"));

        JobStatus statusFilter = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                statusFilter = JobStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + statusStr);
            }
        }

        Page<Job> jobPage;
        if (statusFilter != null) {
            jobPage = jobRepository.findByStatus(statusFilter, pageable);
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

        PaginationMeta meta = new PaginationMeta(
                jobPage.getNumber(),
                jobPage.getSize(),
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.isFirst(),
                jobPage.isLast(),
                jobPage.hasNext(),
                jobPage.hasPrevious());

        return new PageResponse<>(
                items,
                meta,
                jobPage.getTotalElements(),
                jobPage.getTotalPages(),
                jobPage.getNumber(),
                jobPage.getSize());
    }

    @Transactional
    public JobResponse approveJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
        if (job.getStatus() != JobStatus.PENDING) {
            throw new ConflictException("Chỉ có thể phê duyệt tin đang chờ duyệt");
        }
        job.setStatus(JobStatus.APPROVED);
        job.setRejectedReason(null);
        Job saved = jobRepository.save(job);
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
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
        if (job.getStatus() != JobStatus.PENDING) {
            throw new ConflictException("Chỉ có thể từ chối tin đang chờ duyệt");
        }
        job.setStatus(JobStatus.REJECTED);
        job.setRejectedReason(reason != null && !reason.isBlank() ? reason.trim() : "Không đạt yêu cầu");
        Job saved = jobRepository.save(job);
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
        }

        return buildJobResponse(job, principal);
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
                job.getUpdatedAt()
        );
    }
}
