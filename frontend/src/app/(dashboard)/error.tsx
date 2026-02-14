"use client"

import { ErrorBoundary } from "@/components/common/error-boundary"

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  return (
    <div className="flex h-full items-center justify-center p-6">
        <ErrorBoundary error={error} reset={reset} />
    </div>
  )
}
