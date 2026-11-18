package com.example.recruitment_service.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.recruitment_service.dto.DeclineOfferRequest;
import com.example.recruitment_service.dto.EmploymentRecordResponse;
import com.example.recruitment_service.dto.OfferResponse;
import com.example.recruitment_service.dto.SendOfferRequest;
import com.example.recruitment_service.entity.EmploymentRecord;
import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.Offer;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.entity.RecruitmentStatusHistory;
import com.example.recruitment_service.enums.InterviewRoundResult;
import com.example.recruitment_service.enums.OfferStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.repository.EmploymentRecordRepository;
import com.example.recruitment_service.repository.InterviewRoundRepository;
import com.example.recruitment_service.repository.OfferRepository;
import com.example.recruitment_service.repository.RecruitmentRepository;
import com.example.recruitment_service.repository.RecruitmentStatusHistoryRepository;
import com.example.recruitment_service.security.UserPrincipal;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final EmploymentRecordRepository employmentRecordRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final RecruitmentStatusHistoryRepository historyRepository;
    private final JobServiceClient jobServiceClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public OfferService(
            OfferRepository offerRepository,
            EmploymentRecordRepository employmentRecordRepository,
            RecruitmentRepository recruitmentRepository,
            InterviewRoundRepository interviewRoundRepository,
            RecruitmentStatusHistoryRepository historyRepository,
            JobServiceClient jobServiceClient,
            ApplicationEventPublisher eventPublisher) {
        this(offerRepository, employmentRecordRepository, recruitmentRepository,
                interviewRoundRepository, historyRepository, jobServiceClient,
                eventPublisher, Clock.systemUTC());
    }

    public OfferService(
            OfferRepository offerRepository,
            EmploymentRecordRepository employmentRecordRepository,
            RecruitmentRepository recruitmentRepository,
            InterviewRoundRepository interviewRoundRepository,
            RecruitmentStatusHistoryRepository historyRepository,
            JobServiceClient jobServiceClient,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.offerRepository = offerRepository;
        this.employmentRecordRepository = employmentRecordRepository;
        this.recruitmentRepository = recruitmentRepository;
        this.interviewRoundRepository = interviewRoundRepository;
        this.historyRepository = historyRepository;
        this.jobServiceClient = jobServiceClient;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public OfferResponse sendOffer(
            UUID applicationId,
            SendOfferRequest request,
            UserPrincipal actor,
            String authorization) {
        Recruitment recruitment = recruitmentRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        var job = verifyHrOwnership(recruitment, actor, authorization);

        if (recruitment.getStatus() != RecruitmentStatus.INTERVIEW) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể gửi offer khi đơn đang ở trạng thái INTERVIEW",
                    "INVALID_APPLICATION_STATUS");
        }

        List<InterviewRound> rounds = interviewRoundRepository.findByRecruitment_IdOrderByRoundNoAsc(applicationId);
        if (rounds.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Ứng viên chưa có vòng phỏng vấn nào đạt (PASS) để gửi offer",
                    "NO_PASSED_INTERVIEW_ROUND");
        }
        boolean hasPendingRound = rounds.stream()
                .anyMatch(round -> round.getResult() == null || round.getResult() == InterviewRoundResult.PENDING);
        InterviewRound lastRound = rounds.get(rounds.size() - 1);
        if (hasPendingRound || lastRound.getResult() != InterviewRoundResult.PASS) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Tất cả vòng phỏng vấn phải có kết quả và vòng cuối cùng phải PASS trước khi gửi offer",
                    "FINAL_ROUND_NOT_PASSED");
        }

        if (offerRepository.existsByRecruitment_Id(applicationId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Đơn ứng tuyển này đã có offer trước đó",
                    "OFFER_ALREADY_EXISTS");
        }

        Instant now = clock.instant();
        if (!request.responseDeadline().isAfter(now)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Hạn trả lời offer phải ở trong tương lai",
                    "INVALID_RESPONSE_DEADLINE");
        }

        String resolvedCompanyName = StringUtils.hasText(request.companyName())
                ? request.companyName().trim()
                : (StringUtils.hasText(job.companyName()) ? job.companyName().trim() : "Doanh nghiệp tuyển dụng");

        if (recruitment.getCompany_id() == null) {
            recruitment.setCompany_id(job.companyId());
        }

        RecruitmentStatus previousStatus = recruitment.getStatus();
        recruitment.setStatus(RecruitmentStatus.OFFER);
        Recruitment savedRecruitment = recruitmentRepository.save(recruitment);
        recordStatus(savedRecruitment, actor.id(), previousStatus, "HR gửi offer cho vị trí: " + request.position().trim());

        Offer offer = new Offer();
        offer.setRecruitment(savedRecruitment);
        offer.setCompanyName(resolvedCompanyName);
        offer.setPosition(request.position().trim());
        offer.setSalary(request.salary());
        offer.setStartDate(request.startDate());
        offer.setWorkLocation(request.workLocation().trim());
        offer.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        offer.setResponseDeadline(request.responseDeadline());
        offer.setStatus(OfferStatus.SENT);

        Offer savedOffer = offerRepository.save(offer);

        eventPublisher.publishEvent(new OfferSentEvent(
                savedOffer.getId(),
                savedRecruitment.getId(),
                savedRecruitment.getCandidate_email(),
                savedOffer.getCompanyName(),
                savedOffer.getPosition(),
                savedOffer.getSalary(),
                savedOffer.getStartDate(),
                savedOffer.getWorkLocation(),
                savedOffer.getResponseDeadline()));

        return OfferResponse.from(savedOffer);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listCandidateOffers(UserPrincipal candidate) {
        return offerRepository.findForCandidate(candidate.id()).stream()
                .map(offer -> {
                    Instant confirmedAt = employmentRecordRepository
                            .findByApplicationId(offer.getRecruitment().getId())
                            .map(EmploymentRecord::getConfirmedAt)
                            .orElse(null);
                    return OfferResponse.from(offer, confirmedAt);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferResponse getCandidateOffer(UUID offerId, UserPrincipal candidate) {
        Offer offer = offerRepository.findByIdAndCandidateId(offerId, candidate.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));
        Instant confirmedAt = employmentRecordRepository
                .findByApplicationId(offer.getRecruitment().getId())
                .map(EmploymentRecord::getConfirmedAt)
                .orElse(null);
        return OfferResponse.from(offer, confirmedAt);
    }

    @Transactional
    public OfferResponse acceptOffer(UUID offerId, UserPrincipal candidate) {
        Offer offer = offerRepository.findByIdAndCandidateId(offerId, candidate.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        Instant now = clock.instant();
        if (offer.getStatus() == OfferStatus.EXPIRED || !offer.getResponseDeadline().isAfter(now)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Offer đã quá hạn phản hồi và không thể chấp nhận",
                    "OFFER_EXPIRED");
        }
        if (offer.getStatus() != OfferStatus.SENT) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Offer không còn ở trạng thái chờ phản hồi (SENT)",
                    "OFFER_ALREADY_RESPONDED");
        }

        Recruitment recruitment = offer.getRecruitment();
        if (recruitment.getStatus() != RecruitmentStatus.OFFER) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Đơn ứng tuyển không ở trạng thái OFFER",
                    "INVALID_APPLICATION_STATUS");
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(now);
        Offer savedOffer = offerRepository.save(offer);

        RecruitmentStatus previousStatus = recruitment.getStatus();
        recruitment.setStatus(RecruitmentStatus.HIRED);
        Recruitment savedRecruitment = recruitmentRepository.save(recruitment);
        recordStatus(savedRecruitment, candidate.id(), previousStatus, "Ứng viên chấp nhận offer");

        if (!employmentRecordRepository.existsByApplicationId(savedRecruitment.getId())) {
            EmploymentRecord record = new EmploymentRecord();
            record.setUserId(candidate.id());
            record.setCompanyId(savedRecruitment.getCompany_id() != null
                    ? savedRecruitment.getCompany_id()
                    : new UUID(0L, 0L));
            record.setCompanyName(savedOffer.getCompanyName());
            record.setJobId(savedRecruitment.getJob_id());
            record.setApplicationId(savedRecruitment.getId());
            record.setPosition(savedOffer.getPosition());
            record.setStartDate(savedOffer.getStartDate());
            record.setConfirmedAt(null);
            employmentRecordRepository.save(record);
        }

        eventPublisher.publishEvent(new RecruitmentStatusChangedEvent(
                savedRecruitment.getId(),
                savedRecruitment.getCandidate_email(),
                previousStatus,
                RecruitmentStatus.HIRED));

        return OfferResponse.from(savedOffer);
    }

    @Transactional
    public OfferResponse declineOffer(UUID offerId, DeclineOfferRequest request, UserPrincipal candidate) {
        if (request == null || !StringUtils.hasText(request.reason())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lý do từ chối offer là bắt buộc", "REASON_REQUIRED");
        }
        Offer offer = offerRepository.findByIdAndCandidateId(offerId, candidate.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        Instant now = clock.instant();
        if (offer.getStatus() == OfferStatus.EXPIRED || !offer.getResponseDeadline().isAfter(now)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Offer đã quá hạn phản hồi",
                    "OFFER_EXPIRED");
        }
        if (offer.getStatus() != OfferStatus.SENT) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Offer không còn ở trạng thái chờ phản hồi (SENT)",
                    "OFFER_ALREADY_RESPONDED");
        }

        String trimmedReason = request.reason().trim();
        offer.setStatus(OfferStatus.DECLINED);
        offer.setRespondedAt(now);
        offer.setDeclineReason(trimmedReason);
        Offer savedOffer = offerRepository.save(offer);

        Recruitment recruitment = offer.getRecruitment();
        RecruitmentStatus previousStatus = recruitment.getStatus();
        recruitment.setStatus(RecruitmentStatus.REJECTED);
        Recruitment savedRecruitment = recruitmentRepository.save(recruitment);
        recordStatus(savedRecruitment, candidate.id(), previousStatus, "Ứng viên từ chối offer: " + trimmedReason);

        eventPublisher.publishEvent(new RecruitmentStatusChangedEvent(
                savedRecruitment.getId(),
                savedRecruitment.getCandidate_email(),
                previousStatus,
                RecruitmentStatus.REJECTED));

        return OfferResponse.from(savedOffer);
    }

    @Transactional
    public EmploymentRecordResponse confirmOnboard(
            UUID applicationId,
            UserPrincipal actor,
            String authorization) {
        Recruitment recruitment = recruitmentRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));

        verifyHrOwnership(recruitment, actor, authorization);

        Offer offer = offerRepository.findByRecruitment_Id(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found for this application"));

        if (offer.getStatus() != OfferStatus.ACCEPTED || recruitment.getStatus() != RecruitmentStatus.HIRED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể xác nhận đi làm khi ứng viên đã chấp nhận offer (HIRED)",
                    "OFFER_NOT_ACCEPTED");
        }

        EmploymentRecord record = employmentRecordRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Employment record not found"));

        if (record.getConfirmedAt() != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Đã xác nhận ứng viên đi làm trước đó",
                    "ALREADY_CONFIRMED");
        }

        record.setConfirmedAt(clock.instant());
        EmploymentRecord saved = employmentRecordRepository.save(record);
        return EmploymentRecordResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EmploymentRecordResponse> listMyEmploymentRecords(UserPrincipal candidate) {
        return employmentRecordRepository.findByUserIdOrderByStartDateDescCreatedAtDesc(candidate.id())
                .stream()
                .map(EmploymentRecordResponse::from)
                .toList();
    }

    @Transactional
    public int expireOverdueOffers() {
        Instant now = clock.instant();
        List<Offer> overdueOffers = offerRepository.findOverdueOffers(OfferStatus.SENT, now);
        for (Offer offer : overdueOffers) {
            offer.setStatus(OfferStatus.EXPIRED);
            offerRepository.save(offer);

            Recruitment recruitment = offer.getRecruitment();
            if (recruitment != null && recruitment.getStatus() == RecruitmentStatus.OFFER) {
                RecruitmentStatus previous = recruitment.getStatus();
                recruitment.setStatus(RecruitmentStatus.REJECTED);
                Recruitment saved = recruitmentRepository.save(recruitment);
                recordStatus(saved, saved.getUser_id(), previous, "Offer hết hạn phản hồi (EXPIRED)");
                eventPublisher.publishEvent(new RecruitmentStatusChangedEvent(
                        saved.getId(),
                        saved.getCandidate_email(),
                        previous,
                        RecruitmentStatus.REJECTED));
            }
        }
        return overdueOffers.size();
    }

    private JobServiceClient.JobDetails verifyHrOwnership(
            Recruitment recruitment,
            UserPrincipal actor,
            String authorization) {
        if (!"HR".equals(actor.role()) || actor.companyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ HR của doanh nghiệp sở hữu tin mới có quyền thực hiện");
        }
        var job = jobServiceClient.getJob(recruitment.getJob_id(), authorization);
        if (!actor.companyId().equals(job.companyId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Application not found");
        }
        return job;
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
