# PHASE 13: PYTHON FASTAPI DATA EXTRACTION SERVICE SETUP

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - **Spring Boot Backend**: Port 8080 - handles auth, CRUD, business logic
  - **Python Microservice**: Port 8000 - handles LLM-based invoice data extraction
  - **Next.js Frontend**: Port 3000 - user interface

### Current State (Phases 0-12 Completed)
- ✅ Phase 0-3: Docker environment, CI/CD, Hexagonal Architecture, Database
- ✅ Phase 4-9: Authentication, RBAC, Company/User Management, Invoice CRUD, Audit Log, Duplication Control
- ✅ Phase 10-12: Frontend Layout, Authentication Pages, Invoice CRUD UI

### Phase Assignment
- **Assigned To**: FURKAN (AI/LLM Developer)
- **Estimated Duration**: 2 days

---

## OBJECTIVE

Create the foundational Python FastAPI microservice that will handle LLM-based invoice data extraction. This phase focuses on setting up the project structure, Docker configuration, basic endpoints, error handling, logging, and connectivity with the Spring Boot backend. No LLM integration yet - that comes in Phase 15-17.

---

## SERVICE ARCHITECTURE

### Role of This Microservice

The Python service acts as a specialized processing engine:

1. **Spring Boot** sends invoice image/PDF to Python service via HTTP
2. **Python service** preprocesses image, calls LLM API, extracts structured data
3. **Python service** returns JSON invoice data to Spring Boot
4. **Spring Boot** saves the data to PostgreSQL

### Why Separate Service?

- Python has superior LLM SDK support (google-generativeai, openai, anthropic)
- Better image processing libraries (Pillow, PyMuPDF)
- Async processing capabilities with FastAPI
- Independent scaling for CPU/GPU intensive tasks
- Isolation of LLM API costs and failures

---

## DETAILED REQUIREMENTS

### 1. Project Structure

Create this directory structure inside the existing `extraction-service/` folder (created in Phase 0):

```
extraction-service/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI app entry point
│   ├── config/
│   │   ├── __init__.py
│   │   └── settings.py            # Pydantic Settings for env vars
│   ├── api/
│   │   ├── __init__.py
│   │   ├── routes/
│   │   │   ├── __init__.py
│   │   │   ├── health.py          # Health check endpoints
│   │   │   └── extraction.py      # Extraction endpoints (stub)
│   │   └── dependencies.py        # Shared dependencies
│   ├── core/
│   │   ├── __init__.py
│   │   ├── logging.py             # Structured logging setup
│   │   ├── exceptions.py          # Custom exceptions
│   │   └── middleware.py          # CORS, request logging
│   ├── models/
│   │   ├── __init__.py
│   │   ├── requests.py            # Request DTOs
│   │   └── responses.py           # Response DTOs
│   └── services/
│       ├── __init__.py
│       └── extraction_service.py  # Business logic (stub)
├── tests/
│   ├── __init__.py
│   ├── conftest.py
│   └── test_health.py
├── Dockerfile
├── requirements.txt
├── pyproject.toml                 # For modern Python tooling
├── .env.example
└── README.md
```

### 2. Dependencies (requirements.txt)

Include these packages with pinned versions:

**Core Framework:**
- fastapi >= 0.109.0
- uvicorn[standard] >= 0.27.0
- pydantic >= 2.5.0
- pydantic-settings >= 2.1.0

**HTTP & Async:**
- httpx >= 0.26.0 (async HTTP client for calling LLM APIs)
- python-multipart >= 0.0.6 (file upload support)

**Image Processing (for future phases):**
- Pillow >= 10.2.0
- PyMuPDF >= 1.23.0 (PDF to image conversion)

**LLM SDKs (install now, use in later phases):**
- google-generativeai >= 0.3.0 (Gemini)
- openai >= 1.10.0 (GPT)
- anthropic >= 0.18.0 (Claude)

**Utilities:**
- python-dotenv >= 1.0.0
- structlog >= 24.1.0 (structured logging)

**Testing:**
- pytest >= 8.0.0
- pytest-asyncio >= 0.23.0
- httpx (also used for testing)

**Code Quality:**
- ruff >= 0.2.0 (linting)
- black >= 24.1.0 (formatting)
- mypy >= 1.8.0 (type checking)

