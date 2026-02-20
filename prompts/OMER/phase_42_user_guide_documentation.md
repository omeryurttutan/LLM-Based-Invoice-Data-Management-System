# PHASE 42: USER GUIDE AND DEPLOYMENT DOCUMENTATION

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM & Frontend) & Ömer Talha Yurttutan (Backend & Infrastructure)
- **Architecture**: Hybrid Microservices — Spring Boot (8082), Python FastAPI (8001), Next.js (3001)
- **Infrastructure**: PostgreSQL 15, Redis 7, RabbitMQ 3, Nginx (reverse proxy), Docker

### Current State (Phases 0-41 Completed — THIS IS THE FINAL PHASE)
The entire application is feature-complete, tested, performance-optimized, deployed with staging/production environments, monitored with alerting, and has interactive API documentation (Swagger UI). This is the final phase — it produces the documentation deliverables that accompany the software.

### What the System Does (Summary for Documentation Context)
The system automates invoice processing for Turkish businesses:
1. Users upload invoice images (JPEG, PNG, PDF) or e-Invoice XML files
2. The system uses LLM AI (Gemini Flash as primary, GPT and Claude as fallbacks) to extract structured data from invoice images
3. Extracted data is validated with a confidence score (0-100)
4. Users review and verify the extracted data, correcting any mistakes
5. The system learns from corrections (supplier templates) and can apply automation rules
6. Verified invoices can be filtered, searched, exported (XLSX, CSV, and Turkish accounting formats: Logo, Mikro, Netsis, Luca)
7. Real-time notifications (in-app, email, push) keep users informed
8. A dashboard provides visual analytics (charts, trends, pending actions)
9. Full audit trail, version history, and KVKK (Turkish data privacy law) compliance

### User Roles
- **ADMIN**: Full system access — user management, system settings, KVKK, monitoring
- **MANAGER**: Invoice management, rule creation, export, audit logs (no user management)
- **ACCOUNTANT**: Invoice operations, verification, export (no rule/user management)
- **INTERN**: Read-only + basic invoice creation (no edit, delete, export, or admin)

### Phase Assignment
- **Assigned To**: ÖMER (Backend & Infrastructure Developer)
- **Estimated Duration**: 2 days
- **Branch**: `feature/omer/faz-42-documentation`

---

## OBJECTIVE

Create three categories of documentation:
1. **End-User Guide**: For accountants, managers, and admins who use the application daily
2. **Admin Guide**: For system administrators who manage users, configure rules, and monitor the system
3. **Operations Documentation**: Deployment runbook, rollback procedures, troubleshooting guide, and a comprehensive project README

All user-facing documentation should be written in **Turkish** (this is a Turkish university project for Turkish users). Technical/operations documentation should be in **English** (standard for DevOps).

---

## DETAILED REQUIREMENTS

### 1. End-User Guide (Turkish)

Create `docs/user-guide/kullanici-kilavuzu.md` — a comprehensive guide for end users.

**1.1 Introduction Section**

- What is the system and what problem does it solve
- Who is it for (accounting departments, finance teams)
- Browser requirements (Chrome, Firefox, Edge — latest versions)
- PWA installation instructions (how to add to home screen on mobile)
- Language settings (Turkish is default, English available)

**1.2 Getting Started**

- How to register a new account
  - Fill in: full name, email, password
  - Joining an existing company vs creating a new company
- How to log in
- Navigating the interface: sidebar menu items, header (notifications bell, user menu), dark/light mode toggle
- First-time setup recommendations

**1.3 Dashboard**

- Overview of the dashboard page
- Understanding summary cards (total invoices, total amount, pending count, verified count)
- Reading the charts:
  - Category distribution pie chart — what it shows, how to interact
  - Monthly trend line chart — tracking invoice volume over time
  - Top suppliers bar chart — identifying major vendors
- Pending actions list — what it means and what to do
- Date range filtering on the dashboard

