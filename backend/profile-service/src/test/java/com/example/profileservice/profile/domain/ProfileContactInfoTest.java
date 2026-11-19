package com.example.profileservice.profile.domain;

import com.example.profileservice.common.domain.Email;
import com.example.profileservice.common.domain.Phone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileContactInfoTest {

    @Test
    void of_createsInstance() {
        Email email = Email.of("user@example.com");
        Phone phone = Phone.of("0901234567");
        ProfileContactInfo info = ProfileContactInfo.of(email, phone, "123 Street");

        assertThat(info.getEmail()).isEqualTo(email);
        assertThat(info.getPhone()).isEqualTo(phone);
        assertThat(info.getAddress()).isEqualTo("123 Street");
    }

    @Test
    void getMaskedEmail_returnsMasked() {
        Email email = Email.of("username@example.com");
        Phone phone = Phone.of("0901234567");
        ProfileContactInfo info = ProfileContactInfo.of(email, phone, "123 Street");

        assertThat(info.getMaskedEmail()).isEqualTo("use***@example.com");
    }

    @Test
    void getMaskedPhone_returnsMasked() {
        Email email = Email.of("user@example.com");
        Phone phone = Phone.of("0901234567");
        ProfileContactInfo info = ProfileContactInfo.of(email, phone, "123 Street");

        assertThat(info.getMaskedPhone()).isEqualTo("090***7");
    }

    @Test
    void equals_and_hashCode_work() {
        Email email = Email.of("user@example.com");
        Phone phone = Phone.of("0901234567");
        ProfileContactInfo info1 = ProfileContactInfo.of(email, phone, "123 Street");
        ProfileContactInfo info2 = ProfileContactInfo.of(email, phone, "123 Street");

        assertThat(info1).isEqualTo(info2);
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
    }
}