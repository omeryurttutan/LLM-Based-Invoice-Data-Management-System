package com.faturaocr.application.batch.service;

import com.faturaocr.domain.batch.entity.BatchJob;
import com.faturaocr.domain.batch.repository.BatchJobRepository;
import com.faturaocr.domain.batch.valueobject.BatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchJobTrackingService {

    private final BatchJobRepository batchJobRepository;

    @Transactional
    public BatchJob createBatchJob(UUID userId, UUID companyId, int totalFiles) {
        BatchJob batchJob = new BatchJob();
        batchJob.setUserId(userId);
        batchJob.setCompanyId(companyId);
        batchJob.setTotalFiles(totalFiles);
        batchJob.setStatus(BatchStatus.IN_PROGRESS);
        return batchJobRepository.save(batchJob);
    }

    @Transactional
    public void incrementCompleted(UUID batchId) {
        batchJobRepository.findByBatchId(batchId).ifPresent(batchJob -> {
            batchJob.incrementCompleted();
            batchJobRepository.save(batchJob);
        });
    }

    @Transactional
    public void incrementFailed(UUID batchId) {
        batchJobRepository.findByBatchId(batchId).ifPresent(batchJob -> {
            batchJob.incrementFailed();
            batchJobRepository.save(batchJob);
        });
    }

    public BatchJob getBatchJob(UUID batchId) {
        return batchJobRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch job not found: " + batchId));
    }
}
