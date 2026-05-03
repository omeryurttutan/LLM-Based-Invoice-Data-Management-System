package com.faturaocr.infrastructure.monitoring;

import com.faturaocr.domain.monitoring.port.LlmApiUsageRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        List<Map<String, Object>> services = new ArrayList<>();

        // Add main status
        services.add(Map.of(
                "name", "Backend API",
                "status", health.getStatus().toString().equals("UP") ? "UP" : "DOWN"));

        // Add components if available
        if (health instanceof CompositeHealth compositeHealth) {
            compositeHealth.getComponents().forEach((name, component) -> {
                services.add(Map.of(
                        "name", capitalize(name),
                        "status", component.getStatus().toString().equals("UP") ? "UP" : "DOWN"));
            });
        }
        status.put("services", services);

        // 2. Resources Summary
        Map<String, Object> resources = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        resources.put("jvmHeapUsage", runtime.totalMemory() - runtime.freeMemory());
        resources.put("jvmHeapMax", runtime.maxMemory());
        resources.put("dbActiveConnections", 1); // Mocked or get from datasource if possible
        resources.put("dbMaxConnections", 10);
        resources.put("diskUsage", 1024L * 1024 * 1024 * 5); // 5GB mock
        resources.put("diskTotal", 1024L * 1024 * 1024 * 20); // 20GB mock
        status.put("resources", resources);

        // 3. LLM Cost Summary (placeholder)
        status.put("llmCost", Map.of(
                "currentMonthCost", 0.0,
                "monthlyLimit", 100.0,
                "dailyCost", 0.0,
                "dailyLimit", 10.0,
                "byProvider", List.of()));

        // 4. Recent Alerts
        status.put("alerts", List.of());

        return status;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private double getCounterValue(String name) {
        Search search = meterRegistry.find(name);
        if (search.counter() != null) {
            return search.counter().count();
        }
        return 0.0;
    }
}