**1.4 Invoice Management**

- **Viewing invoices**: How the invoice list table works (pagination, sorting by columns, status badges and their colors/meanings)
- **Manual invoice creation**: Step-by-step guide for adding an invoice manually (when to use this vs file upload)
- **Invoice detail view**: What information is shown, line items table
- **Editing an invoice**: Which fields can be edited, who can edit (role-based)
- **Deleting an invoice**: Soft delete concept (data is not permanently lost), who can delete
- **Invoice status workflow**: PENDING → VERIFIED / REJECTED → can be reopened
  - What each status means
  - Who can change status
  - What happens when an invoice is verified (template learning triggers, rules execute)

**1.5 File Upload and LLM Extraction**

- **Single file upload**: Step-by-step guide
  - Supported file types: JPEG, PNG, PDF, XML (e-Invoice)
  - Maximum file size: 10MB
  - What happens after upload (processing indicator, notification when done)
- **Bulk upload**: Uploading multiple files at once (up to 20)
- **Understanding extraction results**:
  - The split-view: original image on left, extracted data on right
  - Confidence score explanation: green (high) / yellow (medium) / red (low)
  - LLM provider badge (which AI model processed the invoice)
- **Verifying extracted data**:
  - Reviewing each field
  - Correcting mistakes (inline editing)
  - Using Tab to navigate between fields, Enter to confirm
  - Verify button: confirms the data is correct
  - Reject button: marks the invoice as needing re-processing or manual entry
- **e-Invoice (XML) files**: These are processed without LLM (direct XML parsing), resulting in 95-100% confidence

**1.6 Filtering and Search**

- Using the filter panel: where it is, how to open/close it
- Available filters:
  - Date range (from/to date picker)
  - Status (PENDING, VERIFIED, REJECTED — multi-select)
  - Supplier name (text search, partial match)
  - Category (dropdown, multi-select)
  - Amount range (min/max slider)
  - Currency (TRY, USD, EUR)
  - Source type (LLM, e-Invoice, Manual)
  - Confidence score range
- Combining filters (AND logic — all filters applied together)
- Clearing filters
- URL-based state: filters are saved in the URL (bookmarkable, shareable)
- Full-text search bar: search by invoice number or supplier name

**1.7 Export**

- How to export data: the export button, format selection dialog
- General formats:
  - XLSX (Excel): When to use, what it looks like
  - CSV: When to use (for importing into other systems)
- Accounting software formats:
  - Logo, Mikro, Netsis, Luca: When to use each (depends on which accounting software your company uses)
  - Note: Accounting formats only export VERIFIED invoices
- Including line items in export (the includeItems option)
- Exporting filtered data: apply filters first, then export (only filtered results are exported)

**1.8 Notifications**

- The notification bell icon in the header: what the red badge number means
- Notification dropdown: viewing recent notifications, clicking to navigate
- Mark as read / mark all as read
- Notification types: extraction completed, extraction failed, low confidence, batch completed, verification by another user
- Notification preferences (Settings page):
  - Choosing which events trigger notifications
  - Choosing channels: in-app, email, push
  - How to enable/disable push notifications in the browser

**1.9 Version History**

- Accessing version history from the invoice detail page
- The timeline view: seeing who changed what and when
- Comparing two versions (diff view): understanding the highlighted differences
- Reverting to a previous version: when to use, confirmation dialog, who can do this (ADMIN, MANAGER only)

**1.10 Automation Rules** (ADMIN and MANAGER only)

- What are automation rules: automatic actions triggered by invoice events
- Navigating to the rules page
- Creating a new rule:
  - Rule name and description
  - Choosing a trigger point: after extraction, after verification, on manual create
  - Defining conditions: field, operator, value (explain the most common examples)
  - Defining actions: set category, set status, add note, flag for review, send notification
  - Setting priority (lower number = runs first)
  - AND vs OR condition logic
