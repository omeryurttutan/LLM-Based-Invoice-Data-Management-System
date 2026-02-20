# PHASE 27-F: FRONTEND — NOTIFICATION SYSTEM UI

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-26 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, DB, Auth, RBAC, Invoice CRUD, Audit Log, Dedup
- ✅ Phase 10: Next.js 14+ App Router, sidebar (Dashboard, Faturalar, Yükleme, Kategoriler), Shadcn/ui, dark/light mode, responsive
- ✅ Phase 11: Login/Register, Zustand auth store, Axios interceptor, token refresh, protected routes
- ✅ Phase 12: Invoice list (TanStack Query, pagination, sorting, badges), detail, manual CRUD, categories
- ✅ Phase 13-22: Full extraction pipeline, upload UI, verification UI
- ✅ Phase 23: Filtering & search (backend Specification + frontend filter panel, URL state)
- ✅ Phase 24-25: Export (XLSX/CSV + Logo/Mikro/Netsis/Luca)
- ✅ Phase 26: Dashboard — backend 6 endpoints + frontend Recharts visualizations, summary cards, pending actions

### What Phase 27 Backend Delivers (Ömer)

**WebSocket Endpoint:**
- STOMP over SockJS at `/ws`
- Authenticated via JWT (token in CONNECT frame header or query param)
- User subscribes to: `/user/queue/notifications`
- Messages pushed in real time when events occur

**WebSocket Message Format (pushed to client):**

| Field | Type | Description |
|---|---|---|
| id | long | Notification DB ID |
| type | string | EXTRACTION_COMPLETED, EXTRACTION_FAILED, BATCH_COMPLETED, BATCH_PARTIALLY_COMPLETED, LOW_CONFIDENCE, HIGH_CONFIDENCE_AUTO_VERIFIED, INVOICE_VERIFIED, INVOICE_REJECTED, PROVIDER_DEGRADED, ALL_PROVIDERS_DOWN, SYSTEM_ANNOUNCEMENT |
| title | string | Short Turkish title |
| message | string | Descriptive Turkish message |
| severity | string | INFO / WARNING / ERROR / SUCCESS |
| referenceType | string | INVOICE / BATCH / SYSTEM |
| referenceId | long | Related entity ID |
| metadata | object | Extra data (confidence_score, provider, file_name, etc.) |
| createdAt | string (ISO 8601) | Timestamp |

**REST API Endpoints:**

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/v1/notifications?page=0&size=20&isRead=false&type=...&sort=createdAt,desc | Paginated notification list |
| GET | /api/v1/notifications/unread-count | Returns `{ "count": N }` |
| PATCH | /api/v1/notifications/{id}/read | Mark as read |
| PATCH | /api/v1/notifications/read-all | Mark all as read |
| DELETE | /api/v1/notifications/{id} | Delete notification |

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 1.5 days

### Frontend Tech Stack
- Next.js 14+ (App Router), React 19, TypeScript 5.x
- Tailwind CSS 3.x, Shadcn/ui
- TanStack Query 5.x, Zustand 4.x, Axios

---

## OBJECTIVE

Build the frontend notification UI: a WebSocket connection for real-time notifications, a bell icon with unread badge in the header, a dropdown panel showing recent notifications, a full notifications page, and navigation from notifications to the relevant entity (invoice, batch). Also upgrade Phase 21's batch polling to use WebSocket for real-time status updates.

---

## DETAILED REQUIREMENTS

### 1. WebSocket Connection Manager

Create a WebSocket connection service/hook that:
- Connects to the backend STOMP endpoint at `ws://localhost:8082/ws` (or the configured backend URL)
- Uses SockJS client library for WebSocket with fallback
- Uses @stomp/stompjs (or stompjs) for STOMP protocol
- Sends the JWT access token during CONNECT (as a header: `Authorization: Bearer {token}`)
- Subscribes to `/user/queue/notifications` after successful connection
- On receiving a message: parse JSON, update notification state, show toast notification
- Handles disconnection with automatic reconnect (exponential backoff: 5s, 10s, 20s, max 60s)
- Disconnects cleanly on logout or page unload

**Connection Lifecycle:**
- On login → establish WebSocket connection
- While connected → receive real-time notifications
- On token refresh → reconnect with new token (if needed)
- On logout → disconnect and clean up
- On network loss → auto-reconnect with backoff

