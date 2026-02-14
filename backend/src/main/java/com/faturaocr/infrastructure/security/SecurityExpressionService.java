package com.faturaocr.infrastructure.security;

import com.faturaocr.domain.user.valueobject.Permission;
import com.faturaocr.domain.user.valueobject.Role;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for custom security expressions in @PreAuthorize.
 */
@Service("securityService")
public class SecurityExpressionService {

    /**
     * Check if current user has a specific permission.
     */
    public boolean hasPermission(String permission) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null)
            return false;

        try {
            Permission perm = Permission.valueOf(permission);
            Role role = Role.valueOf(user.role());
            return role.hasPermission(perm);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if current user has any of the specified permissions.
     */
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if current user has all of the specified permissions.
     */
    public boolean hasAllPermissions(String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if current user belongs to the specified company.
     */
    public boolean belongsToCompany(UUID companyId) {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.companyId().equals(companyId);
    }

    /**
     * Check if current user is accessing their own data.
     */
    public boolean isOwnData(UUID userId) {
        AuthenticatedUser user = getCurrentUser();
        return user != null && user.userId().equals(userId);
    }

    /**
     * Check if current user can manage the target user.
     * Admin can manage anyone in their company.
     * Users cannot manage themselves (except profile updates).
     */
    public boolean canManageUser(UUID targetUserId) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null)
            return false;

        Role role = Role.valueOf(user.role());
        if (!role.canManageUsers())
            return false;

        // Cannot demote/delete yourself
        if (user.userId().equals(targetUserId))
            return false;

        return true;
    }

    /**
     * Check if current user has higher or equal privilege than target role.
     */
    public boolean hasHigherPrivilegeThan(String targetRole) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null)
            return false;

        try {
            Role currentRole = Role.valueOf(user.role());
            Role target = Role.valueOf(targetRole);
            return currentRole.hasHigherOrEqualPrivilegeThan(target);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get current authenticated user.
     */
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser) {
            return (AuthenticatedUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Get current user's company ID.
     */
    public UUID getCurrentCompanyId() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.companyId() : null;
    }

    /**
     * Get current user's ID.
     */
    public UUID getCurrentUserId() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.userId() : null;
    }

    /**
     * Get current user's role.
     */
    public String getCurrentUserRole() {
        AuthenticatedUser user = getCurrentUser();
        return user != null ? user.role() : null;
    }

    /**
     * Check if current user is an admin.
     */
    public boolean isAdmin() {
        AuthenticatedUser user = getCurrentUser();
        return user != null && "ADMIN".equals(user.role());
    }

    /**
     * Check if current user is a manager or higher.
     */
    public boolean isManagerOrHigher() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null)
            return false;
        String role = user.role();
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }
}
