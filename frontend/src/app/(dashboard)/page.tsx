import { LayoutDashboard } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Fatura OCR sistemi genel bakış ve istatistikler."
      />
      <EmptyState
        icon={LayoutDashboard}
        title="Dashboard Hazırlanıyor"
        description="İstatistikler ve grafikler yakında burada görüntülenecek."
      />
    </div>
  )
}
