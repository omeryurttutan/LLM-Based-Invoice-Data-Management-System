# PHASE 34: MULTI-LANGUAGE SUPPORT (i18n) INFRASTRUCTURE

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 — LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-33 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth (JWT + Redis), RBAC, Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10: Next.js 14+ App Router, sidebar nav, Shadcn/ui, dark/light mode, responsive layout
- ✅ Phase 11: Login/Register pages, Zustand auth store, Axios interceptor, protected routes
- ✅ Phase 12: Invoice list table (TanStack Query, pagination, sorting), detail page, manual add/edit, category management
- ✅ Phase 13-22: Full extraction pipeline, file upload UI, verification UI (split-view)
- ✅ Phase 23: Advanced Filtering (filter panel, URL-based state, multi-select filters)
- ✅ Phase 24-25: Export (XLSX/CSV + accounting formats: Logo, Mikro, Netsis, Luca)
- ✅ Phase 26: Dashboard (KPI cards, Recharts charts, pending actions)
- ✅ Phase 27-28: Notification system (WebSocket bell icon, email, push notifications)
- ✅ Phase 29: Version History (timeline, diff viewer, revert)
- ✅ Phase 30: Template Learning & Rule Engine (sidebar "Otomasyon" group)
- ✅ Phase 31: KVKK Compliance (encryption, consent, data retention)
- ✅ Phase 32: Rate Limiting & Security Hardening
- ✅ Phase 33: PWA Configuration (manifest, Service Worker, offline mode, install prompt)

### Relevant Existing Frontend Architecture

**Current Hardcoded Turkish Text:**
All phases so far used hardcoded Turkish strings directly in components. Examples:
- Sidebar: "Faturalar", "Yükleme", "Kategoriler", "Dashboard", "Bildirimler", "Şablonlar", "Kurallar"
- Buttons: "Kaydet", "İptal", "Sil", "Düzenle", "Dışa Aktar", "Yükle", "Doğrula", "Reddet"
- Table headers: "Fatura No", "Tedarikçi", "Tarih", "Tutar", "Durum"
- Messages: "Fatura başarıyla oluşturuldu", "Silmek istediğinize emin misiniz?"
- Form labels: "E-posta", "Şifre", "Ad Soyad", "Şirket Adı", "Vergi No"
- Status badges: "Beklemede", "Doğrulandı", "Reddedildi"
- Empty states: "Henüz fatura bulunmuyor", "Veri yüklenirken hata oluştu"
- Tooltips, placeholders, validation messages — all in Turkish

**Formatting (Phase 26):**
Dashboard uses `Intl.NumberFormat('tr-TR', ...)` for currency and number formatting. Date formatting uses DD.MM.YYYY format.

**PWA Manifest (Phase 33):**
manifest.json has `"lang": "tr"` and Turkish app name/description.

**Project Structure (Phase 10):**
```
frontend/src/
├── app/
│   ├── (auth)/
│   │   ├── login/
│   │   └── register/
│   └── (dashboard)/
│       ├── invoices/
│       ├── upload/
│       ├── categories/
│       ├── templates/
│       ├── rules/
│       ├── notifications/
│       └── page.tsx (dashboard)
├── components/
├── features/
├── lib/
├── services/
├── stores/
└── types/
```

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 2 days

---

## OBJECTIVE

Set up a comprehensive internationalization (i18n) infrastructure using `next-intl` so that:

1. **All UI text** comes from translation files, not hardcoded strings
2. **Turkish (tr)** is the primary and default language with complete coverage
3. **English (en)** is added as a second language to demonstrate the i18n system works
4. **Date, number, and currency formatting** adapts to the selected locale
5. **Language switching** mechanism is in place for future language additions
6. All existing components are refactored to use the translation system

---

## DETAILED REQUIREMENTS

### 1. next-intl Setup

**Install and configure `next-intl`** for Next.js 14+ App Router.

**Configuration files to create:**

**1.1 i18n Configuration File:**

Create `src/i18n.ts` (or `src/i18n/config.ts`):
- Default locale: `tr`
- Supported locales: `['tr', 'en']`
- Locale detection strategy: check user preference (stored in localStorage or cookie), then browser language, then fall back to `tr`

**1.2 Middleware:**

