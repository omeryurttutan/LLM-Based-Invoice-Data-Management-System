export interface AuditLogResponse {
    id: string;
    actionType: string;
    entityType: string;
    entityId: string;
    userId: string;
    userEmail: string;
    companyId: string;
    description: string;
    ipAddress: string;
    userAgent: string;
    createdAt: string;
}

export interface AuditLogFilterDTO {
    action?: string;
    entityType?: string;
    userId?: string;
    startDate?: string;
    endDate?: string;
}
