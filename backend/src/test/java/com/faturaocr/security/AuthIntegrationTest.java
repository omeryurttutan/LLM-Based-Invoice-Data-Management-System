package com.faturaocr.security;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.application.auth.dto.LoginCommand;
import com.faturaocr.application.auth.dto.RefreshTokenCommand;
import com.faturaocr.application.auth.dto.RegisterCommand;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company testCompany;

    @BeforeEach
    void setUp() {
        testCompany = testDataSeeder.seedCompany("Auth Test Company", "1111111111");
    }

    @Test
    void register_ShouldReturnCreated_WhenValidRequest() throws Exception {
        RegisterCommand command = new RegisterCommand(
                testCompany.getId(),
                "newuser@example.com",
                "Password123!",
                "New User",
                "5551234567");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"));
    }

    @Test
    void register_ShouldReturnConflict_WhenEmailExists() throws Exception {
        // Seed user first
        testDataSeeder.seedUser(testCompany.getId(), "existing@example.com", "Password123!", Role.ACCOUNTANT);

        RegisterCommand command = new RegisterCommand(
                testCompany.getId(),
                "existing@example.com",
                "Password123!",
                "New User",
                "5551234567");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAreValid() throws Exception {
        testDataSeeder.seedUser(testCompany.getId(), "loginuser@example.com", "Password123!", Role.MANAGER);

        LoginCommand command = new LoginCommand("loginuser@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenPasswordIsInvalid() throws Exception {
        testDataSeeder.seedUser(testCompany.getId(), "wrongpass@example.com", "Password123!", Role.MANAGER);

        LoginCommand command = new LoginCommand("wrongpass@example.com", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_ShouldReturnNewAccessToken_WhenRefreshTokenIsValid() throws Exception {
        User user = testDataSeeder.seedUser(testCompany.getId(), "refresh@example.com", "Password123!", Role.MANAGER);

        // Login first to get refresh token
        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginCommand("refresh@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = com.jayway.jsonpath.JsonPath.read(loginJson, "$.data.refreshToken");

        RefreshTokenCommand command = new RefreshTokenCommand(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void logout_ShouldInvalidateRefreshToken() throws Exception {
        testDataSeeder.seedUser(testCompany.getId(), "logout@example.com", "Password123!", Role.MANAGER);

        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginCommand("logout@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = com.jayway.jsonpath.JsonPath.read(loginJson, "$.data.refreshToken");

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenCommand(refreshToken))))
                .andExpect(status().isOk());

        // Try refresh again - should fail
        // Note: exact status code might depend on implementation (401 or 403 or 400).
        // Assuming 401 for invalid/revoked token.
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenCommand(refreshToken))))
                .andExpect(status().isUnauthorized());
    }
}
