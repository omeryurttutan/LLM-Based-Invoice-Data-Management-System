'use client';

import { Building2 } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { Button } from "@/components/ui/button"
import { useTranslations } from "next-intl"

export default function CompanyPage() {
  const t = useTranslations('common.pages.company');

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
        actions={
            <Button variant="outline">{t('edit')}</Button>
        }
      />
      <EmptyState
        icon={Building2}
        title={t('profileTitle')}
        description={t('profileDescription')}
      />
    </div>
  )
}
