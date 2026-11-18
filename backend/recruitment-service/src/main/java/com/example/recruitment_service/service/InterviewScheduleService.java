package com.example.recruitment_service.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.recruitment_service.dto.CandidateInterviewResponse;
import com.example.recruitment_service.dto.CompanyInterviewResponse;
import com.example.recruitment_service.dto.InterviewScheduleResponse;
import com.example.recruitment_service.dto.InterviewSlotInput;
import com.example.recruitment_service.dto.InterviewSlotResponse;
import com.example.recruitment_service.dto.RescheduleResponse;
import com.example.recruitment_service.entity.InterviewRound;
import com.example.recruitment_service.entity.InterviewSchedule;
import com.example.recruitment_service.entity.InterviewSlot;
import com.example.recruitment_service.entity.Recruitment;
import com.example.recruitment_service.enums.InterviewScheduleStatus;
import com.example.recruitment_service.enums.InterviewSlotStatus;
import com.example.recruitment_service.enums.RecruitmentStatus;
import com.example.recruitment_service.exception.ApiException;
import com.example.recruitment_service.integration.JobServiceClient;
import com.example.recruitment_service.integration.RikkeiMeetClient;
import com.example.recruitment_service.repository.InterviewRoundRepository;
import com.example.recruitment_service.repository.InterviewScheduleRepository;
import com.example.recruitment_service.repository.InterviewSlotRepository;
import com.example.recruitment_service.security.UserPrincipal;

@Service
public class InterviewScheduleService {
    private static final int MIN_SLOTS = 1;
    private static final int MAX_SLOTS = 5;
    private static final long MIN_DURATION_MINUTES = 30;

    private final InterviewRoundRepository roundRepository;
    private final InterviewSlotRepository slotRepository;
    private final InterviewScheduleRepository scheduleRepository;
    private final JobServiceClient jobServiceClient;
    private final RikkeiMeetClient rikkeiMeetClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public InterviewScheduleService(
            InterviewRoundRepository roundRepository,
            InterviewSlotRepository slotRepository,
            InterviewScheduleRepository scheduleRepository,
            JobServiceClient jobServiceClient,
            RikkeiMeetClient rikkeiMeetClient,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.roundRepository = roundRepository;
        this.slotRepository = slotRepository;
        this.scheduleRepository = scheduleRepository;
        this.jobServiceClient = jobServiceClient;
        this.rikkeiMeetClient = rikkeiMeetClient;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public List<InterviewSlotResponse> proposeSlots(
            UUID roundId,
            List<InterviewSlotInput> inputs,
            UserPrincipal actor,
            String authorization) {
        InterviewRound round = getOwnedRoundForHr(roundId, actor, authorization);
        ensureRecruitmentInInterview(round.getRecruitment());
        validateSlotInputs(inputs);

        if (scheduleRepository.findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
                roundId, InterviewScheduleStatus.SCHEDULED).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "This round already has a scheduled interview. Use reschedule to propose new slots.",
                    "ROUND_ALREADY_SCHEDULED");
        }

        ensureNoOverlapWithExistingOpenSlots(roundId, inputs);
        ensureNoScheduleConflictsForSlots(round, inputs);

