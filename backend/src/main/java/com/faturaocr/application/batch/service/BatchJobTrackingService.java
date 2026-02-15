package com.faturaocr.application.batch.service;

import com.faturaocr.domain.batch.entity.BatchJob;
import com.faturaocr.domain.batch.repository.BatchJobRepository;
import com.faturaocr.domain.batch.valueobject.BatchStatus;
import com.faturaocr.domain.notification.enums.NotificationReferenceType;
import com.faturaocr.domain.notification.enums.NotificationSeverity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchJobTrackingService {

    private final BatchJobRepository batchJobRepository;
    private final com.faturaocr.domain.notification.service.NotificationService notificationService;

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
            boolean wasFinished = isFinished(batchJob.getStatus());
            batchJob.incrementCompleted();
            BatchJob saved = batchJobRepository.save(batchJob);
            if (!wasFinished && isFinished(saved.getStatus())) {
                notifyBatchCompletion(saved);
            }
        });
    }

    @Transactional
    public void incrementFailed(UUID batchId) {
        batchJobRepository.findByBatchId(batchId).ifPresent(batchJob -> {
            boolean wasFinished = isFinished(batchJob.getStatus());
            batchJob.incrementFailed();
            BatchJob saved = batchJobRepository.save(batchJob);
            if (!wasFinished && isFinished(saved.getStatus())) {
                notifyBatchCompletion(saved);
            }
        });
    }

    public BatchJob getBatchJob(UUID batchId) {
        return batchJobRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Batch job not found: " + batchId));
    }

    private boolean isFinished(BatchStatus status) {
        return status == BatchStatus.COMPLETED || status == BatchStatus.FAILED
                || status == BatchStatus.PARTIALLY_COMPLETED;
    }

    private void notifyBatchCompletion(BatchJob batchJob) {
        com.faturaocr.domain.notification.enums.NotificationType type = batchJob
                .getStatus() == BatchStatus.COMPLETED
                        ? com.faturaocr.domain.notification.enums.NotificationType.BATCH_COMPLETED
                        : com.faturaocr.domain.notification.enums.NotificationType.BATCH_FAILED; // Or warning for
                                                                                                 // partial

        String title = "Toplu İşlem Tamamlandı";
        String message = String.format("Toplu yükleme tamamlandı. Toplam: %d, Başarılı: %d, Hatalı: %d",
                batchJob.getTotalFiles(), batchJob.getCompletedFiles(), batchJob.getFailedFiles());

        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("batchId", batchJob.getBatchId());
        metadata.put("total", batchJob.getTotalFiles());
        metadata.put("completed", batchJob.getCompletedFiles());
        metadata.put("failed", batchJob.getFailedFiles());

        notificationService.notify(
                batchJob.getUserId(),
                batchJob.getCompanyId(),
                type,
                title,
                message,
                batchJob.getStatus() == BatchStatus.COMPLETED ? NotificationSeverity.SUCCESS
                        : NotificationSeverity.ERROR,
                NotificationReferenceType.BATCH,
                batchJob.getBatchId(),
                metadata);
    }
}
