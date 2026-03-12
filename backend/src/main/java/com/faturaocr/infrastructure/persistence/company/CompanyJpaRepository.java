package com.faturaocr.infrastructure.persistence.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, UUID> {

    Optional<CompanyJpaEntity> findByTaxNumber(String taxNumber);

    boolean existsByTaxNumber(String taxNumber);

    Page<CompanyJpaEntity> findAllByIsDeletedFalse(Pageable pageable);

    Page<CompanyJpaEntity> findAllByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    java.util.List<CompanyJpaEntity> findAllBySubscriptionStatusAndTrialEndsAtBefore(
            String subscriptionStatus, java.time.LocalDateTime before);
}
