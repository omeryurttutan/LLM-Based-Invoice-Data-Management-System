package com.faturaocr.domain.audit.valueobject;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filter value object for querying audit logs.
 */
@Getter
@Builder
public class AuditLogFilter {

    private AuditActionType actionType;
    private String entityType;
    private UUID entityId;
    private UUID userId;
    private UUID companyId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
