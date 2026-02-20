# PHASE 37: FRONTEND COMPONENT TESTS & END-TO-END (E2E) TESTS

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid — Spring Boot (8082), Python FastAPI (8001), Next.js (3001)

### Current State (Phases 0-36 Completed)
All features implemented. Phase 35-A/B added unit tests (backend + Python). Phase 36-A/B added integration tests (backend + Python). Frontend has zero tests so far.

### Frontend Stack (Phases 10-12, 21-22, 23b, 26b, 27f, 29f, 30b, 33, 34)
- **Next.js 14+** App Router with file-based routing
- **React 19** with TypeScript
- **Tailwind CSS** + **Shadcn/ui** component library
- **TanStack Query 5.x** for server state
- **Zustand** for client state (auth store, notification store)
- **React Hook Form** + **Zod** for form validation
- **Axios** for HTTP client with interceptors (JWT token, refresh, error handling)
- **Recharts** for dashboard charts
- **next-intl** for i18n (Phase 34)
- **next-pwa** for PWA (Phase 33)
- **WebSocket** (SockJS + STOMP) for notifications

### Frontend Pages/Features to Test
- Auth: Login, Register
- Dashboard: KPI cards, charts, pending actions
- Invoices: List (table, pagination, sorting, filtering), Detail, Create, Edit
- Upload: Drag-and-drop, batch upload, progress tracking
- Verification: Split-view, confidence indicators, verify/reject
- Categories: CRUD
- Notifications: Bell icon, dropdown, full page, settings
- Templates: List, detail drawer
- Rules: Card list, rule builder (multi-step form)
- Export: Format selection dialog
- Version History: Timeline, diff viewer
- Settings: Notification preferences
- PWA: Install prompt, update prompt, offline banner
- i18n: Language switcher

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 3-4 days

---

## OBJECTIVE

Implement two levels of frontend testing:

1. **Component Tests** (Jest + React Testing Library + MSW): Test individual React components in isolation with mocked API responses
2. **E2E Tests** (Playwright): Test critical user flows end-to-end in a real browser against running services

---

## DETAILED REQUIREMENTS

### PART 1: COMPONENT TESTS (Jest + React Testing Library)

#### 1.1 Test Infrastructure Setup

**Install dependencies:**
- `jest` (or use Next.js built-in — `@jest/globals`)
- `@testing-library/react`
- `@testing-library/jest-dom` (custom matchers: toBeInTheDocument, toBeVisible, etc.)
- `@testing-library/user-event` (realistic user interaction simulation)
- `msw` (Mock Service Worker — intercepts API calls at the network level)
- `jest-environment-jsdom`

**Configuration files:**

- `jest.config.ts`: Configure for Next.js (module aliases, transform, test environment)
- `jest.setup.ts`: Import `@testing-library/jest-dom`, configure MSW server start/stop
- `src/test-utils/`: Custom render utilities

**Custom render with providers:**

Create `src/test-utils/render.tsx` that wraps components with all required providers:
- `NextIntlClientProvider` (i18n — with Turkish messages)
- `QueryClientProvider` (TanStack Query)
- Theme provider (dark/light mode)
- Router mock (Next.js router)

This way, every test gets the same provider environment.

**MSW Setup:**

Create `src/mocks/`:
- `handlers.ts`: Default API mock handlers for common endpoints
- `server.ts`: MSW server setup for Jest (node environment)

Default handlers should cover:
- GET /api/v1/invoices → paginated invoice list
- GET /api/v1/invoices/:id → single invoice detail
- GET /api/v1/categories → category list
- GET /api/v1/dashboard/stats → dashboard data
- POST /api/v1/auth/login → successful login response
- POST /api/v1/auth/register → successful register response
- GET /api/v1/notifications/unread-count → unread count

Individual tests can override these handlers for specific scenarios.

---

#### 1.2 Auth Component Tests

