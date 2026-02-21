'use client';

import { Users, UserPlus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { useTranslations } from "next-intl"

export default function UsersPage() {
  const t = useTranslations('common.pages.users');

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title')}
        description={t('description')}
        actions={
          <Button>
            <UserPlus className="mr-2 h-4 w-4" />
            {t('newUser')}
          </Button>
        }
      />
      <EmptyState
        icon={Users}
        title={t('listTitle')}
        description={t('listDescription')}
      />
    </div>
  )
}
