package com.faturaocr.infrastructure.persistence.rule;

import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutomationRuleJpaRepository extends JpaRepository<AutomationRuleJpaEntity, Long> {
    Optional<AutomationRuleJpaEntity> findByIdAndCompanyId(Long id, UUID companyId);

    Page<AutomationRuleJpaEntity> findAllByCompanyId(UUID companyId, Pageable pageable);

    List<AutomationRuleJpaEntity> findByCompanyIdAndTriggerPointAndActive(UUID companyId, TriggerPoint triggerPoint,
            boolean active);
}
