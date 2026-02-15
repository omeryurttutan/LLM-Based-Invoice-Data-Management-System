package com.faturaocr.security;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

class MultiTenantIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company companyA;
    private Company companyB;
    private String tokenA;
    private String tokenB;
    private Invoice invoiceA;
    private Invoice invoiceB;

    @BeforeEach
    void setUp() throws Exception {
        companyA = testDataSeeder.seedCompany("Company A", "1111111111");
        companyB = testDataSeeder.seedCompany("Company B", "2222222222");

        testDataSeeder.seedUser(companyA.getId(), "userA@compA.com", "Password123!", Role.ACCOUNTANT);
        testDataSeeder.seedUser(companyB.getId(), "userB@compB.com", "Password123!", Role.ACCOUNTANT);

        tokenA = testDataSeeder.loginAndGetToken(mockMvc, "userA@compA.com", "Password123!");
        tokenB = testDataSeeder.loginAndGetToken(mockMvc, "userB@compB.com", "Password123!");

        invoiceA = testDataSeeder.seedInvoice(companyA.getId(), "INV-A-001");
        invoiceB = testDataSeeder.seedInvoice(companyB.getId(), "INV-B-001");
    }

    @Test
    void getInvoices_ShouldOnlyReturnCompanyInvoices() throws Exception {
        // User A should see 1 invoice (invoiceA)
        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-A-001"));

        // User B should see 1 invoice (invoiceB)
        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].invoiceNumber").value("INV-B-001"));
    }

    @Test
    void getInvoiceById_ShouldReturnNotFoundOrForbidden_WhenAccessingOtherCompanyInvoice() throws Exception {
        // User A tries to access Invoice B -> Should be 404 (preferred for security) or
        // 403
        // Assuming current implementation returns 404 if not found in company scope
        mockMvc.perform(get("/api/v1/invoices/" + invoiceB.getId())
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
