import { FileText } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function NewInvoicePage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Yeni Fatura"
        description="Manuel olarak yeni bir fatura kaydı oluşturun."
      />
      <EmptyState
        icon={FileText}
        title="Fatura Formu"
        description="Fatura giriş formu burada yer alacak."
      />
    </div>
  )
}
