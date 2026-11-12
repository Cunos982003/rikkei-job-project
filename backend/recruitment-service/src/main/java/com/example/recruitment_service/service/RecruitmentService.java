package com.example.recruitment_service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.recruitment_service.dto.RecruitmentDetails;
import com.example.recruitment_service.dto.RecruitmentHistoryItem;
import com.example.recruitment_service.dto.RecruitmentSummary;
import com.example.recruitment_service.dto.ApplyRequest;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.CvServiceClient;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.repository.RecruitmentRepository;
import com.example.recruitment_service.repository.RecruitmentStatusHistoryRepository;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.storage.CvSnapshotStorage;

@Service
public class RecruitmentService {
    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentStatusHistoryRepository historyRepository;
    private final JobServiceClient jobServiceClient;
    private final CvServiceClient cvServiceClient;
    private final CvSnapshotStorage snapshotStorage;
    private final ApplicationEventPublisher eventPublisher;

    public RecruitmentService(
            RecruitmentRepository recruitmentRepository,
            RecruitmentStatusHistoryRepository historyRepository,
            JobServiceClient jobServiceClient,
            CvServiceClient cvServiceClient,
            CvSnapshotStorage snapshotStorage,
            ApplicationEventPublisher eventPublisher) {
        this.recruitmentRepository = recruitmentRepository;
        this.historyRepository = historyRepository;
        this.jobServiceClient = jobServiceClient;
        this.cvServiceClient = cvServiceClient;
        this.snapshotStorage = snapshotStorage;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RecruitmentSummary apply(
            UUID jobId,
            ApplyRequest request,
            UserPrincipal candidate,
            String authorization) {
        var job = jobServiceClient.getJob(jobId, authorization);
        if (!"APPROVED".equalsIgnoreCase(job.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "The job must be approved before applying");
        }
        if (job.expiresAt() != null && !job.expiresAt().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "The job recruitment deadline has passed");
        }
        if (recruitmentRepository.existsActiveByJobAndUser(
                jobId, candidate.id(), RecruitmentStatus.WITHDRAWN)) {
            throw new ApiException(HttpStatus.CONFLICT, "You have already applied for this job");
        }

        byte[] pdf = cvServiceClient.downloadPdf(request.cvId(), authorization);
        Recruitment recruitment = new Recruitment();
        recruitment.setJob_id(jobId);
        recruitment.setUser_id(candidate.id());
        recruitment.setCv_id(request.cvId());
        recruitment.setCover_letter(request.coverLetter());
        recruitment.setStatus(RecruitmentStatus.SUBMITTED);

        Recruitment saved = recruitmentRepository.save(recruitment);
        try {
            saved.setCv_snapshot_url(snapshotStorage.store(saved.getId(), pdf));
            Recruitment persisted = recruitmentRepository.save(saved);
            recordStatus(persisted, candidate.id(), RecruitmentStatus.SUBMITTED, "Recruitment submitted");
            eventPublisher.publishEvent(new RecruitmentSubmittedEvent(
                    persisted.getId(), jobId, job.hrEmails()));
            return RecruitmentSummary.from(persisted);
        } catch (RuntimeException exception) {
            snapshotStorage.delete(saved.getId());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<RecruitmentSummary> list(
            UserPrincipal candidate,
            RecruitmentStatus status) {
        List<Recruitment> recruitments = status == null
                ? recruitmentRepository.findForUser(candidate.id())
                : recruitmentRepository.findForUserAndStatus(candidate.id(), status);
        return recruitments.stream().map(RecruitmentSummary::from).toList();
    }

    @Transactional(readOnly = true)
    public RecruitmentDetails get(
            UUID recruitmentId,
            UserPrincipal candidate) {
        Recruitment recruitment = getOwnedRecruitment(recruitmentId, candidate.id());
        List<RecruitmentHistoryItem> history = historyRepository.findHistory(recruitmentId)
                .stream().map(RecruitmentHistoryItem::from).toList();
        return new RecruitmentDetails(RecruitmentSummary.from(recruitment), history);
    }

    @Transactional
    public RecruitmentSummary withdraw(
            UUID recruitmentId,
            String reason,
            UserPrincipal candidate) {
        Recruitment recruitment = getOwnedRecruitment(recruitmentId, candidate.id());
        if (recruitment.getStatus() == RecruitmentStatus.WITHDRAWN
                || recruitment.getStatus() == RecruitmentStatus.REJECTED
                || recruitment.getStatus() == RecruitmentStatus.HIRED
                || recruitment.getStatus() == RecruitmentStatus.OFFER) {
            throw new ApiException(HttpStatus.CONFLICT, "This recruitment can no longer be withdrawn");
        }

        Recruitment saved = transitionStatus(
                recruitment, candidate.id(), RecruitmentStatus.WITHDRAWN, reason.trim());
        return RecruitmentSummary.from(saved);
    }

    @Transactional(readOnly = true)
    public byte[] getCvSnapshot(
            UUID recruitmentId,
            UserPrincipal principal,
            String authorization) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found"));
        if (principal.role().equals("CANDIDATE")) {
            if (!recruitment.getUser_id().equals(principal.id())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found");
            }
        } else if (principal.role().equals("HR")) {
            var job = jobServiceClient.getJob(recruitment.getJob_id(), authorization);
            if (principal.companyId() == null || !principal.companyId().equals(job.companyId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found");
            }
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the applicant or the job's HR can view this CV");
        }
        return snapshotStorage.read(recruitmentId);
    }

    private Recruitment getOwnedRecruitment(UUID recruitmentId, UUID candidateId) {
        return recruitmentRepository.findForUserById(recruitmentId, candidateId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found"));
    }

    private Recruitment transitionStatus(
            Recruitment recruitment,
            UUID actorId,
            RecruitmentStatus nextStatus,
            String note) {
        RecruitmentStatus previousStatus = recruitment.getStatus();
        recruitment.setStatus(nextStatus);
        Recruitment saved = recruitmentRepository.save(recruitment);
        recordStatus(saved, actorId, previousStatus, note);
        return saved;
    }

    private void recordStatus(
            Recruitment recruitment,
            UUID actorId,
            RecruitmentStatus fromStatus,
            String note) {
        RecruitmentStatusHistory history = new RecruitmentStatusHistory();
        history.setRecruitment_id(recruitment.getId());
        history.setFrom_status(fromStatus);
        history.setTo_status(recruitment.getStatus());
        history.setActor_id(actorId);
        history.setNote(note);
        historyRepository.save(history);
    }
}
