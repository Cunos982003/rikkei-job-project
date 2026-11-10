package com.example.profileservice.skill.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "skill_scores")
@IdClass(SkillScoreId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillScore {

    @Id
    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Id
    @Column(name = "skill_id", columnDefinition = "uuid")
    private UUID skillId;

    @Column(name = "verified_score")
    private Short verifiedScore;

    @Column(name = "self_score")
    private Short selfScore;

    @Column(nullable = false, length = 20)
    private String level; // Cơ bản, Khá, Vững, Nâng cao

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}