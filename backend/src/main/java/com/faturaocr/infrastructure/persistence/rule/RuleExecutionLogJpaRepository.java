package com.faturaocr.infrastructure.persistence.rule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RuleExecutionLogJpaRepository extends JpaRepository<RuleExecutionLogJpaEntity, Long> {
    Page<RuleExecutionLogJpaEntity> findByCompanyId(UUID companyId, Pageable pageable);
}
