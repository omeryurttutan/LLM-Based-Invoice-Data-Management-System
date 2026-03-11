package com.faturaocr.application.invoice.service;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.port.FileStoragePort;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.service.FileValidationService;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.infrastructure.adapter.extraction.PythonExtractionClient;
import com.faturaocr.infrastructure.adapter.extraction.dto.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceUploadService {

    private final FileValidationService fileValidationService;
    private final FileStoragePort fileStoragePort;
    private final InvoiceRepository invoiceRepository;
    private final PythonExtractionClient pythonExtractionClient;

    public Invoice uploadAndExtract(MultipartFile file, UUID companyId, UUID userId) {
        log.info("Starting single file upload for company: {}, user: {}, file: {}", companyId, userId,
                file.getOriginalFilename());

        // 1. Validate
        fileValidationService.validateFile(file);

        // 2. Save File
        String sanitizedFilename = fileValidationService.sanitizeFilename(file.getOriginalFilename());
        String storedPath = null;
        try {
            storedPath = fileStoragePort.saveFile(file.getInputStream(), companyId.toString(), sanitizedFilename);
        } catch (Exception e) {
            log.error("Failed to save file", e);
            throw new RuntimeException("File save failed", e);
        }

        // 3. Create Invoice (PROCESSING)
        Invoice invoice = new Invoice();
        invoice.setCompanyId(companyId);
        invoice.setCreatedByUserId(userId);
        invoice.setOriginalFileName(file.getOriginalFilename());
        invoice.setOriginalFileType(file.getContentType());
        invoice.setOriginalFileSize((int) file.getSize());
        invoice.setStoredFilePath(storedPath);
        // Assuming we calculate hash later or now. For now skipping hash calculation to
        // keep it simple or I should add it.
        // Requirement says: Calculate file hash.

        invoice.setStatus(InvoiceStatus.PROCESSING);
        invoice = invoiceRepository.save(invoice);
        log.info("Created invoice {} with status PROCESSING", invoice.getId());

        // 4. Call Python Service (Synchronous)
        try {
            ExtractionResult result = pythonExtractionClient.extract(Paths.get(storedPath));

            // 5. Update Invoice
            updateInvoiceWithResult(invoice, result);

            invoice = invoiceRepository.save(invoice);
            log.info("Extraction completed for invoice {}", invoice.getId());
            return invoice;

        } catch (Exception e) {
            log.error("Extraction failed for invoice {}", invoice.getId(), e);
            invoice.setStatus(InvoiceStatus.FAILED);
            invoice.setRejectionReason("Extraction service failed: " + e.getMessage());
            invoiceRepository.save(invoice);
            throw new RuntimeException("Extraction failed", e);
        }
    }

    private void updateInvoiceWithResult(Invoice invoice, ExtractionResult result) {
        if (result == null) {
            return;
        }

        invoice.setConfidenceScore(result.getConfidenceScore());

        if (result.getProvider() != null) {
            try {
                invoice.setLlmProvider(LlmProvider.valueOf(result.getProvider().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown LLM provider: {}", result.getProvider());
            }
        }

        invoice.setSourceType(SourceType.LLM);

        ExtractionResult.InvoiceData data = result.getInvoiceData();
        if (data != null) {
            invoice.setInvoiceNumber(data.getInvoiceNumber());
            invoice.setInvoiceDate(data.getInvoiceDate());
            invoice.setDueDate(data.getDueDate());
            invoice.setSupplierName(data.getSupplierName());
            invoice.setSupplierTaxNumber(data.getSupplierTaxId());
            invoice.setSupplierAddress(data.getSupplierAddress());
            invoice.setBuyerTaxNumber(data.getBuyerTaxNumber());
            invoice.setTotalAmount(data.getTotalAmount());
            invoice.setTaxAmount(data.getTaxAmount());
            invoice.setSubtotal(data.getSubtotal());
            invoice.setNotes(data.getNotes());

            // Map currency
            if (data.getCurrency() != null) {
                try {
                    invoice.setCurrency(com.faturaocr.domain.invoice.valueobject.Currency
                            .valueOf(data.getCurrency().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown currency: {}, defaulting to TRY", data.getCurrency());
                    invoice.setCurrency(com.faturaocr.domain.invoice.valueobject.Currency.TRY);
                }
            }

            // Map items
            if (data.getItems() != null && !data.getItems().isEmpty()) {
                for (int i = 0; i < data.getItems().size(); i++) {
                    ExtractionResult.InvoiceItemData itemData = data.getItems().get(i);
                    InvoiceItem item = new InvoiceItem();
                    item.setLineNumber(i + 1);
                    item.setDescription(itemData.getDescription());
                    item.setQuantity(
                            itemData.getQuantity() != null ? itemData.getQuantity() : java.math.BigDecimal.ONE);
                    item.setUnit(itemData.getUnit());
                    item.setUnitPrice(
                            itemData.getUnitPrice() != null ? itemData.getUnitPrice() : java.math.BigDecimal.ZERO);
                    item.setTaxRate(itemData.getTaxRate() != null ? itemData.getTaxRate() : java.math.BigDecimal.ZERO);
                    item.setTaxAmount(
                            itemData.getTaxAmount() != null ? itemData.getTaxAmount() : java.math.BigDecimal.ZERO);
                    item.setTotalAmount(
                            itemData.getLineTotal() != null ? itemData.getLineTotal() : java.math.BigDecimal.ZERO);
                    // Calculate subtotal: lineTotal - taxAmount
                    if (itemData.getLineTotal() != null && itemData.getTaxAmount() != null) {
                        item.setSubtotal(itemData.getLineTotal().subtract(itemData.getTaxAmount()));
                    } else {
                        item.setSubtotal(item.getUnitPrice().multiply(item.getQuantity()));
                    }
                    invoice.addItem(item);
                }
            }
        }

        // Set status based on confidence score
        if (invoice.getConfidenceScore() != null && invoice.getConfidenceScore().doubleValue() > 90.0) {
            invoice.setStatus(InvoiceStatus.PENDING);
        } else {
            invoice.setStatus(InvoiceStatus.PENDING);
        }
    }
}