Create or update `src/middleware.ts`:
- Use `next-intl/middleware` to handle locale routing
- Strategy: Use cookie/localStorage-based locale (NOT URL prefix-based)
- Reason: URL-based locale (e.g., `/tr/invoices`, `/en/invoices`) adds complexity to existing routing and breaks existing links. For this project, a simpler approach using a cookie/localStorage to store the preferred language is better.
- The middleware reads the `NEXT_LOCALE` cookie and sets the locale accordingly

**1.3 Provider Setup:**

Wrap the app with `NextIntlClientProvider` in the root layout:
- Load messages for the current locale
- Pass to the provider
- Make `useTranslations` hook available in all client components

---

### 2. Translation File Structure

Create structured translation files under `src/messages/` (or `src/locales/`):

```
src/messages/
├── tr/
│   ├── common.json        — shared: buttons, labels, statuses, errors, confirmations
│   ├── auth.json           — login, register, forgot password
│   ├── navigation.json     — sidebar, header, breadcrumbs
│   ├── invoices.json       — invoice list, detail, create, edit, verify
│   ├── upload.json         — file upload, batch upload, progress
│   ├── categories.json     — category management
│   ├── dashboard.json      — dashboard cards, charts, labels
│   ├── notifications.json  — notification types, messages, settings
│   ├── templates.json      — supplier templates
│   ├── rules.json          — automation rules
│   ├── export.json         — export dialogs, format labels
│   ├── settings.json       — user settings, preferences
│   ├── versions.json       — version history, diff, revert
│   └── validation.json     — form validation messages
└── en/
    ├── common.json
    ├── auth.json
    ├── navigation.json
    ├── invoices.json
    ├── upload.json
    ├── categories.json
    ├── dashboard.json
    ├── notifications.json
    ├── templates.json
    ├── rules.json
    ├── export.json
    ├── settings.json
    ├── versions.json
    └── validation.json
```

**Why split into multiple files?** Splitting by feature/domain keeps translation files manageable and allows lazy-loading only needed translations per page.

---

### 3. Translation Key Convention

Use a hierarchical, dot-notation convention for translation keys:

**Pattern:** `namespace.section.key`

**Examples:**

```json
// common.json (tr)
{
  "buttons": {
    "save": "Kaydet",
    "cancel": "İptal",
    "delete": "Sil",
    "edit": "Düzenle",
    "create": "Oluştur",
    "close": "Kapat",
    "confirm": "Onayla",
    "retry": "Tekrar Dene",
    "back": "Geri",
    "next": "İleri",
    "export": "Dışa Aktar",
    "import": "İçe Aktar",
    "upload": "Yükle",
    "download": "İndir",
    "search": "Ara",
    "filter": "Filtrele",
    "clearFilters": "Filtreleri Temizle",
    "selectAll": "Tümünü Seç",
    "verify": "Doğrula",
    "reject": "Reddet",
    "reopen": "Yeniden Aç",
    "apply": "Uygula"
  },
  "status": {
    "pending": "Beklemede",
    "verified": "Doğrulandı",
    "rejected": "Reddedildi",
    "processing": "İşleniyor",
    "active": "Aktif",
    "inactive": "Pasif",
    "locked": "Kilitli"
  },
  "messages": {
    "success": {
      "created": "{entity} başarıyla oluşturuldu",
      "updated": "{entity} başarıyla güncellendi",
      "deleted": "{entity} başarıyla silindi"
    },
    "error": {
      "generic": "Bir hata oluştu. Lütfen tekrar deneyin.",
      "notFound": "{entity} bulunamadı",
      "unauthorized": "Bu işlem için yetkiniz bulunmuyor",
      "networkError": "Bağlantı hatası. İnternet bağlantınızı kontrol edin.",
      "rateLimited": "Çok fazla istek gönderildi. Lütfen biraz bekleyin.",
      "serverError": "Sunucu hatası. Lütfen daha sonra tekrar deneyin."
    },
    "confirm": {
      "delete": "{entity} silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
      "discard": "Kaydedilmemiş değişiklikler var. Çıkmak istediğinize emin misiniz?"
    }
  },
  "labels": {
    "createdAt": "Oluşturulma Tarihi",
    "updatedAt": "Güncellenme Tarihi",
    "actions": "İşlemler",
    "details": "Detaylar",
    "total": "Toplam",
    "noData": "Veri bulunamadı",
    "loading": "Yükleniyor...",
    "showing": "{from}-{to} arası gösteriliyor, toplam {total}",
    "rowsPerPage": "Sayfa başına satır"
  },
  "time": {
    "justNow": "Az önce",
    "minutesAgo": "{count} dakika önce",
    "hoursAgo": "{count} saat önce",
    "daysAgo": "{count} gün önce",
    "yesterday": "Dün",
    "today": "Bugün"
  }
}
```

