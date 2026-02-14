package com.faturaocr.interfaces.rest.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.company.CompanyService;
import com.faturaocr.application.company.dto.CompanyResponse;
import com.faturaocr.application.company.dto.CreateCompanyCommand;
import com.faturaocr.application.company.dto.UpdateCompanyCommand;
import com.faturaocr.application.user.UserManagementService;
import com.faturaocr.interfaces.rest.company.dto.CreateCompanyRequest;
import com.faturaocr.interfaces.rest.company.dto.UpdateCompanyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompanyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private UserManagementService userManagementService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCompany_WhenAdmin_ReturnsCreated() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest();
        request.setName("Test Company");
        request.setTaxNumber("1234567890");
        request.setEmail("test@company.com");

        CompanyResponse response = CompanyResponse.builder()
                .id(UUID.randomUUID())
                .name("Test Company")
                .taxNumber("1234567890")
                .email("test@company.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(companyService.createCompany(any(CreateCompanyCommand.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Company"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createCompany_WhenManager_ReturnsForbidden() throws Exception {
        CreateCompanyRequest request = new CreateCompanyRequest();
        request.setName("Test Company");

        mockMvc.perform(post("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCompanyById_WhenAdmin_ReturnsCompany() throws Exception {
        UUID id = UUID.randomUUID();
        CompanyResponse response = CompanyResponse.builder()
                .id(id)
                .name("Test Company")
                .build();

        when(companyService.getCompanyById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/companies/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCompany_WhenAdmin_ReturnsUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateCompanyRequest request = new UpdateCompanyRequest();
        request.setName("Updated Name");

        CompanyResponse response = CompanyResponse.builder()
                .id(id)
                .name("Updated Name")
                .build();

        when(companyService.updateCompany(eq(id), any(UpdateCompanyCommand.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/companies/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCompany_WhenAdmin_ReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/companies/{id}", id))
                .andExpect(status().isNoContent());
    }
}
