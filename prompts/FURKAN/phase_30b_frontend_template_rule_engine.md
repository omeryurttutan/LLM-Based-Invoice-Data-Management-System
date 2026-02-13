# PHASE 30-B: TEMPLATE & RULE ENGINE — FRONTEND UI

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080
  - **Python Microservice**: Port 8000 — LLM-based extraction
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-29 + 30-A Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth (JWT + Redis), RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN), Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10: Next.js 14+ App Router, sidebar navigation with items: Dashboard (/), Faturalar (/invoices), Yükleme (/upload), Kategoriler (/categories). Shadcn/ui, dark/light mode toggle, responsive (collapsible on mobile)
- ✅ Phase 11: Login/Register pages, Zustand auth store, Axios interceptor with token refresh, protected routes
- ✅ Phase 12: Invoice list table (TanStack Query, pagination, sorting, status badges), detail page, manual add/edit forms, category management
- ✅ Phase 13-22: Full extraction pipeline, file upload, verification UI
- ✅ Phase 23: Advanced Filtering — filter panel, URL-based state, search
- ✅ Phase 24-25: Export (XLSX/CSV + accounting formats)
- ✅ Phase 26: Dashboard — summary cards, Recharts charts (category pie, monthly trend, supplier bar), pending actions list
- ✅ Phase 27-28: Notification system — WebSocket bell icon, dropdown, email/push preferences
- ✅ Phase 29: Version History — timeline component, diff viewer, revert confirmation
- ✅ Phase 30-A (ÖMER — Backend): Supplier templates table, automation rules table, rule execution log, template learning service, rule engine service, REST APIs, pipeline integration

### What Phase 30-A Delivers (Backend Endpoints)

**Supplier Templates API — Base URL**: `http://localhost:8080/api/v1/templates`

| Endpoint | Method | Description |
|----------|--------|-------------|
| /api/v1/templates | GET | List all supplier templates (paginated). Params: page, size, sort, search, isActive, minSamples |
| /api/v1/templates/{id} | GET | Single template with full learned_data |
| /api/v1/templates/supplier/{taxNumber} | GET | Lookup by supplier tax number |
| /api/v1/templates/{id} | PUT | Update template (default_category, is_active toggle) |
| /api/v1/templates/{id} | DELETE | Soft delete (set is_active = false) |
| /api/v1/templates/{id}/reset | POST | Reset learned data (clears learned_data, resets sample_count) |
| /api/v1/templates/stats | GET | System-wide template statistics |

**Template Response Shape:**
- id, supplierTaxNumber, supplierName, sampleCount, learnedData (field_accuracy, common_corrections, typical_line_item_count, typical_amount_range), defaultCategory (id + name), defaultCurrency, typicalTaxRates, averageConfidence, lastInvoiceDate, isActive, createdAt, updatedAt

**Automation Rules API — Base URL**: `http://localhost:8080/api/v1/rules`

| Endpoint | Method | Description |
|----------|--------|-------------|
| /api/v1/rules | GET | List all rules (paginated). Params: page, size, sort, search, isActive, triggerPoint |
| /api/v1/rules/{id} | GET | Single rule with full conditions and actions |
| /api/v1/rules | POST | Create a new rule |
| /api/v1/rules/{id} | PUT | Update a rule |
| /api/v1/rules/{id} | DELETE | Soft delete (set is_active = false) |
| /api/v1/rules/{id}/toggle | POST | Toggle is_active |
| /api/v1/rules/{id}/test | POST | Dry run: test rule against a specific invoice. Body: { invoiceId: number } |
| /api/v1/rules/{id}/history | GET | Execution history for a rule (paginated) |
| /api/v1/rules/execution-log | GET | All rule execution logs (paginated) |

**Rule Request Shape (Create/Update):**
- name (string, required)
- description (string, optional)
- conditions (array of { field, operator, value })
- actions (array of { type, params })
- conditionLogic ("AND" or "OR")
- priority (number, 1-1000, default 100)
- triggerPoint ("AFTER_EXTRACTION", "AFTER_VERIFICATION", "ON_MANUAL_CREATE")

**Available Condition Fields:**
supplier_name, supplier_tax_number, total_amount, subtotal, tax_amount, currency, source_type, confidence_score, category_name, status, llm_provider, invoice_date

