# PHASE 29-F: FRONTEND — VERSION HISTORY & DIFF UI

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid
  - **Spring Boot Backend**: Port 8080
  - **Next.js Frontend**: Port 3000

### Current State (Phases 0-28 Completed)
- ✅ Phase 0-12: Infrastructure, backend core, frontend core
- ✅ Phase 13-22: Full extraction pipeline, upload, verification UI
- ✅ Phase 23-28: Filtering, export, dashboard, notifications (WebSocket, email, push)
- ✅ Phase 29 Backend (ÖMER): Version history API — invoice_versions table (JSONB snapshots), automatic version creation on every update, 5 REST endpoints

### What Phase 29 Backend Delivers (API for This Phase)

| Method | Endpoint | Description |
|---|---|---|
| GET | /api/v1/invoices/{id}/versions | Version list: id, versionNumber, changeSource, changeSummary, changedFields, changedBy (name/email), createdAt |
| GET | /api/v1/invoices/{id}/versions/{versionId} | Full snapshot: snapshotData (all fields), itemsSnapshot (items array) |
| GET | /api/v1/invoices/{id}/versions/diff?from=N&to=M | Field-level diff: changes[] with fieldName, fieldLabel (Turkish), oldValue, newValue, changeType; itemChanges with added/removed/modified |
| POST | /api/v1/invoices/{id}/revert/{versionNumber} | Revert to target version (ADMIN/MANAGER only), returns updated invoice |
| GET | /api/v1/invoices/{id}/versions/latest | Latest version number |

**changeSource values:** MANUAL_EDIT, LLM_EXTRACTION, LLM_RE_EXTRACTION, VERIFICATION, STATUS_CHANGE, REVERT, BULK_UPDATE

**Diff response field labels are in Turkish** (e.g., "Tedarikçi Adı", "Genel Toplam").

### Phase Assignment
- **Assigned To**: FURKAN (Frontend Developer)
- **Estimated Duration**: 1.5 days

### Frontend Tech Stack
- Next.js 14+ (App Router), React 19, TypeScript, Tailwind CSS, Shadcn/ui, TanStack Query, Zustand, Axios

---

## OBJECTIVE

Build the frontend UI for viewing invoice version history, comparing two versions side-by-side with highlighted differences, and reverting to a previous version. This consists of: a version timeline component on the invoice detail page, a diff viewer that shows field-level changes with visual highlighting, and a revert confirmation dialog.

---

## DETAILED REQUIREMENTS

### 1. Version History Tab on Invoice Detail Page

**Location:** Add a "Geçmiş" (History) tab to the invoice detail page (from Phase 12) or to the verification page (Phase 22).

**Approach:** Add a tab alongside existing content. For instance, if the invoice detail page has tabs or sections, add a "Versiyon Geçmişi" section. Alternatively, make it a side panel or a dedicated sub-route: `/invoices/{id}/history`.

**Recommended:** Add as a tab on the invoice detail page:
- Tab 1: "Detay" (existing invoice detail)
- Tab 2: "Doğrulama" (link to verification, Phase 22)
- Tab 3: "Geçmiş" (version history — this phase)

### 2. Version Timeline Component

A vertical timeline showing all versions of the invoice, newest first.

**Per version entry:**

| Element | Description |
|---|---|
| Version badge | "v{number}" in a circle (e.g., "v5") |
| Timestamp | Relative time + absolute on hover ("2 saat önce — 11.02.2026 14:30") |
| Change source icon + label | Icon based on source type (see mapping below) |
| Change summary | Human-readable text (e.g., "Tedarikçi adı ve tutar güncellendi") |
| Changed fields tags | Small tag chips showing which fields changed (e.g., "Tedarikçi Adı", "Toplam") |
| Changed by | User name/avatar |
| Action buttons | "Karşılaştır" (Compare), "Geri Al" (Revert — only for ADMIN/MANAGER) |

**Change Source Icon Mapping:**

