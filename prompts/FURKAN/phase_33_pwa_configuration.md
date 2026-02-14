# PHASE 33: PWA (PROGRESSIVE WEB APP) CONFIGURATION

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 — LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-32 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth (JWT + Redis), RBAC, Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10: Next.js 14+ App Router, sidebar navigation (Dashboard, Faturalar, Yükleme, Kategoriler), Shadcn/ui, dark/light mode toggle, responsive layout with collapsible sidebar on mobile
- ✅ Phase 11: Login/Register pages, Zustand auth store, Axios interceptor with token refresh, protected routes
- ✅ Phase 12: Invoice list table (TanStack Query, pagination, sorting, status badges), detail page, manual add/edit forms, category management
- ✅ Phase 13-22: Full extraction pipeline, file upload, verification UI
- ✅ Phase 23: Advanced Filtering — filter panel, URL-based state, search
- ✅ Phase 24-25: Export (XLSX/CSV + accounting formats)
- ✅ Phase 26: Dashboard — summary cards, Recharts charts, pending actions
- ✅ Phase 27-28: Notification system — WebSocket bell icon, dropdown, email/push. **Phase 28 created a basic Service Worker for push notifications** (handles `push` and `notificationclick` events). Push subscriptions are stored in `push_subscriptions` table.
- ✅ Phase 29: Version History — timeline, diff viewer, revert
- ✅ Phase 30: Template Learning & Rule Engine (sidebar "Otomasyon" group with Şablonlar + Kurallar)
- ✅ Phase 31: KVKK Compliance (encryption, consent, data retention)
- ✅ Phase 32: Rate Limiting & Security Hardening (Redis rate limiter, security headers, CORS, brute-force protection, CSP)

### Relevant Existing Infrastructure

**Current Service Worker (Phase 28):**
Phase 28 created a minimal Service Worker for push notification handling. It:
- Listens for `push` events and displays notifications
- Handles `notificationclick` to navigate to relevant pages
- Is registered in the frontend code

This phase needs to EXTEND this Service Worker (not replace it) with PWA caching capabilities.

**Existing Responsive Design (Phase 10):**
The app already has a responsive layout with a collapsible sidebar on mobile. This phase enhances mobile experience further with PWA features.

**Security Headers (Phase 32):**
Phase 32 added Content-Security-Policy, X-Frame-Options, and other security headers. The CSP may affect Service Worker behavior — be aware and adjust if needed. Specifically, the `worker-src` directive in CSP must allow the Service Worker.

**Dark/Light Mode (Phase 10):**
The app has a theme toggle. The PWA theme color should adapt to the user's preference or use a neutral color.

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Transform the existing Next.js application into a full Progressive Web App (PWA) that:

1. Can be **installed** on mobile devices and desktops (Add to Home Screen)
2. Works **offline** with previously loaded data (offline-first cache strategy)
3. Provides a **native app experience** in standalone mode (no browser chrome)
4. Has **intelligent caching** — static assets cached first, API responses use stale-while-revalidate
5. **Auto-updates** — detects new versions and prompts users to reload
6. Is **mobile-optimized** — touch-friendly UI, safe area padding, proper viewport

---

## DETAILED REQUIREMENTS

### 1. next-pwa Package Integration

**Install and configure `next-pwa` (or `@ducanh2912/next-pwa` for Next.js 14+ compatibility).**

**next.config.js / next.config.mjs Update:**

Configure the PWA plugin:
- Enable PWA in production builds (disable in development to avoid caching issues during development)
- Set the Service Worker destination
- Configure the `sw.js` output filename
- Set the scope to "/"
- Configure runtime caching strategies (details in section 3)
- Disable the default Service Worker and use a custom one that merges with the existing push notification Service Worker from Phase 28

**Environment-based toggle:**
- `NEXT_PUBLIC_PWA_ENABLED=true` in production
- `NEXT_PUBLIC_PWA_ENABLED=false` in development (or auto-disable in dev mode)

---

### 2. Web App Manifest (manifest.json)

Create `/public/manifest.json` with the following configuration:

