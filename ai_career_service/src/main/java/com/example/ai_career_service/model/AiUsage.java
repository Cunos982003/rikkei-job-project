package com.example.ai_career_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_usage")
@Getter
@Setter
@NoArgsConstructor
public class AiUsage {

    @EmbeddedId
    private AiUsageId id;

    @Column(name = "count", nullable = false)
    private int count;

    public AiUsage(UUID userId, LocalDate date, int count) {
        this.id = new AiUsageId(userId, date);
        this.count = count;
    }
}
