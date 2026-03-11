package com.faturaocr.infrastructure.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class ExtractionServiceHealthIndicator implements HealthIndicator {

    private final RestClient restClient;
    private final String extractionServiceUrl;

    public ExtractionServiceHealthIndicator(
            @Value("${upload.extraction-service-url:http://localhost:8000}") String extractionServiceUrl) {
        this.restClient = RestClient.builder().build();
        this.extractionServiceUrl = extractionServiceUrl;
    }

    @Override
    public Health health() {
        try {
            String healthUrl = extractionServiceUrl + "/health";
            restClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .toBodilessEntity();

            return Health.up()
                    .withDetail("service", "Extraction Service (Python)")
                    .withDetail("url", extractionServiceUrl)
                    .build();
        } catch (Exception e) {
            log.warn("Extraction service health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("service", "Extraction Service (Python)")
                    .withDetail("url", extractionServiceUrl)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
