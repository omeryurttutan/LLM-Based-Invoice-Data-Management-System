package com.faturaocr.application.dashboard;

import com.faturaocr.application.dashboard.dto.DashboardStatsResponse;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.infrastructure.persistence.dashboard.DashboardQueryRepository;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardQueryRepository dashboardQueryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Should get dashboard stats")
    void shouldGetDashboardStats() {
        // Given
        DashboardStatsResponse expectedResponse = DashboardStatsResponse.builder()
                .summary(DashboardStatsResponse.Summary.builder()
                        .totalInvoices(100)
                        .build())
                .build();
        when(dashboardQueryRepository.getDashboardStats(TestFixtures.COMPANY_ID, null, null, null))
                .thenReturn(expectedResponse);

        // When
        DashboardStatsResponse response = dashboardService.getStats(TestFixtures.COMPANY_ID, null, null, null);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(dashboardQueryRepository).getDashboardStats(TestFixtures.COMPANY_ID, null, null, null);
    }
}