### 3. Configuration (Settings)

Create a Pydantic Settings class that reads from environment variables:

**Required Environment Variables:**
- `APP_NAME` - Application name (default: "Fatura OCR Extraction Service")
- `APP_VERSION` - Version string (default: "1.0.0")
- `DEBUG` - Debug mode flag (default: false)
- `LOG_LEVEL` - Logging level (default: "INFO")
- `ALLOWED_ORIGINS` - CORS origins, comma-separated (default: "http://localhost:3000,http://localhost:8080")

**LLM API Keys (required in later phases, optional now):**
- `GEMINI_API_KEY` - Google Gemini API key
- `OPENAI_API_KEY` - OpenAI API key  
- `ANTHROPIC_API_KEY` - Anthropic Claude API key

**Service URLs:**
- `SPRING_BOOT_URL` - Backend URL (default: "http://localhost:8080")

**Processing Settings:**
- `MAX_FILE_SIZE_MB` - Maximum upload size (default: 10)
- `SUPPORTED_FORMATS` - Allowed file types (default: "pdf,jpg,jpeg,png")
- `REQUEST_TIMEOUT` - HTTP timeout seconds (default: 60)

### 4. FastAPI Application Setup

**Main Application (main.py):**

Configure the FastAPI app with:
- Title: "Fatura OCR Veri Çıkarım Servisi"
- Description: "LLM tabanlı fatura görüntülerinden veri çıkarım API'si"
- Version from settings
- OpenAPI URL at `/openapi.json`
- Docs at `/docs` (Swagger UI)
- Redoc at `/redoc`

**Lifespan Events:**
- On startup: Log service start, validate environment variables, check LLM API keys presence
- On shutdown: Log service stop, cleanup resources

**Include Routers:**
- Health router at `/health`
- Extraction router at `/api/v1/extraction` (stub endpoints)

### 5. Health Check Endpoints

**GET /health**
Returns basic health status:
- status: "healthy" or "unhealthy"
- service: service name
- version: service version
- timestamp: ISO timestamp

**GET /health/ready**
Readiness check (for Kubernetes/Docker):
- Checks if service is ready to accept requests
- Returns 200 if ready, 503 if not

**GET /health/live**
Liveness check:
- Simple ping to confirm service is running
- Always returns 200 if service is up

**GET /health/dependencies**
Dependency health check:
- Checks Spring Boot backend connectivity (HTTP call to /actuator/health)
- Reports LLM API key availability (not validity - just presence)
- Returns status of each dependency

### 6. Extraction Endpoints (Stubs)

Create placeholder endpoints that will be implemented in Phase 15-17:

**POST /api/v1/extraction/extract**
- Accepts: multipart/form-data with image file
- Returns: Stub response with message "Extraction not implemented yet"
- Status: 501 Not Implemented

**POST /api/v1/extraction/extract-batch**
- Accepts: multiple files
- Returns: Stub response
- Status: 501 Not Implemented

**GET /api/v1/extraction/providers**
- Returns: List of available LLM providers based on configured API keys
- Example: ["GEMINI", "GPT", "CLAUDE"] or subset based on keys present

### 7. Request/Response Models

**HealthResponse:**
- status: string
- service: string
- version: string
- timestamp: datetime

**DependencyHealthResponse:**
- spring_boot: object with status and latency_ms
- llm_providers: object with gemini, openai, anthropic availability

**ExtractionRequest (for future):**
- file: UploadFile
- provider: optional string (GEMINI, GPT, CLAUDE)
- options: optional object

**ExtractionResponse (for future):**
- success: boolean
- invoice_data: object (structured invoice fields)
- provider_used: string
- confidence_score: float
- processing_time_ms: integer
- raw_response: optional string (debug)

### 8. Error Handling

**Custom Exception Classes:**
- `ExtractionServiceException` - Base exception
- `FileValidationError` - Invalid file type/size
- `LLMProviderError` - LLM API call failed
- `ConfigurationError` - Missing configuration

**Global Exception Handler:**
Register handlers for:
- `RequestValidationError` → 422 with detailed errors
- `ExtractionServiceException` → 400 with error details
- `HTTPException` → Pass through
- `Exception` → 500 with generic message (log full trace)

