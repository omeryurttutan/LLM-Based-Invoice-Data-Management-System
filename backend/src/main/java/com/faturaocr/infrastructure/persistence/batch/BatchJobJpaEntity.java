package com.faturaocr.infrastructure.persistence.batch;

import com.faturaocr.domain.batch.valueobject.BatchStatus;
import com.faturaocr.infrastructure.persistence.common.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_jobs")
@Getter
@Setter
@NoArgsConstructor
public class BatchJobJpaEntity extends BaseJpaEntity {

    @Column(name = "batch_id", nullable = false, unique = true)
    private UUID batchId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "total_files")
    private int totalFiles;

    @Column(name = "completed_files")
    private int completedFiles;

    @Column(name = "failed_files")
    private int failedFiles;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BatchStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