- Testing a rule (dry run): how to test against an existing invoice before activating
- Enabling/disabling a rule
- Viewing rule execution history
- Common rule examples:
  - "If supplier is ABC Ltd, set category to Technology"
  - "If total amount > 50,000 TRY, flag for manager review"
  - "If confidence score < 70, keep as PENDING and send notification"

**1.11 Supplier Templates** (ADMIN and MANAGER only)

- What are supplier templates: the system learns patterns from verified invoices
- How templates are created automatically (after verifying invoices from the same supplier)
- Viewing the template list: sample count, accuracy, default category
- Editing a template: changing default category, deactivating
- Resetting a template: when the learned data is incorrect

**1.12 Categories**

- Managing invoice categories: viewing, creating, editing, deleting
- How categories are used in filtering and reporting
- Assigning categories to invoices (manual or via automation rules)

---

### 2. Admin Guide (Turkish)

Create `docs/user-guide/admin-kilavuzu.md` — a guide specifically for administrators.

**2.1 User Management**

- Viewing all users in the company
- Understanding roles: ADMIN, MANAGER, ACCOUNTANT, INTERN — what each can do (permissions table)
- Changing a user's role
- Deactivating a user account
- Note: There is no "create user from admin panel" — users self-register and join a company

**2.2 System Monitoring**

- Accessing the system status page (if admin dashboard exists) or using the API endpoint
- Understanding the health indicators: backend, extraction service, database, Redis, RabbitMQ
- What to do if a service is DOWN
- Viewing recent alerts
- Checking the performance metrics (response times, cache hit rates)

**2.3 Audit Log**

- Accessing the audit log page
- Understanding audit entries: who did what, when, to which entity
- Filtering audit logs by date, action type, user, entity
- Using audit logs for compliance and troubleshooting

**2.4 KVKK Compliance**

- What data is encrypted (TC Kimlik No, phone, address — personal data)
- Viewing the KVKK compliance report
- Processing a "right to be forgotten" request: anonymizing a user's data
- Consent tracking: viewing user consent records
- Data retention: how old data is automatically cleaned up

**2.5 Automation Rules (Admin Perspective)**

- Reviewing all active rules
- Monitoring rule execution logs
- Troubleshooting rules that are not working as expected
- Best practices for rule design

**2.6 Backup and Recovery**

- How database backups work (automatic daily backup)
- How to manually trigger a backup
- How to restore from a backup (link to operations documentation)
- Important: Restoring a backup overwrites current data — always verify before restoring

---

### 3. Deployment Runbook (English)

Create `docs/deployment/deployment-runbook.md` — a step-by-step operations guide.

Note: Phase 39 already created `docs/deployment/deployment-guide.md` with basic deployment steps. This runbook is more detailed and includes operational procedures.

**3.1 Prerequisites**

- Server requirements: minimum hardware specs (2 vCPU, 4GB RAM, 40GB disk for production)
- Software requirements: Docker 24+, Docker Compose v2+, Git
- Domain name and DNS configuration (if applicable)
- LLM API keys: How to obtain Gemini, OpenAI, and Anthropic API keys
- SMTP credentials: For email notifications

**3.2 Initial Deployment Procedure**

