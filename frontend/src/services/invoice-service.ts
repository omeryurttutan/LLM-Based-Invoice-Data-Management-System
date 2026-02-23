import apiClient from '@/lib/api-client';
import { InvoiceDetail, InvoiceListItem, CreateInvoiceRequest, UpdateInvoiceRequest, 
         VerifyInvoiceRequest, RejectInvoiceRequest, PaginatedResponse, InvoiceListParams, BatchStatusResponse,
         InvoiceData, ValidationResult, FilterOptionsResponse } from '@/types/invoice';

export const invoiceService = {
  async getInvoices(params: InvoiceListParams = {}): Promise<PaginatedResponse<InvoiceListItem>> {
    const response = await apiClient.get('/invoices', {
      params
    });
    const data = response.data;
    
    // Normalize Spring Boot Page response:
    // New format (3.2+): { content: [...], page: { number, size, totalElements, totalPages } }
    // Old format: { content: [...], number: 0, size: 20, totalElements: 100, totalPages: 5, ... }
    if (data && !data.page && data.content) {
      return {
        content: data.content,
        page: {
          number: data.number ?? 0,
          size: data.size ?? 20,
          totalElements: data.totalElements ?? 0,
          totalPages: data.totalPages ?? 0,
        }
      };
    }
    return data;
  },

  async getFilterOptions(): Promise<FilterOptionsResponse> {
    return (await apiClient.get('/invoices/filter-options')).data;
  },

  async getSuppliers(search: string): Promise<string[]> {
    return (await apiClient.get('/invoices/suppliers', { params: { search } })).data;
  },

  async getInvoice(id: string): Promise<InvoiceDetail> {
    return (await apiClient.get(`/invoices/${id}`)).data;
  },

  async createInvoice(data: CreateInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.post('/invoices', data)).data;
  },

  async updateInvoice(id: string, data: UpdateInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.put(`/invoices/${id}`, data)).data;
  },

  async deleteInvoice(id: string): Promise<void> {
    await apiClient.delete(`/invoices/${id}`);
  },

  async verifyInvoice(id: string, data?: VerifyInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.patch(`/invoices/${id}/verify`, data ?? {})).data;
  },

  async rejectInvoice(id: string, data: RejectInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.patch(`/invoices/${id}/reject`, data)).data;
  },

  async reopenInvoice(id: string): Promise<InvoiceDetail> {
    return (await apiClient.patch(`/invoices/${id}/reopen`, {})).data;
  },

  async uploadInvoice(file: File): Promise<InvoiceDetail> {
    const formData = new FormData();
    formData.append('file', file);
    
    return (await apiClient.post('/invoices/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 90000 // 90 seconds timeout for LLM processing
    })).data;
  },

  async bulkUploadInvoices(files: File[]): Promise<{ batchId: string }> {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    
    return (await apiClient.post('/invoices/bulk-upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })).data;
  },

  async getBatchStatus(batchId: string): Promise<BatchStatusResponse> {
    return (await apiClient.get(`/invoices/batch/${batchId}`)).data;
  },

  async getInvoiceFile(id: string): Promise<Blob> {
    const response = await apiClient.get(`/invoices/${id}/file`, {
      responseType: 'blob'
    });
    return response.data;
  },

  async validateExtraction(data: InvoiceData): Promise<ValidationResult> {
    return (await apiClient.post('/extraction/validate', data)).data;
  },
};
