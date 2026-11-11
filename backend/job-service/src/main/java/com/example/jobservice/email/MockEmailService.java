package com.example.jobservice.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mock email service for local dev & tests (default when app.email.enabled !=
 * true).
 * Prints email content to console log instead of sending.
 * Also stores sent emails in-memory for integration test assertions.
 */
@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class MockEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    public record SentEmail(String to, String recipientName, String companyName, String temporaryPassword) {
    }

    private final List<SentEmail> sentEmails = new ArrayList<>();

    @Override
    public synchronized void sendHrAccountCredentials(
            String recipientEmail,
            String recipientName,
            String companyName,
            String temporaryPassword) {

        sentEmails.add(new SentEmail(recipientEmail, recipientName, companyName, temporaryPassword));

        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║         [MOCK EMAIL] Rikkei Jobs - HR Account        ║");
        log.info("╠══════════════════════════════════════════════════════╣");
        log.info("║  To      : {} ({})", recipientEmail, recipientName);
        log.info("║  Company : {}", companyName);
        log.info("║  Subject : [Rikkei Jobs] Thong tin tai khoan HR");
        log.info("╠══════════════════════════════════════════════════════╣");
        log.info("║  Email dang nhap : {}", recipientEmail);
        log.info("║  Mat khau tam    : {}", temporaryPassword);
        log.info("║  Luu y: Bat buoc doi mat khau o lan dang nhap dau.");
        log.info("╚══════════════════════════════════════════════════════╝");
    }

    public synchronized List<SentEmail> getSentEmails() {
        return Collections.unmodifiableList(new ArrayList<>(sentEmails));
    }

    public synchronized void clear() {
        sentEmails.clear();
    }
}
