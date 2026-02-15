package com.faturaocr.security;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.application.auth.dto.LoginCommand;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        // Override rate limits to be very low for testing
        "faturaocr.security.rate-limit.enabled=true",
        "faturaocr.security.rate-limit.capacity=5",
        "faturaocr.security.rate-limit.refill-tokens=5",
        "faturaocr.security.rate-limit.refill-duration=1m"
})
class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company testCompany;
    private String authToken;
    private String email;

    @BeforeEach
    void setUp() throws Exception {
        testCompany = testDataSeeder.seedCompany("Rate Limit Company", "6666666666");
        email = "ratelimit@test.com";
        testDataSeeder.seedUser(testCompany.getId(), email, "Password123!", Role.INTERN);
        authToken = testDataSeeder.loginAndGetToken(mockMvc, email, "Password123!");
    }

    @Test
    void shouldReturn429_WhenRateLimitExceeded() throws Exception {
        // Send 5 requests - should be OK
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/health") // Assuming health or any public/protected endpoint is limited
                    .header("Authorization", "Bearer " + authToken))
                    .andExpect(status().isOk());
        }

        // Send 6th request - should be 429
        // Note: Check if health endpoint is rate limited. Usually generic filter
        // applies to /api/**
        // Let's use /api/v1/invoices just to be safe it's a business endpoint
        mockMvc.perform(get("/api/v1/invoices")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("X-Rate-Limit-Retry-After-Seconds"));
    }

    @Test
    void shouldLockAccount_AfterFailedLogins() throws Exception {
        String userEmail = "lockout@test.com";
        testDataSeeder.seedUser(testCompany.getId(), userEmail, "Password123!", Role.MANAGER);

        LoginCommand badCreds = new LoginCommand(userEmail, "WrongPass");

        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(badCreds)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt - should be Locked (423) or still 401 but with valid creds it
        // should fail?
        // Usually lockout happens AT the 5th or after.
        // Let's try with VALID credentials now.

        LoginCommand goodCreds = new LoginCommand(userEmail, "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(goodCreds)))
                .andExpect(status().isLocked()); // 423 Locked
    }
}
