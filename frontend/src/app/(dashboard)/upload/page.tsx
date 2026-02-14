import { Upload } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function UploadPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Fatura Yükle"
        description="Fatura görüntülerini veya PDF dosyalarını sisteme yükleyin."
      />
      <EmptyState
        icon={Upload}
        title="Yükleme Modülü"
        description="Dosya sürükle-bırak ve OCR işleme alanı burada olacak."
        action={{ label: "Dosya Seç (Demo)", onClick: () => alert("Demo: Dosya seçildi") }}
      />
    </div>
  )
}
