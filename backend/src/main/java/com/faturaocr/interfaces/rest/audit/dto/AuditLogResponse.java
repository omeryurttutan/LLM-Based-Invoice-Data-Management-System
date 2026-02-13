package com.faturaocr.interfaces.rest.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for audit log entries.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String actionType;
    private String entityType;
    private UUID entityId;
    private UUID companyId;
    private String description;
    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private List<FieldChange> changes;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private LocalDateTime createdAt;

    /**
     * Represents a single field change (diff between old and new value).
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldChange {
        private String field;
        private String oldValue;
        private String newValue;
    }
}
