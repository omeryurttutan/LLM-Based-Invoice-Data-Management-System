'use client';

import { Settings } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { useTranslations } from "next-intl"

export default function SettingsPage() {
  const t = useTranslations('common.pages.settings');

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
      />
      <EmptyState
        icon={Settings}
        title={t('settingsTitle')}
        description={t('settingsDescription')}
      />
    </div>
  )
}
