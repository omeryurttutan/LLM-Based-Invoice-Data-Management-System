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
    COMPANY_CREATE("Create new companies"),
    COMPANY_DELETE("Delete companies"),

    // System permissions
    SYSTEM_SETTINGS("Access system settings"),
    AUDIT_LOG_VIEW("View audit logs"),
    CATEGORY_MANAGE("Manage categories"),

    // Platform / Super Admin permissions
    SUPER_ADMIN_ACCESS("Super admin system-wide access"),
    SUBSCRIPTION_MANAGE("Manage company subscriptions and quotas");

    private final String description;

    Permission(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