```json
// invoices.json (tr)
{
  "title": "Faturalar",
  "singular": "Fatura",
  "table": {
    "invoiceNumber": "Fatura No",
    "supplier": "Tedarikçi",
    "date": "Fatura Tarihi",
    "dueDate": "Vade Tarihi",
    "amount": "Tutar",
    "currency": "Para Birimi",
    "status": "Durum",
    "source": "Kaynak",
    "confidence": "Güven Skoru",
    "category": "Kategori"
  },
  "detail": {
    "title": "Fatura Detayı",
    "invoiceInfo": "Fatura Bilgileri",
    "supplierInfo": "Tedarikçi Bilgileri",
    "buyerInfo": "Alıcı Bilgileri",
    "items": "Fatura Kalemleri",
    "summary": "Özet",
    "subtotal": "Ara Toplam",
    "taxAmount": "KDV Tutarı",
    "totalAmount": "Genel Toplam"
  },
  "create": {
    "title": "Yeni Fatura Oluştur",
    "manual": "Manuel Fatura Girişi"
  },
  "edit": {
    "title": "Fatura Düzenle"
  },
  "verify": {
    "title": "Fatura Doğrulama",
    "confirmVerify": "Bu faturayı doğrulamak istediğinize emin misiniz?",
    "confirmReject": "Bu faturayı reddetmek istediğinize emin misiniz?",
    "rejectReason": "Red nedeni (opsiyonel)"
  },
  "filter": {
    "dateRange": "Tarih Aralığı",
    "supplier": "Tedarikçi",
    "category": "Kategori",
    "status": "Durum",
    "amountRange": "Tutar Aralığı",
    "minAmount": "Min. Tutar",
    "maxAmount": "Maks. Tutar",
    "sourceType": "Kaynak Tipi",
    "currency": "Para Birimi",
    "search": "Fatura no veya tedarikçi ara..."
  },
  "empty": {
    "title": "Henüz fatura bulunmuyor",
    "description": "İlk faturanızı yükleyin veya manuel olarak ekleyin.",
    "cta": "Fatura Yükle"
  },
  "llmProvider": {
    "gemini": "Gemini",
    "gpt": "GPT",
    "claude": "Claude"
  },
  "sourceType": {
    "llm": "LLM Çıkarım",
    "eInvoice": "E-Fatura",
    "manual": "Manuel Giriş"
  }
}
```

**Provide similar complete translation files for ALL namespaces listed above, covering EVERY piece of UI text in the application.** This is the most time-consuming part of the phase — be thorough.

---

### 4. English (en) Translations

Create complete English translation files mirroring the Turkish structure. Examples:

```json
// common.json (en)
{
  "buttons": {
    "save": "Save",
    "cancel": "Cancel",
    "delete": "Delete",
    "edit": "Edit",
    "create": "Create",
    "verify": "Verify",
    "reject": "Reject"
  },
  "status": {
    "pending": "Pending",
    "verified": "Verified",
    "rejected": "Rejected"
  }
}
```

```json
// invoices.json (en)
{
  "title": "Invoices",
  "singular": "Invoice",
  "table": {
    "invoiceNumber": "Invoice No",
    "supplier": "Supplier",
    "date": "Invoice Date",
    "amount": "Amount",
    "status": "Status"
  }
}
```

Every key in the Turkish files MUST have a corresponding English translation.

---

### 5. Refactoring Existing Components

This is the core work: go through ALL existing components and replace hardcoded strings with `useTranslations()` calls.

**Pattern for Client Components:**

```tsx
// Before
export function InvoiceList() {
  return <h1>Faturalar</h1>;
}

// After
import { useTranslations } from 'next-intl';

export function InvoiceList() {
  const t = useTranslations('invoices');
  return <h1>{t('title')}</h1>;
}
```

**Pattern for Dynamic Values (Interpolation):**

