# PHASE 19-A: RABBITMQ CONSUMER (PYTHON FASTAPI)

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
- ✅ Phase 0: Docker Compose — RabbitMQ running, Python service has RABBITMQ_HOST, RABBITMQ_USER, RABBITMQ_PASSWORD env vars
- ✅ Phase 13: FastAPI service setup, structured logging, CORS, Docker
- ✅ Phase 14: Image preprocessing pipeline (Pillow + PyMuPDF)
- ✅ Phase 15: Gemini integration — base provider, extraction prompt, response parser, InvoiceData model, ExtractionResult
- ✅ Phase 16: Fallback chain — GPT + Claude providers, chain manager (Gemini → GPT → Claude), provider health
- ✅ Phase 17: Validation & confidence score — 5-category validation, weighted score (0-100), suggested status
- ✅ Phase 18: E-Invoice XML parser — UBL-TR parsing, file type detection, smart routing (image → LLM, XML → parser)

### What Phase 19-B Delivers (Ömer — Spring Boot Side)
Phase 19-B (completed by Ömer) sets up the Spring Boot producer side. It defines the RabbitMQ topology and message contracts that this phase must consume:

**RabbitMQ Topology (declared by Spring Boot):**
- Exchange: `invoice.extraction` (direct, durable)
- Queue: `invoice.extraction.queue` (durable, with DLX)
- Dead Letter Exchange: `invoice.extraction.dlx`
- Dead Letter Queue: `invoice.extraction.dlq`
- Result Exchange: `invoice.extraction.result` (direct, durable)
- Result Queue: `invoice.extraction.result.queue`

**Key Routing Keys:**
- `extraction.request` — Spring Boot publishes extraction requests
- `extraction.result` — Python publishes extraction results
- `extraction.dead` — Dead letter routing

**Shared File Storage:**
- Both containers mount a shared Docker volume: `invoice-files:/data/invoices`
- The file_path in the message points to a file within this shared volume

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 1.5 days

---

## OBJECTIVE

Implement a RabbitMQ consumer in the Python extraction service that listens to the `invoice.extraction.queue`, processes extraction requests (using the LLM fallback chain or XML parser from Phases 15-18), and publishes results back to the `invoice.extraction.result` exchange. This enables asynchronous, background processing of invoice files for bulk uploads.

---

## MESSAGE CONTRACTS

These contracts are defined by Phase 19-B and must be followed exactly.

### Incoming: Extraction Request Message

The Python consumer will receive messages with this structure from `invoice.extraction.queue`:

| Field | Type | Required | Description |
|---|---|---|---|
| message_id | string (UUID) | Yes | Unique message identifier |
| invoice_id | long | Yes | Database invoice ID |
| company_id | long | Yes | Company the invoice belongs to |
| user_id | long | Yes | User who uploaded the file |
| file_path | string | Yes | Path to the file on shared storage (e.g., /data/invoices/uuid-filename.pdf) |
| file_name | string | Yes | Original file name |
| file_type | string | Yes | MIME type (image/jpeg, image/png, application/pdf, application/xml) |
| file_size | long | Yes | File size in bytes |
| priority | string | No | "HIGH" / "NORMAL" / "LOW" (default: "NORMAL") |
| attempt | integer | No | Current retry attempt number (default: 1) |
| max_attempts | integer | No | Maximum retry attempts (default: 3) |
| timestamp | string (ISO 8601) | Yes | When the message was published |
| correlation_id | string (UUID) | Yes | For matching requests with results |

### Outgoing: Extraction Result Message

After processing, the Python consumer publishes a result to `invoice.extraction.result` exchange with routing key `extraction.result`:

| Field | Type | Required | Description |
|---|---|---|---|
| message_id | string (UUID) | Yes | New unique message ID for this result |
| correlation_id | string (UUID) | Yes | Must match the original request's correlation_id |
| invoice_id | long | Yes | Database invoice ID from the request |
| status | string | Yes | "COMPLETED" or "FAILED" |
| invoice_data | object | If COMPLETED | The extracted InvoiceData (same structure as POST /extract response) |
| confidence_score | float | If COMPLETED | Validation confidence score (0-100) |
| provider | string | If COMPLETED | "GEMINI", "GPT", "CLAUDE", or "XML_PARSER" |
| suggested_status | string | If COMPLETED | "AUTO_VERIFIED" / "NEEDS_REVIEW" / "LOW_CONFIDENCE" |
| error_code | string | If FAILED | Error type identifier |
| error_message | string | If FAILED | Human-readable error description |
| processing_duration_ms | long | Yes | Total processing time in milliseconds |
| timestamp | string (ISO 8601) | Yes | When the result was produced |

