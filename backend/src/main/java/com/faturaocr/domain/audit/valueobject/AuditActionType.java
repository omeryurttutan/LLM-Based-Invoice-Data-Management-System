package com.faturaocr.domain.audit.valueobject;

/**
 * Enum representing the types of auditable actions.
 */
public enum AuditActionType {
    CREATE,
    UPDATE,
    DELETE,
    LOGIN,
    LOGOUT,
    EXPORT,
    VERIFY,
    REJECT
}
