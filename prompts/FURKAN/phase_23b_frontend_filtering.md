# PHASE 23-B: FRONTEND — ADVANCED FILTER PANEL AND SEARCH UI

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 — LLM-based extraction
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-22 + 23-A Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth, RBAC, Invoice CRUD API, Audit Log, Duplication Control
- ✅ Phase 10: Next.js 14+ App Router, sidebar navigation, Shadcn/ui, responsive layout, dark/light mode
- ✅ Phase 11: Login/Register pages, Zustand auth store, Axios interceptor, token refresh, protected routes
- ✅ Phase 12: Invoice list table (TanStack Query, pagination, sorting, status badges), detail page, manual add/edit forms, soft delete, skeleton loading, optimistic updates
- ✅ Phase 13-19: Full extraction pipeline (FastAPI, image preprocessing, Gemini/GPT/Claude LLM, fallback chain, validation, XML parser, RabbitMQ)
- ✅ Phase 20-21: File Upload backend + frontend (drag-and-drop, batch tracking)
- ✅ Phase 22: LLM Verification UI (split-view, inline editing, confidence score, verify/reject)
- ✅ Phase 23-A (ÖMER — Backend): Advanced filtering API with JPA Specifications — all filter query parameters, supplier autocomplete endpoint, filter options endpoint, new database indexes

### What Phase 23-A Delivers (Backend Endpoints for This Phase)

**Updated Invoice List:**
- **GET /api/v1/invoices** — enhanced with the following NEW query parameters (all optional, combine with AND logic):

| Parameter | Type | Example | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (existing) |
| size | int | 20 | Page size (existing) |
| sort | string | invoiceDate,desc | Sort field and direction (existing) |
| status | string (CSV) | PENDING,REJECTED | One or more statuses (enhanced — was single) |
| categoryId | string (CSV) | uuid1,uuid2 | One or more category UUIDs (enhanced) |
| dateFrom | string | 2026-01-01 | Invoice date range start (YYYY-MM-DD) |
| dateTo | string | 2026-06-30 | Invoice date range end (YYYY-MM-DD) |
| amountMin | number | 100.00 | Minimum total amount |
| amountMax | number | 50000.00 | Maximum total amount |
| currency | string (CSV) | TRY,USD | One or more currencies |
| sourceType | string (CSV) | LLM,E_INVOICE | One or more source types |
| llmProvider | string (CSV) | GEMINI,GPT | One or more LLM providers |
| confidenceMin | number | 70 | Minimum confidence score (0-100) |
| confidenceMax | number | 100 | Maximum confidence score (0-100) |
| supplierName | string (CSV) | ABC Ltd.,XYZ Corp. | One or more supplier names |
| search | string | FTR-2026 | Full-text search across invoice_number, supplier_name, buyer_name, notes |
| createdByUserId | UUID | user-uuid | Filter by creator |
| createdFrom | datetime | 2026-01-01T00:00:00Z | Created date range start |
| createdTo | datetime | 2026-06-30T23:59:59Z | Created date range end |

**Supplier Autocomplete:**
- **GET /api/v1/invoices/suppliers?search=abc** — returns distinct supplier names (string array), max 50, alphabetical, company-scoped

**Filter Options (Dynamic Metadata):**
- **GET /api/v1/invoices/filter-options** — returns:
  - `statuses`: [{ value: "PENDING", label: "Beklemede" }, ...] 
  - `categories`: [{ id: "uuid", name: "Teknoloji" }, ...]
  - `currencies`: ["TRY", "USD", "EUR", "GBP"]
  - `sourceTypes`: ["LLM", "E_INVOICE", "MANUAL"]
  - `llmProviders`: ["GEMINI", "GPT", "CLAUDE"]
  - `amountRange`: { min: 10.50, max: 985000.00 }
  - `dateRange`: { earliest: "2025-06-15", latest: "2026-02-11" }
  - `confidenceRange`: { min: 12.5, max: 99.8 }

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 2-3 days

