# PHASE 10: FRONTEND — LAYOUT AND ROUTING

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Backend**: Java 17 + Spring Boot 3.2 (Hexagonal Architecture) — **running on port 8080**
- **Frontend**: Next.js 14+ (App Router, TypeScript, Tailwind CSS, Shadcn/ui) — **running on port 3000**
- **Database**: PostgreSQL 15+

### Current State
**Phases 0-9 have been completed:**
- ✅ Phase 0: Docker Compose environment — Frontend skeleton already created (Next.js 14+, TypeScript, Tailwind CSS). Basic `frontend/` directory exists with `package.json`, `tsconfig.json`, `tailwind.config.ts`, `Dockerfile`, and a minimal `src/app/` structure.
- ✅ Phase 1: CI/CD Pipeline with GitHub Actions (includes frontend lint + build steps)
- ✅ Phase 2: Hexagonal Architecture (backend)
- ✅ Phase 3: Database schema (all tables created)
- ✅ Phase 4: JWT Authentication API — `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- ✅ Phase 5: RBAC with 4 roles (ADMIN, MANAGER, ACCOUNTANT, INTERN)
- ✅ Phase 6: Company & User Management API
- ✅ Phase 7: Invoice CRUD API (with categories, status workflow, items)
- ✅ Phase 8: Audit Log Mechanism
- ✅ Phase 9: Duplication Control

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Build the complete frontend application shell with layout, navigation, and routing using Next.js 14+ App Router. This phase establishes the visual skeleton of the entire application: a professional sidebar-based layout, top header bar, responsive design, page routing for all major sections, loading/error states, and the foundational infrastructure (API client, theme, providers) that all subsequent frontend phases will build upon. No backend integration yet — pages will use placeholder/mock content.

---

## DETAILED REQUIREMENTS

### 1. Project Setup & Dependencies

**IMPORTANT**: A basic Next.js project already exists from Phase 0 in the `frontend/` directory. You are EXTENDING it, not creating from scratch.

**1.1 Install Required Dependencies**:

```bash
cd frontend

# UI Components
npx shadcn-ui@latest init
# Choose: TypeScript, Tailwind, src/ directory, @/components alias

# Install Shadcn components needed for layout
npx shadcn-ui@latest add button
npx shadcn-ui@latest add dropdown-menu
npx shadcn-ui@latest add avatar
npx shadcn-ui@latest add separator
npx shadcn-ui@latest add sheet          # For mobile sidebar
npx shadcn-ui@latest add skeleton
npx shadcn-ui@latest add tooltip
npx shadcn-ui@latest add badge
npx shadcn-ui@latest add toast
npx shadcn-ui@latest add sonner         # Toast alternative (pick one)
npx shadcn-ui@latest add scroll-area
npx shadcn-ui@latest add breadcrumb

# State management & data fetching
npm install zustand @tanstack/react-query axios

# Icons
npm install lucide-react

# Theme
npm install next-themes

# Form handling (for future phases, install now)
npm install react-hook-form @hookform/resolvers zod