```tsx
// Translation: "messages.success.created": "{entity} başarıyla oluşturuldu"
t('messages.success.created', { entity: t('invoices.singular') })
// → "Fatura başarıyla oluşturuldu"
```

**Pattern for Pluralization:**

```tsx
// Translation: "showing": "{count, plural, =0 {Sonuç bulunamadı} one {# sonuç} other {# sonuç}}"
t('showing', { count: totalResults })
```

**Components to refactor (comprehensive list by area):**

**Navigation & Layout (Phase 10):**
- Sidebar menu items: Dashboard, Faturalar, Yükleme, Kategoriler, Bildirimler, Otomasyon → Şablonlar, Kurallar
- Header: user menu labels, theme toggle label
- Breadcrumbs: all page titles
- Footer (if exists)

**Auth Pages (Phase 11):**
- Login form: labels, placeholders, buttons, error messages, "Hesabınız yok mu?" link
- Register form: all labels, validation messages
- Password requirements text

**Invoice CRUD (Phase 12):**
- Invoice list: table headers, status badges, action buttons, empty state, pagination labels
- Invoice detail: all section titles, field labels
- Create/edit form: all labels, placeholders, validation messages
- Category CRUD: labels, form fields

**Upload (Phase 21):**
- Upload area: drag-and-drop text, file type/size hints
- Batch upload: progress labels, status messages
- Processing status: "İşleniyor", "Tamamlandı", "Hata"

**Verification UI (Phase 22):**
- Split-view labels, confidence indicators
- Verify/reject buttons and confirmation dialogs
- LLM provider badge

**Filtering (Phase 23):**
- All filter labels, placeholders, clear button

**Export (Phase 24-25):**
- Export dialog: format selection labels, buttons
- Accounting format names

**Dashboard (Phase 26):**
- KPI card titles: "Toplam Fatura", "Toplam Tutar", "Bekleyen", "Bu Ay"
- Chart titles and axis labels
- Pending actions section

**Notifications (Phase 27):**
- Bell icon tooltip
- Dropdown: "Tümünü okundu işaretle", "Tüm bildirimleri göster"
- Notification page: filter tabs, action buttons
- Notification type labels

**Version History (Phase 29):**
- Timeline labels, diff viewer labels, revert confirmation

**Templates & Rules (Phase 30):**
- Template page: table headers, detail drawer labels
- Rules page: card labels, rule builder steps, condition/action labels

**PWA (Phase 33):**
- Install prompt: banner text, buttons
- Update prompt: banner text
- Offline banner text

**Common Components:**
- Confirmation dialogs
- Error boundaries
- Toast messages
- Loading skeletons (any visible text)
- Empty states
- Pagination component

---

### 6. Date, Number, and Currency Formatting

Replace all manual formatting with `next-intl` formatting utilities.

**6.1 Number Formatting:**

```tsx
import { useFormatter } from 'next-intl';

const format = useFormatter();
// Currency
format.number(amount, { style: 'currency', currency: 'TRY' })
// tr → "₺2.456.789,50"
// en → "₺2,456,789.50"

// Percentage
format.number(0.186, { style: 'percent' })
// tr → "%18,6"
// en → "18.6%"

// Plain number
format.number(1423)
// tr → "1.423"
// en → "1,423"
```

**6.2 Date Formatting:**

```tsx
format.dateTime(date, { year: 'numeric', month: '2-digit', day: '2-digit' })
// tr → "12.02.2026"
// en → "02/12/2026"

format.dateTime(date, { dateStyle: 'long' })
// tr → "12 Şubat 2026"
// en → "February 12, 2026"

format.relativeTime(pastDate)
// tr → "2 gün önce"
// en → "2 days ago"
```

**6.3 Where to Apply:**

- Invoice list: date columns, amount columns
- Invoice detail: all date and amount fields
- Dashboard: KPI values, chart tooltips, axis labels
- Export: date range display
- Notifications: relative timestamps
- Audit log: timestamps
- Version history: version timestamps

**Replace all instances of:**
- `Intl.NumberFormat('tr-TR', ...)` → `format.number()`
- `new Date().toLocaleDateString('tr-TR')` → `format.dateTime()`
- Custom relative time functions → `format.relativeTime()`

---

### 7. Language Switcher Component

Create a language switcher UI component.

**Location:** In the header, next to the theme toggle (dark/light mode button).

