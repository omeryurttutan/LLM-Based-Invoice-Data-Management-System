import { FileText } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { Button } from "@/components/ui/button"
import Link from "next/link"

export default function InvoiceDetailPage({ params }: { params: { id: string } }) {
  return (
    <div className="space-y-6">
      <PageHeader
        title={`Fatura #${params.id}`}
        description="Fatura detayları ve kalem bilgileri."
        actions={
            <Button variant="outline" asChild>
                <Link href={`/invoices/${params.id}/edit`}>Düzenle</Link>
            </Button>
        }
      />
      <EmptyState
        icon={FileText}
        title="Fatura Detayı"
        description={`Fatura ID: ${params.id} detayları burada görüntülenecek.`}
      />
    </div>
  )
}
