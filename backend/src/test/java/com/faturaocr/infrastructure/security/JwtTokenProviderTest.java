package com.faturaocr.infrastructure.security;

import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Email;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.infrastructure.common.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private JwtProperties jwtProperties;
    private JwtTokenProvider jwtTokenProvider;
    private String secret = "my-super-secret-key-that-is-at-least-32-chars-long";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(secret);

        JwtProperties.AccessToken accessToken = new JwtProperties.AccessToken();
        accessToken.setExpiration(3600000); // 1 hour
        jwtProperties.setAccessToken(accessToken);

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        // Arrange
        User user = User.builder()
                .id(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashedPwd")
                .fullName("Test User")
                .role(Role.ACCOUNTANT) // Assuming Role.USER exists or use Role.ACCOUNTANT based on previous files
                .isActive(true)
                .build();

        // Act
        String token = jwtTokenProvider.generateAccessToken(user);

        // Assert
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals(user.getId(), jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }
}
