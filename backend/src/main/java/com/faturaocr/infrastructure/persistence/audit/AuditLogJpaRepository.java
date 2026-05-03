package com.faturaocr.infrastructure.persistence.audit;

import com.faturaocr.domain.audit.valueobject.AuditActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Data JPA repository for audit log entities.
 * Uses JpaSpecificationExecutor for dynamic filtering.
 */
@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID>,
                JpaSpecificationExecutor<AuditLogJpaEntity> {

        Page<AuditLogJpaEntity> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        String entityType, UUID entityId, Pageable pageable);

        Page<AuditLogJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

        @Query("SELECT COUNT(a) FROM AuditLogJpaEntity a WHERE a.actionType = :actionType " +
                        "AND a.createdAt >= :start AND a.createdAt <= :end")
        long countByActionTypeAndDateRange(@Param("actionType") AuditActionType actionType,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Modifying
        @Query("UPDATE AuditLogJpaEntity a SET a.userEmail = :newEmail WHERE a.userId = :userId")
        void updateEmailByUserId(@Param("userId") UUID userId, @Param("newEmail") String newEmail);

}
