package com.faturaocr.infrastructure.persistence.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    Optional<CategoryJpaEntity> findByIdAndCompanyIdAndIsDeletedFalse(UUID id, UUID companyId);

    List<CategoryJpaEntity> findAllByCompanyIdAndIsDeletedFalse(UUID companyId);

    List<CategoryJpaEntity> findAllByCompanyIdAndIsActiveTrueAndIsDeletedFalse(UUID companyId);

    boolean existsByNameAndCompanyIdAndIsDeletedFalse(String name, UUID companyId);
}
