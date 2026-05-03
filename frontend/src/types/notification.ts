export type NotificationType =
    | 'EXTRACTION_COMPLETED'
    | 'EXTRACTION_FAILED'
    | 'BATCH_COMPLETED'
    | 'BATCH_PARTIALLY_COMPLETED'
    | 'LOW_CONFIDENCE'
    | 'HIGH_CONFIDENCE_AUTO_VERIFIED'
    | 'INVOICE_VERIFIED'
    | 'INVOICE_REJECTED'
    | 'PROVIDER_DEGRADED'
    | 'ALL_PROVIDERS_DOWN'
    | 'SYSTEM_ANNOUNCEMENT';

export type NotificationSeverity = 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';

export type ReferenceType = 'INVOICE' | 'BATCH' | 'SYSTEM';

export interface Notification {
    id: number;
    type: NotificationType;
    title: string;
    message: string;
    severity: NotificationSeverity;
    referenceType: ReferenceType;
    referenceId?: number;
    metadata?: Record<string, any>;
    createdAt: string;
    read: boolean;
}
