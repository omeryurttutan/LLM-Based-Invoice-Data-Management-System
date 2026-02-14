import Link from "next/link"
import { FileQuestion } from "lucide-react"
import { Button } from "@/components/ui/button"

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen text-center px-4">
      <div className="flex h-20 w-20 items-center justify-center rounded-full bg-muted mb-6">
        <FileQuestion className="h-10 w-10 text-muted-foreground" />
      </div>
      <h2 className="text-2xl font-bold tracking-tight mb-2">Sayfa Bulunamadı</h2>
      <p className="text-muted-foreground mb-6 max-w-md">
        Aradığınız sayfa mevcut değil veya taşınmış olabilir. Lütfen adresi kontrol edin veya ana sayfaya dönün.
      </p>
      <Button asChild>
        <Link href="/">Dashboard&apos;a Dön</Link>
      </Button>
    </div>
  )
}
