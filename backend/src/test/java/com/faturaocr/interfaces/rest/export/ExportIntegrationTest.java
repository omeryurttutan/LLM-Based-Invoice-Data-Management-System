package com.faturaocr.interfaces.rest.export;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company testCompany;
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("Export Test Company", "5555555555");
        testDataSeeder.seedUser(testCompany.getId(), "exportuser@test.com", "Password123!", Role.ACCOUNTANT);
        authToken = testDataSeeder.loginAndGetToken(mockMvc, "exportuser@test.com", "Password123!");

        testDataSeeder.seedInvoice(testCompany.getId(), "INV-EXP-1");
        testDataSeeder.seedInvoice(testCompany.getId(), "INV-EXP-2");
    }

    @Test
    void exportXlsx_ShouldReturnExcelFile() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "XLSX")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        // Can inspect content if needed, but checking content-type and 200 is good for
        // integration
    }

    @Test
    void exportCsv_ShouldReturnCsvFile() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "CSV")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"));
    }

    @Test
    void exportLogo_ShouldReturnXmlFile() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/export")
                .param("format", "LOGO")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/xml"));
    }
}
