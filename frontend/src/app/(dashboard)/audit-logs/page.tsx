'use client';

import { ScrollText } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { useTranslations } from "next-intl"

export default function AuditLogsPage() {
  const t = useTranslations('common.pages.auditLogs');

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
      />
      <EmptyState
        icon={ScrollText}
        title={t('historyTitle')}
        description={t('historyDescription')}
      />
    </div>
  )
}
