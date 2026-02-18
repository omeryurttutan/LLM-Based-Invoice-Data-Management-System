package com.faturaocr.infrastructure.persistence.monitoring;

import com.faturaocr.domain.monitoring.entity.LlmApiUsage;
import com.faturaocr.domain.monitoring.port.LlmApiUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LlmApiUsageRepositoryAdapter implements LlmApiUsageRepository {

    private final LlmApiUsageJpaRepository jpaRepository;

    @Override
    public LlmApiUsage save(LlmApiUsage usage) {
        return jpaRepository.save(usage);
    }

    @Override
    public List<LlmApiUsage> findByCompanyIdAndCreatedAtBetween(UUID companyId, LocalDateTime start,
            LocalDateTime end) {
        return jpaRepository.findByCompanyIdAndCreatedAtBetween(companyId, start, end);
    }

    @Override
    public List<LlmApiUsage> findByCompanyIdAndProviderAndCreatedAtBetween(UUID companyId, String provider,
            LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCompanyIdAndProviderAndCreatedAtBetween(companyId, provider, start, end);
    }
}
