"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Slash } from "lucide-react"

import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"

export function Breadcrumbs() {
  const pathname = usePathname()
  const segments = pathname.split("/").filter((segment) => segment !== "")

  const segmentLabels: Record<string, string> = {
    invoices: "Faturalar",
    new: "Yeni",
    upload: "Yükle",
    categories: "Kategoriler",
    users: "Kullanıcılar",
    company: "Şirket",
    "audit-logs": "Denetim Logu",
    profile: "Profil",
    settings: "Ayarlar",
    edit: "Düzenle"
  }

  if (segments.length === 0) {
     return (
        <Breadcrumb>
            <BreadcrumbList>
                <BreadcrumbItem>
                    <BreadcrumbPage>Dashboard</BreadcrumbPage>
                </BreadcrumbItem>
            </BreadcrumbList>
        </Breadcrumb>
     )
  }

  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          <BreadcrumbLink href="/">Dashboard</BreadcrumbLink>
        </BreadcrumbItem>
        {segments.map((segment, index) => {
          const href = `/${segments.slice(0, index + 1).join("/")}`
          const isLast = index === segments.length - 1
          const label = segmentLabels[segment] || segment

          return (
            <div key={href} className="flex items-center">
              <BreadcrumbSeparator>
                <Slash />
              </BreadcrumbSeparator>
              <BreadcrumbItem className="ml-2">
                {isLast ? (
                  <BreadcrumbPage className="capitalize">{label}</BreadcrumbPage>
                ) : (
                  <BreadcrumbLink href={href} className="capitalize">{label}</BreadcrumbLink>
                )}
              </BreadcrumbItem>
            </div>
          )
        })}
      </BreadcrumbList>
    </Breadcrumb>
  )
}
