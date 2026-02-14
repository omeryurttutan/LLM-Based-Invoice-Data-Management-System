"use client"

import { useEffect } from "react"
import { AlertCircle, RefreshCcw } from "lucide-react"
import { Button } from "@/components/ui/button"

interface ErrorBoundaryProps {
  error: Error & { digest?: string }
  reset?: () => void
  title?: string
  description?: string
}

export function ErrorBoundary({ 
    error, 
    reset, 
    title = "Bir hata oluştu", 
    description = "İşleminiz sırasında beklenmedik bir hata meydana geldi."
}: ErrorBoundaryProps) {
  useEffect(() => {
    // Log the error to an error reporting service
    console.error(error)
  }, [error])

  return (
    <div className="flex h-full min-h-[400px] flex-col items-center justify-center space-y-4 text-center">
      <div className="flex h-20 w-20 items-center justify-center rounded-full bg-red-100 dark:bg-red-900/20">
        <AlertCircle className="h-10 w-10 text-red-600 dark:text-red-500" />
      </div>
      <div className="space-y-2">
        <h2 className="text-2xl font-bold tracking-tight">{title}</h2>
        <p className="text-muted-foreground max-w-sm mx-auto">
          {description}
        </p>
        <p className="text-xs text-muted-foreground font-mono bg-muted p-2 rounded">
            {error.message}
        </p>
      </div>
      {reset && (
        <Button onClick={reset} variant="outline" className="gap-2">
          <RefreshCcw className="h-4 w-4" />
          Tekrar Dene
        </Button>
      )}
    </div>
  )
}
