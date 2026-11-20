package com.example.jobservice.job;

import com.example.jobservice.auth.client.AuthClient;
import com.example.jobservice.common.exception.BadRequestException;
import com.example.jobservice.common.exception.ConflictException;
import com.example.jobservice.common.exception.ResourceNotFoundException;
import com.example.jobservice.company.domain.Company;
import com.example.jobservice.company.domain.CompanyRepository;
import com.example.jobservice.job.catalog.CatalogSkill;
import com.example.jobservice.job.catalog.SkillCatalogClient;
import com.example.jobservice.job.domain.*;
import com.example.jobservice.job.dto.*;
import com.example.jobservice.job.service.JobService;
import com.example.jobservice.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

        @Mock
        private JobRepository jobRepository;

        @Mock
        private JobSkillRepository jobSkillRepository;

        @Mock
        private CompanyRepository companyRepository;

        @Mock
        private SkillCatalogClient skillCatalogClient;

        @Mock
        private AuthClient authClient;

        private Clock fixedClock;
        private JobService jobService;

        private final UUID companyId = UUID.randomUUID();
        private final UUID otherCompanyId = UUID.randomUUID();
        private UserPrincipal hrPrincipal;

        @BeforeEach
        void setUp() {
                fixedClock = Clock.fixed(Instant.parse("2026-11-20T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
                jobService = new JobService(
                                jobRepository,
                                jobSkillRepository,
                                companyRepository,
                                skillCatalogClient,
                                authClient,
                                fixedClock);

                hrPrincipal = UserPrincipal.of(
                                UUID.randomUUID(),
                                "hr@example.com",
                                "HR Manager",
                                "HR",
                                companyId);
        }

        private Job createMockJob(JobStatus status) {
                Job job = new Job();
                job.setId(UUID.randomUUID());
                job.setCompanyId(companyId);
                job.setTitle("Software Engineer");
                job.setPosition("Backend Dev");
                job.setLevel(JobLevel.JUNIOR);
                job.setType(JobType.FULLTIME);
                job.setQuantity(2);
                job.setWorkMode(WorkMode.ONSITE);
                job.setDeadline(LocalDate.now(fixedClock).plusDays(10));
                job.setStatus(status);
                return job;
        }

        @Test
        @DisplayName("Sửa JD APPROVED -> chuyển sang PENDING và xoá rejectedReason")
        void updateJob_approvedToPending() {
                Job job = createMockJob(JobStatus.APPROVED);
                job.setRejectedReason("Lý do cũ");

                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));
                when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(companyRepository.findById(companyId)).thenReturn(Optional.of(new Company()));

                UpdateJobRequest request = new UpdateJobRequest(
                                "Software Engineer Updated",
                                "Backend Dev",
                                JobLevel.MIDDLE,
                                JobType.FULLTIME,
                                3,
                                "Hà Nội",
                                "Duy Tân",
                                WorkMode.HYBRID,
                                BigDecimal.valueOf(15000000),
                                BigDecimal.valueOf(25000000),
                                "Mô tả",
                                "Yêu cầu",
                                "Quyền lợi",
                                LocalDate.now(fixedClock).plusDays(15));

                JobResponse response = jobService.updateJob(job.getId(), hrPrincipal, request);

                assertThat(response.status()).isEqualTo(JobStatus.PENDING);
                assertThat(response.rejectedReason()).isNull();
        }

        @Test
        @DisplayName("Sửa JD PENDING -> ném ConflictException 409")
        void updateJob_pendingThrowsConflict() {
                Job job = createMockJob(JobStatus.PENDING);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));

                UpdateJobRequest request = new UpdateJobRequest(
                                "Title", "Pos", JobLevel.JUNIOR, JobType.FULLTIME, 1,
                                "HN", "Address", WorkMode.ONSITE, null, null, null, null, null,
                                LocalDate.now(fixedClock).plusDays(5));

                assertThatThrownBy(() -> jobService.updateJob(job.getId(), hrPrincipal, request))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("Tin đang chờ duyệt, không thể chỉnh sửa");
        }

        @Test
        @DisplayName("Sửa JD CLOSED -> ném ConflictException 409")
        void updateJob_closedThrowsConflict() {
                Job job = createMockJob(JobStatus.CLOSED);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));

                UpdateJobRequest request = new UpdateJobRequest(
                                "Title", "Pos", JobLevel.JUNIOR, JobType.FULLTIME, 1,
                                "HN", "Address", WorkMode.ONSITE, null, null, null, null, null,
                                LocalDate.now(fixedClock).plusDays(5));

                assertThatThrownBy(() -> jobService.updateJob(job.getId(), hrPrincipal, request))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("Tin đã đóng, không thể chỉnh sửa");
        }

        @Test
        @DisplayName("Submit JD từ DRAFT sang PENDING khi có kỹ năng bắt buộc")
        void submitJob_draftToPending_success() {
                Job job = createMockJob(JobStatus.DRAFT);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));
                when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(companyRepository.findById(companyId)).thenReturn(Optional.of(new Company()));

                List<JobSkill> skills = List.of(
                                new JobSkill(job.getId(), "skill-1", "JAVA_CORE", "Java Core", true, 70));
                when(jobSkillRepository.findByJobId(job.getId())).thenReturn(skills);

                JobResponse response = jobService.submitJob(job.getId(), hrPrincipal);

                assertThat(response.status()).isEqualTo(JobStatus.PENDING);
                assertThat(response.requiredSkills()).contains("Java Core");
        }

        @Test
        @DisplayName("Submit JD khi không có kỹ năng bắt buộc -> ném BadRequestException 400")
        void submitJob_withoutRequiredSkill_throwsBadRequest() {
                Job job = createMockJob(JobStatus.DRAFT);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));
                when(jobSkillRepository.findByJobId(job.getId())).thenReturn(List.of(
                                new JobSkill(job.getId(), "skill-2", "GIT", "Git", false, 50)));

                assertThatThrownBy(() -> jobService.submitJob(job.getId(), hrPrincipal))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Cần ít nhất 1 kỹ năng bắt buộc để gửi duyệt");
        }

        @Test
        @DisplayName("Submit JD khi đang PENDING hoặc APPROVED -> ném ConflictException 409")
        void submitJob_invalidStatus_throwsConflict() {
                Job job = createMockJob(JobStatus.PENDING);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));

                assertThatThrownBy(() -> jobService.submitJob(job.getId(), hrPrincipal))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("Tin không ở trạng thái có thể gửi duyệt");
        }

        @Test
        @DisplayName("Đóng JD thành công từ bất kỳ trạng thái nào chưa đóng")
        void closeJob_success() {
                Job job = createMockJob(JobStatus.APPROVED);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));
                when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(companyRepository.findById(companyId)).thenReturn(Optional.of(new Company()));

                JobResponse response = jobService.closeJob(job.getId(), hrPrincipal);
                assertThat(response.status()).isEqualTo(JobStatus.CLOSED);
        }

        @Test
        @DisplayName("Đóng JD đã CLOSED -> ném ConflictException 409")
        void closeJob_alreadyClosed_throwsConflict() {
                Job job = createMockJob(JobStatus.CLOSED);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));

                assertThatThrownBy(() -> jobService.closeJob(job.getId(), hrPrincipal))
                                .isInstanceOf(ConflictException.class)
                                .hasMessageContaining("Tin đã được đóng");
        }

        @Test
        @DisplayName("HR thao tác trên JD công ty khác -> ném ResourceNotFoundException 404")
        void hr_jobOfOtherCompany_throwsNotFound() {
                UUID jobId = UUID.randomUUID();
                when(jobRepository.findByIdAndCompanyId(jobId, companyId)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> jobService.submitJob(jobId, hrPrincipal))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Không tìm thấy tin tuyển dụng");
        }

        @Test
        @DisplayName("Tạo JD với deadline quá khứ -> ném BadRequestException 400")
        void createJob_pastDeadline_throwsBadRequest() {
                CreateJobRequest request = new CreateJobRequest(
                                "Title", "Pos", JobLevel.JUNIOR, JobType.FULLTIME, 1,
                                "HN", "Address", WorkMode.ONSITE, null, null, null, null, null,
                                LocalDate.now(fixedClock).minusDays(1));

                assertThatThrownBy(() -> jobService.createJob(hrPrincipal, request))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Hạn nộp hồ sơ phải từ hôm nay trở đi");
        }

        @Test
        @DisplayName("Cập nhật kỹ năng: kỹ năng không tồn tại trong catalog -> ném BadRequestException 400")
        void replaceSkills_unknownSkill_throwsBadRequest() {
                Job job = createMockJob(JobStatus.DRAFT);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));
                when(skillCatalogClient.findByIdOrCode("UNKNOWN_SKILL")).thenReturn(Optional.empty());

                List<JobSkillItem> items = List.of(new JobSkillItem("UNKNOWN_SKILL", true, 70));

                assertThatThrownBy(() -> jobService.replaceSkills(job.getId(), hrPrincipal, items))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Kỹ năng không tồn tại");
        }

        @Test
        @DisplayName("Cập nhật kỹ năng: trùng lặp mã kỹ năng -> ném BadRequestException 400")
        void replaceSkills_duplicateSkill_throwsBadRequest() {
                Job job = createMockJob(JobStatus.DRAFT);
                when(jobRepository.findByIdAndCompanyId(job.getId(), companyId)).thenReturn(Optional.of(job));

                List<JobSkillItem> items = List.of(
                                new JobSkillItem("JAVA_CORE", true, 70),
                                new JobSkillItem("JAVA_CORE", false, 50));

                assertThatThrownBy(() -> jobService.replaceSkills(job.getId(), hrPrincipal, items))
                                .isInstanceOf(BadRequestException.class)
                                .hasMessageContaining("Kỹ năng bị trùng lặp");
        }
}
