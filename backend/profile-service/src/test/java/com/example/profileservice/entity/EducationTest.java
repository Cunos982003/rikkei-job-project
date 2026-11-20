package com.example.profileservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EducationTest {

    @Test
    void isCurrent_toDateNull_returnsTrue() {
        Education edu = Education.builder()
            .userId(UUID.randomUUID())
            .school("University")
            .fromDate(LocalDate.of(2020, 9, 1))
            .toDate(null)
            .build();

        assertThat(edu.isCurrent()).isTrue();
    }

    @Test
    void isCurrent_toDateFuture_returnsTrue() {
        Education edu = Education.builder()
            .userId(UUID.randomUUID())
            .school("University")
            .fromDate(LocalDate.of(2020, 9, 1))
            .toDate(LocalDate.now().plusYears(1))
            .build();

        assertThat(edu.isCurrent()).isTrue();
    }

    @Test
    void isCurrent_toDatePast_returnsFalse() {
        Education edu = Education.builder()
            .userId(UUID.randomUUID())
            .school("University")
            .fromDate(LocalDate.of(2018, 9, 1))
            .toDate(LocalDate.of(2022, 6, 1))
            .build();

        assertThat(edu.isCurrent()).isFalse();
    }

    @Test
    void isCurrent_toDateToday_returnsTrue() {
        Education edu = Education.builder()
            .userId(UUID.randomUUID())
            .school("University")
            .fromDate(LocalDate.of(2020, 9, 1))
            .toDate(LocalDate.now())
            .build();

        assertThat(edu.isCurrent()).isTrue();
    }
}