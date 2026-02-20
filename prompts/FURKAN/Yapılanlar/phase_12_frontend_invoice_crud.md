# PHASE 12: FRONTEND — INVOICE LIST AND CRUD UI

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
- **Backend**: Java 17 + Spring Boot 3.2 — **port 8082**
- **Frontend**: Next.js 14+ (App Router, TypeScript, Tailwind CSS, Shadcn/ui) — **port 3001**

### Current State (Phases 0-11 Completed)
- ✅ Phase 0-3: Docker environment, CI/CD, Hexagonal Architecture, Database schema
- ✅ Phase 4-6: JWT Auth, RBAC (4 roles), Company & User Management
- ✅ Phase 7: Invoice CRUD API with items, categories, status workflow
- ✅ Phase 8-9: Audit Log, Duplication Control
- ✅ Phase 10: Frontend Layout and Routing
- ✅ Phase 11: Frontend Authentication (Login, Register, Auth Store, Protected Routes)

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 3-4 days

---

## OBJECTIVE

Build complete invoice management UI: list with pagination/sorting/filters, detail page, create/edit forms with dynamic line items, delete confirmation, status workflow (verify/reject/reopen), and category management. Use TanStack Query for server state and Shadcn/ui components.

---

## BACKEND API REFERENCE

**Base URL**: `http://localhost:8082/api/v1`

### Invoice Endpoints

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/invoices` | List (paginated) | ALL |
| GET | `/invoices/{id}` | Detail with items | ALL |
| POST | `/invoices` | Create | ALL |
| PUT | `/invoices/{id}` | Update | ADMIN, MANAGER, ACCOUNTANT |
| DELETE | `/invoices/{id}` | Soft delete | ADMIN, MANAGER |
| PATCH | `/invoices/{id}/verify` | Verify | ADMIN, MANAGER, ACCOUNTANT |
| PATCH | `/invoices/{id}/reject` | Reject | ADMIN, MANAGER, ACCOUNTANT |
| PATCH | `/invoices/{id}/reopen` | Reopen rejected | ADMIN, MANAGER |

### Category Endpoints

| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/categories` | List active | ALL |
| POST | `/categories` | Create | ADMIN, MANAGER |
| PUT | `/categories/{id}` | Update | ADMIN, MANAGER |
| DELETE | `/categories/{id}` | Delete | ADMIN, MANAGER |

### Key Request/Response Structures

**GET /invoices** Query Params: `page`, `size`, `sort`, `status`, `categoryId`

**InvoiceListItem Response**:
```json
{
  "id": "uuid", "invoiceNumber": "FTR-2026-001", "invoiceDate": "2026-02-10",
  "supplierName": "ABC Ltd.", "totalAmount": 960.00, "currency": "TRY",
  "status": "PENDING", "sourceType": "MANUAL", "categoryName": "Teknoloji",
  "itemCount": 2, "createdByUserName": "Ömer", "createdAt": "2026-02-10T14:30:00Z"
}
```

**InvoiceDetail Response**: All fields + `items[]` array with line items

**CreateInvoiceRequest**:
```json
{
  "invoiceNumber": "FTR-2026-001", "invoiceDate": "2026-02-10",
  "supplierName": "ABC Ltd.", "supplierTaxNumber": "1234567890",
  "currency": "TRY", "categoryId": "uuid", "notes": "...",
  "items": [{ "description": "...", "quantity": 1, "unit": "ADET", "unitPrice": 500, "taxRate": 20 }]
}
```

---

## DETAILED REQUIREMENTS

### 1. TypeScript Types

**File: `frontend/src/types/invoice.ts`**