# Date utilities
npm install date-fns
```

**1.2 Existing package.json Dependencies** (from Phase 0, already installed):
- `next`, `react`, `react-dom`, `typescript`, `tailwindcss`, `postcss`, `autoprefixer`, `eslint`
- `@tanstack/react-query`, `zustand`, `axios`, `clsx`, `tailwind-merge`

---

### 2. Directory Structure (Feature-Based)

Reorganize and extend the `frontend/src/` directory:

```
frontend/src/
├── app/                              # Next.js App Router pages
│   ├── (auth)/                       # Auth layout group (no sidebar)
│   │   ├── login/
│   │   │   └── page.tsx
│   │   ├── register/
│   │   │   └── page.tsx
│   │   └── layout.tsx                # Centered auth layout
│   │
│   ├── (dashboard)/                  # Main app layout group (with sidebar)
│   │   ├── layout.tsx                # Sidebar + header layout
│   │   ├── page.tsx                  # Dashboard home (redirect or content)
│   │   ├── invoices/
│   │   │   ├── page.tsx              # Invoice list
│   │   │   ├── new/
│   │   │   │   └── page.tsx          # Create invoice
│   │   │   └── [id]/
│   │   │       ├── page.tsx          # Invoice detail
│   │   │       └── edit/
│   │   │           └── page.tsx      # Edit invoice
│   │   ├── upload/
│   │   │   └── page.tsx              # File upload (future)
│   │   ├── categories/
│   │   │   └── page.tsx              # Category management
│   │   ├── users/
│   │   │   └── page.tsx              # User management (ADMIN)
│   │   ├── company/
│   │   │   └── page.tsx              # Company settings
│   │   ├── audit-logs/
│   │   │   └── page.tsx              # Audit logs (ADMIN/MANAGER)
│   │   ├── profile/
│   │   │   └── page.tsx              # User profile
│   │   └── settings/
│   │       └── page.tsx              # App settings
│   │
│   ├── layout.tsx                    # Root layout (providers)
│   ├── globals.css                   # Global styles
│   ├── not-found.tsx                 # 404 page
│   └── error.tsx                     # Global error boundary
│
├── components/
│   ├── ui/                           # Shadcn/ui components (auto-generated)
│   ├── layout/
│   │   ├── sidebar.tsx               # Main sidebar navigation
│   │   ├── sidebar-nav-item.tsx      # Individual nav item
│   │   ├── header.tsx                # Top header bar
│   │   ├── user-menu.tsx             # User dropdown in header
│   │   ├── mobile-sidebar.tsx        # Mobile sidebar (Sheet)
│   │   ├── breadcrumbs.tsx           # Dynamic breadcrumbs
│   │   └── theme-toggle.tsx          # Dark/light mode toggle
│   ├── common/
│   │   ├── page-header.tsx           # Reusable page header (title + actions)
│   │   ├── loading-skeleton.tsx      # Full-page loading skeleton
│   │   ├── empty-state.tsx           # Empty state component
│   │   ├── error-boundary.tsx        # Error boundary wrapper
│   │   ├── confirm-dialog.tsx        # Confirmation dialog
│   │   └── role-gate.tsx             # Role-based UI gate
│   └── providers/
│       ├── query-provider.tsx        # TanStack Query provider
│       ├── theme-provider.tsx        # next-themes provider
│       └── toast-provider.tsx        # Toast/Sonner provider
│
├── lib/
│   ├── utils.ts                      # Utility functions (cn, formatDate, etc.)
│   ├── constants.ts                  # App-wide constants
│   └── validations.ts               # Zod schemas (shared)
│
├── services/
│   ├── api-client.ts                 # Axios instance with interceptors
│   └── endpoints.ts                  # API endpoint constants
│
├── stores/
│   ├── auth-store.ts                 # Zustand auth store (skeleton)
│   ├── sidebar-store.ts             # Sidebar open/collapsed state
│   └── theme-store.ts               # Theme preferences (if not using next-themes)
│
├── types/
│   ├── index.ts                      # Common types
│   ├── auth.ts                       # Auth-related types
│   ├── invoice.ts                    # Invoice types (matching backend DTOs)
│   ├── company.ts                    # Company types
│   ├── user.ts                       # User types
│   └── api.ts                        # API response wrapper types
│
└── hooks/
    ├── use-auth.ts                   # Auth hook (wraps store)
    ├── use-sidebar.ts                # Sidebar state hook
    └── use-media-query.ts            # Responsive breakpoint hook
