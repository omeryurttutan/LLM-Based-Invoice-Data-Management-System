package com.faturaocr.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.io.PrintWriter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private PrintWriter writer;

    private RateLimitFilter rateLimitFilter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(response.getWriter()).thenReturn(writer);

        rateLimitFilter = new RateLimitFilter(redisTemplate, objectMapper);

        org.springframework.test.util.ReflectionTestUtils.setField(rateLimitFilter, "enabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(rateLimitFilter, "publicLimit", 20);
        org.springframework.test.util.ReflectionTestUtils.setField(rateLimitFilter, "loginLimit", 5);
        org.springframework.test.util.ReflectionTestUtils.setField(rateLimitFilter, "windowSeconds", 60);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/public");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Mock redis count to return 5
        when(zSetOperations.zCard(anyString())).thenReturn(5L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void shouldBlockRequestWhenOverLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/public");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Mock redis count to return 25 (limit is 20)
        when(zSetOperations.zCard(anyString())).thenReturn(25L);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(429);
    }
}