**NPM Dependencies to Add:**
- `@stomp/stompjs` — STOMP client
- `sockjs-client` — SockJS fallback transport

### 2. Notification State Management (Zustand Store)

Create a Zustand store for notification state:

**State:**
- notifications: array of recent notifications (last 50 in memory)
- unreadCount: number
- isConnected: boolean (WebSocket connection status)
- isDropdownOpen: boolean

**Actions:**
- addNotification(notification) — prepend to list, increment unreadCount, trigger toast
- setNotifications(list) — replace list (from REST fetch)
- setUnreadCount(count) — update count
- markAsRead(id) — update single notification in local state
- markAllAsRead() — update all in local state
- removeNotification(id) — remove from local state
- setConnected(boolean) — update connection status
- toggleDropdown() — toggle dropdown visibility

### 3. Bell Icon with Unread Badge

Place in the header (top-right area, next to user menu from Phase 10):

**Requirements:**
- Bell icon (use Lucide React: Bell or BellRing)
- Unread count badge: red circle with number, positioned top-right of icon
- Badge shows actual count up to 9, then "9+" for 10 or more
- Badge hidden when count is 0
- Click toggles the notification dropdown
- Subtle animation (shake or pulse) when a new notification arrives in real-time

**Badge fetching:**
- On app load: fetch GET /api/v1/notifications/unread-count → set initial count
- On WebSocket message: increment count locally (optimistic)
- When dropdown opens: refetch to ensure accuracy

### 4. Notification Dropdown Panel

A dropdown panel that appears below the bell icon when clicked:

**Layout:**
- Fixed width (e.g., 400px), max height with scroll
- Header: "Bildirimler" title + "Tümünü okundu işaretle" link
- Notification list: most recent first
- Footer: "Tüm bildirimleri göster" link → navigates to /notifications page
- Empty state: "Bildirim bulunmuyor" with a muted icon

**Each notification item in dropdown:**

| Element | Description |
|---|---|
| Severity icon | Color-coded icon: green check (SUCCESS), yellow triangle (WARNING), red circle (ERROR), blue info (INFO) |
| Title | Bold, single line, truncated if long |
| Message | 2-line truncated preview |
| Timestamp | Relative time ("2 dk önce", "1 saat önce", "Dün") |
| Unread indicator | Blue dot on the left side if unread |
| Click action | Mark as read + navigate to related entity |

**Click Navigation:**
- referenceType=INVOICE → navigate to `/invoices/{referenceId}`
- referenceType=BATCH → navigate to `/invoices?batchId={referenceId}` (or batch detail if exists)
- referenceType=SYSTEM → no navigation, just mark as read

**Data fetching for dropdown:**
- On first open: fetch GET /api/v1/notifications?page=0&size=10&sort=createdAt,desc
- Cache with TanStack Query (staleTime: 30 seconds)
- Real-time additions from WebSocket prepended to the list

### 5. Full Notifications Page

**Route:** `/notifications`

**Add to sidebar:** "Bildirimler" menu item with bell icon and unread badge

**Page features:**
- Full-width notification list (paginated)
- Filter tabs or buttons: "Tümü" / "Okunmamış" / "Okunmuş"
- Filter by type (dropdown): All types, or specific type
- Filter by severity (dropdown): All, INFO, WARNING, ERROR, SUCCESS
- Each notification shows full message (not truncated)
- "Okundu işaretle" button per item (if unread)
- "Tümünü okundu işaretle" button at top
- "Sil" (delete) button per item
- Infinite scroll or pagination (TanStack Query with infinite query or standard pagination)
- Empty state with illustration

**Each notification on the full page:**

| Element | Description |
|---|---|
| Severity icon | Same as dropdown but larger |
| Type badge | Small colored tag showing type (e.g., "Veri Çıkarımı", "Toplu Yükleme", "Sistem") |
| Title | Full text |
| Message | Full text (not truncated) |
| Timestamp | Relative + absolute on hover tooltip |
| Unread indicator | Blue left border or background tint |
| Actions | "Görüntüle" (navigate) / "Okundu" (mark read) / "Sil" (delete) |

### 6. Toast Notifications (Real-Time)

When a notification arrives via WebSocket, show a toast (Shadcn/ui Toast or Sonner):