### Frontend Tech Stack
- Next.js 14+ (App Router)
- React 19
- TypeScript 5.x
- Tailwind CSS 3.x
- Shadcn/ui (component library)
- TanStack Query 5.x (server state)
- Zustand 4.x (client state — if needed)
- Axios (HTTP client with interceptor from Phase 11)

---

## OBJECTIVE

Build a comprehensive, user-friendly filter panel for the invoice list page that leverages all the advanced filtering capabilities provided by Phase 23-A. The filter panel must support all filter types (date range, multi-select dropdowns, amount range, text search, confidence slider), persist filter state in URL query parameters (for shareable/bookmarkable filtered views), and integrate seamlessly with the existing invoice list table from Phase 12.

---

## DETAILED REQUIREMENTS

### 1. Filter Panel Location and Layout

**Location**: Integrate the filter panel into the existing invoice list page (created in Phase 12). The filter panel appears above the invoice table.

**Layout Options (implement the collapsible approach):**

- A **search bar** is always visible at the top of the invoice list, above the table
- Below the search bar, a "Filtreler" (Filters) button/toggle that expands/collapses the full filter panel
- When expanded, the filter panel shows all filter controls in a responsive grid layout
- The filter panel should show a **badge count** on the "Filtreler" button indicating how many filters are currently active (e.g., "Filtreler (3)")
- On mobile: the filter panel expands full-width, filter controls stack vertically

**Filter Panel Grid (Desktop):**
- Row 1: Date range (from-to), Status (multi-select), Category (multi-select)
- Row 2: Supplier (multi-select with search), Amount range (min-max), Currency (multi-select)
- Row 3: Source type (multi-select), LLM provider (multi-select), Confidence score range (slider)
- Below grid: "Uygula" (Apply) and "Temizle" (Clear All) buttons

**On Tablet/Mobile:**
- Stack filter controls in a single column or two-column grid
- Consider a slide-out drawer approach for the filter panel on mobile

### 2. Search Bar

**Always visible** above the table (not inside the collapsible panel):

