package com.example.profileservice.internship.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "internships")
public class Internship {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id; // Đổi từ Long sang UUID

    @Column(name = "user_id", nullable = false)
    private UUID userId; // Theo file Controller của bạn, user_id cũng là UUID

    @Column(name = "batch_id", nullable = false)
    private String batchId; // Trong Controller bạn dùng String cho batchId

    private String batchName;
    private String groupName;
    private String topicName;
    private String mentorEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal finalScore;
    private OffsetDateTime syncedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "internship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InternshipTask> tasks = new ArrayList<>();

    // --- GETTERS VÀ SETTERS ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getMentorEmail() { return mentorEmail; }
    public void setMentorEmail(String mentorEmail) { this.mentorEmail = mentorEmail; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }

    public OffsetDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(OffsetDateTime syncedAt) { this.syncedAt = syncedAt; }

    public List<InternshipTask> getTasks() { return tasks; }
    public void setTasks(List<InternshipTask> tasks) { this.tasks = tasks; }
}