```typescript
export type InvoiceStatus = 'PENDING' | 'PROCESSING' | 'VERIFIED' | 'REJECTED';
export type SourceType = 'LLM' | 'E_INVOICE' | 'MANUAL';
export type LlmProvider = 'GEMINI' | 'GPT' | 'CLAUDE';
export type Currency = 'TRY' | 'USD' | 'EUR' | 'GBP';
export type UnitType = 'ADET' | 'KG' | 'LT' | 'M' | 'M2' | 'M3' | 'PAKET' | 'KUTU' | 'SAAT' | 'GUN';

export interface InvoiceItem {
  id?: string;
  lineNumber: number;
  description: string;
  quantity: number;
  unit: UnitType;
  unitPrice: number;
  taxRate: number;
  taxAmount: number;
  subtotal: number;
  totalAmount: number;
  productCode?: string;
  barcode?: string;
}

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

export interface InvoiceDetail {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  supplierName: string;
  supplierTaxNumber?: string;
  supplierTaxOffice?: string;
  supplierAddress?: string;
  supplierPhone?: string;
  supplierEmail?: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  currency: Currency;
  exchangeRate: number;
  status: InvoiceStatus;
  sourceType: SourceType;
  llmProvider?: LlmProvider;
  confidenceScore?: number;
  categoryId?: string;
  categoryName?: string;
  notes?: string;
  rejectionReason?: string;
  createdByUserId: string;
  createdByUserName: string;
  verifiedByUserId?: string;
  verifiedByUserName?: string;
  verifiedAt?: string;
  rejectedAt?: string;
  createdAt: string;
  updatedAt: string;
  items: InvoiceItem[];
}

export interface CreateInvoiceRequest {
  invoiceNumber: string;
  invoiceDate: string;
  dueDate?: string;
  supplierName: string;
  supplierTaxNumber?: string;
  supplierTaxOffice?: string;
  supplierAddress?: string;
  supplierPhone?: string;
  supplierEmail?: string;
  currency: Currency;
  exchangeRate?: number;
  categoryId?: string;
  notes?: string;
  items: CreateInvoiceItemRequest[];
}

export interface CreateInvoiceItemRequest {
  id?: string;
  description: string;
  quantity: number;
  unit: UnitType;
  unitPrice: number;
  taxRate: number;
  productCode?: string;
}

export type UpdateInvoiceRequest = CreateInvoiceRequest;

export interface VerifyInvoiceRequest { notes?: string; }
export interface RejectInvoiceRequest { rejectionReason: string; }

export interface PaginatedResponse<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number; };
}

export interface InvoiceListParams {
  page?: number;
  size?: number;
  sort?: string;
  status?: InvoiceStatus;
  categoryId?: string;
}
```

**File: `frontend/src/types/category.ts`**

```typescript
export interface Category {
  id: string;
  name: string;
  description?: string;
  color: string;
  icon?: string;
  isActive: boolean;
  invoiceCount: number;
  createdAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
  color: string;
  icon?: string;
}
```

---

### 2. Services

**File: `frontend/src/services/invoice-service.ts`**

```typescript
import apiClient from '@/lib/api-client';
import { InvoiceDetail, InvoiceListItem, CreateInvoiceRequest, UpdateInvoiceRequest, 
         VerifyInvoiceRequest, RejectInvoiceRequest, PaginatedResponse, InvoiceListParams } from '@/types/invoice';

export const invoiceService = {
  async getInvoices(params: InvoiceListParams = {}): Promise<PaginatedResponse<InvoiceListItem>> {
    const response = await apiClient.get('/invoices', {
      params: { page: params.page ?? 0, size: params.size ?? 20, sort: params.sort ?? 'createdAt,desc',
                status: params.status, categoryId: params.categoryId }
    });
    return response.data;
  },

  async getInvoice(id: string): Promise<InvoiceDetail> {
    return (await apiClient.get(`/invoices/${id}`)).data;
  },

  async createInvoice(data: CreateInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.post('/invoices', data)).data;
  },

  async updateInvoice(id: string, data: UpdateInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.put(`/invoices/${id}`, data)).data;
  },

  async deleteInvoice(id: string): Promise<void> {
    await apiClient.delete(`/invoices/${id}`);
  },

  async verifyInvoice(id: string, data?: VerifyInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.patch(`/invoices/${id}/verify`, data ?? {})).data;
  },

  async rejectInvoice(id: string, data: RejectInvoiceRequest): Promise<InvoiceDetail> {
    return (await apiClient.patch(`/invoices/${id}/reject`, data)).data;
  },

  async reopenInvoice(id: string): Promise<InvoiceDetail> {
    return (await apiClient.patch(`/invoices/${id}/reopen`, {})).data;
  },
};
```