Step-by-step instructions:
1. Clone the repository
2. Create `.env.prod` from `.env.example` — fill in all required values
3. Generate SSL certificates (self-signed for testing, or Let's Encrypt for production)
4. Build production images
5. Start the stack
6. Verify all services are healthy
7. Create the first admin user (register via the UI)
8. Verify the full workflow: upload an invoice, check extraction, verify, export

**3.3 Updating the Application**

Step-by-step instructions for deploying a new version:
1. Pull latest code from Git
2. Review the changelog / migration notes
3. Build new production images
4. Stop the current stack gracefully
5. Start the new stack
6. Verify all services are healthy
7. Test critical workflows
8. If something is wrong: follow the rollback procedure

**3.4 Rollback Procedure**

Step-by-step instructions for reverting to a previous version:
1. Identify the previous working version (Git commit SHA or tag)
2. Stop the current stack
3. Checkout the previous version
4. Restore database from backup (if the new version included migrations that need to be reverted)
5. Build images for the previous version
6. Start the stack
7. Verify functionality
8. Document the rollback reason and notify the team

**3.5 Database Operations**

- Running a manual backup
- Restoring from a backup
- Connecting to the database (psql via Docker)
- Running a migration manually (if Flyway auto-migration failed)
- Checking migration status

**3.6 Log Management**

- Where logs are stored (log volume paths)
- How to view logs in real-time for each service
- How to search logs using common commands (grep, jq for JSON logs)
- How to search for a specific correlation ID across all services
- Log rotation configuration

**3.7 Common Maintenance Tasks**

- Restarting a single service without downtime to others
- Clearing Redis cache (when to do it, how)
- RabbitMQ queue management: checking queue depth, purging stuck messages
- Disk space management: cleaning old log files, old backups

---

### 4. Troubleshooting Guide (English)

Create `docs/deployment/troubleshooting.md` — a guide for diagnosing and fixing common problems.

**4.1 Service Won't Start**

| Symptom | Possible Cause | Solution |
|---|---|---|
| Backend fails to start | Database not ready | Check PostgreSQL health, wait for it to be healthy before backend starts |
| Backend fails to start | Migration error | Check Flyway migration logs, fix the SQL, re-run |
| Backend fails to start | Port already in use | Check if another process is using 8082 |
| Frontend fails to start | Build error | Check build logs, verify Node.js version |
| Extraction service fails | Missing Python dependencies | Rebuild the Docker image |
| Extraction service fails | LLM API keys not set | Check .env file for GEMINI_API_KEY, OPENAI_API_KEY, ANTHROPIC_API_KEY |
| Nginx fails | SSL certificate missing | Run the SSL certificate generation script |
| Nginx fails | Invalid nginx.conf | Test with `nginx -t` before starting |

**4.2 Extraction Problems**

| Symptom | Possible Cause | Solution |
|---|---|---|
| Extraction never completes | RabbitMQ not running | Check RabbitMQ health and logs |
| Extraction never completes | Python service crashed | Check extraction service logs and restart |
| Low confidence scores | Poor image quality | Advise users to upload clearer images |
| Low confidence scores | Unusual invoice format | System will learn from corrections over time |
| All LLM providers fail | API keys expired or quota exceeded | Check each provider's dashboard for quota/billing status |
| All LLM providers fail | Network issue | Verify internet connectivity from the extraction service container |

**4.3 Authentication Problems**

| Symptom | Possible Cause | Solution |
|---|---|---|
| Can't log in | Account locked | Wait 30 minutes for auto-unlock, or admin unlocks manually |
| Can't log in | Wrong credentials | Use the forgot password flow (if implemented) or contact admin |
| Token expired | Access token TTL passed | The frontend should auto-refresh; if not, log out and back in |
| 403 Forbidden | Insufficient role | Contact admin for role change |

**4.4 Performance Problems**

| Symptom | Possible Cause | Solution |
|---|---|---|
| Slow page loads | Redis not running | Check Redis health, restart if needed |
| Slow page loads | Missing database indexes | Run EXPLAIN ANALYZE on slow queries, check Phase 38 indexes |
| High memory usage | JVM heap too large | Check JVM memory settings in Dockerfile.prod |
| High memory usage | Docker container limits | Adjust memory limits in docker-compose.prod.yml |

**4.5 Notification Problems**

| Symptom | Possible Cause | Solution |
|---|---|---|
| No email notifications | SMTP credentials wrong | Check SMTP settings in .env |
| No push notifications | VAPID keys not configured | Check VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY |
| No in-app notifications | WebSocket connection failed | Check Nginx WebSocket proxy configuration |
| Duplicate notifications | Multiple service instances | Ensure only one backend instance processes events |

**4.6 Export Problems**

| Symptom | Possible Cause | Solution |
|---|---|---|
| Export times out | Too many records | Apply filters to reduce the dataset before exporting |
| Accounting format has wrong characters | Encoding issue | Check that Mikro uses Windows-1254, others use UTF-8 |
| Empty export file | No invoices match filters | Check filter criteria, verify VERIFIED status for accounting formats |

---

### 5. Project README (English)

Update the root `README.md` to be a professional project overview.

**5.1 Structure**

- **Project Title and Badges**: Build status (GitHub Actions), license
- **Description**: 3-4 sentences about what the project does
- **Architecture Diagram**: A text-based or linked diagram showing the three services, database, cache, message queue, and their interactions
- **Tech Stack**: Listed by service
  - Backend: Java 17, Spring Boot 3.2, PostgreSQL 15, Redis 7, RabbitMQ 3, Flyway
  - Frontend: Next.js 14+, React 19, TypeScript, Tailwind CSS, Shadcn/ui, TanStack Query, Zustand, Recharts
  - Extraction Service: Python 3.11, FastAPI, Pillow, google-generativeai, openai, anthropic
  - Infrastructure: Docker, Nginx, GitHub Actions
- **Features**: Bullet list of key features (LLM extraction, multi-provider fallback, confidence scoring, automation rules, accounting export, KVKK compliance, PWA, i18n, etc.)
- **Quick Start**: How to get the development environment running in 5 minutes
  1. Clone the repo
  2. Copy `.env.example` to `.env` and fill in API keys
  3. Run `docker compose up`
  4. Access the app at `http://localhost:3001`
- **Project Structure**: Brief overview of the directory structure
- **Development**: How to run tests, how to add new features, branch strategy
- **Deployment**: Link to the deployment guide
- **Documentation Links**: Links to user guide, admin guide, API docs (Swagger UI), architecture docs
- **Team**: Names and responsibilities
- **License**: MIT or appropriate
- **Acknowledgments**: University name, advisor name (if applicable)

---

### 6. Architecture Overview Document (English)

Create `docs/architecture/architecture-overview.md` — a high-level system design document.

**6.1 Content**

- **System Architecture**: Text description of the three-service architecture and why this approach was chosen
- **Data Flow**: How an invoice travels through the system (upload → RabbitMQ → extraction service → LLM → validation → confidence score → database → notification → UI)
- **Authentication Flow**: JWT-based auth with Redis token management, refresh token rotation
- **LLM Extraction Pipeline**: Primary (Gemini) → Fallback (GPT) → Fallback (Claude) → All-failed error
- **Technology Decisions**: Brief justification for each major technology choice (why Spring Boot? why FastAPI? why Next.js? why PostgreSQL?)
- **Database Schema Overview**: List of major tables and their relationships (not the full schema — just the conceptual overview)
- **Security Measures**: KVKK encryption, rate limiting, RBAC, CORS, security headers, SSL
- **Monitoring Architecture**: Actuator → metrics → alerts → email/Slack

This document is valuable for the graduation report and for the graduation committee to understand the system design.

---

## FILE STRUCTURE

After completing this phase, the following files should be created or modified:

```
fatura-ocr-system/
├── docs/
│   ├── user-guide/
│   │   ├── kullanici-kilavuzu.md            # NEW — End-user guide (Turkish)
│   │   └── admin-kilavuzu.md                # NEW — Admin guide (Turkish)
│   ├── deployment/
│   │   ├── deployment-runbook.md            # NEW — Detailed ops runbook (English)
│   │   └── troubleshooting.md               # NEW — Troubleshooting guide (English)
│   ├── architecture/
│   │   └── architecture-overview.md         # NEW — System design overview (English)
│   └── api/
│       └── (already exists from Phase 41)
├── README.md                                 # MODIFIED — Professional project README (English)
```

---

## DATABASE CHANGES

**None.** This phase is purely documentation — no code changes, no database schema changes, no migrations.

---

## TESTING REQUIREMENTS

This phase produces documentation, not code. However, the documentation itself should be verified:

### Content Verification

1. **Completeness Check**: Every feature of the application should be covered in at least one guide. Walk through the sidebar navigation and verify each page/feature has corresponding documentation.
2. **Accuracy Check**: Test 3-5 procedures from the user guide (e.g., upload a file, verify an invoice, create a rule) against the actual running application. Ensure the steps match reality.
3. **Link Verification**: All cross-references between documents should be valid. Check that links to other documentation files work.
4. **Screenshot Placeholders**: Where the guide would benefit from screenshots, add placeholder markers like `[Screenshot: Login Page]` — these can be filled in later with actual screenshots. OR if possible, take actual screenshots and embed them.

### Readability Check

1. The Turkish user guide should be understandable by a non-technical person (an accountant using the system for the first time)
2. The deployment runbook should be followable by a junior developer who has never seen the project before
3. The troubleshooting guide should cover the most likely problems a user or operator would encounter

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_42_result.md`

The result file must include:

1. Phase summary (what was created)
2. Files created (full list with paths)
3. Document statistics: word count or approximate page count per document
4. User guide coverage: list of features covered with checkmarks
5. Admin guide coverage: list of admin features covered
6. Deployment runbook sections summary
7. Troubleshooting entries count (how many problem/solution pairs)
8. README quality check: all required sections present
9. Architecture overview sections listed
10. Language verification: Turkish guides are in Turkish, English docs are in English
11. Content verification results (3-5 procedures tested against running app)
12. Known gaps: any features not covered or documentation that needs improvement
13. Graduation report relevance: which documents can be referenced in the graduation report
14. Issues encountered and solutions
15. Final project status summary (all 43 phases complete!)

---

## DEPENDENCIES

### Requires (must be completed first)
- **All phases (0-41)**: The entire application must be built to document it accurately
- **Phase 39**: Deployment infrastructure (for deployment runbook)
- **Phase 40**: Monitoring setup (for admin guide monitoring section)
- **Phase 41**: API documentation (referenced from README and admin guide)

### Required By
- **Nothing** — this is the final phase. After this, the project is complete.

---
## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] kullanici-kilavuzu.md created in Turkish
- [ ] User guide covers: system login and registration
- [ ] User guide covers: dashboard usage
- [ ] User guide covers: single and bulk invoice upload
- [ ] User guide covers: LLM extraction result verification
- [ ] User guide covers: invoice list, filtering, and search
- [ ] User guide covers: export (XLSX, CSV, accounting formats)
- [ ] User guide covers: notification settings
- [ ] User guide covers: version history and diff viewing
- [ ] User guide covers: rule and template management
- [ ] User guide covers: profile settings and language switch
- [ ] User guide covers: PWA installation (Android and iOS)
- [ ] admin-kilavuzu.md created in Turkish
- [ ] Admin guide covers: user management and role assignment
- [ ] Admin guide covers: company management
- [ ] Admin guide covers: system monitoring dashboard
- [ ] Admin guide covers: audit log viewing
- [ ] Admin guide covers: KVKK compliance (consent, forget, report)
- [ ] Admin guide covers: automation rules administration
- [ ] Admin guide covers: backup and restore procedures
- [ ] deployment-runbook.md created in English
- [ ] Runbook covers: initial deployment from scratch
- [ ] Runbook covers: update/upgrade procedure
- [ ] Runbook covers: rollback procedure
- [ ] Runbook covers: database operations (backup, restore, migration)
- [ ] Runbook covers: log management and search
- [ ] Runbook covers: routine maintenance tasks
- [ ] troubleshooting-guide.md created with ≥20 problem/solution pairs
- [ ] Troubleshooting covers: service won't start scenarios
- [ ] Troubleshooting covers: LLM extraction failures
- [ ] Troubleshooting covers: authentication issues
- [ ] Troubleshooting covers: performance issues
- [ ] README.md created with professional formatting
- [ ] README includes: project description, tech stack, quick start, features
- [ ] README includes: documentation links to all guides
- [ ] Architecture overview document created
- [ ] Cross-references between documents work (links tested)
- [ ] 3-5 procedures verified against running application
- [ ] No code changes made in this phase
- [ ] All documents properly formatted Markdown
- [ ] Result file created at docs/OMER/step_results/faz_42_result.md

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ End-user guide (kullanici-kilavuzu.md) covers all 12 sections from section 1 above
2. ✅ End-user guide is written in Turkish, in a non-technical, user-friendly tone
3. ✅ Admin guide (admin-kilavuzu.md) covers user management, monitoring, audit, KVKK, rules, and backup
4. ✅ Admin guide is written in Turkish
5. ✅ Deployment runbook covers initial deployment, updates, rollback, database ops, log management, and maintenance
6. ✅ Deployment runbook is detailed enough for a junior developer to follow
7. ✅ Troubleshooting guide has at least 20 problem/solution pairs covering all major areas
8. ✅ README.md is professional with project description, tech stack, quick start, features, and documentation links
9. ✅ Architecture overview document describes the system design, data flow, and key decisions
10. ✅ All documents are properly formatted Markdown with headers, tables, and lists
11. ✅ Cross-references between documents work (e.g., user guide links to admin guide for admin-only features)
12. ✅ 3-5 procedures from the user guide verified against the running application
13. ✅ No code changes in this phase (documentation only)
14. ✅ Result file created at docs/OMER/step_results/faz_42_result.md

---

## IMPORTANT NOTES

1. **This is the FINAL Phase**: After this phase, the project is complete (all 43 phases done). The documentation is the last deliverable. Take the time to make it high quality — it will be reviewed by the graduation committee.

2. **Turkish User Guides Must Be Natural Turkish**: Do not write the Turkish guides in a translated-from-English style. Write them as a native Turkish speaker would explain the system to a colleague. Use Turkish business terminology for accounting concepts (e.g., "tedarikçi" not "supplier", "fatura" not "invoice", "KDV" not "VAT").

3. **Do NOT Write Code**: This phase is 100% documentation. No Java, no Python, no TypeScript, no SQL. If you find yourself writing code, stop. The only changes to existing files should be the README.md update.

4. **Screenshots Are Optional but Valuable**: If you can take actual screenshots of the running application and include them in the guides, that significantly improves the documentation quality. If not feasible in this phase, add placeholder markers and note them in the result file.

5. **Graduation Report Reference**: The architecture overview document and the user guides will be valuable references for the graduation report. Write them with that dual purpose in mind — they should be good enough to include as appendices in the graduation thesis.

6. **Keep Troubleshooting Practical**: Do not invent problems. Focus on issues that actually occurred during development (check the result files from previous phases for "Issues Encountered" sections) and issues that are likely to occur during operation.

7. **README First Impression**: The README.md is the first thing anyone sees when they visit the project repository. It should be professional, concise, and make the project look impressive. Include the tech stack, a brief feature list, and a quick start guide. Do not make it too long — 200-300 lines is ideal.

8. **Coordinate with FURKAN**: FURKAN may have insights on the frontend user experience that should be reflected in the user guide. Also, the extraction pipeline section should accurately describe the LLM workflow that FURKAN built. Cross-check with FURKAN or review the Phase 13-19 result files.

9. **Phase 39 Deployment Guide Already Exists**: Phase 39 created `docs/deployment/deployment-guide.md`. The deployment runbook in this phase is MORE detailed and operational. You may merge them into a single document or keep them separate (the runbook extending the guide). Either approach is fine — just ensure there is no conflicting information.

10. **Celebrate!**: After this phase, the project is done. All 43 phases complete. 🎉
