package com.faturaocr.infrastructure.persistence.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface for company-scoped entities.
 * Provides methods that automatically filter by company.
 */
@NoRepositoryBean
public interface CompanyScopedRepository<T, ID> extends JpaRepository<T, ID> {

    List<T> findByCompanyId(UUID companyId);

    Optional<T> findByIdAndCompanyId(ID id, UUID companyId);

    boolean existsByIdAndCompanyId(ID id, UUID companyId);

    void deleteByIdAndCompanyId(ID id, UUID companyId);

    long countByCompanyId(UUID companyId);
}