**File: `frontend/src/services/category-service.ts`**

```typescript
import apiClient from '@/lib/api-client';
import { Category, CreateCategoryRequest } from '@/types/category';

export const categoryService = {
  async getCategories(): Promise<Category[]> {
    return (await apiClient.get('/categories')).data;
  },
  async createCategory(data: CreateCategoryRequest): Promise<Category> {
    return (await apiClient.post('/categories', data)).data;
  },
  async updateCategory(id: string, data: CreateCategoryRequest): Promise<Category> {
    return (await apiClient.put(`/categories/${id}`, data)).data;
  },
  async deleteCategory(id: string): Promise<void> {
    await apiClient.delete(`/categories/${id}`);
  },
};
```

---

### 3. TanStack Query Hooks

**File: `frontend/src/hooks/use-invoices.ts`**

```typescript
'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { invoiceService } from '@/services/invoice-service';
import { CreateInvoiceRequest, UpdateInvoiceRequest, VerifyInvoiceRequest, 
         RejectInvoiceRequest, InvoiceListParams } from '@/types/invoice';
import { toast } from 'sonner';

export const invoiceKeys = {
  all: ['invoices'] as const,
  lists: () => [...invoiceKeys.all, 'list'] as const,
  list: (params: InvoiceListParams) => [...invoiceKeys.lists(), params] as const,
  details: () => [...invoiceKeys.all, 'detail'] as const,
  detail: (id: string) => [...invoiceKeys.details(), id] as const,
};

export function useInvoices(params: InvoiceListParams = {}) {
  return useQuery({
    queryKey: invoiceKeys.list(params),
    queryFn: () => invoiceService.getInvoices(params),
    staleTime: 30 * 1000,
  });
}

export function useInvoice(id: string) {
  return useQuery({
    queryKey: invoiceKeys.detail(id),
    queryFn: () => invoiceService.getInvoice(id),
    enabled: !!id,
  });
}

export function useCreateInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateInvoiceRequest) => invoiceService.createInvoice(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      toast.success('Fatura başarıyla oluşturuldu');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura oluşturulamadı'),
  });
}

export function useUpdateInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateInvoiceRequest }) =>
      invoiceService.updateInvoice(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(variables.id) });
      toast.success('Fatura başarıyla güncellendi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura güncellenemedi'),
  });
}

export function useDeleteInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => invoiceService.deleteInvoice(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      toast.success('Fatura başarıyla silindi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura silinemedi'),
  });
}

export function useVerifyInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data?: VerifyInvoiceRequest }) =>
      invoiceService.verifyInvoice(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(variables.id) });
      toast.success('Fatura onaylandı');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura onaylanamadı'),
  });
}

export function useRejectInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: RejectInvoiceRequest }) =>
      invoiceService.rejectInvoice(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(variables.id) });
      toast.success('Fatura reddedildi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura reddedilemedi'),
  });
}

export function useReopenInvoice() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => invoiceService.reopenInvoice(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(id) });
      toast.success('Fatura yeniden açıldı');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Fatura yeniden açılamadı'),
  });
}
```

**File: `frontend/src/hooks/use-categories.ts`**

