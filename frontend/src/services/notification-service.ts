import apiClient from '@/lib/api-client';
import { Notification, NotificationType, NotificationSeverity } from '@/types/notification';

export interface NotificationListParams {
    page?: number;
    size?: number;
    sort?: string;
    isRead?: boolean;
    type?: NotificationType;
    severity?: NotificationSeverity;
}

export interface NotificationListResponse {
    content: Notification[];
    totalPages: number;
    totalElements: number;
    number: number;
    size: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

export const notificationService = {
    async getNotifications(params: NotificationListParams = {}): Promise<NotificationListResponse> {
        return (await apiClient.get('/notifications', { params })).data;
    },

    async getUnreadCount(): Promise<{ count: number }> {
        return (await apiClient.get('/notifications/unread-count')).data;
    },

    async markAsRead(id: number): Promise<void> {
        await apiClient.patch(`/notifications/${id}/read`);
    },

    async markAllAsRead(): Promise<void> {
        await apiClient.patch('/notifications/read-all');
    },

    async deleteNotification(id: number): Promise<void> {
        await apiClient.delete(`/notifications/${id}`);
    }
};