**Available Operators:**
equals, not_equals, contains, not_contains, greater_than, less_than, greater_than_or_equal, less_than_or_equal, in, not_in, is_null, is_not_null, between

**Available Action Types:**
SET_CATEGORY (params: { category_id }), SET_STATUS (params: { status }), ADD_NOTE (params: { note }), FLAG_FOR_REVIEW (params: { reason }), SEND_NOTIFICATION (params: { message, target_roles }), SET_PRIORITY (params: { priority })

**Rule Test (Dry Run) Response:**
- ruleId, ruleName, invoiceId
- conditionsEvaluated: array of { condition, matched, actualValue }
- allConditionsMet: boolean
- actionsToApply: array of action descriptions
- note: "This is a dry run. No changes were made."

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 2-3 days

### Relationship to Phase 30-A
Phase 30 is split:
- **30-A**: Backend (ÖMER) — database, services, REST APIs, pipeline integration
- **30-B (this phase)**: Frontend (FURKAN) — template management page, rule builder UI, rule testing, execution log viewer

---

## OBJECTIVE

Build the frontend pages for managing supplier templates and automation rules. This includes:

1. **Supplier Templates Page**: A read-mostly page showing learned supplier patterns, with the ability to view details, edit default category, toggle active status, and reset learned data.

2. **Automation Rules Page**: A full CRUD interface with a visual rule builder — users can define conditions and actions using dropdowns and input fields, test rules against existing invoices, view execution history, and manage rule priority.

3. **Navigation Integration**: Add new sidebar items for "Şablonlar" (Templates) and "Kurallar" (Rules) under a new "Otomasyon" (Automation) group in the sidebar.

---

## DETAILED REQUIREMENTS

### 1. Sidebar Navigation Update

Add a new navigation group to the existing sidebar:

**Group: "Otomasyon" (Automation)**
- Icon: Use a gear/cog or workflow icon from Lucide
- Sub-items:
  - "Şablonlar" → route: /templates
  - "Kurallar" → route: /rules

The group should be collapsible (consistent with the existing sidebar pattern). Only show to users with ADMIN, MANAGER, or ACCOUNTANT roles. INTERN users should not see these menu items.

---

### 2. Supplier Templates Page (/templates)

**2.1 Templates List View**

Page header: "Tedarikçi Şablonları" with a subtitle "Sistem doğrulanmış faturalardan tedarikçi kalıplarını otomatik olarak öğrenir"

**Statistics Bar** (top of page, using the GET /templates/stats endpoint):
- Total templates count
- Total verified samples
- Average accuracy percentage
- Active templates count

Display as small summary cards in a horizontal row (similar to dashboard stats cards).

**Templates Table:**
Using TanStack Table with the following columns:
- Tedarikçi Adı (Supplier Name) — with the tax number shown below in smaller text
- Öğrenilen Fatura Sayısı (Sample Count) — with a progress-like indicator
- Ortalama Güven Skoru (Average Confidence) — color-coded: green >= 80, yellow >= 60, red < 60
- Varsayılan Kategori (Default Category) — category badge with color
- Son Fatura Tarihi (Last Invoice Date) — relative time display ("3 gün önce")
- Durum (Status) — Active/Inactive toggle switch
- İşlemler (Actions) — View details button, reset button (with confirmation)

**Table Features:**
- Pagination (server-side)
- Search bar: search by supplier name or tax number
- Filter: active/inactive toggle
- Sort by: sample_count, average_confidence, last_invoice_date, supplier_name

**Empty State:**
"Henüz tedarikçi şablonu oluşturulmadı. Faturalar doğrulandıkça sistem otomatik olarak tedarikçi kalıplarını öğrenecektir."

**2.2 Template Detail Drawer/Modal**

When clicking "Detay" on a template row, open a side drawer (Sheet component from Shadcn) showing:

**Header Section:**
- Supplier name (large)
- Supplier tax number
- Active/Inactive badge
- Sample count: "X faturadan öğrenildi"

**Default Settings Section (Editable):**
- Default Category: Dropdown to select/change category (from GET /api/v1/categories)
- Default Currency: Display only (not editable — learned automatically)
- Save button for default category changes

**Field Accuracy Section:**
Display the field_accuracy data from learned_data as a visual grid:

