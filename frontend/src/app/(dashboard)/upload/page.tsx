'use client';

import { Upload } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { useTranslations } from "next-intl"

export default function UploadPage() {
  const t = useTranslations('common.pages.upload');

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
      />
      <EmptyState
        icon={Upload}
        title={t('moduleTitle')}
        description={t('moduleDescription')}
        action={{ label: t('selectFile'), onClick: () => alert("Demo: File selected") }}
      />
    </div>
  )
}
