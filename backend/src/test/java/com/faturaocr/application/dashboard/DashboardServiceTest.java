package com.faturaocr.application.dashboard;

import com.faturaocr.application.dashboard.dto.*;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.infrastructure.persistence.dashboard.DashboardQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardQueryRepository dashboardQueryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getStats_ShouldReturnStats() {
        UUID companyId = UUID.randomUUID();
        DashboardStatsResponse expected = DashboardStatsResponse.builder().build();
        when(dashboardQueryRepository.getDashboardStats(companyId, null, null, Currency.TRY)).thenReturn(expected);

        DashboardStatsResponse actual = dashboardService.getStats(companyId, null, null, Currency.TRY);

        assertEquals(expected, actual);
        verify(dashboardQueryRepository).getDashboardStats(companyId, null, null, Currency.TRY);
    }

    @Test
    void getCategoryDistribution_ShouldReturnList() {
        UUID companyId = UUID.randomUUID();
        List<CategoryDistributionResponse> expected = Collections.emptyList();
        when(dashboardQueryRepository.getCategoryDistribution(companyId, null, null, Currency.TRY))
                .thenReturn(expected);

        List<CategoryDistributionResponse> actual = dashboardService.getCategoryDistribution(companyId, null, null,
                Currency.TRY);

        assertEquals(expected, actual);
        verify(dashboardQueryRepository).getCategoryDistribution(companyId, null, null, Currency.TRY);
    }

    @Test
    void getMonthlyTrend_ShouldReturnList() {
        UUID companyId = UUID.randomUUID();
        List<MonthlyTrendResponse> expected = Collections.emptyList();
        when(dashboardQueryRepository.getMonthlyTrend(companyId, 12, Currency.TRY)).thenReturn(expected);

        List<MonthlyTrendResponse> actual = dashboardService.getMonthlyTrend(companyId, 12, Currency.TRY);

        assertEquals(expected, actual);
        verify(dashboardQueryRepository).getMonthlyTrend(companyId, 12, Currency.TRY);
    }

    @Test
    void getTopSuppliers_ShouldReturnResponse() {
        UUID companyId = UUID.randomUUID();
        TopSuppliersResponse expected = TopSuppliersResponse.builder().build();
        when(dashboardQueryRepository.getTopSuppliers(companyId, null, null, Currency.TRY, 10)).thenReturn(expected);

        TopSuppliersResponse actual = dashboardService.getTopSuppliers(companyId, null, null, Currency.TRY, 10);

        assertEquals(expected, actual);
        verify(dashboardQueryRepository).getTopSuppliers(companyId, null, null, Currency.TRY, 10);
    }

    @Test
    void getPendingActions_ShouldReturnResponse() {
        UUID companyId = UUID.randomUUID();
        PendingActionsResponse expected = PendingActionsResponse.builder().build();
        when(dashboardQueryRepository.getPendingActions(companyId, 10)).thenReturn(expected);

        PendingActionsResponse actual = dashboardService.getPendingActions(companyId, 10);

        assertEquals(expected, actual);
        verify(dashboardQueryRepository).getPendingActions(companyId, 10);
    }

    @Test
    void getStatusTimeline_ShouldReturnList() {
        UUID companyId = UUID.randomUUID();
        List<StatusTimelineResponse> expected = Collections.emptyList();
        when(dashboardQueryRepository.getStatusTimeline(companyId, 30)).thenReturn(expected);

        List<StatusTimelineResponse> actual = dashboardService.getStatusTimeline(companyId, 30);

        assertEquals(expected, actual);
        verify(dashboardQueryRepository).getStatusTimeline(companyId, 30);
    }

    @Test
    void getExtractionPerformance_ShouldReturnResponse() {
        UUID companyId = UUID.randomUUID();
        ExtractionPerformanceResponse expected = ExtractionPerformanceResponse.builder().build();
        when(dashboardQueryRepository.getExtractionPerformance(companyId, null, null)).thenReturn(expected);

        ExtractionPerformanceResponse actual = dashboardService.getExtractionPerformance(companyId, null, null);

        assertEquals(expected, actual);
        verify(dashboardQueryRepository).getExtractionPerformance(companyId, null, null);
    }
}
