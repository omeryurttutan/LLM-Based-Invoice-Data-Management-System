package com.faturaocr.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class RequestSizeFilter extends OncePerRequestFilter {

    @Value("${app.security.request.max-json-body-size:1048576}")
    private long maxJsonBodySize;

    private final ObjectMapper objectMapper;

    public RequestSizeFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isJsonRequest(request)) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > maxJsonBodySize) {
                handleSizeExceeded(response, contentLength);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON);
    }

    private void handleSizeExceeded(HttpServletResponse response, long actualSize) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("error", "PAYLOAD_TOO_LARGE");
        error.put("message", "İstek boyutu çok büyük. Maksimum izin verilen boyut: " + maxJsonBodySize + " bytes.");
        error.put("actualSize", actualSize);
        error.put("maxSize", maxJsonBodySize);

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
