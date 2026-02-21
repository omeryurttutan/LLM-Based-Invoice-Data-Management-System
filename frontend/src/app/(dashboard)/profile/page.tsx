'use client';

import { UserCircle } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { Button } from "@/components/ui/button"
import { useTranslations } from "next-intl"

export default function ProfilePage() {
  const t = useTranslations('common.pages.profile');

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
        actions={
            <Button>{t('save')}</Button>
        }
      />
      <EmptyState
        icon={UserCircle}
        title={t('profileTitle')}
        description={t('profileDescription')}
      />
    </div>
  )
}