```typescript
'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { categoryService } from '@/services/category-service';
import { CreateCategoryRequest } from '@/types/category';
import { toast } from 'sonner';

export const categoryKeys = {
  all: ['categories'] as const,
  list: () => [...categoryKeys.all, 'list'] as const,
};

export function useCategories() {
  return useQuery({
    queryKey: categoryKeys.list(),
    queryFn: () => categoryService.getCategories(),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCategoryRequest) => categoryService.createCategory(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.list() });
      toast.success('Kategori oluşturuldu');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Kategori oluşturulamadı'),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: CreateCategoryRequest }) =>
      categoryService.updateCategory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.list() });
      toast.success('Kategori güncellendi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Kategori güncellenemedi'),
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => categoryService.deleteCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: categoryKeys.list() });
      toast.success('Kategori silindi');
    },
    onError: (error: any) => toast.error(error.response?.data?.message || 'Kategori silinemedi'),
  });
}
```

---

### 4. Invoice List Page

**File: `frontend/src/app/(dashboard)/invoices/page.tsx`**

Create a full-featured invoice list page with:
- Paginated table using `useInvoices` hook
- Status filter dropdown (PENDING, VERIFIED, REJECTED, PROCESSING, ALL)
- Category filter dropdown from `useCategories`
- Sort dropdown (createdAt, invoiceDate, totalAmount, supplierName)
- Status badges with colors: PENDING=yellow, VERIFIED=green, REJECTED=red, PROCESSING=blue
- Source type badges: MANUAL, LLM, E_INVOICE
- Row click → navigate to `/invoices/{id}`
- Action dropdown per row with role-based items:
  - View (all)
  - Edit (ADMIN, MANAGER, ACCOUNTANT - only for PENDING)
  - Verify/Reject (ADMIN, MANAGER, ACCOUNTANT - only for PENDING)
  - Reopen (ADMIN, MANAGER - only for REJECTED)
  - Delete (ADMIN, MANAGER - not for VERIFIED)
- Delete confirmation AlertDialog
- Reject dialog with reason input
- Loading skeleton
- Empty state with action button
- Pagination component at bottom

Key UI elements:
```typescript
const statusConfig = {
  PENDING: { label: 'Beklemede', variant: 'secondary' },
  PROCESSING: { label: 'İşleniyor', variant: 'outline' },
  VERIFIED: { label: 'Onaylı', variant: 'default' },
  REJECTED: { label: 'Reddedildi', variant: 'destructive' },
};

const sourceTypeLabels = { MANUAL: 'Manuel', LLM: 'LLM', E_INVOICE: 'E-Fatura' };
```

---

### 5. Invoice Detail Page

**File: `frontend/src/app/(dashboard)/invoices/[id]/page.tsx`**

Create invoice detail page with:
- Back button
- Invoice header with number, supplier, status badge
- Rejection reason alert (if REJECTED)
- Two-column grid:
  - Invoice Info card (number, date, due date, category, source type)
  - Supplier Info card (name, tax number, address, phone, email)
- Items table with:
  - Line number, description, quantity, unit, unit price, tax rate, tax amount, total
  - Footer with subtotal, tax total, grand total
- Audit info card (created by, dates, verified by if applicable)
- Action buttons (role + status aware):
  - Edit, Verify, Reject, Reopen, Delete

---

### 6. Invoice Form Component

**File: `frontend/src/components/invoice/invoice-form.tsx`**

Create reusable form for create/edit with:
- react-hook-form + zod validation
- Sections: Invoice Info, Supplier Info, Items, Notes
- Dynamic line items with useFieldArray:
  - Add/Remove items
  - Real-time calculation: subtotal = qty × price, tax = subtotal × rate/100, total = subtotal + tax
- Invoice totals calculated from items
- Category select from API
- Currency select (TRY, USD, EUR, GBP)
- Unit select (ADET, KG, LT, M, etc.)

Validation rules:
- invoiceNumber: required
- invoiceDate: required
- supplierName: required
- supplierTaxNumber: optional, regex /^[0-9]{10,11}$/
- items: min 1 required
- item.description: required
- item.quantity: min 0.0001
- item.unitPrice: min 0
- item.taxRate: 0-100

---

### 7. Create & Edit Pages

**File: `frontend/src/app/(dashboard)/invoices/new/page.tsx`**
- Use InvoiceForm with empty defaults
- On submit → createInvoice mutation → redirect to detail