**`LoginPage.test.tsx`:**
- Renders login form with email and password fields
- Shows validation errors when submitting empty form
- Shows validation error for invalid email format
- Shows password requirements hint
- Submits form with valid credentials → calls login API → redirects to dashboard
- Shows error message on invalid credentials (MSW returns 401)
- Shows loading spinner during submission
- "Kayıt Ol" link navigates to register page
- Shows account locked message (MSW returns 423)
- Rate limit error displayed (MSW returns 429)

**`RegisterPage.test.tsx`:**
- Renders register form with all fields (name, email, password, confirm password)
- Validates all fields required
- Validates password requirements (min 8 chars, uppercase, lowercase, digit, special)
- Validates password confirmation match
- Submits form → calls register API → redirects to login
- Shows error for duplicate email (MSW returns 409)

---

#### 1.3 Invoice Components Tests

**`InvoiceList.test.tsx`:**
- Renders table with invoice data from MSW
- Shows loading skeleton while fetching
- Shows empty state when no invoices
- Pagination: next/previous buttons work
- Sorting: clicking column header changes sort order
- Status badges render with correct colors (PENDING=yellow, VERIFIED=green, REJECTED=red)
- Delete button shows confirmation dialog
- Confirming delete calls DELETE API
- Error state shown when API fails

**`InvoiceDetail.test.tsx`:**
- Renders all invoice fields (number, date, supplier, amounts)
- Renders invoice items table
- Shows correct status badge
- Verify button visible for PENDING invoices
- Reject button visible for PENDING invoices
- Verify button calls API and updates status
- Shows 404 when invoice not found

**`InvoiceForm.test.tsx` (Create/Edit):**
- Renders all form fields
- Validates required fields
- Validates amount format (numeric, positive)
- Validates date format
- Dynamic line items: add item, remove item
- Submit creates invoice (POST API call)
- Edit mode: pre-fills existing data
- Submit updates invoice (PUT API call)

---

#### 1.4 Filter Panel Tests

**`FilterPanel.test.tsx`:**
- Renders all filter controls (date range, supplier, category, status, amount range)
- Selecting a filter updates URL query params
- Clear filters resets all selections
- Multiple filters can be combined
- Search input debounces before API call

---

#### 1.5 Dashboard Tests

**`Dashboard.test.tsx`:**
- KPI cards render with correct values
- Charts render (verify Recharts container exists)
- Pending actions list renders
- Date range selector changes API query
- Error state for individual sections (one fails, others work)
- Empty state for new company

---

#### 1.6 Upload Tests

**`UploadArea.test.tsx`:**
- Renders drag-and-drop zone
- File input accepts correct formats (jpg, png, pdf, xml)
- Rejects invalid file types (shows error)
- Rejects oversized files
- Upload progress bar shown during upload
- Success message after upload

---

#### 1.7 Notification Tests

**`NotificationBell.test.tsx`:**
- Bell icon renders
- Badge shows unread count
- Badge hidden when count is 0
- "9+" shown when count > 9
- Click opens dropdown

**`NotificationDropdown.test.tsx`:**
- Shows recent notifications
- Clicking notification marks as read
- "Tümünü okundu işaretle" marks all as read
- "Tüm bildirimleri göster" navigates to /notifications

---

#### 1.8 Rule Builder Tests

**`RuleBuilder.test.tsx`:**
- Step 1: Name and trigger point fields render
- Step 2: Can add condition row, select field/operator/value
- Step 2: Can toggle AND/OR logic
- Step 3: Can add action row, select action type and params
- Step 4: Review shows summary in natural language
- Submit creates rule (POST API call)
- Dynamic add/remove rows work correctly

---

#### 1.9 Common Component Tests

**`ConfirmDialog.test.tsx`:**
- Opens on trigger
- Shows correct message
- Confirm button calls onConfirm
- Cancel button closes dialog

**`EmptyState.test.tsx`:**
- Renders title and description
- CTA button navigates correctly

