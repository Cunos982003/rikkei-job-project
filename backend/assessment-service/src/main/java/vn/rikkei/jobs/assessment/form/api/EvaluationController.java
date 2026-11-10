package vn.rikkei.jobs.assessment.form.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.EvaluationDetailResponse;
import vn.rikkei.jobs.assessment.form.api.dto.EvaluationDtos.UpdateEvaluationRequest;
import vn.rikkei.jobs.assessment.form.application.EvaluationService;
import vn.rikkei.jobs.assessment.security.MentorPrincipal;

@RestController
@RequestMapping("/api/v1/evaluations")
@Tag(name = "Evaluations", description = "DG-01/DG-04: phiếu đánh giá mentor")
@PreAuthorize("hasAnyRole('MENTOR','ADMIN')")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu đánh giá")
    public EvaluationDetailResponse get(
            @AuthenticationPrincipal MentorPrincipal mentor, @PathVariable UUID id) {
        return evaluationService.getEvaluation(mentor, id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Lưu nháp phiếu đánh giá")
    public EvaluationDetailResponse updateDraft(
            @AuthenticationPrincipal MentorPrincipal mentor,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEvaluationRequest request) {
        return evaluationService.updateDraft(mentor, id, request);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Gửi phiếu (không sửa được sau khi gửi)")
    public EvaluationDetailResponse submit(
            @AuthenticationPrincipal MentorPrincipal mentor, @PathVariable UUID id) {
        return evaluationService.submit(mentor, id);
    }
}
