package com.faturaocr.infrastructure.messaging.rabbitmq;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.event.ExtractionRequestMessage;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final InvoiceRepository invoiceRepository;

    @Value("${RABBITMQ_EXTRACTION_EXCHANGE:invoice.extraction}")
    private String extractionExchange;

    @Value("${RABBITMQ_EXTRACTION_ROUTING_KEY:extraction.request}")
    private String routingKey;

    @Transactional
    public String publishExtractionRequest(Invoice invoice) {
        String correlationId = UUID.randomUUID().toString();

        // Update invoice status and correlation ID
        invoice.setStatus(InvoiceStatus.QUEUED);
        invoice.setCorrelationId(correlationId);
        invoiceRepository.save(invoice);

        ExtractionRequestMessage message = ExtractionRequestMessage.builder()
                .messageId(UUID.randomUUID())
                .invoiceId(invoice.getId())
                .companyId(invoice.getCompanyId())
                .userId(invoice.getCreatedByUserId())
                .filePath(invoice.getOriginalFilePath())
                .fileName(invoice.getOriginalFileName())
                .fileType(invoice.getOriginalFileType())
                .fileSize(Long.valueOf(invoice.getOriginalFileSize()))
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .build();

        try {
            rabbitTemplate.convertAndSend(extractionExchange, routingKey, message);
            log.info("Published extraction request for invoice: {}, correlationId: {}", invoice.getId(), correlationId);
            return correlationId;
        } catch (Exception e) {
            log.error("Failed to publish extraction request for invoice: {}", invoice.getId(), e);
            // Revert status if publishing fails (transaction rollback will handle this if
            // configured properly,
            // but explicit handling is safer if rabbit publish is outside transaction
            // boundary usually)
            throw new RuntimeException("Failed to queue invoice for extraction", e);
        }
    }

    @Transactional
    public void republishExtractionRequest(Invoice invoice, int attempt) {
        String correlationId = invoice.getCorrelationId();

        ExtractionRequestMessage message = ExtractionRequestMessage.builder()
                .messageId(UUID.randomUUID())
                .invoiceId(invoice.getId())
                .companyId(invoice.getCompanyId())
                .userId(invoice.getCreatedByUserId())
                .filePath(invoice.getOriginalFilePath())
                .fileName(invoice.getOriginalFileName())
                .fileType(invoice.getOriginalFileType())
                .fileSize(Long.valueOf(invoice.getOriginalFileSize()))
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId)
                .attempt(attempt)
                .build();

        try {
            rabbitTemplate.convertAndSend(extractionExchange, routingKey, message);
            log.info("Republished extraction request (Attempt {}) for invoice: {}, correlationId: {}",
                    attempt, invoice.getId(), correlationId);
        } catch (Exception e) {
            log.error("Failed to republish extraction request for invoice: {}", invoice.getId(), e);
            throw new RuntimeException("Failed to requeue invoice for extraction", e);
        }
    }
}
