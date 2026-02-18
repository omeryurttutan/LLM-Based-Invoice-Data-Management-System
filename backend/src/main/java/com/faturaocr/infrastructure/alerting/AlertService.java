package com.faturaocr.infrastructure.alerting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AlertService {

    private final EmailAlertSender emailAlertSender;
    private final SlackAlertSender slackAlertSender;

    @Value("${app.alerts.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.alerts.slack.enabled:false}")
    private boolean slackEnabled;

    // Alert deduplication cache: AlertType -> Last Sent Time
    private final Map<AlertType, LocalDateTime> lastSentAlerts = new ConcurrentHashMap<>();

    // Mute duration in minutes
    private static final long MUTE_DURATION_MINUTES = 15;

    public AlertService(EmailAlertSender emailAlertSender, SlackAlertSender slackAlertSender) {
        this.emailAlertSender = emailAlertSender;
        this.slackAlertSender = slackAlertSender;
    }

    public void sendAlert(AlertType type, AlertSeverity severity, String message, String details) {
        if (shouldMute(type)) {
            log.info("Alert muted: {} ({})", type, message);
            return;
        }

        log.warn("Sending Alert: [{}] {} - {}", severity, type, message);

        if (emailEnabled) {
            emailAlertSender.send(type, severity, message, details);
        }

        if (slackEnabled) {
            slackAlertSender.send(type, severity, message, details);
        }

        lastSentAlerts.put(type, LocalDateTime.now());

        // TODO: Log to database (alert_log table)
    }

    private boolean shouldMute(AlertType type) {
        LocalDateTime lastSent = lastSentAlerts.get(type);
        if (lastSent == null) {
            return false;
        }
        return lastSent.isAfter(LocalDateTime.now().minusMinutes(MUTE_DURATION_MINUTES));
    }
}
