package com.faturaocr.interfaces.rest.invoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.invoice.InvoiceService;
import com.faturaocr.application.invoice.dto.InvoiceResponse;
import com.faturaocr.interfaces.rest.invoice.dto.CreateInvoiceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class InvoiceControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private InvoiceService invoiceService;

        @Test
        @WithMockUser(authorities = "ADMIN")
        void createInvoice_ShouldReturnOk_WhenValidRequest() throws Exception {
                CreateInvoiceRequest request = new CreateInvoiceRequest();
                request.setInvoiceNumber("INV-001");
                request.setInvoiceDate(LocalDate.now());
                request.setSupplierName("Test Supplier");
                request.setCurrency("TRY");
                request.setExchangeRate(BigDecimal.ONE);

                CreateInvoiceRequest.CreateInvoiceItemRequest item = new CreateInvoiceRequest.CreateInvoiceItemRequest();
                item.setDescription("Test Item");
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(BigDecimal.TEN);
                item.setTaxRate(BigDecimal.valueOf(18));
                request.setItems(Collections.singletonList(item));

                InvoiceResponse response = InvoiceResponse.builder()
                                .id(UUID.randomUUID())
                                .message("Invoice created successfully")
                                .build();

                when(invoiceService.createInvoice(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                                .thenReturn(response);

                mockMvc.perform(post("/api/v1/invoices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        void createInvoice_ShouldReturnConflict_WhenDuplicateExists() throws Exception {
                CreateInvoiceRequest request = new CreateInvoiceRequest();
                request.setInvoiceNumber("INV-DUP");
                request.setInvoiceDate(LocalDate.now());
                request.setSupplierName("Duplicate Supplier");
                request.setCurrency("TRY");

                CreateInvoiceRequest.CreateInvoiceItemRequest item = new CreateInvoiceRequest.CreateInvoiceItemRequest();
                item.setDescription("Test Item");
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(BigDecimal.TEN);
                item.setTaxRate(BigDecimal.valueOf(18));
                request.setItems(Collections.singletonList(item));

                // Mock service to throw DuplicateInvoiceException
                com.faturaocr.application.invoice.dto.DuplicateCheckResult result = com.faturaocr.application.invoice.dto.DuplicateCheckResult
                                .builder()
                                .hasDuplicates(true)
                                .highestConfidence(com.faturaocr.domain.invoice.valueobject.DuplicateConfidence.HIGH)
                                .duplicates(Collections.singletonList(
                                                com.faturaocr.application.invoice.dto.DuplicateMatch.builder()
                                                                .invoiceNumber("INV-DUP")
                                                                .confidence(com.faturaocr.domain.invoice.valueobject.DuplicateConfidence.HIGH)
                                                                .build()))
                                .build();

                when(invoiceService.createInvoice(any(), org.mockito.ArgumentMatchers.eq(false)))
                                .thenThrow(new com.faturaocr.application.common.exception.DuplicateInvoiceException(
                                                result));

                mockMvc.perform(post("/api/v1/invoices")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        void createInvoice_ShouldReturnCreated_WhenForceDuplicateIsTrue() throws Exception {
                CreateInvoiceRequest request = new CreateInvoiceRequest();
                request.setInvoiceNumber("INV-DUP");
                request.setInvoiceDate(LocalDate.now());
                request.setSupplierName("Duplicate Supplier");
                request.setCurrency("TRY");

                CreateInvoiceRequest.CreateInvoiceItemRequest item = new CreateInvoiceRequest.CreateInvoiceItemRequest();
                item.setDescription("Test Item");
                item.setQuantity(BigDecimal.ONE);
                item.setUnitPrice(BigDecimal.TEN);
                item.setTaxRate(BigDecimal.valueOf(18));
                request.setItems(Collections.singletonList(item));

                InvoiceResponse response = InvoiceResponse.builder()
                                .id(UUID.randomUUID())
                                .message("Invoice created successfully")
                                .build();

                when(invoiceService.createInvoice(any(), org.mockito.ArgumentMatchers.eq(true)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/v1/invoices?forceDuplicate=true")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(authorities = "ADMIN")
        void checkDuplicate_ShouldReturnOk_WithResult() throws Exception {
                com.faturaocr.interfaces.rest.invoice.dto.DuplicateCheckRequestDTO request = new com.faturaocr.interfaces.rest.invoice.dto.DuplicateCheckRequestDTO();
                request.setInvoiceNumber("INV-CHECK");

                com.faturaocr.application.invoice.dto.DuplicateCheckResult result = com.faturaocr.application.invoice.dto.DuplicateCheckResult
                                .builder()
                                .hasDuplicates(true)
                                .highestConfidence(com.faturaocr.domain.invoice.valueobject.DuplicateConfidence.HIGH)
                                .duplicates(Collections.emptyList())
                                .build();

                when(invoiceService.checkForDuplicates(any())).thenReturn(result);

                mockMvc.perform(post("/api/v1/invoices/check-duplicate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }
}
