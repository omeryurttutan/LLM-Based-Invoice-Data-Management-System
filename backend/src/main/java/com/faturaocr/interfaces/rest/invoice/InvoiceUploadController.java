package com.faturaocr.interfaces.rest.invoice;

import com.faturaocr.application.batch.service.BatchJobTrackingService;
import com.faturaocr.application.invoice.service.InvoiceBulkUploadService;
import com.faturaocr.application.invoice.service.InvoiceUploadService;
import com.faturaocr.domain.batch.entity.BatchJob;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.port.FileStoragePort;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.interfaces.rest.common.BaseController;
import com.faturaocr.interfaces.rest.invoice.dto.InvoiceBulkUploadResponse;
import com.faturaocr.interfaces.rest.invoice.dto.InvoiceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Upload", description = "Invoice upload and file management endpoints")
public class InvoiceUploadController extends BaseController {

    private final InvoiceUploadService invoiceUploadService;
    private final InvoiceBulkUploadService invoiceBulkUploadService;
    private final BatchJobTrackingService batchJobTrackingService;
    private final InvoiceRepository invoiceRepository;
    private final FileStoragePort fileStoragePort;

    private UUID getCompanyId() {
        return CompanyContextHolder.getCompanyId();
    }

    private UUID getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser) {
            return ((AuthenticatedUser) authentication.getPrincipal()).userId();
        }
        // Fallback or throw exception
        throw new RuntimeException("User not authenticated");
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Upload single invoice file", description = "Uploads a single invoice file and triggers synchronous extraction")
    public ResponseEntity<InvoiceResponse> uploadInvoice(@RequestParam("file") MultipartFile file) {
        UUID companyId = getCompanyId();
        UUID userId = getUserId();

        Invoice invoice = invoiceUploadService.uploadAndExtract(file, companyId, userId);
        return ResponseEntity.ok(mapToResponse(invoice));
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Bulk upload invoices", description = "Uploads multiple invoice files or a ZIP archive and triggers asynchronous extraction")
    public ResponseEntity<InvoiceBulkUploadResponse> bulkUploadInvoices(@RequestParam("files") MultipartFile[] files) {
        UUID companyId = getCompanyId();
        UUID userId = getUserId();

        InvoiceBulkUploadResponse response = invoiceBulkUploadService.processBulkUpload(files, companyId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Get batch job status", description = "Returns the status of a bulk upload batch")
    public ResponseEntity<BatchJob> getBatchStatus(@PathVariable UUID batchId) {
        BatchJob batchJob = batchJobTrackingService.getBatchJob(batchId);
        if (!batchJob.getCompanyId().equals(getCompanyId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(batchJob);
    }

    @GetMapping("/batch/{batchId}/files")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Get batch files", description = "Returns all invoices associated with a batch")
    public ResponseEntity<List<InvoiceResponse>> getBatchFiles(@PathVariable UUID batchId) {
        UUID companyId = getCompanyId();
        List<Invoice> invoices = invoiceRepository.findByBatchId(batchId);

        List<InvoiceResponse> response = invoices.stream()
                .filter(i -> i.getCompanyId().equals(companyId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/file")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    @Operation(summary = "Download/View invoice file", description = "Stream the original invoice file")
    public ResponseEntity<Resource> getInvoiceFile(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "inline") String disposition) {

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (!invoice.getCompanyId().equals(getCompanyId())) {
            return ResponseEntity.notFound().build();
        }

        String filePath = invoice.getStoredFilePath();
        if (filePath == null) {
            filePath = invoice.getOriginalFilePath();
        }

        if (filePath == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            InputStream inputStream = fileStoragePort.readFile(filePath);
            if (inputStream == null) {
                return ResponseEntity.notFound().build();
            }
            InputStreamResource resource = new InputStreamResource(inputStream);

            String contentType = invoice.getOriginalFileType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename=\"" + invoice.getOriginalFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to read file for invoice {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        // Basic mapping
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .companyId(invoice.getCompanyId())
                .batchId(invoice.getBatchId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .supplierName(invoice.getSupplierName())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .originalFileName(invoice.getOriginalFileName() != null ? invoice.getOriginalFileName() : "unknown")
                .originalFileSize(invoice.getOriginalFileSize())
                .build();
    }
}
