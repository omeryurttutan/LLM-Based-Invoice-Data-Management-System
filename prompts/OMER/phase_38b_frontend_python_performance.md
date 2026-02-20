# PHASE 38-B: PERFORMANCE OPTIMIZATION — FRONTEND & PYTHON SERVICE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid — Spring Boot (8082), Python FastAPI (8001), Next.js (3001)

### Current State (Phases 0-37 Completed)
All features implemented and tested. Frontend and Python service have not been explicitly optimized.

### Existing Frontend Stack
- Next.js 14+ App Router, React 19, TypeScript, Tailwind CSS, Shadcn/ui
- TanStack Query 5.x (server state), Zustand (client state)
- Recharts (dashboard charts), next-intl (i18n), next-pwa (PWA/Service Worker)
- Axios with interceptors

### Existing Python Service
- FastAPI with Pillow preprocessing, 3 LLM clients, fallback chain, XML parser
- RabbitMQ consumer/producer

### Phase Assignment
- **Assigned To**: FURKAN (Frontend + Python Developer)
- **Estimated Duration**: 2-3 days
- **Parallel**: ÖMER works on Phase 38-A (Backend performance) simultaneously

---

## OBJECTIVE

Optimize frontend and Python service performance:

1. **Frontend**: Bundle size reduction, lazy loading, code splitting, image optimization, TanStack Query cache tuning, Lighthouse score improvement
2. **Python Service**: Image preprocessing optimization, concurrent request handling, memory efficiency

---

## DETAILED REQUIREMENTS

### PART 1: FRONTEND PERFORMANCE

#### 1. Bundle Analysis & Code Splitting

**1.1 Analyze Bundle Size:**

Install and run `@next/bundle-analyzer`:

```bash
npm install @next/bundle-analyzer
```

Configure in `next.config.mjs` and generate bundle report. Identify:
- Largest modules (Recharts, Shadcn components, date libraries, etc.)
- Modules loaded on every page that should be lazy
- Duplicate dependencies

Document the initial bundle sizes (total JS, per-page).

**1.2 Dynamic Imports for Heavy Components:**

Use `next/dynamic` to lazy-load heavy components that aren't needed on initial render:

| Component | When to Load | Strategy |
|---|---|---|
| Recharts (dashboard charts) | Dashboard page only | `dynamic(() => import(...), { ssr: false })` |
| Invoice verification split-view | Verification page only | Dynamic import |
| Rule builder multi-step form | Rules page only | Dynamic import |
| Export dialog | On "Dışa Aktar" click | Dynamic import |
| Version history timeline | Version tab click | Dynamic import |
| Notification settings grid | Settings page only | Dynamic import |
| Rich diff viewer | Version compare only | Dynamic import |

**1.3 Route-Based Code Splitting:**

Next.js App Router automatically splits code per route. Verify:
- Each page in `app/(dashboard)/` is a separate chunk
- Shared layout code is in the layout chunk (not duplicated per page)
- Auth pages `(auth)/` are separate from dashboard pages

---

#### 2. Image & Asset Optimization

**2.1 Next.js Image Component:**

Replace all `<img>` tags with `next/image`:
- Automatic WebP/AVIF conversion
- Lazy loading by default
- Responsive sizes via `sizes` prop
- Proper `width` and `height` to prevent layout shift

**2.2 SVG Optimization:**

- Inline small SVGs (icons) — Lucide React icons are already optimized
- For larger SVGs (illustrations, empty states): optimize with SVGO or use as React components

**2.3 Font Optimization:**

- Use `next/font` for loading fonts (if custom fonts are used)
- Preconnect to font CDNs
- Use `font-display: swap` to prevent FOIT (Flash of Invisible Text)

---

#### 3. TanStack Query Cache Configuration

**3.1 Global Defaults:**

Configure `QueryClient` with sensible defaults:

```tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30 * 1000,      // 30 seconds — data is "fresh" for 30s
      gcTime: 5 * 60 * 1000,     // 5 minutes — unused cache kept for 5 min
      retry: 1,                   // 1 retry on failure
      retryDelay: 1000,           // 1 second between retries
      refetchOnWindowFocus: false, // Don't refetch when tab regains focus (annoying)
    },
  },
});
```