- Full-width text input with a search icon (magnifying glass)
- Placeholder text: "Fatura numarası, tedarikçi adı veya notlarda ara..."
- **Debounced search**: Wait 400ms after the user stops typing before sending the API request (avoid excessive requests while typing)
- Maps to the `search` query parameter
- Show a clear (X) button when text is present
- Pressing Enter also triggers the search immediately (don't wait for debounce)

### 3. Date Range Filter

**Component**: Two date pickers side by side — "Başlangıç" (From) and "Bitiş" (To)

- Use Shadcn/ui DatePicker component (or Popover with Calendar)
- Display format: DD.MM.YYYY (Turkish format)
- Send to API as: YYYY-MM-DD
- Allow selecting only "from" (open-ended start) or only "to" (open-ended end)
- Quick presets dropdown/buttons: "Bugün" (Today), "Bu Hafta" (This Week), "Bu Ay" (This Month), "Son 3 Ay" (Last 3 Months), "Bu Yıl" (This Year), "Özel Aralık" (Custom Range)
- Maps to `dateFrom` and `dateTo` query parameters
- Show a clear button to reset the date filter

### 4. Status Filter

**Component**: Multi-select dropdown with checkboxes

- Options loaded from filter-options API: PENDING (Beklemede), VERIFIED (Onaylandı), REJECTED (Reddedildi), PROCESSING (İşleniyor)
- Display each status with its corresponding color badge (reuse the status badge component from Phase 12)
- Allow selecting multiple statuses
- Show selected count when collapsed: "Durum (2)"
- Maps to `status` query parameter (comma-separated values)

### 5. Category Filter

**Component**: Multi-select dropdown with checkboxes

- Options loaded from filter-options API (dynamic — categories are company-specific)
- Show category color dot next to each option (if categories have colors from Phase 7)
- Allow selecting multiple categories
- Show selected count when collapsed: "Kategori (3)"
- Maps to `categoryId` query parameter (comma-separated UUIDs)

### 6. Supplier Filter

**Component**: Multi-select with typeahead/autocomplete search

- When the user types in the supplier input, call GET /api/v1/invoices/suppliers?search={input} with debouncing (300ms)
- Show results in a dropdown as the user types
- Allow selecting multiple suppliers from the results
- Show selected suppliers as removable tags/chips below the input
- Maps to `supplierName` query parameter (comma-separated names)
- Handle special characters in supplier names (URL encoding)

### 7. Amount Range Filter

**Component**: Two number inputs side by side — "Min Tutar" (Min Amount) and "Max Tutar" (Max Amount)

- Number inputs with Turkish locale formatting (1.234,56) for display
- Send to API as standard decimal numbers (1234.56)
- Use the amountRange from filter-options to set reasonable min/max boundaries and placeholder hints
- Allow entering only min (open-ended) or only max (open-ended)
- Maps to `amountMin` and `amountMax` query parameters
- Show currency symbol based on the selected currency filter (or default TRY)

### 8. Currency Filter

**Component**: Multi-select dropdown with checkboxes (or a simple button group for the few options)

- Options: TRY (₺), USD ($), EUR (€), GBP (£)
- Show currency symbol alongside the code
- Maps to `currency` query parameter (comma-separated)
- Since there are only 4 options, consider using toggle buttons instead of a dropdown for more compact UI

### 9. Source Type Filter

**Component**: Multi-select dropdown or toggle button group

- Options: LLM (LLM Çıkarım), E_INVOICE (e-Fatura), MANUAL (Manuel)
- Show an icon for each: LLM → brain/AI icon, E_INVOICE → document icon, MANUAL → pencil icon
- Maps to `sourceType` query parameter (comma-separated)

### 10. LLM Provider Filter

**Component**: Multi-select dropdown or toggle button group

- Options: GEMINI, GPT, CLAUDE
- Only show this filter when "LLM" is selected in the source type filter (contextual visibility)
- Use distinct colors/icons for each provider (consistent with Phase 22 verification UI)
- Maps to `llmProvider` query parameter (comma-separated)

### 11. Confidence Score Range Filter

**Component**: Dual-handle range slider

- Range: 0 to 100
- Show current selected range values: "Güven Skoru: 70 - 100"
- Color the slider track: red (0-69), yellow (70-89), green (90-100) — matching Phase 22 confidence colors
- Only show this filter when "LLM" is selected in source type (or always show — design decision, document in result)
- Maps to `confidenceMin` and `confidenceMax` query parameters
- Default: no filter (full 0-100 range)

### 12. URL-Based Filter State (Query Parameter Sync)

**Critical Requirement**: All active filters must be reflected in the browser URL as query parameters. This enables:
- Shareable filtered views (copy URL and share with a colleague)
- Bookmarkable filter configurations
- Browser back/forward navigation through filter states
- Page refresh preserves active filters

**Implementation:**
- Use Next.js `useSearchParams` and `useRouter` to read and write URL query parameters
- When the user applies filters, update the URL with all active filter params
- When the page loads with existing query params, initialize the filter state from the URL
- When filters are cleared, remove the corresponding query params from the URL
- TanStack Query should use the filter params as part of its query key (so changing filters triggers a new API call)

**URL Example:**
`/invoices?status=PENDING,REJECTED&dateFrom=2026-01-01&search=ABC&page=0&size=20&sort=invoiceDate,desc`

### 13. Filter Application Behavior

**Two approaches — implement the "Apply" button approach with instant search:**

- The **search bar** applies instantly (with debounce) — no need to click "Apply"
- All other filters in the collapsible panel require clicking **"Uygula" (Apply)** to take effect
- This prevents excessive API calls while the user is still configuring filters
- The **"Temizle" (Clear All)** button resets ALL filters (including search) and reloads the list

**Active Filter Summary:**
- Below the search bar and above the table, show a horizontal row of **active filter chips/tags**
- Each chip shows the filter name and value: "Durum: Beklemede, Reddedildi" or "Tarih: 01.01.2026 - 30.06.2026"
- Each chip has an (X) button to remove that specific filter
- Removing a filter chip immediately reloads the list

### 14. Integration with Existing Invoice List (Phase 12)

**The invoice list table from Phase 12 must work seamlessly with the new filters:**

- TanStack Query's `useQuery` hook for the invoice list should include all filter params in its query key
- When filters change, the query key changes, triggering a refetch
- Reset to page 0 when filters change (don't stay on page 5 when applying new filters)
- Maintain sort settings when filters change
- Show the total result count prominently: "Toplam 142 fatura" or "Filtrelenmiş: 23 / 142 fatura"
- When no results match the filters, show an empty state: "Seçilen filtrelere uygun fatura bulunamadı" with a "Filtreleri Temizle" button

### 15. Loading States

- When filters are being applied (API call in progress): show a subtle loading indicator on the table (not a full skeleton — the user should still see the filter panel)
- The filter panel itself should not have loading states for interactions
- The filter-options API can be fetched once on page load and cached (TanStack Query with long staleTime)
- Supplier autocomplete should show a small spinner in the input while fetching results

### 16. Responsive Design

- **Desktop (>= 1280px)**: Full filter grid (3 columns) above the table
- **Desktop (1024px - 1279px)**: 2-column filter grid
- **Tablet (768px - 1023px)**: 2-column filter grid, more compact
- **Mobile (< 768px)**: Full-width filter panel as a slide-out sheet/drawer triggered by a "Filtreler" button in the header. Filter controls stack in a single column. Apply and Clear buttons are sticky at the bottom of the drawer.

---

## TESTING REQUIREMENTS

### 1. Component Tests

- Search bar renders and triggers debounced API calls
- Filter panel toggle (expand/collapse) works
- Active filter badge count shows correct number
- Date range picker sets correct values
- Multi-select dropdowns (status, category, currency, sourceType) work correctly
- Supplier autocomplete fetches and displays suggestions
- Amount range inputs accept and format numbers correctly
- Confidence score slider sets correct range values
- LLM provider filter shows/hides based on source type selection
- "Uygula" button builds correct query parameters
- "Temizle" button resets all filters
- Active filter chips display and removal works
- URL sync: filters → URL params (on apply)
- URL sync: URL params → filter state (on page load)
- Pagination resets to page 0 when filters change
- Empty state displays when no results match
- Result count updates correctly with filters
- Responsive layout: mobile drawer opens/closes correctly

### 2. Integration Tests

- Mock GET /api/v1/invoices with various filter params → verify correct params sent
- Mock GET /api/v1/invoices/filter-options → verify dropdowns populated
- Mock GET /api/v1/invoices/suppliers → verify autocomplete works
- Filter + pagination combined → verify correct behavior
- Filter + sort combined → verify correct behavior
- Clear filters → verify all params removed from request

---

## RESULT FILE

When this phase is complete, create a result file at:

**`docs/FURKAN/step_results/faz_23.1_result.md`**

The result file must contain:

### 1. Execution Summary
- Phase number (23-B), assigned developer, start/end dates
- Execution status (COMPLETED / PARTIAL / BLOCKED)
- Total time spent

### 2. Completed Tasks Checklist
- [ ] Search bar with debounced search
- [ ] Collapsible filter panel with active filter count badge
- [ ] Date range filter with quick presets
- [ ] Status multi-select filter
- [ ] Category multi-select filter
- [ ] Supplier filter with typeahead autocomplete
- [ ] Amount range filter (min/max inputs)
- [ ] Currency filter
- [ ] Source type filter
- [ ] LLM provider filter (contextual visibility)
- [ ] Confidence score range slider
- [ ] URL-based filter state sync (query parameters)
- [ ] Apply and Clear buttons
- [ ] Active filter chips with individual removal
- [ ] Integration with existing invoice list table
- [ ] Total/filtered result count display
- [ ] Empty state for no results
- [ ] Loading states
- [ ] Responsive design (desktop, tablet, mobile)

### 3. Files Created/Modified
List every file with full path and description.

### 4. Component Architecture
- Filter panel container component
- Individual filter components (date picker, multi-select, autocomplete, range slider, etc.)
- Search bar component
- Active filter chips component
- Custom hooks (useInvoiceFilters, useFilterOptions, useSupplierAutocomplete, useDebouncedSearch, etc.)

### 5. State Management
- How filter state is managed (local state vs URL params vs Zustand)
- How TanStack Query keys incorporate filter params
- How debouncing is implemented
- How filter-options are cached

### 6. Screenshots / UI Description
For each key state:
- Default view (filters collapsed, search bar visible)
- Expanded filter panel (all filter controls visible)
- Filters applied (active filter chips shown, badge count on button)
- Supplier autocomplete dropdown open
- Date range with quick preset selected
- Confidence score slider adjusted
- Empty state (no results matching filters)
- Mobile drawer view
- Dark mode appearance

### 7. Test Results
- Component test output summary
- Number of tests passed/failed

### 8. Database Changes
- Confirm "No database changes needed for this phase" (frontend only)

### 9. Issues Encountered
Problems and solutions

### 10. UX Decisions
- Document any UX decisions made during implementation (e.g., which filters are always visible vs contextual, apply button vs instant apply, etc.)

### 11. Next Steps
- What Phase 24 (Export) needs — the export button should pass current filters to the export endpoint
- What Phase 26 (Dashboard) might reference — dashboard drill-down could navigate to filtered invoice list
- Any filter improvements identified for future iteration

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 10**: App layout, Shadcn/ui setup, dark/light mode
- **Phase 11**: Auth store, Axios interceptor
- **Phase 12**: Invoice list table (TanStack Query, pagination, sorting) — this is the page being enhanced
- **Phase 23-A**: Backend filtering API — all query parameters, supplier autocomplete, filter-options endpoint

### Required By
- **Phase 24**: Export Module — frontend export button will pass current active filters to the export endpoint
- **Phase 26**: Dashboard — drill-down from dashboard charts may navigate to filtered invoice list
---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Search bar is visible above the invoice table
- [ ] Search triggers after 300ms debounce (not on every keystroke)
- [ ] Filter panel toggles open/close with a button
- [ ] Active filter count badge shows on the filter toggle button
- [ ] Date range picker works with Turkish date display (GG.AA.YYYY)
- [ ] Quick date presets work (Bu Ay, Geçen Ay, Son 7 Gün, Bu Yıl)
- [ ] Status multi-select dropdown works with color-coded badges
- [ ] Category multi-select dropdown works
- [ ] Currency dropdown works
- [ ] Source type dropdown works
- [ ] Supplier typeahead autocomplete fetches from GET /invoices/suppliers
- [ ] Amount range inputs (min/max) accept only numeric values
- [ ] Confidence score slider is dual-handle with color-coded track
- [ ] LLM provider filter shows only when source type includes LLM
- [ ] "Uygula" button applies all filters in a single API call
- [ ] "Temizle" button clears all filters and reloads default list
- [ ] Active filter chips appear below search bar
- [ ] Individual filter chip removal works (X button on each chip)
- [ ] URL query parameters update when filters change
- [ ] Page load with URL query params initializes filters correctly
- [ ] Sharing a URL with filter params reproduces the same filtered view
- [ ] Pagination resets to page 0 when filters change
- [ ] Result count shows "X / Y kayıt gösteriliyor"
- [ ] Empty state message shows when no invoices match filters
- [ ] Responsive layout: filter panel collapses to modal on mobile
- [ ] All text is in Turkish
- [ ] Dark mode works for all filter components
- [ ] `npm run build` completes without errors
- [ ] `npm run lint` passes without errors
- [ ] All component tests pass
- [ ] Result file created at docs/FURKAN/step_results/faz_23.1_result.md

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Search bar is visible above the invoice table and triggers debounced full-text search
2. ✅ Filter panel is collapsible with an active filter count badge
3. ✅ Date range filter works with Turkish date display and quick presets
4. ✅ Status, category, currency, source type multi-select filters work correctly
5. ✅ Supplier filter provides typeahead autocomplete from the backend
6. ✅ Amount range filter (min/max) works correctly
7. ✅ Confidence score dual-handle slider works with color-coded track
8. ✅ LLM provider filter shows conditionally based on source type
9. ✅ All active filters are reflected in the browser URL query parameters
10. ✅ Page load with URL query params initializes filters correctly (shareable URLs)
11. ✅ "Uygula" applies all filter panel changes in a single API call
12. ✅ "Temizle" clears all filters and reloads the default list
13. ✅ Active filter chips show below the search bar with individual removal
14. ✅ Pagination resets to page 0 when filters change
15. ✅ Result count shows total and filtered count
16. ✅ Empty state displays when no invoices match filters
17. ✅ Responsive design works on desktop, tablet, and mobile
18. ✅ All user-facing text is in Turkish
19. ✅ Dark mode support works correctly
20. ✅ All component tests pass
21. ✅ Result file is created at docs/FURKAN/step_results/faz_23.1_result.md

---

## IMPORTANT NOTES

1. **Do NOT Modify Backend Code**: This phase is frontend-only. All required endpoints are provided by Phase 23-A. If you discover a missing endpoint or parameter, document it in the result file.

2. **URL as Single Source of Truth**: The URL query parameters should be the single source of truth for filter state. On page load, read from URL. On filter change, write to URL. TanStack Query reads from URL params. This avoids state synchronization issues.

3. **Debounce Strategy**: Use 400ms debounce for the search bar (instant feel), 300ms debounce for supplier autocomplete. Other filters use the "Apply" button so no debounce needed.

4. **TanStack Query Key Design**: Include all filter params in the query key for the invoice list. Example: `['invoices', { page, size, sort, status, dateFrom, dateTo, search, ... }]`. When any filter changes, TanStack Query treats it as a new query and refetches.

5. **Filter Options Caching**: Fetch GET /api/v1/invoices/filter-options once on component mount with a long staleTime (5 minutes) in TanStack Query. This data changes infrequently and doesn't need constant refetching.

6. **Turkish Number Formatting**: Amount inputs should display with Turkish locale (1.234,56) but the actual filter values sent to the API must be standard decimals (1234.56). Use Intl.NumberFormat('tr-TR') for display formatting.

7. **Turkish Date Formatting**: Date pickers should display DD.MM.YYYY but send YYYY-MM-DD to the API. Ensure consistent formatting.

8. **Performance**: With many filter controls, be mindful of unnecessary re-renders. Consider memoizing filter components with React.memo and using useCallback for event handlers. The filter-options data should be fetched once, not on every render.

9. **Phase 24 Integration Preview**: The Export module (Phase 24) will add an "Dışa Aktar" (Export) button that exports the CURRENTLY FILTERED data. Design the filter state management so that Phase 24 can easily access the current active filters to pass them as export parameters.

10. **Accessibility**: Ensure all filter controls have proper labels, ARIA attributes, and keyboard navigation. Dropdowns should be navigable with arrow keys. The slider should be adjustable with arrow keys.

---

**Phase 23-B Completion Target**: A polished, responsive, and accessible filter panel integrated into the invoice list page — with full-text search, multi-select dropdowns, date and amount ranges, confidence slider, URL-based persistence, active filter visualization, and seamless integration with the existing table and pagination — enabling users to quickly find any invoice across thousands of records.
