package vn.rikkei.jobs.assessment.form.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    private UUID id;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "mentor_email", nullable = false)
    private String mentorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvaluationContext context;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "candidate_name", nullable = false)
    private String candidateName;

    @Column(name = "batch_id")
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvaluationStatus status;

    @Column(name = "overall_comment", columnDefinition = "TEXT")
    private String overallComment;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationItemScore> items = new ArrayList<>();

    protected Evaluation() {}

    public static Evaluation createDraft(
            UUID mentorId,
            String mentorEmail,
            EvaluationContext context,
            UUID subjectId,
            UUID candidateId,
            String candidateName,
            UUID batchId,
            List<UUID> criterionIds) {
        Evaluation evaluation = new Evaluation();
        evaluation.id = UUID.randomUUID();
        evaluation.mentorId = mentorId;
        evaluation.mentorEmail = mentorEmail;
        evaluation.context = context;
        evaluation.subjectId = subjectId;
        evaluation.candidateId = candidateId;
        evaluation.candidateName = candidateName;
        evaluation.batchId = batchId;
        evaluation.status = EvaluationStatus.DRAFT;
        Instant now = Instant.now();
        evaluation.createdAt = now;
        evaluation.updatedAt = now;
        for (UUID criterionId : criterionIds) {
            evaluation.items.add(new EvaluationItemScore(UUID.randomUUID(), evaluation, criterionId));
        }
        return evaluation;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMentorId() {
        return mentorId;
    }

    public String getMentorEmail() {
        return mentorEmail;
    }

    public EvaluationContext getContext() {
        return context;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public String getOverallComment() {
        return overallComment;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<EvaluationItemScore> getItems() {
        return items;
    }

    public Optional<EvaluationItemScore> findItem(UUID criterionId) {
        return items.stream().filter(item -> item.getCriterionId().equals(criterionId)).findFirst();
    }

    public void assertOwnedBy(UUID mentorId) {
        if (!this.mentorId.equals(mentorId)) {
            throw new EvaluationAccessDeniedException("Mentor không có quyền với phiếu này");
        }
    }

    public void assertEditable() {
        if (status == EvaluationStatus.SUBMITTED) {
            throw new EvaluationNotEditableException("Phiếu đã gửi, không thể sửa");
        }
    }

    public void saveDraft(String overallComment) {
        assertEditable();
        this.overallComment = overallComment;
        this.updatedAt = Instant.now();
    }

    public void submit() {
        assertEditable();
        this.status = EvaluationStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.updatedAt = this.submittedAt;
    }
}
