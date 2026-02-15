"use client"

import { MobileSidebar } from "@/components/layout/mobile-sidebar"
import { Breadcrumbs } from "@/components/layout/breadcrumbs"
import { UserMenu } from "@/components/layout/user-menu"
import { Button } from "@/components/ui/button"
import { Bell } from "lucide-react"
import { NotificationDropdown } from "@/components/notifications/notification-dropdown"

export function Header() {
  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center gap-4 border-b bg-background px-4 md:px-6">
      <MobileSidebar />
      <div className="hidden md:flex">
        <Breadcrumbs />
      </div>
      <div className="flex flex-1 items-center justify-end gap-4">
        <NotificationDropdown />
        <UserMenu />
      </div>
    </header>
  )
}
