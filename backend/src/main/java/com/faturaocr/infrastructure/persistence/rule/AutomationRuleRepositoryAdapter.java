package com.faturaocr.infrastructure.persistence.rule;

import com.faturaocr.domain.rule.entity.AutomationRule;
import com.faturaocr.domain.rule.port.AutomationRuleRepository;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AutomationRuleRepositoryAdapter implements AutomationRuleRepository {

    private final AutomationRuleJpaRepository jpaRepository;
    private final AutomationRuleMapper mapper;

    @Override
    public AutomationRule save(AutomationRule rule) {
        AutomationRuleJpaEntity entity = mapper.toJpa(rule);
        AutomationRuleJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AutomationRule> findByIdAndCompanyId(Long id, UUID companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AutomationRule> findAllByCompanyId(UUID companyId, Pageable pageable) {
        return jpaRepository.findAllByCompanyId(companyId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<AutomationRule> findByCompanyIdAndTriggerPointAndActive(UUID companyId, TriggerPoint triggerPoint,
            boolean active) {
        return jpaRepository.findByCompanyIdAndTriggerPointAndActive(companyId, triggerPoint, active)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(AutomationRule rule) {
        if (rule.getId() != null) {
            jpaRepository.deleteById(rule.getId());
        }
    }
}
