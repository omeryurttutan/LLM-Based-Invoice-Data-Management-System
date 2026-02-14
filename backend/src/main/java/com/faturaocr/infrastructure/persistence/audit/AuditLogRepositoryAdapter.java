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

    private final AuditLogJpaRepository jpaRepository;
    private final AuditLogMapper mapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = mapper.toJpaEntity(auditLog);
        AuditLogJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Page<AuditLog> findAllByFilters(AuditLogFilter filter, Pageable pageable) {
        Specification<AuditLogJpaEntity> spec = buildSpecification(filter);
        return jpaRepository.findAll(spec, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable) {
        return jpaRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AuditLog> findByUserId(UUID userId, Pageable pageable) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public long countByActionTypeAndDateRange(AuditActionType actionType, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.countByActionTypeAndDateRange(actionType, start, end);
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
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
