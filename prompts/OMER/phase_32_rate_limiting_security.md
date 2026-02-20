# PHASE 32: RATE LIMITING & SECURITY HARDENING

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 — LLM-based extraction
  - **Next.js Frontend**: Port 3001
  - **RabbitMQ**: Port 5673
  - **Redis**: Port 6380

### Current State (Phases 0-31 Completed)
- ✅ Phase 0-9: Docker, CI/CD, Hexagonal Architecture, Database, Auth (JWT + Redis), RBAC (ADMIN/MANAGER/ACCOUNTANT/INTERN), Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10-12: Frontend layout, auth pages, invoice list/CRUD UI
- ✅ Phase 13-22: Full extraction pipeline (FastAPI, Gemini/GPT/Claude fallback, validation, e-Invoice XML, RabbitMQ async, file upload, verification UI)
- ✅ Phase 23-26: Filtering & Search, Export (XLSX/CSV + accounting formats), Dashboard
- ✅ Phase 27-28: Notification system (in-app WebSocket, email, push, user preferences)
- ✅ Phase 29: Version History & Rollback
- ✅ Phase 30: Template Learning & Rule Engine
- ✅ Phase 31: KVKK Compliance (AES-256-GCM encryption, consent tracking, right to be forgotten, data retention, compliance report)

### Relevant Existing Security Infrastructure

**Authentication (Phase 4):**
- JWT-based stateless auth with access token (15 min) + refresh token (7 days)
- Redis for refresh token storage and blacklisting
- BCrypt password hashing (strength 12)
- JwtAuthenticationFilter in Spring Security filter chain

**RBAC (Phase 5):**
- 4 roles: ADMIN, MANAGER, ACCOUNTANT, INTERN
- CompanyContextFilter for multi-tenant isolation
- @PreAuthorize and custom annotations for method-level security

