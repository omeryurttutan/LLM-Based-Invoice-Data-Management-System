package com.faturaocr.domain.notification.enums;

public enum NotificationType {
    EXTRACTION_COMPLETED,
    EXTRACTION_FAILED,
    BATCH_COMPLETED,
    BATCH_FAILED,
    BATCH_PARTIALLY_COMPLETED,
    LOW_CONFIDENCE,
    HIGH_CONFIDENCE_AUTO_VERIFIED,
    INVOICE_VERIFIED,
    INVOICE_REJECTED,
    PROVIDER_DEGRADED,
    ALL_PROVIDERS_DOWN,
    SYSTEM_ANNOUNCEMENT;

    public NotificationSeverity getDefaultSeverity() {
        return switch (this) {
            case EXTRACTION_FAILED, BATCH_FAILED, ALL_PROVIDERS_DOWN, PROVIDER_DEGRADED -> NotificationSeverity.ERROR;
            case LOW_CONFIDENCE, INVOICE_REJECTED -> NotificationSeverity.WARNING;
            case EXTRACTION_COMPLETED, BATCH_COMPLETED, BATCH_PARTIALLY_COMPLETED, HIGH_CONFIDENCE_AUTO_VERIFIED,
                    INVOICE_VERIFIED, SYSTEM_ANNOUNCEMENT ->
                NotificationSeverity.SUCCESS;
        };
    }
}
