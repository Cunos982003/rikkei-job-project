package com.example.profileservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cv_sections", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cv_sections_cv_type", columnNames = {"cv_id", "section_type"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CVSection {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", length = 50, nullable = false)
    private SectionType sectionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "item_ids", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<UUID> itemIds = List.of();

    @Column(name = "included", nullable = false)
    @Builder.Default
    private boolean included = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}