**Design:**
- A dropdown/select showing the current language flag + code (e.g., 🇹🇷 TR / 🇬🇧 EN)
- On selection: update the locale cookie (`NEXT_LOCALE`), reload translations
- Use flags or language codes — keep it compact
- Shadcn/ui DropdownMenu or Select component
- Dark mode compatible

**Behavior:**
- Switching language should be instant (no full page reload if possible)
- Store preference in a cookie (`NEXT_LOCALE`) so it persists across sessions
- If `next-intl` requires a page reload for Server Components, that's acceptable

**Accessibility:**
- Proper ARIA labels: "Dil seçimi" / "Language selection"
- Keyboard navigable

---

### 8. PWA Manifest Localization

Update the PWA manifest (Phase 33) to support the current locale:

- If the locale is `tr`: manifest name = "Fatura OCR Sistemi", description in Turkish
- If the locale is `en`: manifest name = "Invoice OCR System", description in English
- The `lang` field should match the current locale

Implementation: Generate the manifest dynamically based on locale (via an API route or middleware), or accept that the manifest is static and keep it in Turkish (since the primary audience is Turkish).

For MVP: Keep the manifest in Turkish (static). Add a comment noting it can be made dynamic later.

---

### 9. Backend Error Message Localization

The backend (Spring Boot) returns error messages in Turkish (e.g., "Fatura bulunamadı", "Bu işlem için yetkiniz yok"). For full i18n support:

**Approach for MVP:**
- Backend continues to return error codes (e.g., `INVOICE_NOT_FOUND`, `UNAUTHORIZED`)
- Frontend maps these error codes to translated messages using the translation files
- This is already partially in place — extend it to cover all error responses

**Create an error code → translation key mapping:**

```tsx
const ERROR_CODE_MAP: Record<string, string> = {
  'INVOICE_NOT_FOUND': 'common.messages.error.notFound',
  'UNAUTHORIZED': 'common.messages.error.unauthorized',
  'RATE_LIMIT_EXCEEDED': 'common.messages.error.rateLimited',
  'DUPLICATE_INVOICE': 'invoices.errors.duplicate',
  'VALIDATION_ERROR': 'common.messages.error.validation',
  // ... etc
};
```

The Axios interceptor should catch error responses, look up the error code, and return the translated message.

---

### 10. Type Safety for Translation Keys

Add TypeScript type safety for translation keys to catch typos at compile time.

**Approach:**
- `next-intl` supports typed messages via the `Messages` type
- Create a global type declaration file: `src/types/i18n.d.ts`
- Import the Turkish messages as the source of truth for the type
- This ensures that `t('some.invalid.key')` produces a TypeScript error

```tsx
// src/types/i18n.d.ts
import tr from '@/messages/tr';

type Messages = typeof tr;

declare global {
  interface IntlMessages extends Messages {}
}
```

---

### 11. Testing Requirements

- **Translation completeness**: Write a script (or test) that compares all keys in `tr/*.json` with `en/*.json` and reports any missing keys
- **Component rendering**: Verify key components render correctly in both Turkish and English
- **Formatting**: Test that currency, dates, and numbers format correctly in both locales
- **Language switching**: Verify switching language updates all visible text
- **Fallback**: If a translation key is missing in English, verify it falls back to Turkish

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_34.0_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Files created or modified (with paths)
3. next-intl configuration details
4. Translation file structure (list all files with key counts)
5. Total translation key count (TR and EN)
6. Refactored component list (which components were updated)
7. Formatting changes (where Intl.NumberFormat was replaced)
8. Language switcher implementation details
9. Error code mapping documentation
10. TypeScript type safety setup
11. Translation completeness check results (any missing keys)
12. Screenshots: app in Turkish, app in English, language switcher
13. Test results
14. Known issues (any components not yet refactored, missing translations)
15. Next steps (Phase 35 unit tests, adding more languages in the future)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 10**: App layout (sidebar, header — components to refactor)
- **Phase 11**: Auth pages (login/register — components to refactor)
- **Phase 12**: Invoice CRUD UI (components to refactor)
- **Phase 33**: PWA Configuration (manifest localization consideration)

