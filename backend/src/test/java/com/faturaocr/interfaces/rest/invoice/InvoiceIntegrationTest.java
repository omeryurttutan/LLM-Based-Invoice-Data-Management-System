package com.faturaocr.interfaces.rest.invoice;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.interfaces.rest.invoice.dto.CreateInvoiceRequest;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InvoiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company testCompany;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("Invoice Test Company", "3333333333");
        testDataSeeder.seedUser(testCompany.getId(), "invoiceuser@test.com", "Password123!", Role.ACCOUNTANT);
        authToken = testDataSeeder.loginAndGetToken(mockMvc, "invoiceuser@test.com", "Password123!");
    }

    @Test
    void createInvoice_ShouldReturnCreated_WhenValidRequest() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNumber("INV-NEW-001");
        request.setInvoiceDate(LocalDate.now());
        request.setSupplierName("New Supplier");
        request.setCurrency("TRY");
        request.setExchangeRate(BigDecimal.ONE);
        request.setExchangeRate(BigDecimal.ONE);
        // request.setTotalAmount(BigDecimal.valueOf(118.0)); // Calculated by backend

        CreateInvoiceRequest.CreateInvoiceItemRequest item = new CreateInvoiceRequest.CreateInvoiceItemRequest();
        item.setDescription("Test Item");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(BigDecimal.valueOf(100.0));
        item.setTaxRate(BigDecimal.valueOf(18.0));
        request.setItems(Collections.singletonList(item));

        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.message").value("Invoice created successfully"));
    }

    @Test
    void createInvoice_ShouldReturnConflict_WhenDuplicateExists() throws Exception {
        // Seed an invoice first
        testDataSeeder.seedInvoice(testCompany.getId(), "INV-DUP-CHECK");

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNumber("INV-DUP-CHECK");
        request.setInvoiceDate(LocalDate.now());
        request.setSupplierName("Supplier INV-DUP-CHECK");
        request.setCurrency("TRY");
        request.setItems(Collections.emptyList()); // Simplified for dup check

        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createInvoice_ShouldSucceed_WhenForceDuplicateIsTrue() throws Exception {
        // Seed an invoice first
        testDataSeeder.seedInvoice(testCompany.getId(), "INV-FORCE-DUP");

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNumber("INV-FORCE-DUP");
        request.setInvoiceDate(LocalDate.now());
        request.setSupplierName("Supplier INV-FORCE-DUP");
        request.setCurrency("TRY");
        request.setExchangeRate(BigDecimal.ONE);

        CreateInvoiceRequest.CreateInvoiceItemRequest item = new CreateInvoiceRequest.CreateInvoiceItemRequest();
        item.setDescription("Item");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(BigDecimal.TEN);
        item.setTaxRate(BigDecimal.ZERO);
        request.setItems(Collections.singletonList(item));

        mockMvc.perform(post("/api/v1/invoices?forceDuplicate=true")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void getInvoices_ShouldReturnList() throws Exception {
        testDataSeeder.seedInvoice(testCompany.getId(), "INV-LIST-1");
        testDataSeeder.seedInvoice(testCompany.getId(), "INV-LIST-2");

        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void getInvoiceById_ShouldReturnDetails() throws Exception {
        Invoice invoice = testDataSeeder.seedInvoice(testCompany.getId(), "INV-DETAIL");

        mockMvc.perform(get("/api/v1/invoices/" + invoice.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-DETAIL"));
    }

    @Test
    void updateInvoice_ShouldReturnOk() throws Exception {
        Invoice invoice = testDataSeeder.seedInvoice(testCompany.getId(), "INV-UPDATE");

        CreateInvoiceRequest updateRequest = new CreateInvoiceRequest();
        updateRequest.setInvoiceNumber("INV-UPDATED");
        updateRequest.setInvoiceDate(LocalDate.now());
        updateRequest.setSupplierName("Updated Supplier");
        updateRequest.setCurrency("USD");
        // ... items if required by validation
        CreateInvoiceRequest.CreateInvoiceItemRequest item = new CreateInvoiceRequest.CreateInvoiceItemRequest();
        item.setDescription("Updated Item");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(BigDecimal.valueOf(200.0));
        item.setTaxRate(BigDecimal.valueOf(18.0));
        updateRequest.setItems(Collections.singletonList(item));

        mockMvc.perform(put("/api/v1/invoices/" + invoice.getId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-UPDATED"));
    }

    @Test
    void deleteInvoice_ShouldSoftDelete() throws Exception {
        Invoice invoice = testDataSeeder.seedInvoice(testCompany.getId(), "INV-DELETE");

        mockMvc.perform(delete("/api/v1/invoices/" + invoice.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        // Verify it's gone from list/get
        mockMvc.perform(get("/api/v1/invoices/" + invoice.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyInvoice_ShouldChangeStatus() throws Exception {
        Invoice invoice = testDataSeeder.seedInvoice(testCompany.getId(), "INV-VERIFY");

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/verify")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }
}
