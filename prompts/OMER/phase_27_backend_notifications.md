# PHASE 27: NOTIFICATION SYSTEM — IN-APP REAL-TIME (BACKEND)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001
  - **Next.js Frontend**: Port 3001
  - **RabbitMQ**: Port 5673
  - **Redis**: Port 6380

### Current State (Phases 0-26 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth (JWT + Redis), RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN), Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10-12: Frontend layout (sidebar, dark/light mode), auth pages, invoice list/CRUD UI
- ✅ Phase 13-18: Python extraction pipeline — preprocessing, Gemini + GPT + Claude fallback, validation & confidence score, e-Invoice XML parser
- ✅ Phase 19: RabbitMQ async messaging — Producer (Spring Boot) + Consumer (Python), extraction queue, result queue, DLQ, retry logic
- ✅ Phase 20: File Upload Infrastructure — POST /upload (sync), POST /bulk-upload (async), ZIP support, batch tracking, file validation, shared volume storage
- ✅ Phase 21: Upload UI — drag-and-drop, progress tracking, batch status polling
- ✅ Phase 22: Verification UI — side-by-side document viewer, editable extraction fields, re-validation, approval/rejection
- ✅ Phase 23: Filtering & Search — backend Specification API + frontend filter panel with URL state
- ✅ Phase 24-25: Export — XLSX/CSV + accounting software formats (Logo, Mikro, Netsis, Luca)
- ✅ Phase 26: Dashboard — backend stats API (6 endpoints) + frontend charts (Recharts), summary cards, category/monthly/supplier visualizations

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 1.5 days (backend portion)

### Relationship to Phase 27-F
Phase 27 is split:
- **27 (this phase)**: Backend — WebSocket config, notifications table, notification service, REST endpoints, event triggers
- **27-F**: Frontend (FURKAN) — WebSocket client connection, bell icon, dropdown, notifications page

Both must agree on WebSocket topic paths, message format, and REST API contracts.

---

## OBJECTIVE

Build the backend notification infrastructure: a notifications database table, a notification service that creates and delivers notifications, WebSocket (STOMP over SockJS) for real-time push to connected clients, and REST endpoints for listing/managing notifications. Notifications are triggered by key system events like extraction completion, batch completion, low confidence scores, and provider failures.

---

## DETAILED REQUIREMENTS

### 1. WebSocket Configuration (STOMP + SockJS)

**Why STOMP over SockJS:**
- STOMP provides a simple messaging protocol on top of WebSocket
- SockJS provides fallback for browsers that don't support WebSocket
- Spring Boot has first-class support via spring-boot-starter-websocket

