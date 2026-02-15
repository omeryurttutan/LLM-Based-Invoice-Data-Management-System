package com.faturaocr.infrastructure.persistence.kvkk;

import com.faturaocr.domain.kvkk.valueobject.ConsentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConsentJpaRepository extends JpaRepository<UserConsentJpaEntity, UUID> {

    @Query("SELECT uc FROM UserConsentJpaEntity uc WHERE uc.userId = :userId AND uc.consentType = :type ORDER BY uc.grantedAt DESC LIMIT 1")
    Optional<UserConsentJpaEntity> findLatestConsent(UUID userId, ConsentType type);

    List<UserConsentJpaEntity> findAllByUserIdOrderByGrantedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
