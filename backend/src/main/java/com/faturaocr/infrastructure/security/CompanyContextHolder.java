package com.faturaocr.infrastructure.security;

import java.util.UUID;

/**
 * Thread-local holder for current company context.
 * Used for multi-tenant data isolation.
 *
 */
public class CompanyContextHolder {

    private static final ThreadLocal<UUID> CURRENT_COMPANY_ID = new ThreadLocal<>();

    public static void setCompanyId(UUID companyId) {
        CURRENT_COMPANY_ID.set(companyId);
    }

    public static UUID getCompanyId() {
        return CURRENT_COMPANY_ID.get();
    }

    public static void clear() {
        CURRENT_COMPANY_ID.remove();
    }

    public static boolean hasCompanyContext() {
        return CURRENT_COMPANY_ID.get() != null;
    }
}
