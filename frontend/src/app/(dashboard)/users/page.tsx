import { Users, UserPlus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/common/page-header"
import { EmptyState } from "@/components/common/empty-state"

export default function UsersPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Kullanıcılar"
        description="Sistem kullanıcılarını ve rollerini yönetin."
        actions={
          <Button>
            <UserPlus className="mr-2 h-4 w-4" />
            Yeni Kullanıcı
          </Button>
        }
      />
      <EmptyState
        icon={Users}
        title="Kullanıcı Listesi"
        description="Şirketinizdeki kayıtlı kullanıcılar burada listelenecek."
      />
    </div>
  )
}