**File: `frontend/src/app/(dashboard)/invoices/[id]/edit/page.tsx`**
- Fetch invoice with useInvoice
- Check status === 'PENDING', else show error
- Use InvoiceForm with initialData
- On submit → updateInvoice mutation → redirect to detail

---

### 8. Categories Page

**File: `frontend/src/app/(dashboard)/categories/page.tsx`**

- Table with: color circle, name, description, invoice count
- Create/Edit dialog with: name input, description textarea, color picker
- Delete confirmation (show warning if invoiceCount > 0)
- Role-based: only ADMIN, MANAGER can create/edit/delete

---

### 9. Utility Functions

**File: `frontend/src/lib/utils.ts`** (add)

```typescript
export function formatCurrency(amount: number, currency: string = 'TRY'): string {
  const locales: Record<string, string> = { TRY: 'tr-TR', USD: 'en-US', EUR: 'de-DE', GBP: 'en-GB' };
  return new Intl.NumberFormat(locales[currency] || 'tr-TR', {
    style: 'currency', currency: currency,
  }).format(amount);
}
```

---

### 10. Pagination Component

**File: `frontend/src/components/common/data-table-pagination.tsx`**

- Shows: "X-Y / Z kayıt gösteriliyor"
- Page size select (10, 20, 50, 100)
- First, prev, next, last buttons
- Current page indicator

---

## REQUIRED SHADCN COMPONENTS

```bash
npx shadcn-ui@latest add table dialog alert-dialog select textarea separator
```

---

## TESTING REQUIREMENTS

1. **Invoice List**: Filters, sorting, pagination work
2. **Create Invoice**: Form validates, items calculate, saves correctly
3. **Edit Invoice**: Loads data, updates work
4. **Delete**: Confirmation shown, invoice removed
5. **Status Workflow**: Verify→VERIFIED, Reject→REJECTED (with reason), Reopen→PENDING
6. **Categories**: CRUD works, delete shows warning for categories with invoices
7. **Roles**: Buttons hidden/shown based on role

### Build Verification
```bash
npm run build && npm run lint
```

---

## RESULT FILE REQUIREMENTS

Create `docs/OMER/step_results/faz_12_result.md` with:
1. Execution Status
2. Completed Tasks (checklist)
3. Files Created/Modified
4. Test Results table
5. Screenshots (list, detail, form)
6. Build Output
7. Issues & Solutions
8. Database Changes (if any)
9. Next Steps for Phase 13
10. Time Spent vs Estimated (3-4 days)

---

## VERIFICATION CHECKLIST

- [ ] Types: InvoiceListItem, InvoiceDetail, Category, etc.
- [ ] Services: invoiceService, categoryService
- [ ] Hooks: useInvoices, useInvoice, mutations, useCategories
- [ ] Invoice List: table, filters, pagination, actions
- [ ] Invoice Detail: all fields, items, audit info
- [ ] Invoice Form: validation, dynamic items, calculations
- [ ] Create/Edit pages working
- [ ] Categories page with CRUD
- [ ] Role-based UI
- [ ] Turkish text throughout
- [ ] Build passes

---

## SUCCESS CRITERIA

1. ✅ Invoice list with pagination, filters, sorting
2. ✅ Invoice detail with all fields and items
3. ✅ Create/Edit forms with dynamic items
4. ✅ Delete with confirmation
5. ✅ Verify/Reject/Reopen workflow
6. ✅ Categories CRUD
7. ✅ Role-based button visibility
8. ✅ TanStack Query for all data
9. ✅ Loading/empty states
10. ✅ Build & lint pass
11. ✅ Result file created

---

## DEPENDENCIES

**Requires**: Phase 7 (Invoice API), Phase 10 (Layout), Phase 11 (Auth)

**Required By**: Phase 20 (Upload UI), Phase 23 (Filtering), Phase 24 (Export), Phase 26 (Dashboard)

---

**Phase 12 Completion Target**: Complete invoice management UI with list, detail, create, edit, delete, status workflow, and category management.
