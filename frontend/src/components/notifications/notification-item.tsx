import { formatDistanceToNow } from 'date-fns';
import { tr } from 'date-fns/locale';
import {
    CheckCircle,
    XCircle,
    AlertTriangle,
    Info,
    ShieldCheck,
    AlertOctagon,
    CheckCircle2,
    Trash2
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { Notification, NotificationSeverity, NotificationType } from '@/types/notification';
import { Button } from '@/components/ui/button';

interface NotificationItemProps {
    notification: Notification;
    onClick?: () => void;
    onMarkAsRead?: (e: React.MouseEvent) => void;
    onDelete?: (e: React.MouseEvent) => void;
    compact?: boolean; // For dropdown
}

export function NotificationItem({
    notification,
    onClick,
    onMarkAsRead,
    onDelete,
    compact = false
}: NotificationItemProps) {

    const getIcon = (type: NotificationType, severity: NotificationSeverity) => {
        switch (type) {
            case 'EXTRACTION_COMPLETED':
            case 'INVOICE_VERIFIED':
                return <CheckCircle className="h-5 w-5 text-green-500" />;
            case 'EXTRACTION_FAILED':
            case 'INVOICE_REJECTED':
                return <XCircle className="h-5 w-5 text-red-500" />;
            case 'BATCH_COMPLETED':
                return <CheckCircle2 className="h-5 w-5 text-green-500" />;
            case 'BATCH_PARTIALLY_COMPLETED':
            case 'LOW_CONFIDENCE':
            case 'PROVIDER_DEGRADED':
                return <AlertTriangle className="h-5 w-5 text-yellow-500" />;
            case 'HIGH_CONFIDENCE_AUTO_VERIFIED':
                return <ShieldCheck className="h-5 w-5 text-green-600" />;
            case 'ALL_PROVIDERS_DOWN':
                return <AlertOctagon className="h-5 w-5 text-red-600" />;
            case 'SYSTEM_ANNOUNCEMENT':
            default:
                return <Info className="h-5 w-5 text-blue-500" />;
        }
    };

    const getTypeLabel = (type: NotificationType) => {
        switch (type) {
            case 'EXTRACTION_COMPLETED':
            case 'EXTRACTION_FAILED':
                return 'Veri Çıkarımı';
            case 'BATCH_COMPLETED':
            case 'BATCH_PARTIALLY_COMPLETED':
                return 'Toplu Yükleme';
            case 'LOW_CONFIDENCE':
                return 'Güven Skoru';
            case 'HIGH_CONFIDENCE_AUTO_VERIFIED':
                return 'Otomatik Doğrulama';
            case 'INVOICE_VERIFIED':
            case 'INVOICE_REJECTED':
                return 'Doğrulama';
            case 'PROVIDER_DEGRADED':
            case 'ALL_PROVIDERS_DOWN':
            case 'SYSTEM_ANNOUNCEMENT':
                return 'Sistem';
            default:
                return 'Bildirim';
        }
    };

    // Format date
    const timeAgo = formatDistanceToNow(new Date(notification.createdAt), {
        addSuffix: true,
        locale: tr
    });

    return (
        <div
            className={cn(
                "relative flex gap-3 p-4 transition-colors hover:bg-muted/50 cursor-pointer group",
                !notification.read && "bg-muted/30",
                compact ? "py-3 px-4 border-b last:border-0" : "rounded-lg border mb-2"
            )}
            onClick={onClick}
        >
            {/* Unread Indicator */}
            {!notification.read && (
                <div className="absolute left-0 top-0 bottom-0 w-1 bg-blue-500 rounded-l-lg" />
            )}

            {/* Icon */}
            <div className="mt-1 flex-shrink-0">
                {getIcon(notification.type, notification.severity)}
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between gap-2">
                    <div className="flex flex-col gap-0.5">
                        {!compact && (
                            <span className={cn(
                                "text-xs font-medium px-2 py-0.5 rounded-full w-fit mb-1",
                                notification.severity === 'SUCCESS' && "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
                                notification.severity === 'WARNING' && "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
                                notification.severity === 'ERROR' && "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
                                notification.severity === 'INFO' && "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
                            )}>
                                {getTypeLabel(notification.type)}
                            </span>
                        )}
                        <p className={cn("text-sm font-semibold leading-none", compact && "truncate")}>
                            {notification.title}
                        </p>
                        <p className={cn("text-sm text-muted-foreground mt-1", compact && "line-clamp-2")}>
                            {notification.message}
                        </p>
                        <span className="text-xs text-muted-foreground mt-1 block">
                            {timeAgo}
                        </span>
                    </div>
                </div>
            </div>

            {/* Actions (Only for full view or if explicitly added) */}
            {!compact && (
                <div className="flex flex-col gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    {onMarkAsRead && !notification.read && (
                        <Button variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); onMarkAsRead(e); }}>
                            Okundu
                        </Button>
                    )}
                    {onDelete && (
                        <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive" onClick={(e) => { e.stopPropagation(); onDelete(e); }}>
                            <Trash2 className="h-4 w-4" />
                        </Button>
                    )}
                </div>
            )}

            {/* Unread Dot for Compact View */}
            {compact && !notification.read && (
                <div className="flex-shrink-0 self-center">
                    <div className="h-2 w-2 rounded-full bg-blue-500" />
                </div>
            )}
        </div>
    );
}
