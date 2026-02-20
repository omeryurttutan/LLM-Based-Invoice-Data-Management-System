# PHASE 19-B: RABBITMQ PRODUCER (SPRING BOOT)

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8082
  - **Python Microservice**: Port 8001 - LLM-based extraction
  - **Next.js Frontend**: Port 3001
  - **RabbitMQ**: Port 5673 (AMQP), 15672 (Management UI)

### Current State (Phases 0-18 Completed)
- ✅ Phase 0: Docker Compose environment — RabbitMQ 3.x running on port 5673, credentials in .env (RABBITMQ_USER, RABBITMQ_PASSWORD), Spring Boot already has spring-boot-starter-amqp dependency
- ✅ Phase 1-9: CI/CD, Hexagonal Architecture, Database Schema, Auth (JWT), RBAC, Company/User API, Invoice CRUD API, Audit Log, Duplication Check
- ✅ Phase 10-12: Frontend layout, auth pages, invoice CRUD UI
- ✅ Phase 13-18 (FURKAN): FastAPI service, image preprocessing, Gemini integration, fallback chain (GPT + Claude), validation & confidence score, e-Invoice XML parser

### What Exists Already
- **RabbitMQ container**: Running in Docker Compose (Phase 0), management UI at localhost:15673
- **spring-boot-starter-amqp**: Already in pom.xml (Phase 0)
- **RabbitMQ connection config**: Already in application.yml (host, port, username, password)
- **Invoice entity**: Full CRUD with status field (PENDING, VERIFIED, REJECTED, PROCESSING)
- **Python extraction service**: POST /extract endpoint accepts image/XML files and returns ExtractionResult with InvoiceData, provider name, confidence_score, validation_result

### Phase Assignment
- **Assigned To**: ÖMER (Backend Developer)
- **Estimated Duration**: 1.5 days

### Relationship to Phase 19-A
Phase 19 is split into two sub-phases:
- **19-B (this phase)**: Spring Boot RabbitMQ Producer — ÖMER sets up exchanges, queues, routing, and the message publishing logic
- **19-A**: Python RabbitMQ Consumer — FURKAN sets up the Pika consumer that listens to the queue and processes extraction requests

Both sides must agree on: exchange names, queue names, routing keys, and message JSON schema. This prompt defines those contracts.

---

## OBJECTIVE

Set up the RabbitMQ messaging infrastructure on the Spring Boot side: define exchanges, queues, dead letter queues, routing keys, and implement a producer service that publishes invoice extraction requests to the queue. This enables asynchronous processing for bulk invoice uploads — instead of waiting for each LLM extraction synchronously, files are queued and processed in the background by the Python consumer (Phase 19-A).

---

## COMMUNICATION MODEL

The system supports TWO modes of communication between Spring Boot and Python:

```
MODE 1: SYNCHRONOUS (Single file upload)
┌──────────────┐    HTTP POST /extract    ┌──────────────┐
│ Spring Boot  │ ──────────────────────► │   Python     │
│  (Backend)   │ ◄────────────────────── │  (FastAPI)   │
└──────────────┘   ExtractionResult JSON  └──────────────┘
→ Used for: Single file upload (user waits for result)
→ Already works via Phase 15-18

MODE 2: ASYNCHRONOUS (Bulk upload)
┌──────────────┐   Publish message    ┌──────────────┐   Consume    ┌──────────────┐
│ Spring Boot  │ ──────────────────► │  RabbitMQ    │ ──────────► │   Python     │
│  (Producer)  │                      │   Queue      │             │  (Consumer)  │
└──────────────┘                      └──────────────┘             └──────┬───────┘
       ▲                                                                  │
       │                    HTTP callback / status update                  │
       └──────────────────────────────────────────────────────────────────┘
→ Used for: Bulk upload (files queued, processed in background)
→ THIS PHASE sets up the producer side
```

**Decision Logic (in Spring Boot):**
- Single file upload (POST /api/v1/invoices/upload) → Synchronous HTTP call to Python
- Bulk upload (POST /api/v1/invoices/bulk-upload) → Publish messages to RabbitMQ queue

---

## DETAILED REQUIREMENTS

### 1. RabbitMQ Topology

Define the following RabbitMQ resources. Spring Boot will declare them on startup using Spring AMQP.

**Exchanges:**

| Exchange Name | Type | Durable | Description |
|---|---|---|---|
| invoice.extraction | direct | Yes | Main extraction exchange |
| invoice.extraction.dlx | direct | Yes | Dead letter exchange |
| invoice.extraction.result | direct | Yes | Result callback exchange |

**Queues:**

