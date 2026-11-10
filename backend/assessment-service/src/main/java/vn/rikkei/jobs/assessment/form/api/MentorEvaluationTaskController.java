package vn.rikkei.jobs.assessment.form.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.BatchOptionResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.EvaluationDetailResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.EvaluationTaskListResponse;
import vn.rikkei.jobs.assessment.form.application.EvaluationService;
import vn.rikkei.jobs.assessment.form.domain.EvaluationContext;
import vn.rikkei.jobs.assessment.security.MentorPrincipal;

@RestController
@RequestMapping("/api/v1/mentor")
@Tag(name = "Mentor evaluation tasks", description = "DG-04: danh sách cần đánh giá")
@PreAuthorize("hasAnyRole('MENTOR','ADMIN')")
public class MentorEvaluationTaskController {

    private final EvaluationService evaluationService;

    public MentorEvaluationTaskController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/batches")
    @Operation(summary = "Danh sách đợt để lọc")
    public List<BatchOptionResponse> listBatches() {
        return evaluationService.listBatches();
    }

    @GetMapping("/evaluation-tasks")
    @Operation(summary = "Người cần đánh giá theo context Thực tập / Phỏng vấn")
    public EvaluationTaskListResponse listTasks(
            @AuthenticationPrincipal MentorPrincipal mentor,
            @RequestParam EvaluationContext context,
            @RequestParam(required = false) UUID batchId) {
        return evaluationService.listTasks(mentor, context, batchId);
    }

    @PostMapping("/evaluation-tasks/{subjectId}/open")
    @Operation(summary = "Mở phiếu (tạo nháp nếu chưa có)")
    public EvaluationDetailResponse openTask(
            @AuthenticationPrincipal MentorPrincipal mentor,
            @PathVariable UUID subjectId,
            @RequestParam EvaluationContext context) {
        return evaluationService.openOrCreate(mentor, context, subjectId);
    }
}
