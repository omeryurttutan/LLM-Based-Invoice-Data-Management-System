package com.faturaocr.infrastructure.logging;

public class LoggingConstants {
    public static final String CORRELATION_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String COMPANY_ID_MDC_KEY = "companyId";

    private LoggingConstants() {
        // Private constructor to prevent instantiation
    }
}
