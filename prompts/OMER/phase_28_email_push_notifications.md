# PHASE 28: NOTIFICATION SYSTEM — EMAIL & PUSH NOTIFICATIONS

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001
  - **Next.js Frontend**: Port 3001

### Current State (Phases 0-27 Completed)
- ✅ Phase 0-9: Infrastructure, Auth (JWT + Redis), RBAC, Invoice CRUD, Audit Log, Dedup
- ✅ Phase 10-12: Frontend layout, auth pages, invoice CRUD UI
- ✅ Phase 13-22: Full extraction pipeline, upload UI, verification UI
- ✅ Phase 23-26: Filtering, Export (XLSX/CSV + accounting), Dashboard (backend + frontend)
- ✅ Phase 27 (Backend — ÖMER): WebSocket (STOMP/SockJS) with JWT auth, notifications table (id, user_id, company_id, type, title, message, severity, is_read, reference_type, reference_id, JSONB metadata), notification service (notify, notifyCompany, markAsRead, markAllAsRead, getUnreadCount), REST API (GET /notifications, GET /unread-count, PATCH /read, PATCH /read-all, DELETE), 11 notification types (EXTRACTION_COMPLETED, EXTRACTION_FAILED, BATCH_COMPLETED, LOW_CONFIDENCE, etc.), event triggers wired into result listener and invoice status changes, Turkish message templates, auto-cleanup scheduled job
- ✅ Phase 27-F (Frontend — FURKAN): WebSocket connection manager (SockJS + STOMP), bell icon with unread badge, notification dropdown, full /notifications page, toast notifications, Phase 21 batch polling upgraded to WebSocket

### What Phase 27 Delivered (Foundation for This Phase)
- **Notification Service**: Central service with `notify()` method that stores to DB + sends via WebSocket
- **Notification Types Enum**: 11 types already defined
- **Channel**: Currently only IN_APP (WebSocket + REST). This phase adds EMAIL and PUSH channels
- **notifications table**: Already has all needed fields

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 2-3 days

---

## OBJECTIVE

Extend the Phase 27 notification system with two additional delivery channels: email notifications (via SMTP or SendGrid) and browser push notifications (via Web Push API). Implement user-level notification preferences so each user can control which events trigger which channel. The notification service becomes a multi-channel dispatcher: when an event occurs, it checks the user's preferences and delivers to the appropriate channels (in-app, email, push, or any combination).

---

## DETAILED REQUIREMENTS

### 1. Multi-Channel Architecture

Extend the notification service to support three channels:

```
Event Occurs (e.g., extraction completed)
         │
         ▼
  Notification Service
         │
         ├──► Check user preferences for this event type
         │
         ├──► IN_APP channel (Phase 27 — already done)
         │        └─► Store in DB + send via WebSocket
         │
         ├──► EMAIL channel (this phase)
         │        └─► Render HTML template + send via SMTP/SendGrid
         │
         └──► PUSH channel (this phase)
                  └─► Build push payload + send via Web Push API
```

