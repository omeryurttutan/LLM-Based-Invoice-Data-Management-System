"use client"

import Link from "next/link"
import { LucideIcon } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip"

interface SidebarNavItemProps {
  icon: LucideIcon
  label: string
  href: string
  isActive?: boolean
  isCollapsed?: boolean
  badge?: string | number
}

export function SidebarNavItem({
  icon: Icon,
  label,
  href,
  isActive,
  isCollapsed,
  badge,
}: SidebarNavItemProps) {
  if (isCollapsed) {
    return (
      <TooltipProvider>
        <Tooltip delayDuration={0}>
          <TooltipTrigger asChild>
            <Button
              variant={isActive ? "default" : "ghost"}
              size="icon"
              className={cn(
                "h-9 w-9",
                isActive && "bg-primary text-primary-foreground hover:bg-primary/90"
              )}
              asChild
            >
              <Link href={href}>
                <Icon className="h-4 w-4" />
                <span className="sr-only">{label}</span>
              </Link>
            </Button>
          </TooltipTrigger>
          <TooltipContent side="right" className="flex items-center gap-4">
            {label}
            {badge && (
              <span className="ml-auto text-muted-foreground">{badge}</span>
            )}
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    )
  }

  return (
    <Button
      variant={isActive ? "secondary" : "ghost"}
      className={cn(
        "w-full justify-start",
        isActive && "bg-secondary font-medium"
      )}
      asChild
    >
      <Link href={href}>
        <Icon className="mr-2 h-4 w-4" />
        {label}
        {badge && (
          <span className="ml-auto text-xs font-medium bg-primary/10 text-primary px-1.5 py-0.5 rounded-full">
            {badge}
          </span>
        )}
      </Link>
    </Button>
  )
}
