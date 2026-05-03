package com.faturaocr.infrastructure.persistence.audit;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.audit.valueobject.AuditActionType;
import com.faturaocr.domain.company.entity.Company;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLogIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditLogJpaRepository auditLogJpaRepository;

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company testCompany;
    private String authToken;
    private java.util.UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("Audit Test Company", "7777777777");
        // We need fields. user id?
        // seeder returns User domain object.
        com.faturaocr.domain.user.entity.User user = testDataSeeder.seedUser(testCompany.getId(), "audituser@test.com",
                "Password123!", Role.ACCOUNTANT);
        userId = user.getId();
        authToken = testDataSeeder.loginAndGetToken(mockMvc, "audituser@test.com", "Password123!");
    }

    @Test
    void createInvoice_ShouldCreateAuditLog() throws Exception {
        long initialCount = auditLogJpaRepository.count();

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setInvoiceNumber("INV-AUDIT-1");
        request.setInvoiceDate(LocalDate.now());
        request.setSupplierName("Audit Supplier");
        request.setCurrency("TRY");
        request.setExchangeRate(BigDecimal.ONE);

        CreateInvoiceRequest.CreateInvoiceItemRequest item = new CreateInvoiceRequest.CreateInvoiceItemRequest();
        item.setDescription("Item");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(BigDecimal.TEN);
        item.setTaxRate(BigDecimal.ZERO);
        request.setItems(Collections.singletonList(item));

        mockMvc.perform(post("/api/v1/invoices")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify Audit Log
        List<AuditLogJpaEntity> logs = auditLogJpaRepository.findAll();
        assertThat(logs.size()).isGreaterThan((int) initialCount);

        // Find the log for CREATE
        // Assuming ActionType enum exists and is used.
        // It might be difficult to filter exactly without waiting if async,
        // but let's assume sync or fast enough.

        boolean hasCreateLog = logs.stream()
                .anyMatch(log -> "CREATE".equals(log.getActionType().name())
                        || log.getActionType() == AuditActionType.CREATE);

        assertThat(hasCreateLog).isTrue();
    }
}