### Required By
- **Phase 37**: Frontend E2E Tests (tests may need locale-aware selectors)

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] `next-intl` installed and configured for Next.js 14+ App Router
- [ ] Turkish (tr) translation file created with all keys
- [ ] English (en) translation file created with all keys
- [ ] Translation key completeness check: every TR key has an EN counterpart
- [ ] Zero hardcoded user-facing strings remaining in components (grep verified)
- [ ] All page titles use translated strings
- [ ] All button labels use translated strings
- [ ] All form labels and placeholders use translated strings
- [ ] All error messages use translated strings
- [ ] All toast/notification messages use translated strings
- [ ] All table headers use translated strings
- [ ] All status badges use translated strings
- [ ] All empty state messages use translated strings
- [ ] Date formatting: DD.MM.YYYY for TR, MM/DD/YYYY for EN
- [ ] Number formatting: 1.234,56 for TR, 1,234.56 for EN
- [ ] Currency formatting adapts to locale
- [ ] Relative time: "2 gün önce" for TR, "2 days ago" for EN
- [ ] Language switcher component in header works
- [ ] Language switcher persists preference in cookie
- [ ] Language preference survives page reload
- [ ] Backend error codes mapped to frontend translated messages
- [ ] TypeScript type safety: invalid translation key causes compile error
- [ ] App functions correctly end-to-end in Turkish
- [ ] App functions correctly end-to-end in English
- [ ] Dark mode works in both languages
- [ ] `npm run build` completes without errors
- [ ] `npm run lint` passes without errors
- [ ] Result file created at docs/FURKAN/step_results/faz_34.0_result.md
---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ `next-intl` is installed and configured for Next.js 14+ App Router
2. ✅ Turkish (tr) translation files are complete — covering ALL UI text across all features
3. ✅ English (en) translation files are complete — every TR key has an EN counterpart
4. ✅ ALL existing components are refactored to use `useTranslations()` — zero hardcoded user-facing text
5. ✅ Date formatting adapts to locale (DD.MM.YYYY for TR, MM/DD/YYYY for EN)
6. ✅ Number formatting adapts to locale (1.234,56 for TR, 1,234.56 for EN)
7. ✅ Currency formatting adapts to locale
8. ✅ Relative time formatting works in both languages ("2 gün önce" / "2 days ago")
9. ✅ Language switcher component is in the header and functional
10. ✅ Language preference persists across sessions (cookie-based)
11. ✅ Backend error codes are mapped to translated frontend messages
12. ✅ TypeScript type safety catches invalid translation keys at compile time
13. ✅ Translation completeness check passes (no missing keys between TR and EN)
14. ✅ App looks and functions correctly in both Turkish and English
15. ✅ Dark mode works correctly with both languages
16. ✅ Build and lint pass without errors
17. ✅ Result file is created at docs/FURKAN/step_results/faz_34.0_result.md

---

## IMPORTANT NOTES

1. **Turkish is Primary**: Turkish is the main language. Start by extracting all existing hardcoded Turkish text into `tr/*.json` files. Then create the English versions. If time is short, prioritize complete Turkish extraction — English can have some gaps filled later.

2. **Do NOT Use URL-Based Routing for Locale**: Avoid `/tr/invoices` and `/en/invoices` URL patterns. This would require restructuring all routes and break existing links. Use cookie-based locale selection instead. The URL stays the same regardless of language.

3. **Translation Key Naming Must Be Consistent**: Follow the established convention strictly. Use camelCase for keys. Group by feature and section. Avoid deeply nested structures (max 3 levels).

4. **ICU Message Format**: `next-intl` supports ICU message format for pluralization and interpolation. Use it for dynamic messages: `{count, plural, =0 {Sonuç yok} one {# sonuç} other {# sonuç}}`. Do NOT hardcode plural logic in components.

5. **Do NOT Translate Code-Level Values**: Only translate user-facing text. Do NOT translate: enum values (PENDING, VERIFIED), API field names, CSS class names, log messages, console output.

6. **Lazy Loading Translations**: For performance, consider loading only the needed namespace per page. `next-intl` supports this with `getMessages()` in Server Components. Each page should only load the translations it needs.

7. **Preserve Existing Behavior**: The i18n refactoring must not change any existing functionality. The app should look and behave exactly the same (in Turkish) after the refactoring. English is an addition, not a replacement.

8. **Backend Stays in Turkish for Now**: Backend error messages, log messages, and email templates remain in Turkish. Full backend i18n would require an `Accept-Language` header-based message resolver in Spring Boot — that's out of scope for this phase. The frontend handles display language; the backend returns error codes.
