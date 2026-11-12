package com.example.jobservice.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Real SMTP email sender. Active when app.email.enabled=true in
 * application.yaml.
 * Configured via spring.mail.* properties.
 *
 * Example (Gmail SMTP with App Password):
 * spring.mail.host=smtp.gmail.com
 * spring.mail.port=587
 * spring.mail.username=yourapp@gmail.com
 * spring.mail.password=your-app-password # 16-char Google App Password
 * spring.mail.properties.mail.smtp.auth=true
 * spring.mail.properties.mail.smtp.starttls.enable=true
 * app.email.enabled=true
 * app.email.from=yourapp@gmail.com
 */
@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

  private final JavaMailSender mailSender;
  private final String fromAddress;

  public SmtpEmailService(
      JavaMailSender mailSender,
      @org.springframework.beans.factory.annotation.Value("${app.email.from:noreply@rikkei.vn}") String fromAddress) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
  }

  @Override
  public void sendHrAccountCredentials(
      String recipientEmail,
      String recipientName,
      String companyName,
      String temporaryPassword) {

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromAddress, "Rikkei Jobs - Hệ thống Tuyển dụng");
      helper.setTo(recipientEmail);
      helper.setSubject("[Rikkei Jobs] Thông tin tài khoản HR - Doanh nghiệp: " + companyName);
      helper.setText(buildHtmlBody(recipientName, companyName, recipientEmail, temporaryPassword), true);

      mailSender.send(message);

      log.info("[EMAIL SENT] To: {} | Company: {}", recipientEmail, companyName);

    } catch (MessagingException | java.io.UnsupportedEncodingException e) {
      throw new IllegalStateException("Không thể tạo email thông tin tài khoản HR", e);
    }
  }

  private String buildHtmlBody(
      String name,
      String companyName,
      String email,
      String temporaryPassword) {

    return """
        <!DOCTYPE html>
        <html lang="vi">
        <head><meta charset="UTF-8"></head>
        <body style="font-family: Arial, sans-serif; background:#f5f5f5; padding:24px;">
          <div style="max-width:520px; margin:0 auto; background:#fff; border-radius:8px;
                      border:1px solid #e0e0e0; padding:32px;">

            <div style="text-align:center; margin-bottom:24px;">
              <span style="display:inline-block; background:#2563eb; color:#fff;
                           font-size:28px; font-weight:900; width:48px; height:48px;
                           line-height:48px; border-radius:10px;">R</span>
              <h2 style="margin:12px 0 4px; color:#1e293b;">Rikkei Jobs</h2>
              <p style="color:#64748b; font-size:13px; margin:0;">Hệ thống quản lý tuyển dụng</p>
            </div>

            <p style="color:#334155;">Xin chào <strong>%s</strong>,</p>

            <p style="color:#334155;">
              Cán bộ tuyển dụng vừa cấp tài khoản HR cho bạn tại doanh nghiệp
              <strong>%s</strong> trên hệ thống <strong>Rikkei Jobs</strong>.
            </p>

            <div style="background:#f1f5f9; border-left:4px solid #2563eb;
                        border-radius:6px; padding:16px; margin:20px 0;">
              <p style="margin:0 0 8px; font-size:13px; color:#64748b;">
                THÔNG TIN ĐĂNG NHẬP
              </p>
              <table style="width:100%; font-size:14px; color:#1e293b;">
                <tr>
                  <td style="padding:4px 0; color:#64748b; width:140px;">Email:</td>
                  <td style="font-weight:600;">%s</td>
                </tr>
                <tr>
                  <td style="padding:4px 0; color:#64748b;">Mật khẩu tạm thời:</td>
                  <td>
                    <code style="background:#fef3c7; border:1px solid #fcd34d;
                                 border-radius:4px; padding:2px 8px; font-size:15px;
                                 font-weight:700; color:#92400e; letter-spacing:1px;">
                      %s
                    </code>
                  </td>
                </tr>
              </table>
            </div>

            <div style="background:#fef2f2; border:1px solid #fecaca; border-radius:6px;
                        padding:12px 16px; margin-bottom:20px;">
              <p style="margin:0; font-size:13px; color:#991b1b;">
                &#9888; <strong>Bắt buộc đổi mật khẩu:</strong>
                Bạn sẽ được yêu cầu đổi mật khẩu ngay trong lần đăng nhập đầu tiên.
                Vui lòng đổi sang mật khẩu riêng để bảo mật tài khoản.
              </p>
            </div>

            <p style="color:#64748b; font-size:12px; margin-top:24px; border-top:1px solid #e2e8f0; padding-top:16px;">
              Email này được gửi tự động. Nếu bạn không yêu cầu tài khoản này,
              vui lòng liên hệ quản trị viên.<br><br>
              &copy; 2026 Rikkei Jobs — Hệ thống Tuyển dụng Rikkei Education
            </p>
          </div>
        </body>
        </html>
        """.formatted(escapeHtml(name), escapeHtml(companyName), escapeHtml(email), escapeHtml(temporaryPassword));
  }

  private String escapeHtml(String value) {
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