For each field (invoice_number, invoice_date, due_date, supplier_name, total_amount, etc.):
- Field name (Turkish label)
- Accuracy bar: (correct_count / total_count * 100)% — green bar
- Text: "X / Y doğru" (X out of Y correct)

This gives users insight into which fields the LLM typically gets right for this supplier.

**Common Corrections Section:**
If common_corrections data exists, show a small table:
- Alan (Field)
- Orijinal Değer (Original Value)
- Düzeltilen Değer (Corrected To)
- Sıklık (Frequency)

**Amount Range Section:**
- Typical amount range: "₺{min} - ₺{max}" with average shown
- Typical line item count

**Danger Zone:**
- "Öğrenilen Verileri Sıfırla" (Reset Learned Data) button — red, with confirmation dialog
  - Confirmation: "Bu tedarikçi için öğrenilen tüm veriler sıfırlanacaktır. Bu işlem geri alınamaz. Devam etmek istiyor musunuz?"
  - Calls POST /api/v1/templates/{id}/reset
- "Şablonu Devre Dışı Bırak" (Deactivate Template) button

---

### 3. Automation Rules Page (/rules)

**3.1 Rules List View**

Page header: "Otomasyon Kuralları" with a subtitle "Fatura işleme süreçlerini otomatikleştirmek için kurallar tanımlayın"

**Action Bar:**
- "Yeni Kural Oluştur" (Create New Rule) button — primary button, opens rule builder
- Filter dropdown: trigger point filter (Tümü, Çıkarım Sonrası, Doğrulama Sonrası, Manuel Oluşturma)
- Active/Inactive toggle filter
- Search bar

**Rules List:**
Display as cards (not table) — rules have complex data that works better as cards.

Each rule card shows:
- **Header**: Rule name + priority badge ("Öncelik: X")
- **Status**: Active/Inactive toggle switch (calls POST /toggle)
- **Trigger**: Badge showing trigger_point in Turkish:
  - AFTER_EXTRACTION → "Çıkarım Sonrası"
  - AFTER_VERIFICATION → "Doğrulama Sonrası"
  - ON_MANUAL_CREATE → "Manuel Oluşturma"
- **Conditions Summary**: Natural language summary of conditions. Example: "Tedarikçi adı 'ABC Ltd' VE Toplam tutar > ₺50.000"
- **Actions Summary**: Natural language summary of actions. Example: "Kategori → Ofis Malzemeleri, Bildirim Gönder → Yöneticiler"
- **Stats**: "X kez çalıştı • Son: 2 gün önce" (execution count + last executed)
- **Action Buttons**: Edit, Test, History, Delete

Cards should be sortable by priority (drag-and-drop is optional — a simple priority number input is sufficient).

**Empty State:**
"Henüz otomasyon kuralı tanımlanmadı. Fatura işleme süreçlerinizi otomatikleştirmek için ilk kuralınızı oluşturun."
With a prominent "Yeni Kural Oluştur" button.

**3.2 Rule Builder (Create/Edit)**

Open as a full-page modal or dedicated page (/rules/new, /rules/{id}/edit).

**Step 1: Basic Info**
- Kural Adı (Rule Name) — text input, required
- Açıklama (Description) — textarea, optional
- Tetikleme Noktası (Trigger Point) — select dropdown with Turkish labels
- Öncelik (Priority) — number input (1-1000), with helper text: "Düşük sayı = yüksek öncelik"

**Step 2: Conditions (Koşullar)**

Header: "Koşullar" with a logic toggle: "Tüm koşullar sağlanmalı (VE)" / "Herhangi bir koşul yeterli (VEYA)"

Condition rows — each row has:
- **Field Selector**: Dropdown with Turkish labels for all available fields:
  - Tedarikçi Adı (supplier_name)
  - Tedarikçi Vergi No (supplier_tax_number)
  - Toplam Tutar (total_amount)
  - Ara Toplam (subtotal)
  - KDV Tutarı (tax_amount)
  - Para Birimi (currency)
  - Kaynak Tipi (source_type)
  - Güven Skoru (confidence_score)
  - Kategori (category_name)
  - Durum (status)
  - LLM Sağlayıcı (llm_provider)
  - Fatura Tarihi (invoice_date)

