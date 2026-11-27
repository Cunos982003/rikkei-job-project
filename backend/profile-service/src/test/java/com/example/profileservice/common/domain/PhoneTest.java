package com.example.profileservice.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneTest {

    @Test
    void of_validPhone_returnsInstance() {
        Phone p = Phone.of("0901234567");
        assertThat(p.value()).isEqualTo("0901234567");
    }

    @Test
    void of_validPhoneWithPlus84_returnsInstance() {
        Phone p = Phone.of("+84901234567");
        assertThat(p.value()).isEqualTo("+84901234567");
    }

    @Test
    void of_invalidFormat_throws() {
        assertThatThrownBy(() -> Phone.of("abc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid phone format");
    }

    @Test
    void masked_standardPhone() {
        assertThat(Phone.of("0901234567").masked()).isEqualTo("090***7");
    }

    @Test
    void masked_plus84Phone() {
        assertThat(Phone.of("+84901234567").masked()).isEqualTo("+84***7");
    }

    @Test
    void equals_and_hashCode_work() {
        Phone p1 = Phone.of("0901234567");
        Phone p2 = Phone.of("0901234567");
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
}