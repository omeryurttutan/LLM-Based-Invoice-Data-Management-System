# PHASE 5: ROLE-BASED ACCESS CONTROL (RBAC)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Security**: JWT authentication with role-based authorization

### Current State
**Phases 0-4 have been completed:**
- ✅ Docker Compose environment
- ✅ CI/CD Pipeline with GitHub Actions
- ✅ Hexagonal Architecture layer structure
- ✅ Database schema with users.role column
- ✅ JWT Authentication (register, login, refresh, logout)
- ✅ Password hashing with BCrypt
- ✅ Brute force protection

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Implement a comprehensive Role-Based Access Control (RBAC) system with four distinct roles (ADMIN, MANAGER, ACCOUNTANT, INTERN). Each role has specific permissions that must be enforced at method level using Spring Security's `@PreAuthorize` annotation. Additionally, implement company-based data isolation (multi-tenancy) to ensure users can only access data belonging to their own company.

---

## ROLE PERMISSION MATRIX

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           PERMISSION MATRIX                                      │
├──────────────────┬──────────┬──────────┬─────────────┬────────────┬─────────────┤
│ Permission       │  ADMIN   │ MANAGER  │ ACCOUNTANT  │   INTERN   │   Notes     │
├──────────────────┼──────────┼──────────┼─────────────┼────────────┼─────────────┤
│ View Invoices    │    ✅    │    ✅    │     ✅      │     ✅     │ Own company │
│ Create Invoice   │    ✅    │    ✅    │     ✅      │     ✅     │ Own company │
│ Edit Invoice     │    ✅    │    ✅    │     ✅      │     ❌     │             │
│ Delete Invoice   │    ✅    │    ✅    │     ❌      │     ❌     │ Soft delete │
│ Verify Invoice   │    ✅    │    ✅    │     ✅      │     ❌     │             │
├──────────────────┼──────────┼──────────┼─────────────┼────────────┼─────────────┤
│ View Reports     │    ✅    │    ✅    │     ❌      │     ❌     │ Dashboard   │
│ Export Data      │    ✅    │    ✅    │     ✅      │     ❌     │ XLSX/CSV    │
├──────────────────┼──────────┼──────────┼─────────────┼────────────┼─────────────┤
│ Manage Users     │    ✅    │    ❌    │     ❌      │     ❌     │ CRUD users  │
│ Assign Roles     │    ✅    │    ❌    │     ❌      │     ❌     │             │
│ Manage Company   │    ✅    │    ❌    │     ❌      │     ❌     │ Settings    │
├──────────────────┼──────────┼──────────┼─────────────┼────────────┼─────────────┤
│ System Settings  │    ✅    │    ❌    │     ❌      │     ❌     │ Config      │
│ View Audit Logs  │    ✅    │    ✅    │     ❌      │     ❌     │             │
│ Manage Categories│    ✅    │    ✅    │     ❌      │     ❌     │             │
└──────────────────┴──────────┴──────────┴─────────────┴────────────┴─────────────┘
```

---

## DETAILED REQUIREMENTS

### 1. Permission Enum

**File**: `domain/user/valueobject/Permission.java`

```java
package com.faturaocr.domain.user.valueobject;

/**
 * System permissions enum.
 */
public enum Permission {
    // Invoice permissions
    INVOICE_VIEW("View invoices"),
    INVOICE_CREATE("Create invoices"),
    INVOICE_EDIT("Edit invoices"),
    INVOICE_DELETE("Delete invoices"),
    INVOICE_VERIFY("Verify/Reject invoices"),
    
    // Report permissions
    REPORT_VIEW("View reports and dashboard"),
    EXPORT_DATA("Export data to XLSX/CSV"),
    
    // User management permissions
    USER_VIEW("View users"),
    USER_CREATE("Create users"),
    USER_EDIT("Edit users"),
    USER_DELETE("Delete users"),
    USER_ASSIGN_ROLE("Assign roles to users"),
    
    // Company permissions
    COMPANY_VIEW("View company details"),
    COMPANY_EDIT("Edit company settings"),
    
