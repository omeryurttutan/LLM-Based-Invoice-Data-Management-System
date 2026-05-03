package com.faturaocr.application.dashboard;

import com.faturaocr.application.dashboard.dto.CategoryDistributionResponse;
import com.faturaocr.application.dashboard.dto.DashboardStatsResponse;
import com.faturaocr.application.dashboard.dto.ExtractionPerformanceResponse;
import com.faturaocr.application.dashboard.dto.MonthlyTrendResponse;
import com.faturaocr.application.dashboard.dto.PendingActionsResponse;
import com.faturaocr.application.dashboard.dto.StatusTimelineResponse;
import com.faturaocr.application.dashboard.dto.TopSuppliersResponse;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.infrastructure.persistence.dashboard.DashboardQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardQueryRepository dashboardQueryRepository;

    @Cacheable(value = "dashboard-stats", key = "#companyId.toString() + '-' + " +
            "(#dateFrom != null ? #dateFrom.toString() : 'null') + '-' + " +
            "(#dateTo != null ? #dateTo.toString() : 'null') + '-' + " +
            "(#currency != null ? #currency.name() : 'TRY')")
    public DashboardStatsResponse getStats(UUID companyId, LocalDate dateFrom, LocalDate dateTo, Currency currency) {
        return dashboardQueryRepository.getDashboardStats(companyId, dateFrom, dateTo, currency);
    }

    @Cacheable(value = "dashboard-categories", key = "#companyId.toString() + '-' + " +
            "(#dateFrom != null ? #dateFrom.toString() : 'null') + '-' + " +
            "(#dateTo != null ? #dateTo.toString() : 'null') + '-' + " +
            "(#currency != null ? #currency.name() : 'TRY')")
    public List<CategoryDistributionResponse> getCategoryDistribution(UUID companyId, LocalDate dateFrom,
            LocalDate dateTo, Currency currency) {
        return dashboardQueryRepository.getCategoryDistribution(companyId, dateFrom, dateTo, currency);
    }

    @Cacheable(value = "dashboard-trends", key = "#companyId.toString() + '-' + #months + '-' + " +
            "(#currency != null ? #currency.name() : 'TRY')")
    public List<MonthlyTrendResponse> getMonthlyTrend(UUID companyId, int months, Currency currency) {
        return dashboardQueryRepository.getMonthlyTrend(companyId, months, currency);
    }

    @Cacheable(value = "dashboard-suppliers", key = "#companyId.toString() + '-' + " +
            "(#dateFrom != null ? #dateFrom.toString() : 'null') + '-' + " +
            "(#dateTo != null ? #dateTo.toString() : 'null') + '-' + " +
            "(#currency != null ? #currency.name() : 'TRY') + '-' + #limit")
    public TopSuppliersResponse getTopSuppliers(UUID companyId, LocalDate dateFrom, LocalDate dateTo, Currency currency,
            int limit) {
        return dashboardQueryRepository.getTopSuppliers(companyId, dateFrom, dateTo, currency, limit);
    }

    @Cacheable(value = "dashboard-pending", key = "#companyId.toString() + '-' + #limit")
    public PendingActionsResponse getPendingActions(UUID companyId, int limit) {
        return dashboardQueryRepository.getPendingActions(companyId, limit);
    }

    @Cacheable(value = "dashboard-timeline", key = "#companyId.toString() + '-' + #days")
    public List<StatusTimelineResponse> getStatusTimeline(UUID companyId, int days) {
        return dashboardQueryRepository.getStatusTimeline(companyId, days);
    }

    @Cacheable(value = "dashboard-extraction", key = "#companyId.toString() + '-' + " +
            "(#dateFrom != null ? #dateFrom.toString() : 'null') + '-' + " +
            "(#dateTo != null ? #dateTo.toString() : 'null')")
    public ExtractionPerformanceResponse getExtractionPerformance(UUID companyId, LocalDate dateFrom,
            LocalDate dateTo) {
        return dashboardQueryRepository.getExtractionPerformance(companyId, dateFrom, dateTo);
    }
}
