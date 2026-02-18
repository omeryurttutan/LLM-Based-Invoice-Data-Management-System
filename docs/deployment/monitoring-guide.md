# Monitoring Guide

This guide details how to monitor the Fatura OCR system's health, performance, and costs.

## Health Monitoring

### Actuator Endpoints (Backend)

The backend exposes Spring Boot Actuator endpoints (secured, distinct port or path recommended in prod):

- **Health:** `/actuator/health` (Main status including DB, Redis, RabbitMQ)
- **Info:** `/actuator/info` (Build info)
- **Metrics:** `/actuator/metrics` (JVM and application metrics)

### Python Service Health

- **Endpoint:** `/health` (Returns 200 OK if service is alive)
- **Deep Check:** The backend `ExtractionServiceHealthIndicator` polls this endpoint periodically.

## Application Metrics

Key metrics are exposed via Micrometer (available at `/actuator/metrics` and for export to Prometheus):

- `invoice.upload.count`: Total invoices uploaded.
- `invoice.extraction.time`: Duration of extraction process.
- `invoice.verification.count`: Number of invoices verified.
- `user.login.count`: Number of user logins.
- `llm.cost.total`: Cumulative estimated cost of LLM usage.

## Structured Logging

Logs are output in JSON format to specific directories (`./logs/*`) and stdout.

- **Key Fields:** `correlation_id` (traces requests across services), `level` (INFO, WARN, ERROR), `logger` (component name).
- **Traceability:** Search by `correlation_id` to see the full lifecycle of a request from Nginx -> Backend -> RabbitMQ -> Python Service.

## Alerting

Alerts are triggered for critical events:

- **System Health:** If any component (DB, Redis, RabbitMQ, Extraction Service) is down.
- **LLM Budget:** If daily/monthly spending exceeds configured limits.
- **Extraction Failures:** High failure rates (configured thresholds).

**Channels:**

- Email (configured via generic EmailService)
- Slack (optional webhook configuration)

## Administration

Admin Dashboard (`/admin`) provides visual insights:

- System Status (Green/Red indicators)
- LLM Usage (Cost charts, token breakdown)
