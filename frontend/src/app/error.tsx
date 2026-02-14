"use client"

import { ErrorBoundary } from "@/components/common/error-boundary"

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <html>
      <body>
        <div className="flex min-h-screen items-center justify-center">
            <ErrorBoundary 
                error={error} 
                reset={reset} 
                title="Kritik Hata"
                description="Uygulama çalışmasını engelleyen bir hata oluştu."
            />
        </div>
      </body>
    </html>
  )
}