- **Operator Selector**: Dropdown that changes based on selected field type:
  - For string fields (supplier_name, supplier_tax_number, category_name, etc.): equals, not_equals, contains, not_contains, in, not_in, is_null, is_not_null
  - For numeric fields (total_amount, subtotal, tax_amount, confidence_score): equals, not_equals, greater_than, less_than, greater_than_or_equal, less_than_or_equal, between, is_null, is_not_null
  - For enum fields (currency, source_type, status, llm_provider): equals, not_equals, in, not_in
  - For date fields (invoice_date): equals, greater_than, less_than, between

  Display operators in Turkish:
  - equals → "eşittir"
  - not_equals → "eşit değildir"
  - contains → "içerir"
  - not_contains → "içermez"
  - greater_than → "büyüktür"
  - less_than → "küçüktür"
  - greater_than_or_equal → "büyük veya eşittir"
  - less_than_or_equal → "küçük veya eşittir"
  - in → "şunlardan biri"
  - not_in → "şunlardan biri değil"
  - is_null → "boş"
  - is_not_null → "boş değil"
  - between → "arasında"

- **Value Input**: Changes based on operator:
  - For equals/not_equals/contains/not_contains with strings: text input
  - For numeric operators: number input
  - For in/not_in: multi-value tag input
  - For between: two inputs (min and max) side by side
  - For is_null/is_not_null: no value input needed (hide it)
  - For enum fields: dropdown with predefined values:
    - currency: TRY, USD, EUR
    - source_type: LLM, E_INVOICE, MANUAL
    - status: PENDING, VERIFIED, REJECTED, PROCESSING
    - llm_provider: GEMINI, GPT, CLAUDE

- **Remove Button**: X icon to delete the condition row
- **"Koşul Ekle" (Add Condition) Button**: Adds a new empty condition row

Minimum 1 condition required. Show validation error if no conditions are defined.

**Step 3: Actions (Aksiyonlar)**

Action rows — each row has:
- **Action Type Selector**: Dropdown with Turkish labels:
  - Kategori Ata (SET_CATEGORY)
  - Durum Değiştir (SET_STATUS)
  - Not Ekle (ADD_NOTE)
  - İnceleme İçin İşaretle (FLAG_FOR_REVIEW)
  - Bildirim Gönder (SEND_NOTIFICATION)
  - Öncelik Belirle (SET_PRIORITY)

- **Action Parameters**: Changes based on action type:
  - SET_CATEGORY: Category dropdown (from GET /api/v1/categories)
  - SET_STATUS: Status dropdown (only VERIFIED and REJECTED)
  - ADD_NOTE: Text input for the note content
  - FLAG_FOR_REVIEW: Text input for the reason
  - SEND_NOTIFICATION: Text input for message + multi-select for target roles (ADMIN, MANAGER, ACCOUNTANT)
  - SET_PRIORITY: Select dropdown (HIGH, MEDIUM, LOW)

- **Remove Button**: X icon to delete the action row
- **"Aksiyon Ekle" (Add Action) Button**: Adds a new empty action row

Minimum 1 action required.

**Step 4: Review & Save**

Show a summary of the rule in natural language:

"EĞER [condition1] VE [condition2] İSE → [action1], [action2]"

Example: "EĞER Tedarikçi adı 'ABC Ltd' eşittir VE Toplam tutar 50000 büyüktür İSE → Kategori: Ofis Malzemeleri olarak ata, Yöneticilere bildirim gönder"

Buttons:
- "Kaydet" (Save) — primary
- "Kaydet ve Test Et" (Save and Test) — secondary, saves then opens the test dialog
- "İptal" (Cancel)

**3.3 Rule Test Dialog**

Triggered by the "Test" button on a rule card or "Kaydet ve Test Et" in the builder.

**Invoice Selection:**
- Show a searchable dropdown/combobox to select an invoice to test against
- Search by invoice number or supplier name
- Show: invoice_number, supplier_name, total_amount, status in the dropdown items

**Test Results Display:**
After calling POST /api/v1/rules/{id}/test with { invoiceId }:

- **Overall Result**: Large badge — "EŞLEŞME" (green) or "EŞLEŞME YOK" (gray)
- **Conditions Table**:
  - Koşul (Condition description in Turkish)
  - Gerçek Değer (Actual Value from the invoice)
  - Sonuç (Result): ✅ or ❌ icon