---

## DETAILED REQUIREMENTS

### 1. Project Structure

Add the RabbitMQ consumer module to the existing extraction-service:

```
extraction-service/app/
├── messaging/
│   ├── __init__.py
│   ├── consumer.py               # Main RabbitMQ consumer
│   ├── publisher.py              # Result publisher
│   ├── message_models.py         # Pydantic models for messages
│   └── connection_manager.py     # RabbitMQ connection management
├── services/
│   ├── preprocessing/            ← Phase 14
│   ├── llm/                      ← Phase 15-16
│   ├── validation/               ← Phase 17
│   ├── parsers/                  ← Phase 18
│   └── extraction/
│       └── extraction_service.py ← Phase 15 (smart routing added in Phase 18)
└── ...
```

### 2. RabbitMQ Connection Manager

Use the `pika` library (already listed in tech stack) for RabbitMQ connectivity.

**Connection Manager Responsibilities:**
- Establish connection to RabbitMQ using credentials from environment variables
- Handle connection drops and automatic reconnection
- Provide separate connections for consuming and publishing (Pika recommends separate connections for thread safety)
- Implement heartbeat to keep connection alive
- Log connection state changes

**Connection Parameters (from environment, already configured in Phase 0):**
- RABBITMQ_HOST (default: "rabbitmq" — Docker service name)
- RABBITMQ_PORT (default: 5672)
- RABBITMQ_USER (default: "fatura_mq")
- RABBITMQ_PASSWORD (default: "mq_secret_2026")
- RABBITMQ_VHOST (default: "/")

**Reconnection Strategy:**
- If connection is lost, retry with exponential backoff
- Initial retry delay: 5 seconds
- Max retry delay: 60 seconds
- Infinite retries (the consumer should never give up reconnecting)
- Log each reconnection attempt

### 3. Consumer Implementation

The consumer listens to `invoice.extraction.queue` and processes one message at a time.

**Consumer Lifecycle:**
1. Connect to RabbitMQ
2. Declare (or assert) the queue exists (should already be declared by Spring Boot, but assert to be safe)
3. Set prefetch count to 1 (process one message at a time — LLM calls are resource-intensive)
4. Start consuming with manual acknowledgment mode
5. For each message:
   a. Parse the JSON body into ExtractionRequest model
   b. Validate the message (required fields present)
   c. Read the file from the shared volume (file_path)
   d. Detect file type and route to appropriate extractor:
      - Image files → Preprocessing → LLM fallback chain → Validation
      - XML files → XML parser → Validation
   e. Build the result message
   f. Publish result to the result exchange
   g. Acknowledge the message (ack)
6. If processing fails with an unrecoverable error:
   a. Build a FAILED result message with error details
   b. Publish the FAILED result
   c. Acknowledge the message (ack, NOT nack — because Spring Boot handles retry logic)

**Important: Manual Acknowledgment**
- Always ack after processing (whether success or failure)
- The Python consumer does NOT handle retries — it always acks and publishes a result
- Spring Boot decides whether to retry based on the FAILED result
- Only nack (reject) if the message itself is malformed/unparseable — this sends it to DLQ immediately

### 4. Message Processing Flow

```
Message received from queue
          │
          ▼
   Parse JSON body
          │
          ├──► Invalid JSON → nack (send to DLQ)
          │
          ▼
   Validate required fields
          │
          ├──► Missing fields → nack (send to DLQ)
          │
          ▼
   Read file from shared volume
          │
          ├──► File not found → publish FAILED result, ack
          │
          ▼
   Detect file type
          │
          ├──► XML → XML Parser (Phase 18)
          │           │
          │           ▼
          │     InvoiceData + Validation
          │
          ├──► Image → Preprocessing (Phase 14)
          │           │
          │           ▼
          │     LLM Fallback Chain (Phase 16)
          │           │
          │           ▼
          │     InvoiceData + Validation (Phase 17)
          │
          ▼
   Build result message
          │
          ├──► Success → status="COMPLETED", include invoice_data
          │
          ├──► Extraction failed → status="FAILED", include error details
          │
          ▼
   Publish to result exchange
          │
          ▼
   Ack original message
```

