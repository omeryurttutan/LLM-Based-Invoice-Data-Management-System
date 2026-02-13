import time
import uuid
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.types import ASGIApp
import structlog

logger = structlog.get_logger()

class RequestContextMiddleware(BaseHTTPMiddleware):
    def __init__(self, app: ASGIApp):
        super().__init__(app)

    async def dispatch(self, request: Request, call_next):
        request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))
        structlog.contextvars.clear_contextvars()
        structlog.contextvars.bind_contextvars(request_id=request_id)
        
        start_time = time.time()
        
        # Log request
        if not request.url.path.startswith("/health"):
            logger.info(
                "request_started",
                path=request.url.path,
                method=request.method,
                client_ip=request.client.host if request.client else None
            )

        try:
            response = await call_next(request)
            
            process_time = (time.time() - start_time) * 1000
            
            response.headers["X-Request-ID"] = request_id
            response.headers["X-Processing-Time"] = f"{process_time:.2f}ms"
            
            # Log response
            if not request.url.path.startswith("/health"):
                logger.info(
                    "request_completed",
                    path=request.url.path,
                    method=request.method,
                    status_code=response.status_code,
                    duration_ms=process_time
                )
                
            return response
            
        except Exception as e:
            process_time = (time.time() - start_time) * 1000
            logger.error(
                "request_failed",
                path=request.url.path,
                method=request.method,
                error=str(e),
                duration_ms=process_time
            )
            raise
