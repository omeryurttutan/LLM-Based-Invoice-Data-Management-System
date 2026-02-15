package com.faturaocr.infrastructure.persistence.batch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository(value = "batchJobJpaRepository")
public interface BatchJobJpaRepository extends JpaRepository<BatchJobJpaEntity, UUID> {
    Optional<BatchJobJpaEntity> findByBatchId(UUID batchId);
}
