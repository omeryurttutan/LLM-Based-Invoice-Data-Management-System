import { 
  Building2, 
  FileText, 
  FolderOpen, 
  LayoutDashboard, 
  ScrollText, 
  Settings, 
  Upload, 
  UserCircle, 
  Users 
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
  { title: "Dashboard", href: "/", icon: LayoutDashboard, section: "main" },
  { title: "Faturalar", href: "/invoices", icon: FileText, section: "main" },
  { title: "Fatura Yükle", href: "/upload", icon: Upload, section: "main" },
  { title: "Kategoriler", href: "/categories", icon: FolderOpen, section: "main" },
  
  // Management
  { title: "Kullanıcılar", href: "/users", icon: Users, section: "management", roles: ["ADMIN"] },
  { title: "Şirket", href: "/company", icon: Building2, section: "management", roles: ["ADMIN", "MANAGER"] },
  { title: "Denetim Logu", href: "/audit-logs", icon: ScrollText, section: "management", roles: ["ADMIN", "MANAGER"] },
  
  // Other
  { title: "Profil", href: "/profile", icon: UserCircle, section: "other" },
  { title: "Ayarlar", href: "/settings", icon: Settings, section: "other" },
]
