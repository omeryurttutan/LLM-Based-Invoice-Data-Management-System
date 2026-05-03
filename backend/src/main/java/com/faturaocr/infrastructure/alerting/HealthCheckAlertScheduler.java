package com.faturaocr.infrastructure.alerting;

import com.faturaocr.infrastructure.monitoring.ExtractionServiceHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class HealthCheckAlertScheduler {

    private final ExtractionServiceHealthIndicator extractionServiceHealthIndicator;
    private final AlertService alertService;

    private final AtomicInteger extractionServiceFailureCount = new AtomicInteger(0);
    private static final int FAILURE_THRESHOLD = 2;

    public HealthCheckAlertScheduler(ExtractionServiceHealthIndicator extractionServiceHealthIndicator,
            AlertService alertService) {
        this.extractionServiceHealthIndicator = extractionServiceHealthIndicator;
        this.alertService = alertService;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkHealth() {
        checkExtractionService();
    }

    private void checkExtractionService() {
        Health health = extractionServiceHealthIndicator.health();
        if (Status.DOWN.equals(health.getStatus())) {
            int failures = extractionServiceFailureCount.incrementAndGet();
            if (failures >= FAILURE_THRESHOLD) {
                alertService.sendAlert(
                        AlertType.EXTRACTION_SERVICE_DOWN,
                        AlertSeverity.CRITICAL,
                        "Extraction Service is DOWN",
                        "Service has been down for " + failures + " consecutive checks. Details: "
                                + health.getDetails());
            }
        } else {
            int previousFailures = extractionServiceFailureCount.getAndSet(0);
            if (previousFailures >= FAILURE_THRESHOLD) {
                alertService.sendAlert(
                        AlertType.EXTRACTION_SERVICE_DOWN,
                        AlertSeverity.INFO,
                        "Extraction Service Recovered",
                        "Service is back UP.");
            }
        }
    }
}
