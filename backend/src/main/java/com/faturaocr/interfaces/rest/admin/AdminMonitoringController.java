package com.faturaocr.interfaces.rest.admin;

import com.faturaocr.domain.monitoring.entity.LlmApiUsage;
import com.faturaocr.domain.monitoring.port.LlmApiUsageRepository;
import com.faturaocr.infrastructure.monitoring.LlmCostMonitoringService;
import com.faturaocr.infrastructure.monitoring.SystemStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Monitoring", description = "System monitoring and LLM usage endpoints")
public class AdminMonitoringController {

    private final SystemStatusService systemStatusService;
    private final LlmCostMonitoringService llmCostService;
    private final LlmApiUsageRepository llmApiUsageRepository;

    @GetMapping("/system/status")
    @Operation(summary = "Get system status", description = "Retrieve current system health and status")
    @ApiResponse(responseCode = "200", description = "System status retrieved")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        return ResponseEntity.ok(systemStatusService.getSystemStatus());
    }

    @GetMapping("/llm-usage/summary")
    @Operation(summary = "Get LLM usage summary", description = "Retrieve summary of LLM usage and costs")
    @ApiResponse(responseCode = "200", description = "Usage summary retrieved")
    public ResponseEntity<Map<String, Object>> getLlmUsageSummary(
            @RequestParam(required = false) UUID companyId) {

        if (companyId == null) {
            // Default to a system/demo company ID or handle error if mandatory
            companyId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        Map<String, Object> summary = new HashMap<>();

        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthlyCost = llmCostService.getMonthlyUsage(companyId, currentMonth);
        BigDecimal dailyCost = llmCostService.getDailyUsage(companyId, LocalDate.now());

        summary.put("companyId", companyId);
        summary.put("currentMonth", Map.of(
                "year", currentMonth.getYear(),
                "month", currentMonth.getMonthValue(),
                "totalCostUsd", monthlyCost));
        summary.put("today", Map.of(
                "date", LocalDate.now(),
                "totalCostUsd", dailyCost));

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/llm-usage/details")
    @Operation(summary = "Get LLM usage details", description = "Retrieve detailed LLM usage logs")
    @ApiResponse(responseCode = "200", description = "Usage details retrieved")
    public ResponseEntity<List<LlmApiUsage>> getLlmUsageDetails(
            @RequestParam(required = false) UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (companyId == null) {
            companyId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        }

        List<LlmApiUsage> usage = llmApiUsageRepository.findByCompanyIdAndCreatedAtBetween(
                companyId, from.atStartOfDay(), to.atTime(23, 59, 59));

        return ResponseEntity.ok(usage);
    }
}
