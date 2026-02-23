'use client';

import { ScrollText, Filter } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { useTranslations } from "next-intl"
import { useState, useEffect, useCallback } from "react"
import { auditLogService } from "@/services/audit-log-service"
import { AuditLogResponse } from "@/types/audit-log"
import { AuditLogsTable } from "./_components/audit-logs-table"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"

export default function AuditLogsPage() {
  const t = useTranslations('common.pages.auditLogs');
  const [logs, setLogs] = useState<AuditLogResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const loadLogs = useCallback(async () => {
    try {
      setLoading(true);
      const data = await auditLogService.getAuditLogs({}, page, 20);
      setLogs(data.content);
      setTotalPages(data.totalPages);
    } catch (error) {
      toast.error(t('messages.error'));
      console.error(error);
    } finally {
      setLoading(false);
    }
  }, [page, t]);

  useEffect(() => {
    loadLogs();
  }, [loadLogs]);

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
        actions={
          <Button variant="outline" onClick={loadLogs}>
            <Filter className="mr-2 h-4 w-4" />
            {t('table.filterAction')}
          </Button>
        }
      />
      
      {loading && logs.length === 0 ? (
        <div className="flex justify-center items-center py-12">
           <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
      ) : (
        <div className="space-y-4">
          <AuditLogsTable logs={logs} />
          
          {totalPages > 1 && (
            <div className="flex justify-between items-center mt-4">
              <Button 
                variant="outline" 
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0 || loading}
              >
                {t('buttons.back')}
              </Button>
              <span className="text-sm text-muted-foreground">
                {t('labels.showing', { from: page + 1, to: totalPages, total: totalPages })}
              </span>
              <Button 
                variant="outline" 
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || loading}
              >
                {t('buttons.next')}
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
