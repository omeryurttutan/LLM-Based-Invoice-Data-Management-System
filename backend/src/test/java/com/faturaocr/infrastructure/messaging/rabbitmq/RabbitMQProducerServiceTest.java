package com.faturaocr.infrastructure.messaging.rabbitmq;

import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.event.ExtractionRequestMessage;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMQProducerServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private RabbitMQProducerService producerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producerService, "extractionExchange", "test.exchange");
        ReflectionTestUtils.setField(producerService, "routingKey", "test.key");
    }

    @Test
    void shouldPublishExtractionRequest() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setOriginalFileName("test.pdf");
        invoice.setOriginalFilePath("/path/to/test.pdf");
        invoice.setOriginalFileType("application/pdf");
        invoice.setOriginalFileSize(1024);
        invoice.setCompanyId(UUID.randomUUID());
        invoice.setCreatedByUserId(UUID.randomUUID());

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String correlationId = producerService.publishExtractionRequest(invoice);

        // Then
        assertThat(correlationId).isNotNull();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.QUEUED);
        assertThat(invoice.getCorrelationId()).isEqualTo(correlationId);

        verify(invoiceRepository).save(invoice);

        ArgumentCaptor<ExtractionRequestMessage> captor = ArgumentCaptor.forClass(ExtractionRequestMessage.class);
        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("test.key"), captor.capture());

        ExtractionRequestMessage message = captor.getValue();
        assertThat(message.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(message.getCorrelationId()).isEqualTo(correlationId);
        assertThat(message.getAttempt()).isEqualTo(1);
    }

    @Test
    void shouldRepublishExtractionRequest() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCorrelationId(UUID.randomUUID().toString());
        invoice.setOriginalFileName("test.pdf");
        invoice.setOriginalFilePath("/path/to/test.pdf");
        invoice.setOriginalFileSize(1024);

        int newAttempt = 2;

        // When
        producerService.republishExtractionRequest(invoice, newAttempt);

        // Then
        ArgumentCaptor<ExtractionRequestMessage> captor = ArgumentCaptor.forClass(ExtractionRequestMessage.class);
        verify(rabbitTemplate).convertAndSend(eq("test.exchange"), eq("test.key"), captor.capture());

        ExtractionRequestMessage message = captor.getValue();
        assertThat(message.getInvoiceId()).isEqualTo(invoice.getId());
        assertThat(message.getCorrelationId()).isEqualTo(invoice.getCorrelationId());
        assertThat(message.getAttempt()).isEqualTo(newAttempt);
    }
}
