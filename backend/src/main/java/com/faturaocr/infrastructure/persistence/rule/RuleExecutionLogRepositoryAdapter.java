package com.faturaocr.infrastructure.persistence.rule;

import com.faturaocr.domain.rule.entity.RuleExecutionLog;
import com.faturaocr.domain.rule.port.RuleExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RuleExecutionLogRepositoryAdapter implements RuleExecutionLogRepository {

    private final RuleExecutionLogJpaRepository jpaRepository;
    private final RuleExecutionLogMapper mapper;

    @Override
    public RuleExecutionLog save(RuleExecutionLog log) {
        RuleExecutionLogJpaEntity entity = mapper.toJpa(log);
        RuleExecutionLogJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Page<RuleExecutionLog> findByCompanyId(UUID companyId, Pageable pageable) {
        return jpaRepository.findByCompanyId(companyId, pageable)
                .map(mapper::toDomain);
    }
}
