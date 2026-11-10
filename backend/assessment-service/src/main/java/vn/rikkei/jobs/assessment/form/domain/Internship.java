package vn.rikkei.jobs.assessment.form.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "internships")
public class Internship {

    @Id
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "candidate_name", nullable = false)
    private String candidateName;

    @Column(name = "mentor_id", nullable = false)
    private UUID mentorId;

    @Column(name = "mentor_email", nullable = false)
    private String mentorEmail;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "skills_summary", columnDefinition = "TEXT")
    private String skillsSummary;

    @Column(name = "internship_tasks", columnDefinition = "TEXT")
    private String internshipTasks;

    protected Internship() {}

    public UUID getId() {
        return id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public UUID getMentorId() {
        return mentorId;
    }

    public String getMentorEmail() {
        return mentorEmail;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public String getSkillsSummary() {
        return skillsSummary;
    }

    public String getInternshipTasks() {
        return internshipTasks;
    }
}
