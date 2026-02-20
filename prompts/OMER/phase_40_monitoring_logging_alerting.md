# PHASE 40: MONITORING, LOGGING AND ALERTING

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM & Frontend) & Ömer Talha Yurttutan (Backend & Infrastructure)
- **Architecture**: Hybrid Microservices — Spring Boot (8082), Python FastAPI (8001), Next.js (3001)
- **Infrastructure**: PostgreSQL 15, Redis 7, RabbitMQ 3, Nginx (reverse proxy)

### Current State (Phases 0-39 Completed)
All features are implemented, tested, performance-optimized, and the deployment infrastructure is ready (staging + production Docker Compose, Nginx, SSL, backup scripts). The application is deployable, but currently lacks production-grade observability — there is no centralized logging, no structured monitoring dashboard, and no alerting for critical failures.

### What Already Exists for Monitoring (from previous phases)
- **Phase 13 (Python FastAPI)**: Structured logging with `structlog` (JSON format), request ID middleware, request/response logging, health endpoints (`/health`, `/health/ready`, `/health/live`, `/health/dependencies`)
- **Phase 15 (Gemini Integration)**: Token usage logging note — "Log token usage if the SDK provides it"
- **Phase 38-A (Backend Performance)**: A custom `GET /api/v1/admin/performance/health` endpoint was created that returns DB connection pool stats, Redis pool stats, average query time, cache hit/miss ratio, JVM memory usage, and uptime. Spring Boot Actuator was enabled for `/actuator/metrics`.
- **Phase 39 (Deployment)**: Docker health checks for all services in docker-compose.prod.yml. Service dependency order configured.
- **Phase 27-28 (Notifications)**: Email sending capability exists (SMTP configured). WebSocket notification infrastructure exists.
- **Phase 1 (CI/CD)**: GitHub Actions pipeline exists.

### What This Phase Adds
This phase builds on top of existing infrastructure to create a cohesive monitoring, logging, and alerting system. It does NOT replace what already exists — it enhances and connects the pieces. Additionally, this phase implements LLM API cost monitoring and budget alerting as required by the technical report's risk section ("API maliyet kontrolü — kullanım izleme, bütçe uyarıları").

### Phase Assignment
- **Assigned To**: ÖMER (Backend & Infrastructure Developer)
- **Estimated Duration**: 2 days
- **Branch**: `feature/omer/faz-40-monitoring-logging`

---

## OBJECTIVE

Create a production-grade observability layer across all three services (Spring Boot, Python FastAPI, Next.js) with four pillars:

1. **Monitoring**: Expose health and performance metrics in a structured, queryable way via Spring Boot Actuator and custom endpoints
2. **Logging**: Implement structured JSON logging with correlation IDs across all services, centralized in a file-based log aggregation setup (with optional ELK/Loki path documented)
3. **Alerting**: Detect critical failures and notify the team via email (reusing Phase 28 email infrastructure) or Slack webhook
4. **LLM Cost Monitoring**: Track LLM API usage per provider, calculate estimated costs, enforce budget limits, and trigger alerts when thresholds are exceeded

---

## DETAILED REQUIREMENTS

### 1. Spring Boot Actuator — Enhanced Configuration

Phase 38 already enabled basic Actuator. This phase configures it comprehensively for production.

**1.1 Actuator Endpoint Exposure**

Configure which Actuator endpoints are exposed and to whom:

- **Publicly accessible** (no auth required): `/actuator/health` — shows only UP/DOWN status (no details)
- **Authenticated + ADMIN role only**: `/actuator/health` with full details (show-details: when-authorized), `/actuator/info`, `/actuator/metrics`, `/actuator/loggers`, `/actuator/env` (sanitized — never expose secrets), `/actuator/flyway`
- **Never exposed in production**: `/actuator/shutdown`, `/actuator/threaddump`, `/actuator/heapdump`, `/actuator/beans`

Configure this in `application-prod.yml` and `application-staging.yml`:
- `management.endpoints.web.exposure.include`: health, info, metrics, loggers, flyway
- `management.endpoint.health.show-details`: when-authorized
- `management.endpoint.env.show-values`: when-authorized
- `management.server.port`: Consider running Actuator on a separate port (e.g., 8081) in production so Nginx can block external access to it. If too complex for the graduation project, keep it on 8082 but secure with Spring Security RBAC.

**1.2 Health Indicator Customization**

Spring Boot Actuator's `/actuator/health` should include health indicators for:

- **Database (PostgreSQL)**: Auto-detected by Spring Boot — verify it works
- **Redis**: Auto-detected — verify it works
- **RabbitMQ**: Auto-detected — verify it works
- **Extraction Service (Python FastAPI)**: Create a custom health indicator that calls the Python service's `/health` endpoint. If the Python service is unreachable, report it as DOWN with the reason. This tells the admin at a glance if the LLM extraction pipeline is available.
- **Disk Space**: Auto-detected — useful for monitoring the file upload directory

**1.3 Application Info Endpoint**

