package com.faturaocr.infrastructure.persistence.audit;

import com.faturaocr.domain.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Maps between AuditLog domain entity and AuditLogJpaEntity.
 */
@Component
public class AuditLogMapper {

    public AuditLogJpaEntity toJpaEntity(AuditLog domain) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID());
        entity.setUserId(domain.getUserId());
        entity.setUserEmail(domain.getUserEmail());
        entity.setActionType(domain.getActionType());
        entity.setEntityType(domain.getEntityType());
        entity.setEntityId(domain.getEntityId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setOldValue(domain.getOldValue());
        entity.setNewValue(domain.getNewValue());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setRequestId(domain.getRequestId());
        entity.setDescription(domain.getDescription());
        entity.setMetadata(domain.getMetadata());
        entity.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
        return entity;
    }

    public AuditLog toDomain(AuditLogJpaEntity entity) {
        return AuditLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .actionType(entity.getActionType())
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .companyId(entity.getCompanyId())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .requestId(entity.getRequestId())
                .description(entity.getDescription())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
