package com.faturaocr.interfaces.rest.export;

import com.faturaocr.application.export.ExportFormat;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountingExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceJpaRepository invoiceRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();

        // Create a verified invoice for accounting export
        InvoiceJpaEntity invoice = new InvoiceJpaEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("GIB2024000000001");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setSupplierName("Test Tedarikçi");
        invoice.setSupplierTaxNumber("1234567890");
        invoice.setTotalAmount(new BigDecimal("118.00"));
        invoice.setCurrency(Currency.TRY);
        invoice.setStatus(InvoiceStatus.VERIFIED);
        invoice.setCompanyId(UUID.randomUUID()); // Ensure companyId is set
        invoice.setSourceType(com.faturaocr.domain.invoice.valueobject.SourceType.MANUAL); // Set required fields
        invoiceRepository.save(invoice);
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTANT")
    void shouldExportLogoXml() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "LOGO")
                .param("includeItems", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"faturalar_LOGO.xml\""))
                .andExpect(header().string("Content-Type", "application/xml"));
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTANT")
    void shouldExportNetsisXml() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "NETSIS")
                .param("includeItems", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"faturalar_NETSIS.xml\""))
                .andExpect(header().string("Content-Type", "application/xml"));
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTANT")
    void shouldExportMikroTxt() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "MIKRO")
                .param("includeItems", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"faturalar_MIKRO.txt\""))
                .andExpect(header().string("Content-Type", "text/plain;charset=windows-1254"));
    }

    @Test
    @WithMockUser(authorities = "ACCOUNTANT")
    void shouldExportLucaXlsx() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "LUCA")
                .param("includeItems", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"faturalar_LUCA.xlsx\""))
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