| Queue Name | Bound To Exchange | Routing Key | DLX | TTL | Description |
|---|---|---|---|---|---|
| invoice.extraction.queue | invoice.extraction | extraction.request | invoice.extraction.dlx | - | Main extraction request queue |
| invoice.extraction.dlq | invoice.extraction.dlx | extraction.dead | - | - | Dead letter queue for failed messages |
| invoice.extraction.result.queue | invoice.extraction.result | extraction.result | - | - | Result callback queue (Python → Spring Boot) |

**Dead Letter Configuration:**
- Messages that are rejected (nack without requeue) or exceed max retries go to the DLQ
- The DLQ stores failed messages for manual inspection and potential replay

### 2. Message Schemas

Both Spring Boot and Python must agree on these JSON message formats. Define them clearly — they are the contract between the two services.

**Extraction Request Message (Spring Boot → Python via RabbitMQ):**

| Field | Type | Required | Description |
|---|---|---|---|
| message_id | string (UUID) | Yes | Unique message identifier for tracking |
| invoice_id | long | Yes | Database invoice ID (invoices.id) |
| company_id | long | Yes | Company the invoice belongs to |
| user_id | long | Yes | User who uploaded the file |
| file_path | string | Yes | Path to the saved file on shared storage or within the container volume |
| file_name | string | Yes | Original file name |
| file_type | string | Yes | MIME type (image/jpeg, image/png, application/pdf, application/xml) |
| file_size | long | Yes | File size in bytes |
| priority | string | No | "HIGH" / "NORMAL" / "LOW" (default: "NORMAL") |
| attempt | integer | No | Current retry attempt number (default: 1) |
| max_attempts | integer | No | Maximum retry attempts (default: 3) |
| timestamp | string (ISO 8601) | Yes | When the message was published |
| correlation_id | string (UUID) | Yes | For matching requests with results |

**Extraction Result Message (Python → Spring Boot via RabbitMQ):**

| Field | Type | Required | Description |
|---|---|---|---|
| message_id | string (UUID) | Yes | Unique message identifier |
| correlation_id | string (UUID) | Yes | Matches the original request |
| invoice_id | long | Yes | Database invoice ID |
| status | string | Yes | "COMPLETED" / "FAILED" |
| invoice_data | object | If COMPLETED | The extracted InvoiceData (same as POST /extract response) |
| confidence_score | float | If COMPLETED | Validation confidence score (0-100) |
| provider | string | If COMPLETED | Which LLM was used ("GEMINI", "GPT", "CLAUDE", "XML_PARSER") |
| suggested_status | string | If COMPLETED | "AUTO_VERIFIED" / "NEEDS_REVIEW" / "LOW_CONFIDENCE" |
| error_code | string | If FAILED | Error type (TIMEOUT, RATE_LIMIT, ALL_PROVIDERS_FAILED, PARSE_ERROR, etc.) |
| error_message | string | If FAILED | Human-readable error description |
| processing_duration_ms | long | Yes | Total processing time in milliseconds |
| timestamp | string (ISO 8601) | Yes | When the result was produced |

### 3. Spring AMQP Configuration

Create a RabbitMQ configuration class that declares all exchanges, queues, and bindings on application startup.

**Requirements:**
- All exchanges are durable (survive broker restart)
- All queues are durable
- Main extraction queue has dead letter exchange and dead letter routing key arguments
- Use Spring AMQP declarative approach (Bean definitions for Exchange, Queue, Binding)

### 4. Extraction Message Producer Service

Create a service in the Spring Boot application that publishes extraction request messages.

**Responsibilities:**
- Accept invoice metadata (invoice ID, file path, file info)
- Build the extraction request message JSON
- Generate message_id and correlation_id (UUIDs)
- Set message properties: content type (application/json), delivery mode (persistent), timestamp, correlation ID
- Publish to the invoice.extraction exchange with routing key "extraction.request"
- Return the correlation_id to the caller for tracking

**Message Publishing Configuration:**
- Messages must be persistent (delivery_mode = 2) to survive broker restarts
- Content type: application/json
- Use Jackson for JSON serialization

### 5. Extraction Result Listener

Create a listener that consumes messages from the result queue (invoice.extraction.result.queue).

**Responsibilities:**
- Listen to the result queue
- Deserialize the result message
- Find the invoice by invoice_id
- If status = "COMPLETED":
  - Update the invoice entity with extracted data (invoice_number, dates, amounts, supplier info, etc.)
  - Set confidence_score
  - Set llm_provider
  - Set source_type ("LLM" or "E_INVOICE")
  - Set invoice status based on suggested_status: if "AUTO_VERIFIED" and score >= threshold → VERIFIED; otherwise → PENDING
