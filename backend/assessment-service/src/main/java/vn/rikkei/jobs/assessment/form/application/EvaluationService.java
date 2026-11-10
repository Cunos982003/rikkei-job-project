package vn.rikkei.jobs.assessment.form.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.BatchOptionResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.CriterionScoreRequest;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.CriterionScoreResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.EvaluationDetailResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.EvaluationTaskListResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.EvaluationTaskResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.UpdateEvaluationRequest;
import vn.rikkei.jobs.assessment.form.domain.Evaluation;
import vn.rikkei.jobs.assessment.form.domain.EvaluationAccessDeniedException;
import vn.rikkei.jobs.assessment.form.domain.EvaluationContext;
import vn.rikkei.jobs.assessment.form.domain.EvaluationCriteria;
import vn.rikkei.jobs.assessment.form.domain.EvaluationItemScore;
import vn.rikkei.jobs.assessment.form.domain.EvaluationNotFoundException;
import vn.rikkei.jobs.assessment.form.domain.EvaluationStatus;
import vn.rikkei.jobs.assessment.form.domain.Internship;
import vn.rikkei.jobs.assessment.form.domain.InternshipBatch;
import vn.rikkei.jobs.assessment.form.domain.InterviewRound;
import vn.rikkei.jobs.assessment.form.domain.TaskStatus;
import vn.rikkei.jobs.assessment.form.infrastructure.EvaluationCriteriaRepository;
import vn.rikkei.jobs.assessment.form.infrastructure.EvaluationRepository;
import vn.rikkei.jobs.assessment.form.infrastructure.InternshipBatchRepository;
import vn.rikkei.jobs.assessment.form.infrastructure.InternshipRepository;
import vn.rikkei.jobs.assessment.form.infrastructure.InterviewRoundRepository;
import vn.rikkei.jobs.assessment.security.MentorPrincipal;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCriteriaRepository criteriaRepository;
    private final InternshipRepository internshipRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final InternshipBatchRepository batchRepository;

    public EvaluationService(
            EvaluationRepository evaluationRepository,
            EvaluationCriteriaRepository criteriaRepository,
            InternshipRepository internshipRepository,
            InterviewRoundRepository interviewRoundRepository,
            InternshipBatchRepository batchRepository) {
        this.evaluationRepository = evaluationRepository;
        this.criteriaRepository = criteriaRepository;
        this.internshipRepository = internshipRepository;
        this.interviewRoundRepository = interviewRoundRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional(readOnly = true)
    public List<BatchOptionResponse> listBatches() {
        return batchRepository.findAll().stream()
                .map(batch -> new BatchOptionResponse(batch.getId(), batch.getCode(), batch.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationTaskListResponse listTasks(
            MentorPrincipal mentor, EvaluationContext context, UUID batchId) {
        Map<UUID, String> batchNames = batchRepository.findAll().stream()
                .collect(Collectors.toMap(InternshipBatch::getId, InternshipBatch::getName));

        List<EvaluationTaskResponse> items =
                switch (context) {
                    case INTERNSHIP -> listInternshipTasks(mentor, batchId, batchNames);
                    case INTERVIEW -> listInterviewTasks(mentor, batchId, batchNames);
                };

        long remaining = items.stream()
                .filter(task -> task.taskStatus() != TaskStatus.SUBMITTED)
                .count();
        return new EvaluationTaskListResponse(items, remaining, items.size());
    }

    @Transactional
    public EvaluationDetailResponse openOrCreate(MentorPrincipal mentor, EvaluationContext context, UUID subjectId) {
        Evaluation existing = evaluationRepository
                .findByContextAndSubjectIdAndMentorId(context, subjectId, mentor.userId())
                .orElse(null);
        if (existing != null) {
            return toDetail(existing);
        }

        List<EvaluationCriteria> criteria =
                criteriaRepository.findByContextOrderBySortOrderAsc(context);
        List<UUID> criterionIds = criteria.stream().map(EvaluationCriteria::getId).toList();

        Evaluation created =
                switch (context) {
                    case INTERNSHIP -> {
                        Internship internship = internshipRepository
                                .findByIdAndMentorId(subjectId, mentor.userId())
                                .orElseThrow(() -> new EvaluationAccessDeniedException(
                                        "Không tìm thấy thực tập sinh được giao cho mentor"));
                        yield Evaluation.createDraft(
                                mentor.userId(),
                                mentor.email(),
                                context,
                                internship.getId(),
                                internship.getCandidateId(),
                                internship.getCandidateName(),
                                internship.getBatchId(),
                                criterionIds);
                    }
                    case INTERVIEW -> {
                        InterviewRound round = interviewRoundRepository
                                .findByIdAndInterviewerId(subjectId, mentor.userId())
                                .orElseThrow(() -> new EvaluationAccessDeniedException(
                                        "Không tìm thấy vòng phỏng vấn được giao cho mentor"));
                        yield Evaluation.createDraft(
                                mentor.userId(),
                                mentor.email(),
                                context,
                                round.getId(),
                                round.getCandidateId(),
                                round.getCandidateName(),
                                round.getBatchId(),
                                criterionIds);
                    }
                };

        return toDetail(evaluationRepository.save(created));
    }

    @Transactional(readOnly = true)
    public EvaluationDetailResponse getEvaluation(MentorPrincipal mentor, UUID evaluationId) {
        Evaluation evaluation = loadOwned(mentor, evaluationId);
        return toDetail(evaluation);
    }

    @Transactional
    public EvaluationDetailResponse updateDraft(
            MentorPrincipal mentor, UUID evaluationId, UpdateEvaluationRequest request) {
        Evaluation evaluation = loadOwned(mentor, evaluationId);
        evaluation.assertEditable();
        evaluation.saveDraft(request.overallComment());

        if (request.items() != null) {
            for (CriterionScoreRequest itemRequest : request.items()) {
                EvaluationItemScore item = evaluation
                        .findItem(itemRequest.criterionId())
                        .orElseThrow(() -> new EvaluationNotFoundException(
                                "Không tìm thấy tiêu chí trong phiếu"));
                item.update(itemRequest.score(), itemRequest.comment());
            }
        }

        return toDetail(evaluationRepository.save(evaluation));
    }

    @Transactional
    public EvaluationDetailResponse submit(MentorPrincipal mentor, UUID evaluationId) {
        Evaluation evaluation = loadOwned(mentor, evaluationId);
        evaluation.submit();
        return toDetail(evaluationRepository.save(evaluation));
    }

    private List<EvaluationTaskResponse> listInternshipTasks(
            MentorPrincipal mentor, UUID batchId, Map<UUID, String> batchNames) {
        List<Internship> internships = batchId == null
                ? internshipRepository.findByMentorIdOrderByDueAtAsc(mentor.userId())
                : internshipRepository.findByMentorIdAndBatchIdOrderByDueAtAsc(mentor.userId(), batchId);

        Map<UUID, Evaluation> bySubject = indexEvaluations(
                mentor.userId(),
                EvaluationContext.INTERNSHIP,
                internships.stream().map(Internship::getId).toList());

        return internships.stream()
                .map(internship -> {
                    Evaluation evaluation = bySubject.get(internship.getId());
                    return new EvaluationTaskResponse(
                            internship.getId(),
                            EvaluationContext.INTERNSHIP,
                            internship.getCandidateName(),
                            internship.getCandidateId(),
                            internship.getBatchId(),
                            batchNames.getOrDefault(internship.getBatchId(), ""),
                            internship.getDueAt(),
                            toTaskStatus(evaluation),
                            evaluation == null ? null : evaluation.getId());
                })
                .toList();
    }

    private List<EvaluationTaskResponse> listInterviewTasks(
            MentorPrincipal mentor, UUID batchId, Map<UUID, String> batchNames) {
        List<InterviewRound> rounds;
        if (batchId == null) {
            rounds = interviewRoundRepository.findByInterviewerIdOrderByDueAtAsc(mentor.userId());
        } else {
            rounds = interviewRoundRepository.findByInterviewerIdAndBatchIdOrderByDueAtAsc(
                    mentor.userId(), batchId);
        }

        Map<UUID, Evaluation> bySubject = indexEvaluations(
                mentor.userId(),
                EvaluationContext.INTERVIEW,
                rounds.stream().map(InterviewRound::getId).toList());

        return rounds.stream()
                .map(round -> {
                    Evaluation evaluation = bySubject.get(round.getId());
                    String label = round.getRoundName();
                    if (round.getBatchId() != null) {
                        String batchName = batchNames.get(round.getBatchId());
                        if (batchName != null && !batchName.isBlank()) {
                            label = round.getRoundName() + " · " + batchName;
                        }
                    }
                    return new EvaluationTaskResponse(
                            round.getId(),
                            EvaluationContext.INTERVIEW,
                            round.getCandidateName(),
                            round.getCandidateId(),
                            round.getBatchId(),
                            label,
                            round.getDueAt(),
                            toTaskStatus(evaluation),
                            evaluation == null ? null : evaluation.getId());
                })
                .toList();
    }

    private Map<UUID, Evaluation> indexEvaluations(
            UUID mentorId, EvaluationContext context, List<UUID> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Evaluation> map = new HashMap<>();
        for (Evaluation evaluation :
                evaluationRepository.findByMentorIdAndContextAndSubjectIdIn(mentorId, context, subjectIds)) {
            map.put(evaluation.getSubjectId(), evaluation);
        }
        return map;
    }

    private Evaluation loadOwned(MentorPrincipal mentor, UUID evaluationId) {
        Evaluation evaluation = evaluationRepository
                .findByIdWithItems(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException("Không tìm thấy phiếu đánh giá"));
        evaluation.assertOwnedBy(mentor.userId());
        return evaluation;
    }

    private TaskStatus toTaskStatus(Evaluation evaluation) {
        if (evaluation == null) {
            return TaskStatus.NOT_STARTED;
        }
        return evaluation.getStatus() == EvaluationStatus.SUBMITTED
                ? TaskStatus.SUBMITTED
                : TaskStatus.DRAFT;
    }

    private EvaluationDetailResponse toDetail(Evaluation evaluation) {
        List<EvaluationCriteria> criteria =
                criteriaRepository.findByContextOrderBySortOrderAsc(evaluation.getContext());
        Map<UUID, EvaluationCriteria> criteriaById = criteria.stream()
                .collect(Collectors.toMap(EvaluationCriteria::getId, c -> c));

        String batchOrRoundName;
        String skillsSummary;
        String internshipTasks;

        if (evaluation.getContext() == EvaluationContext.INTERNSHIP) {
            Internship internship = internshipRepository
                    .findById(evaluation.getSubjectId())
                    .orElseThrow(() -> new EvaluationNotFoundException("Không tìm thấy thực tập sinh"));
            batchOrRoundName = batchRepository
                    .findById(internship.getBatchId())
                    .map(InternshipBatch::getName)
                    .orElse("");
            skillsSummary = internship.getSkillsSummary();
            internshipTasks = internship.getInternshipTasks();
        } else {
            InterviewRound round = interviewRoundRepository
                    .findById(evaluation.getSubjectId())
                    .orElseThrow(() -> new EvaluationNotFoundException("Không tìm thấy vòng phỏng vấn"));
            batchOrRoundName = round.getRoundName();
            skillsSummary = round.getSkillsSummary();
            internshipTasks = null;
        }

        List<CriterionScoreResponse> items = evaluation.getItems().stream()
                .map(item -> {
                    EvaluationCriteria criterion = criteriaById.get(item.getCriterionId());
                    return new CriterionScoreResponse(
                            item.getCriterionId(),
                            criterion == null ? "" : criterion.getCode(),
                            criterion == null ? "" : criterion.getName(),
                            criterion == null ? null : criterion.getDescription(),
                            item.getScore(),
                            item.getComment(),
                            criterion == null ? 0 : criterion.getSortOrder());
                })
                .sorted((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()))
                .toList();

        return new EvaluationDetailResponse(
                evaluation.getId(),
                evaluation.getContext(),
                evaluation.getSubjectId(),
                evaluation.getCandidateId(),
                evaluation.getCandidateName(),
                evaluation.getBatchId(),
                batchOrRoundName,
                skillsSummary,
                internshipTasks,
                evaluation.getStatus(),
                evaluation.getOverallComment(),
                evaluation.getSubmittedAt(),
                evaluation.getUpdatedAt(),
                evaluation.getStatus() == EvaluationStatus.DRAFT,
                items);
    }
}
