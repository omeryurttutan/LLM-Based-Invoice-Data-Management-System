package com.faturaocr.security;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company testCompany;
    private String adminToken;
    private String managerToken;
    private String accountantToken;
    private String internToken;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("RBAC Test Company", "2222222222");

        testDataSeeder.seedUser(testCompany.getId(), "admin@rbac.com", "Password123!", Role.ADMIN);
        testDataSeeder.seedUser(testCompany.getId(), "manager@rbac.com", "Password123!", Role.MANAGER);
        testDataSeeder.seedUser(testCompany.getId(), "accountant@rbac.com", "Password123!", Role.ACCOUNTANT);
        testDataSeeder.seedUser(testCompany.getId(), "intern@rbac.com", "Password123!", Role.INTERN);

        adminToken = testDataSeeder.loginAndGetToken(mockMvc, "admin@rbac.com", "Password123!");
        managerToken = testDataSeeder.loginAndGetToken(mockMvc, "manager@rbac.com", "Password123!");
        accountantToken = testDataSeeder.loginAndGetToken(mockMvc, "accountant@rbac.com", "Password123!");
        internToken = testDataSeeder.loginAndGetToken(mockMvc, "intern@rbac.com", "Password123!");
    }

    @Test
    void getInvoices_ShouldBeAccessibleByAllRoles() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminUsers_ShouldOnlyBeAccessibleByAdmin() throws Exception {
        // ADMIN -> OK
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // MANAGER -> Forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // ACCOUNTANT -> Forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isForbidden());

        // INTERN -> Forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + internToken))
                .andExpect(status().isForbidden());
    }
}
