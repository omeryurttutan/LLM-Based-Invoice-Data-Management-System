package com.faturaocr.infrastructure.persistence.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for users.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByEmailAndCompanyId(String email, UUID companyId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndCompanyId(String email, UUID companyId);

    Page<UserJpaEntity> findAllByCompanyId(UUID companyId, Pageable pageable);

    java.util.List<UserJpaEntity> findAllByCompanyId(UUID companyId);

    long countByCompanyIdAndRole(UUID companyId, UserJpaEntity.RoleJpa role);
}
