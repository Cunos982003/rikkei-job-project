package com.example.profileservice.profile.domain;

import com.example.profileservice.common.domain.Email;
import com.example.profileservice.common.domain.Phone;
import java.util.Objects;

public final class ProfileContactInfo {
    private final Email email;
    private final Phone phone;
    private final String address;

    private ProfileContactInfo(Email email, Phone phone, String address) {
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public static ProfileContactInfo of(Email email, Phone phone, String address) {
        return new ProfileContactInfo(email, phone, address);
    }

    public Email getEmail() {
        return email;
    }

    public Phone getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public ProfileContactInfo masked() {
        return new ProfileContactInfo(email, phone, address);
    }

    public String getMaskedEmail() {
        return email != null ? email.masked() : null;
    }

    public String getMaskedPhone() {
        return phone != null ? phone.masked() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfileContactInfo that = (ProfileContactInfo) o;
        return Objects.equals(email, that.email) &&
               Objects.equals(phone, that.phone) &&
               Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, phone, address);
    }

    @Override
    public String toString() {
        return "ProfileContactInfo{" +
               "email=" + email +
               ", phone=" + phone +
               ", address='" + address + '\'' +
               '}';
    }
}