| changeSource | Icon | Label (Turkish) | Color |
|---|---|---|---|
| MANUAL_EDIT | Pencil | "Manuel Düzenleme" | blue |
| LLM_EXTRACTION | Bot/Sparkles | "LLM Çıkarımı" | purple |
| LLM_RE_EXTRACTION | RefreshCw | "Yeniden Çıkarım" | purple |
| VERIFICATION | CheckCircle | "Doğrulama" | green |
| STATUS_CHANGE | ArrowRightLeft | "Durum Değişikliği" | yellow |
| REVERT | Undo | "Geri Alma" | orange |
| BULK_UPDATE | Layers | "Toplu Güncelleme" | gray |

**Current version indicator:** The latest version (top of timeline) is marked as "Güncel Versiyon" with a distinct styling.

**Data fetching:** Use TanStack Query: `useQuery(['invoiceVersions', invoiceId], fetchVersions)`. The version list endpoint returns summaries without heavy snapshot data.

### 3. Version Comparison (Diff Viewer)

When the user clicks "Karşılaştır" on a version, or selects two versions to compare:

**Selection UI:**
- Option A: Click "Karşılaştır" on a version → compares that version with the CURRENT state
- Option B: Two dropdown selectors: "Eski Versiyon" and "Yeni Versiyon" for comparing any two versions
- Implement BOTH: default is compare-with-current, with option to select custom range

**Diff Display — Split View:**
Side-by-side comparison showing old values (left) and new values (right) for each changed field.

**Layout:**

| Left Column (Eski - v{from}) | Right Column (Yeni - v{to}) |
|---|---|
| Field Label | Field Label |
| Old Value (red background/strikethrough) | New Value (green background/highlight) |

**Field-Level Diff Display:**
For each change in the diff response:
- **MODIFIED**: Show old value with red background on left, new value with green background on right
- **ADDED**: Empty on left, green highlighted value on right
- **REMOVED**: Red strikethrough value on left, empty on right
- Unchanged fields: show in normal style (or hide them with a "Tüm alanları göster" toggle)

**Styling:**
- Changed values: distinct background color (light red for old → light green for new)
- Use Shadcn/ui Card or Table for layout
- Responsive: on mobile, stack vertically (old above, new below) instead of side-by-side

**Value Formatting in Diff:**
Display values with appropriate formatting based on field type:
- **Amount fields** (subtotal, tax_amount, total_amount, line_total, unit_price): Format as Turkish currency with the invoice's currency symbol (e.g., "₺1.234,56" or "$1,234.56"). Show both old and new values in the same format so the user can easily spot the difference.
- **Date fields** (invoice_date, due_date): Format as DD.MM.YYYY (Turkish format)
- **Status fields**: Show Turkish-translated status labels (BEKLEMEDE, DOĞRULANMIŞ, REDDEDİLMİŞ) instead of enum values
- **Category fields**: Show category name, not category ID
- **Tax rate**: Show as percentage (e.g., "%18" not "0.18")
- **Null to value changes**: Show "—" (em dash) for null values, not "null" text
- **Long text fields** (notes, address): Truncate to 100 chars in the diff summary, with a "Tümünü göster" (Show all) expand button

This ensures the diff viewer is readable by accountants who are not technical users.

**Items Diff:**
Below the field-level diff, show a table for invoice items changes:
- Added items: highlighted in green
- Removed items: highlighted in red with strikethrough
- Modified items: show changed fields highlighted within the row

### 4. Version Detail View

When clicking on a specific version in the timeline:
- Fetch GET /versions/{versionId} to get full snapshot data
- Display all fields in a read-only card layout (same layout as invoice detail but non-editable)
- Show items in a read-only table
- Show metadata: version number, who changed, when, change source, change summary
- "Geri Al" (Revert) button at the top (only for ADMIN/MANAGER)
- "Karşılaştır" button to compare with current

### 5. Revert Confirmation Dialog

When user clicks "Geri Al" (Revert):

**Show AlertDialog:**
- Title: "Versiyona Geri Dön"
- Body: "Fatura verilerini v{versionNumber} ({createdAt}) versiyonuna geri döndürmek istediğinizden emin misiniz? Bu işlem geri alınabilir (mevcut durum yeni bir versiyon olarak kaydedilir)."
- Additional info: Show a brief summary of what will change (diff between current and target version)
- Buttons: "İptal" (Cancel) and "Geri Al" (Confirm Revert)

