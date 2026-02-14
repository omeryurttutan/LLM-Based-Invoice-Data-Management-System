package com.faturaocr.application.common.service;

import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.infrastructure.security.AuthenticatedUser;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.infrastructure.security.SecurityExpressionService;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * Base class for application services with company scoping.
 */
public abstract class BaseApplicationService {

    @Autowired
    protected SecurityExpressionService securityService;

    /**
     * Get current company ID from security context.
     */
    protected UUID getCurrentCompanyId() {
        UUID companyId = CompanyContextHolder.getCompanyId();
        if (companyId == null) {
            companyId = securityService.getCurrentCompanyId();
        }
        if (companyId == null) {
            throw new DomainException("COMPANY_CONTEXT_REQUIRED",
                    "Company context is required for this operation");
        }
        return companyId;
    }

    /**
     * Get current user ID from security context.
     */
    protected UUID getCurrentUserId() {
        UUID userId = securityService.getCurrentUserId();
        if (userId == null) {
            throw new DomainException("AUTH_REQUIRED",
                    "Authentication is required for this operation");
        }
        return userId;
    }

    /**
     * Get current authenticated user.
     */
    protected AuthenticatedUser getCurrentUser() {
        AuthenticatedUser user = securityService.getCurrentUser();
        if (user == null) {
            throw new DomainException("AUTH_REQUIRED",
                    "Authentication is required for this operation");
        }
        return user;
    }

    /**
     * Verify that the entity belongs to current user's company.
     */
    protected void verifyCompanyAccess(UUID entityCompanyId) {
        UUID currentCompanyId = getCurrentCompanyId();
        if (!currentCompanyId.equals(entityCompanyId)) {
            throw new DomainException("ACCESS_DENIED",
                    "You don't have access to this resource");
        }
    }

    /**
     * Check if current user is admin.
     */
    protected boolean isAdmin() {
        return securityService.isAdmin();
    }

    /**
     * Check if current user is manager or higher.
     */
    protected boolean isManagerOrHigher() {
        return securityService.isManagerOrHigher();
    }
}