### 5. Result Publisher

Publish extraction results to the `invoice.extraction.result` exchange with routing key `extraction.result`.

**Publishing Requirements:**
- Message must be persistent (delivery_mode = 2)
- Content type: application/json
- Set correlation_id in message properties (matching the request)
- Set message_id in message properties (new UUID for the result)
- JSON serialization using the ExtractionResultMessage Pydantic model

### 6. Error Handling and Error Codes

Map internal exceptions to error codes in the FAILED result message:

| Internal Exception | error_code | Description |
|---|---|---|
| FileNotFoundError | FILE_NOT_FOUND | File path doesn't exist on shared volume |
| AllProvidersFailedError | ALL_PROVIDERS_FAILED | All LLM providers failed |
| LLMTimeoutError (all providers) | TIMEOUT | All providers timed out |
| LLMRateLimitError (all providers) | RATE_LIMIT | All providers rate limited |
| XMLParseError | XML_PARSE_ERROR | Invalid XML file |
| NotEInvoiceError | NOT_EINVOICE | XML is not a UBL-TR e-Invoice |
| Unexpected exception | INTERNAL_ERROR | Catch-all for unexpected errors |

### 7. Consumer Startup

The RabbitMQ consumer should run as a background task alongside the FastAPI HTTP server. Both must run in the same Python process/container.

**Approach: Run consumer in a separate thread**
- FastAPI starts normally on port 8001 (serving HTTP endpoints)
- On startup, launch a background thread (or asyncio task) that runs the Pika consumer
- Use FastAPI lifecycle events (on_startup, on_shutdown) to manage the consumer thread
- The consumer thread runs its own Pika event loop (blocking_connection + start_consuming)

**Alternative approach: Separate process with asyncio**
- Use aio-pika (async Pika wrapper) instead of synchronous Pika
- Run the consumer within the FastAPI asyncio event loop
- This avoids thread management but requires async Pika

**Recommendation:** Use synchronous Pika in a background thread. It's simpler and more reliable. The consumer needs its own event loop separate from FastAPI's asyncio loop.

### 8. Graceful Shutdown

When the service receives a shutdown signal (SIGTERM, SIGINT):
1. Stop accepting new messages (cancel consumer)
2. Wait for current message processing to complete (with timeout, e.g., 60 seconds)
3. Close the RabbitMQ connection
4. Shut down FastAPI

This prevents message loss during deployments or restarts.

### 9. Health Check Integration

Update the existing health endpoints to include RabbitMQ consumer status:

**GET /health/ready** should now also check:
- RabbitMQ connection is alive
- Consumer is actively listening

**New field in health response:**
- rabbitmq_connected: true/false
- consumer_active: true/false

### 10. Configuration — Environment Variables

Most are already configured in Phase 0. Add/verify:

- `RABBITMQ_HOST`: Default "rabbitmq"
- `RABBITMQ_PORT`: Default 5672
- `RABBITMQ_USER`: Default "fatura_mq"
- `RABBITMQ_PASSWORD`: Default "mq_secret_2026"
- `RABBITMQ_EXTRACTION_QUEUE`: Default "invoice.extraction.queue"
- `RABBITMQ_RESULT_EXCHANGE`: Default "invoice.extraction.result"
- `RABBITMQ_RESULT_ROUTING_KEY`: Default "extraction.result"
- `RABBITMQ_PREFETCH_COUNT`: Default 1
- `RABBITMQ_HEARTBEAT`: Default 600 (seconds)
- `RABBITMQ_RECONNECT_DELAY`: Default 5 (seconds)
- `RABBITMQ_RECONNECT_MAX_DELAY`: Default 60 (seconds)
- `INVOICE_FILE_STORAGE_PATH`: Default "/data/invoices" (shared volume mount point)

### 11. Logging

**INFO:**
- Consumer started, connected to RabbitMQ
- Message received (message_id, invoice_id, file_name, attempt)
- Processing started (file_type detected, routing decision)
- Extraction completed (invoice_id, provider, confidence_score, duration_ms)
- Result published (correlation_id, status)

