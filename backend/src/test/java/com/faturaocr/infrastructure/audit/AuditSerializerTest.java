package com.faturaocr.infrastructure.audit;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditSerializerTest {

    private AuditSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new AuditSerializer();
    }

    @Test
    void shouldSerializeNullAsNull() {
        assertNull(serializer.serialize(null));
    }

    @Test
    void shouldSerializeUserWithoutSensitiveFields() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("secret-hash")
                .fullName("Test User")
                .role(Role.ACCOUNTANT)
                .isActive(true)
                .failedLoginAttempts(3)
                .build();

        String json = serializer.serialize(user);

        assertNotNull(json);
        // Should contain normal fields
        assertTrue(json.contains("fullName"));
        assertTrue(json.contains("Test User"));
        assertTrue(json.contains("ACCOUNTANT"));

        // Should NOT contain @AuditExclude fields
        assertFalse(json.contains("secret-hash"), "passwordHash should be excluded");
        assertFalse(json.contains("passwordHash"), "passwordHash field should be excluded");
        assertFalse(json.contains("failedLoginAttempts"), "failedLoginAttempts should be excluded");
    }

    @Test
    void shouldDeserializeToMap() {
        String json = "{\"name\":\"Test\",\"value\":123}";
        Map<String, Object> map = serializer.deserializeToMap(json);

        assertEquals("Test", map.get("name"));
        assertEquals(123, map.get("value"));
    }

    @Test
    void shouldReturnEmptyMapForNullInput() {
        Map<String, Object> map = serializer.deserializeToMap(null);
        assertTrue(map.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapForBlankInput() {
        Map<String, Object> map = serializer.deserializeToMap("");
        assertTrue(map.isEmpty());
    }

    @Test
    void shouldHandleInvalidJson() {
        Map<String, Object> map = serializer.deserializeToMap("not-json");
        assertTrue(map.isEmpty());
    }

    @Test
    void shouldTruncateVeryLargeJson() {
        // Create a large string > 10000 chars
        StringBuilder sb = new StringBuilder("{\"data\":\"");
        for (int i = 0; i < 11000; i++) {
            sb.append("x");
        }
        sb.append("\"}");
        String largeJson = sb.toString();

        // Create a simple serializable object that would produce large JSON
        Map<String, String> largeMap = Map.of("data", "x".repeat(11000));
        String result = serializer.serialize(largeMap);

        assertNotNull(result);
        assertTrue(result.length() <= 10020); // 10000 + "(truncated)"
    }
}
