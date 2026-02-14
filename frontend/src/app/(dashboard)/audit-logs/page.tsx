import { ScrollText } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function AuditLogsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Denetim Logu"
        description="Sistemdeki tüm işlem geçmişini ve değişiklikleri izleyin."
      />
      <EmptyState
        icon={ScrollText}
        title="İşlem Geçmişi"
        description="Kullanıcı aktiviteleri ve sistem olayları burada listelenecek."
      />
    </div>
  )
}
