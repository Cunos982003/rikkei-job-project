package com.example.jobservice.job.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class JobSkillId implements Serializable {

    private UUID jobId;
    private String skillId;

    public JobSkillId() {
    }

    public JobSkillId(UUID jobId, String skillId) {
        this.jobId = jobId;
        this.skillId = skillId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        JobSkillId that = (JobSkillId) o;
        return Objects.equals(jobId, that.jobId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, skillId);
    }
}
