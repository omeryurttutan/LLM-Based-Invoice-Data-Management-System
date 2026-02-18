package com.faturaocr.infrastructure.monitoring;

import com.faturaocr.domain.monitoring.port.LlmApiUsageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemStatusService {

    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;
    private final LlmCostMonitoringService llmCostService;

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        // 1. Services Health
        HealthComponent health = healthEndpoint.health();
        status.put("health", health);

        // 2. Metrics Summary
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("extraction_total", getCounterValue("invoice.extraction.total"));
        metrics.put("extraction_success", getCounterValue("invoice.extraction.success"));
        metrics.put("extraction_failure", getCounterValue("invoice.extraction.failure"));
        metrics.put("upload_total", getCounterValue("invoice.upload.total"));
        status.put("metrics", metrics);

        // 3. LLM Cost Summary (Aggregate for all companies or specific if context
        // available)
        // For admin dashboard, we might want total cost across all companies or a
        // specific one.
        // Assuming single tenant or admin viewing aggregate for now.
        // In a real multi-tenant app, we'd need to iterate or sum all.
        // For this project, let's show a placeholder or sum if possible.

        // TODO: Add company context or aggregation query.
        // For now, we will return a structure that the frontend can consume.

        return status;
    }

    private double getCounterValue(String name) {
        Search search = meterRegistry.find(name);
        if (search.counter() != null) {
            return search.counter().count();
        }
        return 0.0;
    }
}
