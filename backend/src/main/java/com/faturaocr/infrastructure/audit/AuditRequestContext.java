package com.faturaocr.infrastructure.audit;

import java.util.UUID;

/**
 * ThreadLocal-based context holder for HTTP request information
 * needed by the audit aspect (IP address, user agent, request ID).
 */
public final class AuditRequestContext {

    private static final ThreadLocal<String> IP_ADDRESS = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private static final ThreadLocal<String> METADATA = new ThreadLocal<>();

    private AuditRequestContext() {
    }

    public static void setIpAddress(String ip) {
        IP_ADDRESS.set(ip);
    }

    public static String getIpAddress() {
        return IP_ADDRESS.get();
    }

    public static void setUserAgent(String ua) {
        USER_AGENT.set(ua);
    }

    public static String getUserAgent() {
        return USER_AGENT.get();
    }

    public static void setRequestId(String id) {
        REQUEST_ID.set(id);
    }

    public static String getRequestId() {
        String id = REQUEST_ID.get();
        if (id == null) {
            id = UUID.randomUUID().toString();
            REQUEST_ID.set(id);
        }
        return id;
    }

    public static void setMetadata(String metadata) {
        METADATA.set(metadata);
    }

    public static String getMetadata() {
        return METADATA.get();
    }

    public static void clear() {
        IP_ADDRESS.remove();
        USER_AGENT.remove();
        REQUEST_ID.remove();
        METADATA.remove();
    }
}
