package com.example.recruitment_service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyOfferSent(OfferSentEvent event) {
        if (!StringUtils.hasText(event.candidateEmail())) {
            log.error("No candidate email is recorded for offer {}", event.offerId());
            return;
        }
        if (!StringUtils.hasText(smtpHost)) {
            log.error("SMTP is not configured; cannot send offer email for offer {}", event.offerId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("JavaMailSender is unavailable; cannot send offer email for offer {}", event.offerId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(senderAddress)) {
            message.setFrom(senderAddress);
        }
        message.setTo(event.candidateEmail());
        message.setSubject("Thư mời nhận việc (Offer): " + event.position());
        message.setText("Chúc mừng bạn đã nhận được đề nghị tuyển dụng cho vị trí " + event.position()
                + " tại " + event.companyName()
                + ".\nMức lương đề nghị: " + event.salary() + " VND"
                + "\nNgày bắt đầu dự kiến: " + event.startDate()
                + "\nĐịa điểm làm việc: " + event.workLocation()
                + "\nHạn phản hồi: " + event.responseDeadline()
                + "\nVui lòng đăng nhập Rikkei Jobs để Chấp nhận hoặc Từ chối offer trước thời hạn.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.error("Failed to send offer email for offer {}", event.offerId(), exception);
        }
    }
}