```

---

### 3. Root Layout and Providers

**File**: `src/app/layout.tsx`

The root layout wraps the entire application with necessary providers:

```tsx
// Root layout provides:
// 1. ThemeProvider (next-themes) for dark/light mode
// 2. QueryProvider (TanStack Query)
// 3. ToastProvider (Sonner or Shadcn toast)
// 4. HTML lang="tr" (Turkish)
// 5. Inter or Geist font (Google Fonts)
```

Key points:
- Set `<html lang="tr">` for Turkish locale
- Use `suppressHydrationWarning` on html tag for theme
- Import global CSS
- Use a clean, professional font (Inter recommended)

---

### 4. Dashboard Layout (Sidebar + Header)

**File**: `src/app/(dashboard)/layout.tsx`

This is the main application layout with:
- **Sidebar** (left, fixed/sticky, collapsible)
- **Header** (top, sticky)
- **Main content area** (scrollable)

```
┌──────────────────────────────────────────────────┐
│ Header (sticky top)                    [User ▾]  │
├──────────┬───────────────────────────────────────┤
│          │                                       │
│ Sidebar  │  Main Content Area                    │
│ (fixed)  │  (scrollable)                         │
│          │                                       │
│ ☰ Logo   │  ┌─ Breadcrumbs ──────────────┐      │
│          │  │                             │      │
│ 📊 Panel │  │  Page Header               │      │
│ 📄 Fatura│  │  + Action Buttons           │      │
│ 📤 Yükle │  │                             │      │
│ 📁 Kateg.│  │  Page Content               │      │
│ 👥 Kull. │  │  ...                        │      │
│ 🏢 Şirket│  │                             │      │
│ 📋 Audit │  │                             │      │
│ ⚙ Ayarlar│  └─────────────────────────────┘      │
│          │                                       │
└──────────┴───────────────────────────────────────┘
```

**Layout behavior**:
- Desktop (≥1024px): Sidebar visible, can be collapsed to icon-only mode
- Tablet (768-1023px): Sidebar collapsed by default (icon-only)
- Mobile (<768px): Sidebar hidden, accessible via hamburger menu (Sheet overlay)

---

### 5. Sidebar Navigation

**File**: `src/components/layout/sidebar.tsx`

**5.1 Sidebar Structure**:

```
┌─────────────────┐
│  🧾 Fatura OCR  │  ← Logo/brand (collapsible: show icon only)
│                 │
├─────────────────┤
│ ANA MENÜ        │  ← Section label
│                 │
│ 📊 Dashboard    │  ← /
│ 📄 Faturalar    │  ← /invoices
│ 📤 Fatura Yükle │  ← /upload
│ 📁 Kategoriler  │  ← /categories
│                 │
├─────────────────┤
│ YÖNETİM        │  ← Section label (visible based on role)
│                 │
│ 👥 Kullanıcılar │  ← /users (ADMIN only)
│ 🏢 Şirket      │  ← /company (ADMIN, MANAGER)
│ 📋 Denetim Log │  ← /audit-logs (ADMIN, MANAGER)
│                 │
├─────────────────┤
│ DİĞER          │
│                 │
│ 👤 Profil       │  ← /profile
│ ⚙ Ayarlar      │  ← /settings
│                 │
├─────────────────┤
│                 │
│  [<< Collapse]  │  ← Toggle collapse/expand
└─────────────────┘
```

**5.2 Navigation Items Configuration**:

```typescript
type NavItem = {
  title: string;            // Turkish display name
  href: string;             // Route path
  icon: LucideIcon;         // Lucide icon component
  badge?: string | number;  // Optional notification badge
  roles?: Role[];           // Visible to these roles (undefined = all)
  section: 'main' | 'management' | 'other';
};

