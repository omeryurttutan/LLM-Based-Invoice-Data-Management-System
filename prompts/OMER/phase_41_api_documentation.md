# PHASE 41: API DOCUMENTATION (SPRINGDOC OPENAPI / SWAGGER UI)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM & Frontend) & Ömer Talha Yurttutan (Backend & Infrastructure)
- **Architecture**: Hybrid Microservices — Spring Boot (8080), Python FastAPI (8000), Next.js (3000)

### Current State (Phases 0-40 Completed)
The entire application is feature-complete, tested, performance-optimized, deployed to staging/production environments, and has a monitoring/alerting system in place. The remaining gap is formal API documentation. While individual phase prompts contained endpoint descriptions, there is no interactive, browseable API documentation accessible to developers or for the graduation report.

### What Already Exists
- **Phase 0**: `springdoc-openapi-starter-webmvc-ui` dependency was added to pom.xml during project setup
- **Phase 2**: Standard API response wrappers (`ApiResponse<T>`, `ErrorResponse`) and `GlobalExceptionHandler` are established in the hexagonal architecture
- **Phase 7-9**: Invoice CRUD, Audit Log, and Duplication endpoints are implemented
- **Phase 4-5**: Authentication (JWT) and RBAC (4 roles) are implemented
- **Phase 13**: Python FastAPI service already has auto-generated Swagger UI at `/docs`
- **Phase 23-30**: Filtering, Export, Dashboard, Notifications, Version History, Templates, and Rules endpoints exist
- **Phase 31-32**: KVKK compliance and rate limiting endpoints exist
- **Phase 38-40**: Performance health, system status, and alert endpoints exist

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 1-2 days
- **Branch**: `feature/omer/faz-41-api-documentation`

---

## OBJECTIVE

Create comprehensive, interactive API documentation using SpringDoc OpenAPI (Swagger UI) for the Spring Boot backend, and ensure the Python FastAPI documentation is polished. The documentation should be a professional reference for:
1. The graduation committee reviewing the project
2. Developers (FURKAN and ÖMER) referencing the API during development
3. Future maintainers of the project

The Swagger UI should be accessible at `/api/docs` and present all endpoints with descriptions, request/response examples, authentication requirements, and error codes.

---

## DETAILED REQUIREMENTS

### 1. SpringDoc OpenAPI Global Configuration

**1.1 OpenAPI Info Configuration**

Create a configuration class that defines the API metadata:

- **Title**: "Fatura OCR ve Veri Yönetim Sistemi API"
- **Description**: A comprehensive description of the API (2-3 sentences in English explaining what the system does — invoice OCR processing, data management, LLM-based extraction)
- **Version**: "1.0.0"
- **Contact**: Team names and email (Muhammed Furkan Akdağ & Ömer Talha Yurttutan)
- **License**: MIT or appropriate license for the graduation project

**1.2 Security Scheme**

Configure the JWT Bearer authentication scheme globally:

- Scheme name: "Bearer Authentication"
- Type: HTTP
- Scheme: bearer
- Bearer format: JWT
- Description: "Enter your JWT access token obtained from POST /api/v1/auth/login. Format: Bearer {token}"

This allows Swagger UI users to click "Authorize", paste their JWT token, and test authenticated endpoints directly from the browser.

**1.3 Server URLs**

Define server URLs for different environments:
- Development: `http://localhost:8080`
- Staging: (the staging URL from Phase 39, or a placeholder)
- Production: (the production URL from Phase 39, or a placeholder)

**1.4 Swagger UI Path**

Configure the Swagger UI to be accessible at `/api/docs` (instead of the default `/swagger-ui.html`):
- `springdoc.swagger-ui.path=/api/docs`
- `springdoc.api-docs.path=/api/v1/api-docs` (the raw OpenAPI JSON/YAML spec)

**1.5 Group Configuration (API Tags)**

