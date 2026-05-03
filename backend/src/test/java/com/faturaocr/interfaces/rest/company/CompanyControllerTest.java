package com.faturaocr.interfaces.rest.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.company.dto.CreateCompanyCommand;
import com.faturaocr.application.company.dto.UpdateCompanyCommand;
import com.faturaocr.application.company.dto.CompanyResponse;
import com.faturaocr.application.company.CompanyService;
import com.faturaocr.interfaces.rest.company.dto.CreateCompanyRequest;
import com.faturaocr.interfaces.rest.company.dto.UpdateCompanyRequest;
import com.faturaocr.infrastructure.security.JwtTokenProvider;
import com.faturaocr.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = CompanyController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller test if acceptable, or keep
                                          // them and mock
// Actually, to test @PreAuthorize, we need security.
// But setting up security in WebMvcTest is complex with custom filters.
// For now, let's try with filters disabled to verify mapping logic,
// or if we want to test security, we need to import security config.
public class CompanyControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CompanyService companyService;

        @MockitoBean
        private com.faturaocr.application.user.UserManagementService userManagementService;

        // We might need to mock Jwt components if security is active
        @MockitoBean
        private JwtTokenProvider jwtTokenProvider;

        @MockitoBean
        private UserDetailsService userDetailsService; // Often required by security config

        @Test
        @WithMockUser(roles = "ADMIN")
        public void testCreateCompany_Success() throws Exception {
                CreateCompanyRequest request = new CreateCompanyRequest();
                request.setName("Test Company");
                request.setTaxNumber("1234567890");
                request.setEmail("test@company.com");

                CompanyResponse response = CompanyResponse.builder()
                                .id(UUID.randomUUID())
                                .name("Test Company")
                                .isActive(true)
                                .build();

                Mockito.when(companyService.createCompany(any(CreateCompanyCommand.class))).thenReturn(response);

                mockMvc.perform(post("/api/v1/companies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf())) // CSRF might be required if enabled
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.name").value("Test Company"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        public void testGetCompanyById_Success() throws Exception {
                UUID id = UUID.randomUUID();
                CompanyResponse response = CompanyResponse.builder()
                                .id(id)
                                .name("Test Company")
                                .build();

                Mockito.when(companyService.getCompanyById(id)).thenReturn(response);

                mockMvc.perform(get("/api/v1/companies/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(id.toString()));
        }
}