const navItems: NavItem[] = [
  // Main section
  { title: 'Dashboard', href: '/', icon: LayoutDashboard, section: 'main' },
  { title: 'Faturalar', href: '/invoices', icon: FileText, section: 'main' },
  { title: 'Fatura Yükle', href: '/upload', icon: Upload, section: 'main' },
  { title: 'Kategoriler', href: '/categories', icon: FolderOpen, section: 'main' },
  
  // Management section (role-restricted)
  { title: 'Kullanıcılar', href: '/users', icon: Users, section: 'management', roles: ['ADMIN'] },
  { title: 'Şirket', href: '/company', icon: Building2, section: 'management', roles: ['ADMIN', 'MANAGER'] },
  { title: 'Denetim Logu', href: '/audit-logs', icon: ScrollText, section: 'management', roles: ['ADMIN', 'MANAGER'] },
  
  // Other section
  { title: 'Profil', href: '/profile', icon: UserCircle, section: 'other' },
  { title: 'Ayarlar', href: '/settings', icon: Settings, section: 'other' },
];
```

**5.3 Sidebar Features**:
- Active state: highlight current route item with accent color/background
- Collapsed mode: show only icons with tooltips on hover
- Smooth transition animation when collapsing/expanding
- Persist collapsed state in localStorage or Zustand store
- Section labels hidden when collapsed
- Badge support (e.g., pending invoice count)
- Role-based visibility: hide nav items the user's role cannot access

---

### 6. Header Bar

**File**: `src/components/layout/header.tsx`

**6.1 Header Structure**:

```
┌──────────────────────────────────────────────────────────┐
│ [☰]  Breadcrumbs: Dashboard / Faturalar    🔔  [Avatar ▾]│
└──────────────────────────────────────────────────────────┘
```

Components:
- **Mobile menu button** (☰): visible only on mobile, opens sidebar Sheet
- **Breadcrumbs**: dynamic, based on current route
- **Notification bell** (🔔): placeholder for future Phase 27 (notification system), show a badge with count
- **User menu** (Avatar + dropdown):
  - User name and email
  - Role badge (e.g., "ADMIN", "MANAGER")
  - Company name
  - Profile link
  - Settings link
  - Theme toggle (dark/light)
  - Logout button

**6.2 User Menu Dropdown**:

```
┌───────────────────────┐
│ Ömer Talha Yurttutan  │
│ omer@sirket.com       │
│ ┌──────────┐          │
│ │  ADMIN   │          │
│ └──────────┘          │
│ ABC Teknoloji Ltd.    │
├───────────────────────┤
│ 👤 Profil             │
│ ⚙  Ayarlar            │
│ 🌙 Karanlık Mod       │ ← Toggle
├───────────────────────┤
│ 🚪 Çıkış Yap         │
└───────────────────────┘
```

---

### 7. Auth Layout

**File**: `src/app/(auth)/layout.tsx`

A separate layout for login/register pages — NO sidebar, NO header. Clean centered layout:

```
┌──────────────────────────────────────────┐
│                                          │
│              🧾 Fatura OCR               │
│         Veri Yönetim Sistemi             │
│                                          │
│        ┌─────────────────────┐           │
│        │                     │           │
│        │  Login / Register   │           │
│        │  Form Card          │           │
│        │                     │           │
│        └─────────────────────┘           │
│                                          │
│        © 2026 Fatura OCR Sistemi         │
└──────────────────────────────────────────┘
```

Features:
- Centered card layout
- Optional background pattern or gradient
- Logo at top
- Footer with copyright
- No sidebar, no app header
- Responsive: full-width card on mobile, contained on desktop

---

### 8. Placeholder Pages

Create placeholder pages for ALL routes. Each page should have a `PageHeader` component with the page title and a brief "Coming soon" or placeholder message. This ensures all routing works before content is implemented in later phases.

**Each placeholder page pattern**:
```tsx
export default function InvoicesPage() {
  return (
    <div className="space-y-6">
      <PageHeader
        title="Faturalar"
        description="Tüm faturaları listeleyin, ekleyin ve yönetin."
        actions={
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Yeni Fatura
          </Button>
        }
      />
      {/* Placeholder content - will be replaced in Phase 12 */}
      <EmptyState
        icon={FileText}
        title="Henüz fatura yok"
        description="İlk faturanızı ekleyerek başlayın."
        action={{ label: "Fatura Ekle", href: "/invoices/new" }}
      />
    </div>
  );
}
```

**Pages to create** (all with Turkish text):

| Route | Page Title (TR) | Description (TR) |
|-------|-----------------|-------------------|
| `/` | Dashboard | Genel bakış ve istatistikler |
| `/invoices` | Faturalar | Tüm faturaları listeleyin, ekleyin ve yönetin |
| `/invoices/new` | Yeni Fatura | Manuel fatura oluşturun |
| `/invoices/[id]` | Fatura Detayı | Fatura bilgileri ve kalem detayları |
| `/invoices/[id]/edit` | Fatura Düzenle | Fatura bilgilerini güncelleyin |
| `/upload` | Fatura Yükle | Fatura görüntüsü veya PDF yükleyin |
| `/categories` | Kategoriler | Fatura kategorilerini yönetin |
| `/users` | Kullanıcılar | Sistem kullanıcılarını yönetin |
| `/company` | Şirket Bilgileri | Şirket ayarlarını düzenleyin |
| `/audit-logs` | Denetim Logu | Sistem değişiklik geçmişi |
| `/profile` | Profil | Profil bilgilerinizi güncelleyin |
| `/settings` | Ayarlar | Uygulama ayarları |
| `/login` | Giriş Yap | Sisteme giriş yapın |
| `/register` | Kayıt Ol | Yeni hesap oluşturun |

---

### 9. Reusable Components

**9.1 PageHeader** (`src/components/common/page-header.tsx`):
- Title (h1)
- Optional description text
- Optional action buttons (right-aligned)
- Responsive: stack on mobile

**9.2 EmptyState** (`src/components/common/empty-state.tsx`):
- Icon
- Title
- Description
- Optional action button/link

**9.3 LoadingSkeleton** (`src/components/common/loading-skeleton.tsx`):
- Full page skeleton with header + content placeholders
- Table skeleton variant
- Card skeleton variant
- Use Shadcn Skeleton component

**9.4 ErrorBoundary** (`src/components/common/error-boundary.tsx`):
- Catches React errors
- Shows friendly error message in Turkish
- "Tekrar Dene" (Try Again) button
- Option to go back to dashboard

**9.5 RoleGate** (`src/components/common/role-gate.tsx`):
```tsx
// Usage: <RoleGate allowedRoles={['ADMIN', 'MANAGER']}>...</RoleGate>
// Hides children if current user's role is not in allowedRoles
// Shows nothing or optional fallback
```

**9.6 ConfirmDialog** (`src/components/common/confirm-dialog.tsx`):
- Confirmation dialog for destructive actions
- Title, description, confirm/cancel buttons
- Destructive variant (red confirm button)

---

### 10. API Client Setup

**File**: `src/services/api-client.ts`

Set up Axios instance with:
- Base URL: `process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1'`
- Request interceptor: attach `Authorization: Bearer <token>` from auth store
- Response interceptor: handle 401 (redirect to login), handle token refresh
- Error standardization: map backend errors to frontend-friendly format
- Timeout: 30 seconds

