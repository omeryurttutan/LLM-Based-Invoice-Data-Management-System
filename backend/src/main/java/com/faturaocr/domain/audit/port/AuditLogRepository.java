package com.faturaocr.domain.audit.port;

import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.audit.valueobject.AuditLogFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repository port for audit logs. No update or delete — audit logs are
 * immutable.
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    Page<AuditLog> findAllByFilters(AuditLogFilter filter, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    long countByActionTypeAndDateRange(AuditActionType actionType, LocalDateTime start, LocalDateTime end);
}
