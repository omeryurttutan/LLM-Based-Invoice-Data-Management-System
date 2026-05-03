package com.faturaocr.infrastructure.messaging.rabbitmq;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.event.ExtractionResultMessage;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQResultListenerTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RabbitMQProducerService producerService;

    @InjectMocks
    private RabbitMQResultListener resultListener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resultListener, "maxRetries", 3);
        ReflectionTestUtils.setField(resultListener, "initialRetryDelayMs", 1000L);
        ReflectionTestUtils.setField(resultListener, "retryMultiplier", 2.0);
    }

    @Test
    void shouldHandleCompletedResult() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.QUEUED);

        ExtractionResultMessage message = new ExtractionResultMessage();
        message.setInvoiceId(invoiceId);
        message.setStatus("COMPLETED");
        message.setConfidenceScore(new BigDecimal("95.0"));
        message.setProvider(LlmProvider.GEMINI);
        message.setSuggestedStatus("AUTO_VERIFIED");

        ExtractionResultMessage.InvoiceDataDto data = new ExtractionResultMessage.InvoiceDataDto();
        data.setInvoiceNumber("INV-123");
        message.setInvoiceData(data);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // When
        resultListener.receiveExtractionResult(message);

        // Then
        verify(invoiceRepository).save(invoice);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VERIFIED);
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-123");
        assertThat(invoice.getConfidenceScore()).isEqualTo(new BigDecimal("95.0"));
    }

    @Test
    void shouldHandleFailedResultAndRetry() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.QUEUED);
        invoice.setCorrelationId("corr-123");

        ExtractionResultMessage message = new ExtractionResultMessage();
        message.setInvoiceId(invoiceId);
        message.setStatus("FAILED");
        message.setErrorMessage("Error");
        message.setAttempt(1);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // When
        resultListener.receiveExtractionResult(message);

        // Then
        verify(producerService).republishExtractionRequest(eq(invoice), eq(2));
        verify(invoiceRepository, never()).save(any(Invoice.class)); // Shouldn't save FAILED status yet
    }

    @Test
    void shouldHandleFailedResultAndFailAfterMaxRetries() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.QUEUED);

        ExtractionResultMessage message = new ExtractionResultMessage();
        message.setInvoiceId(invoiceId);
        message.setStatus("FAILED");
        message.setErrorMessage("Final Error");
        message.setAttempt(3); // Max retries = 3

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // When
        resultListener.receiveExtractionResult(message);

        // Then
        verify(producerService, never()).republishExtractionRequest(any(), anyInt());
        verify(invoiceRepository).save(invoice);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.FAILED);
        assertThat(invoice.getNotes()).contains("Final Error");
    }
}
