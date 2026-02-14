import { FolderOpen, Plus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function CategoriesPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Kategoriler"
        description="Fatura kategorilerini tanımlayın ve yönetin."
        actions={
          <Button variant="outline">
            <Plus className="mr-2 h-4 w-4" />
            Yeni Kategori
          </Button>
        }
      />
      <EmptyState
        icon={FolderOpen}
        title="Kategori Listesi"
        description="Sistemde tanımlı kategoriler burada listelenecek."
      />
    </div>
  )
}
