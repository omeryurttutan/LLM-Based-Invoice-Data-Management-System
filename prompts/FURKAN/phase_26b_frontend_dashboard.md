# PHASE 26-B: FRONTEND — DASHBOARD AND DATA VISUALIZATION

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 — LLM-based extraction
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-25 + 26-A Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth, RBAC, Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10: Next.js 14+ App Router, sidebar navigation with items: Dashboard (/), Faturalar (/invoices), Yükleme (/upload), Kategoriler (/categories). Shadcn/ui, dark/light mode toggle, responsive (collapsible on mobile)
- ✅ Phase 11: Login/Register pages, Zustand auth store, Axios interceptor with token refresh, protected routes
- ✅ Phase 12: Invoice list table (TanStack Query, pagination, sorting, status badges), detail page, manual add/edit forms, category management
- ✅ Phase 13-22: Full extraction pipeline, file upload, verification UI
- ✅ Phase 23: Advanced Filtering — filter panel, URL-based state, search
- ✅ Phase 24-25: Export (XLSX/CSV + accounting formats)
- ✅ Phase 26-A (ÖMER — Backend): Dashboard statistics API — 6 endpoints delivering aggregated data

### What Phase 26-A Delivers (Backend Endpoints)

**Base URL**: `http://localhost:8082/api/v1/dashboard`

| Endpoint | Parameters | Returns |
|----------|-----------|---------|
| GET /stats | dateFrom, dateTo, currency | Summary KPIs: totalInvoices, totalAmount, averageAmount, pendingCount/Amount, verifiedCount/Amount, rejectedCount, processingCount, sourceBreakdown, confidenceStats |
| GET /categories | dateFrom, dateTo, currency | Category distribution array: categoryName, categoryColor, invoiceCount, totalAmount, percentage. Top 10 + "Diğer" |
| GET /monthly-trend | months (default 12), currency | Monthly array: month (YYYY-MM), label (Turkish), invoiceCount, totalAmount, verifiedAmount, averageAmount. Zero-filled. |
| GET /top-suppliers | dateFrom, dateTo, currency, limit | Supplier ranking: supplierName, invoiceCount, totalAmount, percentage. Plus othersCount/othersAmount |
| GET /pending-actions | limit (default 10) | Pending invoices: id, invoiceNumber, supplierName, totalAmount, currency, sourceType, confidenceScore, createdAt, daysPending |
| GET /status-timeline | days (default 30) | Daily activity: date, created, verified, rejected counts. Zero-filled. |

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 2-3 days

### Frontend Tech Stack
- Next.js 14+ (App Router), React 19, TypeScript 5.x
- Tailwind CSS 3.x, Shadcn/ui
- TanStack Query 5.x, Zustand 4.x, Axios
- **Recharts 2.x** (charting library)

---

## OBJECTIVE

Build an interactive, responsive dashboard as the application's home page that visualizes invoice metrics using Recharts. The dashboard displays summary KPI cards, a category distribution pie chart, a monthly trend line chart, a top suppliers bar chart, a pending actions list, and a status timeline area chart. All charts are interactive (clickable — navigating to filtered invoice lists), filterable by date range, and responsive across devices.

---

## DETAILED REQUIREMENTS

### 1. Dashboard Route and Page

**Route**: `/` (home page — sidebar link already exists from Phase 10)

**File**: `frontend/src/app/(dashboard)/page.tsx`

The dashboard is the first page users see after login. Load all data in parallel and render progressively.

### 2. Date Range and Currency Filter

**Location**: Top-right of the dashboard page header, inline with page title "Dashboard"

**Date Range:**
- Quick preset buttons/tabs: "Bu Ay" (This Month), "Son 3 Ay" (Last 3 Months), "Son 6 Ay" (Last 6 Months), "Bu Yıl" (This Year), "Tümü" (All Time)
- Custom range: two date pickers (From — To) with "Uygula" button
- Default: "Bu Ay"
- When changed, ALL dashboard components re-fetch with new dateFrom/dateTo

