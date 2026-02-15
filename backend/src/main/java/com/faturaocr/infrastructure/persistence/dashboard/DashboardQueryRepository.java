package com.faturaocr.infrastructure.persistence.dashboard;

import com.faturaocr.application.dashboard.dto.*;
import com.faturaocr.domain.invoice.valueobject.Currency;

import java.time.LocalDate;
import java.util.UUID;

public interface DashboardQueryRepository {
    DashboardStatsResponse getDashboardStats(UUID companyId, LocalDate dateFrom, LocalDate dateTo, Currency currency);

    java.util.List<CategoryDistributionResponse> getCategoryDistribution(UUID companyId, LocalDate dateFrom,
            LocalDate dateTo, Currency currency);

    java.util.List<MonthlyTrendResponse> getMonthlyTrend(UUID companyId, int months, Currency currency);

    TopSuppliersResponse getTopSuppliers(UUID companyId, LocalDate dateFrom, LocalDate dateTo, Currency currency,
            int limit);

    PendingActionsResponse getPendingActions(UUID companyId, int limit);

    java.util.List<StatusTimelineResponse> getStatusTimeline(UUID companyId, int days);

    ExtractionPerformanceResponse getExtractionPerformance(UUID companyId, LocalDate dateFrom, LocalDate dateTo);
}