**WARNING:**
- File not found on shared volume (file_path)
- Extraction failed (invoice_id, error_code, error_message)
- RabbitMQ connection lost, reconnecting

**ERROR:**
- Malformed message received (parse error, sent to DLQ)
- Failed to publish result (exchange, routing key, error)
- Consumer thread crashed (full traceback)
- Reconnection failed (attempt number, delay)

**DEBUG:**
- Full message body received
- File read details (size, type detection)
- Full extraction result details
- Full result message published

### 12. Dependencies

Add to requirements.txt (if not already present):
- `pika` — RabbitMQ client for Python

Verify in Dockerfile that pika is installed.

### 13. Docker Compose Update

Verify the extraction-service in docker-compose.yml has:
- The shared volume mounted: `invoice-files:/data/invoices`
- Depends on RabbitMQ with health check
- RABBITMQ environment variables

---

## TESTING REQUIREMENTS

### 1. Unit Tests for Message Models
- Test ExtractionRequest model validates all required fields
- Test ExtractionRequest model handles missing optional fields
- Test ExtractionResultMessage model serializes correctly for COMPLETED status
- Test ExtractionResultMessage model serializes correctly for FAILED status
- Test message_id and correlation_id are valid UUIDs

### 2. Unit Tests for Consumer Logic
- Test file type detection routes correctly (image → LLM, XML → parser)
- Test successful extraction produces COMPLETED result with all fields
- Test file not found produces FAILED result with FILE_NOT_FOUND error_code
- Test AllProvidersFailedError produces FAILED result with ALL_PROVIDERS_FAILED
- Test malformed message is rejected (nack)
- Test error code mapping for each exception type

### 3. Unit Tests for Connection Manager
- Test connection parameters built correctly from env vars
- Test reconnection delay calculation (exponential backoff)

### 4. Integration Tests
- Test full flow with mocked RabbitMQ (or test container):
  - Publish request message → consumer processes → result message published
- Test with mocked extraction service:
  - Consumer receives message → calls extraction_service → publishes result
- Test health check includes RabbitMQ status

### 5. Manual Tests
- Publish a test message directly to RabbitMQ Management UI → verify consumer processes it
- Kill RabbitMQ → verify consumer reconnects when RabbitMQ is back
- Publish malformed message → verify it goes to DLQ
- Verify consumer and FastAPI HTTP endpoints run simultaneously

### Test File Structure:
```
extraction-service/tests/
├── unit/
│   ├── test_message_models.py         ← NEW
│   ├── test_consumer_logic.py         ← NEW
│   ├── test_connection_manager.py     ← NEW
│   └── ...
├── integration/
│   ├── test_consumer_flow.py          ← NEW
│   └── ...
└── fixtures/
    ├── messages/                       ← NEW
    │   ├── valid_extraction_request.json
    │   ├── request_missing_fields.json
    │   ├── request_xml_file.json
    │   └── request_image_file.json
    └── ...
```

---

## DATABASE CONSIDERATIONS

The Python consumer does NOT directly access the database. It:
1. Reads files from shared storage
2. Processes extraction (LLM or XML)
3. Publishes results back to RabbitMQ
4. Spring Boot handles all database updates

No migration needed on the Python side.

---

## VERIFICATION CHECKLIST

### Connection
- [ ] Pika connects to RabbitMQ using env vars
- [ ] Connection handles drops and reconnects automatically
- [ ] Heartbeat keeps connection alive
- [ ] Separate connections for consuming and publishing

### Consumer
- [ ] Listens to invoice.extraction.queue
- [ ] Prefetch count set to 1
- [ ] Manual acknowledgment mode
- [ ] Messages deserialized correctly
- [ ] File read from shared volume
- [ ] File type detected and routed correctly
- [ ] Image files → preprocessing → LLM → validation
- [ ] XML files → XML parser → validation
- [ ] Malformed messages rejected to DLQ

### Publisher
- [ ] Results published to invoice.extraction.result exchange
- [ ] Routing key is extraction.result
- [ ] Messages are persistent
- [ ] Content type is application/json
- [ ] correlation_id matches the request

### Result Messages
- [ ] COMPLETED results include invoice_data, confidence_score, provider, suggested_status
- [ ] FAILED results include error_code and error_message
- [ ] Both include processing_duration_ms and timestamp
- [ ] JSON serialization matches the contract from Phase 19-B

