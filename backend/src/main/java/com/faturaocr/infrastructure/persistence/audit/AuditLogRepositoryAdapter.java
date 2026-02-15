package com.faturaocr.infrastructure.persistence.audit;

import com.faturaocr.domain.audit.entity.AuditLog;
import com.faturaocr.domain.audit.port.AuditLogRepository;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.audit.valueobject.AuditLogFilter;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adapter implementing AuditLogRepository port using JPA.
 */
@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = auditLogMapper.toJpaEntity(auditLog);
        AuditLogJpaEntity savedEntity = auditLogJpaRepository.save(entity);
        if (savedEntity == null) {
            throw new RuntimeException("Failed to save audit log: saved entity is null");
        }
        return auditLogMapper.toDomain(savedEntity);
    }

    @Override
    public Page<AuditLog> findAllByFilters(AuditLogFilter filter, Pageable pageable) {
        Specification<AuditLogJpaEntity> spec = buildSpecification(filter);
        return auditLogJpaRepository.findAll(spec, pageable).map(auditLogMapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable) {
        return auditLogJpaRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable)
                .map(auditLogMapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByUserId(UUID userId, Pageable pageable) {
        return auditLogJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(auditLogMapper::toDomain);
    }

    @Override
    public long countByActionTypeAndDateRange(AuditActionType actionType, LocalDateTime start, LocalDateTime end) {
        return auditLogJpaRepository.countByActionTypeAndDateRange(actionType, start, end);
    }

    private Specification<AuditLogJpaEntity> buildSpecification(AuditLogFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getActionType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actionType"), filter.getActionType()));
            }
            if (filter.getEntityType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), filter.getEntityType()));
            }
            if (filter.getEntityId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), filter.getEntityId()));
            }
            if (filter.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), filter.getUserId()));
            }
            if (filter.getCompanyId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("companyId"), filter.getCompanyId()));
            }
            if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
            }
            if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
            }

            // Default ordering by createdAt desc
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
