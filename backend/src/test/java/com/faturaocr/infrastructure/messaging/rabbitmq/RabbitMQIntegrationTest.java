package com.faturaocr.infrastructure.messaging.rabbitmq;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.event.ExtractionRequestMessage;
import com.faturaocr.domain.invoice.event.ExtractionResultMessage;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class RabbitMQIntegrationTest {

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:3.12-management");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private RabbitMQProducerService producerService;

    @Autowired
    private RabbitMQResultListener resultListener;

    @Autowired
    private com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository invoiceJpaRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${RABBITMQ_EXTRACTION_QUEUE:invoice.extraction.queue}")
    private String extractionQueue;

    @Value("${RABBITMQ_RESULT_QUEUE:invoice.extraction.result.queue}")
    private String resultQueue;

    @BeforeEach
    void setUp() {
        invoiceJpaRepository.deleteAll();
    }

    @Test
    void shouldPublishExtractionRequest() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setOriginalFileName("test-invoice.pdf");
        invoice.setOriginalFilePath("/data/invoices/test-invoice.pdf");
        invoice.setOriginalFileType("application/pdf");
        invoice.setOriginalFileSize(1024);
        invoiceRepository.save(invoice);

        // When
        producerService.publishExtractionRequest(invoice);

        // Then
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.QUEUED);
        assertThat(updatedInvoice.getCorrelationId()).isNotNull();

        // Verify message in queue
        ExtractionRequestMessage message = (ExtractionRequestMessage) rabbitTemplate.receiveAndConvert(extractionQueue,
                5000);
        assertThat(message).isNotNull();
        assertThat(message.getInvoiceId()).isEqualTo(invoice.getId());
        assertThat(message.getCorrelationId()).isEqualTo(updatedInvoice.getCorrelationId());
    }

    @Test
    void shouldProcessExtractionResult() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.QUEUED);
        invoice.setCorrelationId(UUID.randomUUID().toString());
        invoiceRepository.save(invoice);

        ExtractionResultMessage resultMessage = new ExtractionResultMessage();
        resultMessage.setMessageId(UUID.randomUUID());
        resultMessage.setCorrelationId(invoice.getCorrelationId());
        resultMessage.setInvoiceId(invoice.getId());
        resultMessage.setStatus("COMPLETED");
        resultMessage.setConfidenceScore(new BigDecimal("95.0"));
        resultMessage.setProvider(LlmProvider.GEMINI);
        resultMessage.setProcessingDurationMs(1500);
        resultMessage.setTimestamp(LocalDateTime.now());
        resultMessage.setSuggestedStatus("AUTO_VERIFIED");

        ExtractionResultMessage.InvoiceDataDto data = new ExtractionResultMessage.InvoiceDataDto();
        data.setInvoiceNumber("INV-123");
        data.setSupplierName("Test Supplier");
        data.setTotalAmount(new BigDecimal("100.00"));
        resultMessage.setInvoiceData(data);

        // When
        rabbitTemplate.convertAndSend("invoice.extraction.result", "extraction.result", resultMessage);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.VERIFIED);
            assertThat(updatedInvoice.getInvoiceNumber()).isEqualTo("INV-123");
            assertThat(updatedInvoice.getSupplierName()).isEqualTo("Test Supplier");
        });
    }

    @Test
    void shouldHandleFailedExtractionResult() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setStatus(InvoiceStatus.QUEUED);
        invoice.setCorrelationId(UUID.randomUUID().toString());
        invoiceRepository.save(invoice);

        ExtractionResultMessage resultMessage = new ExtractionResultMessage();
        resultMessage.setMessageId(UUID.randomUUID());
        resultMessage.setCorrelationId(invoice.getCorrelationId());
        resultMessage.setInvoiceId(invoice.getId());
        resultMessage.setStatus("FAILED");
        resultMessage.setErrorMessage("Extraction failed");
        resultMessage.setTimestamp(LocalDateTime.now());
        resultMessage.setAttempt(3); // Max retries reached

        // When
        rabbitTemplate.convertAndSend("invoice.extraction.result", "extraction.result", resultMessage);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
            assertThat(updatedInvoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
            assertThat(updatedInvoice.getNotes()).contains("Extraction failed");
        });
    }
}