### Lifecycle
- [ ] Consumer starts as background thread alongside FastAPI
- [ ] Graceful shutdown waits for current processing
- [ ] Health endpoint reports RabbitMQ connection and consumer status

### Tests
- [ ] All message model unit tests pass
- [ ] All consumer logic unit tests pass
- [ ] Integration test passes
- [ ] Manual test with RabbitMQ Management UI works

---

## RESULT FILE REQUIREMENTS

After completing this phase, create:
`docs/FURKAN/step_results/faz_19_a_result.md`

Include:
1. Execution status and timing
2. Completed tasks checklist
3. Files created/modified with full paths
4. Consumer architecture (thread model, connection management)
5. Message processing flow (with timing for a sample message)
6. Error handling table (exception → error_code mapping)
7. Health check output example (with RabbitMQ status)
8. Test results (unit + integration)
9. Docker Compose changes (shared volume confirmation)
10. Coordination notes (any schema adjustments agreed with Ömer)
11. Issues encountered and solutions
12. Next steps (what Phase 20 and Phase 30-A need)

---

## DEPENDENCIES

### Requires
- **Phase 0**: Docker Compose with RabbitMQ, env vars
- **Phase 13**: FastAPI service structure
- **Phase 14**: Image preprocessing pipeline
- **Phase 15-16**: LLM extraction + fallback chain
- **Phase 17**: Validation & confidence score
- **Phase 18**: E-Invoice XML parser, file type detection
- **Phase 19-B**: RabbitMQ topology and message contracts (ÖMER)

### Required By
- **Phase 20**: File Upload (Spring Boot publishes, Python consumes — full flow works)
- **Phase 21**: Upload UI (shows processing status via WebSocket/polling)
- **Phase 30-A**: Template Learning (uses extraction results for supplier pattern learning)

---

## SUCCESS CRITERIA

1. ✅ Pika consumer connects to RabbitMQ and listens to extraction queue
2. ✅ Extraction request messages consumed and deserialized correctly
3. ✅ Files read from shared Docker volume
4. ✅ Image files processed via LLM fallback chain (Phases 14-17)
5. ✅ XML files processed via XML parser (Phase 18)
6. ✅ COMPLETED results published with invoice_data, confidence_score, provider
7. ✅ FAILED results published with error_code and error_message
8. ✅ Result messages match the contract from Phase 19-B
9. ✅ Consumer runs alongside FastAPI in the same container
10. ✅ Automatic reconnection on RabbitMQ connection loss
11. ✅ Graceful shutdown (finishes current message before stopping)
12. ✅ Health check includes RabbitMQ status
13. ✅ All tests pass
14. ✅ Result file created

---

## IMPORTANT NOTES

1. **Follow 19-B Contract**: The message schemas are defined by Phase 19-B. Do NOT invent your own fields or change the structure. If you need changes, coordinate with Ömer.

2. **One Message at a Time**: Set prefetch_count=1. LLM calls are expensive and slow (up to 30+ seconds). Processing multiple messages in parallel would overload the LLM API rate limits.

3. **Always Ack**: The Python consumer always acknowledges messages (except for truly malformed ones). Even if extraction fails, ack the message and publish a FAILED result. Spring Boot handles retry decisions.

4. **Don't Write to Database**: Python never writes to the PostgreSQL database. All persistence is Spring Boot's responsibility. Python only reads files and publishes results.

5. **Shared Volume Path**: The file_path in the message is an absolute path within the container (e.g., /data/invoices/abc-123.pdf). Both containers mount the same Docker volume at /data/invoices.

6. **Thread Safety**: Pika is NOT thread-safe. Use separate connections for the consumer thread and any publishing from the main thread. The recommended pattern is: consumer in its own thread with its own connection, publisher with a separate connection.

7. **Existing HTTP Endpoints Still Work**: The POST /extract and POST /extract/base64 HTTP endpoints must continue to work. The RabbitMQ consumer is an additional processing pathway, not a replacement.

8. **Consumer Thread Must Not Crash FastAPI**: If the consumer thread encounters an unhandled exception, it should log the error and attempt to restart — not crash the entire FastAPI process.

9. **Pika vs aio-pika**: Use synchronous Pika (not aio-pika) in a background thread. It's simpler and more battle-tested. aio-pika has compatibility nuances with FastAPI's event loop.