```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add auth token
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor - handle 401, refresh token
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // Try refresh token, if fails → redirect to /login
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

**File**: `src/services/endpoints.ts`

```typescript
export const API_ENDPOINTS = {
  // Auth
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  REFRESH: '/auth/refresh',
  LOGOUT: '/auth/logout',
  
  // Invoices
  INVOICES: '/invoices',
  INVOICE_DETAIL: (id: string) => `/invoices/${id}`,
  INVOICE_VERIFY: (id: string) => `/invoices/${id}/verify`,
  INVOICE_REJECT: (id: string) => `/invoices/${id}/reject`,
  INVOICE_CHECK_DUPLICATE: '/invoices/check-duplicate',
  
  // Categories
  CATEGORIES: '/categories',
  
  // Users
  USERS: '/users',
  PROFILE: '/profile',
  
  // Companies
  COMPANIES: '/companies',
  MY_COMPANY: '/companies/me',
  
  // Audit
  AUDIT_LOGS: '/audit-logs',
};
```

---

### 11. TypeScript Type Definitions

**File**: `src/types/api.ts`

```typescript
// Standard API response wrapper (matching backend)
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
}

export interface ApiErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
    details?: unknown;
    timestamp: string;
  };
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  first: boolean;
  last: boolean;
}
```

**File**: `src/types/auth.ts`

```typescript
export type Role = 'ADMIN' | 'MANAGER' | 'ACCOUNTANT' | 'INTERN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  companyId: string;
  companyName: string;
  avatarUrl?: string;
  isActive: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}
```

**File**: `src/types/invoice.ts`

```typescript
export type InvoiceStatus = 'PENDING' | 'PROCESSING' | 'VERIFIED' | 'REJECTED';
export type SourceType = 'MANUAL' | 'LLM' | 'E_INVOICE';
export type Currency = 'TRY' | 'USD' | 'EUR' | 'GBP';

export interface InvoiceListItem {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  supplierName: string;
  totalAmount: number;
  currency: Currency;
  status: InvoiceStatus;
  sourceType: SourceType;
  categoryName?: string;
  itemCount: number;
  createdByUserName: string;
  createdAt: string;
}

// ... more types will be added in Phase 12
```

---

### 12. Zustand Stores (Skeleton)

**File**: `src/stores/auth-store.ts`

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  
  setTokens: (access: string, refresh: string) => void;
  setUser: (user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      
      setTokens: (access, refresh) =>
        set({ accessToken: access, refreshToken: refresh, isAuthenticated: true }),
      setUser: (user) => set({ user }),
      logout: () =>
        set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false }),
    }),
    { name: 'auth-storage' }
  )
);
```

**File**: `src/stores/sidebar-store.ts`

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SidebarState {
  isCollapsed: boolean;
  isMobileOpen: boolean;
  toggle: () => void;
  setMobileOpen: (open: boolean) => void;
}

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set) => ({
      isCollapsed: false,
      isMobileOpen: false,
      toggle: () => set((s) => ({ isCollapsed: !s.isCollapsed })),
      setMobileOpen: (open) => set({ isMobileOpen: open }),
    }),
    { name: 'sidebar-storage' }
  )
);
```

---

### 13. Dark/Light Theme

**File**: `src/components/providers/theme-provider.tsx`

Use `next-themes` for theme support:
- Default theme: `system` (follow OS preference)
- Options: light, dark, system
- Toggle in header user menu and settings page
- CSS variables defined in `globals.css` for Shadcn theming

**File**: `src/app/globals.css`

Extend Tailwind/Shadcn CSS variables for both light and dark themes. Use the default Shadcn theme as a base, customize with the app's brand colors.

Suggested brand palette:
- Primary: Blue/Indigo (`#4F46E5` / `#6366F1`)
- Accent: Teal (`#14B8A6`)
- Destructive: Red (`#EF4444`)
- Success: Green (`#22C55E`)
- Warning: Amber (`#F59E0B`)

