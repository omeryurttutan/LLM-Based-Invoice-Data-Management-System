package com.faturaocr.application.invoice.service;

import com.faturaocr.application.batch.service.BatchJobTrackingService;
import com.faturaocr.domain.batch.entity.BatchJob;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.port.FileStoragePort;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.service.FileValidationService;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.infrastructure.messaging.rabbitmq.RabbitMQProducerService;
import com.faturaocr.interfaces.rest.invoice.dto.InvoiceBulkUploadResponse;
import com.faturaocr.infrastructure.adapter.web.CustomMultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceBulkUploadService {

    private final FileValidationService fileValidationService;
    private final FileStoragePort fileStoragePort;
    private final InvoiceRepository invoiceRepository;
    private final RabbitMQProducerService rabbitMQProducerService;
    private final BatchJobTrackingService batchJobTrackingService;

    @Transactional
    public InvoiceBulkUploadResponse processBulkUpload(MultipartFile[] files, UUID companyId, UUID userId) {
        log.info("Starting bulk upload for company: {}, user: {}, files: {}", companyId, userId, files.length);

        List<MultipartFile> allFiles = new ArrayList<>();

        // Flatten ZIP files if any
        for (MultipartFile file : files) {
            if (isZipFile(file)) {
                allFiles.addAll(extractZip(file));
            } else {
                allFiles.add(file);
            }
        }

        // Create Batch Job
        BatchJob batchJob = batchJobTrackingService.createBatchJob(userId, companyId, allFiles.size());

        List<InvoiceBulkUploadResponse.FileStatus> fileStatuses = new ArrayList<>();
        int acceptedCount = 0;
        int rejectedCount = 0;

        for (MultipartFile file : allFiles) {
            try {
                // 1. Validate
                fileValidationService.validateFile(file);

                // 2. Save File
                String sanitizedFilename = fileValidationService.sanitizeFilename(file.getOriginalFilename());
                String storedPath = fileStoragePort.saveFile(file.getInputStream(), companyId.toString(),
                        sanitizedFilename);

                // 3. Create Invoice (QUEUED)
                Invoice invoice = new Invoice();
                invoice.setCompanyId(companyId);
                invoice.setCreatedByUserId(userId);
                invoice.setOriginalFileName(file.getOriginalFilename());
                invoice.setOriginalFileType(file.getContentType());
                invoice.setOriginalFileSize((int) file.getSize());
                invoice.setStoredFilePath(storedPath);
                invoice.setBatchId(batchJob.getBatchId());
                invoice.setStatus(InvoiceStatus.QUEUED);

                invoice = invoiceRepository.save(invoice);

                // 4. Publish to RabbitMQ
                rabbitMQProducerService.publishExtractionRequest(invoice);

                acceptedCount++;
                fileStatuses.add(InvoiceBulkUploadResponse.FileStatus.builder()
                        .fileName(file.getOriginalFilename())
                        .status("ACCEPTED")
                        .invoiceId(invoice.getId())
                        .build());

            } catch (Exception e) {
                log.error("Failed to process file {} in bulk upload", file.getOriginalFilename(), e);
                rejectedCount++;
                batchJobTrackingService.incrementFailed(batchJob.getBatchId()); // Immediate fail for rejected ones
                fileStatuses.add(InvoiceBulkUploadResponse.FileStatus.builder()
                        .fileName(file.getOriginalFilename())
                        .status("REJECTED")
                        .rejectionReason(e.getMessage())
                        .build());
            }
        }

        return InvoiceBulkUploadResponse.builder()
                .batchId(batchJob.getBatchId())
                .totalFiles(allFiles.size())
                .acceptedFiles(acceptedCount)
                .rejectedFiles(rejectedCount)
                .files(fileStatuses)
                .build();
    }

    private boolean isZipFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null
                && (contentType.equals("application/zip") || contentType.equals("application/x-zip-compressed"))) {
            return true;
        }
        return file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".zip");
    }

    private List<MultipartFile> extractZip(MultipartFile zipFile) {
        List<MultipartFile> extractedFiles = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;

                String filename = entry.getName();
                // Simple security check for zip slip
                if (filename.contains("..")) {
                    log.warn("Skipping zip entry with invalid name: {}", filename);
                    continue;
                }

                if (filename.toLowerCase().endsWith(".zip")) {
                    log.warn("Nested ZIP not supported: {}", filename);
                    continue;
                }

                // Read content to byte array (memory intensive for large files)
                // Limit to 20MB
                byte[] content = zis.readAllBytes();
                if (content.length > 20 * 1024 * 1024) {
                    log.warn("File {} in ZIP is too large, skipping", filename);
                    continue;
                }

                String contentType = determineContentType(filename);

                extractedFiles.add(new CustomMultipartFile(
                        content,
                        "file",
                        filename,
                        contentType));
            }
        } catch (IOException e) {
            log.error("Failed to extract zip", e);
            throw new RuntimeException("Failed to extract ZIP file: " + e.getMessage());
        }
        return extractedFiles;
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".png"))
            return "image/png";
        if (lower.endsWith(".pdf"))
            return "application/pdf";
        if (lower.endsWith(".xml"))
            return "application/xml";
        return "application/octet-stream";
    }
}
