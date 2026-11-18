package com.example.recruitment_service.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
public class RecruitmentEmailNotifier {
    private static final Logger log = LoggerFactory.getLogger(RecruitmentEmailNotifier.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String senderAddress;
    private final String smtpHost;

    public RecruitmentEmailNotifier(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String senderAddress,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSenderProvider = mailSenderProvider;
        this.senderAddress = senderAddress;
        this.smtpHost = smtpHost;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyHr(RecruitmentSubmittedEvent event) {
        List<String> recipients = event.hrEmails() == null
                ? List.of()
                : event.hrEmails().stream().filter(StringUtils::hasText).distinct().toList();
        if (recipients.isEmpty()) {
            log.error("No HR email recipients were returned for job {}", event.jobId());
            return;
        }
        if (!StringUtils.hasText(smtpHost)) {
            log.error("SMTP is not configured; cannot notify HR for recruitment {}", event.recruitmentId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("JavaMailSender is unavailable; cannot notify HR for recruitment {}", event.recruitmentId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(senderAddress)) {
            message.setFrom(senderAddress);
        }
        message.setTo(recipients.toArray(String[]::new));
        message.setSubject("New recruitment received");
        message.setText("A candidate has applied for job " + event.jobId()
                + ". Recruitment ID: " + event.recruitmentId() + ".");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Failed to notify HR for recruitment {}", event.recruitmentId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyCandidate(RecruitmentStatusChangedEvent event) {
        if (!StringUtils.hasText(event.candidateEmail())) {
            log.error("No candidate email is recorded for recruitment {}", event.recruitmentId());
            return;
        }
        if (!StringUtils.hasText(smtpHost)) {
            log.error("SMTP is not configured; cannot notify candidate for recruitment {}", event.recruitmentId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("JavaMailSender is unavailable; cannot notify candidate for recruitment {}", event.recruitmentId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(senderAddress)) {
            message.setFrom(senderAddress);
        }
        message.setTo(event.candidateEmail());
        message.setSubject("Recruitment status updated");
        message.setText("The status of your recruitment " + event.recruitmentId()
                + " changed from " + event.fromStatus() + " to " + event.toStatus() + ".");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Failed to notify candidate for recruitment {}", event.recruitmentId(), exception);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notifyInterviewScheduled(InterviewScheduledEvent event) {
        List<String> recipients = collectRecipients(event.candidateEmail(), event.hrRecipients());
        if (recipients.isEmpty()) {
            log.warn("No email recipients recorded for interview schedule {}", event.scheduleId());
            return;
        }
        if (!StringUtils.hasText(smtpHost)) {
            log.info("SMTP is not configured; skipping interview scheduled email with .ics for schedule {} to {}",
                    event.scheduleId(), recipients);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender is unavailable; cannot send interview invite for schedule {}", event.scheduleId());
            return;
        }
        String subject = "[Rikkei Jobs] Xác nhận lịch phỏng vấn: " + event.roundName();
        String body = "Lịch phỏng vấn vòng \"" + event.roundName() + "\" đã được xác nhận.\n"
                + "Thời gian: " + event.startAt() + " - " + event.endAt() + "\n"
                + "Phòng họp Rikkei Meet: " + event.meetUrl() + "\n"
                + "Đính kèm file lịch (.ics) để bạn thêm vào Google Calendar / Outlook.";
        byte[] icsBytes = event.icsContent() != null
                ? event.icsContent().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        for (String recipient : recipients) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
                if (StringUtils.hasText(senderAddress)) {
                    helper.setFrom(senderAddress);
                }
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(body, false);
                if (icsBytes.length > 0) {
                    helper.addAttachment("interview-" + event.scheduleId() + ".ics",
                            new ByteArrayResource(icsBytes), "text/calendar; charset=UTF-8");
                }
                mailSender.send(mimeMessage);
            } catch (MailException | MessagingException exception) {
                log.error("Failed to send interview confirmation email to {} for schedule {}",
                        recipient, event.scheduleId(), exception);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notifyInterviewReminder(InterviewReminderEvent event) {
        List<String> recipients = collectRecipients(event.candidateEmail(), event.hrRecipients());
        if (recipients.isEmpty()) {
            log.warn("No email recipients recorded for interview reminder {}", event.scheduleId());
            return;
        }
        if (!StringUtils.hasText(smtpHost)) {
            log.info("SMTP is not configured; skipping {} interview reminder for schedule {} to {}",
                    event.milestone(), event.scheduleId(), recipients);
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender is unavailable; cannot send interview reminder for schedule {}", event.scheduleId());
            return;
        }
        String subject = "[Rikkei Jobs] Nhắc lịch phỏng vấn (" + event.milestone() + "): " + event.roundName();
        String body = "Nhắc lịch phỏng vấn sắp diễn ra trong " + event.milestone() + ".\n"
                + "Vòng phỏng vấn: " + event.roundName() + "\n"
                + "Thời gian: " + event.startAt() + " - " + event.endAt() + "\n"
                + "Link phòng họp Rikkei Meet: " + event.meetUrl();
        for (String recipient : recipients) {
            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(senderAddress)) {
                message.setFrom(senderAddress);
            }
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            try {
                mailSender.send(message);
            } catch (MailException exception) {
                log.error("Failed to send {} interview reminder to {} for schedule {}",
                        event.milestone(), recipient, event.scheduleId(), exception);
            }
        }
    }

    private List<String> collectRecipients(String candidateEmail, List<String> hrRecipients) {
        Set<String> result = new LinkedHashSet<>();
        if (StringUtils.hasText(candidateEmail)) {
            result.add(candidateEmail.trim());
        }
        if (hrRecipients != null) {
            for (String email : hrRecipients) {
                if (StringUtils.hasText(email)) {
                    result.add(email.trim());
                }
            }
        }
        return new ArrayList<>(result);
    }
}