---

### 14. Loading and Error States

**File**: `src/app/(dashboard)/loading.tsx`

Default loading state for dashboard route group — shows a skeleton layout.

**File**: `src/app/(dashboard)/error.tsx`

Default error boundary for dashboard route group — shows error message with retry.

**File**: `src/app/not-found.tsx`

Custom 404 page:
- Turkish text: "Sayfa Bulunamadı"
- Description: "Aradığınız sayfa mevcut değil veya taşınmış olabilir."
- Back to dashboard button

---

### 15. Environment Variables

**File**: `frontend/.env.local`

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_APP_NAME=Fatura OCR
NEXT_PUBLIC_APP_VERSION=1.0.0
```

**File**: `frontend/.env.example`

Same as above with placeholder values, committed to git.

---

## VISUAL DESIGN GUIDELINES

### Color Usage
- Use Shadcn/ui default theme colors as base
- Status colors: PENDING → amber/yellow, VERIFIED → green, REJECTED → red, PROCESSING → blue
- Role colors: ADMIN → purple, MANAGER → blue, ACCOUNTANT → green, INTERN → gray
- Source type colors: MANUAL → gray, LLM → indigo, E_INVOICE → teal

### Typography
- Use Inter font (or Geist, clean sans-serif)
- Page titles: `text-2xl font-bold`
- Section titles: `text-lg font-semibold`
- Body: `text-sm`
- All UI text in Turkish

### Spacing & Layout
- Sidebar width: 256px expanded, 64px collapsed
- Header height: 64px
- Content padding: `p-6` (desktop), `p-4` (mobile)
- Card spacing: `gap-6` between major sections

### Responsive Breakpoints
- Mobile: < 768px
- Tablet: 768px - 1023px
- Desktop: ≥ 1024px

---

## TESTING REQUIREMENTS

### Manual Testing Steps

1. **Run the app**: `npm run dev` → opens at http://localhost:3000
2. **Dashboard route**: Navigate to `/` → see dashboard placeholder with sidebar
3. **Sidebar navigation**: Click each nav item → correct page renders, active state highlights
4. **Sidebar collapse**: Click collapse button → sidebar shrinks to icon mode
5. **Mobile responsive**: Resize to mobile → sidebar hidden, hamburger menu appears
6. **Mobile sidebar**: Click hamburger → sidebar opens as Sheet overlay
7. **Auth pages**: Navigate to `/login` → centered layout, no sidebar
8. **User menu**: Click avatar → dropdown shows with profile, settings, logout
9. **Theme toggle**: Switch dark/light → entire app theme changes
10. **Breadcrumbs**: Navigate to `/invoices/new` → breadcrumbs show "Dashboard / Faturalar / Yeni Fatura"
11. **404 page**: Navigate to `/nonexistent` → custom 404 page in Turkish
12. **Error boundary**: Trigger error → error boundary catches with retry button
13. **Loading state**: Check `/` loading.tsx renders skeleton
14. **All routes work**: Visit every route → no blank pages or crashes

### Build Verification
```bash
cd frontend
npm run build    # Should succeed with no errors
npm run lint     # Should pass with no errors
```

---

## VERIFICATION CHECKLIST

- [ ] Next.js App Router directory structure created
- [ ] Shadcn/ui initialized and configured
- [ ] All dependencies installed (Zustand, TanStack Query, Axios, Lucide, next-themes, etc.)
- [ ] Root layout with providers (Theme, Query, Toast)
- [ ] Dashboard layout with sidebar + header
- [ ] Auth layout (centered, no sidebar)
- [ ] Sidebar with navigation items grouped by section
- [ ] Sidebar role-based visibility (ADMIN-only items hidden for other roles)
- [ ] Sidebar collapse/expand with animation
- [ ] Mobile responsive: Sheet-based sidebar
- [ ] Header with breadcrumbs, notification placeholder, user menu
- [ ] User menu dropdown with profile, settings, theme toggle, logout
- [ ] Dark/light mode working
- [ ] All placeholder pages created with Turkish text
- [ ] PageHeader, EmptyState, LoadingSkeleton, ErrorBoundary, RoleGate components
- [ ] API client (Axios) configured with interceptors
- [ ] TypeScript types defined (auth, invoice, api, company, user)
- [ ] Zustand stores (auth-store, sidebar-store) skeleton ready
- [ ] Environment variables configured
- [ ] 404 page in Turkish
- [ ] Error boundary in Turkish
- [ ] Loading skeleton states
- [ ] `npm run build` passes
- [ ] `npm run lint` passes
- [ ] All routes accessible and rendering

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_10_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed, actual time vs estimated (2-3 days)

### 2. Completed Tasks
List each task with checkbox.

### 3. Files Created
Full file tree of `frontend/src/` with new/modified files marked.

### 4. Routes Summary
| Route | Page Title | Layout | Status |
|-------|-----------|--------|--------|
| `/` | Dashboard | Dashboard | ✅/❌ |
| `/invoices` | Faturalar | Dashboard | ✅/❌ |
| `/login` | Giriş Yap | Auth | ✅/❌ |
| ... | ... | ... | ... |

### 5. Components Created
List all reusable components with brief descriptions.

### 6. Screenshots
Include screenshots (or describe) of:
- Dashboard layout (light mode)
- Dashboard layout (dark mode)
- Sidebar collapsed
- Mobile view with sidebar open
- Auth (login) page
- 404 page

### 7. Build Output
```bash
$ npm run build
# Include actual output
```

### 8. Issues Encountered
Document any problems and solutions.

### 9. Next Steps
What needs to be done in Phase 11 (Frontend Auth Pages). Note:
- Auth store is ready for integration
- API client interceptors prepared for token injection
- Login/register pages need forms and API calls

### 10. Time Spent
Actual time vs estimated (2-3 days).

---

## DEPENDENCIES

### Requires
- **Phase 0**: Frontend skeleton (Next.js project exists) ✅

### Required By
- **Phase 11**: Auth Pages (needs layout, routing, auth store, API client)
- **Phase 12**: Invoice CRUD UI (needs layout, nav items, types, API client)
- **Phase 26**: Dashboard (needs dashboard page route)
- **Phase 33**: PWA Configuration (needs Next.js app structure)
- **Phase 34**: i18n (needs page structure)

---

## SUCCESS CRITERIA

1. ✅ Complete layout skeleton with sidebar and header
2. ✅ All routes accessible with placeholder pages
3. ✅ Sidebar navigation with role-based visibility
4. ✅ Sidebar collapse/expand and mobile responsive
5. ✅ Dark/light theme toggle working
6. ✅ Breadcrumbs dynamic and accurate
7. ✅ User menu dropdown functional
8. ✅ API client configured and ready for integration
9. ✅ TypeScript types matching backend DTOs
10. ✅ Zustand stores skeleton ready
11. ✅ Reusable components (PageHeader, EmptyState, LoadingSkeleton, etc.)
12. ✅ Build and lint pass without errors
13. ✅ Result file created at `docs/OMER/step_results/faz_10_result.md`

---

## IMPORTANT NOTES

1. **Existing Project**: The `frontend/` directory exists from Phase 0. Do NOT reinitialize with `create-next-app`. Extend the existing project.
2. **No Backend Integration Yet**: Pages use placeholder content. API calls and real data come in Phase 11 (auth) and Phase 12 (invoices).
3. **Turkish UI**: ALL user-facing text must be in Turkish. Component names and code remain in English.
4. **Shadcn/ui First**: Use Shadcn/ui components as the primary building blocks. Don't build custom components when Shadcn has one available.
5. **Route Groups**: Use `(auth)` and `(dashboard)` route groups to separate layouts cleanly.
6. **Auth Store Persistence**: Use Zustand `persist` middleware to keep tokens in localStorage. Phase 11 will fully implement the auth flow.
7. **Mobile-First**: Test responsive design throughout. The sidebar must work well on all screen sizes.
8. **Docker**: The frontend runs in Docker via `docker-compose`. Make sure the dev server works both with and without Docker.

---

**Phase 10 Completion Target**: A professional, responsive frontend shell with layout, navigation, routing, dark/light theme, and all foundational infrastructure ready for feature implementation.
