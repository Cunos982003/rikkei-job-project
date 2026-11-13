package com.example.learningservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "position_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionSkill {

    @EmbeddedId
    private PositionSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("positionId")
    @JoinColumn(name = "position_id", nullable = false)
    private TargetPosition targetPosition;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "min_score", nullable = false)
    private Integer minScore;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private Boolean isMandatory = true;
}

