package com.faturaocr.infrastructure.security;

import java.util.UUID;

/**
 * Thread-local holder for current company context.
 * Used for multi-tenant data isolation.
 *
 */
public class CompanyContextHolder {

    private static final ThreadLocal<UUID> currentCompanyId = new ThreadLocal<>();

    public static void setCompanyId(UUID companyId) {
        currentCompanyId.set(companyId);
    }

    public static UUID getCompanyId() {
        return currentCompanyId.get();
    }

    public static void clear() {
        currentCompanyId.remove();
    }

    public static boolean hasCompanyContext() {
        return currentCompanyId.get() != null;
    }
}
