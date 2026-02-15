import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { invoiceService } from '@/services/invoice-service';
import { useNotificationStore } from '@/stores/notification-store';
import { BatchStatusResponse } from '@/types/invoice';

export function useBatchStatus(batchId: string) {
    const queryClient = useQueryClient();
    const { isConnected, notifications } = useNotificationStore();

    // Fetch batch status
    const { data, isLoading, error, refetch } = useQuery({
        queryKey: ['batchStatus', batchId],
        queryFn: () => invoiceService.getBatchStatus(batchId),
        // Poll only if NOT connected via WebSocket and status is PROCESSING
        refetchInterval: (query) => {
            const isProcessing = query.state.data?.status === 'PROCESSING';
            if (isProcessing && !isConnected) {
                return 5000;
            }
            return false;
        },
    });

    // Listen for WebSocket notifications to update status
    useEffect(() => {
        if (!isConnected || !data || data.status !== 'PROCESSING') return;

        // Check the latest notification
        const latestNotification = notifications[0];
        if (!latestNotification) return;

        // Check if notification is relevant to this batch
        const isBatchEvent = latestNotification.referenceType === 'BATCH' && String(latestNotification.referenceId) === batchId;

        // Check if it's an invoice event that might belong to this batch
        // We check metadata for batchId if provided by backend
        const isInvoiceEvent = latestNotification.referenceType === 'INVOICE' &&
            (latestNotification.type === 'EXTRACTION_COMPLETED' || latestNotification.type === 'EXTRACTION_FAILED') &&
            latestNotification.metadata?.batchId === batchId;

        if (isBatchEvent || isInvoiceEvent) {
            // Refetch to get updated progress
            refetch();
        }
    }, [batchId, isConnected, notifications, refetch, data]);

    return {
        batchStatus: data,
        isLoading,
        error,
        isConnected,
        isComplete: data?.status === 'COMPLETED' || data?.status === 'FAILED' || data?.status === 'PARTIALLY_COMPLETED'
    };
}
