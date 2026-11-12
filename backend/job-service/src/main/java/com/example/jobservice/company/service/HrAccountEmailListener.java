package com.example.jobservice.company.service;

import com.example.jobservice.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class HrAccountEmailListener {

    private static final Logger log = LoggerFactory.getLogger(HrAccountEmailListener.class);

    private final EmailService emailService;

    public HrAccountEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendCredentials(HrAccountCreatedEvent event) {
        try {
            emailService.sendHrAccountCredentials(
                    event.email(),
                    event.fullName(),
                    event.companyName(),
                    event.temporaryPassword());
            event.response().markEmailSent();
        } catch (RuntimeException ex) {
            event.response().markEmailFailed();
            log.error("Failed to send HR account credentials to {}", event.email(), ex);
        }
    }
}
