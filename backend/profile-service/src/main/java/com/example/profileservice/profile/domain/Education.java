package com.example.profileservice.profile.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "educations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "school", nullable = false, length = 200)
    private String school;

    @Column(name = "major", length = 200)
    private String major;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "gpa", precision = 3, scale = 2)
    private java.math.BigDecimal gpa;

    @Column(columnDefinition = "TEXT")
    private String note;
}