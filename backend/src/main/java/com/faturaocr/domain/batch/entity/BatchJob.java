package com.faturaocr.domain.batch.entity;

import com.faturaocr.domain.batch.valueobject.BatchStatus;
import com.faturaocr.domain.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BatchJob extends BaseEntity {
    private UUID batchId;
    private UUID userId;
    private UUID companyId;

    private int totalFiles;
    private int completedFiles;
    private int failedFiles;

    private BatchStatus status;
    private LocalDateTime completedAt;

    public BatchJob() {
        super();
        this.batchId = UUID.randomUUID();
        this.status = BatchStatus.IN_PROGRESS;
        this.totalFiles = 0;
        this.completedFiles = 0;
        this.failedFiles = 0;
    }

    public void incrementCompleted() {
        this.completedFiles++;
        checkCompletion();
    }

    public void incrementFailed() {
        this.failedFiles++;
        checkCompletion();
    }

    private void checkCompletion() {
        if (completedFiles + failedFiles >= totalFiles) {
            this.completedAt = LocalDateTime.now();
            if (failedFiles == 0) {
                this.status = BatchStatus.COMPLETED;
            } else if (completedFiles == 0) {
                this.status = BatchStatus.FAILED;
            } else {
                this.status = BatchStatus.PARTIALLY_COMPLETED;
            }
        }
    }
}
