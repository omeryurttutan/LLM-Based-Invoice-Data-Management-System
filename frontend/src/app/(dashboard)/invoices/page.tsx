import Link from "next/link"
import { FileText, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function InvoicesPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Faturalar"
        description="Tüm faturaları listeleyin, ekleyin ve yönetin."
        actions={
          <Button asChild>
            <Link href="/invoices/new">
                <Plus className="mr-2 h-4 w-4" />
                Yeni Fatura
            </Link>
          </Button>
        }
      />
      <EmptyState
        icon={FileText}
        title="Henüz fatura yok"
        description="İlk faturanızı ekleyerek başlayın veya sistemden veri çekin."
        action={{ label: "Yeni Fatura Oluştur", href: "/invoices/new" }}
      />
    </div>
  )
}