**On confirm:**
- Call POST /api/v1/invoices/{id}/revert/{versionNumber}
- Show success toast: "Fatura v{versionNumber} versiyonuna geri döndürüldü"
- Refresh the version list (invalidate TanStack Query)
- Refresh the invoice detail (invalidate invoice query)

**RBAC:** Only show the "Geri Al" button for users with ADMIN or MANAGER role. Use the auth store from Phase 11 to check the user's role.

### 6. Compare Mode Selector

Above the diff viewer, provide version selectors:

- "Eski:" dropdown with all version numbers (default: the version being compared)
- "Yeni:" dropdown with all version numbers (default: latest/current)
- "Karşılaştır" button to trigger the diff fetch
- Swap button (↔) to swap the from/to versions

Data fetching: `useQuery(['versionDiff', invoiceId, fromVersion, toVersion], fetchDiff, { enabled: !!fromVersion && !!toVersion })`

### 7. Empty State and Edge Cases

- **No versions yet**: "Bu fatura için henüz versiyon geçmişi bulunmuyor." (Shouldn't normally happen since version 1 is created on invoice creation)
- **Only 1 version**: Show the single version, disable comparison
- **Loading**: Skeleton loaders for timeline and diff viewer
- **Error**: Error message with retry button
- **Many fields changed**: When a version has more than 10 changed fields (common for LLM_EXTRACTION or REVERT operations), show a collapsed summary: "15 alan değiştirildi" with an "Expand" button to see all changes. Show the top 5 most significant changes by default (prioritize: total_amount, invoice_number, supplier_name, invoice_date, status).
- **Rapid successive versions**: If multiple versions were created within a short time (e.g., less than 5 seconds apart — common during LLM extraction → auto-rule execution), group them visually in the timeline with a subtle connector and a label: "Otomatik işlemler" (Automatic operations).
- **Large items diff**: When invoice items diff involves more than 20 rows (additions + removals + modifications), paginate the items diff section (show 10 per page) to avoid performance issues with large invoices.



### 8. Integration with Existing Pages

**Invoice Detail Page (Phase 12):**
Add "Geçmiş" tab or button linking to version history

**Verification Page (Phase 22):**
After saving changes on verification, the version list auto-updates (TanStack Query cache invalidation)

**Notification Click (Phase 27-F):**
When a notification about an invoice is clicked, navigate to invoice detail → the user can then access history

---
### 9. Keyboard Navigation and Accessibility

- **Timeline navigation**: Up/Down arrow keys navigate between versions in the timeline
- **Enter**: Opens the selected version's detail view or triggers comparison
- **Escape**: Closes the version detail view or comparison panel
- **Tab order**: Timeline → Compare selectors → Diff content → Revert button
- **Screen reader**: ARIA labels on timeline entries: "Versiyon {N}, {changeSource}, {changeSummary}, {date}"
- **Diff view ARIA**: Changed fields announced with role, e.g., "Değiştirildi: Tedarikçi Adı, eski değer: ABC, yeni değer: XYZ"

---

## TESTING REQUIREMENTS

### 1. Component Tests
- Version timeline renders with correct version entries
- Change source icons and labels render correctly
- Diff viewer shows modified fields with correct highlighting
- Added fields shown in green, removed in red
- Items diff table renders correctly
- Revert button only visible for ADMIN/MANAGER
- Revert dialog shows correct version info
- Compare mode selectors work
- Empty state renders

### 2. Integration Tests (mocked API)
- Fetch version list → timeline populated
- Click "Karşılaştır" → diff fetched and displayed
- Click "Geri Al" → confirm dialog → API call → success toast → list refreshed
- Select custom from/to versions → correct diff displayed
- Revert by non-admin → button not shown

---

## VERIFICATION CHECKLIST

### Version Timeline
- [ ] Timeline displays all versions newest first
- [ ] Version badge (v1, v2, ...) rendered
- [ ] Change source icon and Turkish label correct
- [ ] Change summary displayed
- [ ] Changed fields shown as tags
- [ ] User name who made the change shown
- [ ] Relative timestamps in Turkish
- [ ] Current version marked as "Güncel Versiyon"

### Diff Viewer
- [ ] Side-by-side layout on desktop
- [ ] Stacked layout on mobile
- [ ] Modified fields: red old value, green new value
- [ ] Added fields: green only
- [ ] Removed fields: red strikethrough
- [ ] Turkish field labels used
- [ ] Items diff shows added/removed/modified rows
- [ ] "Tüm alanları göster" toggle works
- [ ] Custom from/to version selectors work

### Revert
- [ ] "Geri Al" button only for ADMIN/MANAGER
- [ ] Confirmation dialog with version info
- [ ] API call on confirm
- [ ] Success toast shown
- [ ] Version list and invoice detail refreshed
- [ ] Error handling on revert failure

### Integration
- [ ] "Geçmiş" tab/button on invoice detail page
- [ ] Data fetching with TanStack Query
- [ ] Loading skeletons
- [ ] Error states with retry

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/OMER/step_results/faz_29_f_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified
4. Component list (timeline, diff viewer, revert dialog, compare selector)
5. Screenshots: version timeline, diff viewer (side-by-side), revert dialog, mobile layout
6. TanStack Query configuration (queries, cache invalidation)
7. RBAC integration for revert button
8. Test results
9. Issues and solutions
10. Next steps

---

## DEPENDENCIES

### Requires
- **Phase 12**: Invoice detail page (tab integration)
- **Phase 22**: Verification UI (version created on save)
- **Phase 29 Backend (ÖMER)**: Version history API endpoints

### Required By
- Nothing directly, but improves overall UX and audit capabilities

---

## SUCCESS CRITERIA

1. ✅ Version timeline displays all versions with source icons, summaries, timestamps
2. ✅ Diff viewer shows field-level changes with color highlighting (red/green)
3. ✅ Items diff shows added/removed/modified rows
4. ✅ Custom version comparison (select any two versions)
5. ✅ Revert dialog with confirmation, RBAC-gated (ADMIN/MANAGER)
6. ✅ Revert triggers API, shows toast, refreshes data
7. ✅ Turkish labels for all fields and UI text
8. ✅ Responsive design (side-by-side on desktop, stacked on mobile)
9. ✅ Integrated as tab on invoice detail page
10. ✅ All tests pass
11. ✅ Result file created
12. ✅ Amount and date values formatted correctly in diff viewer (Turkish formatting)
13. ✅ Many-field changes collapsed with expand option
14. ✅ Keyboard navigation works for timeline and diff viewer
---

## IMPORTANT NOTES

1. **No Backend Changes**: This phase is frontend-only. All API endpoints are ready from Phase 29 backend.

2. **Diff Colors**: Use subtle background colors, not harsh reds and greens. Light pastel shades work better for readability: e.g., `bg-red-50` for removed, `bg-green-50` for added, with `text-red-700` and `text-green-700` for text. Ensure they work in both light and dark mode.

3. **Performance**: The version list is lightweight (no snapshot data). Only fetch full snapshots when the user clicks a specific version or triggers a diff. Don't pre-fetch all snapshot data.

4. **Field Label Mapping**: The diff API returns `fieldLabel` in Turkish. Use it directly in the UI — no need for frontend mapping.

5. **Revert is Reversible**: Emphasize in the dialog that revert creates a new version. The user can always revert the revert if needed. This reduces anxiety about using the feature.

6. **Timeline Visual Design**: Consider using Shadcn/ui's existing patterns or a simple custom timeline with a vertical line and dots. Keep it clean — this is a professional accounting tool, not a social media feed.

7. **Value Formatting is Critical**: The diff viewer shows raw values from the backend's JSONB snapshots. These are unformatted (e.g., "1234.56" instead of "₺1.234,56"). The frontend MUST format these values before display. Use the same formatters that the invoice detail page uses (from Phase 12/22) to ensure consistency. A raw numeric diff is confusing to accountants — a formatted diff is immediately useful.