**3.2 Per-Query Overrides:**

Different data types have different freshness needs:

| Query | staleTime | gcTime | Rationale |
|---|---|---|---|
| Invoice list | 30s | 5 min | Changes often, but not in real-time |
| Invoice detail | 60s | 10 min | Changes less frequently |
| Categories | 5 min | 30 min | Rarely changes |
| Dashboard stats | 60s | 5 min | OK to be slightly stale |
| Notification unread count | 10s | 1 min | Should be fairly fresh |
| Templates | 5 min | 30 min | Rarely changes |
| Rules | 5 min | 30 min | Rarely changes |
| User profile | 10 min | 30 min | Almost never changes |

**3.3 Prefetching:**

Prefetch data for likely next navigation:
- On invoice list page: prefetch the first invoice detail (hover or on page load)
- On dashboard: prefetch invoice list data
- On sidebar hover: prefetch the target page data

```tsx
// Prefetch on hover
<Link href={`/invoices/${id}`} onMouseEnter={() => {
  queryClient.prefetchQuery({ queryKey: ['invoice', id], queryFn: ... });
}}>
```

**3.4 Optimistic Updates:**

Verify that mutations use optimistic updates for instant UI feedback:
- Mark notification as read → instant UI update, API call in background
- Verify invoice → status badge changes immediately
- Delete invoice → removed from list immediately

If not already implemented, add optimistic updates to key mutations.

---

#### 4. Rendering Performance

**4.1 Virtualized Lists:**

For large invoice lists, consider virtualizing the table rows with `@tanstack/react-virtual` or `react-window`:

- Only render visible rows in the DOM
- Reduces DOM nodes from 100+ to ~20 (visible rows)
- Especially important for mobile where rendering is slower

For MVP: Only implement if the invoice list noticeably lags with 50+ rows. Otherwise, document as a future optimization.

**4.2 Memoization:**

Review key components for unnecessary re-renders:

- `React.memo()` for pure display components (status badge, KPI card)
- `useMemo()` for expensive computations (chart data transformations)
- `useCallback()` for event handlers passed as props (filter change handlers)

Use React DevTools Profiler to identify components that re-render too often.

**4.3 Debounce Search Input:**

Verify that the search input in the filter panel debounces API calls:
- User types → wait 300ms after last keystroke → then fire API request
- Should already be implemented in Phase 23 — verify and fix if not

---

#### 5. Lighthouse Audit & Improvement

**5.1 Run Lighthouse Audit:**

Run Lighthouse in Chrome DevTools on key pages:
- Dashboard (/)
- Invoice list (/invoices)
- Login (/login)
- Upload (/upload)

Record scores for: Performance, Accessibility, Best Practices, SEO, PWA.

**5.2 Target Scores:**

| Category | Target | Notes |
|---|---|---|
| Performance | ≥ 80 | Green zone |
| Accessibility | ≥ 90 | Critical for usability |
| Best Practices | ≥ 90 | Security headers help here |
| SEO | ≥ 80 | Meta tags, semantic HTML |
| PWA | ≥ 90 | Phase 33 should cover this |

**5.3 Common Fixes:**

- **LCP (Largest Contentful Paint)**: Preload critical resources, optimize hero images
- **CLS (Cumulative Layout Shift)**: Set explicit width/height on images, reserve space for dynamic content
- **FID/INP (Interaction to Next Paint)**: Reduce JavaScript execution time, break up long tasks
- **Accessibility**: Add `alt` text to images, proper heading hierarchy, ARIA labels, color contrast
- **SEO**: Meta titles and descriptions on each page, proper `<h1>` usage, semantic HTML

---

#### 6. PWA Performance (Phase 33 Enhancement)

**6.1 Cache Hit Rate:**

Verify Service Worker caching is working effectively:
- Static assets should always be served from cache after first load
- API responses should use stale-while-revalidate effectively
- Check in Chrome DevTools → Application → Cache Storage

**6.2 Offline Performance:**

Verify that cached pages load in < 1 second when offline.

**6.3 Service Worker Update:**

Ensure the Service Worker update prompt (Phase 33) doesn't block the main thread.

