package com.faturaocr.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        loginAttemptService = new LoginAttemptService(redisTemplate);

        org.springframework.test.util.ReflectionTestUtils.setField(loginAttemptService, "maxAttempts", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(loginAttemptService, "lockoutDurationMinutes", 15);
    }

    @Test
    void shouldBlockUserAfterMaxAttempts() {
        String key = "test@example.com";
        String redisKey = "login_attempts:" + key;

        // Simulate 4 previous attempts
        when(valueOperations.get(redisKey)).thenReturn("4");

        loginAttemptService.loginFailed(key);

        // Verify that the key is set to 5 with block duration
        verify(valueOperations).set(eq(redisKey), eq("5"), any(Duration.class));
    }

    @Test
    void shouldReturnTrueWhenBlocked() {
        String key = "blocked@example.com";
        String redisKey = "login_attempts:" + key;

        when(valueOperations.get(redisKey)).thenReturn("5");

        boolean isBlocked = loginAttemptService.isBlocked(key);

        assertTrue(isBlocked);
    }

    @Test
    void shouldReturnFalseWhenNotBlocked() {
        String key = "clean@example.com";
        String redisKey = "login_attempts:" + key;

        when(valueOperations.get(redisKey)).thenReturn("2");

        boolean isBlocked = loginAttemptService.isBlocked(key);

        assertFalse(isBlocked);
    }

    @Test
    void shouldResetOnSuccess() {
        String key = "success@example.com";
        String redisKey = "login_attempts:" + key;

        loginAttemptService.loginSucceeded(key);

        verify(redisTemplate).delete(redisKey);
    }
}
