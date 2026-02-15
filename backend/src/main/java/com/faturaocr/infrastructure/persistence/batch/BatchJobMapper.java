package com.faturaocr.infrastructure.persistence.batch;

import com.faturaocr.domain.batch.entity.BatchJob;
import org.springframework.stereotype.Component;

@Component
public class BatchJobMapper {

    public BatchJob toDomain(BatchJobJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        BatchJob batchJob = new BatchJob();
        // BaseEntity fields
        batchJob.setId(entity.getId());
        batchJob.setCreatedAt(entity.getCreatedAt());
        batchJob.setUpdatedAt(entity.getUpdatedAt());
        batchJob.setDeleted(entity.isDeleted());
        batchJob.setDeletedAt(entity.getDeletedAt());

        // BatchJob fields
        batchJob.setBatchId(entity.getBatchId());
        batchJob.setUserId(entity.getUserId());
        batchJob.setCompanyId(entity.getCompanyId());
        batchJob.setTotalFiles(entity.getTotalFiles());
        batchJob.setCompletedFiles(entity.getCompletedFiles());
        batchJob.setFailedFiles(entity.getFailedFiles());
        batchJob.setStatus(entity.getStatus());
        batchJob.setCompletedAt(entity.getCompletedAt());

        return batchJob;
    }

    public BatchJobJpaEntity toJpa(BatchJob domain) {
        if (domain == null) {
            return null;
        }
        BatchJobJpaEntity entity = new BatchJobJpaEntity();
        // BaseEntity fields
        entity.setId(domain.getId());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeleted(domain.isDeleted());
        entity.setDeletedAt(domain.getDeletedAt());

        // BatchJob fields
        entity.setBatchId(domain.getBatchId());
        entity.setUserId(domain.getUserId());
        entity.setCompanyId(domain.getCompanyId());
        entity.setTotalFiles(domain.getTotalFiles());
        entity.setCompletedFiles(domain.getCompletedFiles());
        entity.setFailedFiles(domain.getFailedFiles());
        entity.setStatus(domain.getStatus());
        entity.setCompletedAt(domain.getCompletedAt());

        return entity;
    }
}
