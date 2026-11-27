package com.example.jobservice.job.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "job_skills")
@IdClass(JobSkillId.class)
public class JobSkill {

    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Id
    @Column(name = "skill_id", length = 64, nullable = false)
    private String skillId;

    @Column(name = "skill_code", length = 64)
    private String skillCode;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "required", nullable = false)
    private boolean required = true;

    @Column(name = "min_score", nullable = false)
    private int minScore = 0;

    public JobSkill() {
    }

    public JobSkill(UUID jobId, String skillId, String skillCode, String skillName, boolean required, int minScore) {
        this.jobId = jobId;
        this.skillId = skillId;
        this.skillCode = skillCode;
        this.skillName = skillName;
        this.required = required;
        this.minScore = minScore;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getSkillCode() {
        return skillCode;
    }

    public void setSkillCode(String skillCode) {
        this.skillCode = skillCode;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public int getMinScore() {
        return minScore;
    }

    public void setMinScore(int minScore) {
        this.minScore = minScore;
    }
}

