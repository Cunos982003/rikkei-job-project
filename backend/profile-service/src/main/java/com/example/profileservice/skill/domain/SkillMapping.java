package com.example.profileservice.skill.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "skill_mappings", uniqueConstraints = @UniqueConstraint(columnNames = {"source", "source_code", "skill_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillMapping {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 20)
    private String source; // LMS, EXAM, PM

    @Column(name = "source_code", nullable = false, length = 50)
    private String sourceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal weight;
}