**Currency Selector:**
- Dropdown next to date range: TRY (₺, default), USD ($), EUR (€), GBP (£)
- Changes amount-related metrics across the dashboard

**State**: Store dateFrom, dateTo, currency in URL query params for shareable dashboard links. TanStack Query hooks include these in query keys.

### 3. Summary KPI Cards (4 Cards)

**Data Source**: GET /dashboard/stats

**Layout**: 4 cards in a row (desktop), 2x2 grid (mobile)

**Card 1 — "Toplam Fatura":**
- Large number: totalInvoices
- Subtitle: source breakdown — "LLM: 68,9% | e-Fatura: 21,9% | Manuel: 9,2%"
- Icon: FileText (lucide-react)
- Click → `/invoices`

**Card 2 — "Toplam Tutar":**
- Large number: totalAmount (Turkish formatted, e.g., "₺2.456.789,50")
- Subtitle: "Ortalama: ₺1.726,45"
- Icon: TrendingUp
- Click → `/invoices`

**Card 3 — "Bekleyen Onay":**
- Large number: pendingCount
- Subtitle: pendingAmount formatted
- Yellow/amber accent
- Icon: Clock
- Click → `/invoices?status=PENDING`

**Card 4 — "Onaylanan":**
- Large number: verifiedCount
- Subtitle: verifiedAmount formatted
- Green accent
- Icon: CheckCircle
- Click → `/invoices?status=VERIFIED`

**Styling**: Shadcn/ui Card, subtle background accent per card, hover lift effect, skeleton loading, optional count-up animation.

### 4. Category Distribution — Pie Chart

**Data Source**: GET /dashboard/categories

**Location**: Left side of second row (50% desktop width)

**Recharts PieChart (donut style):**
- Inner radius = 60% of outer radius
- Slice colors from API's `categoryColor` field
- External labels: category name + percentage
- Tooltip: "Teknoloji: 245 fatura — ₺456.789,00 (18,6%)"
- Legend below chart with color dots
- Center of donut: total invoice count or total amount

**Interactions**: Click slice → `/invoices?categoryId={id}`

**Empty state**: "Henüz kategorize edilmiş fatura bulunmuyor"

### 5. Monthly Trend — Line Chart

**Data Source**: GET /dashboard/monthly-trend

**Location**: Right side of second row (50% desktop width)

**Recharts LineChart (or ComposedChart):**
- X-axis: Month labels (Turkish abbreviated — "Oca", "Şub", "Mar" — or full if space allows)
- Y-axis: Amount (abbreviated: "245K", "1,2M")
- Line 1: "Toplam Tutar" (totalAmount) — primary color, solid
- Line 2: "Onaylanan Tutar" (verifiedAmount) — green, dashed
- Optional subtle bars for invoiceCount behind lines
- Tooltip: month label + all metrics
- Grid lines

**Interactions**: Click month data point → `/invoices?dateFrom=YYYY-MM-01&dateTo=YYYY-MM-{lastDay}`

### 6. Top Suppliers — Bar Chart

**Data Source**: GET /dashboard/top-suppliers

**Location**: Left side of third row (50% desktop width)

**Recharts BarChart (horizontal):**
- Y-axis: Supplier names (truncate > 25 chars)
- X-axis: Total amount
- Amount label at bar end
- "Diğer" bar at bottom in gray for othersAmount
- Tooltip: supplier name, invoiceCount, totalAmount, percentage

**Interactions**: Click bar → `/invoices?supplierName={name}`

### 7. Pending Actions — List

**Data Source**: GET /dashboard/pending-actions

**Location**: Right side of third row (50% desktop width)

**Compact table/card list:**

Each item shows:
- Invoice number (linked to `/invoices/{id}/verify`)
- Supplier name
- Amount + currency
- Source type badge (LLM / e-Fatura / Manuel)
- Confidence score badge (green/yellow/red) — LLM source only
- "X gün bekliyor" — red highlight if > 7 days
- "Doğrula" action button → `/invoices/{id}/verify`

