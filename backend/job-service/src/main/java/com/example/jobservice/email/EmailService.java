package com.example.jobservice.email;

/**
 * Contract for sending HR account credential emails.
 */
public interface EmailService {
    void sendHrAccountCredentials(
            String recipientEmail,
            String recipientName,
            String companyName,
            String temporaryPassword);
}
