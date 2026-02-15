package com.faturaocr.interfaces.rest.invoice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class InvoiceBulkUploadResponse {
    private UUID batchId;
    private int totalFiles;
    private int acceptedFiles;
    private int rejectedFiles;
    private List<FileStatus> files;

    @Data
    @Builder
    public static class FileStatus {
        private String fileName;
        private String status; // ACCEPTED, REJECTED
        private UUID invoiceId;
        private String rejectionReason;
    }
}
