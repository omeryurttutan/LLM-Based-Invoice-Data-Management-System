import apiClient from './api-client';
import { API_ENDPOINTS } from './endpoints';
import { AuditLogResponse, AuditLogFilterDTO } from '@/types/audit-log';

export const auditLogService = {
  getAuditLogs: async (
    filter: AuditLogFilterDTO = {},
    page = 0,
    size = 20,
    sort = 'createdAt,desc'
  ): Promise<{ content: AuditLogResponse[], totalPages: number, totalElements: number }> => {
    const response = await apiClient.get(API_ENDPOINTS.AUDIT_LOGS, {
      params: { ...filter, page, size, sort }
    });
    return response.data || { content: [], totalPages: 0, totalElements: 0 };
  },

  getEntityHistory: async (
    entityType: string,
    entityId: string,
    page = 0,
    size = 20
  ): Promise<{ content: AuditLogResponse[], totalPages: number, totalElements: number }> => {
    const response = await apiClient.get(`${API_ENDPOINTS.AUDIT_LOGS}/entity/${entityType}/${entityId}`, {
      params: { page, size, sort: 'createdAt,desc' }
    });
    return response.data || { content: [], totalPages: 0, totalElements: 0 };
  }
};
