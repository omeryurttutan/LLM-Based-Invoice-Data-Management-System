package com.faturaocr.application.audit;

import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.audit.valueobject.AuditLogFilter;
import com.faturaocr.domain.audit.port.AuditDataSerializer;
import com.faturaocr.infrastructure.audit.AuditSerializer;
import com.faturaocr.interfaces.rest.audit.dto.AuditLogFilterDTO;
import com.faturaocr.interfaces.rest.audit.dto.AuditLogResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditDataSerializer auditSerializer;
    private AuditLogQueryService queryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditSerializer = new AuditSerializer();
        queryService = new AuditLogQueryService(auditLogRepository, auditSerializer);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldListAuditLogsWithFilters() {
        // Arrange
        AuditLogFilterDTO filterDTO = new AuditLogFilterDTO();
        filterDTO.setActionType("CREATE");
        filterDTO.setEntityType("INVOICE");

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .userEmail("user@test.com")
                .actionType(AuditActionType.CREATE)
                .entityType("INVOICE")
                .entityId(UUID.randomUUID())
                .newValue("{\"invoiceNumber\":\"INV-001\"}")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<AuditLog>(List.of(log));
        when(auditLogRepository.findAllByFilters(any(AuditLogFilter.class), any(Pageable.class)))
                .thenReturn(page);

        // Act
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLogResponse> result = queryService.listAuditLogs(filterDTO, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        AuditLogResponse resp = result.getContent().get(0);
        assertEquals("CREATE", resp.getActionType());
        assertEquals("INVOICE", resp.getEntityType());

        // Verify filter was built correctly
        ArgumentCaptor<AuditLogFilter> filterCaptor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(auditLogRepository).findAllByFilters(filterCaptor.capture(), any());
        assertEquals(AuditActionType.CREATE, filterCaptor.getValue().getActionType());
        assertEquals("INVOICE", filterCaptor.getValue().getEntityType());
    }

    @Test
    void shouldComputeChangesDiff() {
        // Arrange
        AuditLogFilterDTO filterDTO = new AuditLogFilterDTO();

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .actionType(AuditActionType.UPDATE)
                .entityType("COMPANY")
                .oldValue("{\"name\":\"OldCompany\",\"city\":\"Istanbul\"}")
                .newValue("{\"name\":\"NewCompany\",\"city\":\"Istanbul\"}")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<AuditLog>(List.of(log));
        when(auditLogRepository.findAllByFilters(any(), any())).thenReturn(page);

        // Act
        Page<AuditLogResponse> result = queryService.listAuditLogs(filterDTO, PageRequest.of(0, 20));

        // Assert
        AuditLogResponse resp = result.getContent().get(0);
        assertNotNull(resp.getChanges());
        assertEquals(1, resp.getChanges().size()); // Only "name" changed, "city" is same
        assertEquals("name", resp.getChanges().get(0).getField());
        assertEquals("OldCompany", resp.getChanges().get(0).getOldValue());
        assertEquals("NewCompany", resp.getChanges().get(0).getNewValue());
    }

    @Test
    void shouldGetEntityHistory() {
        // Arrange
        String entityType = "INVOICE";
        UUID entityId = UUID.randomUUID();

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .actionType(AuditActionType.CREATE)
                .entityType(entityType)
                .entityId(entityId)
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<AuditLog>(List.of(log));
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("INVOICE"), eq(entityId), any()))
                .thenReturn(page);

        // Act
        Page<AuditLogResponse> result = queryService.getEntityHistory(entityType, entityId, PageRequest.of(0, 20));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(entityId, result.getContent().get(0).getEntityId());
    }

    @Test
    void shouldGetUserActivity() {
        // Arrange
        UUID userId = UUID.randomUUID();

        AuditLog log = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .actionType(AuditActionType.LOGIN)
                .entityType("USER")
                .createdAt(LocalDateTime.now())
                .build();

        Page<AuditLog> page = new PageImpl<AuditLog>(List.of(log));
        when(auditLogRepository.findByUserId(eq(userId), any())).thenReturn(page);

        // Act
        Page<AuditLogResponse> result = queryService.getUserActivity(userId, PageRequest.of(0, 20));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(userId, result.getContent().get(0).getUserId());
    }
}
