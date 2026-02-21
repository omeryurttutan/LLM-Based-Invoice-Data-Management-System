import {
  Building2,
  FileText,
  FolderOpen,
  LayoutDashboard,
  ScrollText,
  Settings,
  Upload,
  UserCircle,
  Users,
  Bell
} from "lucide-react"

export interface NavItem {
  title: string
  href: string
  icon: any
  badge?: string | number
  roles?: string[]
  section: "main" | "management" | "other"
}

export const navItems: NavItem[] = [
  // Main
  { title: "dashboard", href: "/dashboard", icon: LayoutDashboard, section: "main" },
  { title: "invoices", href: "/invoices", icon: FileText, section: "main" },
  { title: "upload", href: "/invoices/upload", icon: Upload, section: "main" },
  { title: "categories", href: "/categories", icon: FolderOpen, section: "main" },
  { title: "notifications", href: "/notifications", icon: Bell, section: "main" },

  // Management
  { title: "users", href: "/users", icon: Users, section: "management", roles: ["ADMIN"] },
  { title: "company", href: "/company", icon: Building2, section: "management", roles: ["ADMIN", "MANAGER"] },
  { title: "audit", href: "/audit-logs", icon: ScrollText, section: "management", roles: ["ADMIN", "MANAGER"] },

  // Other
  { title: "profile", href: "/profile", icon: UserCircle, section: "other" },
  { title: "settings", href: "/settings", icon: Settings, section: "other" },
]