    // System permissions
    SYSTEM_SETTINGS("Access system settings"),
    AUDIT_LOG_VIEW("View audit logs"),
    CATEGORY_MANAGE("Manage categories");
    
    private final String description;
    
    Permission(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

---

### 2. Enhanced Role Enum with Permissions

**File**: `domain/user/valueobject/Role.java` (update existing)

```java
package com.faturaocr.domain.user.valueobject;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * User roles with associated permissions.
 */
public enum Role {
    ADMIN("Administrator with full system access", EnumSet.allOf(Permission.class)),
    
    MANAGER("Manager with invoice management and reporting", EnumSet.of(
        Permission.INVOICE_VIEW,
        Permission.INVOICE_CREATE,
        Permission.INVOICE_EDIT,
        Permission.INVOICE_DELETE,
        Permission.INVOICE_VERIFY,
        Permission.REPORT_VIEW,
        Permission.EXPORT_DATA,
        Permission.AUDIT_LOG_VIEW,
        Permission.CATEGORY_MANAGE,
        Permission.COMPANY_VIEW
    )),
    
    ACCOUNTANT("Accountant with invoice CRUD operations", EnumSet.of(
        Permission.INVOICE_VIEW,
        Permission.INVOICE_CREATE,
        Permission.INVOICE_EDIT,
        Permission.INVOICE_VERIFY,
        Permission.EXPORT_DATA,
        Permission.COMPANY_VIEW
    )),
    
    INTERN("Intern with view and create only", EnumSet.of(
        Permission.INVOICE_VIEW,
        Permission.INVOICE_CREATE,
        Permission.COMPANY_VIEW
    ));
    
    private final String description;
    private final Set<Permission> permissions;
    
    Role(String description, Set<Permission> permissions) {
        this.description = description;
        this.permissions = Collections.unmodifiableSet(permissions);
    }
    
    public String getDescription() {
        return description;
    }
    
    public Set<Permission> getPermissions() {
        return permissions;
    }
    
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
    
    public boolean hasHigherOrEqualPrivilegeThan(Role other) {
        return this.ordinal() <= other.ordinal();
    }
    
    // Convenience methods
    public boolean canManageUsers() {
        return hasPermission(Permission.USER_CREATE);
    }
    
    public boolean canDeleteInvoices() {
        return hasPermission(Permission.INVOICE_DELETE);
    }
    
    public boolean canEditInvoices() {
        return hasPermission(Permission.INVOICE_EDIT);
    }
    
    public boolean canViewReports() {
        return hasPermission(Permission.REPORT_VIEW);
    }
    
    public boolean canExportData() {
        return hasPermission(Permission.EXPORT_DATA);
    }
    
    public boolean canVerifyInvoices() {
        return hasPermission(Permission.INVOICE_VERIFY);
    }
    
    public boolean canManageCategories() {
        return hasPermission(Permission.CATEGORY_MANAGE);
    }
    
    public boolean canViewAuditLogs() {
        return hasPermission(Permission.AUDIT_LOG_VIEW);
    }
}
```

---

### 3. Security Expressions Service

**File**: `infrastructure/security/SecurityExpressionService.java`

```java
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
        if (user == null) return false;
        
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
        if (user == null) return false;
        
        Role role = Role.valueOf(user.role());
        if (!role.canManageUsers()) return false;
        
        // Cannot demote/delete yourself
        if (user.userId().equals(targetUserId)) return false;
        
        return true;
    }
    
    /**
     * Check if current user has higher or equal privilege than target role.
     */
    public boolean hasHigherPrivilegeThan(String targetRole) {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) return false;
        
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
        if (user == null) return false;
        String role = user.role();
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }
}
```

---

### 4. Company Context Holder

**File**: `infrastructure/security/CompanyContextHolder.java`

```java
package com.faturaocr.infrastructure.security;

import java.util.UUID;

/**
 * Thread-local holder for current company context.
 * Used for multi-tenant data isolation.
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
```

---

### 5. Company Context Filter

**File**: `infrastructure/security/CompanyContextFilter.java`

```java
package com.faturaocr.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to set company context after authentication.
 */
@Component
@Order(2) // After JwtAuthenticationFilter
public class CompanyContextFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication != null && 
                authentication.getPrincipal() instanceof AuthenticatedUser user) {
                CompanyContextHolder.setCompanyId(user.companyId());
            }
            
            filterChain.doFilter(request, response);
        } finally {
            CompanyContextHolder.clear();
        }
    }
}
```

---

### 6. Update Security Config

**File**: `infrastructure/common/config/SecurityConfig.java` (update)

Add the CompanyContextFilter after JwtAuthenticationFilter:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> 
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/api/v1/health").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            
            // Admin only endpoints
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/system/**").hasRole("ADMIN")
            
            // Manager and above
            .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "MANAGER")
            .requestMatchers("/api/v1/audit-logs/**").hasAnyRole("ADMIN", "MANAGER")
            
            // All authenticated users
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(companyContextFilter, JwtAuthenticationFilter.class);
    
    return http.build();
}
```

---

### 7. Authorization Annotations Interface

**File**: `infrastructure/security/annotations/RequirePermission.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Custom annotation for permission-based authorization.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@securityService.hasPermission(#permission)")
public @interface RequirePermission {
    String value();
}
```

**File**: `infrastructure/security/annotations/RequireRole.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Custom annotation for role-based authorization.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}
```

---

### 8. Pre-defined Authorization Annotations

**File**: `infrastructure/security/annotations/IsAdmin.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface IsAdmin {
}
```

**File**: `infrastructure/security/annotations/IsManagerOrHigher.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public @interface IsManagerOrHigher {
}
```

**File**: `infrastructure/security/annotations/CanEditInvoice.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@securityService.hasPermission('INVOICE_EDIT')")
public @interface CanEditInvoice {
}
```

**File**: `infrastructure/security/annotations/CanDeleteInvoice.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@securityService.hasPermission('INVOICE_DELETE')")
public @interface CanDeleteInvoice {
}
```

**File**: `infrastructure/security/annotations/CanExportData.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@securityService.hasPermission('EXPORT_DATA')")
public @interface CanExportData {
}
```

**File**: `infrastructure/security/annotations/CanManageUsers.java`

```java
package com.faturaocr.infrastructure.security.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@securityService.hasPermission('USER_CREATE')")
public @interface CanManageUsers {
}
```

---

### 9. Company-Scoped Repository Base

**File**: `infrastructure/persistence/common/CompanyScopedRepository.java`

```java
package com.faturaocr.infrastructure.persistence.common;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface for company-scoped entities.
 * Provides methods that automatically filter by company.
 */
@NoRepositoryBean
public interface CompanyScopedRepository<T, ID> extends JpaRepository<T, ID> {
    
    List<T> findByCompanyId(UUID companyId);
    
    Optional<T> findByIdAndCompanyId(ID id, UUID companyId);
    
    boolean existsByIdAndCompanyId(ID id, UUID companyId);
    
    void deleteByIdAndCompanyId(ID id, UUID companyId);
    
    long countByCompanyId(UUID companyId);
}
```

---

### 10. Base Application Service with Company Scoping

**File**: `application/common/service/BaseApplicationService.java`

```java
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
```

---

### 11. Example: Invoice Service with RBAC

**File**: `application/invoice/service/InvoiceApplicationService.java`

```java
package com.faturaocr.application.invoice.service;

import com.faturaocr.application.common.service.ApplicationService;
import com.faturaocr.application.common.service.BaseApplicationService;
import com.faturaocr.domain.common.exception.DomainException;
import com.faturaocr.domain.common.exception.EntityNotFoundException;
import com.faturaocr.infrastructure.security.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;

/**
 * Invoice application service with RBAC.
 * This is a skeleton - full implementation in Phase 7.
 */
@ApplicationService
public class InvoiceApplicationService extends BaseApplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceApplicationService.class);
    