- **Actions Preview** (only shown if all conditions matched):
  - List of actions that would be applied, in Turkish
  - Note: "Bu bir test çalıştırmasıdır. Hiçbir değişiklik yapılmamıştır."

**3.4 Rule Execution History**

Accessible from the "Geçmiş" (History) button on a rule card.

Opens a drawer or modal showing a paginated table:
- Fatura (Invoice) — invoice number, clickable to navigate to invoice detail
- Tetikleme (Trigger) — trigger point badge
- Uygulanan Aksiyonlar (Applied Actions) — summary
- Sonuç (Result) — SUCCESS (green) or FAILED (red) badge
- Hata (Error) — shown only if FAILED, error message
- Tarih (Date) — when executed

**3.5 Global Execution Log Page (/rules/log)**

Accessible via a tab or link on the rules page: "Çalıştırma Geçmişi"

Shows ALL rule executions across all rules, paginated:
- Kural Adı (Rule Name)
- Fatura (Invoice number)
- Tetikleme Noktası (Trigger point)
- Uygulanan Aksiyonlar (Actions applied)
- Sonuç (Result)
- Tarih (Date)

With filters:
- Date range
- Rule name search
- Result (SUCCESS/FAILED)

---

### 4. Template Suggestions in Verification UI

Enhance the existing Verification UI (Phase 22) with template suggestions:

When viewing an invoice in the verification screen, if a supplier template exists for the invoice's supplier:

- Show a small info banner at the top: "Bu tedarikçiden {sampleCount} fatura öğrenildi" with a link to the template detail
- If the template has a default_category and the invoice has no category assigned: show a suggestion chip: "Önerilen Kategori: {categoryName}" with a "Uygula" (Apply) button
- If any fields have low accuracy in the template (< 70% correct): highlight those fields with a small warning icon and tooltip: "Bu alan bu tedarikçi için sıklıkla düzeltiliyor"

This is a lightweight enhancement — do not overhaul the verification UI, just add the suggestion elements.

---

### 5. Rule-Applied Indicator on Invoice Detail

On the invoice detail page (/invoices/{id}), if rules were applied to this invoice:

- Show a small section "Uygulanan Kurallar" (Applied Rules) with:
  - Rule name
  - When applied
  - What actions were taken
- Data source: GET /api/v1/rules/execution-log?invoiceId={id} (or a new endpoint if needed)

This is also a lightweight addition to the existing invoice detail page.

---

### 6. Technical Implementation Notes

**State Management:**
- Use TanStack Query for all API calls (templates list, rules list, execution logs)
- Use Zustand or React state for the rule builder form (complex nested state)
- URL-based state for filters on the templates and rules list pages (consistent with Phase 23)

**Form Handling:**
- Use React Hook Form for the rule builder form with Zod validation
- Dynamic form fields for conditions and actions (useFieldArray from React Hook Form)

**All UI Text in Turkish:**
All labels, buttons, messages, placeholders, tooltips, and error messages must be in Turkish.

**Dark Mode:**
All new components must support the existing dark/light mode toggle.

**Responsive Design:**
- Desktop: Full layout with side-by-side elements
- Tablet: Stacked layout where needed
- Mobile: Full-width cards, collapsible sections

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_30.1_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Pages and routes created
3. Components created (with file paths)
4. API integrations (which endpoints are called, from where)
5. State management approach
6. Dark mode and responsive design status
7. Test results (component tests if written)
8. Database changes: Confirm "No database changes needed for this phase" (frontend only)
9. Issues encountered and solutions
10. UX decisions made during implementation
11. Screenshots or descriptions of key screens
12. Next steps (Phase 31 requirements, any improvements identified)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 10**: App layout, sidebar navigation, Shadcn/ui setup, dark/light mode
- **Phase 11**: Auth store, Axios interceptor, protected routes
- **Phase 12**: Invoice list table, category management (for category dropdown in rule actions)
- **Phase 22**: Verification UI (for template suggestions enhancement)
- **Phase 30-A**: All backend REST APIs for templates and rules

