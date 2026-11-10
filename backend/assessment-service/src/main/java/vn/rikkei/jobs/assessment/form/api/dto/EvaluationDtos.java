package vn.rikkei.jobs.assessment.form.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import vn.rikkei.jobs.assessment.form.domain.EvaluationContext;
import vn.rikkei.jobs.assessment.form.domain.EvaluationStatus;
import vn.rikkei.jobs.assessment.form.domain.TaskStatus;

public final class EvaluationDtos {

    private EvaluationDtos() {}

    public record BatchOptionResponse(UUID id, String code, String name) {}

    public record EvaluationTaskResponse(
            UUID subjectId,
            EvaluationContext context,
            String candidateName,
            UUID candidateId,
            UUID batchId,
            String batchOrRoundName,
            Instant dueAt,
            TaskStatus taskStatus,
            UUID evaluationId) {}

    public record EvaluationTaskListResponse(
            List<EvaluationTaskResponse> items, long remainingCount, long totalCount) {}

    public record CriterionScoreRequest(
            @NotNull UUID criterionId,
            @Min(0) @Max(100) Integer score,
            String comment) {}

    public record UpdateEvaluationRequest(
            String overallComment, @Valid List<CriterionScoreRequest> items) {}

    public record CriterionScoreResponse(
            UUID criterionId,
            String code,
            String name,
            String description,
            Integer score,
            String comment,
            int sortOrder) {}

    public record EvaluationDetailResponse(
            UUID id,
            EvaluationContext context,
            UUID subjectId,
            UUID candidateId,
            String candidateName,
            UUID batchId,
            String batchOrRoundName,
            String skillsSummary,
            String internshipTasks,
            EvaluationStatus status,
            String overallComment,
            Instant submittedAt,
            Instant updatedAt,
            boolean editable,
            List<CriterionScoreResponse> items) {}
}