**Error Response Format:**
- error: error code string
- message: human-readable message (Turkish)
- details: optional additional info
- timestamp: ISO timestamp
- request_id: correlation ID

### 9. Structured Logging

Use structlog for JSON-formatted logs:

**Log Fields:**
- timestamp (ISO format)
- level (INFO, WARNING, ERROR, DEBUG)
- message
- service_name
- request_id (from middleware)
- extra context fields

**Log Events:**
- Application startup/shutdown
- Request received (method, path, client IP)
- Request completed (status code, duration_ms)
- Errors with full traceback
- External API calls (LLM, Spring Boot)

**Configuration:**
- Development: Pretty console output
- Production: JSON format for log aggregation

### 10. CORS and Security Middleware

**CORS Configuration:**
- Allow origins from ALLOWED_ORIGINS setting
- Allow methods: GET, POST, OPTIONS
- Allow headers: Content-Type, Authorization, X-Request-ID
- Expose headers: X-Request-ID, X-Processing-Time
- Max age: 600 seconds

**Request ID Middleware:**
- Generate UUID for each request if not provided
- Add to response headers as X-Request-ID
- Include in all log entries

**Request Logging Middleware:**
- Log incoming requests
- Log outgoing responses with timing
- Skip logging for health endpoints (reduce noise)

### 11. Docker Configuration

**Dockerfile:**
- Base image: python:3.11-slim
- Working directory: /app
- Install system dependencies (for Pillow, PyMuPDF)
- Copy requirements.txt and install with --no-cache-dir
- Copy application code
- Create non-root user for security
- Expose port 8000
- Run with uvicorn, 4 workers, host 0.0.0.0

**Required system packages:**
- libpng-dev, libjpeg-dev (for Pillow)
- libmupdf-dev (for PyMuPDF) - or install via pip

**Docker Compose Update:**
Add/update the extraction-service in docker-compose.yml:
- Service name: extraction-service
- Build from ./extraction-service
- Port mapping: 8000:8000
- Environment variables from .env
- Depends on: backend (for connectivity test)
- Health check: curl to /health
- Networks: same as backend

### 12. Spring Boot Connectivity Test

Create a simple test to verify communication:

**From Python to Spring Boot:**
- Call GET http://backend:8080/actuator/health
- Parse response and log status
- Handle connection errors gracefully

**From Spring Boot to Python (manual test):**
- Call GET http://extraction-service:8000/health
- Verify response structure

**Integration Test Endpoint:**
GET /health/integration
- Tests round-trip: Python → Spring Boot → Python response
- Returns latency measurements

---

## TESTING REQUIREMENTS

### Unit Tests

Create tests in `tests/` directory:

1. **test_health.py:**
   - Test /health returns 200 with correct structure
   - Test /health/ready returns 200
   - Test /health/live returns 200
   - Test /health/dependencies returns provider status

2. **test_config.py:**
   - Test settings load from environment
   - Test default values work
   - Test validation errors for invalid values

3. **test_models.py:**
   - Test request model validation
   - Test response model serialization

### Running Tests

```bash
# Inside extraction-service directory
pytest tests/ -v --asyncio-mode=auto

# With coverage
pytest tests/ --cov=app --cov-report=html
```

### Manual Testing

1. Start services with Docker Compose
2. Access Swagger UI at http://localhost:8000/docs
3. Test each health endpoint
4. Verify logs in container output
5. Test CORS with browser fetch from frontend

---

## VERIFICATION CHECKLIST

### Project Setup
- [ ] Directory structure created as specified
- [ ] requirements.txt with all dependencies
- [ ] pyproject.toml for tooling configuration
- [ ] .env.example with all variables documented

### FastAPI Application
- [ ] App created with correct metadata
- [ ] Lifespan events (startup/shutdown) working
- [ ] Routers included at correct paths
- [ ] Docs accessible at /docs

### Configuration
- [ ] Pydantic Settings class created
- [ ] All env vars documented
- [ ] Defaults work for local development
- [ ] Validation errors are clear

### Endpoints
- [ ] GET /health returns correct structure
- [ ] GET /health/ready works
- [ ] GET /health/live works
- [ ] GET /health/dependencies shows provider status
- [ ] POST /api/v1/extraction/extract returns 501
- [ ] GET /api/v1/extraction/providers lists available providers

