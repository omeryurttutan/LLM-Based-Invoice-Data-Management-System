package com.faturaocr.infrastructure.audit;

import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.audit.annotation.Auditable;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Spring AOP Aspect that intercepts methods annotated with @Auditable
 * and automatically creates audit log entries.
 *
 * For UPDATE/DELETE/VERIFY/REJECT: loads old state BEFORE method execution.
 * For CREATE/UPDATE/VERIFY/REJECT: captures new state from method return value.
 * Saves audit log in the same transaction as the business operation.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditEntityLoader entityLoader;
    private final AuditSerializer serializer;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        AuditActionType action = auditable.action();
        String entityType = auditable.entityType();

        // 1. Capture old state BEFORE execution (for UPDATE, DELETE, VERIFY, REJECT)
        String oldValue = null;
        UUID entityId = null;

        if (requiresOldState(action)) {
            entityId = extractEntityIdFromArgs(joinPoint);
            if (entityId != null) {
                Object oldEntity = entityLoader.loadEntity(entityType, entityId);
                oldValue = serializer.serialize(oldEntity);
            }
        }

        // 2. Execute the target method
        Object result = joinPoint.proceed();

        // 3. Capture new state AFTER execution (for CREATE, UPDATE, VERIFY, REJECT)
        String newValue = null;
        if (requiresNewState(action) && result != null) {
            newValue = serializer.serialize(result);
        }

        // 4. Extract entity ID from result (for CREATE, where ID is in the return
        // value)
        if (entityId == null && result != null) {
            entityId = extractEntityIdFromResult(result);
        }
        // Fallback: try to extract from args if still null
        if (entityId == null) {
            entityId = extractEntityIdFromArgs(joinPoint);
        }

        // 5. Get current user info
        UUID userId = null;
        String userEmail = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            userId = authenticatedUser.userId();
            userEmail = authenticatedUser.email();
        }

        // 6. Build description
        String description = buildDescription(auditable, entityType, entityId);

        // 7. Build and save audit log
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .actionType(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .companyId(CompanyContextHolder.getCompanyId())
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(AuditRequestContext.getIpAddress())
                    .userAgent(AuditRequestContext.getUserAgent())
                    .requestId(AuditRequestContext.getRequestId())
                    .description(description)
                    .metadata(AuditRequestContext.getMetadata())
                    .build();

            auditLogRepository.save(auditLog);
            LOGGER.debug("Audit log saved: {} {} {} (entityId={})", action, entityType,
                    joinPoint.getSignature().getName(), entityId);
        } catch (Exception e) {
            // Don't let audit logging failures break the main operation
            LOGGER.error("Failed to save audit log for {} {}: {}",
                    action, entityType, e.getMessage(), e);
        }

        return result;
    }

    private boolean requiresOldState(AuditActionType action) {
        return action == AuditActionType.UPDATE
                || action == AuditActionType.DELETE
                || action == AuditActionType.VERIFY
                || action == AuditActionType.REJECT;
    }

    private boolean requiresNewState(AuditActionType action) {
        return action == AuditActionType.CREATE
                || action == AuditActionType.UPDATE
                || action == AuditActionType.VERIFY
                || action == AuditActionType.REJECT;
    }

    /**
     * Extract entity ID from method arguments.
     * Looks for the first UUID parameter (by convention, this is the entity ID).
     */
    private UUID extractEntityIdFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid;
            }
        }
        return null;
    }

    /**
     * Extract entity ID from the method return value.
     * Looks for getId() method on the result object.
     */
    private UUID extractEntityIdFromResult(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            if (id instanceof UUID uuid) {
                return uuid;
            }
        } catch (NoSuchMethodException e) {
            // Result doesn't have getId() — that's OK
        } catch (Exception e) {
            LOGGER.debug("Could not extract entity ID from result: {}", e.getMessage());
        }
        return null;
    }

    private String buildDescription(Auditable auditable, String entityType, UUID entityId) {
        if (!auditable.description().isEmpty()) {
            return auditable.description();
        }
        String action = auditable.action().name().toLowerCase();
        String entity = entityType.toLowerCase();
        if (entityId != null) {
            return String.format("%s %s %s", action, entity, entityId);
        }
        return String.format("%s %s", action, entity);
    }
}
