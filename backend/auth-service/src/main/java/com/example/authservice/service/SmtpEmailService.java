package com.example.authservice.service;

import com.example.authservice.exception.ErrorCode;
import com.example.authservice.exception.SsoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromEmail;
    private final String resetUrl;

    public SmtpEmailService(
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${app.mail.from:no-reply@rikkei-jobs.vn}") String fromEmail,
        @Value("${app.mail.reset-url:http://localhost:5173/reset-password}") String resetUrl
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromEmail = fromEmail;
        this.resetUrl = resetUrl;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = resetUrl + "?token=" + resetToken;
        String content = "Hello,\n\n"
            + "We received a request to reset your Rikkei Jobs HR account password.\n"
            + "Please click the link below or use the token to set a new password:\n\n"
            + resetLink + "\n\n"
            + "Token: " + resetToken + "\n\n"
            + "This token will expire in 30 minutes and can only be used once.\n"
            + "If you did not request a password reset, please ignore this email.\n";

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.error("JavaMailSender is not configured. Cannot send password reset email to recipient {}", toEmail);
            throw new SsoException(
                ErrorCode.EXTERNAL_UNAVAILABLE,
                "Mail service is not configured. Please contact system administrator."
            );
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Rikkei Jobs - Password Reset Request");
            message.setText(content);
            mailSender.send(message);
            log.info("Password reset email successfully sent to {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to deliver password reset email via SMTP to {}: {}", toEmail, ex.getMessage());
            throw new SsoException(
                ErrorCode.EXTERNAL_UNAVAILABLE,
                "Unable to deliver password reset email due to mail service error. Please try again later.",
                ex
            );
        }
    }
}