**Header**: "Bekleyen İşlemler (47)"
**Footer**: "Tümünü Görüntüle" → `/invoices?status=PENDING`
**Empty**: "Bekleyen fatura bulunmuyor — Tüm faturalar işlenmiş!" with checkmark

### 8. Status Timeline — Area Chart

**Data Source**: GET /dashboard/status-timeline

**Location**: Full width, fourth row

**Recharts AreaChart (stacked):**
- X-axis: Dates (DD.MM format, last 30 days)
- Y-axis: Count
- Three stacked areas:
  - "Oluşturulan" — blue
  - "Onaylanan" — green
  - "Reddedilen" — red
- Tooltip: date + all counts
- Smooth curves (type="monotone")
- Legend

**Interactions**: Click date → `/invoices?dateFrom={date}&dateTo={date}`
---
### 9. LLM Extraction Performance Card (ADMIN/MANAGER Only)

**Data Source:** GET /dashboard/extraction-performance (Phase 26-A)

**Location:** Full width, between the status timeline and the system health panel (Phase 40 addition). Only visible to ADMIN and MANAGER roles.

**Layout — Compact Summary Card:**

A single card with a horizontal layout containing:

1. **Success Rate — Donut Mini Chart:**
   - Small Recharts PieChart (60px diameter) showing success vs failure ratio
   - Center text: "94%" (success rate)
   - Green for success, red for failure

2. **Key Metrics — Three Stat Blocks:**
   - "Toplam Çıkarım": totalExtractions count
   - "Ortalama Güven": averageConfidence score with color badge
   - "Ortalama Süre": averageDuration formatted as "2.3 sn"

3. **Provider Breakdown — Mini Bar Chart:**
   - Three horizontal bars (one per provider): Gemini, GPT, Claude
   - Bar shows attempt count, colored by success rate (green if >90%, yellow if 70-90%, red if <70%)
   - Tooltip: provider name, attempts, success count, failure count, fallback count

**Empty State:** If no LLM extractions exist, show: "Henüz LLM ile çıkarım yapılmadı."

**Conditional Rendering:** Only render this section if the user's role is ADMIN or MANAGER. Do not make the API call for ACCOUNTANT or INTERN.
---

### 10. Dashboard Layout Grid

**Desktop (≥ 1280px):**
Row 1: [KPI-1] [KPI-2] [KPI-3] [KPI-4]
Row 2: [Pie Chart 50%] [Line Chart 50%]
Row 3: [Bar Chart 50%] [Pending List 50%]
Row 4: [Status Timeline 100%]
Row 5: [LLM Extraction Performance 100%] — ADMIN/MANAGER only
Row 6: [System Health Panel 100%] — ADMIN only (Phase 40 addition)

**Tablet (768-1279px):**
KPIs: 2x2, Charts: full width stacked

**Mobile (< 768px):**
All components full width stacked. KPIs: 2 per row. Charts: reduced height (250px).

### 11. Data Fetching

**Parallel fetching** — all 6 API calls fire simultaneously on mount. Each section has its own TanStack Query hook with independent loading/error states.

**TanStack Query config:**
- Query keys include dateFrom, dateTo, currency
- staleTime: 30 seconds
- refetchOnWindowFocus: true
- retry: 2

**Custom hooks**: `useDashboardStats()`, `useCategoryDistribution()`, `useMonthlyTrend()`, `useTopSuppliers()`, `usePendingActions()`, `useStatusTimeline()`

### 12. Loading, Error, and Empty States

**Loading**: Each section shows independent skeleton placeholders.
**Error**: Failed section shows "Veri yüklenirken hata oluştu" with "Tekrar Dene" button. Other sections unaffected.
**Empty (new company)**: Welcome message — "Henüz fatura bulunmuyor. İlk faturanızı yükleyin!" with CTA → `/upload`

### 13. Turkish Formatting

- Amounts: `Intl.NumberFormat('tr-TR', { style: 'currency', currency })` → "₺2.456.789,50"
- Large numbers on axes: abbreviated "245K", "1,2M"
- Dates: DD.MM.YYYY
- Percentages: "18,6%"
- Counts: "1.423 fatura"

