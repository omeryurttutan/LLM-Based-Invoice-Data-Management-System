import { Building2 } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { Button } from "@/components/ui/button"

export default function CompanyPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Şirket Bilgileri"
        description="Şirket detayları ve fatura ayarları."
        actions={
            <Button variant="outline">Düzenle</Button>
        }
      />
      <EmptyState
        icon={Building2}
        title="Şirket Profili"
        description="Şirketinizin vergi ve adres bilgileri burada yer alacak."
      />
    </div>
  )
}