    /**
     * Get all invoices for current company.
     * All authenticated users can view invoices.
     */
    @PreAuthorize("@securityService.hasPermission('INVOICE_VIEW')")
    public void listInvoices() {
        UUID companyId = getCurrentCompanyId();
        logger.info("Listing invoices for company: {}", companyId);
        // Implementation in Phase 7
    }
    
    /**
     * Get single invoice by ID.
     * Verifies company access.
     */
    @PreAuthorize("@securityService.hasPermission('INVOICE_VIEW')")
    public void getInvoice(UUID invoiceId) {
        UUID companyId = getCurrentCompanyId();
        logger.info("Getting invoice {} for company: {}", invoiceId, companyId);
        // Fetch invoice and verify company
        // verifyCompanyAccess(invoice.getCompanyId());
    }
    
    /**
     * Create new invoice.
     * All roles can create invoices.
     */
    @PreAuthorize("@securityService.hasPermission('INVOICE_CREATE')")
    public void createInvoice() {
        UUID companyId = getCurrentCompanyId();
        UUID userId = getCurrentUserId();
        logger.info("Creating invoice for company: {} by user: {}", companyId, userId);
        // Implementation in Phase 7
    }
    
    /**
     * Update existing invoice.
     * INTERN cannot edit invoices.
     */
    @CanEditInvoice
    public void updateInvoice(UUID invoiceId) {
        UUID companyId = getCurrentCompanyId();
        logger.info("Updating invoice {} for company: {}", invoiceId, companyId);
        // Fetch invoice, verify company, update
    }
    
