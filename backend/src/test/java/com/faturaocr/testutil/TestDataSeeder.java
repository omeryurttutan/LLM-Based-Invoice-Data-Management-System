package com.faturaocr.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.auth.dto.AuthResponse;
import com.faturaocr.application.auth.dto.LoginCommand;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.company.port.CompanyRepository;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.port.UserRepository;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.interfaces.rest.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TestDataSeeder {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    public Company seedCompany(String name, String taxNumber) {
        Company company = TestDataBuilder.aCompany()
                .withName(name)
                .withTaxNumber(taxNumber)
                .withEmail("info@" + name.toLowerCase().replace(" ", "") + ".com")
                .build();
        return companyRepository.save(company);
    }

    public User seedUser(UUID companyId, String email, String password, Role role) {
        User user = TestDataBuilder.aUser()
                .withCompanyId(companyId)
                .withEmail(email)
                .withRole(role)
                .emailVerified(true)
                .withPasswordHash(passwordEncoder.encode(password))
                .build();

        return userRepository.save(user);
    }

    @Autowired
    private com.faturaocr.domain.invoice.port.InvoiceRepository invoiceRepository;

    public com.faturaocr.domain.invoice.entity.Invoice seedInvoice(UUID companyId, String invoiceNumber) {
        com.faturaocr.domain.invoice.entity.Invoice invoice = TestDataBuilder.anInvoice()
                .withCompanyId(companyId)
                .withInvoiceNumber(invoiceNumber)
                .withSupplierName("Supplier " + invoiceNumber)
                .build();
        return invoiceRepository.save(invoice);
    }

    public String loginAndGetToken(MockMvc mockMvc, String email, String password) throws Exception {
        LoginCommand loginCommand = new LoginCommand(email, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginCommand)))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        ApiResponse<AuthResponse> response = objectMapper.readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<ApiResponse<AuthResponse>>() {
                });

        return response.getData().accessToken();
    }
}