**`Pagination.test.tsx`:**
- Previous/Next buttons
- Disabled on first/last page
- Page number display

---

### PART 2: E2E TESTS (Playwright)

#### 2.1 Playwright Setup

**Install:**
- `@playwright/test`
- `npx playwright install` (browser binaries)

**Configuration (`playwright.config.ts`):**
- Base URL: `http://localhost:3001`
- Browsers: Chromium (primary), optionally Firefox and WebKit
- Timeout: 30 seconds per test
- Retries: 1 (flaky test protection)
- Screenshots on failure
- Video on failure (optional)
- Web server: auto-start Next.js dev server before tests (or expect it running)

**Test Environment:**

E2E tests need the FULL stack running:
- Spring Boot backend (port 8082) — or a mock server
- Python extraction service (port 8001) — or a mock server
- Next.js frontend (port 3001)
- PostgreSQL, Redis, RabbitMQ

**Two approaches:**

**Option A (Recommended for graduation project):** Run E2E tests against the full Docker Compose stack. Start all services with `docker-compose up`, seed test data, then run Playwright.

**Option B (Simpler):** Mock the backend API with MSW in the browser and only test the frontend behavior. This is faster and more reliable but doesn't test real integration.

Document which approach is chosen. Option A is more impressive but more complex. Option B is acceptable for MVP.

---

#### 2.2 E2E Test: Authentication Flow

**`auth.spec.ts`:**

- **Register → Login flow:**
  1. Navigate to /register
  2. Fill in name, email, password, confirm password
  3. Click "Kayıt Ol"
  4. Redirected to /login (or auto-login)
  5. Fill in email, password
  6. Click "Giriş Yap"
  7. Redirected to dashboard
  8. User name visible in header

- **Login validation:**
  1. Navigate to /login
  2. Click "Giriş Yap" without filling form
  3. Validation errors visible
  4. Fill invalid email → error shown
  5. Fill valid email + wrong password → "Hatalı giriş" error

- **Protected routes:**
  1. Without logging in, navigate to /invoices
  2. Redirected to /login

- **Logout:**
  1. Login
  2. Click user menu → Çıkış Yap
  3. Redirected to /login
  4. Cannot access /invoices

---

#### 2.3 E2E Test: Invoice Lifecycle

**`invoice-lifecycle.spec.ts`:**

- **Full lifecycle:**
  1. Login as ACCOUNTANT
  2. Navigate to /invoices
  3. Click "Yeni Fatura" (or navigate to create)
  4. Fill invoice form (number, date, supplier, items, amounts)
  5. Click "Kaydet"
  6. Redirected to invoice detail
  7. Verify data displayed correctly
  8. Status is PENDING

- **Verify flow (if running with real backend):**
  1. Login as MANAGER
  2. Navigate to PENDING invoice
  3. Click "Doğrula"
  4. Confirm dialog → "Onayla"
  5. Status changes to VERIFIED

---

#### 2.4 E2E Test: Upload Flow

**`upload.spec.ts`:**

- **Single file upload:**
  1. Login
  2. Navigate to /upload
  3. Upload a test image file (use `page.setInputFiles()`)
  4. Progress bar visible
  5. Upload completes → redirected to verification page (or success message)

- **Invalid file:**
  1. Attempt to upload a .txt file
  2. Error message displayed

---

#### 2.5 E2E Test: Filtering & Export

**`filtering-export.spec.ts`:**

- **Filtering:**
  1. Login
  2. Navigate to /invoices
  3. Select status filter → "Doğrulandı"
  4. Table updates → only VERIFIED invoices shown
  5. Clear filter → all invoices shown again

- **Export:**
  1. Apply a filter
  2. Click "Dışa Aktar"
  3. Select XLSX format
  4. Click "İndir"
  5. Verify download started (check download event)

---

#### 2.6 E2E Test: Dashboard

