package com.faturaocr.domain.monitoring.port;

import com.faturaocr.domain.monitoring.entity.LlmApiUsage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LlmApiUsageRepository {
    LlmApiUsage save(LlmApiUsage usage);

    List<LlmApiUsage> findByCompanyIdAndCreatedAtBetween(UUID companyId, LocalDateTime start, LocalDateTime end);

    List<LlmApiUsage> findByCompanyIdAndProviderAndCreatedAtBetween(UUID companyId, String provider,
            LocalDateTime start, LocalDateTime end);
}
