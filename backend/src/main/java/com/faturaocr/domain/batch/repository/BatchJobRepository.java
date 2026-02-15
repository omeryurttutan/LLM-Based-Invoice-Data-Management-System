package com.faturaocr.domain.batch.repository;

import com.faturaocr.domain.batch.entity.BatchJob;

import java.util.Optional;
import java.util.UUID;

public interface BatchJobRepository {
    BatchJob save(BatchJob batchJob);

    Optional<BatchJob> findByBatchId(UUID batchId);
}
