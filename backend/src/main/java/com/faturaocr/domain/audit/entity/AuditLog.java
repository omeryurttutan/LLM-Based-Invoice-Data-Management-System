package com.faturaocr.domain.audit.entity;

import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.common.entity.BaseEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log domain entity. Immutable — no setters, no update/delete.
 */
@Getter
public class AuditLog extends BaseEntity {

    private UUID userId;
    private String userEmail;
    private AuditActionType actionType;
    private String entityType;
    private UUID entityId;
    private UUID companyId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private String requestId;
    private String description;
    private String metadata;

    @Builder
    public AuditLog(UUID id, UUID userId, String userEmail, AuditActionType actionType,
            String entityType, UUID entityId, UUID companyId,
            String oldValue, String newValue, String ipAddress,
            String userAgent, String requestId, String description,
            String metadata, LocalDateTime createdAt) {
        super(id != null ? id : UUID.randomUUID());
        this.userId = userId;
        this.userEmail = userEmail;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.companyId = companyId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestId = requestId;
        this.description = description;
        this.metadata = metadata;
        if (createdAt != null) {
            this.createdAt = createdAt;
        }
    }
}