### Error Handling
- [ ] Custom exceptions defined
- [ ] Global handler catches all errors
- [ ] Error responses are Turkish
- [ ] Stack traces logged but not exposed

### Logging
- [ ] Structured logging configured
- [ ] Request ID in all logs
- [ ] Request/response logging works
- [ ] Different formats for dev/prod

### Middleware
- [ ] CORS allows configured origins
- [ ] Request ID generated/forwarded
- [ ] Timing headers added

### Docker
- [ ] Dockerfile builds successfully
- [ ] Container runs on port 8000
- [ ] Health check passes
- [ ] Connects to same network as backend

### Integration
- [ ] Python can reach Spring Boot /actuator/health
- [ ] Spring Boot can reach Python /health
- [ ] docker-compose up starts all services

### Tests
- [ ] pytest runs without errors
- [ ] Health endpoint tests pass
- [ ] Minimum 80% code coverage

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/FURKAN/step_results/faz_13_result.md`

Include:

### 1. Execution Status
- Overall: Success / Partial Success / Failed
- Date completed
- Actual time vs estimated (2 days)

### 2. Completed Tasks
Checklist of all requirements

### 3. Files Created
Full tree of extraction-service/ directory

### 4. Endpoint Summary
| Endpoint | Method | Status | Description |
|----------|--------|--------|-------------|
| /health | GET | ✅ | Basic health |
| /health/ready | GET | ✅ | Readiness |
| ... | ... | ... | ... |

### 5. Docker Status
- Build output
- Container logs snippet
- Health check result

### 6. Integration Test Results
- Spring Boot → Python: Working/Failed
- Python → Spring Boot: Working/Failed
- Latency measurements

### 7. Test Output
```
pytest tests/ -v
# Include actual output
```

### 8. Issues Encountered
Document problems and solutions

### 9. Environment Variables
List all configured variables and their purposes

### 10. Next Steps
What Phase 14 (Image Preprocessing) needs from this service

### 11. Time Spent
Actual vs estimated

---

## DEPENDENCIES

### Requires
- **Phase 0**: Docker Compose environment, extraction-service directory created
- **Phase 1**: CI/CD pipeline (add Python linting step)

### Required By
- **Phase 14**: Image Preprocessing Pipeline (needs base FastAPI service)
- **Phase 15**: Gemini LLM Integration (needs service structure, config, error handling)
- **Phase 16**: GPT Fallback (needs same)
- **Phase 17**: Claude Fallback (needs same)
- **Phase 18**: Provider Selection Strategy (needs /providers endpoint)

---

## SUCCESS CRITERIA

1. ✅ FastAPI service runs on port 8000
2. ✅ All health endpoints return correct responses
3. ✅ Swagger docs accessible and accurate
4. ✅ Structured logging working
5. ✅ CORS configured for frontend/backend
6. ✅ Error handling returns Turkish messages
7. ✅ Docker container builds and runs
8. ✅ Spring Boot connectivity verified
9. ✅ LLM API keys configurable (not validated)
10. ✅ Tests pass with good coverage
11. ✅ CI/CD includes Python linting
12. ✅ Result file created

---

## IMPORTANT NOTES

1. **No LLM Calls Yet**: This phase only sets up infrastructure. LLM integration comes in Phase 15-17.

2. **Port 8000**: The extraction service runs on 8000, not 8080 (that's Spring Boot).

3. **Docker Networking**: Services communicate via Docker network using service names (extraction-service, backend), not localhost.

4. **Environment Variables**: Use .env file for local development. In production, use proper secret management.

5. **Python Version**: Use Python 3.11 for best compatibility with LLM SDKs.

6. **Async First**: FastAPI is async. Use httpx (async) instead of requests (sync) for HTTP calls.

7. **Turkish Messages**: User-facing error messages should be in Turkish. Logs can be in English.

8. **CI/CD Update**: Add Python linting (ruff) and testing (pytest) to GitHub Actions workflow.

---

**Phase 13 Completion Target**: A running, tested, documented FastAPI microservice with health endpoints, proper configuration, logging, error handling, and verified connectivity with the Spring Boot backend - ready for LLM integration in subsequent phases.
