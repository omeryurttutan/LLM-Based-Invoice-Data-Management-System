import apiClient from '@/lib/api-client';
import { InvoiceVersionSummary, InvoiceVersionDetail, VersionDiff } from '@/types/version-history';
import { InvoiceDetail } from '@/types/invoice';

export const versionService = {
    // Get list of versions for an invoice
    getVersions: async (invoiceId: string): Promise<InvoiceVersionSummary[]> => {
        const response = await apiClient.get<InvoiceVersionSummary[]>(`/invoices/${invoiceId}/versions`);
        return response.data;
    },

    // Get full snapshot of a specific version
    getVersionDetail: async (invoiceId: string, versionId: string): Promise<InvoiceVersionDetail> => {
        const response = await apiClient.get<InvoiceVersionDetail>(`/invoices/${invoiceId}/versions/${versionId}`);
        return response.data;
    },

    // Get diff between two versions
    getVersionDiff: async (invoiceId: string, fromVersion: number, toVersion: number): Promise<VersionDiff> => {
        const response = await apiClient.get<VersionDiff>(`/invoices/${invoiceId}/versions/diff`, {
            params: { from: fromVersion, to: toVersion }
        });
        return response.data;
    },

    // Get latest version number
    getLatestVersionNumber: async (invoiceId: string): Promise<number> => {
        const response = await apiClient.get<number>(`/invoices/${invoiceId}/versions/latest`);
        return response.data;
    },

    // Revert to a specific version
    revertToVersion: async (invoiceId: string, versionNumber: number): Promise<InvoiceDetail> => {
        const response = await apiClient.post<InvoiceDetail>(`/invoices/${invoiceId}/revert/${versionNumber}`);
        return response.data;
    }
};