**Configuration Requirements:**
- Enable WebSocket message broker
- Register STOMP endpoint: `/ws` (SockJS enabled)
- Configure simple message broker with destination prefix: `/topic` (broadcast) and `/queue` (user-specific)
- Application destination prefix: `/app`
- User destination prefix: `/user` (for sending to specific users)
- Allowed origins: frontend URL (http://localhost:3001 in dev, configurable for prod)

**Authentication Integration:**
- WebSocket connections must be authenticated
- Intercept the CONNECT frame, extract JWT token from the `Authorization` header or from a query parameter (`?token=xxx`)
- Validate the JWT token using the existing auth service (Phase 4)
- Associate the WebSocket session with the authenticated user (set user principal)
- Reject unauthenticated connections

**STOMP Destinations (Topics):**

| Destination | Type | Description |
|---|---|---|
| /user/queue/notifications | User-specific | Private notifications for the connected user |
| /topic/system | Broadcast | System-wide announcements (e.g., maintenance) |

The frontend subscribes to `/user/queue/notifications` to receive personal notifications in real time.

### 2. Notifications Database Table

Create via Flyway migration: `V{next_number}__phase_27_notifications.sql`

**notifications table:**

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| user_id | BIGINT | NOT NULL, FK → users(id) | Recipient |
| company_id | BIGINT | NOT NULL, FK → companies(id) | Company context |
| type | VARCHAR(50) | NOT NULL | Notification type (enum) |
| title | VARCHAR(255) | NOT NULL | Short title |
| message | TEXT | NOT NULL | Detail message |
| severity | VARCHAR(20) | NOT NULL, DEFAULT 'INFO' | INFO / WARNING / ERROR / SUCCESS |
| is_read | BOOLEAN | NOT NULL, DEFAULT FALSE | Read status |
| read_at | TIMESTAMP | NULL | When marked as read |
| reference_type | VARCHAR(50) | NULL | Related entity type (INVOICE, BATCH, SYSTEM) |
| reference_id | BIGINT | NULL | Related entity ID (invoice_id, batch_id, etc.) |
| metadata | JSONB | NULL | Extra data (confidence_score, provider, error details) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:**
- idx_notifications_user_id ON (user_id)
- idx_notifications_user_read ON (user_id, is_read) — for unread count queries
- idx_notifications_user_created ON (user_id, created_at DESC) — for listing
- idx_notifications_company ON (company_id)

### 3. Notification Types

| Type Enum | Severity | Title (Turkish) | Trigger Event |
|---|---|---|---|
| EXTRACTION_COMPLETED | SUCCESS | "Veri çıkarımı tamamlandı" | Single file extraction finished successfully |
| EXTRACTION_FAILED | ERROR | "Veri çıkarımı başarısız" | All LLM providers failed for a file |
| BATCH_COMPLETED | SUCCESS | "Toplu yükleme tamamlandı" | All files in a batch finished processing |
| BATCH_PARTIALLY_COMPLETED | WARNING | "Toplu yükleme kısmen tamamlandı" | Batch done but some files failed |
| LOW_CONFIDENCE | WARNING | "Düşük güven skoru" | Extraction completed but confidence < review threshold (70) |
| HIGH_CONFIDENCE_AUTO_VERIFIED | INFO | "Fatura otomatik doğrulandı" | Extraction auto-verified (confidence ≥ 90) |
| INVOICE_VERIFIED | SUCCESS | "Fatura doğrulandı" | Manual verification by a user |
| INVOICE_REJECTED | WARNING | "Fatura reddedildi" | Manual rejection by a user |
| PROVIDER_DEGRADED | WARNING | "LLM sağlayıcı sorunlu" | A provider marked as DEGRADED |
| ALL_PROVIDERS_DOWN | ERROR | "Tüm LLM sağlayıcılar erişilemez" | All providers UNHEALTHY |
| SYSTEM_ANNOUNCEMENT | INFO | (dynamic) | Admin-triggered system message |

### 4. Notification Service

Central service responsible for creating, storing, and delivering notifications.

**Responsibilities:**
- Create notification record in database
- Determine recipient(s) — usually the user who uploaded the invoice, or all users in the company for system-level events
- Send via WebSocket to connected user(s)
- If user is not connected (no active WebSocket session), notification is stored in DB and will be fetched when user next opens the app

**Key Methods:**

| Method | Parameters | Description |
|---|---|---|
| notify | userId, type, title, message, severity, referenceType, referenceId, metadata | Create + store + push to user |
| notifyCompany | companyId, type, title, message, severity, ... | Notify all users in a company |
| notifyAdmins | type, title, message, severity, ... | Notify all ADMIN role users |
| markAsRead | notificationId, userId | Mark single notification as read |
| markAllAsRead | userId | Mark all user's notifications as read |
| getUnreadCount | userId | Count unread notifications for badge |

**WebSocket Delivery:**
- Use SimpMessagingTemplate to send to specific user: `convertAndSendToUser(username, "/queue/notifications", payload)`
- The payload sent via WebSocket matches the notification entity structure (id, type, title, message, severity, referenceType, referenceId, metadata, createdAt)

### 5. Notification Triggers (Integration Points)

Wire notification creation into existing services at these points:

**In the RabbitMQ Result Listener (Phase 19-B):**
When extraction result arrives from Python:
- If COMPLETED + confidence ≥ 90 (auto-verified) → notify: HIGH_CONFIDENCE_AUTO_VERIFIED
- If COMPLETED + confidence < 70 (low) → notify: LOW_CONFIDENCE
- If COMPLETED + normal range → notify: EXTRACTION_COMPLETED
- If FAILED → notify: EXTRACTION_FAILED

**In the Batch Job completion logic (Phase 20):**
When batch status changes to COMPLETED or PARTIALLY_COMPLETED:
- All succeeded → notify: BATCH_COMPLETED
- Some failed → notify: BATCH_PARTIALLY_COMPLETED

**In the Invoice verification flow (Phase 7 / Phase 22 backend):**
- When invoice status changes to VERIFIED manually → notify: INVOICE_VERIFIED
- When invoice status changes to REJECTED → notify: INVOICE_REJECTED

**In the single upload sync flow (Phase 20):**
After synchronous extraction completes:
- Same confidence-based notifications as async flow

**Provider health changes (Phase 16, if health tracking updates reach Spring Boot):**
- Optional: If Spring Boot monitors provider health (or Python reports via an endpoint), trigger PROVIDER_DEGRADED or ALL_PROVIDERS_DOWN
- This can also be triggered by the result listener when error_code indicates provider issues

### 6. REST API Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| GET | /api/v1/notifications | List notifications (paginated) | All authenticated |
| GET | /api/v1/notifications/unread-count | Get unread notification count | All authenticated |
| PATCH | /api/v1/notifications/{id}/read | Mark single notification as read | Owner only |
| PATCH | /api/v1/notifications/read-all | Mark all as read for current user | All authenticated |
| DELETE | /api/v1/notifications/{id} | Delete a notification | Owner only |

**GET /api/v1/notifications query parameters:**

| Parameter | Type | Default | Description |
|---|---|---|---|
| page | integer | 0 | Page number |
| size | integer | 20 | Page size (max 50) |
| isRead | boolean | null | Filter: null=all, true=read only, false=unread only |
| type | string | null | Filter by notification type |
| severity | string | null | Filter by severity |
| sort | string | createdAt,desc | Sort field and direction |

**GET /api/v1/notifications response format:**
Standard paginated response wrapping notification objects. Each notification includes: id, type, title, message, severity, isRead, readAt, referenceType, referenceId, metadata, createdAt.

**GET /api/v1/notifications/unread-count response:**
Simple object: `{ "count": 5 }`

### 7. WebSocket Message Format

The JSON payload pushed to clients via WebSocket:

| Field | Type | Description |
|---|---|---|
| id | long | Notification database ID |
| type | string | Notification type enum |
| title | string | Short title (Turkish) |
| message | string | Descriptive message |
| severity | string | INFO / WARNING / ERROR / SUCCESS |
| referenceType | string | INVOICE / BATCH / SYSTEM |
| referenceId | long | Related entity ID |
| metadata | object | Extra key-value data |
| createdAt | string (ISO 8601) | Timestamp |

### 8. Message Content Templates

Define clear, informative Turkish messages for each notification type:

**EXTRACTION_COMPLETED:**
- Title: "Veri çıkarımı tamamlandı"
- Message: "{fileName} dosyasından veri çıkarımı başarıyla tamamlandı. Güven skoru: {confidenceScore}. Sağlayıcı: {provider}."

**EXTRACTION_FAILED:**
- Title: "Veri çıkarımı başarısız"
- Message: "{fileName} dosyası işlenirken hata oluştu: {errorMessage}. Lütfen tekrar deneyin."

**BATCH_COMPLETED:**
- Title: "Toplu yükleme tamamlandı"
- Message: "{totalFiles} dosyanın tamamı başarıyla işlendi."

**BATCH_PARTIALLY_COMPLETED:**
- Title: "Toplu yükleme kısmen tamamlandı"
- Message: "{totalFiles} dosyadan {completedFiles} başarılı, {failedFiles} başarısız."

**LOW_CONFIDENCE:**
- Title: "Düşük güven skoru"
- Message: "{invoiceNumber} faturası düşük güven skoru ({confidenceScore}) ile çıkarıldı. Manuel doğrulama gerekiyor."

### 9. Configuration — Environment Variables

- `WEBSOCKET_ALLOWED_ORIGINS`: Default "http://localhost:3001"
- `NOTIFICATION_MAX_PER_USER`: Default 500 (max stored, oldest auto-deleted)
- `NOTIFICATION_AUTO_CLEANUP_DAYS`: Default 90 (delete notifications older than X days)
- `NOTIFICATION_BATCH_NOTIFY_THRESHOLD`: Default 1 (send batch notification only when batch has more than X files)

### 10. Auto-Cleanup

Create a scheduled job that runs daily and:
- Deletes read notifications older than NOTIFICATION_AUTO_CLEANUP_DAYS
- Deletes oldest notifications if a user exceeds NOTIFICATION_MAX_PER_USER
- Logs cleanup statistics

---

## TESTING REQUIREMENTS

### 1. Unit Tests
- Notification entity creation with all fields
- Notification service: notify creates DB record and attempts WebSocket send
- Notification service: notifyCompany sends to all company users
- markAsRead sets is_read=true and read_at timestamp
- markAllAsRead updates all unread for user
- getUnreadCount returns correct count
- Message template rendering with variables

### 2. Integration Tests
- WebSocket connection with valid JWT → connection accepted
- WebSocket connection without JWT → connection rejected
- Send notification → appears on user's WebSocket subscription
- Send notification to offline user → stored in DB, available via REST
- GET /notifications returns paginated results
- GET /notifications?isRead=false filters correctly
- PATCH /{id}/read marks as read
- PATCH /read-all marks all as read
- DELETE /{id} removes notification
- GET /unread-count returns correct number

### 3. Event Trigger Tests
- Mock extraction result → correct notification type created
- Mock batch completion → batch notification created
- Mock low confidence extraction → LOW_CONFIDENCE notification
- Mock all providers failed → EXTRACTION_FAILED notification

---

## VERIFICATION CHECKLIST

### WebSocket
- [ ] STOMP endpoint /ws configured with SockJS
- [ ] JWT authentication on WebSocket CONNECT
- [ ] User-specific destination /user/queue/notifications works
- [ ] Unauthenticated connections rejected

### Database
- [ ] notifications table created via Flyway migration
- [ ] Indexes on user_id, (user_id, is_read), (user_id, created_at)
- [ ] JSONB metadata column works

### Notification Service
- [ ] notify() creates DB record + sends WebSocket
- [ ] notifyCompany() reaches all company users
- [ ] Offline users receive notifications via REST on next login
- [ ] Message templates render correctly with variables

### REST API
- [ ] GET /notifications returns paginated, sorted, filtered results
- [ ] GET /unread-count returns correct count
- [ ] PATCH /{id}/read works (owner only)
- [ ] PATCH /read-all works
- [ ] DELETE /{id} works (owner only)

### Event Triggers
- [ ] Extraction completed → notification created
- [ ] Extraction failed → notification created
- [ ] Batch completed → notification created
- [ ] Low confidence → notification created
- [ ] Manual verify/reject → notification created

### Cleanup
- [ ] Scheduled cleanup job runs
- [ ] Old read notifications deleted
- [ ] Max per user enforced

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/OMER/step_results/faz_27_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified
4. Database migration details
5. WebSocket configuration details (endpoint, auth flow)
6. REST API endpoint summary with example responses
7. Notification types table with trigger points
8. Message template examples
9. Test results
10. Coordination notes for Furkan (WebSocket subscription paths, message format)
11. Issues and solutions
12. Next steps (Phase 27-F frontend, Phase 28 email/push)

---

## DEPENDENCIES

### Requires
- **Phase 4**: JWT Authentication (token validation for WebSocket)
- **Phase 7**: Invoice CRUD (invoice entity, status changes)
- **Phase 19-B**: RabbitMQ Result Listener (extraction result triggers)
- **Phase 20**: Batch job tracking (batch completion triggers)

### Required By
- **Phase 27-F**: Frontend notification UI (FURKAN) — needs WebSocket endpoint and REST API
- **Phase 28**: Email & Push notifications — extends this notification service with additional delivery channels

---

## SUCCESS CRITERIA

1. ✅ WebSocket (STOMP/SockJS) configured with JWT auth
2. ✅ notifications table created with proper indexes
3. ✅ Notification service creates, stores, and delivers notifications
4. ✅ Real-time push to connected users via WebSocket
5. ✅ Offline users see notifications when they next fetch via REST
6. ✅ All notification types triggered from correct system events
7. ✅ REST endpoints for listing, filtering, read status, delete
8. ✅ Turkish message templates for all notification types
9. ✅ Auto-cleanup scheduled job
10. ✅ All tests pass
11. ✅ Result file created

---

## IMPORTANT NOTES

1. **WebSocket + JWT**: The trickiest part is authenticating WebSocket connections. STOMP CONNECT frame doesn't use standard HTTP headers the same way. Use a ChannelInterceptor on the CONNECT frame to extract and validate the JWT.

2. **User Principal**: After JWT validation, set the authenticated user as the principal on the StompHeaderAccessor. This is required for `/user/queue/...` destinations to work correctly.

3. **SimpMessagingTemplate**: Use this Spring class to send messages to users. `convertAndSendToUser(username, "/queue/notifications", payload)` — Spring automatically prefixes with `/user/{username}`.

4. **Don't Block the Result Listener**: Notification creation should not block the RabbitMQ result listener. If notification send fails (WebSocket down, etc.), log the error but don't fail the extraction result processing.

5. **Company Scoping**: Notifications must be company-scoped. Users should only see notifications for their own company's invoices.

6. **RBAC on Notifications**: All authenticated users can see their own notifications. No role-based restriction on viewing notifications, but ADMIN might see additional system-level notifications.

7. **Phase 21 Upgrade**: After this phase, Phase 21's polling for batch status can optionally be replaced by WebSocket push. Furkan can upgrade the batch tracking in Phase 27-F to use WebSocket instead of polling.