Configure `/actuator/info` to include:
- Application name and description
- Build version (from pom.xml or gradle build info)
- Git commit hash (if using `git-commit-id-plugin` or Spring Boot's built-in `git.properties`)
- Java version
- Spring Boot version
- Active profile (staging/prod)
- Build timestamp

This is very useful for verifying which version is deployed after a deployment.

**1.4 Custom Metrics**

In addition to the built-in Actuator metrics, register custom metrics using Micrometer (Spring Boot's metrics library):

- **`invoice.extraction.total`** (Counter): Total number of extraction requests sent to the Python service
- **`invoice.extraction.success`** (Counter): Successful extractions
- **`invoice.extraction.failure`** (Counter): Failed extractions (all providers failed)
- **`invoice.extraction.duration`** (Timer): Time taken for extraction (from request sent to result received via RabbitMQ)
- **`invoice.verification.total`** (Counter): Total invoices verified manually
- **`invoice.upload.total`** (Counter): Total file uploads
- **`llm.provider.usage`** (Counter, tagged by provider: gemini/gpt/claude): Which LLM provider was used
- **`llm.provider.fallback`** (Counter): How many times fallback was triggered
- **`notification.sent`** (Counter, tagged by channel: in_app/email/push): Notifications sent per channel
- **`login.attempt`** (Counter, tagged by result: success/failure): Login attempts
- **`login.lockout`** (Counter): Account lockouts triggered
- **`llm.cost.estimated`** (Counter, tagged by provider): Estimated cost in USD per provider (incremented by estimated cost of each call)

These counters are incremented in the existing service code by injecting `MeterRegistry` and calling `.increment()`. This requires small additions to existing service classes.

---

### 2. Structured Logging — Spring Boot

**2.1 JSON Log Format for Production**

Configure Logback (Spring Boot's default logging) to output structured JSON in production:

Create `logback-spring.xml` in `src/main/resources/`:
- **Development profile**: Keep the default human-readable console output (colored, readable)
- **Staging and Production profiles**: Output JSON-formatted log lines

Each JSON log line should include these fields:
- `timestamp`: ISO 8601 format
- `level`: LOG level (INFO, WARN, ERROR, DEBUG)
- `logger`: Logger name (class name)
- `message`: Log message
- `thread`: Thread name
- `service`: "fatura-ocr-backend" (hardcoded)
- `environment`: Active Spring profile (staging/prod)
- `correlationId`: Request correlation ID (see 2.2)
- `userId`: Authenticated user's ID (if available)
- `companyId`: User's company ID (if available)
- `traceId`: For tracing across services (see 2.3)

Use Logstash Logback Encoder (`net.logstash.logback:logstash-logback-encoder`) for JSON formatting. This is the standard library for structured logging in Spring Boot.

**2.2 Correlation ID (Request ID)**

Implement a correlation ID that tracks a request from Nginx through the backend:

- Create a servlet filter (or Spring interceptor) that:
  1. Checks for an incoming `X-Request-ID` header (set by Nginx)
  2. If present, uses it as the correlation ID
  3. If absent, generates a UUID
  4. Stores it in MDC (Mapped Diagnostic Context) so all log lines within the request include it
  5. Adds `X-Request-ID` to the response headers
  6. Clears MDC after the request completes

- When the backend calls the Python extraction service (via RabbitMQ or HTTP), include the correlation ID in the message/header so logs can be traced across services

**2.3 Cross-Service Tracing**

For requests that span multiple services (e.g., file upload → backend → RabbitMQ → Python extraction → RabbitMQ → backend):

- Pass the correlation ID in RabbitMQ message headers
- The Python service (Phase 13) already has request ID middleware — ensure it reads the correlation ID from incoming messages and includes it in its logs
- This allows an admin to search for a single correlation ID and see the entire request flow across all services

**2.4 Key Log Events**

Ensure the following events are logged at appropriate levels:

| Event | Level | Context Fields |
|---|---|---|
| Application startup | INFO | version, profile, port |
| Application shutdown | INFO | reason |
| User login success | INFO | userId, email, IP |
| User login failure | WARN | email, IP, reason, failureCount |
| Account locked | WARN | email, IP, lockReason |
| Invoice created (manual) | INFO | invoiceId, userId, companyId |
| Invoice extraction requested | INFO | invoiceId, fileName, fileSize |
| Extraction completed | INFO | invoiceId, provider, confidenceScore, duration |
| Extraction failed (all providers) | ERROR | invoiceId, fileName, providers tried, errors |
| Invoice verified/rejected | INFO | invoiceId, userId, previousStatus, newStatus |
| Rule executed | INFO | ruleId, invoiceId, action, result |
| Template learned | INFO | templateId, supplierId, sampleCount |
| Export generated | INFO | format, recordCount, userId, duration |
| Notification sent | DEBUG | type, channel, userId |
| Rate limit exceeded | WARN | userId, IP, endpoint, limit |
| Database connection pool exhausted | ERROR | active, idle, waiting |
| Redis connection failure | ERROR | host, port, error |
| RabbitMQ connection failure | ERROR | host, port, error |
| Unhandled exception | ERROR | exception class, message, stacktrace (in a separate field, not inline) |
| Slow query detected | WARN | query, duration, threshold |
| KVKK data access | INFO | userId, dataType, action (for compliance audit) |
| LLM API call completed | INFO | provider, model, inputTokens, outputTokens, estimatedCostUsd, durationMs, success |
| LLM budget threshold reached | WARN | companyId, currentCost, monthlyLimit, percentUsed |
| LLM budget exceeded | ERROR | companyId, currentCost, monthlyLimit |

Most of these log statements already exist in the codebase from previous phases. This task is about ensuring they follow the structured format and include the right context fields. Review existing log statements and enhance them where needed.

---

### 3. Structured Logging — Python FastAPI (Enhancement)

Phase 13 already configured structured logging with `structlog` for the extraction service. This phase adds enhancements for cross-service tracing:

**3.1 Correlation ID from RabbitMQ Messages**

When the Python service receives an extraction request via RabbitMQ:
- Read the `X-Correlation-ID` from the message headers (set by Spring Boot in Phase 19)
- Bind it to the structlog context so all subsequent log lines include it
- When publishing the extraction result back to RabbitMQ, include the same correlation ID in the response message headers

**3.2 LLM API Call Logging**

Ensure the following is logged for every LLM API call:
- Provider name (Gemini, GPT, Claude)
- Request timestamp
- Response timestamp and duration
- Status (success/failure)
- Token usage (if available from the API response)
- Confidence score of the result
- Whether this was a primary call or a fallback
- Correlation ID linking it back to the original invoice

**3.3 Log Output to File**

In production, configure the Python service to write logs to a file in addition to stdout:
- Log file path: `/var/log/extraction-service/extraction.log` (mount as a Docker volume)
- Log rotation: by size (10MB) or daily
- Keep last 7 log files

---

### 4. Centralized Log Collection

**4.1 File-Based Approach (Primary — implement this)**

For the graduation project, implement a practical file-based log aggregation:

- All services write JSON logs to mounted Docker volumes:
  - Backend logs: `./logs/backend/`
  - Extraction service logs: `./logs/extraction/`
  - Nginx access/error logs: `./logs/nginx/`
  - Frontend logs: `./logs/frontend/` (Next.js server-side logs if applicable)

- Update `docker-compose.prod.yml` to mount these log volumes
- Create a log rotation configuration (logrotate) for the host machine, or document that Docker's built-in log rotation should be configured

Configure Docker's logging driver for all services in docker-compose.prod.yml:
- Use `json-file` driver (Docker's default)
- Set `max-size: "10m"` and `max-file: "5"` to prevent disk exhaustion
- This ensures even if the application-level log rotation fails, Docker itself will not let logs grow unbounded

**4.2 ELK Stack / Loki (Optional — document only)**

Create a documentation file `docs/deployment/log-aggregation-options.md` that describes:

- **Option A: ELK Stack (Elasticsearch + Logstash + Kibana)**
  - What each component does
  - How to add it to docker-compose (Elasticsearch, Logstash reading from log files, Kibana for visualization)
  - Estimated resource requirements (note: ELK is heavy — 2-4 GB RAM for Elasticsearch alone)
  - Not recommended for the graduation project due to resource requirements, but good for future reference

- **Option B: Grafana Loki + Promtail**
  - Lighter weight alternative to ELK
  - Loki for log storage, Promtail for log shipping, Grafana for visualization
  - Docker Compose example (commented out, ready to enable)
  - Estimated resource requirements (much lighter than ELK)

- **Option C: Simple grep-based log search (current approach)**
  - How to search logs with `docker compose logs`, `grep`, and `jq` (for JSON logs)
  - Example commands for common searches:
    - Find all logs for a specific correlation ID
    - Find all errors in the last hour
    - Find all extraction failures
    - Find all login attempts from a specific IP

---

### 5. Alerting System

**5.1 Alert Service — Spring Boot**

Create an `AlertService` in the backend that can send alerts when critical events occur:

- Reuse the email sending infrastructure from Phase 28 (SMTP/SendGrid)
- Add optional Slack webhook support (send a JSON payload to a Slack incoming webhook URL)

Alert channels configuration (via application.yml):
- `app.alerts.email.enabled`: true/false
- `app.alerts.email.recipients`: comma-separated admin email addresses
- `app.alerts.slack.enabled`: true/false
- `app.alerts.slack.webhook-url`: Slack incoming webhook URL

**5.2 Alert Triggers**

Define the critical events that should trigger alerts:

| Alert | Severity | Trigger Condition | Channels |
|---|---|---|---|
| Extraction Service Down | CRITICAL | Custom health indicator reports DOWN for extraction service for more than 2 consecutive checks | Email, Slack |
| All LLM Providers Failed | HIGH | An extraction request fails on all 3 LLM providers (Gemini, GPT, Claude) | Email, Slack |
| Database Connection Pool Exhausted | CRITICAL | HikariCP active connections reach max pool size | Email, Slack |
| Redis Connection Failure | HIGH | Redis health check fails | Email, Slack |
| RabbitMQ Connection Failure | HIGH | RabbitMQ health check fails | Email, Slack |
| High Error Rate | HIGH | More than 10 unhandled exceptions in a 5-minute window | Email, Slack |
| Disk Space Low | WARN | Disk usage exceeds 85% on the file upload volume | Email |
| Login Brute Force Detected | WARN | More than 20 failed login attempts from a single IP in 10 minutes | Email, Slack |
| Database Backup Failed | CRITICAL | Backup script exits with non-zero code (integrate with Phase 39 backup script) | Email, Slack |
| LLM Budget Warning | WARN | Monthly LLM cost reaches alert threshold percentage (default 80%) | Email, Slack |
| LLM Budget Exceeded | CRITICAL | Monthly LLM cost exceeds 100% of configured budget | Email, Slack |
| LLM Daily Limit Exceeded | HIGH | Daily LLM cost exceeds configured daily limit | Email |

**5.3 Alert Implementation Approach**

For each trigger, decide on the implementation:

- **Health-check based alerts** (Extraction Down, DB Pool, Redis, RabbitMQ): Create a scheduled task (`@Scheduled`) that runs every 60 seconds, calls `/actuator/health`, and checks the status of each component. If a component is DOWN for N consecutive checks (configurable, default 2), fire an alert. When it recovers, fire a recovery alert.

- **Event-based alerts** (All LLM Failed, High Error Rate, Login Brute Force, LLM Budget): These are triggered inline in the existing code. When the event occurs, call `alertService.sendAlert(...)` immediately. For rate-based alerts (error rate, brute force), use a simple in-memory counter with a sliding time window (or use Micrometer's `Timer` / `Counter` with a `@Scheduled` evaluator).

- **External alerts** (Disk Space, Backup Failed): These come from outside the Spring Boot application. For disk space, the scheduled health check can read from Actuator's disk space indicator. For backup failure, the backup script (Phase 39) can call a webhook endpoint on the backend: `POST /api/v1/internal/alerts/backup-failed` (secured with an internal API key or IP restriction).

**5.4 Alert Deduplication**

Avoid alert storms:
- Do not send the same alert more than once per 15-minute window
- Track last alert time per alert type in memory (or Redis)
- Include a "mute duration" configurable per alert type

**5.5 Alert Log**

Log all sent alerts to the database for audit purposes:
- Create a simple `alert_log` table (or use the existing `audit_logs` table with a new action type)
- Fields: alert_type, severity, message, channel (email/slack), recipient, sent_at, resolved_at
- Provide a `GET /api/v1/admin/alerts` endpoint (ADMIN only) to list recent alerts

Note on database changes: If you create an `alert_log` table, create a Flyway migration file for it. Keep it simple — this is the only expected database change in this phase (along with the `llm_api_usage` table in section 9).

**5.6 Slack Webhook Format**

When sending to Slack, format the message as a Slack Block Kit message:
- Color-coded by severity (red for CRITICAL, orange for HIGH, yellow for WARN)
- Include: alert title, description, timestamp, affected service, recommended action
- Include a "View Dashboard" link pointing to the application's admin panel

**5.7 Email Alert Format**

When sending email alerts, use a simple HTML template:
- Subject: "[Fatura OCR] SEVERITY — Alert Title"
- Body: Alert details, timestamp, affected service, recommended action
- Reuse the existing email template infrastructure from Phase 28

---

### 6. LLM Service Monitoring (Extraction Service Health)

**6.1 LLM API Availability Tracking**

The Python extraction service (Phase 13) already has a `/health/dependencies` endpoint that checks LLM API key availability. Enhance this:

- Periodically (every 5 minutes) make a lightweight test call to each LLM API to verify not just key presence but actual API reachability. This could be a minimal request (e.g., a simple "hello" prompt with max 10 tokens) to each provider.
- Or, alternatively, simply check the HTTP endpoint of each provider (a HEAD request or equivalent) without consuming tokens.
- Store the last check result and timestamp in memory.
- Expose via `GET /health/dependencies` with richer information: status (UP/DOWN), last_check_time, last_response_time_ms, consecutive_failures

**6.2 Backend-Side LLM Monitoring**

In the Spring Boot backend, the custom health indicator (section 1.2) already checks if the Python service is reachable. Additionally:

- Track extraction success/failure rates using the Micrometer counters (section 1.4)
- If the failure rate exceeds a threshold (e.g., 50% of extractions failing in the last hour), trigger a HIGH alert
- Expose these metrics via the existing `/api/v1/admin/performance/health` endpoint (Phase 38)

**6.3 Automatic Restart (Docker-Level)**

Docker Compose already has `restart: always` for the extraction service (from Phase 39). This handles process crashes. For the graduation project, this is sufficient.

Document that in a production environment, a more sophisticated approach would be used:
- Kubernetes liveness/readiness probes
- Container orchestration with automatic scaling
- Circuit breaker patterns for LLM API calls

---

### 7. Nginx Access and Error Logging

**7.1 Structured Nginx Access Log**

Update the Nginx configuration (from Phase 39) to output structured JSON access logs:

Configure a custom `log_format` in `nginx.conf` that outputs JSON with fields:
- timestamp
- remote_addr (client IP)
- request_method
- request_uri
- status (HTTP status code)
- body_bytes_sent
- request_time (total request duration)
- upstream_response_time (time the backend took)
- http_user_agent
- http_referer
- request_id (the X-Request-ID header)

This enables log analysis with `jq` or future log aggregation tools.

**7.2 Nginx Error Log**

Configure Nginx error log at `warn` level in production. Mount the error log to the host via Docker volume alongside access logs.

---

### 8. Monitoring Dashboard Endpoint (Admin API)

**8.1 System Status Dashboard Endpoint**

Create (or enhance the Phase 38 endpoint) `GET /api/v1/admin/system/status` (ADMIN only) that returns a comprehensive system status:

- **Services**: Health status of each service (backend, extraction, PostgreSQL, Redis, RabbitMQ) — sourced from Actuator health
- **Metrics Summary**:
  - Total invoices processed today
  - Extraction success rate (last 24 hours)
  - Average extraction duration (last 24 hours)
  - Active users today (distinct login count)
  - Pending invoices (PENDING status count)
- **Resource Usage**:
  - JVM heap usage (used/max)
  - DB connection pool (active/idle/max)
  - Redis connection pool stats
  - Disk usage on upload directory
- **LLM Cost Summary** (from section 9):
  - Current month total cost (USD)
  - Monthly budget limit and percentage used
  - Today's cost
  - Cost breakdown by provider
- **Recent Alerts**: Last 10 alerts from the alert log
- **Uptime**: Backend uptime, last restart time

This endpoint provides a single API call that the frontend admin dashboard (or a future monitoring UI) can use to display system health.

---

### 9. LLM API Cost Monitoring & Budget Alerting

The technical report explicitly requires "API maliyet kontrolü — kullanım izleme, bütçe uyarıları" (API cost control — usage monitoring, budget alerts). This section tracks LLM API spending, enforces budget limits, and triggers alerts when thresholds are approached or exceeded.

**9.1 LLM Usage Tracking Table**

Create a new database table to persist LLM API usage data. Each row represents one LLM API call (successful or failed).

Table name: `llm_api_usage`

Fields:
- `id` (UUID, PK, default gen_random_uuid())
- `company_id` (UUID, NOT NULL, FK → companies)
- `provider` (VARCHAR(20), NOT NULL) — GEMINI, GPT, CLAUDE
- `model` (VARCHAR(50), NOT NULL) — gemini-3-flash-preview, gpt-5.2, claude-haiku-4-5
- `request_type` (VARCHAR(30), NOT NULL) — EXTRACTION, RE_EXTRACTION, VALIDATION
- `input_tokens` (INTEGER, nullable) — tokens sent (if available from SDK)
- `output_tokens` (INTEGER, nullable) — tokens received (if available from SDK)
- `estimated_cost_usd` (DECIMAL(10,6)) — estimated cost in USD for this single call
- `success` (BOOLEAN, NOT NULL, default true)
- `duration_ms` (INTEGER) — API call duration in milliseconds
- `invoice_id` (UUID, nullable, FK → invoices)
- `correlation_id` (VARCHAR(36), nullable) — links to the request correlation ID
- `created_at` (TIMESTAMP, NOT NULL, default NOW())

Indexes:
- `idx_llm_usage_company_date` ON (company_id, created_at) — for monthly/daily aggregation queries
- `idx_llm_usage_provider` ON (provider, created_at) — for per-provider analytics

**9.2 Cost Estimation Logic (Python Extraction Service)**

Enhance the Python extraction service to report usage data after every LLM API call:

- After each LLM call (successful or failed), collect: provider, model, input_tokens, output_tokens, duration_ms, success status
- Calculate `estimated_cost_usd` based on configurable per-token pricing. Default pricing (environment variables):
  - `LLM_COST_GEMINI_INPUT_PER_1K`: 0.00015
  - `LLM_COST_GEMINI_OUTPUT_PER_1K`: 0.0006
  - `LLM_COST_GPT_INPUT_PER_1K`: 0.005
  - `LLM_COST_GPT_OUTPUT_PER_1K`: 0.015
  - `LLM_COST_CLAUDE_INPUT_PER_1K`: 0.0008
  - `LLM_COST_CLAUDE_OUTPUT_PER_1K`: 0.004
- If the LLM SDK does not provide token counts, estimate input tokens based on image size (average ~1000 tokens per image) and output tokens based on response length
- Send usage data to Spring Boot via a new internal endpoint: `POST /api/v1/internal/llm-usage` (secured with internal API key or IP restriction, same pattern as the backup webhook in section 5.3)
- The request body should include: provider, model, request_type, input_tokens, output_tokens, estimated_cost_usd, success, duration_ms, invoice_id, correlation_id
- Spring Boot persists this data to the `llm_api_usage` table

**9.3 Budget Configuration**

Add budget configuration to application settings (application.yml):

- `app.llm.budget.monthly-limit-usd`: Monthly budget limit in USD (default: 50.0)
- `app.llm.budget.daily-limit-usd`: Daily budget limit in USD (default: 5.0)
- `app.llm.budget.alert-threshold-percent`: Percentage at which to send a warning alert (default: 80)
- `app.llm.budget.hard-limit-enabled`: Whether to block new extractions when budget is exceeded (default: false)

These values should also be configurable via environment variables for production:
- `LLM_BUDGET_MONTHLY_LIMIT_USD`
- `LLM_BUDGET_DAILY_LIMIT_USD`
- `LLM_BUDGET_ALERT_THRESHOLD_PERCENT`
- `LLM_BUDGET_HARD_LIMIT_ENABLED`

**9.4 Budget Monitoring Service (Spring Boot)**

Create `LlmCostMonitoringService.java` in `infrastructure/monitoring/`:

Key methods:
- `recordUsage(LlmUsageRecord record)` — persists a usage record and checks budget thresholds
- `getDailyUsage(UUID companyId, LocalDate date)` — returns total cost for a specific day
- `getMonthlyUsage(UUID companyId, YearMonth month)` — returns total cost for a specific month
- `getUsageByProvider(UUID companyId, LocalDate from, LocalDate to)` — returns cost breakdown by provider
- `checkBudgetStatus(UUID companyId)` — returns a budget status object with: currentMonthCost, monthlyLimit, percentUsed, isOverBudget, isWarning

When `recordUsage` is called:
1. Persist the record to `llm_api_usage`
2. Query the current month's total cost for the company
3. If total cost >= `alert-threshold-percent` of monthly limit → trigger LLM_BUDGET_WARNING alert (via AlertService from section 5)
4. If total cost >= monthly limit → trigger LLM_BUDGET_EXCEEDED alert
5. Query today's total cost — if it exceeds daily limit → trigger LLM_DAILY_LIMIT alert

**9.5 Budget Check Before Extraction (Optional Hard Limit)**

If `hard-limit-enabled` is true:
- Before the backend sends an extraction request to the Python service (in the file upload flow, Phase 20), check the budget status
- If the budget is exceeded, do NOT send the extraction request
- Instead, save the invoice with status `PENDING_MANUAL` (or a new status) and return a response indicating that the LLM budget has been exceeded and the invoice requires manual data entry
- The frontend (Phase 21/22) should handle this response and show an appropriate message: "Aylık LLM bütçe limiti aşıldı. Fatura verileri manuel olarak girilmelidir."

For the graduation project, `hard-limit-enabled` defaults to false. The feature is implemented but disabled — demonstrating awareness of cost control without disrupting the demo.

**9.6 LLM Usage Dashboard Endpoints**

Create endpoints for admin users to view LLM usage and costs:

**`GET /api/v1/admin/llm-usage/summary`** (ADMIN, MANAGER)

Returns a summary of LLM usage and cost for the current company:

Response structure:
- `currentMonth`: totalCostUsd, monthlyLimitUsd, percentUsed, totalRequests, successfulRequests, failedRequests
- `today`: totalCostUsd, dailyLimitUsd, totalRequests
- `byProvider`: array of { provider, requests, costUsd, avgDurationMs }
- `dailyTrend`: array of { date, costUsd, requests } for the current month (one entry per day)

**`GET /api/v1/admin/llm-usage/details`** (ADMIN only)

Paginated list of individual LLM API calls. Supports filters:
- `provider` (GEMINI, GPT, CLAUDE)
- `dateFrom`, `dateTo`
- `success` (true/false)
- `invoiceId`

Standard pagination: page, size, sort (default: created_at DESC)

**`POST /api/v1/internal/llm-usage`** (Internal only — secured with API key or IP restriction)

Receives usage data from the Python extraction service. This is NOT a user-facing endpoint.

**9.7 Python Service Environment Variables**

Add to the extraction service configuration:

- `LLM_COST_GEMINI_INPUT_PER_1K`: default 0.00015
- `LLM_COST_GEMINI_OUTPUT_PER_1K`: default 0.0006
- `LLM_COST_GPT_INPUT_PER_1K`: default 0.005
- `LLM_COST_GPT_OUTPUT_PER_1K`: default 0.015
- `LLM_COST_CLAUDE_INPUT_PER_1K`: default 0.0008
- `LLM_COST_CLAUDE_OUTPUT_PER_1K`: default 0.004
- `LLM_USAGE_REPORTING_ENABLED`: true (default)
- `LLM_USAGE_REPORT_ENDPOINT`: http://backend:8080/api/v1/internal/llm-usage
- `LLM_USAGE_INTERNAL_API_KEY`: shared secret for authenticating to the internal endpoint

---

## FILE STRUCTURE

After completing this phase, the following files should be created or modified:

```
fatura-ocr-system/
├── backend/
│   └── src/main/
│       ├── java/com/faturaocr/
│       │   ├── infrastructure/
│       │   │   ├── monitoring/
│       │   │   │   ├── ExtractionServiceHealthIndicator.java    # NEW
│       │   │   │   ├── CustomMetricsConfig.java                 # NEW
│       │   │   │   ├── SystemStatusService.java                 # NEW
│       │   │   │   └── LlmCostMonitoringService.java            # NEW (section 9)
│       │   │   ├── alerting/
│       │   │   │   ├── AlertService.java                        # NEW
│       │   │   │   ├── AlertType.java                           # NEW (enum — includes LLM_BUDGET_WARNING, LLM_BUDGET_EXCEEDED, LLM_DAILY_LIMIT)
│       │   │   │   ├── AlertSeverity.java                       # NEW (enum)
│       │   │   │   ├── SlackAlertSender.java                    # NEW
│       │   │   │   ├── EmailAlertSender.java                    # NEW
│       │   │   │   └── HealthCheckAlertScheduler.java           # NEW
│       │   │   └── logging/
│       │   │       ├── CorrelationIdFilter.java                 # NEW
│       │   │       └── LoggingConstants.java                    # NEW
│       │   ├── interfaces/
│       │   │   └── rest/
│       │   │       ├── AdminMonitoringController.java           # NEW (or enhance existing admin controller)
│       │   │       └── InternalLlmUsageController.java          # NEW (POST /internal/llm-usage)
│       │   └── domain/
│       │       └── monitoring/
│       │           └── entity/
│       │               └── LlmApiUsage.java                     # NEW (entity for llm_api_usage table)
│       └── resources/
│           ├── logback-spring.xml                               # NEW
│           ├── application-staging.yml                          # MODIFIED (Actuator config, LLM budget config)
│           └── application-prod.yml                             # MODIFIED (Actuator config, LLM budget config)
├── extraction-service/
│   └── app/
│       ├── core/
│       │   └── logging.py                                       # MODIFIED (correlation ID from RabbitMQ)
│       └── services/
│           └── usage_reporter.py                                # NEW (collects token usage, calculates cost, sends to Spring Boot)
├── nginx/
│   └── nginx.conf                                               # MODIFIED (JSON access log format)
├── docker-compose.prod.yml                                      # MODIFIED (log volumes, Docker log config)
├── docs/
│   └── deployment/
│       ├── log-aggregation-options.md                           # NEW
│       └── monitoring-guide.md                                  # NEW
└── scripts/
    └── backup-db.sh                                             # MODIFIED (webhook call on failure)
```

---

## DATABASE CHANGES

This phase requires TWO migrations:

**Migration file 1**: `V{next}__phase_40_alert_log.sql`

`alert_log` table:
- id (BIGSERIAL PRIMARY KEY)
- alert_type (VARCHAR, NOT NULL) — e.g., EXTRACTION_SERVICE_DOWN, ALL_LLM_FAILED, LLM_BUDGET_WARNING, LLM_BUDGET_EXCEEDED, LLM_DAILY_LIMIT
- severity (VARCHAR, NOT NULL) — CRITICAL, HIGH, WARN
- message (TEXT, NOT NULL)
- channel (VARCHAR, NOT NULL) — EMAIL, SLACK
- recipient (VARCHAR) — email address or slack channel
- sent_at (TIMESTAMP, NOT NULL)
- resolved_at (TIMESTAMP, nullable)
- metadata (JSONB, nullable) — extra context

Index on `(sent_at DESC)` for listing recent alerts.
Index on `(alert_type, sent_at DESC)` for deduplication queries.

If you decide this is overkill for the graduation project, you may skip the table and log alerts only to the application log file. Document the decision. However, having the table makes the admin alerts endpoint possible and is relatively simple to implement.

**Migration file 2**: `V{next}__phase_40_llm_api_usage.sql`

`llm_api_usage` table (as described in section 9.1):
- id (UUID, PK, default gen_random_uuid())
- company_id (UUID, NOT NULL, FK → companies)
- provider (VARCHAR(20), NOT NULL)
- model (VARCHAR(50), NOT NULL)
- request_type (VARCHAR(30), NOT NULL)
- input_tokens (INTEGER, nullable)
- output_tokens (INTEGER, nullable)
- estimated_cost_usd (DECIMAL(10,6))
- success (BOOLEAN, NOT NULL, default true)
- duration_ms (INTEGER)
- invoice_id (UUID, nullable, FK → invoices)
- correlation_id (VARCHAR(36), nullable)
- created_at (TIMESTAMP, NOT NULL, default NOW())

Index on `(company_id, created_at)` for monthly/daily aggregation.
Index on `(provider, created_at)` for per-provider analytics.

---

## TESTING REQUIREMENTS

### Unit Tests

1. **AlertService tests**: Test that alerts are sent to the correct channels based on configuration. Mock the email and Slack senders.
2. **HealthCheckAlertScheduler tests**: Test that alerts are triggered when health checks fail N consecutive times, and that recovery alerts are sent.
3. **Alert deduplication tests**: Test that the same alert is not sent twice within the mute window.
4. **CorrelationIdFilter tests**: Test that correlation ID is read from header, generated if absent, stored in MDC, and added to response.
5. **ExtractionServiceHealthIndicator tests**: Test UP when Python service responds, DOWN when it doesn't.
6. **SystemStatusService tests**: Test that it aggregates data from Actuator, metrics, and DB correctly.
7. **LlmCostMonitoringService tests**: Test cost calculation for each provider with known token counts. Test budget threshold detection (below warning, at warning, over limit). Test that alert is triggered when monthly budget threshold is reached. Test daily limit detection.
8. **Usage reporting tests**: Test that usage records are correctly persisted. Test aggregation queries (daily, monthly, by provider).

### Integration Tests

1. Test that the `/actuator/health` endpoint includes the custom extraction service health indicator.
2. Test that the `/api/v1/admin/system/status` endpoint returns all expected fields including LLM cost summary.
3. Test that the `/api/v1/admin/alerts` endpoint returns alert log entries.
4. Test that the correlation ID filter passes the ID through the entire request-response cycle.
5. Test that `POST /api/v1/internal/llm-usage` persists a usage record and returns 201.
6. Test that `GET /api/v1/admin/llm-usage/summary` returns correct aggregations for current month, today, and by provider.
7. Test that `GET /api/v1/admin/llm-usage/details` returns paginated results with correct filters.
8. Test that budget alert is triggered when inserting a usage record that pushes the monthly total over the threshold.

### Manual Tests

1. Stop the Python extraction service container → verify that within 2 minutes, the health indicator shows DOWN and an alert is triggered.
2. Restart the Python service → verify the recovery alert is sent.
3. Check that backend logs are in JSON format when running with the production profile.
4. Search for a specific correlation ID across backend and extraction service logs to verify cross-service tracing works.
5. Verify Nginx access logs are in JSON format.
6. Insert several LLM usage records to push monthly cost over the alert threshold → verify the budget warning alert is triggered.
7. Verify the `/api/v1/admin/llm-usage/summary` endpoint returns accurate cost data after inserting test usage records.

---

## RESULT FILE

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_40_result.md`

The result file must include:

1. Phase summary (what was implemented)
2. Files created or modified (full list with paths)
3. Database migration details (alert_log table, llm_api_usage table)
4. Actuator configuration summary (which endpoints exposed, access control)
5. Custom health indicators implemented
6. Custom Micrometer metrics registered (list with descriptions)
7. Structured logging configuration (JSON format details, MDC fields)
8. Correlation ID flow description (Nginx → Backend → RabbitMQ → Python → Response)
9. Alert types implemented with trigger conditions
10. Alert channels configured (email, Slack)
11. Alert deduplication strategy
12. LLM API cost monitoring details (usage table, cost calculation logic, budget config)
13. LLM usage dashboard endpoint response examples
14. Log volume mounts in Docker Compose
15. Nginx structured access log format
16. System status endpoint response example
17. Log search example commands (grep/jq for common queries)
18. Test results (unit, integration, manual)
19. Documentation files created
20. Issues encountered and solutions
21. Next steps (Phase 41 API Documentation)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 1**: CI/CD Pipeline (to add monitoring-related test steps)
- **Phase 13**: Python FastAPI service with structured logging and health endpoints
- **Phase 15-16**: LLM integration with token usage logging
- **Phase 19**: RabbitMQ messaging (correlation ID in message headers)
- **Phase 27-28**: Notification infrastructure (email sending, SMTP config)
- **Phase 38**: Performance optimization (existing Actuator setup, performance health endpoint)
- **Phase 39**: Staging/Production deployment (docker-compose.prod.yml, Nginx config, backup script)

### Required By
- **Phase 41**: API Documentation (LLM usage endpoints need to be documented)
- **Phase 42**: User Guide (references monitoring capabilities and LLM cost dashboard for admin documentation)

---

## VERIFICATION CHECKLIST

Before marking this phase as complete, verify every item:

- [ ] Spring Boot Actuator is secured: public sees only UP/DOWN, ADMIN sees full details
- [ ] Custom health indicator for Python extraction service returns UP/DOWN correctly
- [ ] `/actuator/info` shows build version, git commit, active profile
- [ ] All custom Micrometer metrics are registered and increment correctly
- [ ] `logback-spring.xml` outputs JSON in staging/production, human-readable in development
- [ ] Correlation ID filter generates/reads UUID and adds to MDC and response header
- [ ] Correlation ID is passed through RabbitMQ to Python service and back
- [ ] Cross-service log tracing works (search one correlation ID → see logs in both services)
- [ ] Python extraction service reads correlation ID from RabbitMQ message headers
- [ ] AlertService sends email alerts correctly
- [ ] AlertService sends Slack webhook alerts correctly (or is implemented but disabled)
- [ ] Health-check alerts fire when services go DOWN (after N consecutive failures)
- [ ] Recovery alerts fire when services come back UP
- [ ] Alert deduplication prevents duplicate alerts within mute window
- [ ] Alert log table created and queryable via `GET /api/v1/admin/alerts`
- [ ] `GET /api/v1/admin/system/status` returns comprehensive status including LLM cost summary
- [ ] Nginx JSON access logs are configured and working
- [ ] Docker Compose production file updated with log volumes and log driver limits
- [ ] Backup script calls alert webhook on failure
- [ ] `llm_api_usage` table created with correct schema and indexes
- [ ] Python service calculates estimated cost after each LLM call using configurable pricing
- [ ] Python service sends usage data to `POST /api/v1/internal/llm-usage`
- [ ] `LlmCostMonitoringService` persists usage records and checks budget thresholds
- [ ] Budget warning alert triggers at configured threshold percentage
- [ ] Budget exceeded alert triggers when monthly limit is surpassed
- [ ] Daily limit alert triggers when daily cost exceeds daily limit
- [ ] `GET /api/v1/admin/llm-usage/summary` returns correct monthly, daily, and per-provider data
- [ ] `GET /api/v1/admin/llm-usage/details` returns paginated, filterable usage records
- [ ] Budget configuration is externalized via environment variables
- [ ] Hard limit feature is implemented (disabled by default) and documented
- [ ] Log aggregation options documented in `docs/deployment/log-aggregation-options.md`
- [ ] Monitoring guide documented in `docs/deployment/monitoring-guide.md`
- [ ] `POST /api/v1/admin/alerts/test` endpoint works for testing alert channels
- [ ] All existing tests still pass (no regressions)
- [ ] All new unit and integration tests pass
- [ ] Result file created at `docs/OMER/step_results/faz_40_result.md`

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ Spring Boot Actuator endpoints properly secured (public: basic health only; ADMIN: full details + metrics)
2. ✅ Custom health indicator for extraction service (Python FastAPI) works correctly
3. ✅ `/actuator/info` shows build version, git commit, active profile
4. ✅ Custom Micrometer metrics registered for extraction, verification, upload, login, and LLM cost events
5. ✅ Logback configured with JSON output for staging/production profiles
6. ✅ Human-readable log output preserved for development profile
7. ✅ Correlation ID filter implemented and adds X-Request-ID to all logs and responses
8. ✅ Correlation ID passed through RabbitMQ messages to Python service
9. ✅ Cross-service log tracing works (search one correlation ID → see logs in both services)
10. ✅ Python extraction service enhanced with correlation ID from RabbitMQ
11. ✅ AlertService implemented with email and Slack webhook channels
12. ✅ Health-check-based alerts fire when services go DOWN and send recovery when UP
13. ✅ Alert deduplication prevents alert storms (same alert muted for 15 minutes)
14. ✅ Alert log stored (in DB table or application logs) and queryable via admin endpoint
15. ✅ System status endpoint (`/api/v1/admin/system/status`) returns comprehensive status including LLM cost data
16. ✅ Nginx configured with JSON-structured access logs
17. ✅ Docker Compose production file updated with log volume mounts and Docker log driver limits
18. ✅ Backup script (Phase 39) enhanced to call alert webhook on failure
19. ✅ `llm_api_usage` table created and Python service reports usage data after every LLM call
20. ✅ LLM cost estimation works with configurable per-token pricing per provider
21. ✅ Budget monitoring triggers alerts at warning threshold and when budget is exceeded
22. ✅ `GET /api/v1/admin/llm-usage/summary` returns monthly, daily, and per-provider cost data
23. ✅ `GET /api/v1/admin/llm-usage/details` returns paginated usage records with filters
24. ✅ Budget hard limit feature implemented (disabled by default)
25. ✅ Log aggregation options documented (ELK, Loki, grep-based)
26. ✅ Monitoring guide documentation created
27. ✅ All existing tests still pass (no regressions)
28. ✅ New unit and integration tests pass
29. ✅ Result file created at docs/OMER/step_results/faz_40_result.md

---

## IMPORTANT NOTES

1. **Do NOT Over-Engineer**: This is a graduation project. A full Prometheus + Grafana + AlertManager stack is impressive but likely overkill and resource-heavy. The approach in this prompt (Actuator metrics, file-based logging, simple email/Slack alerts) is practical, demonstrable, and appropriate for the project scope. Document the more advanced options for the graduation report but implement the simpler version.

2. **Do NOT Break Existing Functionality**: Adding logging, metrics, and health checks should be invisible to end users. The application must work exactly as before. Run all existing tests after changes.

3. **Logstash Logback Encoder Dependency**: Add `net.logstash.logback:logstash-logback-encoder` to `pom.xml`. This is the standard library for JSON logging in Spring Boot. Do not reinvent JSON formatting.

4. **Slack Webhook is Optional**: If setting up a Slack workspace is not feasible, implement the Slack sender code but mark it as disabled by default. The email alert channel is the minimum required. The Slack integration demonstrates the extensibility of the alert system.

5. **Correlation ID is High-Value**: The correlation ID / request tracing is one of the most valuable features for debugging production issues. Prioritize getting this right — it connects logs across Nginx, Spring Boot, RabbitMQ, and Python into a single traceable flow.

6. **Coordinate with FURKAN**: The Python extraction service changes (correlation ID from RabbitMQ, enhanced health endpoint, usage reporter) affect FURKAN's code. Communicate the expected message header format for correlation ID and the usage reporting format. If FURKAN has already moved on, make the changes yourself (they are small) and document them.

7. **Metrics Are Counters, Not Dashboards**: In this phase, you register Micrometer counters and timers. These are exposed via `/actuator/metrics/{metric-name}`. You do NOT need to build a visual dashboard for them. The system status endpoint aggregates the key ones into a single API response. A visual dashboard (Grafana, etc.) is a future improvement — document it but don't implement it.

8. **Log Volume Sizes**: For the graduation demo, logs will be small. But configure Docker's log rotation (`max-size: 10m, max-file: 5`) as a best practice. This prevents a runaway logging bug from filling the disk.

9. **Alert Testing**: To test alerts without waiting for real failures, consider adding a `POST /api/v1/admin/alerts/test` endpoint (ADMIN only) that sends a test alert to all configured channels. This is very useful for verifying email/Slack connectivity during deployment.

10. **LLM Cost Prices Change**: The per-token pricing for LLM providers changes over time. That's why all pricing values are configurable via environment variables rather than hardcoded. Document the current prices and the date they were last checked. Update them before production deployment.

11. **Cost Estimation is Approximate**: The cost estimation is based on token counts from SDK responses (when available) and configurable per-token rates. It is an approximation, not a billing substitute. Document this clearly — the actual billing comes from the LLM provider dashboards (Google AI Studio, OpenAI dashboard, Anthropic console).
