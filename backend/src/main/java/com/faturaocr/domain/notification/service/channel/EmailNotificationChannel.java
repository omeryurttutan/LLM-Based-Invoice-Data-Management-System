package com.faturaocr.domain.notification.service.channel;

import com.faturaocr.domain.notification.entity.Notification;
import com.faturaocr.domain.notification.enums.NotificationType;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final UserRepository userRepository;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public String getChannelName() {
        return "email";
    }

    @Override
    public boolean supports(NotificationType type) {
        // Support all types, let preferences decide if it should be sent
        return true;
    }

    @Override
    @Async
    public void send(Notification notification, UUID userId) {
        if (userId == null) {
            log.warn("Cannot send email: userId is null");
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) {
            log.warn("Cannot send email: user or email not found for userId: {}", userId);
            return;
        }

        try {
            sendEmail(user, notification);
            log.info("Sent email notification to user {} for event {}", user.getEmail(), notification.getType());
        } catch (Exception e) {
            log.error("Failed to send email to user {}", userId, e);
        }
    }

    private void sendEmail(User user, Notification notification) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(String.format("%s <%s>", fromName, fromEmail));
        helper.setTo(user.getEmailValue());
        helper.setSubject(getSubject(notification));

        Context context = new Context();
        context.setVariable("firstName", user.getFullName());
        context.setVariable("message", notification.getMessage());
        context.setVariable("title", notification.getTitle());
        context.setVariable("notification", notification);
        context.setVariable("actionUrl", getActionUrl(notification));

        // Use a generic template if specific one doesn't exist (or just use generic for
        // now)
        // In a real app, we might map NotificationType to template name
        String htmlContent = templateEngine.process("email/generic-notification", context);
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }

    private String getSubject(Notification notification) {
        // Can be customized based on type
        return notification.getTitle();
    }

    private String getActionUrl(Notification notification) {
        // Logic to build URL based on reference type and ID
        if (notification.getReferenceType() != null && notification.getReferenceId() != null) {
            switch (notification.getReferenceType()) {
                case INVOICE:
                    return frontendUrl + "/invoices/" + notification.getReferenceId();
                case BATCH:
                    return frontendUrl + "/invoices?batchId=" + notification.getReferenceId();
                default:
                    return frontendUrl + "/notifications";
            }
        }
        return frontendUrl + "/notifications";
    }
}
