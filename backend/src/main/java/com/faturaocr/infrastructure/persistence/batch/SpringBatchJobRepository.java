package com.faturaocr.infrastructure.persistence.batch;

import com.faturaocr.domain.batch.entity.BatchJob;
import com.faturaocr.domain.batch.repository.BatchJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SpringBatchJobRepository implements BatchJobRepository {

    private final BatchJobJpaRepository jpaRepository;
    private final BatchJobMapper mapper;

    @Override
    public BatchJob save(BatchJob batchJob) {
        BatchJobJpaEntity entity = mapper.toJpa(batchJob);
        BatchJobJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<BatchJob> findByBatchId(UUID batchId) {
        return jpaRepository.findByBatchId(batchId)
                .map(mapper::toDomain);
    }
}
