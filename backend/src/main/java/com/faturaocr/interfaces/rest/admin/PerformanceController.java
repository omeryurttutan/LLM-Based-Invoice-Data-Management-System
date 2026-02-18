package com.faturaocr.interfaces.rest.admin;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/performance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Performance", description = "Performance monitoring endpoints")
public class PerformanceController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get system health", description = "Check status of Database, Redis, and System Memory")
    @ApiResponse(responseCode = "200", description = "Health status retrieved")
    public ResponseEntity<PerformanceHealthResponse> getHealth() {
        PerformanceHealthResponse response = new PerformanceHealthResponse();

        // 1. Check Database
        HealthStatus.HealthStatusBuilder dbBuilder = HealthStatus.builder();
        try {
            long start = System.currentTimeMillis();
            jdbcTemplate.execute("SELECT 1");
            long duration = System.currentTimeMillis() - start;
            dbBuilder.status("UP")
                    .latencyMs(duration)
                    .details("Connection successful");

            // Attempt to get Hikari Pool stats
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDS = (com.zaxxer.hikari.HikariDataSource) dataSource;
                com.zaxxer.hikari.HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();
                if (poolMXBean != null) {
                    Map<String, Object> poolStats = new HashMap<>();
                    poolStats.put("active", poolMXBean.getActiveConnections());
                    poolStats.put("idle", poolMXBean.getIdleConnections());
                    poolStats.put("total", poolMXBean.getTotalConnections());
                    poolStats.put("waiting", poolMXBean.getThreadsAwaitingConnection());
                    dbBuilder.extra(poolStats);
                }
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbBuilder.status("DOWN").details(e.getMessage());
        }
        response.setDatabase(dbBuilder.build());

        // 2. Check Redis
        HealthStatus.HealthStatusBuilder redisBuilder = HealthStatus.builder();
        try {
            long start = System.currentTimeMillis();
            redisConnectionFactory.getConnection().ping();
            long duration = System.currentTimeMillis() - start;
            redisBuilder.status("UP")
                    .latencyMs(duration)
                    .details("Connection successful");

            // Try to get Redis info
            try {
                java.util.Properties info = redisConnectionFactory.getConnection().info();
                if (info != null) {
                    Map<String, Object> redisInfo = new HashMap<>();
                    redisInfo.put("connected_clients", info.getProperty("connected_clients"));
                    redisInfo.put("used_memory_human", info.getProperty("used_memory_human"));
                    redisInfo.put("uptime_days", info.getProperty("uptime_in_days"));
                    redisBuilder.extra(redisInfo);
                }
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            redisBuilder.status("DOWN").details(e.getMessage());
        }
        response.setRedis(redisBuilder.build());

        // 3. System Memory & Uptime
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

        Map<String, Object> sysDetails = new HashMap<>();
        sysDetails.put("heapUsedMb", heapUsage.getUsed() / (1024 * 1024));
        sysDetails.put("heapMaxMb", heapUsage.getMax() / (1024 * 1024));
        sysDetails.put("nonHeapUsedMb", nonHeapUsage.getUsed() / (1024 * 1024));
        sysDetails.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());

        response.setSystem(HealthStatus.builder()
                .status("UP")
                .details("System Info")
                .extra(sysDetails)
                .build());

        return ResponseEntity.ok(response);
    }

    @Data
    @Schema(description = "Performance health response")
    public static class PerformanceHealthResponse {
        @Schema(description = "Database status")
        private HealthStatus database;
        @Schema(description = "Redis status")
        private HealthStatus redis;
        @Schema(description = "System status")
        private HealthStatus system;
    }

    @Data
    @Builder
    @Schema(description = "Health status detail")
    public static class HealthStatus {
        @Schema(description = "Status (UP/DOWN)", example = "UP")
        private String status;
        @Schema(description = "Latency in milliseconds", example = "5")
        private Long latencyMs;
        @Schema(description = "Details", example = "Connection successful")
        private String details;
        @Schema(description = "Extra metrics")
        private Map<String, Object> extra;
    }
}
