import { UserCircle } from "lucide-react"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"
import { Button } from "@/components/ui/button"

export default function ProfilePage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Profil"
        description="Kişisel bilgilerinizi ve tercihlerinizi güncelleyin."
        actions={
            <Button>Kaydet</Button>
        }
      />
      <EmptyState
        icon={UserCircle}
        title="Kullanıcı Profili"
        description="Ad, soyad, e-posta ve şifre değiştirme formu burada olacak."
      />
    </div>
  )
}