    /**
     * Delete invoice (soft delete).
     * Only ADMIN and MANAGER can delete.
     */
    @CanDeleteInvoice
    public void deleteInvoice(UUID invoiceId) {
        UUID companyId = getCurrentCompanyId();
        logger.info("Deleting invoice {} for company: {}", invoiceId, companyId);
        // Fetch invoice, verify company, soft delete
    }
    
    /**
     * Verify invoice.
     * INTERN cannot verify invoices.
     */
    @PreAuthorize("@securityService.hasPermission('INVOICE_VERIFY')")
    public void verifyInvoice(UUID invoiceId) {
        UUID companyId = getCurrentCompanyId();
        UUID userId = getCurrentUserId();
        logger.info("Verifying invoice {} by user: {}", invoiceId, userId);
        // Verify invoice
    }
    
    /**
     * Export invoices.
     * INTERN cannot export.
     */
    @CanExportData
    public void exportInvoices(String format) {
        UUID companyId = getCurrentCompanyId();
        logger.info("Exporting invoices for company: {} in format: {}", companyId, format);
        // Export implementation
    }
}
```

---

### 12. Access Denied Handler

**File**: `infrastructure/security/CustomAccessDeniedHandler.java`

```java
package com.faturaocr.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.interfaces.rest.common.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for access denied errors.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    
    private final ObjectMapper objectMapper;
    
    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ErrorResponse error = new ErrorResponse(
            "ACCESS_DENIED",
            "You don't have permission to perform this action",
            request.getRequestURI()
        );
        
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
```

**File**: `infrastructure/security/CustomAuthenticationEntryPoint.java`

```java
package com.faturaocr.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.interfaces.rest.common.ErrorResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for authentication errors.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ErrorResponse error = new ErrorResponse(
            "UNAUTHORIZED",
            "Authentication is required to access this resource",
            request.getRequestURI()
        );
        
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
```

---

### 13. Update Security Config with Handlers

**File**: `infrastructure/common/config/SecurityConfig.java` (final update)

```java
package com.faturaocr.infrastructure.common.config;