### Required By
- **Phase 37**: Frontend E2E Tests — rule builder and template pages should be covered

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Sidebar shows "Otomasyon" group with "Şablonlar" and "Kurallar" sub-items
- [ ] RBAC: sidebar items hidden for INTERN users
- [ ] Templates list page loads with pagination
- [ ] Templates list: search by supplier name works
- [ ] Templates list: filter by active/inactive works
- [ ] Template detail drawer opens and shows field accuracy stats
- [ ] Template detail: common corrections table renders
- [ ] Template detail: amount range visualization renders
- [ ] Template default category edit and save works
- [ ] Template deactivate with confirmation dialog works
- [ ] Template reset learned data with confirmation dialog works
- [ ] Rules list page loads and shows rules as cards
- [ ] Rule card shows condition/action summary in Turkish natural language
- [ ] Create new rule: rule builder dialog opens
- [ ] Rule builder: condition field selector shows correct operators per field type
- [ ] Rule builder: string fields do not show GREATER_THAN operator
- [ ] Rule builder: numeric fields do not show CONTAINS operator
- [ ] Rule builder: action parameter inputs change based on action type
- [ ] Rule builder: validation requires at least 1 condition and 1 action
- [ ] Rule builder: save creates rule and shows in list
- [ ] Edit rule: pre-fills existing conditions and actions
- [ ] Delete rule: confirmation dialog works
- [ ] Toggle rule active/inactive works
- [ ] Rule test (dry run): select invoice dialog works
- [ ] Rule test: shows evaluation results without modifying data
- [ ] Rule execution history: viewable per rule
- [ ] Rule execution history: global view works
- [ ] Verification UI: template suggestions banner shows for known suppliers
- [ ] Invoice detail: applied rules section shows
- [ ] All text is in Turkish
- [ ] Dark mode works for all new components
- [ ] Responsive design works on mobile, tablet, desktop
- [ ] `npm run build` completes without errors
- [ ] `npm run lint` passes without errors
- [ ] Result file created at docs/FURKAN/step_results/faz_30.1_result.md

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Sidebar has new "Otomasyon" group with "Şablonlar" and "Kurallar" sub-items
2. ✅ RBAC: menu items hidden for INTERN users
3. ✅ Templates list page shows all supplier templates with pagination, search, and filtering
4. ✅ Template detail drawer shows field accuracy, common corrections, and amount range visualizations
5. ✅ Template default category can be edited and saved
6. ✅ Template can be deactivated and reset (with confirmation dialogs)
7. ✅ Rules list page shows all rules as cards with condition/action summaries in Turkish
8. ✅ Rule builder allows creating new rules with dynamic conditions and actions
9. ✅ Condition field selector shows correct operators based on field type
10. ✅ Action parameter inputs change based on action type
11. ✅ Rule validation works (at least 1 condition, at least 1 action, required fields)
12. ✅ Rule can be edited, deleted, and toggled active/inactive
13. ✅ Rule test (dry run) dialog works — select invoice, see evaluation results
14. ✅ Rule execution history is viewable per rule and globally
15. ✅ Verification UI shows template suggestions when available
16. ✅ Invoice detail page shows applied rules
17. ✅ All text is in Turkish
18. ✅ Dark mode works for all new components
19. ✅ Responsive design works on mobile, tablet, and desktop
20. ✅ Result file is created at docs/FURKAN/step_results/faz_30.1_result.md

---

## IMPORTANT NOTES

1. **Do NOT Modify Backend Code**: This phase is frontend-only. All required endpoints are provided by Phase 30-A. If you discover a missing endpoint or parameter, document it in the result file and coordinate with Ömer.

2. **Rule Builder UX is Critical**: The rule builder is the most complex UI in this phase. Focus on making it intuitive — users who are not technical should be able to define simple rules like "If supplier is X, set category to Y" without confusion. Use clear Turkish labels and helpful placeholders.

3. **Condition-Operator Mapping**: Not all operators make sense for all fields. A string field should not show "greater_than". A numeric field should not show "contains". Implement proper operator filtering based on the selected field type.

4. **Natural Language Summaries**: Rules should be displayed as human-readable Turkish sentences wherever possible. Instead of showing raw JSON, convert conditions and actions to natural language: "EĞER Tedarikçi adı 'ABC Ltd' eşittir İSE → Kategori: Ofis Malzemeleri olarak ata"

5. **Performance**: The rules and templates lists should use server-side pagination. Do not fetch all rules/templates at once.

6. **Template Suggestions are Non-Intrusive**: The template suggestions in the verification UI should be helpful but not annoying. A small info banner and optional suggestion chips — not popups or mandatory steps.