Organize endpoints into logical groups using OpenAPI tags. Each tag represents a section in the Swagger UI:

| Tag Name | Description | Endpoints |
|---|---|---|
| Authentication | User registration, login, token refresh, and logout | /api/v1/auth/* |
| Users & Company | User management and company profile | /api/v1/admin/users/*, /api/v1/users/me |
| Invoices | Invoice CRUD operations and status management | /api/v1/invoices (GET, POST, PUT, DELETE, verify, reject, reopen) |
| Invoice Upload | File upload and LLM-based data extraction | /api/v1/invoices/upload, /api/v1/invoices/bulk-upload |
| Filtering & Search | Advanced invoice filtering with multiple criteria | /api/v1/invoices (with filter query params) |
| Export | Data export in XLSX, CSV, and accounting formats | /api/v1/invoices/export |
| Dashboard | Dashboard statistics and summary metrics | /api/v1/dashboard/* |
| Categories | Invoice category management | /api/v1/categories/* |
| Notifications | In-app notification management | /api/v1/notifications/* |
| Notification Preferences | Email and push notification settings | /api/v1/notifications/preferences/* |
| Version History | Invoice version tracking, diff, and revert | /api/v1/invoices/{id}/versions/* |
| Supplier Templates | LLM-learned supplier templates | /api/v1/templates/* |
| Automation Rules | User-defined automation rules engine | /api/v1/rules/* |
| Audit Log | System audit trail | /api/v1/audit-logs |
| KVKK Compliance | Data privacy and GDPR/KVKK operations | /api/v1/admin/kvkk/* |
| System Monitoring | Health checks, metrics, and system status | /api/v1/admin/system/*, /api/v1/admin/performance/* |
| Alerts | Alert management and test endpoints | /api/v1/admin/alerts/* |

---

### 2. Controller-Level Annotations

For EVERY controller in the project, add OpenAPI annotations. This is the bulk of the work. Go through each controller and add:

**2.1 Class-Level Annotations**

On each controller class, add a tag annotation to group it with the correct tag defined in section 1.5.

**2.2 Method-Level Annotations**

For each endpoint method, add an operation annotation that includes:

- **summary**: A short one-line description (e.g., "Create a new invoice manually")
- **description**: A longer description explaining what the endpoint does, any business rules, and side effects (e.g., "Creates a manual invoice entry. Triggers duplication check. Creates an audit log entry. Company-scoped — only accessible within the authenticated user's company.")
- **operationId**: A unique identifier (e.g., "createInvoice", "listInvoices")
- **Response descriptions**: For each possible HTTP status code, describe what it means in the context of this endpoint

**2.3 Response Documentation**

For each endpoint, document all possible response codes:

| Status | When | Description Template |
|---|---|---|
| 200 | Success (GET, PUT, PATCH) | "Successfully retrieved/updated {resource}" |
| 201 | Created (POST) | "Resource created successfully" |
| 204 | No Content (DELETE) | "Resource deleted successfully" |
| 400 | Bad Request | "Invalid request body or parameters. See error details." |
| 401 | Unauthorized | "Missing or invalid authentication token" |
| 403 | Forbidden | "Insufficient permissions. Required role: {ROLE}" |
| 404 | Not Found | "{Resource} not found" |
| 409 | Conflict | "Resource already exists (duplicate detection)" |
| 422 | Unprocessable Entity | "Validation failed. See field errors." |
| 423 | Locked | "Account locked due to too many failed login attempts" |
| 429 | Too Many Requests | "Rate limit exceeded. Retry after {X} seconds." |
| 500 | Internal Server Error | "An unexpected error occurred" |

Not every endpoint returns all codes — document only the relevant ones per endpoint.

---

### 3. Detailed Endpoint Documentation

Below is the complete list of endpoints that must be documented. For each one, add the appropriate OpenAPI annotations to the existing controller methods.

**3.1 Authentication API (`/api/v1/auth`)**

| Method | Path | Summary | Auth Required | Roles |
|---|---|---|---|---|
| POST | /register | Register a new user account | No | - |
| POST | /login | Authenticate and obtain JWT tokens | No | - |
| POST | /refresh | Refresh an expired access token | No (uses refresh token) | - |
| POST | /logout | Invalidate refresh token and end session | Yes | ALL |

Document the request body for each:
- Register: fullName, email, password, passwordConfirmation, companyName (for new company) or companyCode (to join existing)
- Login: email, password — Response: accessToken, refreshToken, expiresIn, user object
- Refresh: refreshToken — Response: new accessToken, refreshToken
- Logout: refreshToken

**3.2 Invoice CRUD API (`/api/v1/invoices`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | / | List invoices with pagination, sorting, and filtering | ALL |
| GET | /{id} | Get invoice detail with line items | ALL |
| POST | / | Create a new invoice manually | ALL |
| PUT | /{id} | Update an existing invoice | ADMIN, MANAGER, ACCOUNTANT |
| DELETE | /{id} | Soft delete an invoice | ADMIN, MANAGER |
| PATCH | /{id}/verify | Verify an invoice (change status to VERIFIED) | ADMIN, MANAGER, ACCOUNTANT |
| PATCH | /{id}/reject | Reject an invoice (change status to REJECTED) | ADMIN, MANAGER, ACCOUNTANT |
| PATCH | /{id}/reopen | Reopen a rejected invoice (back to PENDING) | ADMIN, MANAGER |

For the GET list endpoint, document ALL query parameters from Phase 23:
- page, size, sort
- dateFrom, dateTo (ISO date)
- status (PENDING, PROCESSING, VERIFIED, REJECTED)
- categoryId
- supplierName (partial match)
- amountMin, amountMax
- currency (TRY, USD, EUR)
- sourceType (LLM, E_INVOICE, MANUAL)
- llmProvider (GEMINI, GPT, CLAUDE)
- confidenceMin, confidenceMax (0-100)
- search (full-text search on invoice number, supplier name)

**3.3 File Upload API**

| Method | Path | Summary | Roles |
|---|---|---|---|
| POST | /invoices/upload | Upload a single file for LLM extraction | ALL |
| POST | /invoices/bulk-upload | Upload multiple files for batch extraction | ALL |

Document:
- Upload: multipart/form-data with file parameter. Accepted types: JPEG, PNG, PDF, XML. Max size: 10MB.
- Bulk upload: multipart/form-data with multiple files. Max 20 files per batch.
- Response: Upload confirmation with processing status (PROCESSING). The actual extraction result arrives asynchronously via WebSocket notification.

**3.4 Export API**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | /invoices/export | Export filtered invoices to XLSX, CSV, or accounting format | ADMIN, MANAGER, ACCOUNTANT |

Document query parameters:
- format (required): xlsx, csv, logo, mikro, netsis, luca
- All filter parameters from 3.2 (same filter set)
- includeItems (boolean): Whether to include line item rows
- Response: File download (Content-Disposition: attachment)

**3.5 Dashboard API (`/api/v1/dashboard`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | /stats | Dashboard summary statistics | ALL |
| GET | /category-distribution | Invoice count by category (for pie chart) | ALL |
| GET | /monthly-trend | Monthly invoice trend (for line chart) | ALL |
| GET | /top-suppliers | Top suppliers by total amount (for bar chart) | ALL |
| GET | /pending-actions | Invoices pending verification | ALL |

Document query parameters: dateFrom, dateTo for time range filtering.

**3.6 Category API (`/api/v1/categories`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | / | List all active categories | ALL |
| POST | / | Create a new category | ADMIN, MANAGER |
| PUT | /{id} | Update a category | ADMIN, MANAGER |
| DELETE | /{id} | Delete a category | ADMIN, MANAGER |

**3.7 Notification API (`/api/v1/notifications`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | / | List notifications (paginated, filterable by read status) | ALL |
| GET | /unread-count | Get the count of unread notifications | ALL |
| PATCH | /{id}/read | Mark a notification as read | ALL |
| PATCH | /read-all | Mark all notifications as read | ALL |
| DELETE | /{id} | Delete a notification | ALL |
| GET | /preferences | Get notification preferences | ALL |
| PUT | /preferences | Update notification preferences | ALL |

**3.8 Version History API (`/api/v1/invoices/{id}/versions`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | / | List all versions of an invoice | ALL |
| GET | /{versionId} | Get a specific version snapshot | ALL |
| GET | /diff?from=X&to=Y | Compare two versions (diff) | ALL |
| POST | /revert/{versionNumber} | Revert invoice to a previous version | ADMIN, MANAGER |

**3.9 Supplier Template API (`/api/v1/templates`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | / | List supplier templates (paginated) | ADMIN, MANAGER, ACCOUNTANT |
| GET | /{id} | Get a template with learned data | ADMIN, MANAGER, ACCOUNTANT |
| GET | /supplier/{taxNumber} | Look up template by supplier tax number | ADMIN, MANAGER, ACCOUNTANT |
| PUT | /{id} | Update template settings | ADMIN, MANAGER |
| DELETE | /{id} | Deactivate a template | ADMIN, MANAGER |
| POST | /{id}/reset | Reset learned data | ADMIN |
| GET | /stats | Template system statistics | ADMIN, MANAGER |

**3.10 Automation Rules API (`/api/v1/rules`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | / | List automation rules (paginated, sortable by priority) | ADMIN, MANAGER, ACCOUNTANT |
| GET | /{id} | Get a single rule with conditions and actions | ADMIN, MANAGER, ACCOUNTANT |
| POST | / | Create a new automation rule | ADMIN, MANAGER |
| PUT | /{id} | Update an existing rule | ADMIN, MANAGER |
| DELETE | /{id} | Deactivate a rule | ADMIN, MANAGER |
| POST | /{id}/toggle | Toggle rule active status | ADMIN, MANAGER |
| POST | /{id}/test | Dry run a rule against a specific invoice | ADMIN, MANAGER |
| GET | /{id}/history | Rule execution history | ADMIN, MANAGER |
| GET | /execution-log | All rule execution logs | ADMIN, MANAGER |

Document the rule condition fields, operators, and action types in the schema descriptions.

**3.11 Audit Log API (`/api/v1/audit-logs`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | / | List audit log entries (paginated, filterable) | ADMIN, MANAGER |
| GET | /entity/{entityType}/{entityId} | Get audit trail for a specific entity | ADMIN, MANAGER |

**3.12 User & Company Management API**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | /users/me | Get current user profile | ALL |
| PUT | /users/me | Update current user profile | ALL |
| PUT | /users/me/password | Change current user's password | ALL |
| GET | /admin/users | List all users in the company | ADMIN |
| PUT | /admin/users/{id}/role | Change a user's role | ADMIN |
| DELETE | /admin/users/{id} | Deactivate a user | ADMIN |

**3.13 KVKK Compliance API (`/api/v1/admin/kvkk`)**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | /report | KVKK compliance report | ADMIN |
| POST | /anonymize/{userId} | Exercise right to be forgotten | ADMIN |
| GET | /consents | List user consent records | ADMIN |

**3.14 System Monitoring API**

| Method | Path | Summary | Roles |
|---|---|---|---|
| GET | /admin/system/status | Comprehensive system status | ADMIN |
| GET | /admin/performance/health | Performance metrics and pool stats | ADMIN |
| GET | /admin/alerts | List recent alert log entries | ADMIN |
| POST | /admin/alerts/test | Send a test alert to all channels | ADMIN |

---

### 4. Request/Response Schema Documentation

**4.1 DTO Annotations**

For ALL DTOs (request and response), add schema annotations:

- On the class: Schema annotation with description
- On each field: Schema annotation with description, example value, required flag, min/max constraints where applicable

Focus on the most-used DTOs:
- CreateInvoiceRequest / UpdateInvoiceRequest
- InvoiceListResponse / InvoiceDetailResponse
- InvoiceItemDto
- LoginRequest / LoginResponse / RegisterRequest
- ExportRequest query parameters
- RuleCreateRequest / RuleResponse
- NotificationResponse
- DashboardStatsResponse
- ErrorResponse (the standard error format)

Provide realistic example values (in Turkish context where appropriate, e.g., Turkish company names, Turkish invoice numbers like "FTR-2026-001", Turkish tax numbers).

**4.2 Enum Documentation**

Document all enums used in the API:
- InvoiceStatus: PENDING, PROCESSING, VERIFIED, REJECTED — with descriptions
- SourceType: LLM, E_INVOICE, MANUAL — with descriptions
- LlmProvider: GEMINI, GPT, CLAUDE
- Currency: TRY, USD, EUR, GBP
- UnitType: ADET, KG, LT, M, M2, M3, PAKET, KUTU, SAAT, GUN
- UserRole: ADMIN, MANAGER, ACCOUNTANT, INTERN — with permission descriptions
- ExportFormat: XLSX, CSV, LOGO, MIKRO, NETSIS, LUCA
- NotificationType: (all types from Phase 27)
- AlertSeverity: CRITICAL, HIGH, WARN
- RuleTriggerPoint: AFTER_EXTRACTION, AFTER_VERIFICATION, ON_MANUAL_CREATE
- RuleOperator: All operators
- RuleActionType: All action types

---

### 5. Error Code Dictionary

**5.1 Create a dedicated documentation section or endpoint**

Create a markdown document `docs/api/error-codes.md` that lists ALL error codes returned by the API:

| Error Code | HTTP Status | Description (EN) | Description (TR) |
|---|---|---|---|
| VALIDATION_ERROR | 400 | Request validation failed | İstek doğrulama hatası |
| INVALID_ARGUMENT | 400 | Invalid argument provided | Geçersiz parametre |
| UNAUTHORIZED | 401 | Authentication required | Kimlik doğrulama gerekli |
| INVALID_CREDENTIALS | 401 | Wrong email or password | Hatalı e-posta veya şifre |
| TOKEN_EXPIRED | 401 | JWT token has expired | Oturum süresi doldu |
| FORBIDDEN | 403 | Insufficient permissions | Bu işlem için yetkiniz yok |
| INVOICE_NOT_FOUND | 404 | Invoice not found | Fatura bulunamadı |
| USER_NOT_FOUND | 404 | User not found | Kullanıcı bulunamadı |
| CATEGORY_NOT_FOUND | 404 | Category not found | Kategori bulunamadı |
| TEMPLATE_NOT_FOUND | 404 | Template not found | Şablon bulunamadı |
| RULE_NOT_FOUND | 404 | Rule not found | Kural bulunamadı |
| VERSION_NOT_FOUND | 404 | Version not found | Versiyon bulunamadı |
| DUPLICATE_EMAIL | 409 | Email already registered | Bu e-posta zaten kayıtlı |
| DUPLICATE_INVOICE | 409 | Potential duplicate invoice detected | Mükerrer fatura tespit edildi |
| INVALID_STATUS_TRANSITION | 422 | Invalid invoice status transition | Geçersiz durum değişikliği |
| ACCOUNT_LOCKED | 423 | Account locked due to failed attempts | Hesap kilitlendi |
| RATE_LIMIT_EXCEEDED | 429 | Too many requests | İstek limiti aşıldı |
| EXPORT_LIMIT_EXCEEDED | 429 | Too many export requests | Dışa aktarım limiti aşıldı |
| FILE_TOO_LARGE | 413 | File size exceeds limit | Dosya boyutu çok büyük |
| UNSUPPORTED_FILE_TYPE | 415 | File type not supported | Desteklenmeyen dosya türü |
| EXTRACTION_FAILED | 500 | All LLM providers failed | Veri çıkarım başarısız |
| INTERNAL_ERROR | 500 | Unexpected server error | Beklenmeyen sunucu hatası |

**5.2 Reference in Swagger UI**

Add a reference to this error code dictionary in the Swagger UI description or as an external documentation link.

---

### 6. Python FastAPI Documentation Enhancement

The Python extraction service (FastAPI) automatically generates Swagger UI at `/docs` and ReDoc at `/redoc`. Enhance the existing documentation:

**6.1 API Metadata**

Update the FastAPI app initialization to include:
- Title: "Fatura OCR Extraction Service API"
- Description: Description of the service (LLM-based invoice data extraction)
- Version: "1.0.0"
- Contact information

**6.2 Endpoint Descriptions**

Ensure all endpoints have proper descriptions:
- Health endpoints: /health, /health/ready, /health/live, /health/dependencies
- Extraction endpoints: /api/v1/extraction/extract, /api/v1/extraction/extract-batch, /api/v1/extraction/providers
- Include request/response examples

**6.3 Internal-Only Documentation Note**

Add a note in the FastAPI docs description that this service is internal-only (not directly accessible from outside the Docker network in production). It communicates with the Spring Boot backend via RabbitMQ and is not intended for direct external access.

---

### 7. API Versioning Documentation

**7.1 Versioning Strategy Document**

Create `docs/api/versioning-strategy.md` that explains:
- The API uses URL-based versioning: `/api/v1/...`
- All current endpoints are v1
- How a v2 would be introduced (new controllers with `/api/v2/` prefix, v1 maintained for backward compatibility)
- Deprecation policy (document in headers when an endpoint is deprecated)

**7.2 Swagger UI Note**

Add a note in the Swagger UI description that this is API version 1.0.0 and all endpoints use the `/api/v1/` prefix.

---

### 8. Postman Collection Export (Optional but Recommended)

**8.1 Generate Postman Collection**

SpringDoc can generate an OpenAPI 3.0 JSON spec at `/api/v1/api-docs`. This can be imported directly into Postman. Document the steps:
1. Access `http://localhost:8080/api/v1/api-docs` to get the raw OpenAPI JSON
2. Import into Postman using "Import > Link" or "Import > Raw Text"
3. Configure a Postman environment with variables: `baseUrl`, `accessToken`

**8.2 Alternative: Create a simple Postman collection manually**

If time permits, create a `docs/api/postman-collection.json` with pre-configured requests for the most common workflows:
- Register → Login → Create Invoice → List Invoices → Verify Invoice → Export

This is optional but impressive for the graduation demo.

---

### 9. Production Access Control

**9.1 Swagger UI Availability by Environment**

Configure SpringDoc availability based on the Spring profile:

- **Development**: Swagger UI enabled, accessible at `/api/docs`
- **Staging**: Swagger UI enabled (useful for testing), accessible at `/api/docs`
- **Production**: Swagger UI DISABLED for security (already configured in Phase 39's application-prod.yml, verify and ensure)

Configuration:
- `springdoc.swagger-ui.enabled=true` (dev/staging)
- `springdoc.swagger-ui.enabled=false` (prod)
- `springdoc.api-docs.enabled=false` (prod — also disable the raw JSON spec)

**9.2 Alternative: Password-Protected Swagger in Production**

If the team wants Swagger available in production for debugging:
- Keep it enabled but add HTTP Basic auth or restrict to ADMIN role only
- This is optional — disabling is the safer default

---

## FILE STRUCTURE

After completing this phase, the following files should be created or modified:

```
fatura-ocr-system/
├── backend/
│   └── src/main/java/com/faturaocr/
│       └── infrastructure/
│           └── config/
│               └── OpenApiConfig.java                # NEW — Global OpenAPI configuration
│       └── interfaces/
│           └── rest/
│               ├── AuthController.java               # MODIFIED — Add OpenAPI annotations
│               ├── InvoiceController.java            # MODIFIED — Add OpenAPI annotations
│               ├── CategoryController.java           # MODIFIED — Add OpenAPI annotations
│               ├── DashboardController.java          # MODIFIED — Add OpenAPI annotations
│               ├── NotificationController.java       # MODIFIED — Add OpenAPI annotations
│               ├── ExportController.java             # MODIFIED — Add OpenAPI annotations
│               ├── VersionHistoryController.java     # MODIFIED — Add OpenAPI annotations
│               ├── TemplateController.java           # MODIFIED — Add OpenAPI annotations
│               ├── RuleController.java               # MODIFIED — Add OpenAPI annotations
│               ├── AuditLogController.java           # MODIFIED — Add OpenAPI annotations
│               ├── UserController.java               # MODIFIED — Add OpenAPI annotations
│               ├── AdminKvkkController.java          # MODIFIED — Add OpenAPI annotations
│               └── AdminMonitoringController.java    # MODIFIED — Add OpenAPI annotations
│       └── interfaces/
│           └── dto/                                   # MODIFIED — Add Schema annotations to all DTOs
├── extraction-service/
│   └── app/
│       └── main.py                                    # MODIFIED — Enhanced metadata
│       └── api/routes/
│           ├── health.py                              # MODIFIED — Enhanced descriptions
│           └── extraction.py                          # MODIFIED — Enhanced descriptions
├── docs/
│   └── api/
│       ├── error-codes.md                             # NEW
│       ├── versioning-strategy.md                     # NEW
│       ├── api-overview.md                            # NEW — High-level API overview
│       └── postman-collection.json                    # NEW (optional)
└── backend/src/main/resources/
    ├── application.yml                                # MODIFIED (springdoc path config)
    ├── application-staging.yml                        # MODIFIED (swagger enabled)
    └── application-prod.yml                           # MODIFIED (swagger disabled — verify)
```

---

## DATABASE CHANGES

**None.** This phase is purely documentation — no database schema changes, no migrations.

---

## TESTING REQUIREMENTS

### Verification Tests

1. **Swagger UI Access**: Navigate to `http://localhost:8080/api/docs` — verify the Swagger UI loads and displays all endpoint groups
2. **Authentication in Swagger**: Click "Authorize" → enter a valid JWT token → test a protected endpoint (e.g., GET /invoices) — verify it works
3. **All Endpoints Visible**: Verify every endpoint from sections 3.1-3.14 appears in the Swagger UI with correct method, path, and description
4. **Request Examples**: Verify that clicking "Try it out" on POST endpoints shows a pre-filled example request body
5. **Response Examples**: Verify that response schemas show the correct structure with example values
6. **Error Responses**: Verify that each endpoint documents its possible error codes
7. **Enum Values**: Verify that enum fields (status, sourceType, currency, etc.) show their allowed values in the schema
8. **OpenAPI JSON**: Access `http://localhost:8080/api/v1/api-docs` — verify it returns valid JSON that can be imported into Postman
9. **Python Docs**: Access `http://localhost:8000/docs` — verify the extraction service documentation is enhanced
10. **Production Config**: Start with `SPRING_PROFILES_ACTIVE=prod` — verify Swagger UI is NOT accessible

### Quality Checks

1. No endpoint should have a missing description (summary)
2. No endpoint should be missing response code documentation
3. All required request fields should be marked as required in the schema
4. All DTOs should have example values
5. All enums should have their values listed

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_41_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Files created or modified (full list with paths)
3. OpenAPI configuration details (info, security, servers, tags)
4. Total number of documented endpoints (count by tag group)
5. Swagger UI URL and screenshots (or description of the UI)
6. OpenAPI JSON spec location
7. Error code dictionary summary (total error codes documented)
8. Python FastAPI documentation enhancements
9. API versioning strategy summary
10. Postman collection (if created)
11. Production access control configuration
12. Verification test results (all 10 checks)
13. Quality check results
14. Issues encountered and solutions
15. Next steps (Phase 42 User Guide)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 0**: springdoc-openapi dependency already in pom.xml
- **Phase 2**: ErrorResponse, ApiResponse wrapper DTOs (to document)
- **Phase 4-5**: Authentication and RBAC (security scheme documentation)
- **Phase 7**: Invoice CRUD (the core endpoints to document)
- **All feature phases (0-40)**: All endpoints must be implemented before they can be documented

### Required By
- **Phase 42**: User Guide — may reference API documentation for admin/developer sections

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ OpenApiConfig created with correct API metadata (title, description, version, contact)
2. ✅ JWT Bearer security scheme configured and working in Swagger UI "Authorize" button
3. ✅ Server URLs configured for dev/staging/production
4. ✅ All endpoints organized into logical tag groups (17 groups)
5. ✅ Every controller has OpenAPI annotations with summary and description
6. ✅ Every endpoint documents its possible HTTP response codes
7. ✅ All DTOs have Schema annotations with descriptions and example values
8. ✅ All enums are documented with allowed values
9. ✅ Swagger UI is accessible at `/api/docs` and fully functional
10. ✅ "Try it out" works — endpoints can be tested directly from Swagger UI
11. ✅ OpenAPI JSON spec available at `/api/v1/api-docs` and is valid
12. ✅ Error code dictionary created (`docs/api/error-codes.md`)
13. ✅ API versioning strategy documented
14. ✅ API overview document created
15. ✅ Python FastAPI documentation enhanced with metadata and descriptions
16. ✅ Swagger UI disabled in production profile
17. ✅ All existing tests still pass (annotation changes should not affect functionality)
18. ✅ Result file created at docs/OMER/step_results/faz_41_result.md

---

## IMPORTANT NOTES

1. **This Is Annotation Work, Not Feature Work**: This phase does NOT change any business logic, create new endpoints, or modify any service code. It only adds OpenAPI annotations to existing controllers and DTOs, creates a configuration class, and writes documentation files. If you find yourself writing business logic, you are doing it wrong.

2. **Do NOT Break Existing Tests**: OpenAPI annotations should be purely additive. They do not affect the runtime behavior of the application. All existing tests must continue to pass without modification.

3. **Realistic Example Values**: Use Turkish context for example values — Turkish company names (e.g., "ABC Teknoloji Ltd. Şti."), Turkish tax numbers (10 digits for VKN, 11 for TCKN), Turkish invoice numbers (e.g., "FTR-2026-00142"), TRY currency amounts, Turkish dates. This makes the documentation feel authentic for the graduation demo.

4. **Do NOT Over-Document**: Write concise descriptions. A one-liner summary plus a 2-3 sentence description per endpoint is sufficient. Do not write paragraphs for simple CRUD endpoints. Save longer descriptions for complex endpoints like the rule engine test, export with filters, or the system status endpoint.

5. **Focus on the Spring Boot API**: The Python FastAPI documentation is already auto-generated and good. The enhancements there are minor (metadata, descriptions). Spend 90% of your time on the Spring Boot Swagger documentation.

6. **Swagger UI Is a Demo Asset**: During the graduation presentation, the Swagger UI is an impressive visual demonstration of the API. Make sure it looks professional — correct descriptions, consistent formatting, no typos, logical grouping.

7. **Coordinate with FURKAN**: Let Furkan know the Swagger UI URL so he can reference it during the frontend development. Also, let him know the Python extraction service docs URL for any extraction-related work.

8. **Check springdoc Version Compatibility**: The project uses Spring Boot 3.2.x. Ensure the springdoc-openapi version in pom.xml is compatible (2.3.x or later for Spring Boot 3). If there is a version mismatch from Phase 0, update it.
