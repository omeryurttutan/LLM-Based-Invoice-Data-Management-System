package com.faturaocr.interfaces.rest.dashboard;

import com.faturaocr.application.dashboard.DashboardService;
import com.faturaocr.application.dashboard.dto.*;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard Statistics and Metrics API")
public class DashboardController {

    private final DashboardService dashboardService;

    private UUID getCompanyId() {
        return CompanyContextHolder.getCompanyId();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get dashboard summary statistics")
    public ResponseEntity<DashboardStatsResponse> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "TRY") Currency currency) {
        return ResponseEntity.ok(dashboardService.getStats(getCompanyId(), dateFrom, dateTo, currency));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get category distribution")
    public ResponseEntity<List<CategoryDistributionResponse>> getCategoryDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "TRY") Currency currency) {
        return ResponseEntity.ok(dashboardService.getCategoryDistribution(getCompanyId(), dateFrom, dateTo, currency));
    }

    @GetMapping("/monthly-trend")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get monthly invoice trends")
    public ResponseEntity<List<MonthlyTrendResponse>> getMonthlyTrend(
            @RequestParam(required = false, defaultValue = "12") int months,
            @RequestParam(required = false, defaultValue = "TRY") Currency currency) {
        return ResponseEntity.ok(dashboardService.getMonthlyTrend(getCompanyId(), months, currency));
    }

    @GetMapping("/top-suppliers")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get top suppliers")
    public ResponseEntity<TopSuppliersResponse> getTopSuppliers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "TRY") Currency currency,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getTopSuppliers(getCompanyId(), dateFrom, dateTo, currency, limit));
    }

    @GetMapping("/pending-actions")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get pending invoices for action")
    public ResponseEntity<PendingActionsResponse> getPendingActions(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getPendingActions(getCompanyId(), limit));
    }

    @GetMapping("/status-timeline")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    @Operation(summary = "Get daily status changes timeline")
    public ResponseEntity<List<StatusTimelineResponse>> getStatusTimeline(
            @RequestParam(required = false, defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getStatusTimeline(getCompanyId(), days));
    }

    @GetMapping("/extraction-performance")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    @Operation(summary = "Get LLM extraction performance metrics")
    public ResponseEntity<ExtractionPerformanceResponse> getExtractionPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(dashboardService.getExtractionPerformance(getCompanyId(), dateFrom, dateTo));
    }
}
