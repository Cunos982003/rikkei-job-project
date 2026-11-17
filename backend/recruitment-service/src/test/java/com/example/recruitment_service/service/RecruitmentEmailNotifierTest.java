package com.example.recruitment_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.recruitment_service.enums.RecruitmentStatus;

@ExtendWith(MockitoExtension.class)
class RecruitmentEmailNotifierTest {
    private static final UUID RECRUITMENT_ID = UUID.randomUUID();

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendsStatusChangeEmailToCandidate() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        RecruitmentEmailNotifier notifier = new RecruitmentEmailNotifier(
                mailSenderProvider, "recruitment@example.test", "smtp.example.test");

        notifier.notifyCandidate(new RecruitmentStatusChangedEvent(
                RECRUITMENT_ID,
                "candidate@example.test",
                RecruitmentStatus.SUBMITTED,
                RecruitmentStatus.SCREENING));

        verify(mailSender).send(argThat((SimpleMailMessage message) ->
                message.getTo().length == 1
                        && "candidate@example.test".equals(message.getTo()[0])
                        && message.getText().contains("SUBMITTED")
                        && message.getText().contains("SCREENING")));
    }

    @Test
    void handlesCandidateMailFailureWithoutPropagatingIt() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        org.springframework.mail.MailSendException failure =
                new org.springframework.mail.MailSendException("SMTP unavailable");
        doThrow(failure).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        RecruitmentEmailNotifier notifier = new RecruitmentEmailNotifier(
                mailSenderProvider, "recruitment@example.test", "smtp.example.test");

        notifier.notifyCandidate(new RecruitmentStatusChangedEvent(
                RECRUITMENT_ID,
                "candidate@example.test",
                RecruitmentStatus.SUBMITTED,
                RecruitmentStatus.SCREENING));

        verify(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void deliversStatusChangeEventsAfterCommit() throws Exception {
        TransactionalEventListener listener = RecruitmentEmailNotifier.class
                .getMethod("notifyCandidate", RecruitmentStatusChangedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertNotNull(listener);
        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
    }
}
