package com.example.recruitment_service.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.recruitment_service.dto.CreateInterviewRoundRequest;
import com.example.recruitment_service.dto.InterviewRoundSummary;
import com.example.recruitment_service.dto.InterviewScheduleResponse;
import com.example.recruitment_service.dto.InterviewSlotResponse;
import com.example.recruitment_service.dto.RecruitmentReviewDetails;
import com.example.recruitment_service.dto.RecruitmentStaffSummary;
import com.example.recruitment_service.dto.RecruitmentDetails;
import com.example.recruitment_service.dto.RecruitmentHistoryItem;
import com.example.recruitment_service.dto.RecruitmentSummary;
import com.example.recruitment_service.dto.ApplyRequest;
import com.example.recruitment_service.dto.TransitionRequest;
import com.example.recruitment_service.dto.UpdateInterviewRoundRequest;
import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.InterviewSchedule;
import com.example.recruitment_service.entity.InterviewSlot;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.InterviewScheduleStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.AiCareerServiceClient;
import com.example.recruitment_service.integration.CvServiceClient;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.repository.InterviewRoundRepository;
import com.example.recruitment_service.repository.InterviewScheduleRepository;
import com.example.recruitment_service.repository.InterviewSlotRepository;
import com.example.recruitment_service.repository.RecruitmentRepository;
import com.example.recruitment_service.repository.RecruitmentStatusHistoryRepository;
import com.example.recruitment_service.security.UserPrincipal;
import com.example.recruitment_service.storage.CvSnapshotStorage;

@Service
public class RecruitmentService {
    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentStatusHistoryRepository historyRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final InterviewSlotRepository interviewSlotRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final RecruitmentTransitionPolicy transitionPolicy;
    private final JobServiceClient jobServiceClient;
    private final AiCareerServiceClient aiCareerServiceClient;
    private final CvServiceClient cvServiceClient;
    private final CvSnapshotStorage snapshotStorage;
    private final ApplicationEventPublisher eventPublisher;

    public RecruitmentService(
            RecruitmentRepository recruitmentRepository,
            RecruitmentStatusHistoryRepository historyRepository,
            InterviewRoundRepository interviewRoundRepository,
            RecruitmentTransitionPolicy transitionPolicy,
            JobServiceClient jobServiceClient,
            AiCareerServiceClient aiCareerServiceClient,
            CvServiceClient cvServiceClient,
            CvSnapshotStorage snapshotStorage,
            ApplicationEventPublisher eventPublisher) {
        this(
                recruitmentRepository,
                historyRepository,
                interviewRoundRepository,
                null,
                null,
                transitionPolicy,
                jobServiceClient,
                aiCareerServiceClient,
                cvServiceClient,
                snapshotStorage,
                eventPublisher);
    }

