"use client"

import { useEffect, useState } from "react"

import { usePathname } from "next/navigation"
import Link from "next/link"
import {
  Briefcase,
  ChevronLeft
} from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import { SidebarNavItem } from "@/components/layout/sidebar-nav-item"
import { useSidebarStore } from "@/stores/sidebar-store"
import { useAuthStore } from "@/stores/auth-store"
import { navItems } from "@/components/layout/nav-config"
import { useTranslations } from "next-intl"

export function Sidebar() {
  const pathname = usePathname()
  const { isCollapsed, toggle } = useSidebarStore()
  const { user } = useAuthStore()
  // Hydration check for persist middleware
  const [isMounted, setIsMounted] = useState(false)
  const t = useTranslations('navigation.sidebar')

  useEffect(() => {
    setIsMounted(true)
  }, [])

  if (!isMounted) {
    return null // or a skeleton
  }

  const filteredNavItems = navItems.filter(item => {
    if (!item.roles) return true
    if (!user || !user.role) return false
    return item.roles.includes(user.role)
  })

  return (
    <div
      className={cn(
        "relative flex flex-col border-r bg-card transition-all duration-300",
        isCollapsed ? "w-[64px]" : "w-[256px]"
      )}
    >
      <div className="flex h-16 items-center px-4 py-4">
        <Link href="/" className={cn("flex items-center gap-2 font-bold text-xl hover:opacity-80 transition-opacity", isCollapsed && "justify-center w-full")}>
          <Briefcase className="h-6 w-6 text-primary" />
          {!isCollapsed && <span className="truncate">Fatura OCR</span>}
        </Link>
      </div>

      <Separator />

      <ScrollArea className="flex-1 py-4">
        <nav className="grid gap-2 px-2">
          {["main", "management", "other"].map((section, index) => {
            const items = filteredNavItems.filter((item) => item.section === section)
            if (items.length === 0) return null

            return (
              <div key={section} className="flex flex-col gap-1">
                {!isCollapsed && index > 0 && <Separator className="my-2" />}
                {!isCollapsed && index > 0 && (
                  <span className="px-2 text-xs font-semibold text-muted-foreground uppercase mb-1">
                    {section === "management" ? t('management') : (section === "other" ? t('other') : "")}
                  </span>
                )}

                {items.map((item) => (
                  <SidebarNavItem
                    key={item.href}
                    icon={item.icon}
                    label={t(item.title)}
                    href={item.href}
                    isActive={pathname === item.href}
                    isCollapsed={isCollapsed}
                    badge={item.badge}
                  />
                ))}
              </div>
            )
          })}
        </nav>
      </ScrollArea>

      <div className="p-2 border-t mt-auto">
        <Button
          variant="ghost"
          size="icon"
          className="w-full h-9"
          onClick={toggle}
        >
          <ChevronLeft className={cn("h-4 w-4 transition-transform", isCollapsed && "rotate-180")} />
        </Button>
      </div>
    </div>
  )
}