---

### PART 2: PYTHON SERVICE PERFORMANCE

#### 7. Image Preprocessing Optimization

**7.1 Measure Current Performance:**

Time each preprocessing step for a typical invoice image:
- Image loading
- Rotation correction
- Contrast adjustment
- Resize
- Format conversion
- Base64 encoding

**7.2 Optimize Pillow Operations:**

- Use `Image.thumbnail()` instead of `Image.resize()` for aspect-ratio-preserving resize (faster)
- Process images in RGB mode (skip RGBA conversion when alpha isn't needed)
- For JPEG output: use `quality=85` (good balance of quality vs size)
- Skip preprocessing for already-optimized images (check dimensions and quality first)

**7.3 Memory Management:**

- Close PIL Image objects explicitly after use (`image.close()`)
- For large images: process in chunks or use memory-mapped files
- Monitor memory usage during batch processing

---

#### 8. LLM API Call Optimization

**8.1 Request Size Optimization:**

- Minimize the image size sent to LLM APIs (already done in preprocessing, but verify)
- Target: images should be < 1MB after preprocessing for optimal API response time
- Larger images = slower LLM processing

**8.2 Timeout Tuning:**

Review LLM API timeouts:
- Current: 30 seconds per provider
- Consider: reduce to 20 seconds (if typical responses come in < 10 seconds)
- Document average response times per provider

**8.3 Connection Reuse:**

Ensure HTTP clients reuse connections (keep-alive):
- `httpx.AsyncClient` should be created once (app lifespan) and reused, not created per request
- Verify each LLM client uses a shared session

---

#### 9. Concurrent Request Handling

**9.1 FastAPI Worker Configuration:**

Optimize Uvicorn worker settings for the extraction service:

```bash
uvicorn app.main:app --workers 2 --limit-concurrency 10
```

- `--workers 2`: 2 worker processes (extraction is I/O bound — waiting for LLM APIs)
- `--limit-concurrency 10`: Max 10 concurrent requests (prevent overload)

**9.2 Async Performance:**

Verify that all I/O operations in the extraction service are async:
- LLM API calls: async HTTP client (httpx)
- RabbitMQ: async consumer
- File I/O: use `aiofiles` for async file operations (if not already)

**9.3 Load Test (Coordinate with Ömer):**

Perform a simple load test with Ömer:
- Send 5, 10, 20 concurrent extraction requests
- Measure: response time distribution, error rate, memory usage
- Identify bottleneck: CPU (preprocessing), I/O (LLM API), memory

Document results in the result file.

---

#### 10. XML Parser Optimization

**10.1 Parser Selection:**

Verify using the most efficient XML parser:
- `lxml` is faster than `xml.etree.ElementTree` for large XMLs
- For small e-Invoice XMLs (< 1MB), the difference is negligible

**10.2 Namespace Handling:**

Verify XML namespace handling isn't causing repeated lookups. Cache namespace mappings.

---

### PART 3: PERFORMANCE DOCUMENTATION

#### 11. Performance Baseline Document

Create a comprehensive performance baseline document for the graduation report:

| Metric | Value | Target | Status |
|---|---|---|---|
| Login page load time | Xms | < 1s | ✅/❌ |
| Dashboard load time | Xms | < 2s | ✅/❌ |
| Invoice list (100 records) | Xms | < 1s | ✅/❌ |
| Single invoice detail | Xms | < 500ms | ✅/❌ |
| File upload + extraction | Xs | < 30s | ✅/❌ |
| XML parsing | Xms | < 500ms | ✅/❌ |
| Bundle size (total JS) | X KB | < 500KB | ✅/❌ |
| Lighthouse Performance | X | ≥ 80 | ✅/❌ |
| Lighthouse Accessibility | X | ≥ 90 | ✅/❌ |
| LLM response time (avg) | Xs | < 15s | ✅/❌ |

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_38b_result.md`

The result file must include:

1. Phase summary
2. Bundle analysis results (before/after sizes, charts)
3. Dynamic imports added (component list)
4. TanStack Query cache configuration details
5. Prefetching implementation details
6. Lighthouse audit scores (before/after per page)
7. Lighthouse issues fixed
8. Image preprocessing timing measurements
9. LLM API response time measurements
10. Concurrent request load test results
11. Python service worker configuration
12. Performance baseline document (table above)
13. Issues encountered and solutions
14. Next steps (Phase 39 Deployment)

---

## DEPENDENCIES

### Requires (must be completed first)
- **All frontend phases (10-34)**: All UI code implemented
- **All Python phases (13-19)**: All extraction code implemented
- **Phase 35-37**: Tests pass (don't break tests during optimization)

### Required By
- **Phase 39**: Deployment (optimized application for production)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Bundle analyzer report generated (document total bundle size)
- [ ] Heavy components identified and lazy-loaded with next/dynamic (≥5 components)
- [ ] Recharts lazy-loaded (only loaded on dashboard page)
- [ ] PDF viewer lazy-loaded (only loaded on verification page)
- [ ] All <img> tags replaced with next/image
- [ ] TanStack Query global defaults: staleTime, gcTime, retry configured
- [ ] Per-query staleTime overrides: dashboard (30s), invoice list (10s), categories (5min)
- [ ] Prefetching: invoice list → detail navigation prefetches on hover
- [ ] React.memo applied to expensive components (invoice table rows, chart components)
- [ ] Lighthouse Performance score ≥ 80 on dashboard page
- [ ] Lighthouse Performance score ≥ 80 on invoice list page
- [ ] Lighthouse Accessibility score ≥ 90
- [ ] Python: image preprocessing step timing documented
- [ ] Python: LLM API average response time documented per provider
- [ ] Python: HTTP client uses connection pooling (httpx with connection reuse)
- [ ] Python: Uvicorn worker count configured appropriately
- [ ] Load test: 5-10 concurrent extraction requests completed
- [ ] Load test results documented (avg response time, p95, error rate)
- [ ] Performance baseline document created at docs/performance-baseline.md
- [ ] All existing tests still pass (no regressions)
- [ ] Result file created at docs/FURKAN/step_results/faz_38b_result.md

---
## SUCCESS CRITERIA

1. ✅ Bundle analyzer report generated and documented
2. ✅ Heavy components lazy-loaded with `next/dynamic` (minimum 5 components)
3. ✅ All `<img>` tags replaced with `next/image`
4. ✅ TanStack Query global defaults configured (staleTime, gcTime, retry)
5. ✅ Per-query staleTime overrides for different data types
6. ✅ Prefetching implemented for invoice list → detail navigation
7. ✅ Lighthouse Performance score ≥ 80 on dashboard and invoice list
8. ✅ Lighthouse Accessibility score ≥ 90
9. ✅ Image preprocessing step timing documented
10. ✅ LLM API average response time documented
11. ✅ HTTP client connection reuse verified
12. ✅ Uvicorn worker configuration optimized
13. ✅ Load test (5-10 concurrent requests) completed and documented
14. ✅ Performance baseline document created
15. ✅ All existing tests still pass
16. ✅ Result file created at docs/FURKAN/step_results/faz_38b_result.md

---

## IMPORTANT NOTES

1. **Measure First, Then Optimize**: Run Lighthouse and profiling tools BEFORE making changes. Document the before state. Then optimize. Then measure again. This before/after comparison is valuable for the graduation report.

2. **Don't Break Functionality**: Performance optimization should be invisible to the user (same behavior, just faster). Run all tests after changes.

3. **Lighthouse is Not Everything**: Lighthouse scores are a guide, not a goal. A score of 75 with great UX is better than 100 with broken features. Don't sacrifice functionality for scores.

4. **Virtualized Lists are Optional**: Only implement if the invoice list noticeably lags. For < 100 visible rows, standard rendering is fine. Document the decision.

5. **LLM Response Time is External**: You can't optimize the LLM API's response time — only optimize what you send (smaller images, better prompts) and how you handle the response. Document the average times for the graduation report.

6. **Coordinate Load Test with Ömer**: The concurrent request test (section 9.3) should be done together. Ömer measures backend impact, you measure Python service impact.

7. **PWA Cache Verification**: Just verify it works — don't change the caching strategies from Phase 33 unless there's a clear problem.