        List<InterviewSlot> saved = saveOpenSlots(round, inputs);
        return saved.stream().map(InterviewSlotResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InterviewSlotResponse> listRoundSlots(
            UUID roundId,
            UserPrincipal actor,
            String authorization) {
        InterviewRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview round not found"));
        if ("HR".equals(actor.role())) {
            verifyHrOwnsRecruitment(round.getRecruitment(), actor, authorization);
        } else if (!"STAFF".equals(actor.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only HR or STAFF can view round slots");
        }
        return slotRepository.findByRound_IdOrderByStartAtAsc(roundId).stream()
                .map(InterviewSlotResponse::from)
                .toList();
    }

    @Transactional
    public RescheduleResponse reschedule(
            UUID roundId,
            List<InterviewSlotInput> newInputs,
            UserPrincipal actor,
            String authorization) {
        InterviewRound round = getOwnedRoundForHr(roundId, actor, authorization);
        ensureRecruitmentInInterview(round.getRecruitment());
        validateSlotInputs(newInputs);
        ensureNoScheduleConflictsForSlots(round, newInputs);

        List<InterviewSchedule> activeSchedules =
                scheduleRepository.findByRound_IdAndStatus(roundId, InterviewScheduleStatus.SCHEDULED);
        for (InterviewSchedule schedule : activeSchedules) {
            schedule.setStatus(InterviewScheduleStatus.CANCELLED);
        }
        if (!activeSchedules.isEmpty()) {
            scheduleRepository.saveAll(activeSchedules);
        }

        List<InterviewSlot> existingSlots = slotRepository.findByRound_IdOrderByStartAtAsc(roundId);
        int cancelledSlots = 0;
        for (InterviewSlot slot : existingSlots) {
            if (slot.getStatus() != InterviewSlotStatus.CANCELLED) {
                slot.setStatus(InterviewSlotStatus.CANCELLED);
                cancelledSlots++;
            }
        }
        if (!existingSlots.isEmpty()) {
            slotRepository.saveAll(existingSlots);
        }

        round.setScheduledAt(null);
        roundRepository.save(round);

        List<InterviewSlot> createdSlots = saveOpenSlots(round, newInputs);
        return new RescheduleResponse(
                roundId,
                activeSchedules.size(),
                cancelledSlots,
                createdSlots.stream().map(InterviewSlotResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public List<CandidateInterviewResponse> listCandidateInterviews(UserPrincipal candidate) {
        if (!"CANDIDATE".equals(candidate.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only candidates can view their interviews");
        }
        List<InterviewRound> rounds = roundRepository.findByCandidateUserId(candidate.id());
        if (rounds.isEmpty()) {
            return List.of();
        }
        List<UUID> roundIds = rounds.stream().map(InterviewRound::getId).toList();

        Map<UUID, List<InterviewSlotResponse>> slotsByRound = new LinkedHashMap<>();
        for (InterviewSlot slot : slotRepository.findByRound_IdInOrderByStartAtAsc(roundIds)) {
            slotsByRound.computeIfAbsent(slot.getRound().getId(), k -> new ArrayList<>())
                    .add(InterviewSlotResponse.from(slot));
        }

        Map<UUID, InterviewScheduleResponse> scheduleByRound = new LinkedHashMap<>();
        for (InterviewSchedule schedule : scheduleRepository.findAllWithDetailsByRoundIds(roundIds)) {
            UUID rId = schedule.getRound().getId();
            if (!scheduleByRound.containsKey(rId)
                    || (schedule.getStatus() == InterviewScheduleStatus.SCHEDULED
                    && scheduleByRound.get(rId).status() != InterviewScheduleStatus.SCHEDULED)) {
                scheduleByRound.put(rId, InterviewScheduleResponse.from(schedule));
            }
        }

        List<CandidateInterviewResponse> results = new ArrayList<>();
        for (InterviewRound round : rounds) {
            List<InterviewSlotResponse> roundSlots = slotsByRound.getOrDefault(round.getId(), List.of());
            InterviewScheduleResponse schedule = scheduleByRound.get(round.getId());
            if (roundSlots.isEmpty() && schedule == null) {
                continue;
            }
            Recruitment recruitment = round.getRecruitment();
            results.add(new CandidateInterviewResponse(
                    round.getId(),
                    recruitment.getId(),
                    recruitment.getJob_id(),
                    recruitment.getCompany_id(),
                    round.getRoundNo(),
                    round.getName(),
                    round.getInterviewerEmail(),
                    round.getResult(),
                    round.getScore(),
                    round.getNote(),
                    recruitment.getStatus(),
                    roundSlots,
                    schedule));
        }
        return results;
    }

    @Transactional
    public InterviewScheduleResponse pickSlot(
            UUID roundId,
            UUID slotId,
            UserPrincipal candidate,
            String authorization) {
        if (!"CANDIDATE".equals(candidate.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only candidates can pick an interview slot");
        }
        InterviewRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview round not found"));
        Recruitment recruitment = round.getRecruitment();
        if (!candidate.id().equals(recruitment.getUser_id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Interview round not found");
        }

        InterviewSlot chosenSlot = slotRepository.findByIdAndRound_Id(slotId, roundId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview slot not found"));

        if (chosenSlot.getStatus() != InterviewSlotStatus.OPEN) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Interview slot is no longer available (status: " + chosenSlot.getStatus() + ")",
                    "SLOT_NOT_OPEN");
        }
        if (scheduleRepository.findFirstByRound_IdAndStatusOrderByCreatedAtDesc(
                roundId, InterviewScheduleStatus.SCHEDULED).isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "An interview schedule is already confirmed for this round",
                    "ROUND_ALREADY_SCHEDULED");
        }

        ensureNoScheduleConflict(round, chosenSlot.getStartAt(), chosenSlot.getEndAt());

        String roomTitle = "Phỏng vấn Vòng " + round.getRoundNo() + " - " + round.getName();
        String meetUrl = rikkeiMeetClient.createRoom(roomTitle, chosenSlot.getStartAt(), chosenSlot.getEndAt());

        List<InterviewSlot> allSlots = slotRepository.findByRound_IdOrderByStartAtAsc(roundId);
        for (InterviewSlot slot : allSlots) {
            if (slot.getId().equals(chosenSlot.getId())) {
                slot.setStatus(InterviewSlotStatus.PICKED);
            } else if (slot.getStatus() == InterviewSlotStatus.OPEN) {
                slot.setStatus(InterviewSlotStatus.CANCELLED);
            }
        }
        slotRepository.saveAll(allSlots);

        round.setScheduledAt(LocalDateTime.ofInstant(chosenSlot.getStartAt(), ZoneOffset.UTC));
        roundRepository.save(round);

        InterviewSchedule schedule = new InterviewSchedule();
        schedule.setRound(round);
        schedule.setSlot(chosenSlot);
        schedule.setMeetUrl(meetUrl);
        schedule.setStatus(InterviewScheduleStatus.SCHEDULED);
        InterviewSchedule savedSchedule = scheduleRepository.save(schedule);

        List<String> hrRecipients = resolveHrRecipients(round, recruitment, authorization);
        String icsContent = IcsCalendarBuilder.buildInterviewInvite(
                savedSchedule.getId(),
                roomTitle,
                "Phỏng vấn trực tuyến qua Rikkei Meet: " + meetUrl,
                meetUrl,
                chosenSlot.getStartAt(),
                chosenSlot.getEndAt(),
                Instant.now(clock));

        eventPublisher.publishEvent(new InterviewScheduledEvent(
                savedSchedule.getId(),
                round.getId(),
                recruitment.getId(),
                round.getName(),
                recruitment.getCandidate_email(),
                hrRecipients,
                chosenSlot.getStartAt(),
                chosenSlot.getEndAt(),
                meetUrl,
                icsContent));

        return InterviewScheduleResponse.from(savedSchedule);
    }

    @Transactional(readOnly = true)
    public List<CompanyInterviewResponse> listCompanyInterviews(
            UserPrincipal actor,
            Instant from,
            Instant to) {
        if (!"HR".equals(actor.role()) || actor.companyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only HR with a valid company can view company interviews");
        }
        Instant effectiveFrom = from;
        Instant effectiveTo = to;
        if (effectiveFrom == null || effectiveTo == null) {
            LocalDateTime nowUtc = LocalDateTime.ofInstant(Instant.now(clock), ZoneOffset.UTC);
            LocalDateTime mondayStart = nowUtc.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate().atStartOfDay();
            effectiveFrom = effectiveFrom != null ? effectiveFrom : mondayStart.toInstant(ZoneOffset.UTC);
            effectiveTo = effectiveTo != null ? effectiveTo : mondayStart.plusDays(7).toInstant(ZoneOffset.UTC);
        }
        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "'to' must be after or equal to 'from'", "INVALID_DATE_RANGE");
        }

        return scheduleRepository.findCompanySchedulesInRange(actor.companyId(), effectiveFrom, effectiveTo)
                .stream()
                .map(CompanyInterviewResponse::from)
                .toList();
    }

    @Transactional
    public ReminderRunResult sendDueReminders() {
        Instant now = Instant.now(clock);
        Instant oneHourLater = now.plus(Duration.ofHours(1));
        Instant twentyFourHoursLater = now.plus(Duration.ofHours(24));

        int sent24h = 0;
        List<InterviewSchedule> due24h = scheduleRepository.findDueFor24hReminder(oneHourLater, twentyFourHoursLater);
        for (InterviewSchedule schedule : due24h) {
            schedule.setReminded24hAt(now);
            scheduleRepository.save(schedule);
            publishReminder(schedule, "24 giờ");
            sent24h++;
        }

        int sent1h = 0;
        List<InterviewSchedule> due1h = scheduleRepository.findDueFor1hReminder(now, oneHourLater);
        for (InterviewSchedule schedule : due1h) {
            schedule.setReminded1hAt(now);
            scheduleRepository.save(schedule);
            publishReminder(schedule, "1 giờ");
            sent1h++;
        }

        return new ReminderRunResult(sent24h, sent1h);
    }

    private void publishReminder(InterviewSchedule schedule, String milestone) {
        InterviewRound round = schedule.getRound();
        Recruitment recruitment = round.getRecruitment();
        InterviewSlot slot = schedule.getSlot();
        List<String> hrRecipients = StringUtils.hasText(round.getInterviewerEmail())
                ? List.of(round.getInterviewerEmail().trim())
                : List.of();
        eventPublisher.publishEvent(new InterviewReminderEvent(
                schedule.getId(),
                round.getId(),
                round.getName(),
                milestone,
                recruitment.getCandidate_email(),
                hrRecipients,
                slot.getStartAt(),
                slot.getEndAt(),
                schedule.getMeetUrl()));
    }

    private InterviewRound getOwnedRoundForHr(UUID roundId, UserPrincipal actor, String authorization) {
        if (!"HR".equals(actor.role()) || actor.companyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only HR with a valid company can manage interview slots");
        }
        InterviewRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Interview round not found"));
        verifyHrOwnsRecruitment(round.getRecruitment(), actor, authorization);
        return round;
    }

    private void verifyHrOwnsRecruitment(Recruitment recruitment, UserPrincipal actor, String authorization) {
        if (actor.companyId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the job's HR can manage this recruitment");
        }
        if (recruitment.getCompany_id() != null) {
            if (!actor.companyId().equals(recruitment.getCompany_id())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Interview round not found");
            }
            return;
        }
        var job = jobServiceClient.getJob(recruitment.getJob_id(), authorization);
        if (!actor.companyId().equals(job.companyId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Interview round not found");
        }
    }

    private void ensureRecruitmentInInterview(Recruitment recruitment) {
        if (recruitment.getStatus() != RecruitmentStatus.INTERVIEW) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Interview slots can only be proposed while recruitment is in INTERVIEW status",
                    "INVALID_STATUS");
        }
    }

    private void validateSlotInputs(List<InterviewSlotInput> inputs) {
        if (inputs == null || inputs.size() < MIN_SLOTS || inputs.size() > MAX_SLOTS) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Must propose between " + MIN_SLOTS + " and " + MAX_SLOTS + " interview slots",
                    "INVALID_SLOT_COUNT");
        }
        Instant now = Instant.now(clock);
        List<InterviewSlotInput> sorted = inputs.stream()
                .sorted(Comparator.comparing(
                        InterviewSlotInput::startAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            InterviewSlotInput slot = sorted.get(i);
            if (slot == null || slot.startAt() == null || slot.endAt() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Each slot must specify both startAt and endAt",
                        "INVALID_SLOT_TIME");
            }
            if (!slot.startAt().isAfter(now)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Interview slot startAt must be in the future",
                        "SLOT_IN_PAST");
            }
            if (!slot.endAt().isAfter(slot.startAt())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Interview slot endAt must be after startAt",
                        "INVALID_SLOT_RANGE");
            }
            long durationMinutes = Duration.between(slot.startAt(), slot.endAt()).toMinutes();
            if (durationMinutes < MIN_DURATION_MINUTES) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "Each interview slot must be at least 30 minutes long",
                        "SLOT_TOO_SHORT");
            }
            if (i > 0) {
                InterviewSlotInput prev = sorted.get(i - 1);
                if (prev.endAt().isAfter(slot.startAt())) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "Proposed interview slots must not overlap",
                            "OVERLAPPING_SLOTS");
                }
            }
        }
    }

    private List<InterviewSlot> saveOpenSlots(InterviewRound round, List<InterviewSlotInput> inputs) {
        List<InterviewSlot> entities = new ArrayList<>(inputs.size());
        for (InterviewSlotInput input : inputs) {
            InterviewSlot slot = new InterviewSlot();
            slot.setRound(round);
            slot.setStartAt(input.startAt());
            slot.setEndAt(input.endAt());
            slot.setStatus(InterviewSlotStatus.OPEN);
            entities.add(slot);
        }
        List<InterviewSlot> saved = slotRepository.saveAll(entities);
        saved.sort(Comparator.comparing(InterviewSlot::getStartAt));
        return saved;
    }

    private void ensureNoOverlapWithExistingOpenSlots(UUID roundId, List<InterviewSlotInput> inputs) {
        List<InterviewSlot> existingOpen = slotRepository.findByRound_IdAndStatus(roundId, InterviewSlotStatus.OPEN);
        if (existingOpen.isEmpty()) {
            return;
        }
        for (InterviewSlotInput candidateInput : inputs) {
            for (InterviewSlot existing : existingOpen) {
                if (existing.getStartAt().isBefore(candidateInput.endAt())
                        && existing.getEndAt().isAfter(candidateInput.startAt())) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "Khung giờ đề xuất bị trùng với khung giờ đang mở của vòng phỏng vấn này",
                            "SLOT_OVERLAP_EXISTING");
                }
            }
        }
    }

    private void ensureNoScheduleConflictsForSlots(InterviewRound round, List<InterviewSlotInput> inputs) {
        for (InterviewSlotInput input : inputs) {
            ensureNoScheduleConflict(round, input.startAt(), input.endAt());
        }
    }

    private void ensureNoScheduleConflict(InterviewRound round, Instant startAt, Instant endAt) {
        Recruitment recruitment = round.getRecruitment();
        List<InterviewSchedule> candidateConflicts = scheduleRepository.findConflictingCandidateSchedules(
                recruitment.getUser_id(), round.getId(), startAt, endAt);
        if (!candidateConflicts.isEmpty()) {
            InterviewSchedule conflict = candidateConflicts.get(0);
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Khung giờ bị trùng với lịch phỏng vấn khác của ứng viên ("
                            + conflict.getRound().getName() + ": "
                            + conflict.getSlot().getStartAt() + " - " + conflict.getSlot().getEndAt() + ")",
                    "CANDIDATE_SCHEDULE_CONFLICT");
        }

        if (StringUtils.hasText(round.getInterviewerEmail())) {
            List<InterviewSchedule> interviewerConflicts = scheduleRepository.findConflictingInterviewerSchedules(
                    round.getInterviewerEmail().trim(), round.getId(), startAt, endAt);
            if (!interviewerConflicts.isEmpty()) {
                InterviewSchedule conflict = interviewerConflicts.get(0);
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "Người phỏng vấn (" + round.getInterviewerEmail().trim()
                                + ") đã có lịch phỏng vấn trùng khung giờ ("
                                + conflict.getRound().getName() + ": "
                                + conflict.getSlot().getStartAt() + " - " + conflict.getSlot().getEndAt() + ")",
                        "INTERVIEWER_SCHEDULE_CONFLICT");
            }
        }
    }

    private List<String> resolveHrRecipients(InterviewRound round, Recruitment recruitment, String authorization) {
        Set<String> emails = new LinkedHashSet<>();
        if (StringUtils.hasText(round.getInterviewerEmail())) {
            emails.add(round.getInterviewerEmail().trim());
        }
        try {
            if (StringUtils.hasText(authorization)) {
                var job = jobServiceClient.getJob(recruitment.getJob_id(), authorization);
                if (job.hrEmails() != null) {
                    for (String email : job.hrEmails()) {
                        if (StringUtils.hasText(email)) {
                            emails.add(email.trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Job service HR email lookup is best-effort when interviewer email is already recorded
        }
        return new ArrayList<>(emails);
    }

    public record ReminderRunResult(int sent24hCount, int sent1hCount) {
    }
}
