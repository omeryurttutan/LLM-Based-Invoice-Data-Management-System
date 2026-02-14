import { FileText } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function EditInvoicePage({ params }: { params: { id: string } }) {
  return (
    <div className="space-y-6">
      <PageHeader
        title={`Faturayı Düzenle #${params.id}`}
        description="Mevcut fatura bilgilerini güncelleyin."
      />
      <EmptyState
        icon={FileText}
        title="Düzenleme Formu"
        description={`Fatura ID: ${params.id} düzenleme formu burada yer alacak.`}
      />
    </div>
  )
}