    @Autowired
    public RecruitmentService(
            RecruitmentRepository recruitmentRepository,
            RecruitmentStatusHistoryRepository historyRepository,
            InterviewRoundRepository interviewRoundRepository,
            InterviewSlotRepository interviewSlotRepository,
            InterviewScheduleRepository interviewScheduleRepository,
            RecruitmentTransitionPolicy transitionPolicy,
            JobServiceClient jobServiceClient,
            AiCareerServiceClient aiCareerServiceClient,
            CvServiceClient cvServiceClient,
            CvSnapshotStorage snapshotStorage,
            ApplicationEventPublisher eventPublisher) {
        this.recruitmentRepository = recruitmentRepository;
        this.historyRepository = historyRepository;
        this.interviewRoundRepository = interviewRoundRepository;
        this.interviewSlotRepository = interviewSlotRepository;
        this.interviewScheduleRepository = interviewScheduleRepository;
        this.transitionPolicy = transitionPolicy;
        this.jobServiceClient = jobServiceClient;
        this.aiCareerServiceClient = aiCareerServiceClient;
        this.cvServiceClient = cvServiceClient;
        this.snapshotStorage = snapshotStorage;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RecruitmentSummary recruit(
            UUID jobId,
            ApplyRequest request,
            UserPrincipal candidate,
            String authorization) {
        if (!StringUtils.hasText(candidate.email())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Candidate email is required");
        }
        var job = jobServiceClient.getJob(jobId, authorization);
        if (!"APPROVED".equalsIgnoreCase(job.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "The job must be approved before recruiting");
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
        recruitment.setCompany_id(job.companyId());
        recruitment.setUser_id(candidate.id());
        recruitment.setCandidate_email(candidate.email());
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

    @Transactional(readOnly = true)
    public RecruitmentReviewDetails getReviewDetails(
            UUID recruitmentId,
            UserPrincipal actor,
            String authorization) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found"));
        if ("HR".equals(actor.role())) {
            if (actor.companyId() == null) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Only the job's HR can view this recruitment");
            }
            var job = jobServiceClient.getJob(recruitment.getJob_id(), authorization);
            if (!actor.companyId().equals(job.companyId())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found");
            }
        } else if (!"STAFF".equals(actor.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only HR or STAFF can view this recruitment");
        }

        var matchScore = aiCareerServiceClient.getMatchScore(recruitment.getUser_id(), recruitment.getJob_id());
        List<InterviewRound> roundEntities = interviewRoundRepository
                .findByRecruitment_IdOrderByRoundNoAsc(recruitmentId);
        List<InterviewRoundSummary> rounds = mapRoundSummaries(roundEntities);
        List<RecruitmentHistoryItem> history = historyRepository.findHistory(recruitmentId)
                .stream().map(RecruitmentHistoryItem::from).toList();
        return new RecruitmentReviewDetails(
                RecruitmentSummary.from(recruitment),
                recruitment.getUser_id(),
                recruitment.getCandidate_email(),
                recruitment.getCover_letter(),
                recruitment.getCv_snapshot_url(),
                matchScore.score(),
                rounds,
                history);
    }

    @Transactional(readOnly = true)
    public Page<RecruitmentStaffSummary> listForStaff(
            UUID companyId,
            UUID jobId,
            RecruitmentStatus status,
            int page) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Page must be zero or greater", "INVALID_PAGE");
        }
        PageRequest pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "created_at"));
        return recruitmentRepository.findForStaff(companyId, jobId, status, pageable)
                .map(RecruitmentStaffSummary::from);
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
        if (!StringUtils.hasText(recruitment.getCandidate_email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Candidate email must be backfilled before withdrawing");
        }

        Recruitment saved = transitionStatus(
                recruitment, candidate.id(), RecruitmentStatus.WITHDRAWN, reason.trim());
        return RecruitmentSummary.from(saved);
    }

    @Transactional
    public RecruitmentSummary transition(
            UUID recruitmentId,
            TransitionRequest request,
            UserPrincipal actor,
            String authorization) {
        Recruitment recruitment = getHrOwnedRecruitment(recruitmentId, actor, authorization);
        if (!StringUtils.hasText(recruitment.getCandidate_email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Candidate email must be backfilled before transitioning");
        }
        RecruitmentStatus nextStatus = request.toStatus();
        if (!transitionPolicy.allows(recruitment.getStatus(), nextStatus)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "This recruitment status transition is not allowed",
                    "INVALID_TRANSITION");
        }

        RecruitmentStatus previousStatus = recruitment.getStatus();
        recruitment.setStatus(nextStatus);
        Recruitment saved = recruitmentRepository.save(recruitment);
        String note = request.note() == null ? null : request.note().trim();
        recordStatus(saved, actor.id(), previousStatus, note);
        eventPublisher.publishEvent(new RecruitmentStatusChangedEvent(
                saved.getId(), saved.getCandidate_email(), previousStatus, nextStatus));
        return RecruitmentSummary.from(saved);
    }

    @Transactional
    public InterviewRound createRound(
            UUID recruitmentId,
            CreateInterviewRoundRequest request,
            UserPrincipal actor,
            String authorization) {
        Recruitment recruitment = getHrOwnedRecruitment(recruitmentId, actor, authorization);
        requireInterviewStatus(recruitment);

        List<InterviewRound> rounds = interviewRoundRepository.findByRecruitment_IdOrderByRoundNoAsc(recruitmentId);
        int nextRoundNo = rounds.stream()
                .map(InterviewRound::getRoundNo)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
        InterviewRound round = new InterviewRound();
        round.setRecruitment(recruitment);
        round.setRoundNo(nextRoundNo);
        round.setResult(InterviewRoundResult.PENDING);
        round.setName(request.name());
        round.setInterviewerEmail(request.interviewerEmail());
        round.setScheduledAt(request.scheduledAt());
        return interviewRoundRepository.save(round);
    }

    @Transactional
    public InterviewRound updateRound(
            UUID recruitmentId,
            UUID roundId,
            UpdateInterviewRoundRequest request,
            UserPrincipal actor,
            String authorization) {
        Recruitment recruitment = getHrOwnedRecruitment(recruitmentId, actor, authorization);
        requireInterviewStatus(recruitment);

        if (request.result() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Interview result is required");
        }
        Integer score = request.score();
        if (score != null && (score < 0 || score > 100)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Interview score must be between 0 and 100");
        }

        InterviewRound round = interviewRoundRepository.findById(roundId)
                .filter(existing -> existing.getRecruitment() != null
                        && recruitmentId.equals(existing.getRecruitment().getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview round not found"));
        round.setResult(request.result());
        round.setScore(score);
        if (request.note() != null) {
            round.setNote(request.note());
        }
        if (request.scheduledAt() != null) {
            round.setScheduledAt(request.scheduledAt());
        }
        return interviewRoundRepository.save(round);
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

    private Recruitment getHrOwnedRecruitment(
            UUID recruitmentId,
            UserPrincipal actor,
            String authorization) {
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found"));
        if (!"HR".equals(actor.role()) || actor.companyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the job's HR can manage this recruitment");
        }
        var job = jobServiceClient.getJob(recruitment.getJob_id(), authorization);
        if (!actor.companyId().equals(job.companyId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Recruitment not found");
        }
        return recruitment;
    }

    private void requireInterviewStatus(Recruitment recruitment) {
        if (recruitment.getStatus() != RecruitmentStatus.INTERVIEW) {
            throw new ApiException(HttpStatus.CONFLICT, "Interview rounds can only be changed during INTERVIEW");
        }
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
        eventPublisher.publishEvent(new RecruitmentStatusChangedEvent(
                saved.getId(), saved.getCandidate_email(), previousStatus, nextStatus));
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

    private List<InterviewRoundSummary> mapRoundSummaries(List<InterviewRound> roundEntities) {
        if (roundEntities.isEmpty()) {
            return List.of();
        }
        if (interviewSlotRepository == null || interviewScheduleRepository == null) {
            return roundEntities.stream().map(InterviewRoundSummary::from).toList();
        }
        List<UUID> roundIds = roundEntities.stream().map(InterviewRound::getId).toList();
        Map<UUID, List<InterviewSlotResponse>> slotsByRound = new LinkedHashMap<>();
        for (InterviewSlot slot : interviewSlotRepository.findByRound_IdInOrderByStartAtAsc(roundIds)) {
            slotsByRound.computeIfAbsent(slot.getRound().getId(), k -> new ArrayList<>())
                    .add(InterviewSlotResponse.from(slot));
        }
        Map<UUID, InterviewScheduleResponse> scheduleByRound = new LinkedHashMap<>();
        for (InterviewSchedule schedule : interviewScheduleRepository.findAllWithDetailsByRoundIds(roundIds)) {
            UUID rId = schedule.getRound().getId();
            if (!scheduleByRound.containsKey(rId)
                    || (schedule.getStatus() == InterviewScheduleStatus.SCHEDULED
                    && scheduleByRound.get(rId).status() != InterviewScheduleStatus.SCHEDULED)) {
                scheduleByRound.put(rId, InterviewScheduleResponse.from(schedule));
            }
        }
        return roundEntities.stream()
                .map(r -> InterviewRoundSummary.from(
                        r,
                        slotsByRound.getOrDefault(r.getId(), List.of()),
                        scheduleByRound.get(r.getId())))
                .toList();
    }
}
