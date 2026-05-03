package com.faturaocr.infrastructure.alerting;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailAlertSender {

    private final JavaMailSender javaMailSender;

    @Value("${app.alerts.email.recipients:admin@example.com}")
    private String[] recipients;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailAlertSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void send(AlertType type, AlertSeverity severity, String message, String details) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(recipients);
            helper.setSubject("[Fatura OCR] " + severity + " - " + type);

            String htmlContent = String.format("""
                    <html>
                    <body>
                        <h2>Alert: %s</h2>
                        <p><strong>Severity:</strong> <span style="color:%s">%s</span></p>
                        <p><strong>Message:</strong> %s</p>
                        <p><strong>Details:</strong></p>
                        <pre>%s</pre>
                        <p><em>Sent by Fatura OCR Alert System at %s</em></p>
                    </body>
                    </html>
                    """,
                    type,
                    getColorForSeverity(severity), severity,
                    message,
                    details,
                    java.time.LocalDateTime.now());

            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("Email alert sent to {}", (Object) recipients);

        } catch (MessagingException e) {
            log.error("Failed to send email alert", e);
        }
    }

    private String getColorForSeverity(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "red";
            case HIGH -> "orange";
            case WARN -> "gold";
            default -> "black";
        };
    }
}
