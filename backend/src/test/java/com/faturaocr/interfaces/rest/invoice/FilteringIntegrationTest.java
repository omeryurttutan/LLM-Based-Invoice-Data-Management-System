package com.faturaocr.interfaces.rest.invoice;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.valueobject.Currency;
import com.faturaocr.domain.invoice.valueobject.InvoiceStatus;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FilteringIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    @Autowired
    private com.faturaocr.domain.invoice.port.InvoiceRepository invoiceRepository;

    private Company testCompany;
    private String authToken;

    private Invoice inv1;
    private Invoice inv2;
    private Invoice inv3;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("Filter Test Company", "4444444444");
        testDataSeeder.seedUser(testCompany.getId(), "filteruser@test.com", "Password123!", Role.MANAGER);
        authToken = testDataSeeder.loginAndGetToken(mockMvc, "filteruser@test.com", "Password123!");

        // Seed Data Manually to control attributes
        inv1 = testDataSeeder.seedInvoice(testCompany.getId(), "INV-F-1");
        inv1.setSupplierName("Supplier A");
        inv1.setStatus(InvoiceStatus.PENDING);
        inv1.setTotalAmount(BigDecimal.valueOf(100));
        inv1.setCurrency(Currency.TRY);
        inv1.setInvoiceDate(LocalDate.now());
        invoiceRepository.save(inv1);

        inv2 = testDataSeeder.seedInvoice(testCompany.getId(), "INV-F-2");
        inv2.setSupplierName("Supplier B");
        inv2.setStatus(InvoiceStatus.VERIFIED);
        inv2.setTotalAmount(BigDecimal.valueOf(200));
        inv2.setCurrency(Currency.USD);
        inv2.setInvoiceDate(LocalDate.now().minusDays(1));
        invoiceRepository.save(inv2);

        inv3 = testDataSeeder.seedInvoice(testCompany.getId(), "INV-F-3");
        inv3.setSupplierName("Supplier A");
        inv3.setStatus(InvoiceStatus.REJECTED);
        inv3.setTotalAmount(BigDecimal.valueOf(300));
        inv3.setCurrency(Currency.TRY);
        inv3.setInvoiceDate(LocalDate.now().minusDays(2));
        invoiceRepository.save(inv3);
    }

    @Test
    void filterByStatus_ShouldReturnMatchingInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")
                .param("status", "PENDING")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-F-1"));

        mockMvc.perform(get("/api/v1/invoices")
                .param("status", "VERIFIED")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-F-2"));
    }

    @Test
    void filterBySupplier_ShouldReturnMatchingInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")
                .param("supplierName", "Supplier A")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2))); // INV-1 and INV-3
    }

    @Test
    void filterByAmountRange_ShouldReturnMatchingInvoices() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")
                .param("minAmount", "150")
                .param("maxAmount", "350")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2))); // INV-2 (200) and INV-3 (300)
    }

    @Test
    void filterByDateRange_ShouldReturnMatchingInvoices() throws Exception {
        // From yesterday to today
        String fromDate = LocalDate.now().minusDays(1).toString();
        String toDate = LocalDate.now().toString();

        mockMvc.perform(get("/api/v1/invoices")
                .param("startDate", fromDate)
                .param("endDate", toDate)
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2))); // INV-1 and INV-2
    }

    @Test
    void combinedFilter_ShouldReturnIntersection() throws Exception {
        // Supplier A AND Amount < 200 -> Only INV-1 (100)
        // INV-3 is Supplier A but Amount is 300
        mockMvc.perform(get("/api/v1/invoices")
                .param("supplierName", "Supplier A")
                .param("maxAmount", "200")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-F-1"));
    }
}
