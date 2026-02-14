"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { usePathname } from "next/navigation"
import { 
  Briefcase, 
  Building2, 
  ChevronLeft, 
  FileText, 
  FolderOpen, 
  LayoutDashboard, 
  ScrollText, 
  Settings, 
  Upload, 
  UserCircle, 
  Users 
} from "lucide-react"

import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import { SidebarNavItem } from "@/components/layout/sidebar-nav-item"
import { useSidebarStore } from "@/stores/sidebar-store"

import { navItems } from "@/components/layout/nav-config"

// Mock role for now - Phase 11 will implement auth
const USER_ROLE = "ADMIN" 

export function Sidebar() {
  const pathname = usePathname()
  const { isCollapsed, toggle } = useSidebarStore()
  // Hydration check for persist middleware
  const [isMounted, setIsMounted] = useState(false)

  useEffect(() => {
    setIsMounted(true)
  }, [])

  if (!isMounted) {
    return null // or a skeleton
  }

  const filteredNavItems = navItems.filter(item => {
    if (!item.roles) return true
    return item.roles.includes(USER_ROLE)
  })

  return (
    <div
      className={cn(
        "relative flex flex-col border-r bg-card transition-all duration-300",
        isCollapsed ? "w-[64px]" : "w-[256px]"
      )}
    >
      <div className="flex h-16 items-center px-4 py-4">
        <div className={cn("flex items-center gap-2 font-bold text-xl", isCollapsed && "justify-center w-full")}>
           <Briefcase className="h-6 w-6 text-primary" />
           {!isCollapsed && <span className="truncate">Fatura OCR</span>}
        </div>
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
                     {section === "management" ? "Yönetim" : "Diğer"}
                   </span>
                )}
                
                {items.map((item) => (
                  <SidebarNavItem
                    key={item.href}
                    icon={item.icon}
                    label={item.title}
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
