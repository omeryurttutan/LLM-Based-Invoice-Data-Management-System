package com.faturaocr.infrastructure.persistence.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.company.entity.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataMaskingServiceTest {

    private DataMaskingService dataMaskingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dataMaskingService = new DataMaskingService(objectMapper);
        dataMaskingService.init(); // Initialize rules
    }

    @Test
    void testMaskUser() throws Exception {
        // User has email (EMAIL) and phone (PHONE) and fullName (PARTIAL)
        String json = "{\"email\": \"test@example.com\", \"phone\": \"+905551234567\", \"fullName\": \"John Doe\"}";

        // Since User class simple name is "User", pass "User"
        String masked = dataMaskingService.mask("User", json);

        System.out.println("Masked User: " + masked);

        // Check masking
        assertTrue(masked.contains("\"email\":\"t***@example.com\"") || masked.contains("\"email\":\"t***@e"));
        // EMAIL logic: char(0) + *** + substring(@)
        // t***@example.com

        assertTrue(masked.contains("\"phone\":\"****67\""));
        // PHONE logic: **** + last 2

        assertTrue(masked.contains("\"fullName\":\"Jo****oe\""));
        // PARTIAL logic: first 2 + **** + last 2
    }

    @Test
    void testMaskCompany() throws Exception {
        // Company has taxNumber (PARTIAL), address (FULL)
        String json = "{\"taxNumber\": \"1234567890\", \"address\": \"Some Address 123\"}";

        String masked = dataMaskingService.mask("Company", json);

        System.out.println("Masked Company: " + masked);

        assertTrue(masked.contains("\"taxNumber\":\"12****90\""));
        assertTrue(masked.contains("\"address\":\"[MASKED]\""));
    }

    @Test
    void testMaskUnknownEntity() {
        String json = "{\"secret\": \"value\"}";
        String masked = dataMaskingService.mask("UnknownEntity", json);
        assertEquals(json, masked);
    }
}