**Current SecurityConfig (Phase 4/5):**
- CSRF disabled (JWT-based, stateless)
- CORS configured for localhost:3001 and localhost:8082
- Session management: STATELESS
- Public endpoints: /api/v1/auth/**, /api/v1/health, /actuator/health, /swagger-ui/**, /v3/api-docs/**
- All other endpoints: authenticated

**What is currently MISSING (this phase will add):**
- No rate limiting — any client can make unlimited requests
- No security response headers (X-Content-Type-Options, X-Frame-Options, HSTS, CSP)
- CORS is only configured for development (localhost)
- No brute-force protection on login endpoint
- No request size limits
- No IP-based blocking capability
- SQL injection protection exists (JPA prepared statements) but is not explicitly validated/documented

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 2 days

---

## OBJECTIVE

Harden the application's security posture by implementing:

1. **Rate limiting** using Redis (or Bucket4j) to prevent API abuse and brute-force attacks
2. **Security response headers** to protect against XSS, clickjacking, MIME sniffing, and other browser-based attacks
3. **CORS hardening** with environment-specific configuration
4. **Login brute-force protection** with account lockout after failed attempts
5. **Request validation hardening** to verify SQL injection and XSS protections are solid
6. **Request size limits** to prevent denial-of-service through oversized payloads
7. **Security audit** of existing endpoints to identify and fix any gaps

---

## DETAILED REQUIREMENTS

### 1. Rate Limiting with Redis

**Why Redis-based (not Bucket4j)?**
Redis is already in the stack (used for JWT token storage). Using Redis for rate limiting avoids adding another dependency and works across multiple backend instances if the application is horizontally scaled.

**Implementation approach:** Use a Redis-based sliding window rate limiter implemented as a Spring filter.

**Rate Limit Tiers:**

| Tier | Target | Limit | Window | Description |
|---|---|---|---|---|
| PUBLIC | Unauthenticated endpoints (/api/v1/auth/**) | 20 requests | Per minute | Registration, login, refresh |
| LOGIN | POST /api/v1/auth/login specifically | 5 requests | Per minute | Brute-force protection |
| AUTHENTICATED | All authenticated endpoints | 100 requests | Per minute | Normal API usage |
| UPLOAD | POST /api/v1/invoices/upload, /bulk-upload | 10 requests | Per minute | File upload (expensive operations) |
| EXPORT | GET /api/v1/invoices/export | 5 requests | Per minute | Export operations (heavy queries) |
| ADMIN | /api/v1/admin/** endpoints | 50 requests | Per minute | Admin operations |

**Rate Limit Key:**
- For authenticated users: `rate_limit:{tier}:{userId}`
- For unauthenticated users: `rate_limit:{tier}:{ipAddress}`
- Use IP address from `X-Forwarded-For` header (if behind a reverse proxy) or `request.getRemoteAddr()`

**Redis Implementation:**

Use Redis sorted sets with sliding window algorithm:
1. Key: `rate_limit:{tier}:{identifier}`
2. On each request:
   - Remove entries older than the window (ZREMRANGEBYSCORE)
   - Count remaining entries (ZCARD)
   - If count >= limit: reject with 429
   - If count < limit: add current timestamp (ZADD) and set key TTL
3. All Redis operations should be in a single pipeline/transaction for atomicity

**Response Headers for Rate Limited Requests:**

All API responses should include:
- `X-RateLimit-Limit`: Maximum requests allowed in the window
- `X-RateLimit-Remaining`: Remaining requests in the current window
- `X-RateLimit-Reset`: Unix timestamp when the window resets

When rate limit is exceeded:
- HTTP Status: `429 Too Many Requests`
- `Retry-After`: Seconds until the client can retry
- Response body (consistent with existing error format):
  - error: "RATE_LIMIT_EXCEEDED"
  - message: "İstek limiti aşıldı. Lütfen {seconds} saniye sonra tekrar deneyin."
  - retryAfter: seconds

**Rate Limit Filter:**

Create a `RateLimitFilter` that extends `OncePerRequestFilter`:
1. Determine the tier based on the request path and authentication status
2. Determine the identifier (userId or IP)
3. Check Redis for current count
4. If exceeded: return 429 response immediately
5. If not exceeded: increment counter and continue the filter chain
6. Add rate limit headers to the response

Place this filter AFTER JwtAuthenticationFilter (so userId is available) but BEFORE controller processing.

---

### 2. Login Brute-Force Protection

Beyond the general rate limit on `/api/v1/auth/login`, implement account-level lockout:

**Logic:**
1. Track failed login attempts per email in Redis: `login_attempts:{email}`
2. On each failed login: increment the counter, set TTL to 15 minutes
3. After 5 consecutive failed attempts: lock the account for 15 minutes
4. On successful login: reset the counter
5. While locked: return HTTP 423 Locked with message: "Hesabınız çok fazla başarısız giriş denemesi nedeniyle geçici olarak kilitlendi. {remainingMinutes} dakika sonra tekrar deneyin."

**Admin Override:**
- ADMIN users should be able to unlock a locked account via: `POST /api/v1/admin/users/{userId}/unlock`
- This endpoint clears the Redis key for that user's email

**Configuration:**
- `app.security.login.max-attempts`: 5
- `app.security.login.lockout-duration-minutes`: 15

---

### 3. Security Response Headers

Update the SecurityConfig to add comprehensive security headers.

**Headers to add:**

| Header | Value | Purpose |
|---|---|---|
| X-Content-Type-Options | nosniff | Prevents MIME type sniffing |
| X-Frame-Options | DENY | Prevents clickjacking (page cannot be framed) |
| X-XSS-Protection | 0 | Disabled — modern browsers use CSP instead, and this header can introduce vulnerabilities |
| Strict-Transport-Security | max-age=31536000; includeSubDomains | HSTS — forces HTTPS for 1 year. Only add when HTTPS is available (use a flag) |
| Content-Security-Policy | default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none' | CSP — restricts resource loading |
| Referrer-Policy | strict-origin-when-cross-origin | Controls referrer information sent with requests |
| Permissions-Policy | camera=(), microphone=(), geolocation=(), payment=() | Restricts browser features |
| Cache-Control | no-store, no-cache, must-revalidate | For API responses — prevent caching of sensitive data |
| Pragma | no-cache | Legacy cache prevention |

**Implementation:**

Use Spring Security's `headers()` configuration in the SecurityFilterChain. For headers not directly supported by Spring Security, use a custom filter or `StaticHeadersWriter`.

**IMPORTANT**: The Content-Security-Policy header shown above is for API responses. The frontend (Next.js) may need different CSP values. The backend CSP should be restrictive since it only serves JSON API responses, not HTML pages. If Swagger UI is enabled, you may need to relax CSP for `/swagger-ui/**` paths.

**Configuration:**
- `app.security.headers.hsts-enabled`: false (set to true only when HTTPS is configured in production)
- `app.security.headers.csp-enabled`: true

---

### 4. CORS Hardening

The current CORS configuration only allows `localhost:3001` and `localhost:8082`. Make it environment-aware.

**Requirements:**

- Move allowed origins to configuration (application.yml / environment variables)
- Development: `http://localhost:3001`, `http://localhost:8082`
- Staging: staging domain URL
- Production: production domain URL
- Do NOT use wildcard `*` for allowed origins in any environment
- Allow only necessary HTTP methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
- Allow only necessary headers: Authorization, Content-Type, Accept, X-Requested-With
- Expose headers: Authorization, X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset, Content-Disposition (for file downloads)
- Max age: 3600 seconds (1 hour)
- Allow credentials: true

**Configuration:**
- `app.security.cors.allowed-origins`: list of allowed origins (from environment variable)
- `CORS_ALLOWED_ORIGINS`: environment variable, comma-separated

---

### 5. SQL Injection Protection Validation

The system already uses JPA/Hibernate prepared statements, which inherently protect against SQL injection. However, this phase should VERIFY and DOCUMENT the protection:

**Verification checklist:**

1. **JPA Repositories**: Confirm all custom queries use parameter binding (`:paramName` or `?1`), not string concatenation. Check all `@Query` annotations in repository interfaces.

2. **Specification Pattern (Phase 23)**: Verify that the dynamic query building in `InvoiceSpecification` uses CriteriaBuilder properly and does not construct raw SQL.

3. **Native Queries**: Search for any `@Query(nativeQuery = true)` and verify parameter binding.

4. **DTO Validation**: Verify that all request DTOs use Bean Validation annotations:
   - `@NotBlank`, `@NotNull` for required fields
   - `@Size(max=...)` for string length limits
   - `@Pattern` for format validation (email, phone, tax number)
   - `@Min`, `@Max` for numeric ranges
   - `@Valid` on nested objects

5. **Input Sanitization**: For text fields that will be displayed in the frontend (invoice notes, rule names, notification messages), add a sanitization utility that strips HTML tags and script content. This prevents stored XSS.

**Implementation:**

Create a `SanitizationUtils` class with methods:
- `sanitizeHtml(String input)`: Strip all HTML tags using a simple regex or a library like OWASP Java HTML Sanitizer
- `sanitizeForLog(String input)`: Sanitize input before logging (prevent log injection)

Apply sanitization in service layer for user-provided text fields: notes, descriptions, rule names, notification messages.

**Do NOT sanitize:**
- Invoice data from LLM extraction (may contain legitimate special characters)
- Tax numbers, amounts, dates (structured data)
- Encrypted fields (would corrupt them)

---

### 6. Request Size Limits

Prevent denial-of-service through oversized payloads.

**Configuration (application.yml):**

- `spring.servlet.multipart.max-file-size`: 20MB (for single file uploads)
- `spring.servlet.multipart.max-request-size`: 100MB (for bulk uploads with ZIP)
- `server.tomcat.max-http-form-post-size`: 20MB
- `server.tomcat.max-swallow-size`: 100MB

**JSON Request Body Limits:**

Add a filter that checks Content-Length for non-multipart requests:
- Maximum JSON body size: 1MB (configurable)
- If exceeded: return HTTP 413 Payload Too Large with message: "İstek boyutu çok büyük. Maksimum izin verilen boyut: {maxSize}"

**Configuration:**
- `app.security.request.max-json-body-size`: 1048576 (1MB in bytes)

---

### 7. Security Endpoint: Security Status

Create an admin endpoint that reports the current security configuration status.

**Endpoint**: `GET /api/v1/admin/security/status`
**RBAC**: ADMIN only

**Returns:**

- **Rate Limiting**:
  - Enabled: true/false
  - Tiers configured (list with limits)
  - Current active rate limit keys in Redis (count)

- **Brute Force Protection**:
  - Enabled: true/false
  - Max attempts before lockout
  - Lockout duration
  - Currently locked accounts (count)

- **Security Headers**:
  - HSTS enabled: true/false
  - CSP enabled: true/false
  - All headers list with current values

- **CORS**:
  - Allowed origins list
  - Allowed methods

- **Request Limits**:
  - Max file upload size
  - Max JSON body size

- **Encryption (from Phase 31)**:
  - Data encryption enabled: true/false

---

### 8. Python Microservice Security

The Python FastAPI service (port 8001) also needs basic security hardening. Since it's an internal service (only called by Spring Boot, not exposed to the public), the requirements are lighter:

**8.1 Internal API Key Authentication:**

Add a simple shared API key for Spring Boot → FastAPI communication:
- FastAPI checks for `X-Internal-API-Key` header on all requests
- Spring Boot includes this header when calling FastAPI
- The key is shared via environment variable: `INTERNAL_API_KEY`
- If the key is missing or wrong: return 401 Unauthorized

**8.2 FastAPI Rate Limiting:**

Add basic rate limiting to the FastAPI extraction endpoints:
- `/extract` and `/extract/base64`: 30 requests per minute per client IP
- Use a simple in-memory rate limiter (e.g., `slowapi` library) — Redis-based is optional since this service has a single instance

**8.3 Request Size Limit:**

- Maximum file upload to FastAPI: 20MB
- Configure in FastAPI/Uvicorn settings

**8.4 CORS:**

Since FastAPI is internal-only, CORS should be restricted:
- Only allow requests from Spring Boot (http://localhost:8082, or the backend container hostname in Docker)

---
### 9. Frontend: 429 Rate Limit Response Handling

**Assigned To**: FURKAN (coordinate with ÖMER)

The Axios response interceptor (Phase 11, `lib/api/client.ts`) currently handles only 401 errors. Extend it to handle 429 (Too Many Requests) responses from the backend rate limiter.

**9.1 Axios Interceptor Enhancement**

In the existing response error interceptor (after the 401 handling block), add a 429 handler:

- Read the `Retry-After` header from the 429 response (value is in seconds)
- Show a toast notification: "Çok fazla istek gönderildi. Lütfen {N} saniye bekleyin." (where N is the Retry-After value, default 60 if header is missing)
- Implement automatic retry with exponential backoff:
  - Wait for the `Retry-After` duration
  - Retry the original request ONCE
  - If the retry also gets 429, reject the promise (do not retry indefinitely)
- Track a flag (`_rateLimitRetry`) on the request config to prevent infinite retry loops (same pattern as the existing `_retry` flag for 401)

**9.2 Toast Notification Integration**

Use the existing toast/notification system (from Phase 27-F or the UI component library):
- Toast type: WARNING (yellow/orange)
- Auto-dismiss after the Retry-After duration
- Include a countdown or progress indicator if feasible

**9.3 Upload Page Special Handling**

The file upload page (Phase 21) has a different UX for rate limits:
- If a batch upload gets rate-limited, pause the upload queue (do not cancel already-uploading files)
- Show a specific message: "Yükleme hız sınırına ulaşıldı. {N} saniye sonra devam edilecek."
- Auto-resume after the Retry-After period

**9.4 Login Page Special Handling**

If the login endpoint returns 429 (due to brute-force protection):
- Show a specific message: "Çok fazla giriş denemesi. Lütfen {N} saniye bekleyin."
- Disable the login button for the Retry-After duration with a visual countdown
- Do NOT auto-retry login requests (security consideration)

**Testing:**
- Mock a 429 response with Retry-After header → verify toast appears with correct message
- Mock a 429 on upload page → verify upload queue pauses and resumes
- Mock a 429 on login page → verify button is disabled with countdown
- Verify automatic retry succeeds after Retry-After period
- Verify infinite retry prevention (two consecutive 429s → reject)

---
### 10. Testing Requirements

Write tests for:

- **Rate Limiting:**
  - Verify requests within limit succeed
  - Verify requests exceeding limit get 429
  - Verify rate limit headers are present
  - Verify different tiers have different limits
  - Verify rate limit resets after window expires

- **Brute Force:**
  - Verify account locks after N failed login attempts
  - Verify locked account returns 423
  - Verify account unlocks after lockout period
  - Verify admin can manually unlock

- **Security Headers:**
  - Verify all expected headers are present in responses
  - Verify X-Frame-Options is DENY
  - Verify X-Content-Type-Options is nosniff
  - Verify CSP header is present

- **CORS:**
  - Verify allowed origin gets proper CORS headers
  - Verify disallowed origin is rejected
  - Verify preflight (OPTIONS) requests work

- **Request Size:**
  - Verify oversized JSON body returns 413
  - Verify normal-sized requests pass through

- **Input Sanitization:**
  - Verify HTML tags are stripped from sanitized fields
  - Verify script content is removed
  - Verify legitimate text is preserved

- **Internal API Key (FastAPI):**
  - Verify request without key returns 401
  - Verify request with wrong key returns 401
  - Verify request with correct key succeeds

---

### 11. Configuration Summary

Add to application.yml:

- `app.security.rate-limit.enabled`: true
- `app.security.rate-limit.public-limit`: 20
- `app.security.rate-limit.login-limit`: 5
- `app.security.rate-limit.authenticated-limit`: 100
- `app.security.rate-limit.upload-limit`: 10
- `app.security.rate-limit.export-limit`: 5
- `app.security.rate-limit.admin-limit`: 50
- `app.security.rate-limit.window-seconds`: 60
- `app.security.login.max-attempts`: 5
- `app.security.login.lockout-duration-minutes`: 15
- `app.security.headers.hsts-enabled`: false
- `app.security.headers.csp-enabled`: true
- `app.security.cors.allowed-origins`: http://localhost:3001
- `app.security.request.max-json-body-size`: 1048576

Add to .env.example:
- `CORS_ALLOWED_ORIGINS=http://localhost:3001`
- `INTERNAL_API_KEY=<generate-a-random-key>`

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_32.0_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Rate limiting implementation details (algorithm, tiers, Redis key structure)
3. Brute-force protection implementation
4. Security headers added (full list with values)
5. CORS configuration per environment
6. SQL injection protection audit results (list all repositories/queries checked)
7. Input sanitization implementation
8. Request size limit configuration
9. Python microservice security additions
10. Security status endpoint documentation
11. Files created or modified (with paths)
12. Test results
13. Configuration properties added
14. Impact on existing features (especially file upload, frontend CORS)
15. Issues encountered and solutions
16. Next steps (Phase 33 PWA — note if any security headers affect PWA service worker)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 4**: Authentication (SecurityConfig, JWT filter, Redis)
- **Phase 5**: RBAC (CompanyContextFilter, role-based access)
- **Phase 13**: FastAPI service (for internal API key addition)
- **Phase 20**: File Upload (for upload rate limiting tier)
- **Phase 24**: Export (for export rate limiting tier)
- **Phase 31**: KVKK (encryption infrastructure, builds on security)

### Required By
- **Phase 33**: PWA (service worker may be affected by CSP headers)
- **Phase 35**: Unit Tests (rate limiting and security are test targets)
- **Phase 39**: Production Environment (HSTS, CORS, production origins)

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Redis-based rate limiting is active on all endpoints with tier-based limits
2. ✅ Rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset) are present in all responses
3. ✅ Exceeding rate limit returns 429 with Retry-After header
4. ✅ Login brute-force protection locks account after 5 failed attempts
5. ✅ Locked account returns 423 with informative message
6. ✅ Admin can manually unlock locked accounts
7. ✅ X-Content-Type-Options: nosniff header is present
8. ✅ X-Frame-Options: DENY header is present
9. ✅ Content-Security-Policy header is present and correctly configured
10. ✅ Referrer-Policy and Permissions-Policy headers are present
11. ✅ CORS is environment-configurable (not hardcoded to localhost)
12. ✅ CORS rejects unauthorized origins
13. ✅ SQL injection protection is verified across all repositories and queries
14. ✅ Input sanitization strips HTML/script from user-provided text fields
15. ✅ Oversized JSON requests return 413
16. ✅ FastAPI internal API key authentication is implemented
17. ✅ Security status admin endpoint returns comprehensive report
18. ✅ All tests pass
19. ✅ Result file is created at docs/OMER/step_results/faz_32.0_result.md
20. ✅ Frontend Axios interceptor handles 429 responses with Retry-After header
21. ✅ Toast notification shown to user with Turkish message and wait time
22. ✅ Automatic retry works after Retry-After period (single retry, no infinite loop)
23. ✅ Upload page pauses upload queue on rate limit and auto-resumes
24. ✅ Login page disables button with countdown on rate limit

---

## IMPORTANT NOTES

1. **Rate Limiting Must Not Break WebSocket**: The notification system (Phase 27) uses WebSocket (STOMP over SockJS). Rate limiting should NOT apply to WebSocket upgrade requests or STOMP message frames. Exclude WebSocket paths (`/ws/**`) from the rate limit filter.

2. **Rate Limiting Must Not Break File Uploads**: File uploads (Phase 20) can take a long time. The rate limit check should happen at the START of the request, not count against the window duration. Also, the upload tier has a separate, lower limit — this is intentional since uploads are expensive operations.

3. **CSRF Stays Disabled**: Since the application uses JWT-based stateless authentication (no cookies for auth), CSRF protection is not needed and should remain disabled. CSRF protection is relevant for cookie-based sessions. Adding CSRF tokens to a JWT-based API would create unnecessary complexity without security benefit. Document this decision.

4. **HSTS Only in Production**: The Strict-Transport-Security header should ONLY be enabled when HTTPS is configured. Enabling it in development (HTTP) would cause browser issues. Use the `app.security.headers.hsts-enabled` flag.

5. **CSP and Swagger UI**: The Content-Security-Policy may break Swagger UI if it's too restrictive. Add a less restrictive CSP for `/swagger-ui/**` and `/v3/api-docs/**` paths, or exclude them from CSP entirely in development.

6. **Redis Availability**: If Redis goes down, the rate limiter should fail OPEN (allow requests through) rather than fail CLOSED (block all requests). Log a warning when Redis is unavailable. Do not let a Redis outage make the entire API inaccessible.

7. **IP Behind Proxy**: In production, the app will be behind Nginx (Phase 39). The client's real IP will be in `X-Forwarded-For` header. Make sure the rate limiter reads this header when available, but validate it (take the first IP in the chain, not the last).

8.**Coordinate with Furkan**: Section 9 (Frontend 429 Handling) is assigned to Furkan. Share the exact rate limit response format (429 status, Retry-After header, response body structure) so the frontend implementation matches. CORS changes should also be communicated so the frontend dev server proxy is updated if needed.
