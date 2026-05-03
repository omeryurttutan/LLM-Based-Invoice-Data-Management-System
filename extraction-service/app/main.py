from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.config.settings import settings
from app.core.logging import configure_logging, logger
from app.core.middleware import RequestContextMiddleware
from app.core.exceptions import ExtractionServiceException
from app.models.responses import ErrorResponse
from app.api.routes import health, extraction, preprocessing

# Configure logging
configure_logging()

from app.messaging.consumer import ExtractionConsumer

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("service_startup", version=settings.APP_VERSION, config=settings.model_dump(exclude={"GEMINI_API_KEY", "OPENAI_API_KEY", "ANTHROPIC_API_KEY"}))
    
    # Start RabbitMQ Consumer
    print("DEBUG: Lifespan starting consumer...")
    from app.messaging.consumer import ExtractionConsumer as EC_Class
    print(f"DEBUG: ExtractionConsumer class is: {EC_Class}")
    consumer = ExtractionConsumer()
    print(f"DEBUG: Consumer instance: {consumer}, type: {type(consumer)}")
    
    # Check if patched
    if hasattr(consumer, 'start') and not isinstance(consumer, EC_Class):
         print("DEBUG: Consumer seems to be a Mock!")
    else:
         print("DEBUG: Consumer seems REAL!")

    consumer.start()
    app.state.consumer = consumer # Store in app state for health checks
    logger.info("consumer_background_thread_started")
    
    yield
    
    # Shutdown
    logger.info("consumer_shutdown_initiated")
    if hasattr(app.state, "consumer"):
        app.state.consumer.stop()
        app.state.consumer.join(timeout=5.0)
    logger.info("service_shutdown")

app = FastAPI(
    title=settings.APP_NAME,
    description="""
    Fatura OCR Extraction Service API
    
    This service provides:
    - AI-powered invoice data extraction
    - Image preprocessing and OCR
    - Format validation and normalization
    - Support for PDF, PNG, JPEG, and Base64 inputs
    """,
    version=settings.APP_VERSION,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json",
    contact={
        "name": "Fatura OCR Team",
        "url": "https://faturaocr.com",
        "email": "support@faturaocr.com",
    },
    license_info={
        "name": "Private",
        "url": "https://faturaocr.com/license",
    },
    openapi_tags=[
        {
            "name": "Extraction",
            "description": "Invoice data extraction and validation endpoints",
        },
        {
            "name": "Health",
            "description": "Service health and dependency checks",
        },
        {
            "name": "Preprocessing",
            "description": "Image preprocessing operations",
        },
    ]
)

# Security Middleware
from fastapi import status
import time
from collections import defaultdict

RATE_LIMIT = 30 # requests
RATE_WINDOW = 60 # seconds
request_counts = defaultdict(list)

@app.middleware("http")
async def security_middleware(request: Request, call_next):
    # Skip health check and docs
    if request.url.path.startswith("/health") or request.url.path.startswith("/docs") or request.url.path.startswith("/openapi.json") or request.url.path.startswith("/redoc"):
         return await call_next(request)

    # Check API Key
    api_key = request.headers.get("X-Internal-API-Key")
    expected_api_key = settings.INTERNAL_API_KEY

    if not api_key or api_key != expected_api_key:
        return JSONResponse(
            status_code=status.HTTP_401_UNAUTHORIZED,
            content={"error": "UNAUTHORIZED", "message": "Invalid or missing Internal API Key"}
        )

    # Rate Limiting
    client_ip = request.client.host
    now = time.time()
    
    # Clean old requests
    request_counts[client_ip] = [t for t in request_counts[client_ip] if now - t < RATE_WINDOW]
    
    if len(request_counts[client_ip]) >= RATE_LIMIT:
        return JSONResponse(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            content={"error": "RATE_LIMIT_EXCEEDED", "message": "Too many requests. Please try again later."}
        )
    
    request_counts[client_ip].append(now)
    
    return await call_next(request)

# Middleware
app.add_middleware(RequestContextMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins_list,
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Content-Type", "Authorization", "X-Request-ID"],
    expose_headers=["X-Request-ID", "X-Processing-Time"],
    max_age=600,
)

# Exception Handlers
@app.exception_handler(ExtractionServiceException)
async def extraction_exception_handler(request: Request, exc: ExtractionServiceException):
    return JSONResponse(
        status_code=400,
        content=ErrorResponse(
            error=exc.code,
            message=exc.message,
            details=exc.details,
            request_id=request.headers.get("X-Request-ID")
        ).model_dump(mode='json')
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content=ErrorResponse(
            error="VALIDATION_ERROR",
            message="Geçersiz istek verisi",
            details={"errors": exc.errors()},
            request_id=request.headers.get("X-Request-ID")
        ).model_dump(mode='json')
    )

@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request: Request, exc: StarletteHTTPException):
    return JSONResponse(
        status_code=exc.status_code,
        content=ErrorResponse(
            error="HTTP_ERROR",
            message=str(exc.detail),
            request_id=request.headers.get("X-Request-ID")
        ).model_dump(mode='json')
    )

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error("unhandled_exception", error=str(exc), traceback=True)
    return JSONResponse(
        status_code=500,
        content=ErrorResponse(
            error="INTERNAL_ERROR",
            message="Beklenmeyen bir hata oluştu",
            request_id=request.headers.get("X-Request-ID")
        ).model_dump(mode='json')
    )

# Routers
app.include_router(health.router)
app.include_router(extraction.router, prefix="/api/v1/extraction")
app.include_router(preprocessing.router, prefix="/api")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8001, reload=True)
