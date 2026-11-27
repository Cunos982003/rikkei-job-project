package com.example.profileservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateTest {

    @Test
    void isExpired_expiresAtNull_returnsFalse() {
        Certificate cert = Certificate.builder()
            .userId(UUID.randomUUID())
            .name("Cert")
            .issuer("Issuer")
            .issuedAt(LocalDate.of(2023, 1, 1))
            .expiresAt(null)
            .build();

        assertThat(cert.isExpired()).isFalse();
        assertThat(cert.isValid()).isTrue();
    }

    @Test
    void isExpired_expiresAtFuture_returnsFalse() {
        Certificate cert = Certificate.builder()
            .userId(UUID.randomUUID())
            .name("Cert")
            .issuer("Issuer")
            .issuedAt(LocalDate.of(2023, 1, 1))
            .expiresAt(LocalDate.now().plusYears(1))
            .build();

        assertThat(cert.isExpired()).isFalse();
        assertThat(cert.isValid()).isTrue();
    }

    @Test
    void isExpired_expiresAtPast_returnsTrue() {
        Certificate cert = Certificate.builder()
            .userId(UUID.randomUUID())
            .name("Cert")
            .issuer("Issuer")
            .issuedAt(LocalDate.of(2020, 1, 1))
            .expiresAt(LocalDate.of(2022, 12, 31))
            .build();

        assertThat(cert.isExpired()).isTrue();
        assertThat(cert.isValid()).isFalse();
    }

    @Test
    void isExpired_expiresAtToday_returnsFalse() {
        Certificate cert = Certificate.builder()
            .userId(UUID.randomUUID())
            .name("Cert")
            .issuer("Issuer")
            .issuedAt(LocalDate.of(2023, 1, 1))
            .expiresAt(LocalDate.now())
            .build();

        assertThat(cert.isExpired()).isFalse();
        assertThat(cert.isValid()).isTrue();
    }
}