**Toast behavior:**
- Appears in bottom-right corner
- Shows notification title and truncated message
- Severity-based styling: green border (SUCCESS), yellow (WARNING), red (ERROR), blue (INFO)
- Auto-dismiss after 5 seconds
- Clickable: clicking navigates to the related entity
- Dismiss button (X)
- Stack up to 3 toasts, older ones dismissed

### 7. Phase 21 Upgrade: WebSocket Batch Status

Replace or supplement the 5-second polling in Phase 21's batch upload tracking with WebSocket:

**Approach:**
- When a batch upload is in progress, the user is already connected via WebSocket
- Backend sends BATCH_COMPLETED or BATCH_PARTIALLY_COMPLETED notifications
- Also, individual EXTRACTION_COMPLETED / EXTRACTION_FAILED notifications for each file
- Use these WebSocket messages to update the batch tracking UI in real time
- Keep polling as fallback (if WebSocket disconnects)

**Implementation:**
- Create a custom hook: `useBatchStatus(batchId)` that:
  - Subscribes to WebSocket notifications filtered by referenceType=BATCH and referenceId=batchId
  - Falls back to polling GET /api/v1/invoices/batch/{batchId} if WebSocket is not connected
  - Returns: { batchProgress, isComplete, isConnected }

### 8. Notification Type Mapping (UI)

Map notification types to UI elements:

| Type | Icon | Color | Type Badge Text |
|---|---|---|---|
| EXTRACTION_COMPLETED | CheckCircle | green | "Veri Çıkarımı" |
| EXTRACTION_FAILED | XCircle | red | "Veri Çıkarımı" |
| BATCH_COMPLETED | CheckCircle2 | green | "Toplu Yükleme" |
| BATCH_PARTIALLY_COMPLETED | AlertTriangle | yellow | "Toplu Yükleme" |
| LOW_CONFIDENCE | AlertTriangle | yellow | "Güven Skoru" |
| HIGH_CONFIDENCE_AUTO_VERIFIED | ShieldCheck | green | "Otomatik Doğrulama" |
| INVOICE_VERIFIED | CheckCircle | green | "Doğrulama" |
| INVOICE_REJECTED | XCircle | red | "Doğrulama" |
| PROVIDER_DEGRADED | AlertTriangle | yellow | "Sistem" |
| ALL_PROVIDERS_DOWN | AlertOctagon | red | "Sistem" |
| SYSTEM_ANNOUNCEMENT | Info | blue | "Sistem" |

### 9. Relative Time Formatting (Turkish)

Format timestamps as Turkish relative time:
- < 1 minute: "Az önce"
- 1-59 minutes: "{n} dk önce"
- 1-23 hours: "{n} saat önce"
- Yesterday: "Dün"
- 2-6 days: "{n} gün önce"
- 7+ days: "DD.MM.YYYY" (Turkish date format)

### 10. Accessibility

- Bell icon: aria-label="Bildirimler" with unread count announced
- Dropdown: role="menu", keyboard navigable (arrow keys), Escape to close
- Toast notifications: role="alert" for screen readers
- Focus management: dropdown traps focus, returns to bell on close

---

## TESTING REQUIREMENTS

### 1. Component Tests
- Bell icon renders with correct unread count
- Badge hidden when count is 0
- Badge shows "9+" when count > 9
- Dropdown opens/closes on bell click
- Notification items render with correct severity icons
- Click on notification navigates to correct route
- Toast appears with correct content and styling
- Empty state renders when no notifications

### 2. WebSocket Tests (mocked)
- Connection established on login
- Notification received → added to store, toast shown, badge updated
- Disconnection → reconnect attempted
- Logout → connection closed

### 3. Integration Tests (with mocked API)
- Fetch unread count on mount
- Fetch notification list on dropdown open
- Mark as read → API called, local state updated, badge decremented
- Mark all as read → API called, all local notifications updated
- Delete notification → API called, removed from list
- Full notifications page: pagination, filtering by read/unread

---

## VERIFICATION CHECKLIST

### WebSocket
- [ ] SockJS + STOMP connection established with JWT
- [ ] Subscribes to /user/queue/notifications
- [ ] Receives real-time notifications
- [ ] Auto-reconnect on disconnect
- [ ] Clean disconnect on logout

### Bell Icon & Badge
- [ ] Bell icon in header, right side
- [ ] Unread count badge (red, correct count)
- [ ] Badge hidden at 0, shows "9+" at 10+
- [ ] Animation on new notification

