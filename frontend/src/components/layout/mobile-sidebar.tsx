"use client"

import { useState } from "react"

import { usePathname } from "next/navigation"
import Link from "next/link"
import { Briefcase, Menu } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"
import { ScrollArea } from "@/components/ui/scroll-area"

import { navItems } from "@/components/layout/nav-config"
import { SidebarNavItem } from "@/components/layout/sidebar-nav-item"


export function MobileSidebar() {
  const [open, setOpen] = useState(false)
  const pathname = usePathname()
  const USER_ROLE = "ADMIN" // Mock

  const filteredNavItems = navItems.filter(item => {
    if (!item.roles) return true
    return item.roles.includes(USER_ROLE)
  })

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="ghost" size="icon" className="md:hidden">
          <Menu className="h-5 w-5" />
          <span className="sr-only">Toggle Menu</span>
        </Button>
      </SheetTrigger>
      <SheetContent side="left" className="pr-0 bg-card w-[300px] p-0">
        <Link href="/" className="flex items-center gap-2 px-6 py-4 font-bold text-xl border-b hover:opacity-80 transition-opacity">
           <Briefcase className="h-6 w-6 text-primary" />
           <span>Fatura OCR</span>
        </Link>
        <ScrollArea className="h-[calc(100vh-65px)] px-4 py-4">
           <div className="flex flex-col gap-4">
            {["main", "management", "other"].map((section) => {
                const items = filteredNavItems.filter((item) => item.section === section)
                if (items.length === 0) return null

                return (
                  <div key={section} className="flex flex-col gap-1">
                    <span className="px-2 text-xs font-semibold text-muted-foreground uppercase mb-1">
                        {section === "management" ? "Yönetim" : section === "other" ? "Diğer" : "Menü"}
                    </span>
                    {items.map((item) => (
                      <div key={item.href} onClick={() => setOpen(false)}>
                        <SidebarNavItem
                            icon={item.icon}
                            label={item.title}
                            href={item.href}
                            isActive={pathname === item.href}
                            isCollapsed={false}
                            badge={item.badge}
                        />
                      </div>
                    ))}
                  </div>
                )
             })}
           </div>
        </ScrollArea>
      </SheetContent>
    </Sheet>
  )
}
