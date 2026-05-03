import { useTranslations } from 'next-intl';
import { AuditLogResponse } from '@/types/audit-log';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { format } from 'date-fns';
import { tr, enUS } from 'date-fns/locale';
import { useParams } from 'next/navigation';

interface AuditLogsTableProps {
  logs: AuditLogResponse[];
}

export function AuditLogsTable({ logs }: AuditLogsTableProps) {
  const t = useTranslations('common.pages.auditLogs');
  const params = useParams();
  const locale = params?.locale === 'tr' ? tr : enUS;

  const formatDate = (dateString: string) => {
    try {
      return format(new Date(dateString), 'dd MMM yyyy HH:mm', { locale });
    } catch {
      return dateString;
    }
  };

  const getActionColor = (action?: string) => {
    const a = action || '';
    if (a.includes('CREATE') || a.includes('LOGIN')) return 'bg-green-100 text-green-800';
    if (a.includes('DELETE') || a.includes('FAIL')) return 'bg-red-100 text-red-800';
    if (a.includes('UPDATE')) return 'bg-blue-100 text-blue-800';
    return 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{t('table.action')}</TableHead>
            <TableHead>{t('table.entity')}</TableHead>
            <TableHead>{t('table.user')}</TableHead>
            <TableHead>{t('table.details')}</TableHead>
            <TableHead>{t('table.date')}</TableHead>
            <TableHead>{t('table.ip')}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {!logs || logs.length === 0 ? (
            <TableRow>
              <TableCell colSpan={6} className="text-center h-24 text-muted-foreground">
                {t('table.noLogs')}
              </TableCell>
            </TableRow>
          ) : (
            logs?.map((log) => (
              <TableRow key={log.id}>
                <TableCell>
                  <Badge variant="outline" className={`font-mono text-xs ${getActionColor(log.actionType)}`}>
                    {log.actionType || '-'}
                  </Badge>
                </TableCell>
                <TableCell className="font-mono text-xs">
                  {log.entityType}
                  <br />
                  <span className="text-gray-400 truncate w-32 inline-block" title={log.entityId}>
                    {log.entityId}
                  </span>
                </TableCell>
                <TableCell>
                  <span title={log.userId}>{log.userEmail}</span>
                </TableCell>
                <TableCell className="max-w-[200px] truncate" title={log.description}>
                  {log.description || '-'}
                </TableCell>
                <TableCell className="whitespace-nowrap">
                  {formatDate(log.createdAt)}
                </TableCell>
                <TableCell className="text-muted-foreground text-xs">
                  {log.ipAddress}
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </div>
  );
}
