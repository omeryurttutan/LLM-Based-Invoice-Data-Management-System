package com.faturaocr.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String correlationId = request.getHeader(LoggingConstants.CORRELATION_ID_HEADER);

            if (!StringUtils.hasText(correlationId)) {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put(LoggingConstants.CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(LoggingConstants.CORRELATION_ID_MDC_KEY);
            MDC.remove(LoggingConstants.USER_ID_MDC_KEY);
            MDC.remove(LoggingConstants.COMPANY_ID_MDC_KEY);
        }
    }
}