import com.faturaocr.infrastructure.security.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration with RBAC.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CompanyContextFilter companyContextFilter;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    
    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CompanyContextFilter companyContextFilter,
            CustomAccessDeniedHandler accessDeniedHandler,
            CustomAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.companyContextFilter = companyContextFilter;
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (using JWT)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Exception handling
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Admin only endpoints (URL-based)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/system/**").hasRole("ADMIN")
                
                // Manager and above (URL-based)
                .requestMatchers("/api/v1/audit-logs/**").hasAnyRole("ADMIN", "MANAGER")
                
                // All other endpoints require authentication
                // Method-level security handles specific permissions
                .anyRequest().authenticated()
            )
            
            // Add filters
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(companyContextFilter, JwtAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3001",
            "http://localhost:8082"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

---

### 14. Integration Tests for RBAC

**File**: `src/test/java/com/faturaocr/security/RbacIntegrationTest.java`

```java
package com.faturaocr.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RBAC.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RbacIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    // Helper method to get token for each role
    // Implementation depends on your test setup
    
    @Nested
    @DisplayName("ADMIN Role Tests")
    class AdminRoleTests {
        
        @Test
        @DisplayName("ADMIN can access user management")
        void adminCanAccessUserManagement() throws Exception {
            // Get admin token
            // mockMvc.perform(get("/api/v1/users")
            //     .header("Authorization", "Bearer " + adminToken))
            //     .andExpect(status().isOk());
        }
        
        @Test
        @DisplayName("ADMIN can delete invoices")
        void adminCanDeleteInvoices() throws Exception {
            // Test delete permission
        }
        
        @Test
        @DisplayName("ADMIN can access audit logs")
        void adminCanAccessAuditLogs() throws Exception {
            // Test audit log access
        }
    }
    
    @Nested
    @DisplayName("MANAGER Role Tests")
    class ManagerRoleTests {
        
        @Test
        @DisplayName("MANAGER cannot access user management")
        void managerCannotAccessUserManagement() throws Exception {
            // Get manager token
            // mockMvc.perform(get("/api/v1/users")
            //     .header("Authorization", "Bearer " + managerToken))
            //     .andExpect(status().isForbidden());
        }
        
        @Test
        @DisplayName("MANAGER can delete invoices")
        void managerCanDeleteInvoices() throws Exception {
            // Test delete permission
        }
        
        @Test
        @DisplayName("MANAGER can access reports")
        void managerCanAccessReports() throws Exception {
            // Test report access
        }
    }
    
    @Nested
    @DisplayName("ACCOUNTANT Role Tests")
    class AccountantRoleTests {
        
        @Test
        @DisplayName("ACCOUNTANT cannot delete invoices")
        void accountantCannotDeleteInvoices() throws Exception {
            // Test that accountant gets 403 on delete
        }
        
        @Test
        @DisplayName("ACCOUNTANT can edit invoices")
        void accountantCanEditInvoices() throws Exception {
            // Test edit permission
        }
        
        @Test
        @DisplayName("ACCOUNTANT cannot access reports")
        void accountantCannotAccessReports() throws Exception {
            // Test that accountant gets 403 on reports
        }
    }
    
    @Nested
    @DisplayName("INTERN Role Tests")
    class InternRoleTests {
        
        @Test
        @DisplayName("INTERN can view invoices")
        void internCanViewInvoices() throws Exception {
            // Test view permission
        }
        
        @Test
        @DisplayName("INTERN can create invoices")
        void internCanCreateInvoices() throws Exception {
            // Test create permission
        }
        
        @Test
        @DisplayName("INTERN cannot edit invoices")
        void internCannotEditInvoices() throws Exception {
            // Test that intern gets 403 on edit
        }
        
        @Test
        @DisplayName("INTERN cannot delete invoices")
        void internCannotDeleteInvoices() throws Exception {
            // Test that intern gets 403 on delete
        }
        
        @Test
        @DisplayName("INTERN cannot export data")
        void internCannotExportData() throws Exception {
            // Test that intern gets 403 on export
        }
    }
    
    @Nested
    @DisplayName("Company Isolation Tests")
    class CompanyIsolationTests {
        
        @Test
        @DisplayName("User cannot access other company's data")
        void userCannotAccessOtherCompanyData() throws Exception {
            // Create invoice in company A
            // Try to access with user from company B
            // Should get 403 or 404
        }
    }
}
```

---

## TESTING REQUIREMENTS

### Test 1: Permission Check
```bash
# Login as ADMIN
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@demo.com", "password": "Admin123!"}' | jq -r '.data.accessToken')

# Should succeed (ADMIN has all permissions)
curl -X GET http://localhost:8082/api/v1/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# Expected: 200 OK
```

### Test 2: INTERN Restrictions
```bash
# Create INTERN user first, then login
INTERN_TOKEN="..."

# Should fail - INTERN cannot delete
curl -X DELETE http://localhost:8082/api/v1/invoices/some-id \
  -H "Authorization: Bearer $INTERN_TOKEN"
# Expected: 403 Forbidden

# Should fail - INTERN cannot export
curl -X GET http://localhost:8082/api/v1/invoices/export \
  -H "Authorization: Bearer $INTERN_TOKEN"
# Expected: 403 Forbidden
```

### Test 3: Company Isolation
```bash
# User from Company A tries to access Company B's invoice
curl -X GET http://localhost:8082/api/v1/invoices/company-b-invoice-id \
  -H "Authorization: Bearer $COMPANY_A_TOKEN"
# Expected: 403 or 404
```

### Test 4: Run Integration Tests
```bash
cd backend
mvn test -Dtest=RbacIntegrationTest
```

---

## VERIFICATION CHECKLIST

After completing this phase, verify all items:

- [ ] Permission enum with all permissions created
- [ ] Role enum updated with permission sets
- [ ] SecurityExpressionService created with helper methods
- [ ] CompanyContextHolder for multi-tenant context
- [ ] CompanyContextFilter sets company context
- [ ] Custom annotations (@IsAdmin, @CanEditInvoice, etc.)
- [ ] BaseApplicationService with company scoping
- [ ] CustomAccessDeniedHandler returns proper JSON
- [ ] CustomAuthenticationEntryPoint returns proper JSON
- [ ] SecurityConfig updated with handlers and filters
- [ ] ADMIN can access all endpoints
- [ ] MANAGER cannot access user management
- [ ] ACCOUNTANT cannot delete invoices
- [ ] INTERN cannot edit/delete/export
- [ ] Company isolation working
- [ ] Integration tests created
- [ ] All tests pass
- [ ] CI pipeline passes

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_5_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (2-3 days)

### 2. Completed Tasks
List each task with checkbox.

### 3. Files Created
Organized by layer:
```
domain/user/valueobject/
├── Permission.java
└── Role.java (updated)

infrastructure/security/
├── SecurityExpressionService.java
├── CompanyContextHolder.java
├── CompanyContextFilter.java
├── CustomAccessDeniedHandler.java
├── CustomAuthenticationEntryPoint.java
└── annotations/
    ├── IsAdmin.java
    ├── IsManagerOrHigher.java
    ├── CanEditInvoice.java
    ├── CanDeleteInvoice.java
    ├── CanExportData.java
    └── CanManageUsers.java
```

### 4. Permission Matrix Verification
| Role | Tested Permissions | Result |
|------|-------------------|--------|
| ADMIN | All | ✅ |
| MANAGER | ... | ✅ |
| ACCOUNTANT | ... | ✅ |
| INTERN | ... | ✅ |

### 5. Test Results
Include test outputs and screenshots.

### 6. Issues Encountered
Document any problems and solutions.

### 7. Next Steps
What needs to be done in Phase 6 (Company/User API).

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 4**: Authentication ✅

### Required By (blocks these phases)
- **Phase 6**: Company/User API (needs RBAC)
- **Phase 7**: Invoice CRUD API (needs RBAC)
- All subsequent phases with protected endpoints

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ 4 roles (ADMIN, MANAGER, ACCOUNTANT, INTERN) have distinct permissions
2. ✅ @PreAuthorize annotations work at method level
3. ✅ Custom annotations simplify authorization
4. ✅ Company isolation prevents cross-tenant access
5. ✅ Access denied returns proper JSON response
6. ✅ Authentication required returns proper JSON response
7. ✅ SecurityExpressionService provides helper methods
8. ✅ All roles tested with correct access/denial
9. ✅ Integration tests pass
10. ✅ Result file is created with complete documentation

---

**Phase 5 Completion Target**: Complete RBAC system with company-based multi-tenancy