- If status = "FAILED":
  - Check if attempt < max_attempts → republish to extraction queue with incremented attempt number
  - If max attempts exceeded → set invoice status to FAILED, log the error
- Save the updated invoice
- Trigger notification (Phase 27) if applicable: extraction completed, extraction failed, low confidence

### 6. Invoice Status Lifecycle for Async Processing

```
File Uploaded
      │
      ▼
   QUEUED ← new status (message published to RabbitMQ)
      │
      ▼ (Python consumer picks up)
  PROCESSING ← Python sends status update
      │
      ├──► COMPLETED → then based on confidence:
      │        ├──► VERIFIED (auto, score >= 90)
      │        └──► PENDING (needs review, score < 90)
      │
      └──► FAILED → check retry:
               ├──► Retry available → back to QUEUED
               └──► Max retries exceeded → FAILED (final)
```

**New status needed: QUEUED**
The invoices.status column currently has: PENDING, VERIFIED, REJECTED, PROCESSING

Add "QUEUED" to the status enum/constraint. This requires a Flyway migration to update the CHECK constraint on the status column.

Also add "FAILED" if not already present.

### 7. Retry Mechanism

**Retry Logic (handled by Spring Boot, not RabbitMQ TTL):**
- Default max retries: 3
- Exponential backoff: 1st retry after 10s, 2nd after 30s, 3rd after 60s
- When a FAILED result comes back from Python:
  - If attempt < max_attempts → wait the backoff delay, then republish with attempt + 1
  - If attempt >= max_attempts → move to DLQ (reject without requeue)
- Use Spring's scheduling or a delayed republish mechanism for backoff

**Configurable via environment variables:**
- `RABBITMQ_MAX_RETRIES`: Default 3
- `RABBITMQ_RETRY_INITIAL_DELAY_MS`: Default 10000
- `RABBITMQ_RETRY_MULTIPLIER`: Default 3.0

### 8. Shared File Storage

For async processing, the Python service needs access to the uploaded file. Two approaches:

**Option A (Recommended for Docker Compose):** Shared Docker volume
- Both Spring Boot and Python containers mount the same volume for invoice files
- Spring Boot saves the file to the shared volume path
- The file_path in the message points to the location within this shared volume
- Volume: `invoice-files:/data/invoices` mounted in both containers

**Option B (Alternative):** Base64 in message
- Embed the file content as base64 in the message
- Simpler but increases message size significantly (not recommended for large files)

**Decision:** Use Option A (shared volume). Add the shared volume to docker-compose.yml if not already present.

### 9. Database Migration

Create a Flyway migration to:
- Add "QUEUED" and "FAILED" to the invoices.status CHECK constraint (if not already there)
- Optionally add a `correlation_id VARCHAR(36)` column to the invoices table for matching async requests with results

Migration file: `V{next_number}__phase_19_async_processing_support.sql`

### 10. Configuration — Environment Variables

Add/update:
- `RABBITMQ_EXTRACTION_EXCHANGE`: Default "invoice.extraction"
- `RABBITMQ_EXTRACTION_QUEUE`: Default "invoice.extraction.queue"
- `RABBITMQ_EXTRACTION_DLQ`: Default "invoice.extraction.dlq"
- `RABBITMQ_RESULT_EXCHANGE`: Default "invoice.extraction.result"
- `RABBITMQ_RESULT_QUEUE`: Default "invoice.extraction.result.queue"
- `RABBITMQ_MAX_RETRIES`: Default 3
- `RABBITMQ_RETRY_INITIAL_DELAY_MS`: Default 10000
- `RABBITMQ_RETRY_MULTIPLIER`: Default 3.0
- `INVOICE_FILE_STORAGE_PATH`: Default "/data/invoices"

### 11. Logging

**INFO:**
- Message published to extraction queue (message_id, invoice_id, file_name)
- Result received from Python (correlation_id, invoice_id, status)
- Invoice updated with extraction result
- Retry scheduled (invoice_id, attempt number, delay)

**WARNING:**
- Extraction failed, retrying (invoice_id, error_code, attempt)
- Max retries exceeded, moving to DLQ (invoice_id)
- Result received for unknown invoice_id

**ERROR:**
- Message publishing failed (exchange, routing key, error)
- Result processing failed (deserialization error, DB error)
- DLQ message (full message content for debugging)

---

## TESTING REQUIREMENTS

### 1. Unit Tests
- Test message builder creates correct JSON structure with all required fields
- Test correlation_id and message_id are unique UUIDs
- Test retry logic increments attempt and calculates correct backoff delay
- Test status transitions: QUEUED → PROCESSING → COMPLETED/FAILED
- Test result handler updates invoice correctly for COMPLETED status
- Test result handler triggers retry for FAILED status within max attempts
- Test result handler moves to DLQ when max attempts exceeded

