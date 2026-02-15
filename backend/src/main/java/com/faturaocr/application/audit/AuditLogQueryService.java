package com.faturaocr.application.audit;

import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditDataSerializer;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.audit.valueobject.AuditLogFilter;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.interfaces.rest.audit.dto.AuditLogFilterDTO;
import com.faturaocr.interfaces.rest.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for querying audit logs.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditDataSerializer auditSerializer;

    /**
     * List audit logs with optional filters. MANAGER sees only their company's
     * logs.
     */
    public Page<AuditLogResponse> listAuditLogs(AuditLogFilterDTO filterDTO, Pageable pageable) {
        AuditLogFilter.AuditLogFilterBuilder filterBuilder = AuditLogFilter.builder();

        if (filterDTO.getActionType() != null && !filterDTO.getActionType().isBlank()) {
            filterBuilder.actionType(AuditActionType.valueOf(filterDTO.getActionType()));
        }
        if (filterDTO.getEntityType() != null && !filterDTO.getEntityType().isBlank()) {
            filterBuilder.entityType(filterDTO.getEntityType());
        }
        if (filterDTO.getEntityId() != null) {
            filterBuilder.entityId(filterDTO.getEntityId());
        }
        if (filterDTO.getUserId() != null) {
            filterBuilder.userId(filterDTO.getUserId());
        }
        if (filterDTO.getStartDate() != null) {
            filterBuilder.startDate(filterDTO.getStartDate());
        }
        if (filterDTO.getEndDate() != null) {
            filterBuilder.endDate(filterDTO.getEndDate());
        }

        // Company scoping: MANAGER sees only their company's logs
        // ADMIN sees all (no company filter applied unless explicitly set)
        UUID companyId = CompanyContextHolder.getCompanyId();
        if (companyId != null && !isAdmin()) {
            filterBuilder.companyId(companyId);
        }

        AuditLogFilter filter = filterBuilder.build();
        return auditLogRepository.findAllByFilters(filter, pageable).map(this::mapToResponse);
    }

    /**
     * Get entity change history.
     */
    public Page<AuditLogResponse> getEntityHistory(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType.toUpperCase(), entityId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get user activity log (ADMIN only).
     */
    public Page<AuditLogResponse> getUserActivity(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLog auditLog) {
        Map<String, Object> oldValueMap = auditSerializer.deserializeToMap(auditLog.getOldValue());
        Map<String, Object> newValueMap = auditSerializer.deserializeToMap(auditLog.getNewValue());

        List<AuditLogResponse.FieldChange> changes = computeChanges(oldValueMap, newValueMap);

        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUserId())
                .userEmail(auditLog.getUserEmail())
                .actionType(auditLog.getActionType() != null ? auditLog.getActionType().name() : null)
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .companyId(auditLog.getCompanyId())
                .description(auditLog.getDescription())
                .oldValue(oldValueMap.isEmpty() ? null : oldValueMap)
                .newValue(newValueMap.isEmpty() ? null : newValueMap)
                .changes(changes.isEmpty() ? null : changes)
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .requestId(auditLog.getRequestId())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    /**
     * Compute field-level changes between old and new values.
     */
    private List<AuditLogResponse.FieldChange> computeChanges(Map<String, Object> oldMap, Map<String, Object> newMap) {
        if (oldMap.isEmpty() || newMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<AuditLogResponse.FieldChange> changes = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(oldMap.keySet());
        allKeys.addAll(newMap.keySet());

        for (String key : allKeys) {
            Object oldVal = oldMap.get(key);
            Object newVal = newMap.get(key);

            if (!Objects.equals(oldVal, newVal)) {
                changes.add(AuditLogResponse.FieldChange.builder()
                        .field(key)
                        .oldValue(oldVal != null ? String.valueOf(oldVal) : null)
                        .newValue(newVal != null ? String.valueOf(newVal) : null)
                        .build());
            }
        }
        return changes;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser user) {
            return user.isAdmin();
        }
        return false;
    }
}
