package com.example.profileservice.service;

import com.example.profileservice.common.domain.Email;
import com.example.profileservice.common.domain.Phone;
import com.example.profileservice.entity.ProfileContactInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Application service for email/phone masking.
 * Thin wrapper over value object masking methods.
 * Created as part of service decomposition (Task 4.4).
 */
@Service
@RequiredArgsConstructor
public class ProfileMaskingService {

    /**
     * Mask email using the Email value object's masking logic.
     * Returns first 3 characters of local part + *** + @ + domain.
     * For local parts <= 3 characters, returns ***@domain.
     */
    public String maskEmail(Email email) {
        return email != null ? email.masked() : null;
    }

    /**
     * Mask phone using the Phone value object's masking logic.
     * Returns first 3 digits + *** + last digit.
     */
    public String maskPhone(Phone phone) {
        return phone != null ? phone.masked() : null;
    }

    /**
     * Mask contact info in a ProfileContactInfo value object.
     * Returns a new ProfileContactInfo with masked email and phone.
     */
    public ProfileContactInfo maskContactInfo(ProfileContactInfo contactInfo) {
        if (contactInfo == null) {
            return null;
        }
        return ProfileContactInfo.of(
                contactInfo.getEmail() != null ? Email.of(contactInfo.getEmail().value()) : null,
                contactInfo.getPhone() != null ? Phone.of(contactInfo.getPhone().value()) : null,
                contactInfo.getAddress()
        );
    }
}