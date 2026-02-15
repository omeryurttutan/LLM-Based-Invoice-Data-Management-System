"use client";

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Check,
    Trash2,
    Filter,
    Loader2,
    Bell
} from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/components/ui/select';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Separator } from '@/components/ui/separator';

import { notificationService, NotificationListParams } from '@/services/notification-service';
import { NotificationItem } from '@/components/notifications/notification-item';
import { DataTablePagination } from '@/components/common/data-table-pagination';
import { Notification, NotificationType, NotificationSeverity } from '@/types/notification';
import { useNotificationStore } from '@/stores/notification-store';

export default function NotificationsPage() {
    const router = useRouter();
    const queryClient = useQueryClient();
    const { markAsRead: markAsReadInStore, markAllAsRead: markAllAsReadInStore, removeNotification } = useNotificationStore();

    const [page, setPage] = useState(0);
    const [size, setSize] = useState(20);
    const [filterTab, setFilterTab] = useState<"ALL" | "UNREAD" | "READ">("ALL");
    const [selectedType, setSelectedType] = useState<NotificationType | "ALL">("ALL");
    const [selectedSeverity, setSelectedSeverity] = useState<NotificationSeverity | "ALL">("ALL");

    const queryParams: NotificationListParams = {
        page,
        size,
        sort: 'createdAt,desc',
        ...(filterTab === 'UNREAD' ? { isRead: false } : {}),
        ...(filterTab === 'READ' ? { isRead: true } : {}),
        ...(selectedType !== 'ALL' ? { type: selectedType } : {}),
        ...(selectedSeverity !== 'ALL' ? { severity: selectedSeverity } : {}),
    };

    const { data, isLoading } = useQuery({
        queryKey: ['notifications', queryParams],
        queryFn: () => notificationService.getNotifications(queryParams),
    });

    const markAsReadMutation = useMutation({
        mutationFn: (id: number) => notificationService.markAsRead(id),
        onSuccess: (_, id) => {
            markAsReadInStore(id);
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
            queryClient.invalidateQueries({ queryKey: ['notificationUnreadCount'] });
            queryClient.invalidateQueries({ queryKey: ['recentNotifications'] });
        },
    });

    const markAllAsReadMutation = useMutation({
        mutationFn: () => notificationService.markAllAsRead(),
        onSuccess: () => {
            markAllAsReadInStore();
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
            queryClient.invalidateQueries({ queryKey: ['notificationUnreadCount'] });
            queryClient.invalidateQueries({ queryKey: ['recentNotifications'] });
        },
    });

    const deleteMutation = useMutation({
        mutationFn: (id: number) => notificationService.deleteNotification(id),
        onSuccess: (_, id) => {
            removeNotification(id);
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
            queryClient.invalidateQueries({ queryKey: ['notificationUnreadCount'] });
            queryClient.invalidateQueries({ queryKey: ['recentNotifications'] });
        },
    });

    const handleNotificationClick = (notification: Notification) => {
        if (!notification.read) {
            markAsReadMutation.mutate(notification.id);
        }

        // Navigation logic
        if (notification.referenceType === 'INVOICE' && notification.referenceId) {
            router.push(`/invoices/${notification.referenceId}`);
        } else if (notification.referenceType === 'BATCH' && notification.referenceId) {
            router.push(`/invoices?batchId=${notification.referenceId}`);
        }
    };

    return (
        <div className="space-y-6 h-full flex flex-col">
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold tracking-tight">Bildirimler</h1>
                <Button
                    variant="outline"
                    onClick={() => markAllAsReadMutation.mutate()}
                    disabled={markAllAsReadMutation.isPending}
                >
                    <Check className="mr-2 h-4 w-4" /> Tümünü Okundu İşaretle
                </Button>
            </div>

            <div className="flex flex-col space-y-4">
                <div className="flex flex-col sm:flex-row gap-4 items-center justify-between">
                    <Tabs value={filterTab} onValueChange={(v: string) => { setFilterTab(v as "ALL" | "UNREAD" | "READ"); setPage(0); }} className="w-full sm:w-auto">
                        <TabsList>
                            <TabsTrigger value="ALL">Tümü</TabsTrigger>
                            <TabsTrigger value="UNREAD">Okunmamış</TabsTrigger>
                            <TabsTrigger value="READ">Okunmuş</TabsTrigger>
                        </TabsList>
                    </Tabs>

                    <div className="flex gap-2 w-full sm:w-auto">
                        <Select value={selectedType} onValueChange={(v: string) => { setSelectedType(v as NotificationType | "ALL"); setPage(0); }}>
                            <SelectTrigger className="w-[180px]">
                                <SelectValue placeholder="Tür" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="ALL">Tüm Türler</SelectItem>
                                <SelectItem value="EXTRACTION_COMPLETED">Veri Çıkarımı</SelectItem>
                                <SelectItem value="BATCH_COMPLETED">Toplu Yükleme</SelectItem>
                                <SelectItem value="LOW_CONFIDENCE">Güven Skoru</SelectItem>
                                <SelectItem value="HIGH_CONFIDENCE_AUTO_VERIFIED">Otomatik Doğrulama</SelectItem>
                                <SelectItem value="INVOICE_VERIFIED">Doğrulama</SelectItem>
                                <SelectItem value="SYSTEM_ANNOUNCEMENT">Sistem</SelectItem>
                            </SelectContent>
                        </Select>

                        <Select value={selectedSeverity} onValueChange={(v: string) => { setSelectedSeverity(v as NotificationSeverity | "ALL"); setPage(0); }}>
                            <SelectTrigger className="w-[150px]">
                                <SelectValue placeholder="Önem Derecesi" />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="ALL">Tüm Dereceler</SelectItem>
                                <SelectItem value="INFO">Bilgi (Info)</SelectItem>
                                <SelectItem value="WARNING">Uyarı (Warning)</SelectItem>
                                <SelectItem value="ERROR">Hata (Error)</SelectItem>
                                <SelectItem value="SUCCESS">Başarılı (Success)</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                </div>

                <div className="border rounded-md bg-card flex-1 min-h-[400px] flex flex-col">
                    {isLoading ? (
                        <div className="flex items-center justify-center flex-1 h-64">
                            <Loader2 className="h-8 w-8 animate-spin" />
                        </div>
                    ) : data?.content.length === 0 ? (
                        <div className="flex flex-col items-center justify-center flex-1 h-64 text-muted-foreground p-8">
                            <Bell className="h-12 w-12 mb-4 opacity-20" />
                            <p className="text-lg font-medium">Bildirim bulunamadı</p>
                            <p className="text-sm">Seçilen kriterlere uygun bildirim yok.</p>
                        </div>
                    ) : (
                        <div className="flex flex-col p-2 gap-2">
                            {data?.content.map((notification: Notification) => (
                                <NotificationItem
                                    key={notification.id}
                                    notification={notification}
                                    onClick={() => handleNotificationClick(notification)}
                                    onMarkAsRead={(e) => {
                                        markAsReadMutation.mutate(notification.id);
                                    }}
                                    onDelete={(e) => {
                                        deleteMutation.mutate(notification.id);
                                    }}
                                />
                            ))}
                        </div>
                    )}

                    {data && data.content.length > 0 && (
                        <div className="p-4 border-t mt-auto">
                            <DataTablePagination
                                pageIndex={data.number}
                                pageSize={data.size}
                                totalElements={data.totalElements}
                                totalPages={data.totalPages}
                                onPageChange={setPage}
                                onPageSizeChange={setSize}
                            />
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
