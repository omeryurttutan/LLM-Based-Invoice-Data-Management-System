package com.faturaocr.infrastructure.persistence.template;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierTemplateJpaRepository extends JpaRepository<SupplierTemplateJpaEntity, Long> {
    Optional<SupplierTemplateJpaEntity> findByCompanyIdAndSupplierTaxNumber(UUID companyId, String supplierTaxNumber);

    Page<SupplierTemplateJpaEntity> findAllByCompanyId(UUID companyId, Pageable pageable);

    Optional<SupplierTemplateJpaEntity> findByIdAndCompanyId(Long id, UUID companyId);
}
