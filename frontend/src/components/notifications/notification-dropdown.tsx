import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, Check, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/components/ui/popover';
import { ScrollArea } from '@/components/ui/scroll-area';
import { useNotificationStore } from '@/stores/notification-store';
import { notificationService } from '@/services/notification-service';
import { NotificationItem } from './notification-item';
import { cn } from '@/lib/utils';
import { Notification } from '@/types/notification';

export function NotificationDropdown() {
    const router = useRouter();
    const queryClient = useQueryClient();
    const {
        notifications,
        unreadCount,
        setNotifications,
        setUnreadCount,
        markAsRead: markAsReadInStore,
        markAllAsRead: markAllAsReadInStore,
        isDropdownOpen,
        setDropdownOpen
    } = useNotificationStore();

    // Fetch unread count on mount
    useQuery({
        queryKey: ['notificationUnreadCount'],
        queryFn: async () => {
            const data = await notificationService.getUnreadCount();
            setUnreadCount(data.count);
            return data;
        },
        refetchInterval: 60000, // Refresh every minute
    });

    // Fetch recent notifications when dropdown opens
    // We use a query but we manualy sync to store to keep usage consistent
    const { data: recentNotifications, refetch: refetchNotifications } = useQuery({
        queryKey: ['recentNotifications'],
        queryFn: async () => {
            const data = await notificationService.getNotifications({ page: 0, size: 10, sort: 'createdAt,desc' });
            return data.content;
        },
        enabled: isDropdownOpen, // Only fetch when open
        staleTime: 1000 * 30, // 30 seconds
    });

    // Sync fetched notifications to store
    useEffect(() => {
        if (recentNotifications) {
            // Logic to merge or replace. 
            // Requirement says: "Real-time additions from WebSocket prepended".
            // If we just replace, we lose real-time ones if the fetch is slow or if there's a race.
            // But typically "recent" fetch returns the absolute latest.
            // So replacing is fine, BUT we need to be careful not to overwrite a JUST arrived WS message if the fetch was initiated before it.
            // For simplicity and robustness given the requirement "setNotifications(list) — replace list", we replace.
            // PRO TIP: We only replace if the fetched list is "newer" or just trust the API.
            setNotifications(recentNotifications);
        }
    }, [recentNotifications, setNotifications]);

    const markAsReadMutation = useMutation({
        mutationFn: (id: number) => notificationService.markAsRead(id),
        onSuccess: (_, id) => {
            markAsReadInStore(id);
            queryClient.invalidateQueries({ queryKey: ['notificationUnreadCount'] });
            queryClient.invalidateQueries({ queryKey: ['recentNotifications'] });
        },
    });

    const markAllAsReadMutation = useMutation({
        mutationFn: () => notificationService.markAllAsRead(),
        onSuccess: () => {
            markAllAsReadInStore();
            queryClient.invalidateQueries({ queryKey: ['notificationUnreadCount'] });
            queryClient.invalidateQueries({ queryKey: ['recentNotifications'] });
        },
    });

    const handleNotificationClick = (notification: Notification) => {
        // Mark as read
        if (!notification.read) {
            markAsReadMutation.mutate(notification.id);
        }

        setDropdownOpen(false);

        // Navigate
        if (notification.referenceType === 'INVOICE' && notification.referenceId) {
            router.push(`/invoices/${notification.referenceId}`);
        } else if (notification.referenceType === 'BATCH' && notification.referenceId) {
            // If backend supports filtering by batchId via query param
            router.push(`/invoices?batchId=${notification.referenceId}`);
        } else if (notification.referenceType === 'SYSTEM') {
            // Do nothing or maybe go to a system page
        }
    };

    return (
        <Popover open={isDropdownOpen} onOpenChange={setDropdownOpen}>
            <PopoverTrigger asChild>
                <Button variant="ghost" size="icon" className="relative">
                    <Bell className={cn("h-5 w-5", isDropdownOpen && "fill-current")} />
                    {unreadCount > 0 && (
                        <span className="absolute top-1.5 right-1.5 flex h-4 w-4 items-center justify-center rounded-full bg-red-600 text-[10px] font-bold text-white ring-2 ring-background">
                            {unreadCount > 9 ? '9+' : unreadCount}
                        </span>
                    )}
                    <span className="sr-only">Bildirimler</span>
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-80 p-0" align="end">
                <div className="flex items-center justify-between px-4 py-3 border-b">
                    <h4 className="font-semibold text-sm">Bildirimler</h4>
                    {unreadCount > 0 && (
                        <Button
                            variant="ghost"
                            size="sm"
                            className="h-auto px-1 text-xs text-primary"
                            onClick={() => markAllAsReadMutation.mutate()}
                            disabled={markAllAsReadMutation.isPending}
                        >
                            Tümünü okundu işaretle
                        </Button>
                    )}
                </div>
                <ScrollArea className="h-[300px]">
                    {notifications.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-full p-4 text-center text-muted-foreground">
                            <Bell className="h-8 w-8 mb-2 opacity-20" />
                            <p className="text-sm">Bildirim bulunmuyor</p>
                        </div>
                    ) : (
                        <div className="flex flex-col">
                            {notifications.map((notification) => (
                                <NotificationItem
                                    key={notification.id}
                                    notification={notification}
                                    onClick={() => handleNotificationClick(notification)}
                                    compact
                                />
                            ))}
                        </div>
                    )}
                </ScrollArea>
                <div className="border-t p-2">
                    <Button variant="outline" className="w-full h-8 text-xs" onClick={() => {
                        setDropdownOpen(false);
                        router.push('/notifications');
                    }}>
                        Tüm bildirimleri göster
                    </Button>
                </div>
            </PopoverContent>
        </Popover>
    );
}
