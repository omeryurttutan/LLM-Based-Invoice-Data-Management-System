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
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit log entry details")
public class AuditLogResponse {

    @Schema(description = "Audit Log ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "User ID who performed action", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;

    @Schema(description = "User email", example = "user@example.com")
    private String userEmail;

    @Schema(description = "Action type", example = "UPDATE")
    private String actionType;

    @Schema(description = "Entity type", example = "INVOICE")
    private String entityType;

    @Schema(description = "Entity ID", example = "987e6543-e21b-56d3-a456-426614174000")
    private UUID entityId;

    @Schema(description = "Company ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID companyId;

    @Schema(description = "Action description", example = "Updated invoice status")
    private String description;

    @Schema(description = "Old value (JSON)")
    private Map<String, Object> oldValue;

    @Schema(description = "New value (JSON)")
    private Map<String, Object> newValue;

    @Schema(description = "List of field changes")
    private List<FieldChange> changes;

    @Schema(description = "IP Address", example = "192.168.1.1")
    private String ipAddress;

    @Schema(description = "User Agent", example = "Mozilla/5.0...")
    private String userAgent;

    @Schema(description = "Request ID")
    private String requestId;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    /**
     * Represents a single field change (diff between old and new value).
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field change detail")
    public static class FieldChange {
        @Schema(description = "Field name", example = "status")
        private String field;

        @Schema(description = "Old value", example = "PENDING")
        private String oldValue;

        @Schema(description = "New value", example = "APPROVED")
        private String newValue;
    }
}
