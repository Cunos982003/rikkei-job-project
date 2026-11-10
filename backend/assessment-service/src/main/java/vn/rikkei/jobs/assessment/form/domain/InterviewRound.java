package vn.rikkei.jobs.assessment.form.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interview_rounds")
public class InterviewRound {

    @Id
    private UUID id;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "round_name", nullable = false)
    private String roundName;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "candidate_name", nullable = false)
    private String candidateName;

    @Column(name = "interviewer_id", nullable = false)
    private UUID interviewerId;

    @Column(name = "interviewer_email", nullable = false)
    private String interviewerEmail;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "skills_summary", columnDefinition = "TEXT")
    private String skillsSummary;

    protected InterviewRound() {}

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public String getRoundName() {
        return roundName;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public UUID getInterviewerId() {
        return interviewerId;
    }

    public String getInterviewerEmail() {
        return interviewerEmail;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public String getSkillsSummary() {
        return skillsSummary;
    }
}