### 14. Dark Mode

All charts work in light and dark modes. Chart background transparent. Grid/text colors from CSS variables. Tooltip matches theme.

### 15. Responsive Charts

Use `<ResponsiveContainer width="100%" height={300}>`. Reduce to 250px on mobile. Labels truncate/rotate on small screens.

---
### 16. Admin System Health Panel (ADMIN Only)

For users with the ADMIN role, add a "Sistem Durumu" (System Health) section at the bottom of the dashboard page. This section is conditionally rendered — hidden for MANAGER, ACCOUNTANT, and INTERN roles.

**Data Source**: `GET /api/v1/admin/system/status` (Phase 40 endpoint)

**16.1 Service Health Cards**

Display a row of small status cards for each service:
- Backend (Spring Boot)
- Extraction Service (Python FastAPI)
- PostgreSQL
- Redis
- RabbitMQ

Each card shows:
- Service name
- Status indicator: green dot for UP, red dot for DOWN, gray dot for UNKNOWN
- If DOWN: show since when (if available from the API response)

**16.2 Resource Usage Summary**

Display a compact grid showing:
- JVM heap: used / max (with a mini progress bar, color-coded: green <70%, yellow 70-85%, red >85%)
- DB connection pool: active / max (same color logic)
- Disk usage: used percentage (same color logic)

**16.3 LLM Cost Summary Card**

Display the LLM cost data from the system status endpoint (or from `GET /api/v1/admin/llm-usage/summary` if Phase 40 is completed):
- "Bu Ay LLM Maliyeti" — current month cost / monthly limit with a progress bar
- Percentage used with color coding (green <60%, yellow 60-80%, red >80%)
- Small text: "Günlük: $X / $Y" for today's cost vs daily limit
- Provider breakdown: three small badges showing per-provider cost (Gemini: $X, GPT: $Y, Claude: $Z)

If Phase 40 is not yet completed, this card should gracefully handle a 404 response and show "Maliyet verileri henüz mevcut değil" (Cost data not yet available).

**16.4 Recent Alerts**

Display the last 5 alerts from the system status response:
- Each alert: severity badge (CRITICAL=red, HIGH=orange, WARN=yellow), message, timestamp
- If no alerts: show "Son 24 saatte alarm yok" (No alerts in last 24 hours)

**16.5 Conditional Rendering**

- Use the auth store to check the user's role
- Only render section 15 if `user.role === 'ADMIN'`
- If the user is not ADMIN, do not make the API call at all (avoid unnecessary 403 responses)

**16.6 Error Handling**

- If the system status endpoint returns an error (403, 500, network error), show a subtle error card: "Sistem durumu yüklenemedi" with a retry button
- Do NOT let this section's failure affect the rest of the dashboard — it should fail independently
---

## TESTING REQUIREMENTS

### Component Tests
- Dashboard renders all 6 sections
- KPI cards display correct formatted values
- Charts render with correct data points
- Pending list renders correct items
- Date range change triggers refetch
- Currency change triggers refetch
- Chart click navigations work correctly
- Loading skeletons display
- Error state with retry button works
- Empty state for new company
- Responsive: mobile stacks vertically
- Dark mode renders correctly

### Integration Tests
- Mock all 6 endpoints → verify data flows correctly
- Date/currency change → correct params sent
- One endpoint error → only that section shows error

---

## RESULT FILE

**`docs/FURKAN/step_results/faz_26.1_result.md`**

Include: execution summary, completed tasks checklist, files created/modified, component architecture, Recharts integration details, screenshots/UI descriptions (default, dark mode, loading, error, empty, mobile, tooltip), test results, performance notes, issues encountered, next steps.

---

## DEPENDENCIES

### Requires
- **Phase 10**: Layout, sidebar, dark/light mode
- **Phase 11**: Auth store, Axios interceptor
- **Phase 12**: TanStack Query patterns, status badges, currency formatting
- **Phase 23-B**: URL-based filter state (drill-down navigations)
- **Phase 26-A**: All 6 dashboard backend endpoints