### 2. Integration Tests (with Testcontainers)
- Test RabbitMQ topology declared correctly on startup (exchanges, queues, bindings exist)
- Test message published to extraction queue is received
- Test message published to DLQ when rejected
- Test result listener receives and processes result messages
- Test full round-trip: publish request → receive result → invoice updated in DB

### 3. Manual Tests
- Verify exchanges and queues visible in RabbitMQ Management UI (localhost:15673)
- Verify message persistence (restart RabbitMQ, check messages survive)
- Verify DLQ receives rejected messages

---

## VERIFICATION CHECKLIST

### RabbitMQ Topology
- [ ] invoice.extraction exchange declared (direct, durable)
- [ ] invoice.extraction.dlx exchange declared
- [ ] invoice.extraction.result exchange declared
- [ ] invoice.extraction.queue bound with correct routing key
- [ ] invoice.extraction.dlq bound to DLX
- [ ] invoice.extraction.result.queue bound correctly
- [ ] Dead letter arguments set on main queue

### Producer
- [ ] Message published with all required fields
- [ ] Messages are persistent (delivery_mode = 2)
- [ ] Content type is application/json
- [ ] correlation_id set for request-result matching
- [ ] Jackson serialization produces correct JSON

### Result Listener
- [ ] Listens to result queue
- [ ] Deserializes COMPLETED results correctly
- [ ] Updates invoice with extracted data
- [ ] Handles FAILED results with retry logic
- [ ] Moves to DLQ after max retries
- [ ] Handles unknown invoice_id gracefully

### Status Management
- [ ] QUEUED status added to database constraint
- [ ] Status transitions are correct
- [ ] correlation_id stored on invoice entity

### Configuration
- [ ] All exchange/queue names configurable
- [ ] Retry parameters configurable
- [ ] Shared volume path configurable
- [ ] Docker Compose updated with shared volume

### Tests
- [ ] All unit tests pass
- [ ] Integration tests with Testcontainers pass
- [ ] Exchanges/queues visible in Management UI

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_19_b_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified with full paths
4. RabbitMQ topology screenshot from Management UI
5. Message schema examples (request and result JSON samples)
6. Database migration details
7. Test results (unit + integration)
8. Docker Compose changes (shared volume)
9. Issues encountered and solutions
10. Coordination notes for Furkan (Phase 19-A consumer implementation)
11. Next steps (Phase 20 — File Upload will use this producer)

---

## DEPENDENCIES

### Requires
- **Phase 0**: Docker Compose with RabbitMQ, Spring AMQP dependency
- **Phase 7**: Invoice CRUD API (invoice entity, repository, service)
- **Phase 3**: Database schema (invoices table)

### Required By
- **Phase 19-A**: Python RabbitMQ Consumer (FURKAN) — must match the message schemas defined here
- **Phase 20**: File Upload Infrastructure — uses the producer to queue bulk uploads
- **Phase 21**: Upload UI — shows queue status

---

## SUCCESS CRITERIA

1. ✅ All RabbitMQ exchanges, queues, and bindings created on startup
2. ✅ Extraction request messages published correctly with all fields
3. ✅ Result listener receives and processes extraction results
4. ✅ Invoice status updated correctly (QUEUED → PROCESSING → COMPLETED/FAILED)
5. ✅ Retry mechanism works (exponential backoff, max 3 retries)
6. ✅ Dead letter queue receives messages after max retries
7. ✅ Message schemas documented and agreed with FURKAN
8. ✅ Shared volume configured for file access
9. ✅ Database migration for new status values
10. ✅ All tests pass
11. ✅ Result file created

---

## IMPORTANT NOTES

1. **Coordinate with FURKAN**: The message schemas defined in this prompt are the contract. FURKAN's Phase 19-A consumer must consume these exact message formats. Share the final message schema with FURKAN before he starts.

2. **Shared Volume**: Both containers must mount the same Docker volume. Update docker-compose.yml to add a named volume mounted in both backend and extraction-service containers.

3. **Message Persistence**: All messages MUST be persistent. A RabbitMQ restart should not lose queued extraction requests.

4. **Don't Process Extraction Here**: Spring Boot only publishes and receives results. The actual LLM/XML extraction happens in Python (Phase 19-A).

5. **Idempotency**: The result listener should handle duplicate result messages gracefully (same correlation_id received twice).

6. **Status Transitions**: Only valid transitions should be allowed. Implement a simple state machine or validation: QUEUED → PROCESSING → COMPLETED/FAILED.
