package com.example.jobservice.company;

import com.example.jobservice.company.dto.HrAccountResponse;
import com.example.jobservice.company.service.HrAccountCreatedEvent;
import com.example.jobservice.company.service.HrAccountEmailListener;
import com.example.jobservice.email.EmailService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class HrAccountEmailListenerTest {

    @Test
    void emailFailureIsReflectedInAccountCreationResponse() {
        EmailService emailService = mock(EmailService.class);
        doThrow(new IllegalStateException("SMTP unavailable"))
                .when(emailService)
                .sendHrAccountCredentials("hr@example.com", "HR User", "Company", "temp-password");

        HrAccountResponse response = new HrAccountResponse(
                UUID.randomUUID(), "hr@example.com", "HR User", "ROLE_HR", UUID.randomUUID(), true);
        HrAccountCreatedEvent event = new HrAccountCreatedEvent(
                "hr@example.com", "HR User", "Company", "temp-password", response);

        new HrAccountEmailListener(emailService).sendCredentials(event);

        assertThat(response.isEmailSent()).isFalse();
        assertThat(response.getMessage()).contains("không gửi được email");
    }
}