### Required By
- **Phase 27**: Notification system — may add indicator near dashboard header
- **Phase 40**: Monitoring (provides the GET /api/v1/admin/system/status endpoint consumed by section 15)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Dashboard renders at `/` as the home page
- [ ] 4 KPI cards display with correct values and Turkish formatting
- [ ] KPI card click navigates to filtered invoice list
- [ ] Category distribution pie chart renders with correct data
- [ ] Pie chart slice click navigates to filtered invoice list
- [ ] Monthly trend line chart renders 12 months
- [ ] Line chart has two lines: invoice count and total amount
- [ ] Top suppliers bar chart renders top 10 ranked
- [ ] Pending actions list shows items with urgency indicators
- [ ] Pending action items have clickable action buttons
- [ ] Status timeline area chart shows 30-day activity
- [ ] Date range filter refreshes all dashboard sections
- [ ] Currency selector refreshes amount-based metrics
- [ ] Loading skeletons display while data is fetching
- [ ] Error state displays on API failure
- [ ] Empty state displays when no data is available
- [ ] Responsive layout works on desktop (≥1024px)
- [ ] Responsive layout works on tablet (768-1023px)
- [ ] Responsive layout works on mobile (<768px)
- [ ] Dark mode works correctly for all charts and cards
- [ ] All text is in Turkish
- [ ] `npm run build` completes without errors
- [ ] All tests pass
- [ ] Result file created at docs/FURKAN/step_results/faz_26.1_result.md
---

## SUCCESS CRITERIA

1. ✅ Dashboard renders at `/` with all 6 sections
2. ✅ 4 KPI cards with correct Turkish formatting and click navigation
3. ✅ Pie chart shows category distribution with API colors and drill-down
4. ✅ Line chart shows 12-month trend with two lines
5. ✅ Bar chart shows top 10 suppliers ranked
6. ✅ Pending actions list with urgency indicators and action buttons
7. ✅ Status timeline shows 30-day activity
8. ✅ Date range filter refreshes all data
9. ✅ Currency selector refreshes amount metrics
10. ✅ All chart clicks navigate to filtered invoice list
11. ✅ Loading, error, and empty states work correctly
12. ✅ Responsive on desktop, tablet, mobile
13. ✅ Dark mode support for all charts
14. ✅ Turkish UI text throughout
15. ✅ All tests pass
16. ✅ Result file at docs/FURKAN/step_results/faz_26.1_result.md
17. ✅ Admin-only "Sistem Durumu" panel renders for ADMIN users
18. ✅ System health panel hidden for non-ADMIN users
19. ✅ Service health cards show UP/DOWN status correctly
20. ✅ LLM cost summary card shows monthly cost with progress bar
21. ✅ Recent alerts display with severity color coding
22. ✅ System health panel handles API errors gracefully without breaking the dashboard
23. ✅ LLM extraction performance card renders for ADMIN/MANAGER
24. ✅ Provider breakdown shows success rates per provider

---

## IMPORTANT NOTES

1. **ResponsiveContainer**: Always wrap Recharts charts in `<ResponsiveContainer>`. Never set fixed pixel width.
2. **Parallel Fetching**: Fire all 6 requests simultaneously. Each section renders independently.
3. **Category Colors from API**: Pie chart colors come from `categoryColor` field. Use them directly.
4. **Drill-Down Uses Phase 23-B URL Params**: Navigate to `/invoices?categoryId={uuid}` — Phase 23-B reads and applies the filter.
5. **Memoize Chart Data**: Use `useMemo` for chart data to avoid unnecessary Recharts re-renders.
6. **Consistent Chart Heights**: 300px desktop, 250px mobile for clean grid alignment.
7. **Empty Charts**: Show explicit empty state message instead of blank chart area.
8. **Accessibility**: Charts have `aria-label`, pending table is keyboard-navigable, KPI cards focusable.
