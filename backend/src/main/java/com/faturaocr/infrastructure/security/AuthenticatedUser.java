package com.faturaocr.infrastructure.security;

import java.util.UUID;

/**
 * Authenticated user principal.
 */
public record AuthenticatedUser(
        UUID userId,
        String email,
        UUID companyId,
        String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isManager() {
        return "MANAGER".equals(role) || isAdmin();
    }
}
