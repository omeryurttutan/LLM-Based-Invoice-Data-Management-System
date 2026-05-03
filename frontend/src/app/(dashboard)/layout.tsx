import { Header } from "@/components/layout/header"
import { Sidebar } from "@/components/layout/sidebar"
import { WebSocketProvider } from "@/components/providers/websocket-provider"

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex min-h-screen w-full flex-col bg-muted/40 md:flex-row md:bg-background">
      <WebSocketProvider />
      <div className="hidden border-r bg-background md:block">
        <Sidebar />
      </div>
      <div className="flex flex-col flex-1 min-h-screen">
        <Header />
        <main className="flex flex-1 flex-col gap-4 p-4 md:gap-8 md:p-6 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  )
}
