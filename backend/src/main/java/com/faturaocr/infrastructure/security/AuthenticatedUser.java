package com.faturaocr.infrastructure.security;

import java.util.UUID;

/**
 * Authenticated user principal.
 */
public record AuthenticatedUser(
        UUID userId,
        String email,
        UUID companyId,
        java.util.List<String> accessibleCompanyIds,
        String role) {

    public AuthenticatedUser(UUID userId, String email, UUID companyId, String role) {
        this(userId, email, companyId, java.util.List.of(), role);
    }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(role);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role) || isSuperAdmin();
    }

    public boolean isManager() {
        return "MANAGER".equals(role) || isAdmin();
    }
}