**`dashboard.spec.ts`:**

- **Dashboard loads:**
  1. Login
  2. Landing page is dashboard
  3. KPI cards visible with numeric values
  4. Charts visible (at least containers rendered)
  5. Pending actions section visible

---

#### 2.7 E2E Test: Responsive & PWA (Lightweight)

**`responsive.spec.ts`:**

- Mobile viewport (375x667):
  1. Sidebar is collapsed
  2. Hamburger menu visible
  3. Click hamburger → sidebar opens
  4. Navigation works

---

#### 2.8 E2E Test: i18n

**`i18n.spec.ts`:**

- **Language switch:**
  1. Login (Turkish by default)
  2. Verify Turkish text: "Faturalar", "Dashboard"
  3. Switch language to English
  4. Verify English text: "Invoices", "Dashboard"
  5. Refresh page → English persists

---

### PART 3: CI/CD INTEGRATION

Update GitHub Actions workflow:

**Component tests:**
- Run `npm test -- --coverage` in the frontend job
- Fail if tests fail
- Publish coverage report

**E2E tests:**
- Separate workflow or job
- Start Docker Compose services (or use Option B with MSW)
- Run `npx playwright test`
- Upload test artifacts (screenshots, videos on failure)
- Run on merge to main (not every push — E2E is slow)

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_37.0_result.md`

The result file must include:

1. Phase summary
2. Test infrastructure setup (Jest config, MSW setup, Playwright config)
3. Component test count by feature area
4. Total component test count
5. E2E test count by flow
6. Total E2E test count
7. Component test coverage report
8. Files created (all test files with paths)
9. MSW handler documentation
10. Custom test utilities documentation
11. E2E approach chosen (full stack vs mocked backend) and rationale
12. Playwright configuration details
13. CI/CD changes
14. Screenshots from E2E tests (Playwright generates these on failure)
15. Bugs found during testing
16. Issues encountered and solutions
17. Next steps (Phase 38 Performance Optimization)

---

## DEPENDENCIES

### Requires (must be completed first)
- **All frontend phases (10-12, 21-22, 23b, 26b, 27f, 29f, 30b, 33, 34)**: All UI code implemented
- **Phase 35-A/B + 36-A/B**: Test patterns and fixtures for reference

### Required By
- **Phase 38**: Performance Optimization (Lighthouse scores from E2E)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Jest + React Testing Library configured for Next.js 14+
- [ ] MSW configured with default API handlers for all endpoints
- [ ] Custom render utility wraps components with all required providers
- [ ] Login page tests: empty form shows validation errors
- [ ] Login page tests: invalid credentials shows error message
- [ ] Login page tests: successful login redirects to dashboard
- [ ] Register page tests: validation errors for invalid input
- [ ] Register page tests: successful registration flow
- [ ] Invoice list tests: table renders with mock data
- [ ] Invoice list tests: pagination works
- [ ] Invoice list tests: sorting works
- [ ] Invoice list tests: empty state renders
- [ ] Invoice list tests: error state renders
- [ ] Invoice detail tests: all fields render correctly
- [ ] Invoice detail tests: verify/reject buttons work
- [ ] Invoice form tests: validation errors shown
- [ ] Invoice form tests: line item add/remove works
- [ ] Invoice form tests: create and edit modes work
- [ ] Filter panel tests: filter controls render and interact
- [ ] Filter panel tests: URL state synchronization works
- [ ] Dashboard tests: KPI cards render with data
- [ ] Dashboard tests: chart containers render
- [ ] Upload area tests: file type validation works
- [ ] Upload area tests: progress indicator works
- [ ] Upload area tests: success and error states work
- [ ] Notification bell tests: unread badge shows count
- [ ] Notification dropdown tests: notifications list renders
- [ ] Rule builder tests: multi-step form works
- [ ] Playwright installed and configured
- [ ] E2E: register → login → see dashboard → logout
- [ ] E2E: create invoice → verify in list
- [ ] E2E: apply filters → export filtered data
- [ ] E2E: dashboard loads with charts
- [ ] E2E: responsive mobile layout renders correctly
- [ ] E2E: language switch (TR ↔ EN) works
- [ ] Total component test count ≥ 80
- [ ] Total E2E test count ≥ 10
- [ ] All tests pass
- [ ] CI/CD: component tests run on every push
- [ ] CI/CD: E2E tests run on merge to main
- [ ] Result file created at docs/FURKAN/step_results/faz_37.0_result.md
---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Jest + React Testing Library configured for Next.js 14+
2. ✅ MSW configured with default API handlers
3. ✅ Custom render utility with all providers
4. ✅ Login page: all validation and submission scenarios tested
5. ✅ Register page: all validation and submission scenarios tested
6. ✅ Invoice list: rendering, pagination, sorting, empty state, error state tested
7. ✅ Invoice detail: all fields, verify/reject buttons tested
8. ✅ Invoice form: validation, line items, create and edit modes tested
9. ✅ Filter panel: filter controls and URL state tested
10. ✅ Dashboard: KPI cards and chart containers tested
11. ✅ Upload area: file type validation, progress, success/error tested
12. ✅ Notification bell + dropdown tested
13. ✅ Rule builder multi-step form tested
14. ✅ Playwright installed and configured
15. ✅ E2E: Auth flow (register → login → protected route → logout)
16. ✅ E2E: Invoice creation flow
17. ✅ E2E: Filtering and export flow
18. ✅ E2E: Dashboard loads correctly
19. ✅ E2E: Responsive mobile layout
20. ✅ E2E: Language switching
21. ✅ Total component test count ≥ 80
22. ✅ Total E2E test count ≥ 10
23. ✅ All tests pass
24. ✅ CI/CD runs component tests on every push, E2E on merge
25. ✅ Result file created at docs/FURKAN/step_results/faz_37.0_result.md

---

## IMPORTANT NOTES

1. **Component Tests First, E2E Second**: Component tests are faster, more reliable, and catch more bugs per time invested. Prioritize component tests. E2E tests cover the top critical flows only — don't try to E2E test every feature.

2. **MSW Over Manual Mocks**: Use MSW (Mock Service Worker) for API mocking. It intercepts at the network level, so the actual fetch/axios calls run. This is more realistic than mocking the HTTP client directly.

3. **Test User Behavior, Not Implementation**: Use React Testing Library's approach: find elements by role, label, text — not by CSS class or test ID. Test what the user sees and does, not internal component state.

4. **i18n in Tests**: Since Phase 34 added i18n, all text queries in tests should work with Turkish text (the default locale). Wrap all test renders with the i18n provider.

5. **TanStack Query in Tests**: Wrap components with `QueryClientProvider` using a fresh `QueryClient` per test (to avoid cache leaks between tests). Alternatively, use MSW to control what the queries return.

6. **E2E Test Data**: E2E tests need predictable test data. If running against a real backend, seed specific test data before each E2E test suite. If using MSW in browser, define the exact responses.

7. **Playwright Selectors**: Use stable selectors. Prefer `data-testid` attributes for elements that are hard to select otherwise. Add `data-testid` to key interactive elements during this phase if needed.

8. **Don't Over-Test Shadcn/ui**: Shadcn/ui components (Button, Dialog, Input) are already tested by the library. Test YOUR usage of them (correct props, correct behavior in context), not the components themselves.

9. **Screenshots Are Documentation**: Playwright screenshots on failure are valuable for the graduation report. Configure to always capture screenshots for failed tests.

10. **Flaky E2E Tests**: E2E tests can be flaky due to timing. Use Playwright's auto-waiting (it waits for elements to be actionable). Avoid `page.waitForTimeout()` — use `expect(locator).toBeVisible()` which auto-retries.
