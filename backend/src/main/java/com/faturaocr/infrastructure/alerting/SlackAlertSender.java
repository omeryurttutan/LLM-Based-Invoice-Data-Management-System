package com.faturaocr.infrastructure.alerting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SlackAlertSender {

    // Placeholder for Slack integration.
    // In a real implementation, this would use RestClient or WebClient to POST to a
    // Slack Webhook URL.

    public void send(AlertType type, AlertSeverity severity, String message, String details) {
        log.info("[SLACK MOCK] Sending alert to Slack: Type={}, Severity={}, Message={}", type, severity, message);
    }
}
