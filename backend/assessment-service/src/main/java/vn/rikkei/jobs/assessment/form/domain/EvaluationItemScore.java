package vn.rikkei.jobs.assessment.form.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "evaluation_item_scores")
public class EvaluationItemScore {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_id", nullable = false)
    private Evaluation evaluation;

    @Column(name = "criterion_id", nullable = false)
    private UUID criterionId;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    protected EvaluationItemScore() {}

    public EvaluationItemScore(UUID id, Evaluation evaluation, UUID criterionId) {
        this.id = id;
        this.evaluation = evaluation;
        this.criterionId = criterionId;
    }

    public UUID getId() {
        return id;
    }

    public Evaluation getEvaluation() {
        return evaluation;
    }

    public UUID getCriterionId() {
        return criterionId;
    }

    public Integer getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }

    public void update(Integer score, String comment) {
        this.score = score;
        this.comment = comment;
    }
}
