package com.faturaocr.infrastructure.persistence.monitoring;

import com.faturaocr.domain.monitoring.entity.LlmApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LlmApiUsageJpaRepository extends JpaRepository<LlmApiUsage, UUID> {

    List<LlmApiUsage> findByCompanyIdAndCreatedAtBetween(UUID companyId, LocalDateTime start, LocalDateTime end);

    List<LlmApiUsage> findByCompanyIdAndProviderAndCreatedAtBetween(UUID companyId, String provider,
            LocalDateTime start, LocalDateTime end);
}