**Channel Selection Logic:**
For each notification event:
1. Check if user has preferences for this event type
2. If no preferences set → use defaults (see section 5)
3. For each enabled channel → dispatch to that channel's sender
4. All channels are independent — one failing doesn't block others
5. Email and push are sent asynchronously (don't block the main thread)

### 2. Email Notification Service

**Technology Choice:** Spring Boot Starter Mail (SMTP) as primary. Optionally support SendGrid as an alternative (configurable).

**SMTP Configuration:**
- Use spring-boot-starter-mail (already available or add to pom.xml)
- Configure via environment variables
- Support both direct SMTP and SendGrid SMTP relay

**Email Templates:**
Use Thymeleaf HTML email templates. Each template should be professional, responsive (works in email clients), and branded.

**Required Email Templates:**

| Template | Subject (Turkish) | Triggered By |
|---|---|---|
| extraction_completed | "Fatura veri çıkarımı tamamlandı — {invoiceNumber}" | EXTRACTION_COMPLETED |
| extraction_failed | "Fatura veri çıkarımı başarısız — {fileName}" | EXTRACTION_FAILED |
| batch_completed | "Toplu yükleme tamamlandı — {completedCount}/{totalCount}" | BATCH_COMPLETED, BATCH_PARTIALLY_COMPLETED |
| low_confidence | "Düşük güven skoru — {invoiceNumber} ({confidenceScore})" | LOW_CONFIDENCE |
| all_providers_down | "⚠ Sistem Uyarısı: Tüm LLM sağlayıcılar erişilemez" | ALL_PROVIDERS_DOWN |
| daily_summary | "Günlük Fatura Özeti — {date}" | Scheduled daily (optional) |

**Email Template Content Structure:**
- Company logo/header area (configurable)
- Greeting: "Merhaba {firstName},"
- Main content specific to the notification type
- Action button: "Faturayı Görüntüle" / "Toplu Yüklemeyi Görüntüle" etc. (links to frontend URL)
- Footer: "Bu bildirimi almak istemiyorsanız bildirim tercihlerinizi güncelleyebilirsiniz." with link to preferences

**Action Button URLs:**
- INVOICE reference → `{FRONTEND_URL}/invoices/{referenceId}`
- BATCH reference → `{FRONTEND_URL}/invoices?batchId={referenceId}`
- SYSTEM reference → `{FRONTEND_URL}/notifications`
- Preferences → `{FRONTEND_URL}/settings/notifications`

**Email Sending Rules:**
- Send asynchronously (use @Async or CompletableFuture)
- Rate limit: max 10 emails per user per hour (prevent spam during bulk operations)
- For batch operations: send ONE summary email, not one per file
- Respect user preferences (only send if email channel enabled for this event type)
- Log every email sent (recipient, template, subject, success/failure)

### 3. Push Notification Service (Web Push API)

**Technology: Web Push Protocol with VAPID**

**Backend (Spring Boot):**
- Generate VAPID key pair (public + private) — store securely in environment variables
- Use a Web Push library for Java (e.g., `web-push-java` or `nl.martijndwars:web-push-utils`)
- Store push subscriptions from clients

**Push Subscription Storage:**
Create a new table via Flyway migration:

**push_subscriptions table:**

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| user_id | BIGINT | NOT NULL, FK → users(id) | Subscriber |
| endpoint | TEXT | NOT NULL | Push service URL |
| p256dh_key | TEXT | NOT NULL | Client public key |
| auth_key | TEXT | NOT NULL | Client auth secret |
| user_agent | VARCHAR(255) | NULL | Browser info |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| last_used_at | TIMESTAMP | NULL | Last successful push |

Index: idx_push_subscriptions_user ON (user_id)

**Push Subscription REST API:**

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/v1/push/subscribe | Register a push subscription |
| DELETE | /api/v1/push/unsubscribe | Remove a push subscription |
| GET | /api/v1/push/vapid-public-key | Return the VAPID public key (for frontend) |

**Push Notification Payload:**

| Field | Type | Description |
|---|---|---|
| title | string | Notification title (Turkish) |
| body | string | Short message |
| icon | string | App icon URL |
| badge | string | Badge icon URL |
| tag | string | Notification tag (for grouping/replacing) |
| data | object | { url: "link to open on click", notificationId, type } |

**Push Sending Rules:**
- Send asynchronously
- If push fails with 410 Gone (subscription expired) → remove the subscription from DB
- If push fails with other errors → log and retry once
- Respect user preferences
- Collapse similar notifications: use `tag` field so multiple extractions don't stack 20 separate push notifications — instead, replace with "X dosya işlendi"

**Frontend Push Integration (Service Worker):**
This phase also requires a small frontend addition:
- Register a Service Worker (or update existing if Phase 33 PWA adds one)
- Request push notification permission from the user
- On permission granted → get PushSubscription → send to POST /api/v1/push/subscribe
- In Service Worker: handle `push` event → show notification, handle `notificationclick` → navigate to URL in data

### 4. Permission Request Flow (Frontend)

When to ask for push notification permission:
- NOT on first page load (bad UX, users likely deny)
- Show a soft prompt after user has been using the app for a while
- Trigger: After the user's first successful invoice upload, OR after they visit the notification settings page
- Soft prompt UI: A small banner or card: "Fatura işlemlerinizle ilgili anlık bildirimler almak ister misiniz?" with "İzin Ver" and "Şimdi Değil" buttons
- If "İzin Ver" → call Notification.requestPermission()
- If granted → subscribe to push, send subscription to backend
- If "Şimdi Değil" → hide for this session, show again after 7 days
- If denied by browser → don't ask again, show a note in settings: "Push bildirimleri tarayıcı tarafından engellenmiş"

### 5. User Notification Preferences

**Database: notification_preferences table**

| Column | Type | Constraints | Description |
|---|---|---|---|
| id | BIGSERIAL | PRIMARY KEY | |
| user_id | BIGINT | NOT NULL, UNIQUE FK → users(id) | One row per user |
| preferences | JSONB | NOT NULL, DEFAULT '{}' | Preference map |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Preferences JSONB Structure:**
A map of notification type → enabled channels.

Example:
```
{
  "EXTRACTION_COMPLETED": { "in_app": true, "email": false, "push": true },
  "EXTRACTION_FAILED": { "in_app": true, "email": true, "push": true },
  "BATCH_COMPLETED": { "in_app": true, "email": true, "push": true },
  "BATCH_PARTIALLY_COMPLETED": { "in_app": true, "email": true, "push": true },
  "LOW_CONFIDENCE": { "in_app": true, "email": true, "push": false },
  "HIGH_CONFIDENCE_AUTO_VERIFIED": { "in_app": true, "email": false, "push": false },
  "INVOICE_VERIFIED": { "in_app": true, "email": false, "push": false },
  "INVOICE_REJECTED": { "in_app": true, "email": true, "push": false },
  "PROVIDER_DEGRADED": { "in_app": true, "email": false, "push": false },
  "ALL_PROVIDERS_DOWN": { "in_app": true, "email": true, "push": true },
  "SYSTEM_ANNOUNCEMENT": { "in_app": true, "email": false, "push": false }
}
```

**Default Preferences (when user has no preferences row):**

| Type | In-App | Email | Push |
|---|---|---|---|
| EXTRACTION_COMPLETED | ✅ | ❌ | ✅ |
| EXTRACTION_FAILED | ✅ | ✅ | ✅ |
| BATCH_COMPLETED | ✅ | ✅ | ✅ |
| BATCH_PARTIALLY_COMPLETED | ✅ | ✅ | ✅ |
| LOW_CONFIDENCE | ✅ | ✅ | ❌ |
| HIGH_CONFIDENCE_AUTO_VERIFIED | ✅ | ❌ | ❌ |
| INVOICE_VERIFIED | ✅ | ❌ | ❌ |
| INVOICE_REJECTED | ✅ | ✅ | ❌ |
| PROVIDER_DEGRADED | ✅ | ❌ | ❌ |
| ALL_PROVIDERS_DOWN | ✅ | ✅ | ✅ |
| SYSTEM_ANNOUNCEMENT | ✅ | ❌ | ❌ |

**REST API for Preferences:**

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/v1/notifications/preferences | Get current user's preferences |
| PUT | /api/v1/notifications/preferences | Update preferences (full replace) |
| PATCH | /api/v1/notifications/preferences/{type} | Update single type preferences |

### 6. Notification Settings Page (Frontend)

**Route:** `/settings/notifications` (or add a tab to an existing settings page)

**UI:**
- Table/grid layout: rows = notification types (Turkish labels), columns = channels (Uygulama İçi, E-posta, Push)
- Each cell is a toggle switch (Shadcn/ui Switch component)
- Type labels (Turkish):
  - EXTRACTION_COMPLETED → "Veri Çıkarımı Tamamlandı"
  - EXTRACTION_FAILED → "Veri Çıkarımı Başarısız"
  - BATCH_COMPLETED → "Toplu Yükleme Tamamlandı"
  - BATCH_PARTIALLY_COMPLETED → "Toplu Yükleme Kısmen Tamamlandı"
  - LOW_CONFIDENCE → "Düşük Güven Skoru"
  - HIGH_CONFIDENCE_AUTO_VERIFIED → "Otomatik Doğrulama"
  - INVOICE_VERIFIED → "Fatura Doğrulandı"
  - INVOICE_REJECTED → "Fatura Reddedildi"
  - PROVIDER_DEGRADED → "LLM Sağlayıcı Sorunlu"
  - ALL_PROVIDERS_DOWN → "Tüm Sağlayıcılar Erişilemez"
  - SYSTEM_ANNOUNCEMENT → "Sistem Duyurusu"
- Push column: disabled/greyed out if push permission not granted, with "İzin Ver" button
- "Kaydet" button to save changes
- "Varsayılana Sıfırla" link to reset to defaults
- In-app column toggles should show a warning: "Uygulama içi bildirimleri kapatmak önerilmez"

### 7. Updated Notification Service Flow

Update the existing Phase 27 notification service:

**Before (Phase 27):**
1. Event → create notification → store in DB → send via WebSocket

**After (Phase 28):**
1. Event → determine notification type
2. Look up user's preferences for this type
3. For each enabled channel:
   a. IN_APP: store in DB + send via WebSocket (existing Phase 27 logic)
   b. EMAIL: render template + send async via SMTP (new)
   c. PUSH: build payload + send async via Web Push (new)
4. Log delivery status per channel

**Channel Dispatcher Pattern:**
Create a NotificationChannel interface with implementations:
- InAppNotificationChannel (existing, wraps Phase 27 logic)
- EmailNotificationChannel (new)
- PushNotificationChannel (new)

The notification service iterates over enabled channels and dispatches to each. This makes it easy to add more channels in the future.

### 8. Configuration — Environment Variables

**Email (SMTP):**
- `SPRING_MAIL_HOST`: SMTP host (e.g., "smtp.gmail.com" or "smtp.sendgrid.net")
- `SPRING_MAIL_PORT`: SMTP port (default: 587)
- `SPRING_MAIL_USERNAME`: SMTP username
- `SPRING_MAIL_PASSWORD`: SMTP password or API key
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH`: true
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE`: true
- `NOTIFICATION_EMAIL_FROM`: Sender address (e.g., "bildirim@faturaocr.com")
- `NOTIFICATION_EMAIL_FROM_NAME`: Sender display name ("Fatura OCR Sistemi")
- `NOTIFICATION_EMAIL_RATE_LIMIT_PER_HOUR`: Default 10

**Push (Web Push / VAPID):**
- `VAPID_PUBLIC_KEY`: VAPID public key (base64url encoded)
- `VAPID_PRIVATE_KEY`: VAPID private key (base64url encoded)
- `VAPID_SUBJECT`: Contact email or URL (e.g., "mailto:admin@faturaocr.com")

**General:**
- `NOTIFICATION_EMAIL_ENABLED`: Default true
- `NOTIFICATION_PUSH_ENABLED`: Default true
- `FRONTEND_URL`: Default "http://localhost:3001" (for email links)

### 9. Database Migration

Create Flyway migration: `V{next_number}__phase_28_email_push_notifications.sql`

**Changes:**
- Create `push_subscriptions` table
- Create `notification_preferences` table
- Index on push_subscriptions(user_id)
- Index on notification_preferences(user_id) — already unique

### 10. Logging

**INFO:**
- Email sent (recipient, template, subject)
- Push notification sent (user_id, subscription endpoint)
- Push subscription registered/removed
- Preferences updated (user_id)

**WARNING:**
- Email send failed (recipient, error — will not retry by default)
- Push subscription expired (410 Gone — auto-removed)
- Email rate limit reached for user (user_id)
- Push permission not granted (user tried to subscribe without permission)

**ERROR:**
- SMTP connection failed
- VAPID key configuration invalid
- Template rendering failed
- Push send failed (non-410 error)

---

## TESTING REQUIREMENTS

### 1. Unit Tests
- Email template renders correctly with variables
- Push payload built correctly
- Preference lookup returns correct channels for each type
- Default preferences used when no user preference exists
- Email rate limiter blocks after threshold
- Channel dispatcher routes to correct channels based on preferences
- Push subscription expired (410) → removed from DB

### 2. Integration Tests
- Email sent via mock SMTP server (use GreenMail or similar)
- Push subscription stored and retrieved correctly
- Preferences saved and fetched correctly via REST API
- Full flow: event → check preferences → dispatch to enabled channels
- Email not sent when email channel disabled in preferences
- Push not sent when push channel disabled

### 3. Frontend Tests
- Push permission request flow (mock Notification API)
- Notification settings page renders all types and channels
- Toggle switch updates preference correctly
- Disabled push column when permission not granted
- Subscription sent to backend on permission grant

---

## VERIFICATION CHECKLIST

### Email
- [ ] spring-boot-starter-mail configured
- [ ] SMTP connection works (test with real or mock server)
- [ ] All 5+ email templates created and render correctly
- [ ] Email sent asynchronously
- [ ] Rate limiting works (max 10/user/hour)
- [ ] Batch events send ONE summary email (not per file)
- [ ] Unsubscribe link in footer

### Push
- [ ] VAPID key pair generated and configured
- [ ] POST /push/subscribe stores subscription
- [ ] DELETE /push/unsubscribe removes subscription
- [ ] GET /push/vapid-public-key returns correct key
- [ ] Push notification sent to subscriber
- [ ] Expired subscriptions (410) auto-removed
- [ ] Service Worker handles push event
- [ ] Click on push notification navigates to correct URL

### Preferences
- [ ] GET /preferences returns user preferences (or defaults)
- [ ] PUT /preferences updates all preferences
- [ ] PATCH /preferences/{type} updates single type
- [ ] notification_preferences table created
- [ ] Default preferences applied for new users

### Settings UI
- [ ] Settings page renders type × channel grid
- [ ] Toggle switches work and save
- [ ] Push column disabled when permission not granted
- [ ] "İzin Ver" button triggers browser permission request
- [ ] "Varsayılana Sıfırla" resets to defaults

### Multi-Channel Dispatch
- [ ] Event triggers all enabled channels
- [ ] Disabled channels skipped
- [ ] One channel failing doesn't block others
- [ ] All dispatches logged

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/OMER/step_results/faz_28_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified
4. Database migration details (push_subscriptions, notification_preferences)
5. Email configuration (SMTP settings, templates created)
6. Push configuration (VAPID setup, subscription flow)
7. Preferences API details with example request/response
8. Settings page screenshot
9. Channel dispatcher architecture
10. Test results
11. Issues and solutions
12. Next steps

---

## DEPENDENCIES

### Requires
- **Phase 27**: Notification service (in-app channel, notification types, triggers, DB table)
- **Phase 27-F**: Frontend WebSocket (for extending with push permission flow)
- **Phase 4**: Auth (JWT, user entity with email)

### Required By
- **Phase 33**: PWA Configuration — may reuse the Service Worker for push notifications
- **Phase 40**: Monitoring — email/push delivery metrics

---

## SUCCESS CRITERIA

1. ✅ Email notifications sent via SMTP for enabled event types
2. ✅ HTML email templates professional and responsive
3. ✅ Email rate limiting prevents spam (max 10/user/hour)
4. ✅ Batch events produce a single summary email
5. ✅ Push notifications delivered via Web Push API
6. ✅ VAPID key pair configured, push subscriptions stored
7. ✅ Expired push subscriptions auto-cleaned
8. ✅ Service Worker handles push events and click navigation
9. ✅ User notification preferences stored and respected
10. ✅ Notification settings UI with type × channel toggle grid
11. ✅ Default preferences applied for users without custom settings
12. ✅ Multi-channel dispatch: one channel failing doesn't block others
13. ✅ All channels dispatched asynchronously
14. ✅ All tests pass
15. ✅ Result file created

---

## IMPORTANT NOTES

1. **Don't Break Phase 27**: The existing in-app notification channel (WebSocket + DB) must continue to work exactly as before. This phase adds EMAIL and PUSH alongside it, not replacing it.

2. **Email in Dev**: For development, use a mock SMTP server (MailHog, GreenMail, or Mailtrap) instead of real SMTP. Add MailHog to Docker Compose as an optional service (ports 1025 SMTP, 8025 UI).

3. **VAPID Key Generation**: Generate the VAPID key pair once and store in .env. There are online generators or use a Java/Node.js tool. The same key pair must be used consistently — changing it invalidates all existing push subscriptions.

4. **Push is HTTPS-Only**: Web Push API requires HTTPS in production. In development, localhost is exempt. Note this in documentation for Phase 39 (deployment).

5. **Service Worker Scope**: If Phase 33 (PWA) will create a Service Worker, coordinate to avoid conflicts. Ideally, push handling is added to the same Service Worker. If Phase 33 is not done yet, create a minimal Service Worker just for push in this phase, designed to be merged later.

6. **Batch Email Aggregation**: When a batch of 20 files completes, do NOT send 20 individual emails. Wait for the batch to complete, then send ONE email: "Toplu yüklemeniz tamamlandı: 18 başarılı, 2 başarısız." Use the BATCH_COMPLETED / BATCH_PARTIALLY_COMPLETED event, not individual EXTRACTION_COMPLETED events.

7. **Email Template Quality**: Use inline CSS (email clients strip <style> tags). Keep templates simple — tables for layout, inline styles, no JavaScript. Test in common email clients (Gmail, Outlook). Consider using a pre-built responsive email template framework like MJML (compile to HTML) or keep it simple with basic HTML tables.

8. **Preferences are Optional**: If a user never visits the settings page, default preferences apply. The system should work perfectly without any user customization.

9. **Frontend Coordination**: The notification settings page and push permission flow require frontend work. Since this phase is assigned to Ömer, either Ömer implements these frontend pieces, or coordinate with Furkan. The settings page is relatively simple (a grid of toggles). The push permission flow can be a small utility component.

10. **Don't Overengineer**: For a graduation project, the email and push features should work but don't need enterprise-grade reliability. Focus on the happy path and handle the most common errors. Skip complex features like email bounce handling, delivery receipts, or A/B testing.