### Dropdown
- [ ] Opens below bell icon
- [ ] Shows recent notifications (last 10)
- [ ] Severity icons and colors correct
- [ ] Relative timestamps in Turkish
- [ ] Unread indicator (blue dot)
- [ ] Click navigates to entity and marks as read
- [ ] "Tümünü okundu işaretle" works
- [ ] "Tüm bildirimleri göster" navigates to /notifications
- [ ] Keyboard accessible

### Toast
- [ ] Real-time notifications show toast
- [ ] Severity-based styling
- [ ] Auto-dismiss after 5s
- [ ] Clickable (navigates)
- [ ] Max 3 stacked

### Notifications Page
- [ ] Route /notifications works
- [ ] Sidebar menu item with badge
- [ ] Filter: Tümü / Okunmamış / Okunmuş
- [ ] Filter by type and severity
- [ ] Full message display
- [ ] Mark as read, mark all, delete actions
- [ ] Pagination or infinite scroll
- [ ] Empty state

### Phase 21 Upgrade
- [ ] Batch status updates via WebSocket
- [ ] Fallback to polling if WebSocket disconnected
- [ ] Individual file completion shown in real time

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/OMER/step_results/faz_27_f_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified
4. Component list (WebSocket manager, bell icon, dropdown, notifications page, toast)
5. Zustand store structure
6. WebSocket connection flow (connect, subscribe, receive, reconnect, disconnect)
7. Screenshots: bell with badge, dropdown open, notification page, toast notification
8. Phase 21 upgrade details (polling → WebSocket)
9. Test results
10. Issues and solutions
11. Next steps (Phase 28 may add email preference settings to the notification UI)

---

## DEPENDENCIES

### Requires
- **Phase 10**: Header layout (bell icon placement)
- **Phase 11**: Auth store (JWT token for WebSocket, login/logout lifecycle)
- **Phase 21**: Upload UI (batch tracking upgrade to WebSocket)
- **Phase 27 Backend (ÖMER)**: WebSocket endpoint, REST API, notification types and message format

### Required By
- **Phase 28**: Email & Push preferences UI (may add notification settings to user profile)

---

## SUCCESS CRITERIA

1. ✅ WebSocket connection established with JWT authentication
2. ✅ Real-time notifications received and displayed as toasts
3. ✅ Bell icon with accurate unread count badge in header
4. ✅ Notification dropdown with recent notifications, severity icons, relative timestamps
5. ✅ Click on notification navigates to the related invoice/batch
6. ✅ Full notifications page with filtering, pagination, mark-read, delete
7. ✅ Phase 21 batch tracking upgraded from polling to WebSocket
8. ✅ Auto-reconnect on WebSocket disconnect
9. ✅ All user-facing text in Turkish
10. ✅ Keyboard accessible and responsive
11. ✅ All tests pass
12. ✅ Result file created

---

## IMPORTANT NOTES

1. **SockJS + STOMP Libraries**: Use `@stomp/stompjs` and `sockjs-client`. These are the standard libraries for STOMP over SockJS in JavaScript. Install via npm.

2. **JWT in WebSocket**: The STOMP CONNECT frame supports custom headers. Send `Authorization: Bearer {token}` as a connect header. If that doesn't work with the backend config, fall back to sending the token as a query parameter: `/ws?token={jwt}`.

3. **Token Refresh**: If the JWT expires while the WebSocket is connected, the connection may drop. Handle this by detecting disconnection, refreshing the token (via the existing refresh flow from Phase 11), and reconnecting with the new token.

4. **Don't Duplicate State**: Use TanStack Query for the REST API data (notification list, unread count) and Zustand only for real-time state (WebSocket connection status, incoming notifications buffer). When the dropdown opens, fetch from REST and merge with any unsynced WebSocket notifications.

5. **Toast Library**: Use Shadcn/ui's built-in Toast component or the Sonner library (already compatible with Shadcn/ui). Don't build a custom toast system.

6. **Turkish Relative Time**: You can use a small utility function or the `date-fns` library with Turkish locale (`import { formatDistanceToNow } from 'date-fns'` with `{ locale: tr }`). Install `date-fns` if not already present.

7. **Dropdown Click Outside**: Close the dropdown when clicking outside. Use a Popover component from Shadcn/ui or a custom click-outside hook.

8. **No Backend Changes**: This phase is frontend-only. All backend endpoints and WebSocket configuration are ready from Phase 27 backend. If something is missing, coordinate with Ömer.
