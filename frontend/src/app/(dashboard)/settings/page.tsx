import { Settings } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function SettingsPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Ayarlar"
        description="Uygulama genel yapılandırması ve tercihler."
      />
      <EmptyState
        icon={Settings}
        title="Uygulama Ayarları"
        description="Tema, bildirimler ve dil seçenekleri burada yönetilecek."
      />
    </div>
  )
}
