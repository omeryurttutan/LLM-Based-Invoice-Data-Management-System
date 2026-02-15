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
}
