package com.faturaocr.infrastructure.messaging.rabbitmq;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.event.ExtractionResultMessage;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.notification.enums.NotificationReferenceType;
import com.faturaocr.domain.notification.enums.NotificationSeverity;
import com.faturaocr.domain.rule.service.RuleEngine;
import com.faturaocr.domain.rule.valueobject.TriggerPoint;
import com.faturaocr.domain.template.service.SupplierTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQResultListener {

    private final InvoiceRepository invoiceRepository;
    private final RabbitMQProducerService producerService;
    private final com.faturaocr.domain.notification.service.NotificationService notificationService;
    private final com.faturaocr.application.batch.service.BatchJobTrackingService batchJobTrackingService;
    private final SupplierTemplateService supplierTemplateService;
    private final RuleEngine ruleEngine;

    @Value("${RABBITMQ_MAX_RETRIES:3}")
    private int maxRetries;

    @Value("${RABBITMQ_RETRY_INITIAL_DELAY_MS:10000}")
    private long initialRetryDelayMs;

    @Value("${RABBITMQ_RETRY_MULTIPLIER:3.0}")
    private double retryMultiplier;

    @Transactional
    @RabbitListener(queues = "${RABBITMQ_RESULT_QUEUE:invoice.extraction.result.queue}")
    public void receiveExtractionResult(ExtractionResultMessage message) {
        log.info("Received extraction result for invoice: {}, status: {}", message.getInvoiceId(), message.getStatus());

        Optional<Invoice> invoiceOpt = invoiceRepository.findById(message.getInvoiceId());
        if (invoiceOpt.isEmpty()) {
            log.warn("Invoice not found for ID: {}. Ignoring result.", message.getInvoiceId());
            return;
        }

        Invoice invoice = invoiceOpt.get();

        // Idempotency check: if already processed, ignore
        if (invoice.getStatus() != InvoiceStatus.QUEUED && invoice.getStatus() != InvoiceStatus.PROCESSING) {
            log.info("Invoice {} is already in status {}. Ignoring result.", invoice.getId(), invoice.getStatus());
            return;
        }

        if ("COMPLETED".equalsIgnoreCase(message.getStatus())) {
            handleCompletedResult(invoice, message);
        } else if ("FAILED".equalsIgnoreCase(message.getStatus())) {
            handleFailedResult(invoice, message);
        } else {
            log.warn("Unknown status '{}' in result message for invoice {}", message.getStatus(), invoice.getId());
        }
    }

    private void handleCompletedResult(Invoice invoice, ExtractionResultMessage message) {
        // Update Invoice Data
        ExtractionResultMessage.InvoiceDataDto data = message.getInvoiceData();
        if (data != null) {
            invoice.setInvoiceNumber(data.getInvoiceNumber());
            // Safe parsing for dates would be better, assuming ISO or consistent format
            // from Python
            if (data.getInvoiceDate() != null) {
                try {
                    invoice.setInvoiceDate(java.time.LocalDate.parse(data.getInvoiceDate()));
                } catch (Exception e) {
                    log.warn("Failed to parse invoice date: {}", data.getInvoiceDate());
                }
            }
            if (data.getDueDate() != null) {
                try {
                    invoice.setDueDate(java.time.LocalDate.parse(data.getDueDate()));
                } catch (Exception e) {
                    log.warn("Failed to parse due date: {}", data.getDueDate());
                }
            }

            invoice.setSupplierName(data.getSupplierName());
            invoice.setSupplierTaxNumber(data.getSupplierTaxId());
            invoice.setSupplierAddress(data.getSupplierAddress());

            invoice.setSubtotal(data.getSubtotal());
            invoice.setTaxAmount(data.getTaxAmount());
            invoice.setTotalAmount(data.getTotalAmount());

            // Map Line Items
            if (data.getLineItems() != null) {
                invoice.getItems().clear();
                for (ExtractionResultMessage.LineItemDto itemDto : data.getLineItems()) {
                    InvoiceItem item = new InvoiceItem();
                    item.setDescription(itemDto.getDescription());
                    item.setQuantity(itemDto.getQuantity());
                    item.setUnitPrice(itemDto.getUnitPrice());
                    item.setTotalAmount(itemDto.getTotalPrice());
                    item.setTaxRate(itemDto.getTaxRate());
                    // item calculation if needed
                    invoice.addItem(item);
                }
            }
        }

        invoice.setConfidenceScore(message.getConfidenceScore());
        invoice.setLlmProvider(message.getProvider());
        invoice.setProcessingDurationMs(message.getProcessingDurationMs());

        // Apply Template Suggestions (Auto-Correction)
        try {
            supplierTemplateService.applyTemplateToInvoice(invoice);
        } catch (Exception e) {
            log.warn("Failed to apply template suggestions for invoice {}", invoice.getId(), e);
        }

        // Run Rule Engine (AFTER_EXTRACTION)
        try {
            ruleEngine.evaluateAndExecute(TriggerPoint.AFTER_EXTRACTION, invoice);
        } catch (Exception e) {
            log.error("Failed to execute rules for invoice {}", invoice.getId(), e);
        }

        // Determine Status
        if ("AUTO_VERIFIED".equalsIgnoreCase(message.getSuggestedStatus()) &&
                message.getConfidenceScore() != null &&
                message.getConfidenceScore().compareTo(BigDecimal.valueOf(90)) >= 0) {
            invoice.setStatus(InvoiceStatus.VERIFIED);
            invoice.setVerifiedAt(java.time.LocalDateTime.now());
        } else {
            invoice.setStatus(InvoiceStatus.PENDING);
        }

        invoiceRepository.save(invoice);
        log.info("Invoice {} updated with extraction results. New Status: {}", invoice.getId(), invoice.getStatus());

        // Send Notification
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("invoiceId", invoice.getId());
        metadata.put("invoiceNumber", invoice.getInvoiceNumber());
        metadata.put("provider", invoice.getLlmProvider());
        metadata.put("confidenceScore", invoice.getConfidenceScore());

        com.faturaocr.domain.notification.enums.NotificationType type;
        String title;
        String messageStr;

        if (InvoiceStatus.VERIFIED.equals(invoice.getStatus())) {
            type = com.faturaocr.domain.notification.enums.NotificationType.HIGH_CONFIDENCE_AUTO_VERIFIED;
            title = "Fatura otomatik doğrulandı";
            messageStr = String.format("%s numaralı fatura yüksek güven skoru (%s) ile otomatik doğrulandı.",
                    invoice.getInvoiceNumber(), invoice.getConfidenceScore());
        } else if (invoice.getConfidenceScore() != null &&
                invoice.getConfidenceScore().compareTo(BigDecimal.valueOf(70)) < 0) {
            type = com.faturaocr.domain.notification.enums.NotificationType.LOW_CONFIDENCE;
            title = "Düşük güven skoru";
            messageStr = String.format(
                    "%s numaralı fatura düşük güven skoru (%s) ile çıkarıldı. İncelemeniz gerekiyor.",
                    invoice.getInvoiceNumber(), invoice.getConfidenceScore());
        } else {
            type = com.faturaocr.domain.notification.enums.NotificationType.EXTRACTION_COMPLETED;
            title = "Veri çıkarımı tamamlandı";
            messageStr = String.format("%s numaralı fatura için veri çıkarımı başarıyla tamamlandı.",
                    invoice.getInvoiceNumber());
        }

        notificationService.notify(
                invoice.getCreatedByUserId(), // Assuming user who created gets the notification
                invoice.getCompanyId(),
                type,
                title,
                messageStr,
                NotificationSeverity.SUCCESS,
                NotificationReferenceType.INVOICE,
                invoice.getId(),
                metadata);

        // Update Batch Job if exists
        if (invoice.getBatchId() != null) {
            try {
                batchJobTrackingService.incrementCompleted(invoice.getBatchId());
            } catch (Exception e) {
                log.warn("Failed to update batch job {} for invoice {}", invoice.getBatchId(), invoice.getId(), e);
            }
        }
    }

    private void handleFailedResult(Invoice invoice, ExtractionResultMessage message) {
        int currentAttempt = message.getAttempt() != null ? message.getAttempt() : 1;

        if (currentAttempt < maxRetries) {
            log.warn("Invoice {} extraction failed (Attempt {}/{}). Scheduling retry...",
                    invoice.getId(), currentAttempt, maxRetries);

            try {
                // Increment attempt and republish.
                producerService.republishExtractionRequest(invoice, currentAttempt + 1);

            } catch (Exception e) {
                log.error("Failed to republish retry for invoice {}", invoice.getId(), e);
                failInvoice(invoice, message.getErrorMessage());
            }
        } else {
            log.warn("Max retries ({}) reached for invoice {}. Marking as FAILED.", maxRetries, invoice.getId());
            failInvoice(invoice, message.getErrorMessage());
        }
    }

    private void failInvoice(Invoice invoice, String errorMessage) {
        invoice.setStatus(InvoiceStatus.FAILED);
        invoice.setNotes("Extraction failed: " + errorMessage);
        invoiceRepository.save(invoice);
        log.error("Invoice {} extraction failed. Error: {}", invoice.getId(), errorMessage);

        // Notify failure
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("invoiceId", invoice.getId());
        metadata.put("error", errorMessage);

        notificationService.notify(
                invoice.getCreatedByUserId(),
                invoice.getCompanyId(),
                com.faturaocr.domain.notification.enums.NotificationType.EXTRACTION_FAILED,
                "Veri çıkarımı başarısız",
                String.format("%s numaralı fatura için işlem başarısız oldu: %s",
                        invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "Bilinmeyen",
                        errorMessage),
                NotificationSeverity.ERROR,
                NotificationReferenceType.INVOICE,
                invoice.getId(),
                metadata);

        // Update Batch Job if exists
        if (invoice.getBatchId() != null) {
            try {
                batchJobTrackingService.incrementFailed(invoice.getBatchId());
            } catch (Exception e) {
                log.warn("Failed to update batch job {} for invoice {}", invoice.getBatchId(), invoice.getId(), e);
            }
        }
    }
}
