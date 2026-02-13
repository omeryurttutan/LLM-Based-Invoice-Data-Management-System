package com.faturaocr.interfaces.rest.audit.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for binding audit log query filter parameters.
 */
@Getter
@Setter
public class AuditLogFilterDTO {

    private String actionType;
    private String entityType;
    private UUID entityId;
    private UUID userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
