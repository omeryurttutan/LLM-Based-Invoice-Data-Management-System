import { Briefcase } from "lucide-react"

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 dark:bg-gray-900 p-4">
      <div className="w-full max-w-md space-y-8">
        <div className="flex flex-col items-center">
            <div className="flex items-center gap-2 font-bold text-2xl mb-2">
                <Briefcase className="h-8 w-8 text-primary" />
                <span>Fatura OCR</span>
            </div>
            <p className="text-center text-sm text-muted-foreground">
                Veri Yönetim Sistemi
            </p>
        </div>
        
        {children}

        <div className="text-center text-xs text-muted-foreground mt-8">
             &copy; 2026 Fatura OCR Sistemi
        </div>
      </div>
    </div>
  )
}
