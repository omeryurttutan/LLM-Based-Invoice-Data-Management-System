package com.faturaocr.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;

import static org.mockito.Mockito.*;

class RequestSizeFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private PrintWriter writer;

    private RequestSizeFilter requestSizeFilter;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        requestSizeFilter = new RequestSizeFilter(objectMapper);
        ReflectionTestUtils.setField(requestSizeFilter, "maxJsonBodySize", 100L); // Small limit for testing
        when(response.getWriter()).thenReturn(writer);
    }

    @Test
    void shouldAllowSmallJsonRequest() throws Exception {
        when(request.getContentType()).thenReturn("application/json");
        when(request.getContentLengthLong()).thenReturn(50L);

        requestSizeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void shouldBlockLargeJsonRequest() throws Exception {
        when(request.getContentType()).thenReturn("application/json");
        when(request.getContentLengthLong()).thenReturn(150L); // > 100

        requestSizeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(413); // PAYLOAD_TOO_LARGE
    }

    @Test
    void shouldAllowLargeNonJsonRequest() throws Exception {
        when(request.getContentType()).thenReturn("multipart/form-data");
        when(request.getContentLengthLong()).thenReturn(150L); // > 100 but ignored

        requestSizeFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
