package com.faturaocr.interfaces.rest.invoice;

import com.faturaocr.application.invoice.InvoiceService;
import com.faturaocr.application.invoice.dto.InvoiceListResponse;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceFilteringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        // Mock Company Context
        CompanyContextHolder.setCompanyId(UUID.randomUUID());
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void listInvoices_ShouldReturnPageOfInvoices_WhenNoFilterProvided() throws Exception {
        InvoiceListResponse invoice = new InvoiceListResponse();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-001");
        invoice.setSupplierName("Supplier A");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setStatus(InvoiceStatus.VERIFIED);

        Page<InvoiceListResponse> page = new PageImpl<>(List.of(invoice));

        when(invoiceService.listInvoices(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-001"));
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void listInvoices_ShouldReturnFilteredInvoices_WhenFilterProvided() throws Exception {
        InvoiceListResponse invoice = new InvoiceListResponse();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-002");
        invoice.setSupplierName("Supplier B");
        invoice.setInvoiceDate(LocalDate.now().minusDays(1));
        invoice.setStatus(InvoiceStatus.PENDING);

        Page<InvoiceListResponse> page = new PageImpl<>(List.of(invoice));

        when(invoiceService.listInvoices(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/invoices")
                .param("supplierName", "Supplier B")
                .param("status", "PENDING")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("INV-002"));
    }

    @Test
    @WithMockUser(authorities = "MANAGER")
    void getSuppliers_ShouldReturnListOfSuppliers() throws Exception {
        when(invoiceService.getSuppliers(any())).thenReturn(List.of("Supplier A", "Supplier B"));

        mockMvc.perform(get("/api/v1/invoices/suppliers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Supplier A"))
                .andExpect(jsonPath("$[1]").value("Supplier B"));
    }
}
