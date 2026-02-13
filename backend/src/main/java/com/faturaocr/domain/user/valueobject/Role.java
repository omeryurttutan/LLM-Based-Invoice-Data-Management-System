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
            Permission.COMPANY_VIEW)),

    ACCOUNTANT("Accountant with invoice CRUD operations", EnumSet.of(
            Permission.INVOICE_VIEW,
            Permission.INVOICE_CREATE,
            Permission.INVOICE_EDIT,
            Permission.INVOICE_VERIFY,
            Permission.EXPORT_DATA,
            Permission.COMPANY_VIEW)),

    INTERN("Intern with view and create only", EnumSet.of(
            Permission.INVOICE_VIEW,
            Permission.INVOICE_CREATE,
            Permission.COMPANY_VIEW));

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