**Required Fields:**
- `name`: "Fatura OCR Sistemi" (full name, shown in app install dialog)
- `short_name`: "FaturaOCR" (shown on home screen under icon)
- `description`: "Fatura görüntülerinden otomatik veri çıkarım ve yönetim sistemi"
- `start_url`: "/" (opens the dashboard)
- `display`: "standalone" (no browser chrome — full app experience)
- `orientation`: "any" (support both portrait and landscape)
- `theme_color`: "#1e40af" (blue-700 — matches the app's primary color)
- `background_color`: "#ffffff" (white — shown during app launch splash screen)
- `scope`: "/"
- `lang`: "tr"
- `dir`: "ltr"
- `categories`: ["business", "finance", "productivity"]

**Icons:**
Generate a set of PWA icons from the app logo. Required sizes:

| Size | File | Purpose |
|---|---|---|
| 72x72 | icon-72x72.png | Android legacy |
| 96x96 | icon-96x96.png | Android legacy |
| 128x128 | icon-128x128.png | Chrome Web Store |
| 144x144 | icon-144x144.png | Windows tile |
| 152x152 | icon-152x152.png | iOS |
| 192x192 | icon-192x192.png | Android (required for PWA) |
| 384x384 | icon-384x384.png | Android splash |
| 512x512 | icon-512x512.png | Android splash (required for PWA) |

Also include:
- `maskable` purpose icons for at least 192x192 and 512x512 (with safe zone padding)
- A favicon.ico (32x32 and 16x16)
- An Apple touch icon (180x180)

If generating real icons is not possible, create simple placeholder icons with the app's initials "FO" on a blue background. The icons should look professional but can be simple for MVP.

**Manifest icons array format:**
Each icon entry: `{ "src": "/icons/icon-192x192.png", "sizes": "192x192", "type": "image/png", "purpose": "any" }`

**Shortcuts (optional but nice):**
Add quick action shortcuts:

- "Fatura Yükle": url "/upload", icon for upload
- "Faturalarım": url "/invoices", icon for list
- "Dashboard": url "/", icon for chart

**Link manifest in HTML head:**
Add `<link rel="manifest" href="/manifest.json">` to the Next.js document head (via `app/layout.tsx` metadata).

Also add Apple-specific meta tags in the head:
- `<meta name="apple-mobile-web-app-capable" content="yes">`
- `<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">`
- `<meta name="apple-mobile-web-app-title" content="FaturaOCR">`
- `<link rel="apple-touch-icon" href="/icons/apple-touch-icon.png">`

---

### 3. Service Worker & Caching Strategies

**IMPORTANT**: Phase 28 already registered a Service Worker for push notifications. This phase EXTENDS that Service Worker — do not create a conflicting one.

**Approach:**
- Use `next-pwa`'s Workbox-based Service Worker generation
- Inject the existing push notification handlers into the generated Service Worker
- Or use a custom Service Worker that imports Workbox for caching and includes push handlers

**Caching Strategies:**

**3.1 Static Assets — Cache First (cacheName: "static-assets")**

| Pattern | Strategy | Max Entries | Max Age |
|---|---|---|---|
| /_next/static/** | CacheFirst | 200 | 365 days |
| /icons/** | CacheFirst | 30 | 365 days |
| /images/** | CacheFirst | 50 | 30 days |
| *.js, *.css (hashed) | CacheFirst | 100 | 365 days |
| Google Fonts (if used) | CacheFirst | 10 | 365 days |

Hashed static assets (Next.js generates unique filenames) are safe to cache indefinitely — they change filename on content change.

**3.2 HTML Pages — Network First (cacheName: "pages")**

| Pattern | Strategy | Max Entries | Max Age | Network Timeout |
|---|---|---|---|---|
| / (dashboard) | NetworkFirst | 10 | 24 hours | 3 seconds |
| /invoices | NetworkFirst | 10 | 24 hours | 3 seconds |
| /invoices/* | NetworkFirst | 20 | 12 hours | 3 seconds |
| /upload | NetworkFirst | 5 | 24 hours | 3 seconds |
| /templates | NetworkFirst | 5 | 24 hours | 3 seconds |
| /rules | NetworkFirst | 5 | 24 hours | 3 seconds |
| /login | NetworkFirst | 1 | 7 days | 3 seconds |

NetworkFirst means: try the network first; if it fails or times out, serve from cache. This ensures users see fresh content when online, but can still access the app when offline.

**3.3 API Responses — Stale While Revalidate (cacheName: "api-responses")**

| Pattern | Strategy | Max Entries | Max Age |
|---|---|---|---|
| /api/v1/invoices (GET list) | StaleWhileRevalidate | 20 | 5 minutes |
| /api/v1/invoices/* (GET detail) | StaleWhileRevalidate | 50 | 10 minutes |
| /api/v1/categories | StaleWhileRevalidate | 5 | 1 hour |
| /api/v1/dashboard/** | StaleWhileRevalidate | 10 | 5 minutes |
| /api/v1/templates (GET) | StaleWhileRevalidate | 10 | 15 minutes |
| /api/v1/rules (GET) | StaleWhileRevalidate | 10 | 15 minutes |
| /api/v1/notifications (GET) | StaleWhileRevalidate | 5 | 1 minute |

StaleWhileRevalidate means: serve from cache immediately (fast), then update the cache from the network in the background. Next time, the user sees fresh data.

**3.4 DO NOT Cache These:**

| Pattern | Reason |
|---|---|
| /api/v1/auth/** | Authentication must always hit the server |
| POST/PUT/DELETE requests | Write operations must always reach the server |
| /api/v1/invoices/upload | File uploads cannot be cached |
| /api/v1/invoices/export | Downloads are dynamic |
| /ws/** | WebSocket connections |

**3.5 Offline Fallback Page:**

Create a `/offline.html` page (or `/offline` route) that is shown when:
- The user is offline AND
- The requested page is not in the cache

Content of offline page:
- App logo
- "Çevrimdışı Modu" (Offline Mode) heading
- "Şu anda internet bağlantınız yok. Daha önce görüntülediğiniz sayfalar çevrimdışı olarak kullanılabilir." message
- "Tekrar Dene" (Retry) button that reloads the page
- List of cached pages the user can navigate to

---

### 4. Install Prompt (Add to Home Screen)

**4.1 Custom Install Banner:**

Browsers have their own "Add to Home Screen" prompt, but it's not always visible. Create a custom install prompt:

**Logic:**
- Listen for the `beforeinstallprompt` event
- Store the event reference
- Show a custom banner at the bottom of the screen (after the user has visited at least 2 pages — don't show immediately)
- Banner content: "FaturaOCR'u ana ekranınıza ekleyin ve daha hızlı erişim sağlayın" with "Yükle" (Install) and "Daha Sonra" (Later) buttons
- "Yükle" triggers the stored `beforeinstallprompt` event
- "Daha Sonra" dismisses the banner and saves a flag in localStorage to not show for 7 days
- After successful installation: show a toast "Uygulama başarıyla yüklendi!"

**4.2 Install Prompt Component:**

Create a reusable `InstallPrompt` component:
- Renders as a bottom sheet/banner (Shadcn Sheet or custom)
- Includes app icon, name, and description
- "Yükle" and "Daha Sonra" buttons
- Only shown on mobile devices (use user agent or screen size detection)
- Supports dark mode

**4.3 Detect Standalone Mode:**

After installation, detect if the app is running in standalone mode:
- Check `window.matchMedia('(display-mode: standalone)').matches`
- Or check `navigator.standalone` (iOS Safari)
- When in standalone mode: hide certain browser-related UI elements (e.g., "Open in browser" links)
- Can be used to adjust layout (no address bar, more vertical space)

---

### 5. Auto-Update Mechanism

When a new version of the app is deployed, the Service Worker will detect it. Implement a user-friendly update flow:

**5.1 Update Detection:**
- Listen for the `controllerchange` event on the Service Worker registration
- Or listen for `updatefound` and then check if the new Service Worker is `waiting`

**5.2 Update Prompt:**
- Show a non-intrusive banner at the top of the page: "Uygulamanın yeni bir sürümü mevcut." with "Güncelle" (Update) button
- "Güncelle" sends a `SKIP_WAITING` message to the waiting Service Worker and then reloads the page
- Banner should be dismissable (but re-appears on next page navigation)

**5.3 Update Component:**

Create an `UpdatePrompt` component:
- Fixed position at the top of the screen (below the header)
- Blue/info color scheme
- "Yeni sürüm mevcut" text with "Güncelle" button
- Appears only when a new Service Worker is waiting
- Dark mode compatible

---

### 6. Offline Status Indicator

Show the user when they are offline:

**6.1 Online/Offline Detection:**
- Use `navigator.onLine` and listen for `online`/`offline` events
- Create a Zustand store or React context for network status

**6.2 Offline Banner:**
When the user goes offline:
- Show a persistent banner at the top: "Çevrimdışı moddasınız. Bazı özellikler kısıtlı olabilir." (yellow/warning color)
- When back online: show a brief "Bağlantı yeniden sağlandı" (green/success) toast that auto-dismisses after 3 seconds

**6.3 Disable Write Operations Offline:**
When offline, disable buttons that require network (write operations):
- "Fatura Yükle" button → disabled with tooltip "Çevrimdışıyken dosya yükleyemezsiniz"
- "Kaydet" buttons on forms → disabled
- "Dışa Aktar" button → disabled
- Read-only browsing of cached data should still work

---

### 7. Mobile Responsive Optimization

Enhance the existing responsive design for better mobile PWA experience:

**7.1 Safe Area Padding:**
For devices with notches (iPhone X+), add safe area padding:
- Add `viewport-fit=cover` to the viewport meta tag
- Use `env(safe-area-inset-top)`, `env(safe-area-inset-bottom)` CSS variables for padding
- Apply to the header, bottom navigation (if any), and modal/drawer components

**7.2 Touch Optimization:**
- Ensure all interactive elements have a minimum touch target of 44x44px (Apple HIG) / 48x48dp (Material)
- Add `touch-action: manipulation` to prevent double-tap zoom on interactive elements
- Remove 300ms tap delay (should be handled by modern browsers, but verify)

**7.3 Pull-to-Refresh:**
- Consider adding a pull-to-refresh gesture for the invoice list and dashboard pages
- Can be implemented with a simple custom hook that detects overscroll + touch events
- On pull: trigger TanStack Query invalidation for the current page's data
- Optional — skip if too complex for the timeline

**7.4 Viewport Meta Tag:**
Ensure the viewport is correctly configured:
`<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover">`

Note: `maximum-scale=1, user-scalable=no` prevents zoom which is standard for app-like PWAs but may be considered an accessibility concern. Consider allowing zoom (`user-scalable=yes`) as the default and only restricting on specific views (e.g., the verification split-view).

---

### 8. Splash Screen

**iOS Splash Screens:**
iOS requires specific `apple-touch-startup-image` meta tags for splash screens. Generate for common device sizes:
- iPhone SE, iPhone 8 (750x1334)
- iPhone X/XS/11 Pro (1125x2436)
- iPhone XR/11 (828x1792)
- iPhone 12/13/14 (1170x2532)
- iPad (various sizes)

Each splash screen: app icon centered on a blue (#1e40af) background with the app name below.

If generating all splash images is too time-consuming, at minimum create:
- A simple 1x and 2x splash for the most common iPhone and Android sizes
- Or use a CSS-based approach with the background_color from manifest.json (this is the browser default)

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_33.0_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Files created or modified (with paths)
3. next-pwa configuration details
4. manifest.json contents
5. Service Worker caching strategy documentation
6. How Phase 28 push notification SW was merged/extended
7. Install prompt behavior description
8. Auto-update mechanism description
9. Offline behavior documentation (what works, what doesn't)
10. Mobile optimization details (safe area, touch targets, viewport)
11. Icons generated (list with sizes)
12. Lighthouse PWA audit score (run Lighthouse and note the score)
13. Test results (install flow tested on Android/iOS if available, or Chrome DevTools)
14. CSP compatibility notes (any Phase 32 security header adjustments needed)
15. Issues encountered and solutions
16. Next steps (Phase 34 i18n, any PWA improvements identified)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 10**: App layout, responsive design, dark/light mode
- **Phase 11**: Auth pages (login page needs to be accessible offline for redirect)
- **Phase 28**: Push notification Service Worker (must be merged, not replaced)
- **Phase 32**: Security headers (CSP must allow Service Worker registration via `worker-src 'self'`)

### Required By
- **Phase 34**: i18n — localized manifest and PWA strings
- **Phase 37**: E2E Tests — PWA install flow and offline behavior should be testable
- **Phase 39**: Production Deployment — PWA requires HTTPS in production

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] `next-pwa` package installed and configured in next.config.js
- [ ] Service Worker generated on `npm run build` (check .next/sw.js or public/sw.js)
- [ ] manifest.json present at /manifest.json with correct fields
- [ ] manifest.json: name, short_name, description in Turkish
- [ ] manifest.json: icons at all required sizes (192x192, 512x512 minimum)
- [ ] manifest.json: display set to "standalone"
- [ ] manifest.json: theme_color and background_color set
- [ ] App installable on Android Chrome ("Add to Home Screen" prompt appears)
- [ ] App installable on iOS Safari (Add to Home Screen works)
- [ ] Installed app opens in standalone mode (no browser address bar)
- [ ] Static assets (JS, CSS, images) cached and load on repeat visits
- [ ] API responses cached with stale-while-revalidate strategy
- [ ] App works offline: cached pages load, uncached pages show offline fallback
- [ ] Offline fallback page renders correctly with Turkish text
- [ ] Push notifications (Phase 28) still work after PWA Service Worker merge
- [ ] Custom install banner appears on mobile after 2 page visits
- [ ] Auto-update prompt appears when new Service Worker version is detected
- [ ] Offline status banner shows when connectivity is lost
- [ ] Offline status banner hides when connectivity is restored
- [ ] Write operation buttons (save, upload, verify) disabled when offline
- [ ] Safe area padding works on notched devices (iPhone X+)
- [ ] Touch targets meet minimum 44x44px requirement
- [ ] Lighthouse PWA audit: all required criteria pass
- [ ] Dark mode works correctly in standalone/installed mode
- [ ] PWA disabled in development mode (no caching confusion)
- [ ] Cache cleanup on logout (sensitive API data cleared)
- [ ] All text is in Turkish
- [ ] `npm run build` completes without errors
- [ ] Result file created at docs/FURKAN/step_results/faz_33.0_result.md
---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ `next-pwa` is configured and generates a Service Worker on production build
2. ✅ manifest.json is present with correct name, icons, display mode, and theme color
3. ✅ App can be installed on Android (Chrome "Add to Home Screen")
4. ✅ App can be added to iOS Home Screen (Safari)
5. ✅ Installed app runs in standalone mode (no browser chrome)
6. ✅ Static assets are cached and load instantly on repeat visits
7. ✅ API responses are cached with stale-while-revalidate strategy
8. ✅ App works offline: cached pages are accessible, offline fallback shown for uncached pages
9. ✅ Push notifications (Phase 28) continue to work after PWA Service Worker merge
10. ✅ Custom install banner appears on mobile after 2 page visits
11. ✅ Auto-update prompt appears when a new Service Worker version is detected
12. ✅ Offline status banner shows/hides when connectivity changes
13. ✅ Write operation buttons are disabled when offline
14. ✅ Safe area padding works on notched devices
15. ✅ Touch targets meet minimum 44x44px requirement
16. ✅ Lighthouse PWA score is 90+ (or all required PWA criteria pass)
17. ✅ Dark mode works correctly in standalone/installed mode
18. ✅ All PWA text is in Turkish
19. ✅ Result file is created at docs/FURKAN/step_results/faz_33.0_result.md

---

## IMPORTANT NOTES

1. **Do NOT Break Push Notifications**: Phase 28's push notification Service Worker MUST continue to work. If `next-pwa` generates a new Service Worker, merge the push event handlers into it. Test push notifications after this phase is complete.

2. **Disable PWA in Development**: PWA caching in development causes confusion (stale code, need to clear cache constantly). Use environment flag to disable PWA in dev, or configure next-pwa to skip SW registration in development mode.

3. **HTTPS Required for Full PWA**: Service Workers (beyond push) require HTTPS in production. In development, `localhost` is exempt. Document this for Phase 39 (deployment).

4. **CSP Compatibility**: Phase 32's Content-Security-Policy header may block Service Worker registration if `worker-src` is not set to `'self'`. If CSP issues arise, document them in the result file so Ömer can adjust the CSP in the backend.

5. **Cache Invalidation on Logout**: When a user logs out, sensitive cached API responses should be cleared. Add a cache cleanup step to the logout flow: call `caches.delete('api-responses')` to clear cached API data.

6. **TanStack Query + Service Worker Cache**: The app uses TanStack Query for API data management. With Service Worker caching, there are now two cache layers. This is fine — TanStack Query manages in-memory cache for the current session, while Service Worker provides persistence across sessions and offline access. They complement each other.

7. **Icon Generation**: If you cannot generate proper app icons (requires a designer or design tool), create simple SVG-based icons with the app initials "FO" on a solid blue background. Convert to PNG at required sizes. Tools: Sharp (Node.js), or online PWA icon generators.

8. **Testing PWA Install**: To test the install prompt in Chrome DevTools: go to Application tab → Manifest → check installability. To trigger the install prompt manually, use the browser's address bar install icon. On Android, use Chrome's "Add to home screen" from the menu.
