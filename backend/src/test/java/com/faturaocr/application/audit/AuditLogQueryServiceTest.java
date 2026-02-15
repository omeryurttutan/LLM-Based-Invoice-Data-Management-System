package com.faturaocr.application.audit;

import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditLogFilter;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogQueryService auditLogQueryService;

    @Test
    @DisplayName("Should list audit logs")
    void shouldListAuditLogs() {
        // Given
        UUID companyId = TestFixtures.COMPANY_ID;
        com.faturaocr.interfaces.rest.audit.dto.AuditLogFilterDTO filterDTO = new com.faturaocr.interfaces.rest.audit.dto.AuditLogFilterDTO();
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> expectedPage = new PageImpl<>(Collections.emptyList());

        when(auditLogRepository.findAllByFilters(any(AuditLogFilter.class), eq(pageable))).thenReturn(expectedPage);

        // When
        com.faturaocr.interfaces.rest.audit.dto.AuditLogResponse dummyResponse = com.faturaocr.interfaces.rest.audit.dto.AuditLogResponse
                .builder().build();
        // We can't easily mock the private mapToResponse, so we rely on the service to
        // map it.
        // But since list returned empty, the result should be empty.

        Page<com.faturaocr.interfaces.rest.audit.dto.AuditLogResponse> result = auditLogQueryService
                .listAuditLogs(filterDTO, pageable);

        // Then
        assertThat(result).isNotNull();
        verify(auditLogRepository).findAllByFilters(any(AuditLogFilter.class), eq(pageable));
    }
}
